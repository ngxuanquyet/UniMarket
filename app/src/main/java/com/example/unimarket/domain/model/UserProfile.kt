package com.example.unimarket.domain.model

data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String,
    val university: String = "",
    val isLock: Boolean = false,
    val studentId: String = "",
    val boughtCount: Int = 0,
    val soldCount: Int = 0,
    val averageRating: Double = 0.0,
    val ratingCount: Int = 0,
    val walletBalance: Double = 0.0,
    val paymentMethods: List<SellerPaymentMethod> = emptyList()
)
