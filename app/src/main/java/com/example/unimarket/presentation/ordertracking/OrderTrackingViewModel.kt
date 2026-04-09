package com.example.unimarket.presentation.ordertracking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.order.GetBuyerOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getBuyerOrdersUseCase: GetBuyerOrdersUseCase
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>("orderId").orEmpty()

    private val _uiState = MutableStateFlow(OrderTrackingUiState())
    val uiState: StateFlow<OrderTrackingUiState> = _uiState.asStateFlow()

    init {
        loadOrder()
    }

    fun refresh() {
        loadOrder()
    }

    private fun loadOrder() {
        if (orderId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Order not found"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getBuyerOrdersUseCase()
                .onSuccess { orders ->
                    val order = orders.firstOrNull { it.id == orderId }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            order = order,
                            errorMessage = if (order == null) "Order not found" else null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load order"
                        )
                    }
                }
        }
    }
}

