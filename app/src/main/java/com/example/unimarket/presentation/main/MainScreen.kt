package com.example.unimarket.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.unimarket.presentation.auth.AuthState
import com.example.unimarket.presentation.auth.AuthViewModel
import com.example.unimarket.presentation.auth.LoginScreen
import com.example.unimarket.presentation.auth.SignUpScreen
import com.example.unimarket.presentation.cart.CartScreen
import com.example.unimarket.presentation.explore.ExploreScreen
import com.example.unimarket.presentation.home.HomeScreen
import com.example.unimarket.presentation.messages.MessagesScreen
import com.example.unimarket.presentation.navigation.Screen
import com.example.unimarket.presentation.profile.ProfileScreen
import com.example.unimarket.presentation.sell.SellScreen
import com.example.unimarket.presentation.theme.PrimaryYellowDark

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val showBottomBar = currentRoute in listOf(
                Screen.Home.route,
                Screen.Explore.route,
                Screen.Messages.route,
                Screen.Profile.route
            )
            
            if (showBottomBar) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.Sell.route) },
                    shape = CircleShape,
                    containerColor = PrimaryYellowDark,
                    contentColor = Color.White,
                    modifier = Modifier.shadow(8.dp, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Sell",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            MainNavGraph(navController = navController)
        }
    }
}

@Composable
fun MainNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) { 
            val viewModel: AuthViewModel = hiltViewModel()
            val authState by viewModel.authState.collectAsState()

            LaunchedEffect(authState) {
                if (authState is AuthState.Success) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                    viewModel.resetState()
                }
            }

            LoginScreen(
                onLoginClick = { email, password -> viewModel.login(email, password) },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                isLoading = authState is AuthState.Loading,
                errorMessage = (authState as? AuthState.Error)?.message
            ) 
        }
        composable(Screen.SignUp.route) { 
            val viewModel: AuthViewModel = hiltViewModel()
            val authState by viewModel.authState.collectAsState()

            LaunchedEffect(authState) {
                if (authState is AuthState.Success) {
                    navController.popBackStack()
                    viewModel.resetState()
                }
            }

            SignUpScreen(
                onSignUpClick = { name, email, studentId, password ->
                    viewModel.signUp(name, email, studentId, password)
                },
                onNavigateBack = { navController.navigateUp() },
                onNavigateToLogin = { navController.popBackStack() },
                isLoading = authState is AuthState.Loading,
                errorMessage = (authState as? AuthState.Error)?.message
            ) 
        }
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Explore.route) { ExploreScreen() }
        composable(Screen.Sell.route) { SellScreen() }
        composable(Screen.Messages.route) { MessagesScreen() }
        composable(Screen.Profile.route) { ProfileScreen(navController) }
        composable(Screen.Cart.route) { CartScreen(navController) }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Home", Screen.Home.route, Icons.Default.Home),
        BottomNavItem("Explore", Screen.Explore.route, Icons.Default.Search),
        BottomNavItem("", "", Icons.Default.Home), // Empty space for FAB
        BottomNavItem("Messages", Screen.Messages.route, Icons.Default.ChatBubbleOutline),
        BottomNavItem("Profile", Screen.Profile.route, Icons.Default.PersonOutline)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Explore.route,
        Screen.Messages.route,
        Screen.Profile.route
    )

    if (showBottomBar) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 8.dp
        ) {
            items.forEachIndexed { index, item ->
                if (index == 2) {
                    // Empty item for FAB
                    NavigationBarItem(
                        selected = false,
                        onClick = { },
                        icon = { },
                        enabled = false,
                        colors = NavigationBarItemDefaults.colors(
                            disabledIconColor = Color.Transparent,
                            disabledTextColor = Color.Transparent
                        )
                    )
                } else {
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
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryYellowDark,
                            selectedTextColor = PrimaryYellowDark,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}
