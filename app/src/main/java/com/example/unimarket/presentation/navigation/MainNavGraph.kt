package com.example.unimarket.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.unimarket.presentation.auth.AuthViewModel
import com.example.unimarket.presentation.cart.CartScreen
import com.example.unimarket.presentation.checkout.CheckoutScreen
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
        composable(Screen.Explore.route) { 
            ExploreScreen(
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.route + "/$productId")
                },
                onCartClick = { 
                    navController.navigate(Screen.Cart.route)
                }
            ) 
        }
        composable(
            route = Screen.Sell.route + "?productId={productId}",
            arguments = listOf(androidx.navigation.navArgument("productId") { 
                type = androidx.navigation.NavType.StringType
                nullable = true
                defaultValue = null 
            })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            SellScreen(
                productId = productId,
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
                onBackClick = { navController.popBackStack() },
                onAddClick = {
                    navController.navigate(Screen.Sell.route)
                },
                onEditClick = { productId ->
                    navController.navigate(Screen.Sell.route + "?productId=$productId")
                }
            )
        }
        composable(
            route = Screen.ProductDetail.route + "/{productId}",
            arguments = listOf(androidx.navigation.navArgument("productId") {
                type = androidx.navigation.NavType.StringType
            })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            com.example.unimarket.presentation.productdetail.ProductDetailScreen(
                productId = productId,
                onBackClick = { navController.popBackStack() },
                onBuyNowClick = { pId ->
                    navController.navigate(Screen.Checkout.route + "/$pId")
                }
            )
        }
        composable(
            route = Screen.Checkout.route + "/{productId}",
            arguments = listOf(androidx.navigation.navArgument("productId") {
                type = androidx.navigation.NavType.StringType
            })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            CheckoutScreen(
                productId = productId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
