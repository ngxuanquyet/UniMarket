package com.example.unimarket.presentation.main

import com.example.unimarket.presentation.theme.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
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
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()

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
        Box(modifier = Modifier.padding(innerPadding)) {
            MainNavGraph(navController = tabNavController, rootNavController = rootNavController)
        }
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
