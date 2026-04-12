package com.example.unimarket.presentation.ordertracking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.usecase.chat.CreateOrGetConversationUseCase
import com.example.unimarket.domain.usecase.order.GetBuyerOrdersUseCase
import com.example.unimarket.domain.usecase.order.UpdateOrderStatusUseCase
import com.example.unimarket.presentation.util.localizedText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getBuyerOrdersUseCase: GetBuyerOrdersUseCase,
    private val createOrGetConversationUseCase: CreateOrGetConversationUseCase,
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>("orderId").orEmpty()

    private val _uiState = MutableStateFlow(OrderTrackingUiState())
    val uiState: StateFlow<OrderTrackingUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<OrderTrackingEvent>()
    val events: SharedFlow<OrderTrackingEvent> = _events.asSharedFlow()

    init {
        loadOrder()
    }

    fun refresh() {
        loadOrder()
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun contactSeller(order: Order) {
        if (order.productId.isBlank() || order.sellerId.isBlank()) {
            return
        }

        viewModelScope.launch {
            createOrGetConversationUseCase(order.toChatProduct())
                .onSuccess { conversationId ->
                    _events.emit(OrderTrackingEvent.OpenConversation(conversationId))
                }
                .onFailure {
                    // Keep silent for now to avoid breaking current OrderTracking UI flow.
                }
        }
    }

    fun cancelOrder(order: Order) {
        if (order.status != OrderStatus.WAITING_PAYMENT &&
            order.status != OrderStatus.WAITING_CONFIRMATION
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCancellingOrder = true,
                    errorMessage = null,
                    successMessage = null
                )
            }

            updateOrderStatusUseCase(order, OrderStatus.CANCELLED)
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            isCancellingOrder = false,
                            order = current.order?.copy(
                                status = OrderStatus.CANCELLED,
                                statusLabel = OrderStatus.CANCELLED.label
                            ),
                            successMessage = localizedText(
                                english = "Order cancelled successfully",
                                vietnamese = "Đã hủy đơn hàng thành công"
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCancellingOrder = false,
                            errorMessage = error.message ?: localizedText(
                                english = "Failed to cancel order",
                                vietnamese = "Không thể hủy đơn hàng"
                            )
                        )
                    }
                }
        }
    }

    private fun loadOrder() {
        if (orderId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isCancellingOrder = false,
                    errorMessage = localizedText(
                        english = "Order not found",
                        vietnamese = "Không tìm thấy đơn hàng"
                    )
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
                            isCancellingOrder = false,
                            order = order,
                            errorMessage = if (order == null) {
                                localizedText(
                                    english = "Order not found",
                                    vietnamese = "Không tìm thấy đơn hàng"
                                )
                            } else {
                                null
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isCancellingOrder = false,
                            errorMessage = error.message ?: localizedText(
                                english = "Failed to load order",
                                vietnamese = "Không thể tải đơn hàng"
                            )
                        )
                    }
                }
        }
    }
}

private fun Order.toChatProduct(): Product {
    return Product(
        id = productId,
        name = productName,
        price = unitPrice,
        description = productDetail,
        imageUrls = listOfNotNull(productImageUrl.takeIf { it.isNotBlank() }),
        categoryId = "",
        condition = "",
        sellerName = storeName,
        rating = 0.0,
        location = "",
        timeAgo = "",
        userId = sellerId
    )
}

sealed interface OrderTrackingEvent {
    data class OpenConversation(val conversationId: String) : OrderTrackingEvent
}
