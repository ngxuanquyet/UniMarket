package com.example.unimarket.data.repository

import com.example.unimarket.domain.model.CartItem
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class FakeCartRepositoryImpl @Inject constructor() : CartRepository {
    
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())

    init {
        _cartItems.value = listOf(
            CartItem(
                id = "1",
                product = Product(
                    id = "101",
                    name = "Introduction to Algorithms, 3rd Edition (Used)",
                    price = 45.00,
                    imageUrls = listOf("https://picsum.photos/seed/algo/100/100"),
                    categoryId = "3",
                    condition = "Used",
                    sellerName = "Senior",
                    rating = 4.5,
                    location = "Library",
                    timeAgo = "1d ago"
                ),
                quantity = 1
            ),
            CartItem(
                id = "2",
                product = Product(
                    id = "102",
                    name = "LED Desk Lamp with USB Charging Port",
                    price = 15.00,
                    imageUrls = listOf("https://picsum.photos/seed/lamp2/100/100"),
                    categoryId = "4",
                    condition = "Used",
                    sellerName = "Sophomore",
                    rating = 4.8,
                    location = "Dorm A",
                    timeAgo = "2d ago"
                ),
                quantity = 2
            )
        )
    }

    override fun getCartItems(): Flow<List<CartItem>> = _cartItems

    override suspend fun removeFromCart(cartItemId: String) {
        _cartItems.update { current ->
            current.filter { it.id != cartItemId }
        }
    }

    override suspend fun updateQuantity(cartItemId: String, quantity: Int) {
        _cartItems.update { current ->
            current.map { item ->
                if (item.id == cartItemId) item.copy(quantity = quantity)
                else item
            }
        }
    }
}
