package com.example.unimarket

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.unimarket.presentation.navigation.RootNavGraph
import com.example.unimarket.presentation.theme.SecondaryBlue
import com.example.unimarket.presentation.theme.UniMarketTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var pendingConversationId by mutableStateOf<String?>(null)
    private var hasRequestedNotificationPermission = false
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.util.Log.w(TAG, "Notification permission was denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingConversationId = extractConversationId(intent)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            UniMarketTheme {
                var showStartupSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1800)
                    showStartupSplash = false
                }

                if (showStartupSplash) {
                    StartupSplashScreen()
                } else {
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
        if (!isGranted && !hasRequestedNotificationPermission) {
            hasRequestedNotificationPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun extractConversationId(intent: Intent?): String? {
        return intent?.getStringExtra(EXTRA_CONVERSATION_ID)?.takeIf { it.isNotBlank() }
    }

    companion object {
        private const val TAG = "MainActivity"
        const val EXTRA_CONVERSATION_ID = "conversationId"
    }
}

@Composable
private fun StartupSplashScreen() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.loading_splash)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_unimarket),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(124.dp)
                        .clip(RoundedCornerShape(28.dp))
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = SecondaryBlue,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .height(120.dp)
                )
            }
        }
    }
}
