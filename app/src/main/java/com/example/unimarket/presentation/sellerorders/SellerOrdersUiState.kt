package com.example.unimarket.presentation.sellerorders

import com.example.unimarket.domain.model.Order

data class SellerOrdersUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val updatingOrderId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
