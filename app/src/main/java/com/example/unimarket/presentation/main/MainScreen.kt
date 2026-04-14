package com.example.unimarket.presentation.main

import com.example.unimarket.presentation.theme.*

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.unimarket.R
import com.example.unimarket.presentation.navigation.MainNavGraph
import com.example.unimarket.presentation.navigation.Screen
import com.example.unimarket.presentation.navigation.SessionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    rootNavController: NavHostController,
    pendingConversationId: String? = null,
    onConversationIntentConsumed: () -> Unit = {}
) {
    val tabNavController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (showExitDialog) {
            showExitDialog = false
            return@BackHandler
        }
        val hasBackStackInTabs = tabNavController.popBackStack()
        if (!hasBackStackInTabs) {
            showExitDialog = true
        }
    }

    LaunchedEffect(pendingConversationId) {
        val conversationId = pendingConversationId ?: return@LaunchedEffect
        tabNavController.navigate(Screen.ChatDetail.route + "/$conversationId") {
            launchSingleTop = true
        }
        onConversationIntentConsumed()
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = tabNavController,
                unreadMessageCount = sessionState.unreadMessageCount
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .blur(if (showExitDialog) 8.dp else 0.dp)
        ) {
            MainNavGraph(navController = tabNavController, rootNavController = rootNavController)
        }
    }

    if (showExitDialog) {
        ExitAppDialog(
            onDismiss = { showExitDialog = false },
            onExit = { context.findActivity()?.finish() }
        )
    }
}


@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    unreadMessageCount: Int
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val compactScreen = screenWidthDp < 380
    val iconSize = if (compactScreen) 20.dp else 24.dp
    val labelFontSize = if (compactScreen) 10.sp else 11.sp
    val coroutineScope = rememberCoroutineScope()
    var isAnimatingToSell by remember { mutableStateOf(false) }
    val items = listOf(
        BottomNavItem(R.string.bottom_nav_explore, route = Screen.Explore.route, icon = Icons.Default.Explore),
        BottomNavItem(R.string.bottom_nav_sell, route = Screen.Sell.route, icon = Icons.Default.AddCircleOutline),
        BottomNavItem(
            R.string.bottom_nav_my_listings,
            shortLabelRes = R.string.bottom_nav_my_listings_short,
            route = Screen.MyListings.route,
            icon = Icons.AutoMirrored.Filled.ListAlt
        ),
        BottomNavItem(R.string.bottom_nav_messages, route = Screen.Messages.route, icon = Icons.Default.ChatBubbleOutline),
        BottomNavItem(R.string.bottom_nav_profile, route = Screen.Profile.route, icon = Icons.Default.PersonOutline)
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val normalizedRoute = currentRoute
        ?.substringBefore("?")
        ?.substringBefore("/")

    val showBottomBar = normalizedRoute in listOf(
        Screen.Explore.route,
        Screen.MyListings.route,
        Screen.Messages.route,
        Screen.Profile.route
    ) && !isAnimatingToSell

    AnimatedVisibility(
        visible = showBottomBar,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 8.dp
        ) {
            items.forEach { item ->
                val label = if (compactScreen && item.shortLabelRes != null) {
                    stringResource(item.shortLabelRes)
                } else {
                    stringResource(item.labelRes)
                }
                NavigationBarItem(
                    selected = normalizedRoute == item.route,
                    onClick = {
                        if (item.route == Screen.Sell.route && normalizedRoute != Screen.Sell.route) {
                            if (isAnimatingToSell) return@NavigationBarItem
                            isAnimatingToSell = true
                            coroutineScope.launch {
                                delay(180)
                                navController.navigate(item.route) {
                                    navController.graph.startDestinationRoute?.let { route ->
                                        popUpTo(route) { saveState = true }
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                isAnimatingToSell = false
                            }
                        } else {
                            navController.navigate(item.route) {
                                navController.graph.startDestinationRoute?.let { route ->
                                    popUpTo(route) { saveState = true }
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        if (item.route == Screen.Messages.route && unreadMessageCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(
                                            text = if (unreadMessageCount > 99) "99+" else unreadMessageCount.toString()
                                        )
                                    }
                                }
                            ) {
                                Icon(item.icon, contentDescription = label, modifier = Modifier.size(iconSize))
                            }
                        } else {
                            Icon(item.icon, contentDescription = label, modifier = Modifier.size(iconSize))
                        }
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = labelFontSize),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AppBlue,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = AppBlue
                    )
                )
            }
        }
    }
}

@Composable
private fun ExitAppDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = LightBlueSelection,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = AppBlue,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.exit_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextDarkBlack,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.exit_dialog_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.exit_dialog_stay),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = onExit) {
                        Text(
                            text = stringResource(R.string.exit_dialog_exit),
                            color = TextGray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
