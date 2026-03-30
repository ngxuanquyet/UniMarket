package com.example.unimarket.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.unimarket.presentation.auth.AuthViewModel
import com.example.unimarket.presentation.cart.CartScreen
import com.example.unimarket.presentation.checkout.CheckoutScreen
import com.example.unimarket.presentation.explore.ExploreScreen
import com.example.unimarket.presentation.messages.ChatDetailScreen
import com.example.unimarket.presentation.messages.MessagesScreen
import com.example.unimarket.presentation.mypurchases.MyPurchasesScreen
import com.example.unimarket.presentation.profile.ProfileScreen
import com.example.unimarket.presentation.sell.SellScreen
import com.example.unimarket.presentation.sellerorders.SellerOrdersScreen
import com.example.unimarket.presentation.sellerprofile.SellerProfileScreen

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
                onSellerClick = { sellerId ->
                    navController.navigate(Screen.SellerProfile.route + "/$sellerId?productId=")
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
        composable(Screen.Messages.route) {
            MessagesScreen(
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.ChatDetail.route + "/$conversationId")
                }
            )
        }
        composable(Screen.Profile.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            ProfileScreen(
                onLogoutClick = {
                    authViewModel.logout()
                },
                onBack = { navController.popBackStack() },
                onMyPurchasesClick = {
                    navController.navigate(Screen.MyPurchases.route)
                },
                onSellerOrdersClick = {
                    navController.navigate(Screen.SellerOrders.route)
                }
            )
        }
        composable(Screen.MyPurchases.route) {
            MyPurchasesScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.SellerOrders.route) {
            SellerOrdersScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Cart.route) {
            CartScreen(
                onBackClick = { navController.popBackStack() },
                onCheckoutClick = { selectedCartItemIds ->
                    val encodedIds = Uri.encode(selectedCartItemIds.joinToString(","))
                    navController.navigate(Screen.CartCheckout.route + "?cartItemIds=$encodedIds")
                }
            )
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
                onConversationOpen = { conversationId ->
                    navController.navigate(Screen.ChatDetail.route + "/$conversationId")
                },
                onSellerClick = { sellerId, currentProductId ->
                    navController.navigate(
                        Screen.SellerProfile.route + "/$sellerId?productId=$currentProductId"
                    )
                },
                onBuyNowClick = { pId, quantity ->
                    navController.navigate(Screen.Checkout.route + "/$pId?quantity=$quantity")
                }
            )
        }
        composable(
            route = Screen.SellerProfile.route + "/{sellerId}?productId={productId}",
            arguments = listOf(
                androidx.navigation.navArgument("sellerId") {
                    type = androidx.navigation.NavType.StringType
                },
                androidx.navigation.navArgument("productId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            SellerProfileScreen(
                onBackClick = { navController.popBackStack() },
                onConversationOpen = { conversationId ->
                    navController.navigate(Screen.ChatDetail.route + "/$conversationId")
                },
                onProductClick = { selectedProductId ->
                    navController.navigate(Screen.ProductDetail.route + "/$selectedProductId")
                }
            )
        }
        composable(
            route = Screen.ChatDetail.route + "/{conversationId}",
            arguments = listOf(androidx.navigation.navArgument("conversationId") {
                type = androidx.navigation.NavType.StringType
            })
        ) {
            ChatDetailScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Checkout.route + "/{productId}?quantity={quantity}",
            arguments = listOf(androidx.navigation.navArgument("productId") {
                type = androidx.navigation.NavType.StringType
            }, androidx.navigation.navArgument("quantity") {
                type = androidx.navigation.NavType.IntType
                defaultValue = 1
            })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            val quantity = backStackEntry.arguments?.getInt("quantity") ?: 1
            CheckoutScreen(
                productId = productId,
                quantity = quantity,
                onBackClick = { navController.popBackStack() },
                onPurchaseCompleted = {
                    navController.navigate(Screen.Explore.route) {
                        popUpTo(Screen.Explore.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = Screen.CartCheckout.route + "?cartItemIds={cartItemIds}",
            arguments = listOf(androidx.navigation.navArgument("cartItemIds") {
                type = androidx.navigation.NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val cartItemIds = backStackEntry.arguments
                ?.getString("cartItemIds")
                .orEmpty()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            CheckoutScreen(
                cartItemIds = cartItemIds,
                onBackClick = { navController.popBackStack() },
                onPurchaseCompleted = {
                    navController.navigate(Screen.Explore.route) {
                        popUpTo(Screen.Explore.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
