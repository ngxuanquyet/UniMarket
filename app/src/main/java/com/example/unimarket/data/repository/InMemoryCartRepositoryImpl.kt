package com.example.unimarket.data.repository

import com.example.unimarket.domain.model.CartItem
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class InMemoryCartRepositoryImpl @Inject constructor() : CartRepository {

    private val cartItems = MutableStateFlow<List<CartItem>>(emptyList())

    override fun getCartItems(): Flow<List<CartItem>> = cartItems

    override suspend fun removeFromCart(cartItemId: String) {
        cartItems.update { currentItems ->
            currentItems.filter { it.id != cartItemId }
        }
    }

    override suspend fun updateQuantity(cartItemId: String, quantity: Int) {
        val currentItems = cartItems.value.toMutableList()
        val itemIndex = currentItems.indexOfFirst { it.id == cartItemId }
        if (itemIndex != -1 && quantity > 0) {
            val maxQuantity = currentItems[itemIndex].product.quantityAvailable
            currentItems[itemIndex] = currentItems[itemIndex].copy(
                quantity = quantity.coerceAtMost(maxQuantity)
            )
            cartItems.value = currentItems
        }
    }

    override suspend fun addToCart(product: Product, quantity: Int) {
        if (quantity <= 0) return

        val currentItems = cartItems.value.toMutableList()
        val itemIndex = currentItems.indexOfFirst { it.product.id == product.id }
        if (itemIndex != -1) {
            currentItems[itemIndex] =
                currentItems[itemIndex].copy(
                    quantity = (currentItems[itemIndex].quantity + quantity)
                        .coerceAtMost(product.quantityAvailable)
                )
        } else {
            currentItems.add(
                CartItem(
                    id = "cart_${System.currentTimeMillis()}",
                    product = product,
                    quantity = quantity.coerceAtMost(product.quantityAvailable)
                )
            )
        }
        cartItems.value = currentItems
    }
}
