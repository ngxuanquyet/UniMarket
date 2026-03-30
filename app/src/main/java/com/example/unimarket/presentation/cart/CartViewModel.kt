package com.example.unimarket.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.cart.GetCartItemsUseCase
import com.example.unimarket.domain.usecase.cart.RemoveFromCartUseCase
import com.example.unimarket.domain.usecase.cart.UpdateQuantityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CartViewModel @Inject constructor(
    private val getCartItemsUseCase: GetCartItemsUseCase,
    private val updateQuantityUseCase: UpdateQuantityUseCase,
    private val removeFromCartUseCase: RemoveFromCartUseCase
) : ViewModel() {

    private val selectedCartItemIds = MutableStateFlow<Set<String>?>(null)

    val uiState: StateFlow<CartUiState> = combine(
        getCartItemsUseCase(),
        selectedCartItemIds
    ) { items, selectedIds ->
        val validItemIds = items.map { it.id }.toSet()
        val effectiveSelectedIds = when (selectedIds) {
            null -> validItemIds
            else -> selectedIds.intersect(validItemIds)
        }

        CartUiState(
            cartItems = items,
            selectedCartItemIds = effectiveSelectedIds,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CartUiState(isLoading = true)
    )

    fun toggleSelection(cartItemId: String, isSelected: Boolean) {
        val currentSelection = selectedCartItemIds.value.orEmpty().toMutableSet()
        if (isSelected) {
            currentSelection += cartItemId
        } else {
            currentSelection -= cartItemId
        }
        selectedCartItemIds.value = currentSelection
    }

    fun selectAll(select: Boolean) {
        selectedCartItemIds.value = if (select) {
            uiState.value.cartItems.map { it.id }.toSet()
        } else {
            emptySet()
        }
    }

    fun removeSelectedItemIds(cartItemIds: Set<String>) {
        val currentSelection = selectedCartItemIds.value.orEmpty().toMutableSet()
        currentSelection.removeAll(cartItemIds)
        selectedCartItemIds.value = currentSelection
    }

    fun updateQuantity(cartItemId: String, quantity: Int) {
        viewModelScope.launch {
            if (quantity > 0) {
                updateQuantityUseCase(cartItemId, quantity)
            } else {
                removeFromCartUseCase(cartItemId)
                removeSelectedItemIds(setOf(cartItemId))
            }
        }
    }

    fun removeItem(cartItemId: String) {
        viewModelScope.launch {
            removeFromCartUseCase(cartItemId)
            removeSelectedItemIds(setOf(cartItemId))
        }
    }
}
