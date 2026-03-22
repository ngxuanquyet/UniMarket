package com.example.unimarket.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.unimarket.MainActivity
import com.example.unimarket.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChatFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank()) return
        android.util.Log.d(TAG, "onNewToken tokenPrefix=${token.take(12)}")
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { fcmTokenManager.saveCurrentUserToken(token) }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        android.util.Log.d(
            TAG,
            "onMessageReceived notification=${remoteMessage.notification != null} data=${remoteMessage.data}"
        )

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data[KEY_TITLE]
            ?: DEFAULT_TITLE
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data[KEY_BODY]
            ?: DEFAULT_BODY
        val conversationId = remoteMessage.data[KEY_CONVERSATION_ID].orEmpty()

        showNotification(
            title = title,
            body = body,
            conversationId = conversationId
        )
    }

    private fun showNotification(
        title: String,
        body: String,
        conversationId: String
    ) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_CONVERSATION_ID, conversationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHAT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(conversationId.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHAT_CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHAT_CHANNEL_ID,
            CHAT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHAT_CHANNEL_DESCRIPTION
        }
        notificationManager.createNotificationChannel(channel)
    }

    private companion object {
        const val TAG = "ChatFcmService"
        const val CHAT_CHANNEL_ID = "chat_messages"
        const val CHAT_CHANNEL_NAME = "Chat messages"
        const val CHAT_CHANNEL_DESCRIPTION = "Notifications for new chat messages"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_CONVERSATION_ID = MainActivity.EXTRA_CONVERSATION_ID
        const val DEFAULT_TITLE = "Tin nhan moi"
        const val DEFAULT_BODY = "Ban co tin nhan moi"
    }
}
