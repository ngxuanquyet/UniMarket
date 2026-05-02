package com.example.unimarket.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Explore : Screen("explore")
    object Sell : Screen("sell")
    object Messages : Screen("messages")
    object ChatDetail : Screen("chat")
    object SellerProfile : Screen("seller_profile")
    object SellerReviews : Screen("seller_reviews")
    object Profile : Screen("profile")
    object PaymentMethods : Screen("payment_methods")
    object PaymentMethodEditor : Screen("payment_method_editor")
    object Wallet : Screen("wallet")
    object WalletTopUp : Screen("wallet_top_up")
    object MyAddresses : Screen("my_addresses") {
        const val SELECT_MODE_ARG = "selectMode"

        fun route(selectMode: Boolean): String {
            return "$route?$SELECT_MODE_ARG=$selectMode"
        }
    }
    object Cart : Screen("cart")
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object SignUp : Screen("sign_up")
    object PhoneSetup : Screen("phone_setup")
    object PhoneVerification : Screen("phone_verification?phone={phone}&flow={flow}") {
        const val BASE_ROUTE = "phone_verification"

        fun route(phone: String, flow: String): String {
            return "$BASE_ROUTE?phone=$phone&flow=$flow"
        }

        fun matches(route: String?): Boolean {
            return route?.substringBefore("?") == BASE_ROUTE
        }
    }
    object MyListings : Screen("my_listings")
    object MyPurchases : Screen("my_purchases")
    object SellerOrders : Screen("seller_orders")
    object Statistics : Screen("statistics")
    object ProductDetail : Screen("product_detail")
    object OrderTracking : Screen("order_tracking")
    object Checkout : Screen("checkout")
    object QrTransfer : Screen("qr_transfer")
    object CartCheckout : Screen("cart_checkout")
    object PaymentSuccess : Screen("payment_success")
    object Notifications : Screen("notifications")
    
    // Nested Graph Routes
    object AuthGraph : Screen("auth_graph")
    object MainGraph : Screen("main_graph")
}
