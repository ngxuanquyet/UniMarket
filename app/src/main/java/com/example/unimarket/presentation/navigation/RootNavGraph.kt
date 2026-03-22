package com.example.unimarket.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navigation
import com.example.unimarket.presentation.auth.AuthViewModel
import com.example.unimarket.presentation.auth.LoginScreen
import com.example.unimarket.presentation.auth.SignUpScreen
import com.example.unimarket.presentation.auth.state.AuthState
import com.example.unimarket.presentation.main.MainScreen

@Composable
fun RootNavGraph(
    navController: NavHostController,
    pendingConversationId: String? = null,
    onConversationIntentConsumed: () -> Unit = {}
) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val sessionState = sessionViewModel.uiState.collectAsStateWithLifecycle()
    val currentBackStackEntry = navController.currentBackStackEntryAsState()

    val startDestination = if (sessionState.value.isAuthenticated) {
        Screen.MainGraph.route
    } else {
        Screen.AuthGraph.route
    }

    LaunchedEffect(sessionState.value.isAuthenticated, currentBackStackEntry.value) {
        val currentDestination = currentBackStackEntry.value?.destination ?: return@LaunchedEffect
        val targetRoute = if (sessionState.value.isAuthenticated) {
            Screen.MainGraph.route
        } else {
            Screen.AuthGraph.route
        }

        val isAlreadyOnTarget = currentDestination.hierarchy.any { it.route == targetRoute }
        if (!isAlreadyOnTarget) {
            navController.navigate(targetRoute) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        route = "root_graph"
    ) {
        navigation(
            startDestination = Screen.Login.route,
            route = Screen.AuthGraph.route
        ) {
            composable(Screen.Login.route) { 
                val viewModel: AuthViewModel = hiltViewModel()
                val authState = viewModel.authState.collectAsStateWithLifecycle()

                LoginScreen(
                    onLoginClick = { email, password -> viewModel.login(email, password) },
                    onGoogleLoginClick = { idToken -> viewModel.loginWithGoogle(idToken) },
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    isLoading = authState.value is AuthState.Loading,
                    errorMessage = (authState.value as? AuthState.Error)?.message
                ) 
            }
            composable(Screen.SignUp.route) { 
                val viewModel: AuthViewModel = hiltViewModel()
                val authState = viewModel.authState.collectAsStateWithLifecycle()

                SignUpScreen(
                    onSignUpClick = { name, email, password ->
                        viewModel.signUp(name, email, password)
                    },
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToLogin = { navController.popBackStack() },
                    isLoading = authState.value is AuthState.Loading,
                    errorMessage = (authState.value as? AuthState.Error)?.message
                ) 
            }
        }

        composable(route = Screen.MainGraph.route) {
            MainScreen(
                rootNavController = navController,
                pendingConversationId = pendingConversationId,
                onConversationIntentConsumed = onConversationIntentConsumed
            )
        }
    }
}
