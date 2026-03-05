package com.example.unimarket.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.CartItem
import com.example.unimarket.domain.repository.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository
) : ViewModel() {

    val uiState: StateFlow<CartUiState> = cartRepository.getCartItems()
        .map { items ->
            val subtotal = items.sumOf { it.product.price * it.quantity }
            CartUiState(
                cartItems = items,
                subtotal = subtotal,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CartUiState(isLoading = true)
        )

    fun updateQuantity(cartItemId: String, quantity: Int) {
        viewModelScope.launch {
            if (quantity > 0) {
                cartRepository.updateQuantity(cartItemId, quantity)
            } else {
                cartRepository.removeFromCart(cartItemId)
            }
        }
    }

    fun removeItem(cartItemId: String) {
        viewModelScope.launch {
            cartRepository.removeFromCart(cartItemId)
        }
    }
}
