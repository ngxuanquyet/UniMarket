package com.example.unimarket

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.unimarket.presentation.navigation.RootNavGraph
import com.example.unimarket.presentation.theme.UniMarketTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var pendingConversationId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingConversationId = extractConversationId(intent)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            UniMarketTheme {
                val rootNavController = rememberNavController()
                RootNavGraph(
                    navController = rootNavController,
                    pendingConversationId = pendingConversationId,
                    onConversationIntentConsumed = {
                        pendingConversationId = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingConversationId = extractConversationId(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val isGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!isGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun extractConversationId(intent: Intent?): String? {
        return intent?.getStringExtra(EXTRA_CONVERSATION_ID)?.takeIf { it.isNotBlank() }
    }

    companion object {
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        const val EXTRA_CONVERSATION_ID = "conversationId"
    }
}
