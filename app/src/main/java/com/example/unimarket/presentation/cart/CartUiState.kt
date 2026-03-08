package com.example.unimarket.presentation.cart

import com.example.unimarket.domain.model.CartItem

data class CartUiState(
    val cartItems: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 2.00,
    val discount: Double = 5.00,
    val isLoading: Boolean = false
) {
    val total: Double
        get() = (subtotal + deliveryFee - discount).coerceAtLeast(0.0)
}
