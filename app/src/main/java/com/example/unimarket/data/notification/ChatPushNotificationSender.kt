package com.example.unimarket.data.notification

import android.util.Log
import com.example.unimarket.BuildConfig
import com.example.unimarket.data.api.NotificationApiService
import com.example.unimarket.data.api.model.ChatNotificationRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatPushNotificationSender @Inject constructor(
    private val auth: FirebaseAuth,
    private val notificationApiService: NotificationApiService
) {

    suspend fun notifyNewMessage(
        conversationId: String,
        text: String
    ) {
        if (BuildConfig.NOTIFICATION_SERVER_BASE_URL.isBlank()) return
        val currentUser = auth.currentUser ?: return
        val idToken = currentUser.getIdToken(false).await().token.orEmpty()
        if (idToken.isBlank()) return

        runCatching {
            val response = notificationApiService.notifyNewChatMessage(
                authorization = "Bearer $idToken",
                body = ChatNotificationRequest(
                    conversationId = conversationId,
                    text = text
                )
            )
            Log.d(
                TAG,
                "notifyNewMessage conversationId=$conversationId code=${response.code()} success=${response.isSuccessful}"
            )
            if (!response.isSuccessful) {
                Log.w(TAG, "Backend notification request failed code=${response.code()} message=${response.message()}")
            }
        }.onFailure { error ->
            Log.w(TAG, "Failed to notify backend about new chat message", error)
        }
    }

    private companion object {
        const val TAG = "ChatPushNotifier"
    }
}
