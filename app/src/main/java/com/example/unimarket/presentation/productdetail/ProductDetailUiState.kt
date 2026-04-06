package com.example.unimarket.presentation.productdetail

import com.example.unimarket.domain.model.Product

data class ProductDetailUiState(
    val product: Product? = null,
    val sellerAvatarUrl: String = "",
    val sellerAverageRating: Double = 0.0,
    val sellerRatingCount: Int = 0,
    val sellerSoldCount: Int = 0,
    val isSellerVerifiedStudent: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
