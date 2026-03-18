package com.example.unimarket.presentation.sellerprofile

import com.example.unimarket.domain.model.Product

data class SellerProfileUiState(
    val isLoading: Boolean = true,
    val sellerId: String = "",
    val sellerName: String = "",
    val avatarUrl: String = "",
    val studentId: String = "",
    val isVerifiedStudent: Boolean = false,
    val averageRating: Double = 0.0,
    val activeListings: List<Product> = emptyList(),
    val soldCount: Int = 0,
    val memberSinceLabel: String = "New",
    val selectedProductForChat: Product? = null,
    val errorMessage: String? = null
)
