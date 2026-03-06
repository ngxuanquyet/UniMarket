package com.example.unimarket.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.unimarket.presentation.auth.AuthViewModel
import com.example.unimarket.presentation.auth.LoginScreen
import com.example.unimarket.presentation.auth.SignUpScreen
import com.example.unimarket.presentation.auth.state.AuthState
import com.example.unimarket.presentation.cart.CartScreen
import com.example.unimarket.presentation.explore.ExploreScreen
import com.example.unimarket.presentation.home.HomeScreen
import com.example.unimarket.presentation.messages.MessagesScreen
import com.example.unimarket.presentation.profile.ProfileScreen
import com.example.unimarket.presentation.sell.SellScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainNavGraph(navController: NavHostController) {
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Screen.Home.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) { 
            val viewModel: AuthViewModel = hiltViewModel()
            val authState by viewModel.authState.collectAsState()

            LaunchedEffect(authState) {
                if (authState is AuthState.Success) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
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
        composable(Screen.Home.route) { 
            HomeScreen(onCartClick = { navController.navigate(Screen.Cart.route) }) 
        }
        composable(Screen.Explore.route) { ExploreScreen() }
        composable(Screen.Sell.route) { SellScreen() }
        composable(Screen.Messages.route) { MessagesScreen() }
        composable(Screen.Profile.route) { 
            val authViewModel: AuthViewModel = hiltViewModel()
            ProfileScreen(
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            ) 
        }
        composable(Screen.Cart.route) { 
            CartScreen(onBackClick = { navController.popBackStack() }) 
        }
    }
}
