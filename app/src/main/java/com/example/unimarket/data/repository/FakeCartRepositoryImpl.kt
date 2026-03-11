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
                    description = "Used for CS intro class, has some dog-eared pages.",
                    imageUrls = listOf("https://picsum.photos/seed/algo/100/100"),
                    categoryId = "3",
                    condition = "Used",
                    sellerName = "Senior",
                    rating = 4.5,
                    location = "Library",
                    timeAgo = "1 week ago",
                    isFavorite = false,
                    isNegotiable = true,
                    userId = "fakeUser6",
                    specifications = mapOf("Publisher" to "MIT Press")
                ),
                quantity = 1
            ),
            CartItem(
                id = "2",
                product = Product(
                    id = "102",
                    name = "LED Desk Lamp with USB Charging Port",
                    price = 15.00,
                    description = "Has 3 brightness settings and a USB port.",
                    imageUrls = listOf("https://picsum.photos/seed/lamp2/100/100"),
                    categoryId = "4",
                    condition = "Used",
                    sellerName = "Sophomore",
                    rating = 4.8,
                    location = "Dorm A",
                    timeAgo = "2 days ago",
                    isFavorite = true,
                    isNegotiable = false,
                    userId = "fakeUser5",
                    specifications = mapOf("Color" to "White", "Feature" to "USB Port")
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
        val currentItems = _cartItems.value.toMutableList()
        val index = currentItems.indexOfFirst { it.id == cartItemId }
        if (index != -1 && quantity > 0) {
            currentItems[index] = currentItems[index].copy(quantity = quantity)
            _cartItems.value = currentItems
        }
    }

    override suspend fun addToCart(product: Product) {
        val currentItems = _cartItems.value.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            // If already in cart, just increase quantity
            currentItems[index] = currentItems[index].copy(quantity = currentItems[index].quantity + 1)
        } else {
            // If not in cart, add as new item
            currentItems.add(
                CartItem(
                    id = "cart_${System.currentTimeMillis()}",
                    product = product,
                    quantity = 1
                )
            )
        }
        _cartItems.value = currentItems
    }
}
