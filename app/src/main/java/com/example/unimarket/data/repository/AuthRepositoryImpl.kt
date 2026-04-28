package com.example.unimarket.data.repository

import android.util.Log
import androidx.core.net.toUri
import com.example.unimarket.data.api.OtpDevApiService
import com.example.unimarket.data.api.model.OtpDevErrorResponseDto
import com.example.unimarket.data.api.model.OtpDevSendVerificationRequestDto
import com.example.unimarket.data.api.model.OtpDevVerificationDataDto
import com.example.unimarket.data.notification.FcmTokenManager
import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.model.SellerPaymentMethodType
import com.example.unimarket.data.local.UserSessionLocalDataSource
import com.example.unimarket.domain.model.UserProfile
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.domain.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.SetOptions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import retrofit2.Response
import com.google.gson.Gson
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val otpDevApiService: OtpDevApiService,
    private val remoteConfig: FirebaseRemoteConfig,
    private val userSessionLocalDataSource: UserSessionLocalDataSource,
    private val fcmTokenManager: FcmTokenManager
) : AuthRepository {
    private companion object {
        const val TAG = "AuthRepositoryImpl"
        const val USERS_COLLECTION = "users"
        const val PHONE_NUMBERS_COLLECTION = "phoneNumbers"
        const val ADDRESSES_COLLECTION = "addresses"
        const val PAYMENT_METHODS_FIELD = "paymentMethods"
        const val KEY_OTP_SMS_API_KEY = "API_KEY_OTP_SMS"
        const val KEY_OTP_CODE_LENGTH = "CODE_LENGTH"
        const val KEY_OTP_SENDER = "SENDER"
        const val KEY_OTP_TEMPLATE = "TEMPLATE"
        const val DEFAULT_OTP_SENDER = "71e8a9ea-7daf-4fcc-b5d2-51d5ba57408a"
        const val DEFAULT_OTP_TEMPLATE = "027fe998-92a5-4fd6-9b2a-83e4576ce1a9"
        const val DEFAULT_OTP_CODE_LENGTH = 4
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val currentUser = auth.currentUser
            currentUser?.reload()?.await()

            if (currentUser.requiresEmailVerification()) {
                logout()
                Result.failure(Exception("Please verify your email before logging in. Check your inbox for the verification link."))
            } else {
                refreshCurrentUserProfile().map { Unit }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(
        name: String,
        email: String,
        university: String,
        password: String,
        phoneNumber: String
    ): Result<Unit> {
        return try {
            val storedPhoneNumber = phoneNumber.toStoredPhoneNumber()
            ensurePhoneNumberAvailableForCurrentUser(storedPhoneNumber)

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user

            // Optionally set the display name using the provided 'name'
            if (user != null) {
                val profileUpdates = userProfileChangeRequest {
                    displayName = name
                    photoUri = "https://ui-avatars.com/api/?name=${
                        name.replace(
                            " ",
                            "+"
                        )
                    }&background=random".toUri()
                }
                user.updateProfile(profileUpdates).await()

                val userProfile = UserProfile(
                    id = user.uid,
                    displayName = name,
                    email = email,
                    avatarUrl = profileUpdates.photoUri?.toString().orEmpty(),
                    phoneNumber = storedPhoneNumber,
                    university = university.trim(),
                    studentId = "",
                    boughtCount = 0,
                    soldCount = 0,
                    averageRating = 0.0,
                    ratingCount = 0,
                    walletBalance = 0.0
                )

                try {
                    syncUserDocumentWithPhoneReservation(
                        profile = userProfile,
                        includeBoughtCount = true,
                        includeSoldCount = true,
                        includeWalletBalance = true
                    )
                } catch (e: Exception) {
                    runCatching { user.delete().await() }
                    runCatching { logout() }
                    throw e
                }

                user.sendEmailVerification().await()
            }

            logout()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        runCatching { fcmTokenManager.clearCurrentUserToken() }
            .onFailure { Log.w("AuthRepositoryImpl", "Failed to clear FCM token before logout", it) }
        auth.signOut()
        userSessionLocalDataSource.clear()
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user

            if (user != null && user.displayName.isNullOrEmpty()) {
                // Set default display name if none exists
                val profileUpdates = userProfileChangeRequest {
                    displayName = user.email ?: "User"
                }
                user.updateProfile(profileUpdates).await()
            }

            refreshCurrentUserProfile().map { Unit }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPhoneVerificationCode(phoneNumber: String): Result<Unit> {
        return try {
            runCatching { remoteConfig.fetchAndActivate().await() }

            val apiKey = remoteConfig.getString(KEY_OTP_SMS_API_KEY).trim()
            val sender = remoteConfig.getString(KEY_OTP_SENDER).trim().ifBlank { DEFAULT_OTP_SENDER }
            val template = remoteConfig.getString(KEY_OTP_TEMPLATE).trim().ifBlank { DEFAULT_OTP_TEMPLATE }
            val codeLength = remoteConfig.getString(KEY_OTP_CODE_LENGTH).trim().toIntOrNull()
                ?.coerceIn(4, 10)
                ?: DEFAULT_OTP_CODE_LENGTH
            val normalizedPhone = phoneNumber.toOtpDevPhone()
            ensurePhoneNumberAvailableForCurrentUser(normalizedPhone.toStoredPhoneNumber())

            if (apiKey.isBlank()) {
                return Result.failure(Exception("Chưa cấu hình khóa gửi mã OTP"))
            }
            Log.d(
                TAG,
                "sendPhoneVerificationCode requested for ${normalizedPhone.maskForLog()}"
            )
            Log.d(
                TAG,
                "sendPhoneVerificationCode request -> endpoint=/v1/verifications, headers={X-OTP-Key=${apiKey.maskSecretForLog()}, accept=application/json, content-type=application/json}, body={data:{channel=sms,sender=$sender,phone=$normalizedPhone,template=$template,code_length=$codeLength}}"
            )
            val response = otpDevApiService.sendSmsVerification(
                apiKey = apiKey,
                body = OtpDevSendVerificationRequestDto(
                    data = OtpDevVerificationDataDto(
                        channel = "sms",
                        sender = sender,
                        phone = normalizedPhone,
                        template = template,
                        code_length = codeLength
                    )
                )
            )
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                val rawMessage = response.otpDevErrorMessage()
                val message = "Không thể gửi mã xác thực"
                Log.e(
                    TAG,
                    "sendPhoneVerificationCode failed: code=${response.code()}, message=$rawMessage, rawError=${response.rawErrorBodyForLog()}"
                )
                return Result.failure(Exception(message))
            }
            Log.d(
                TAG,
                "sendPhoneVerificationCode success: messageId=${body.message_id}, to=${body.phone.maskForLog()}, expireDate=${body.expire_date}"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "sendPhoneVerificationCode exception", e)
            Result.failure(e)
        }
    }

    override suspend fun verifyPhoneVerificationCode(phoneNumber: String, code: String): Result<Unit> {
        return try {
            runCatching { remoteConfig.fetchAndActivate().await() }
            val apiKey = remoteConfig.getString(KEY_OTP_SMS_API_KEY).trim()
            if (apiKey.isBlank()) {
                return Result.failure(Exception("Chưa cấu hình khóa gửi mã OTP"))
            }
            val normalizedPhone = phoneNumber.toOtpDevPhone()
            val normalizedCode = code.trim()
            Log.d(
                TAG,
                "verifyPhoneVerificationCode requested for ${normalizedPhone.maskForLog()} with codeLength=${normalizedCode.length}"
            )
            val response = otpDevApiService.verifySmsCode(
                apiKey = apiKey,
                code = normalizedCode,
                phone = normalizedPhone
            )
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                val rawMessage = response.otpDevErrorMessage()
                val message = "Mã xác thực không đúng hoặc đã hết hạn"
                Log.e(
                    TAG,
                    "verifyPhoneVerificationCode failed: code=${response.code()}, message=$rawMessage, rawError=${response.rawErrorBodyForLog()}"
                )
                return Result.failure(Exception(message))
            }
            if (body.data.isNullOrEmpty()) {
                Log.w(
                    TAG,
                    "verifyPhoneVerificationCode rejected: empty data for ${normalizedPhone.maskForLog()}"
                )
                return Result.failure(Exception("Mã xác thực không đúng hoặc đã hết hạn"))
            }
            val first = body.data.first()
            Log.d(
                TAG,
                "verifyPhoneVerificationCode success: messageId=${first.message_id}, to=${first.phone.maskForLog()}, expireDate=${first.expire_date}"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "verifyPhoneVerificationCode exception", e)
            Result.failure(e)
        }
    }

    override suspend fun hasPhoneNumber(): Result<Boolean> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("Vui lòng đăng nhập lại"))
        if (!currentUser.phoneNumber.isNullOrBlank()) return Result.success(true)
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()
            val hasPhone = snapshot.getString("phoneNumber").orEmpty().isNotBlank()
            Log.d(TAG, "hasPhoneNumber from firestore: $hasPhone")
            Result.success(hasPhone)
        } catch (e: Exception) {
            Log.e(TAG, "hasPhoneNumber failed", e)
            Result.failure(e)
        }
    }

    override suspend fun savePhoneNumber(phoneNumber: String): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("Vui lòng đăng nhập lại"))
        val normalized = phoneNumber.toStoredPhoneNumber()
        if (normalized.isBlank()) return Result.failure(Exception("Vui lòng nhập số điện thoại"))
        return try {
            Log.d(TAG, "savePhoneNumber for user=${currentUser.uid.take(6)}*** number=${normalized.maskForLog()}")
            ensurePhoneNumberAvailableForCurrentUser(normalized)
            savePhoneNumberWithReservation(
                userId = currentUser.uid,
                phoneNumber = normalized
            )
            refreshCurrentUserProfile().map { Unit }
        } catch (e: Exception) {
            Log.e(TAG, "savePhoneNumber failed", e)
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): Any? {
        return auth.currentUser
    }

    override fun getCachedUser(): UserProfile? {
        return userSessionLocalDataSource.getCachedUser()
    }

    override fun observeCachedUser(): Flow<UserProfile?> {
        return userSessionLocalDataSource.cachedUser
    }

    override suspend fun refreshCurrentUserProfile(): Result<UserProfile> {
        val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))

        return try {
            val userDoc = firestore.collection(USERS_COLLECTION).document(user.uid).get().await()
            val userProfile = userDoc.toUserProfile(user)

            if (shouldBackfillUserDocument(userDoc)) {
                syncUserDocument(
                    profile = userProfile,
                    includeBoughtCount = true,
                    includeSoldCount = true,
                    includeWalletBalance = true
                )
            }

            userSessionLocalDataSource.saveUser(userProfile)
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserAvatarUrl(id: String): Result<String> {
        return try {
            val user = firestore.collection("users")
                .document(id)
                .get(Source.SERVER)
                .await()

            val snapshot = if (!user.exists()) {
                firestore.collection("users")
                    .document(id)
                    .get(Source.CACHE)
                    .await()
            } else {
                user
            }

            if (snapshot.exists()) {
                val avatarUrl = snapshot.getString("avatarUrl").orEmpty()
                Log.d(
                    "AuthRepositoryImpl",
                    "getUserAvatarUrl id=$id exists=${snapshot.exists()} avatarUrl=$avatarUrl"
                )
                Result.success(avatarUrl)
            } else {
                Log.w("AuthRepositoryImpl", "getUserAvatarUrl user not found for id=$id")
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "getUserAvatarUrl failed for id=$id", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserUniversityById(userId: String): Result<String> {
        if (userId.isBlank()) return Result.success("")
        return try {
            val user = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get(Source.SERVER)
                .await()

            val snapshot = if (!user.exists()) {
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .get(Source.CACHE)
                    .await()
            } else {
                user
            }

            if (snapshot.exists()) {
                Result.success(snapshot.getString("university").orEmpty().trim())
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(name: String?, avatarUrl: String?): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                val profileUpdates = userProfileChangeRequest {
                    if (name != null) displayName = name
                    if (avatarUrl != null) photoUri = avatarUrl.toUri()
                }
                user.updateProfile(profileUpdates).await()
                syncUserDocument(
                    profile = UserProfile(
                        id = user.uid,
                        displayName = user.displayName ?: user.email ?: "User",
                        email = user.email.orEmpty(),
                        avatarUrl = user.authAvatarUrl()
                    )
                )
                refreshCurrentUserProfile().map { Unit }
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUniversity(university: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No user logged in"))
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(mapOf("university" to university.trim()), SetOptions.merge())
                .await()
            refreshCurrentUserProfile().map { Unit }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserAddresses(): Result<List<UserAddress>> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
        return getAddressesByUserId(userId)
    }

    override suspend fun getUserPaymentMethods(): Result<List<SellerPaymentMethod>> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
        return getPaymentMethodsByUserId(userId)
    }

    override suspend fun getAddressesByUserId(userId: String): Result<List<UserAddress>> {
        return try {
            val snapshot = addressCollection(userId).get().await()
            Result.success(snapshot.toAddresses())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPaymentMethodsByUserId(userId: String): Result<List<SellerPaymentMethod>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            Result.success(snapshot.toPaymentMethods())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addUserAddress(address: UserAddress): Result<Unit> {
        return try {
            val userId =
                auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val existingAddresses = addressCollection(userId).get().await().toAddresses()
            val shouldBeDefault = address.isDefault || existingAddresses.none { it.isDefault }
            val docRef = if (address.id.isBlank()) {
                addressCollection(userId).document()
            } else {
                addressCollection(userId).document(address.id)
            }
            saveAddress(
                userId,
                docRef.id,
                address.copy(id = docRef.id, isDefault = shouldBeDefault)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserAddress(address: UserAddress): Result<Unit> {
        return try {
            val userId =
                auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            if (address.id.isBlank()) return Result.failure(Exception("Address id is required"))
            saveAddress(userId, address.id, address)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUserAddress(addressId: String): Result<Unit> {
        return try {
            val userId =
                auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val collection = addressCollection(userId)
            val currentSnapshot = collection.get().await()
            val existingAddresses = currentSnapshot.toAddresses()
            val deletedAddress = existingAddresses.firstOrNull { it.id == addressId }
            collection.document(addressId).delete().await()

            if (deletedAddress?.isDefault == true) {
                val firstRemaining = collection.get().await().documents.firstOrNull()
                firstRemaining?.reference?.update("isDefault", true)?.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserPaymentMethod(method: SellerPaymentMethod): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            val currentMethods = userRef.get().await().toPaymentMethods()
            val sanitizedMethod = method.sanitized().copy(
                id = method.id.ifBlank { UUID.randomUUID().toString() }
            )

            val nextMethods = currentMethods
                .filterNot { it.id == sanitizedMethod.id }
                .plus(sanitizedMethod)
                .normalizeDefaults()

            userRef.set(
                mapOf(PAYMENT_METHODS_FIELD to nextMethods.toFirestoreList()),
                SetOptions.merge()
            ).await()

            refreshCurrentUserProfile()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUserPaymentMethod(methodId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            val remainingMethods = userRef.get().await()
                .toPaymentMethods()
                .filterNot { it.id == methodId }
                .normalizeDefaults()

            userRef.set(
                mapOf(PAYMENT_METHODS_FIELD to remainingMethods.toFirestoreList()),
                SetOptions.merge()
            ).await()

            refreshCurrentUserProfile()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveAddress(
        userId: String,
        addressId: String,
        address: UserAddress
    ): Result<Unit> {
        val collection = addressCollection(userId)
        val batch = firestore.batch()

        if (address.isDefault) {
            collection.get().await().documents.forEach { document ->
                batch.update(document.reference, "isDefault", document.id == addressId)
            }
        }

        batch.set(
            collection.document(addressId),
            mapOf(
                "recipientName" to address.recipientName,
                "phoneNumber" to address.phoneNumber,
                "addressLine" to address.addressLine,
                "isDefault" to address.isDefault
            )
        )
        batch.commit().await()
        return Result.success(Unit)
    }

    private fun addressCollection(userId: String) =
        firestore.collection(USERS_COLLECTION).document(userId).collection(ADDRESSES_COLLECTION)

    private suspend fun ensurePhoneNumberAvailableForCurrentUser(phoneNumber: String) {
        val currentUserId = auth.currentUser?.uid
        val storedPhoneNumber = phoneNumber.toStoredPhoneNumber()
        val legacyE164PhoneNumber = storedPhoneNumber.toE164Phone()
        val snapshot = firestore.collection(PHONE_NUMBERS_COLLECTION)
            .document(storedPhoneNumber.toPhoneNumberDocumentId())
            .get()
            .await()
        val ownerUid = snapshot.getString("uid")
        if (snapshot.exists() && (ownerUid.isNullOrBlank() || ownerUid != currentUserId)) {
            throw PhoneNumberAlreadyUsedException()
        }

        val storedOwner = findOtherPhoneOwner(
            phoneNumber = storedPhoneNumber,
            currentUserId = currentUserId
        )
        val legacyOwner = if (storedOwner == null) {
            findOtherPhoneOwner(
                phoneNumber = legacyE164PhoneNumber,
                currentUserId = currentUserId
            )
        } else {
            null
        }

        if (storedOwner != null || legacyOwner != null) {
            throw PhoneNumberAlreadyUsedException()
        }
    }

    private suspend fun findOtherPhoneOwner(
        phoneNumber: String,
        currentUserId: String?
    ): DocumentSnapshot? {
        return firestore.collection(USERS_COLLECTION)
            .whereEqualTo("phoneNumber", phoneNumber)
            .limit(2)
            .get()
            .await()
            .documents
            .firstOrNull { it.id != currentUserId }
    }

    private suspend fun syncUserDocumentWithPhoneReservation(
        profile: UserProfile,
        includeBoughtCount: Boolean = false,
        includeSoldCount: Boolean = false,
        includeWalletBalance: Boolean = false
    ) {
        val normalizedPhoneNumber = profile.phoneNumber.toStoredPhoneNumber()
        val userRef = firestore.collection(USERS_COLLECTION).document(profile.id)
        val phoneRef = firestore.collection(PHONE_NUMBERS_COLLECTION)
            .document(normalizedPhoneNumber.toPhoneNumberDocumentId())
        val updateMap = buildUserUpdateMap(
            profile = profile.copy(phoneNumber = normalizedPhoneNumber),
            includeBoughtCount = includeBoughtCount,
            includeSoldCount = includeSoldCount,
            includeWalletBalance = includeWalletBalance
        )

        firestore.runTransaction { transaction ->
            val phoneSnapshot = transaction.get(phoneRef)
            val ownerUid = phoneSnapshot.getString("uid")
            if (phoneSnapshot.exists() && (ownerUid.isNullOrBlank() || ownerUid != profile.id)) {
                throw PhoneNumberAlreadyUsedException()
            }

            transaction.set(
                phoneRef,
                buildPhoneNumberReservationMap(
                    userId = profile.id,
                    phoneNumber = normalizedPhoneNumber,
                    exists = phoneSnapshot.exists()
                ),
                SetOptions.merge()
            )
            transaction.set(userRef, updateMap, SetOptions.merge())
            null
        }.await()
    }

    private suspend fun savePhoneNumberWithReservation(userId: String, phoneNumber: String) {
        val userRef = firestore.collection(USERS_COLLECTION).document(userId)
        val phoneRef = firestore.collection(PHONE_NUMBERS_COLLECTION)
            .document(phoneNumber.toPhoneNumberDocumentId())

        firestore.runTransaction { transaction ->
            val phoneSnapshot = transaction.get(phoneRef)
            val ownerUid = phoneSnapshot.getString("uid")
            if (phoneSnapshot.exists() && (ownerUid.isNullOrBlank() || ownerUid != userId)) {
                throw PhoneNumberAlreadyUsedException()
            }

            val userSnapshot = transaction.get(userRef)
            val previousPhoneNumber = userSnapshot.getString("phoneNumber").orEmpty()
                .toStoredPhoneNumber()
            val previousPhoneRef = previousPhoneNumber
                .takeIf { it.isNotBlank() && it != phoneNumber }
                ?.let {
                    firestore.collection(PHONE_NUMBERS_COLLECTION)
                        .document(it.toPhoneNumberDocumentId())
                }
            val previousPhoneSnapshot = previousPhoneRef?.let { transaction.get(it) }

            transaction.set(
                phoneRef,
                buildPhoneNumberReservationMap(
                    userId = userId,
                    phoneNumber = phoneNumber,
                    exists = phoneSnapshot.exists()
                ),
                SetOptions.merge()
            )
            transaction.set(userRef, mapOf("phoneNumber" to phoneNumber), SetOptions.merge())

            if (previousPhoneRef != null && previousPhoneSnapshot?.getString("uid") == userId) {
                transaction.delete(previousPhoneRef)
            }

            null
        }.await()
    }

    private suspend fun syncUserDocument(
        profile: UserProfile,
        includeBoughtCount: Boolean = false,
        includeSoldCount: Boolean = false,
        includeWalletBalance: Boolean = false
    ) {
        val updateMap = buildUserUpdateMap(
            profile = profile,
            includeBoughtCount = includeBoughtCount,
            includeSoldCount = includeSoldCount,
            includeWalletBalance = includeWalletBalance
        )

        firestore.collection(USERS_COLLECTION)
            .document(profile.id)
            .set(updateMap, SetOptions.merge())
            .await()
    }

    private fun buildUserUpdateMap(
        profile: UserProfile,
        includeBoughtCount: Boolean = false,
        includeSoldCount: Boolean = false,
        includeWalletBalance: Boolean = false
    ): MutableMap<String, Any> {
        val updateMap = mutableMapOf<String, Any>(
            "displayName" to profile.displayName,
            "name" to profile.displayName,
            "avatarUrl" to profile.avatarUrl,
            "university" to profile.university.trim(),
            "isLock" to profile.isLock,
            "photoUrl" to FieldValue.delete()
        )

        if (profile.email.isNotBlank()) {
            updateMap["email"] = profile.email
        }
        if (profile.studentId.isNotBlank()) {
            updateMap["studentId"] = profile.studentId
        }
        if (profile.phoneNumber.isNotBlank()) {
            updateMap["phoneNumber"] = profile.phoneNumber.toStoredPhoneNumber()
        }
        if (includeBoughtCount) {
            updateMap["boughtCount"] = profile.boughtCount
        }
        if (includeSoldCount) {
            updateMap["soldCount"] = profile.soldCount
        }
        if (includeWalletBalance) {
            updateMap["walletBalance"] = profile.walletBalance
        }

        return updateMap
    }

    private fun buildPhoneNumberReservationMap(
        userId: String,
        phoneNumber: String,
        exists: Boolean
    ): Map<String, Any> {
        return buildMap {
            put("uid", userId)
            put("phoneNumber", phoneNumber)
            put("updatedAt", FieldValue.serverTimestamp())
            if (!exists) {
                put("createdAt", FieldValue.serverTimestamp())
            }
        }
    }

    private fun shouldBackfillUserDocument(userDoc: DocumentSnapshot): Boolean {
        val documentData = userDoc.data.orEmpty()
        return !userDoc.exists() ||
                userDoc.getString("name").isNullOrBlank() ||
                userDoc.getString("displayName").isNullOrBlank() ||
                userDoc.getString("email").isNullOrBlank() ||
                userDoc.getString("avatarUrl").isNullOrBlank() ||
                !documentData.containsKey("boughtCount") ||
                !documentData.containsKey("soldCount") ||
                !documentData.containsKey("walletBalance") ||
                !documentData.containsKey("isLock")
    }

    private fun DocumentSnapshot.toUserProfile(user: FirebaseUser): UserProfile {
        val displayName = getString("name").orEmpty()
            .ifBlank { getString("displayName").orEmpty() }
            .ifBlank { user.displayName ?: user.email ?: "User" }
        val avatarUrl = getString("avatarUrl").orEmpty()
            .ifBlank { user.authAvatarUrl() }

        return UserProfile(
            id = user.uid,
            displayName = displayName,
            email = getString("email").orEmpty().ifBlank { user.email.orEmpty() },
            avatarUrl = avatarUrl,
            phoneNumber = getString("phoneNumber").orEmpty().ifBlank { user.phoneNumber.orEmpty() },
            university = getString("university").orEmpty(),
            isLock = getBoolean("isLock") ?: false,
            studentId = getString("studentId").orEmpty(),
            boughtCount = (getLong("boughtCount")?.toInt() ?: 0).coerceAtLeast(0),
            soldCount = (getLong("soldCount")?.toInt() ?: 0).coerceAtLeast(0),
            averageRating = getDouble("averageRating") ?: 0.0,
            ratingCount = (getLong("ratingCount")?.toInt() ?: 0).coerceAtLeast(0),
            walletBalance = getDouble("walletBalance") ?: 0.0,
            paymentMethods = toPaymentMethods()
        )
    }

    private fun DocumentSnapshot.toPaymentMethods(): List<SellerPaymentMethod> {
        val rawMethods = get(PAYMENT_METHODS_FIELD) as? List<*> ?: return emptyList()
        return rawMethods.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            SellerPaymentMethod(
                id = map["id"] as? String ?: "",
                type = SellerPaymentMethodType.fromRaw(map["type"] as? String),
                label = map["label"] as? String ?: "",
                accountName = map["accountName"] as? String ?: "",
                accountNumber = map["accountNumber"] as? String ?: "",
                bankCode = map["bankCode"] as? String ?: "",
                bankName = map["bankName"] as? String ?: "",
                phoneNumber = map["phoneNumber"] as? String ?: "",
                note = map["note"] as? String ?: "",
                isDefault = map["isDefault"] as? Boolean ?: false
            ).sanitized()
        }.normalizeDefaults()
    }

    private fun QuerySnapshot.toAddresses(): List<UserAddress> {
        return documents.map { doc ->
            UserAddress(
                id = doc.id,
                recipientName = doc.getString("recipientName").orEmpty(),
                phoneNumber = doc.getString("phoneNumber").orEmpty(),
                addressLine = doc.getString("addressLine").orEmpty(),
                isDefault = doc.getBoolean("isDefault") ?: false
            )
        }.sortedWith(compareByDescending<UserAddress> { it.isDefault }.thenBy { it.recipientName })
    }

    private fun FirebaseUser.authAvatarUrl(): String {
        return photoUrl?.toString().orEmpty()
    }

    private fun FirebaseUser?.requiresEmailVerification(): Boolean {
        if (this == null) return false
        val signedInWithPassword = providerData.any { provider ->
            provider.providerId == EmailAuthProvider.PROVIDER_ID
        }
        return signedInWithPassword && !isEmailVerified
    }

    private fun SellerPaymentMethod.sanitized(): SellerPaymentMethod {
        return copy(
            id = id.trim(),
            label = label.trim(),
            accountName = accountName.trim(),
            accountNumber = accountNumber.trim(),
            bankCode = bankCode.trim(),
            bankName = bankName.trim(),
            phoneNumber = phoneNumber.trim(),
            note = note.trim()
        )
    }

    private fun List<SellerPaymentMethod>.normalizeDefaults(): List<SellerPaymentMethod> {
        if (isEmpty()) return emptyList()
        val defaultId = firstOrNull { it.isDefault }?.id ?: first().id
        return map { it.copy(isDefault = it.id == defaultId) }
            .sortedWith(compareByDescending<SellerPaymentMethod> { it.isDefault }.thenBy { it.displayTitle })
    }

    private fun List<SellerPaymentMethod>.toFirestoreList(): List<Map<String, Any>> {
        return map { method ->
            mapOf(
                "id" to method.id,
                "type" to method.type.storageValue,
                "label" to method.label,
                "accountName" to method.accountName,
                "accountNumber" to method.accountNumber,
                "bankCode" to method.bankCode,
                "bankName" to method.bankName,
                "phoneNumber" to method.phoneNumber,
                "note" to method.note,
                "isDefault" to method.isDefault
            )
        }
    }

}

private fun Response<*>.errorMessage(): String? {
    val errorBody = runCatching { errorBody()?.string() }.getOrNull().orEmpty()
    return when {
        errorBody.isBlank() -> null
        "\"error\"" in errorBody ->
            Regex("\"error\"\\s*:\\s*\"([^\"]+)\"")
                .find(errorBody)
                ?.groupValues
                ?.getOrNull(1)
        else -> errorBody
    }?.trim()
}

private fun Response<*>.rawErrorBodyForLog(): String {
    return runCatching { errorBody()?.string() }.getOrNull().orEmpty().ifBlank { "<empty>" }
}

private fun Response<*>.otpDevErrorMessage(): String? {
    val errorBody = runCatching { errorBody()?.string() }.getOrNull().orEmpty()
    if (errorBody.isBlank()) return null

    val parsed = runCatching {
        Gson().fromJson(errorBody, OtpDevErrorResponseDto::class.java)
    }.getOrNull()
    val firstError = parsed?.errors?.firstOrNull()
    val msg = firstError?.message?.trim().orEmpty()
    val code = firstError?.code?.trim().orEmpty()

    return when {
        msg.isBlank() -> errorBody.trim()
        code.isBlank() -> msg
        else -> "$msg (code: $code)"
    }
}

private fun String?.maskForLog(): String {
    val source = this.orEmpty().trim()
    if (source.isBlank()) return "<empty>"
    if (source.length <= 6) return "***"
    return "${source.take(3)}***${source.takeLast(2)}"
}

private fun String.toOtpDevPhone(): String {
    return trim()
        .replace(" ", "")
        .replace("-", "")
        .replace("(", "")
        .replace(")", "")
        .removePrefix("+")
}

private fun String.toE164Phone(): String {
    val digits = trim().removePrefix("+")
    return if (digits.isBlank()) "" else "+$digits"
}

private fun String.toStoredPhoneNumber(): String {
    return toOtpDevPhone()
}

private fun String.toPhoneNumberDocumentId(): String {
    return "e164_${toOtpDevPhone()}"
}

private fun String.maskSecretForLog(): String {
    val source = trim()
    if (source.isBlank()) return "<empty>"
    if (source.length <= 8) return "***"
    return "${source.take(4)}***${source.takeLast(4)}"
}

private class PhoneNumberAlreadyUsedException : Exception("Số điện thoại này đã được sử dụng")
