package com.example.unimarket.presentation.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.example.unimarket.presentation.auth.AuthViewModel
import com.example.unimarket.presentation.cart.CartScreen
import com.example.unimarket.presentation.checkout.CheckoutScreen
import com.example.unimarket.presentation.checkout.QrTransferScreen
import com.example.unimarket.presentation.checkout.PaymentSuccessScreen
import com.example.unimarket.presentation.explore.ExploreScreen
import com.example.unimarket.presentation.messages.ChatDetailScreen
import com.example.unimarket.presentation.messages.MessagesScreen
import com.example.unimarket.presentation.mypurchases.MyPurchasesScreen
import com.example.unimarket.presentation.ordertracking.OrderTrackingScreen
import com.example.unimarket.presentation.notifications.NotificationsScreen
import com.example.unimarket.presentation.profile.MyAddressesScreen
import com.example.unimarket.presentation.profile.PaymentMethodEditorScreen
import com.example.unimarket.presentation.profile.PaymentMethodsScreen
import com.example.unimarket.presentation.profile.ProfileScreen
import com.example.unimarket.presentation.sell.SellScreen
import com.example.unimarket.presentation.statistics.StatisticsScreen
import com.example.unimarket.presentation.sellerorders.SellerOrdersScreen
import com.example.unimarket.presentation.sellerprofile.SellerProfileScreen
import com.example.unimarket.presentation.sellerreviews.SellerReviewsScreen
import com.example.unimarket.presentation.wallet.WalletScreen
import com.example.unimarket.presentation.wallet.WalletTransactionMode
import com.example.unimarket.presentation.wallet.WalletTopUpScreen

