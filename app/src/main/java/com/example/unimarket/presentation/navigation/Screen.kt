package com.example.unimarket.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Explore : Screen("explore")
    object Sell : Screen("sell")
    object Messages : Screen("messages")
    object Profile : Screen("profile")
    object Cart : Screen("cart")
    object Login : Screen("login")
    object SignUp : Screen("sign_up")
    object MyListings : Screen("my_listings")
    
    // Nested Graph Routes
    object AuthGraph : Screen("auth_graph")
    object MainGraph : Screen("main_graph")
}
