package com.example.unimarket.presentation.main

import com.example.unimarket.presentation.theme.*

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.unimarket.presentation.navigation.MainNavGraph
import com.example.unimarket.presentation.navigation.Screen
import com.example.unimarket.presentation.navigation.SessionViewModel
import com.example.unimarket.presentation.theme.PrimaryYellowDark

@Composable
fun MainScreen(rootNavController: NavHostController) {
    val tabNavController = rememberNavController()
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()

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
    val items = listOf(
        BottomNavItem("Explore", Screen.Explore.route, Icons.Default.Explore),
        BottomNavItem("Sell", Screen.Sell.route, Icons.Default.AddCircleOutline),
        BottomNavItem("My Listings", Screen.MyListings.route, Icons.AutoMirrored.Filled.ListAlt),
        BottomNavItem("Messages", Screen.Messages.route, Icons.Default.ChatBubbleOutline),
        BottomNavItem("Profile", Screen.Profile.route, Icons.Default.PersonOutline)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Explore.route,
        Screen.Sell.route,
        Screen.MyListings.route,
        Screen.Messages.route,
        Screen.Profile.route
    )

    if (showBottomBar) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 8.dp
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            navController.graph.startDestinationRoute?.let { route ->
                                popUpTo(route) { saveState = true }
                            }
                            launchSingleTop = true
                            restoreState = true
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
                                Icon(item.icon, contentDescription = item.title)
                            }
                        } else {
                            Icon(item.icon, contentDescription = item.title)
                        }
                    },
                    label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
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
