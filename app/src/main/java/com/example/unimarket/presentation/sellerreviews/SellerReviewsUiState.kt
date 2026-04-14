package com.example.unimarket.presentation.sellerreviews

import com.example.unimarket.domain.model.Review

data class SellerReviewsUiState(
    val isLoading: Boolean = true,
    val sellerId: String = "",
    val reviews: List<Review> = emptyList(),
    val averageRating: Double = 0.0,
    val ratingCount: Int = 0,
    val errorMessage: String? = null
)
