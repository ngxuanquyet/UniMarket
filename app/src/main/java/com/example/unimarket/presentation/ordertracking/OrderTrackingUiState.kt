package com.example.unimarket.presentation.ordertracking

import com.example.unimarket.domain.model.Order

data class OrderTrackingUiState(
    val isLoading: Boolean = true,
    val order: Order? = null,
    val errorMessage: String? = null
)

