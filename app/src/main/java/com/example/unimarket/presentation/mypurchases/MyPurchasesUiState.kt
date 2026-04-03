package com.example.unimarket.presentation.mypurchases

import com.example.unimarket.domain.model.Order

data class MyPurchasesUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val submittingReviewOrderId: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
