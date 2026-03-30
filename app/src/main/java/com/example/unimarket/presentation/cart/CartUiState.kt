package com.example.unimarket.presentation.cart

import com.example.unimarket.domain.model.CartItem

data class CartUiState(
    val cartItems: List<CartItem> = emptyList(),
    val selectedCartItemIds: Set<String> = emptySet(),
    val isLoading: Boolean = false
) {
    val selectedItems: List<CartItem>
        get() = cartItems.filter { it.id in selectedCartItemIds }

    val selectedSubtotal: Double
        get() = selectedItems.sumOf { it.product.price * it.quantity }
}
