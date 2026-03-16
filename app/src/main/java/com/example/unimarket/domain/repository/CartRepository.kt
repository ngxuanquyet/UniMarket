package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun getCartItems(): Flow<List<CartItem>>
    suspend fun addToCart(product: com.example.unimarket.domain.model.Product, quantity: Int = 1)
    suspend fun removeFromCart(cartItemId: String)
    suspend fun updateQuantity(cartItemId: String, quantity: Int)
}
