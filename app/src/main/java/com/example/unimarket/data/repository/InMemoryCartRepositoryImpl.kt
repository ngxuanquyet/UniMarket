package com.example.unimarket.data.repository

import android.content.Context
import com.example.unimarket.domain.model.CartItem
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.repository.CartRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class InMemoryCartRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : CartRepository {

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val cartItemListType = object : TypeToken<List<CartItem>>() {}.type

    private val cartItems = MutableStateFlow(loadCartItems())

    override fun getCartItems(): Flow<List<CartItem>> = cartItems

    override suspend fun removeFromCart(cartItemId: String) {
        cartItems.update { currentItems ->
            currentItems.filter { it.id != cartItemId }
        }
        persistCartItems()
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
            persistCartItems()
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
        persistCartItems()
    }

    private fun loadCartItems(): List<CartItem> {
        val json = sharedPreferences.getString(KEY_CART_ITEMS, null).orEmpty()
        if (json.isBlank()) return emptyList()

        return runCatching {
            gson.fromJson<List<CartItem>>(json, cartItemListType).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun persistCartItems() {
        sharedPreferences
            .edit()
            .putString(KEY_CART_ITEMS, gson.toJson(cartItems.value, cartItemListType))
            .apply()
    }

    private companion object {
        const val PREF_NAME = "unimarket_cart_prefs"
        const val KEY_CART_ITEMS = "cart_items"
    }
}
