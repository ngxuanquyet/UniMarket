package com.example.unimarket.data.repository

import android.net.Uri
import com.example.unimarket.domain.model.UserAddress
import com.example.unimarket.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import androidx.core.net.toUri

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
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
                    photoUri = Uri.parse("https://ui-avatars.com/api/?name=${name.replace(" ", "+")}&background=random")
                }
                user.updateProfile(profileUpdates).await()

                firestore.collection("users").document(user.uid).set(
                    mapOf(
                        "displayName" to name,
                        "email" to email,
                        "studentId" to studentId,
                        "photoUrl" to profileUpdates.photoUri?.toString().orEmpty()
                    )
                ).await()
            }
            
            // Note: studentId could be saved to a Firestore collection here
            // e.g., firestore.collection("users").document(user!!.uid).set(mapOf("studentId" to studentId))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        auth.signOut()
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
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): Any? {
        return auth.currentUser
    }

    override suspend fun updateProfile(name: String?, photoUrl: String?): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                val profileUpdates = userProfileChangeRequest {
                    if (name != null) displayName = name
                    if (photoUrl != null) photoUri = photoUrl.toUri()
                }
                user.updateProfile(profileUpdates).await()
                val updateMap = mutableMapOf<String, Any>()
                if (name != null) updateMap["displayName"] = name
                if (photoUrl != null) updateMap["photoUrl"] = photoUrl
                if (updateMap.isNotEmpty()) {
                    firestore.collection("users").document(user.uid).update(updateMap).await()
                }
                Result.success(Unit)
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
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            val existingAddresses = addressCollection(userId).get().await().toAddresses()
            val shouldBeDefault = address.isDefault || existingAddresses.none { it.isDefault }
            val docRef = if (address.id.isBlank()) {
                addressCollection(userId).document()
            } else {
                addressCollection(userId).document(address.id)
            }
            saveAddress(userId, docRef.id, address.copy(id = docRef.id, isDefault = shouldBeDefault))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserAddress(address: UserAddress): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
            if (address.id.isBlank()) return Result.failure(Exception("Address id is required"))
            saveAddress(userId, address.id, address)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUserAddress(addressId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
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

    private suspend fun saveAddress(userId: String, addressId: String, address: UserAddress): Result<Unit> {
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
        firestore.collection("users").document(userId).collection("addresses")

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
}
