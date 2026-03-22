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

    private suspend fun saveToken(
        userId: String,
        token: String
    ) {
        if (token.isBlank()) return
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

    private companion object {
        const val TAG = "FcmTokenManager"
        const val USERS_COLLECTION = "users"
        const val FCM_TOKEN_FIELD = "fcmToken"
        const val FCM_TOKENS_FIELD = "fcmTokens"
    }
}