private const val CHECKOUT_SELECTED_ADDRESS_ID_KEY = "checkout_selected_address_id"
private const val CHECKOUT_ADDRESS_LOG_TAG = "CheckoutAddress"

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
                },
                onNotificationsClick = {
                    navController.navigate(Screen.Notifications.route)
                }
            ) 
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() }
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
                onMyAddressesClick = {
                    navController.navigate(Screen.MyAddresses.route)
                },
                onMyPurchasesClick = {
                    navController.navigate(Screen.MyPurchases.route)
                },
                onWalletClick = {
                    navController.navigate(Screen.Wallet.route)
                },
                onPaymentMethodsClick = {
                    navController.navigate(Screen.PaymentMethods.route)
                },
                onSellerOrdersClick = {
                    navController.navigate(Screen.SellerOrders.route)
                },
                onStatisticsClick = {
                    navController.navigate(Screen.Statistics.route)
                }
            )
        }
        composable(
            route = Screen.MyAddresses.route + "?${Screen.MyAddresses.SELECT_MODE_ARG}={${Screen.MyAddresses.SELECT_MODE_ARG}}",
            arguments = listOf(
                androidx.navigation.navArgument(Screen.MyAddresses.SELECT_MODE_ARG) {
                    type = androidx.navigation.NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val selectionMode = backStackEntry.arguments
                ?.getBoolean(Screen.MyAddresses.SELECT_MODE_ARG)
                ?: false
            MyAddressesScreen(
                onBackClick = { navController.popBackStack() },
                selectionMode = selectionMode,
                onAddressSelected = { address ->
                    Log.d(
                        CHECKOUT_ADDRESS_LOG_TAG,
                        "MyAddresses selected address: id=${address.id}, previousRoute=${navController.previousBackStackEntry?.destination?.route}"
                    )
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(CHECKOUT_SELECTED_ADDRESS_ID_KEY, address.id)
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.PaymentMethods.route) {
            PaymentMethodsScreen(
                onBackClick = { navController.popBackStack() },
                onAddClick = {
                    navController.navigate(Screen.PaymentMethodEditor.route)
                },
                onEditClick = { methodId ->
                    navController.navigate(Screen.PaymentMethodEditor.route + "?methodId=${Uri.encode(methodId)}")
                }
            )
        }
        composable(
            route = Screen.PaymentMethodEditor.route + "?methodId={methodId}",
            arguments = listOf(
                androidx.navigation.navArgument("methodId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val parentEntry = remember(navController) {
                navController.getBackStackEntry(Screen.PaymentMethods.route)
            }
            val paymentMethodsViewModel: com.example.unimarket.presentation.profile.PaymentMethodsViewModel =
                hiltViewModel(parentEntry)
            PaymentMethodEditorScreen(
                methodId = backStackEntry.arguments?.getString("methodId"),
                viewModel = paymentMethodsViewModel,
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Wallet.route + "?topUpAmount={topUpAmount}&topUpAt={topUpAt}&withdrawAmount={withdrawAmount}&withdrawAt={withdrawAt}",
            arguments = listOf(
                androidx.navigation.navArgument("topUpAmount") {
                    type = androidx.navigation.NavType.LongType
                    defaultValue = 0L
                },
                androidx.navigation.navArgument("topUpAt") {
                    type = androidx.navigation.NavType.LongType
                    defaultValue = 0L
                },
                androidx.navigation.navArgument("withdrawAmount") {
                    type = androidx.navigation.NavType.LongType
                    defaultValue = 0L
                },
                androidx.navigation.navArgument("withdrawAt") {
                    type = androidx.navigation.NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val topUpAmount = backStackEntry.arguments?.getLong("topUpAmount") ?: 0L
            val topUpAt = backStackEntry.arguments?.getLong("topUpAt") ?: 0L
            val withdrawAmount = backStackEntry.arguments?.getLong("withdrawAmount") ?: 0L
            val withdrawAt = backStackEntry.arguments?.getLong("withdrawAt") ?: 0L
            WalletScreen(
                onBackClick = { navController.popBackStack() },
                topUpAmount = topUpAmount,
                topUpAt = topUpAt,
                withdrawAmount = withdrawAmount,
                withdrawAt = withdrawAt,
                onTopUpClick = { balance ->
                    navController.navigate(
                        Screen.WalletTopUp.route + "?balance=$balance&mode=${WalletTransactionMode.TOP_UP.routeValue}"
                    )
                },
                onWithdrawClick = { balance ->
                    navController.navigate(
                        Screen.WalletTopUp.route + "?balance=$balance&mode=${WalletTransactionMode.WITHDRAW.routeValue}"
                    )
                }
            )
        }
        composable(
            route = Screen.WalletTopUp.route + "?balance={balance}&mode={mode}",
            arguments = listOf(
                androidx.navigation.navArgument("balance") {
                    type = androidx.navigation.NavType.FloatType
                    defaultValue = 0f
                },
                androidx.navigation.navArgument("mode") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = WalletTransactionMode.TOP_UP.routeValue
                }
            )
        ) { backStackEntry ->
            val balance = backStackEntry.arguments?.getFloat("balance")?.toDouble() ?: 0.0
            val mode = WalletTransactionMode.fromRoute(backStackEntry.arguments?.getString("mode"))
            WalletTopUpScreen(
                currentBalance = balance,
                mode = mode,
                onBackClick = { navController.popBackStack() },
                onTopUpClick = { topUpAmount ->
                    navController.navigate(
                        Screen.QrTransfer.route + "?topUpAmount=$topUpAmount"
                    )
                },
                onWithdrawSubmitted = { amount ->
                    navController.navigate(
                        Screen.Wallet.route +
                            "?topUpAmount=0&topUpAt=0&withdrawAmount=$amount&withdrawAt=${System.currentTimeMillis()}"
                    ) {
                        popUpTo(Screen.WalletTopUp.route + "?balance={balance}&mode={mode}") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onOpenPaymentMethods = {
                    navController.navigate(Screen.PaymentMethods.route)
                }
            )
        }
        composable(Screen.MyPurchases.route) {
            MyPurchasesScreen(
                onBackClick = { navController.popBackStack() },
                onPendingPaymentClick = { orderId ->
                    navController.navigate(
                        Screen.QrTransfer.route + "?orderIds=" + Uri.encode(orderId) + "&topUpAmount=0"
                    )
                },
                onTrackOrderClick = { orderId ->
                    navController.navigate(Screen.OrderTracking.route + "/$orderId")
                },
                onConversationOpen = { conversationId ->
                    navController.navigate(Screen.ChatDetail.route + "/$conversationId")
                }
            )
        }
        composable(
            route = Screen.OrderTracking.route + "/{orderId}",
            arguments = listOf(androidx.navigation.navArgument("orderId") {
                type = androidx.navigation.NavType.StringType
            })
        ) {
            OrderTrackingScreen(
                onBackClick = { navController.popBackStack() },
                onConversationOpen = { conversationId ->
                    navController.navigate(Screen.ChatDetail.route + "/$conversationId")
                }
            )
        }
        composable(Screen.SellerOrders.route) {
            SellerOrdersScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Statistics.route) {
            StatisticsScreen(
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
                },
                onViewReviewsClick = { sellerId ->
                    navController.navigate(Screen.SellerReviews.route + "/$sellerId")
                }
            )
        }
        composable(
            route = Screen.SellerReviews.route + "/{sellerId}",
            arguments = listOf(
                androidx.navigation.navArgument("sellerId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) {
            SellerReviewsScreen(
                onBackClick = { navController.popBackStack() }
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
            route = Screen.QrTransfer.route + "?orderIds={orderIds}&topUpAmount={topUpAmount}",
            arguments = listOf(
                androidx.navigation.navArgument("orderIds") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                },
                androidx.navigation.navArgument("topUpAmount") {
                    type = androidx.navigation.NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val orderIds = backStackEntry.arguments
                ?.getString("orderIds")
                .orEmpty()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val topUpAmount = backStackEntry.arguments?.getLong("topUpAmount") ?: 0L
            QrTransferScreen(
                orderIds = orderIds,
                topUpAmount = topUpAmount,
                onBackClick = { navController.popBackStack() },
                onTransferCompleted = { order ->
                    navController.navigate(
                        Screen.PaymentSuccess.route +
                                "?orderId=${order.id}" +
                                "&productName=${Uri.encode(order.productName)}" +
                                "&quantity=${order.quantity}" +
                                "&totalAmount=${order.totalAmount}"
                    ) {
                        popUpTo(Screen.QrTransfer.route + "?orderIds={orderIds}&topUpAmount={topUpAmount}") { inclusive = true }
                    }
                },
                onTopUpCompleted = {
                    navController.navigate(
                        Screen.Wallet.route +
                            "?topUpAmount=$topUpAmount" +
                            "&topUpAt=${System.currentTimeMillis()}"
                    ) {
                        popUpTo(Screen.Wallet.route + "?topUpAmount={topUpAmount}&topUpAt={topUpAt}") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        dialog(
            route = Screen.PaymentSuccess.route + "?orderId={orderId}&productName={productName}&quantity={quantity}&totalAmount={totalAmount}",
            arguments = listOf(
                androidx.navigation.navArgument("orderId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("productName") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("quantity") { type = androidx.navigation.NavType.IntType },
                androidx.navigation.navArgument("totalAmount") { type = androidx.navigation.NavType.FloatType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val productName = backStackEntry.arguments?.getString("productName").orEmpty()
            val quantity = backStackEntry.arguments?.getInt("quantity") ?: 1
            val totalAmount = backStackEntry.arguments?.getFloat("totalAmount")?.toDouble() ?: 0.0

            PaymentSuccessScreen(
                orderId = orderId,
                productName = productName,
                quantity = quantity,
                totalAmount = totalAmount,
                onTrackOrderClick = {
                    navController.navigate(Screen.MyPurchases.route) {
                        popUpTo(Screen.PaymentSuccess.route + "?orderId={orderId}&productName={productName}&quantity={quantity}&totalAmount={totalAmount}") { inclusive = true }
                    }
                },
                onBackToMarketplaceClick = {
                    navController.navigate(Screen.Explore.route) {
                        popUpTo(Screen.Explore.route) { inclusive = true }
                    }
                }
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
            val selectedAddressId by backStackEntry.savedStateHandle
                .getStateFlow(CHECKOUT_SELECTED_ADDRESS_ID_KEY, "")
                .collectAsStateWithLifecycle()
            Log.d(
                CHECKOUT_ADDRESS_LOG_TAG,
                "BuyNow checkout savedState selectedAddressId=$selectedAddressId"
            )
            CheckoutScreen(
                productId = productId,
                quantity = quantity,
                selectedAddressIdFromPicker = selectedAddressId.ifBlank { null },
                onAddressSelectionConsumed = {
                    Log.d(CHECKOUT_ADDRESS_LOG_TAG, "BuyNow checkout consumed selectedAddressId=$selectedAddressId")
                    backStackEntry.savedStateHandle[CHECKOUT_SELECTED_ADDRESS_ID_KEY] = ""
                },
                onBackClick = { navController.popBackStack() },
                onChangeAddressClick = {
                    navController.navigate(Screen.MyAddresses.route(selectMode = true))
                },
                onTransferOrdersReady = { orderIds ->
                    navController.navigate(
                        Screen.QrTransfer.route + "?orderIds=" + Uri.encode(orderIds.joinToString(",")) + "&topUpAmount=0"
                    )
                },
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
            val selectedAddressId by backStackEntry.savedStateHandle
                .getStateFlow(CHECKOUT_SELECTED_ADDRESS_ID_KEY, "")
                .collectAsStateWithLifecycle()
            Log.d(
                CHECKOUT_ADDRESS_LOG_TAG,
                "Cart checkout savedState selectedAddressId=$selectedAddressId"
            )

            CheckoutScreen(
                cartItemIds = cartItemIds,
                selectedAddressIdFromPicker = selectedAddressId.ifBlank { null },
                onAddressSelectionConsumed = {
                    Log.d(CHECKOUT_ADDRESS_LOG_TAG, "Cart checkout consumed selectedAddressId=$selectedAddressId")
                    backStackEntry.savedStateHandle[CHECKOUT_SELECTED_ADDRESS_ID_KEY] = ""
                },
                onBackClick = { navController.popBackStack() },
                onChangeAddressClick = {
                    navController.navigate(Screen.MyAddresses.route(selectMode = true))
                },
                onTransferOrdersReady = { orderIds ->
                    navController.navigate(
                        Screen.QrTransfer.route + "?orderIds=" + Uri.encode(orderIds.joinToString(",")) + "&topUpAmount=0"
                    )
                },
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
