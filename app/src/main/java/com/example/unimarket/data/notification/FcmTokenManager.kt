package com.example.unimarket.data.notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun syncCurrentUserToken() {
        val currentUser = auth.currentUser ?: return
        val token = FirebaseMessaging.getInstance().token.await()
        saveToken(userId = currentUser.uid, token = token)
    }

    suspend fun saveCurrentUserToken(token: String) {
        val currentUser = auth.currentUser ?: return
        saveToken(userId = currentUser.uid, token = token)
    }

    suspend fun clearCurrentUserToken() {
        val currentUser = auth.currentUser ?: return
        val token = FirebaseMessaging.getInstance().token.await()
        if (token.isBlank()) return
        removeTokenFromUser(userId = currentUser.uid, token = token, removePrimaryIfMatches = true)
    }

    private suspend fun saveToken(
        userId: String,
        token: String
    ) {
        if (token.isBlank()) return
        detachTokenFromOtherUsers(currentUserId = userId, token = token)
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .set(
                mapOf(
                    FCM_TOKEN_FIELD to token,
                    FCM_TOKENS_FIELD to FieldValue.arrayUnion(token)
                ),
                SetOptions.merge()
            )
            .await()
        Log.d(TAG, "Saved FCM token for userId=$userId tokenPrefix=${token.take(12)}")
    }

    private suspend fun detachTokenFromOtherUsers(
        currentUserId: String,
        token: String
    ) {
        val usersRef = firestore.collection(USERS_COLLECTION)
        val matchedUsers = linkedMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()

        usersRef.whereArrayContains(FCM_TOKENS_FIELD, token).get().await().documents.forEach { document ->
            if (document.id != currentUserId) {
                matchedUsers[document.id] = document
            }
        }

        usersRef.whereEqualTo(FCM_TOKEN_FIELD, token).get().await().documents.forEach { document ->
            if (document.id != currentUserId) {
                matchedUsers[document.id] = document
            }
        }

        if (matchedUsers.isEmpty()) return

        val batch = firestore.batch()
        matchedUsers.values.forEach { document ->
            val updates = mutableMapOf<String, Any>(
                FCM_TOKENS_FIELD to FieldValue.arrayRemove(token)
            )
            if (document.getString(FCM_TOKEN_FIELD) == token) {
                updates[FCM_TOKEN_FIELD] = FieldValue.delete()
            }
            batch.set(document.reference, updates, SetOptions.merge())
        }
        batch.commit().await()
        Log.d(
            TAG,
            "Detached token from other users count=${matchedUsers.size} tokenPrefix=${token.take(12)}"
        )
    }

    private suspend fun removeTokenFromUser(
        userId: String,
        token: String,
        removePrimaryIfMatches: Boolean
    ) {
        val documentRef = firestore.collection(USERS_COLLECTION).document(userId)
        val snapshot = documentRef.get().await()
        val updates = mutableMapOf<String, Any>(
            FCM_TOKENS_FIELD to FieldValue.arrayRemove(token)
        )
        if (removePrimaryIfMatches && snapshot.getString(FCM_TOKEN_FIELD) == token) {
            updates[FCM_TOKEN_FIELD] = FieldValue.delete()
        }
        documentRef.set(updates, SetOptions.merge()).await()
        Log.d(TAG, "Removed FCM token for userId=$userId tokenPrefix=${token.take(12)}")
    }

    private companion object {
        const val TAG = "FcmTokenManager"
        const val USERS_COLLECTION = "users"
        const val FCM_TOKEN_FIELD = "fcmToken"
        const val FCM_TOKENS_FIELD = "fcmTokens"
    }
}
