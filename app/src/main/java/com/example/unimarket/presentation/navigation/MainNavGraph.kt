package com.example.unimarket.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.unimarket.presentation.auth.AuthViewModel
import com.example.unimarket.presentation.cart.CartScreen
import com.example.unimarket.presentation.explore.ExploreScreen
import com.example.unimarket.presentation.messages.MessagesScreen
import com.example.unimarket.presentation.profile.ProfileScreen
import com.example.unimarket.presentation.sell.SellScreen

@Composable
fun MainNavGraph(navController: NavHostController, rootNavController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Explore.route,
        route = "main_tabs_graph"
    ) {
        composable(Screen.Explore.route) { ExploreScreen() }
        composable(Screen.Sell.route) {
            SellScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Messages.route) { MessagesScreen() }
        composable(Screen.Profile.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            ProfileScreen(
                onLogoutClick = {
                    authViewModel.logout()
                    rootNavController.navigate(Screen.AuthGraph.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Cart.route) {
            CartScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.MyListings.route) {
            com.example.unimarket.presentation.mylistings.MyListingsScreen(
                navController = navController
            )
        }
    }
}
