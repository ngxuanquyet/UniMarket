package com.example.unimarket.presentation.mypurchases

import com.example.unimarket.domain.model.Order

data class MyPurchasesUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
