package com.example.unimarket.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.CartItem
import com.example.unimarket.domain.usecase.cart.GetCartItemsUseCase
import com.example.unimarket.domain.usecase.cart.UpdateQuantityUseCase
import com.example.unimarket.domain.usecase.cart.RemoveFromCartUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CartViewModel @Inject constructor(
    private val getCartItemsUseCase: GetCartItemsUseCase,
    private val updateQuantityUseCase: UpdateQuantityUseCase,
    private val removeFromCartUseCase: RemoveFromCartUseCase
) : ViewModel() {

    val uiState: StateFlow<CartUiState> = getCartItemsUseCase()
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
                updateQuantityUseCase(cartItemId, quantity)
            } else {
                removeFromCartUseCase(cartItemId)
            }
        }
    }

    fun removeItem(cartItemId: String) {
        viewModelScope.launch {
            removeFromCartUseCase(cartItemId)
        }
    }
}
