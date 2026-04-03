package com.example.unimarket.data.repository

import android.util.Log
import android.net.Uri
import androidx.core.net.toUri
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userSessionLocalDataSource: UserSessionLocalDataSource
) : AuthRepository {

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
        studentId: String,
        password: String
    ): Result<Unit> {
        return try {
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

                syncUserDocument(
                    profile = UserProfile(
                        id = user.uid,
                        displayName = name,
                        email = email,
                        avatarUrl = profileUpdates.photoUri?.toString().orEmpty(),
                        studentId = studentId,
                        boughtCount = 0,
                        soldCount = 0,
                        averageRating = 0.0,
                        ratingCount = 0
                    ),
                    includeBoughtCount = true,
                        includeSoldCount = true
                )

                user.sendEmailVerification().await()
            }

            logout()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
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
                    includeSoldCount = true
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

    override suspend fun getUserAddresses(): Result<List<UserAddress>> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
        return getAddressesByUserId(userId)
    }

    override suspend fun getAddressesByUserId(userId: String): Result<List<UserAddress>> {
        return try {
            val snapshot = addressCollection(userId).get().await()
            Result.success(snapshot.toAddresses())
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

    private suspend fun syncUserDocument(
        profile: UserProfile,
        includeBoughtCount: Boolean = false,
        includeSoldCount: Boolean = false
    ) {
        val updateMap = mutableMapOf<String, Any>(
            "displayName" to profile.displayName,
            "name" to profile.displayName,
            "avatarUrl" to profile.avatarUrl,
            "photoUrl" to FieldValue.delete()
        )

        if (profile.email.isNotBlank()) {
            updateMap["email"] = profile.email
        }
        if (profile.studentId.isNotBlank()) {
            updateMap["studentId"] = profile.studentId
        }
        if (includeBoughtCount) {
            updateMap["boughtCount"] = profile.boughtCount
        }
        if (includeSoldCount) {
            updateMap["soldCount"] = profile.soldCount
        }

        firestore.collection(USERS_COLLECTION)
            .document(profile.id)
            .set(updateMap, SetOptions.merge())
            .await()
    }

    private fun shouldBackfillUserDocument(userDoc: DocumentSnapshot): Boolean {
        val documentData = userDoc.data.orEmpty()
        return !userDoc.exists() ||
                userDoc.getString("name").isNullOrBlank() ||
                userDoc.getString("displayName").isNullOrBlank() ||
                userDoc.getString("email").isNullOrBlank() ||
                userDoc.getString("avatarUrl").isNullOrBlank() ||
                !documentData.containsKey("boughtCount") ||
                !documentData.containsKey("soldCount")
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
            studentId = getString("studentId").orEmpty(),
            boughtCount = getLong("boughtCount")?.toInt() ?: 0,
            soldCount = getLong("soldCount")?.toInt() ?: 0,
            averageRating = getDouble("averageRating") ?: 0.0,
            ratingCount = getLong("ratingCount")?.toInt() ?: 0
        )
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

    private companion object {
        const val USERS_COLLECTION = "users"
        const val ADDRESSES_COLLECTION = "addresses"
    }
}
