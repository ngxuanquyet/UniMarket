package com.example.unimarket.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.unimarket.presentation.auth.AuthViewModel
import com.example.unimarket.presentation.auth.LoginScreen
import com.example.unimarket.presentation.auth.SignUpScreen
import com.example.unimarket.presentation.auth.state.AuthState
import com.example.unimarket.presentation.main.MainScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RootNavGraph(navController: NavHostController) {
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Screen.MainGraph.route
    } else {
        Screen.AuthGraph.route
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
                val authState by viewModel.authState.collectAsState()

                LaunchedEffect(authState) {
                    if (authState is AuthState.Success) {
                        navController.navigate(Screen.MainGraph.route) {
                            popUpTo(Screen.AuthGraph.route) { inclusive = true }
                        }
                    }
                }

                LoginScreen(
                    onLoginClick = { email, password -> viewModel.login(email, password) },
                    onGoogleLoginClick = { idToken -> viewModel.loginWithGoogle(idToken) },
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
                    onSignUpClick = { name, email, password ->
                        viewModel.signUp(name, email, password)
                    },
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToLogin = { navController.popBackStack() },
                    isLoading = authState is AuthState.Loading,
                    errorMessage = (authState as? AuthState.Error)?.message
                ) 
            }
        }

        composable(route = Screen.MainGraph.route) {
            MainScreen(rootNavController = navController)
        }
    }
}
