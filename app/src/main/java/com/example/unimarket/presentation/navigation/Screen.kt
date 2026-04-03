package com.example.unimarket.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Explore : Screen("explore")
    object Sell : Screen("sell")
    object Messages : Screen("messages")
    object ChatDetail : Screen("chat")
    object SellerProfile : Screen("seller_profile")
    object Profile : Screen("profile")
    object MyAddresses : Screen("my_addresses")
    object Cart : Screen("cart")
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object SignUp : Screen("sign_up")
    object MyListings : Screen("my_listings")
    object MyPurchases : Screen("my_purchases")
    object SellerOrders : Screen("seller_orders")
    object Statistics : Screen("statistics")
    object ProductDetail : Screen("product_detail")
    object Checkout : Screen("checkout")
    object CartCheckout : Screen("cart_checkout")
    
    // Nested Graph Routes
    object AuthGraph : Screen("auth_graph")
    object MainGraph : Screen("main_graph")
}
