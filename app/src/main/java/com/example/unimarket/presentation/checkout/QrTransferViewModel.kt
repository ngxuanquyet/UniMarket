package com.example.unimarket.presentation.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.domain.usecase.order.CheckTransferPaymentUseCase
import com.example.unimarket.domain.usecase.order.GetBuyerOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QrTransferUiState(
    val requestedOrderIds: List<String> = emptyList(),
    val orders: List<Order> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val isCheckingPayment: Boolean = false,
    val errorMessage: String? = null
) {
    val currentOrder: Order?
        get() = orders.getOrNull(currentIndex)
}

@HiltViewModel
class QrTransferViewModel @Inject constructor(
    private val getBuyerOrdersUseCase: GetBuyerOrdersUseCase,
    private val checkTransferPaymentUseCase: CheckTransferPaymentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrTransferUiState(isLoading = true))
    val uiState: StateFlow<QrTransferUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class PaymentConfirmed(val orderId: String) : UiEvent()
        data class AllTransfersCompleted(val order: Order) : UiEvent()
    }

    fun loadOrders(orderIds: List<String>) {
        if (orderIds.isEmpty()) {
            _uiState.value = QrTransferUiState(
                requestedOrderIds = emptyList(),
                isLoading = false,
                errorMessage = "No transfer order found"
            )
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    requestedOrderIds = orderIds,
                    isLoading = true,
                    errorMessage = null
                )
            }
            refreshOrders(
                orderIds = orderIds,
                preserveSelection = false
            )
        }
    }

    fun moveToNextOrder() {
        val state = _uiState.value
        val currentOrder = state.currentOrder
        if (state.currentIndex < state.orders.lastIndex) {
            _uiState.update { it.copy(currentIndex = it.currentIndex + 1) }
        } else if (currentOrder != null) {
            emitAllTransfersCompleted(currentOrder)
        }
    }

    fun onPaymentSuccessHandled(orderId: String) {
        val state = _uiState.value
        val currentOrder = state.currentOrder
        if (currentOrder?.id != orderId) return

        if (state.currentIndex < state.orders.lastIndex) {
            _uiState.update { it.copy(currentIndex = it.currentIndex + 1) }
        } else {
            emitAllTransfersCompleted(currentOrder)
        }
    }

    fun checkCurrentOrderPayment(showPendingMessage: Boolean) {
        val order = _uiState.value.currentOrder ?: return
        if (order.status != OrderStatus.WAITING_PAYMENT) return
        if (_uiState.value.isCheckingPayment) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingPayment = true) }

            val result = checkTransferPaymentUseCase(order.id)
            _uiState.update { it.copy(isCheckingPayment = false) }

            val checkResult = result.getOrNull()
            if (checkResult != null) {
                _uiState.update { state ->
                    state.copy(
                        orders = state.orders.map { existing ->
                            if (existing.id == order.id) {
                                existing.copy(
                                    status = checkResult.status,
                                    statusLabel = checkResult.statusLabel,
                                    paymentExpiresAt = checkResult.paymentExpiresAt,
                                    paymentConfirmedAt = checkResult.paymentConfirmedAt,
                                    updatedAt = System.currentTimeMillis()
                                )
                            } else {
                                existing
                            }
                        }
                    )
                }

                when (checkResult.status) {
                    OrderStatus.WAITING_PAYMENT -> {
                        if (showPendingMessage) {
                            _uiEvent.emit(UiEvent.ShowSnackbar("Payment not found yet. Please try again in a moment."))
                        }
                    }

                    OrderStatus.CANCELLED -> {
                        _uiEvent.emit(UiEvent.ShowSnackbar("This transfer request has expired."))
                    }

                    else -> {
                        _uiEvent.emit(UiEvent.PaymentConfirmed(order.id))
                    }
                }
            } else {
                val error = result.exceptionOrNull()
                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        error?.message ?: "Failed to verify transfer payment"
                    )
                )
            }

            refreshOrders(
                orderIds = _uiState.value.requestedOrderIds,
                preserveSelection = true
            )
        }
    }

    private suspend fun refreshOrders(
        orderIds: List<String>,
        preserveSelection: Boolean
    ) {
        val previousCurrentOrderId = _uiState.value.currentOrder?.id

        getBuyerOrdersUseCase()
            .onSuccess { orders ->
                val mappedOrders = orderIds.mapNotNull { orderId ->
                    orders.firstOrNull { it.id == orderId }
                }

                val newIndex = when {
                    mappedOrders.isEmpty() -> 0
                    preserveSelection && previousCurrentOrderId != null -> {
                        mappedOrders.indexOfFirst { it.id == previousCurrentOrderId }
                            .takeIf { it >= 0 }
                            ?: 0
                    }
                    else -> 0
                }

                _uiState.update {
                    it.copy(
                        orders = mappedOrders,
                        currentIndex = newIndex.coerceIn(0, mappedOrders.lastIndex.coerceAtLeast(0)),
                        isLoading = false,
                        errorMessage = if (mappedOrders.isEmpty()) {
                            "Transfer order was not found"
                        } else {
                            null
                        }
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        orders = emptyList(),
                        currentIndex = 0,
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load transfer details"
                    )
                }
            }
    }

    private fun emitAllTransfersCompleted(order: Order) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.AllTransfersCompleted(order))
        }
    }
}
