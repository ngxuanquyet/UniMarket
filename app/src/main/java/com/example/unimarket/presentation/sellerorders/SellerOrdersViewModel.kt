package com.example.unimarket.presentation.sellerorders

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.domain.usecase.order.GetSellerOrdersUseCase
import com.example.unimarket.domain.usecase.order.UpdateOrderStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SellerOrdersViewModel @Inject constructor(
    private val getSellerOrdersUseCase: GetSellerOrdersUseCase,
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerOrdersUiState(isLoading = true))
    val uiState: StateFlow<SellerOrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun refresh() {
        loadOrders()
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun advanceOrder(order: Order) {
        val nextStatus = nextStatusFor(order) ?: return
        val successMessage = successMessageFor(order, nextStatus)
        updateOrder(order, nextStatus, successMessage)
    }

    fun cancelOrder(order: Order) {
        updateOrder(order, OrderStatus.CANCELLED, "Order cancelled")
    }

    private fun updateOrder(order: Order, status: OrderStatus, successMessage: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updatingOrderId = order.id,
                    errorMessage = null,
                    successMessage = null
                )
            }

            updateOrderStatusUseCase(order, status)
                .onSuccess {
                    Log.d(
                        "SellerOrdersViewModel",
                        "Order status updated: orderId=${order.id}, from=${order.status}, to=$status"
                    )
                    _uiState.update { current ->
                        current.copy(
                            updatingOrderId = null,
                            orders = current.orders.map { existing ->
                                if (existing.id == order.id) {
                                    existing.copy(
                                        status = status,
                                        statusLabel = status.label,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                } else {
                                    existing
                                }
                            },
                            successMessage = successMessage
                        )
                    }
                }
                .onFailure { error ->
                    Log.e(
                        "SellerOrdersViewModel",
                        "Failed to update order status: orderId=${order.id}, from=${order.status}, to=$status",
                        error
                    )
                    _uiState.update {
                        it.copy(
                            updatingOrderId = null,
                            errorMessage = error.message ?: "Failed to update order"
                        )
                    }
                }
        }
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            getSellerOrdersUseCase()
                .onSuccess { orders ->
                    _uiState.update {
                        it.copy(
                            orders = orders.filterNot { order ->
                                order.status == OrderStatus.WAITING_PAYMENT
                            },
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            orders = emptyList(),
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load seller orders"
                        )
                    }
                }
        }
    }

    private fun nextStatusFor(order: Order): OrderStatus? {
        return when (order.status) {
            OrderStatus.WAITING_CONFIRMATION -> OrderStatus.WAITING_PICKUP
            OrderStatus.WAITING_PICKUP -> OrderStatus.SHIPPING
            OrderStatus.SHIPPING -> OrderStatus.DELIVERED
            OrderStatus.IN_TRANSIT -> OrderStatus.DELIVERED
            OrderStatus.OUT_FOR_DELIVERY -> OrderStatus.DELIVERED
            else -> null
        }
    }

    private fun successMessageFor(order: Order, nextStatus: OrderStatus): String {
        if (order.status == OrderStatus.WAITING_CONFIRMATION) {
            return "Order confirmed"
        }

        return when (nextStatus) {
            OrderStatus.WAITING_PICKUP -> "Order moved to waiting pickup"
            OrderStatus.SHIPPING -> "Delivery started"
            OrderStatus.DELIVERED -> "Order marked delivered"
            else -> "Order updated"
        }
    }
}
