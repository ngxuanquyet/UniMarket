package com.example.unimarket.presentation.mypurchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.domain.model.Product
import com.example.unimarket.domain.usecase.chat.CreateOrGetConversationUseCase
import com.example.unimarket.domain.usecase.order.GetBuyerOrdersUseCase
import com.example.unimarket.domain.usecase.order.UpdateOrderStatusUseCase
import com.example.unimarket.domain.usecase.product.GetProductByIdUseCase
import com.example.unimarket.domain.usecase.product.UpdateProductUseCase
import com.example.unimarket.domain.usecase.review.GetBuyerReviewsUseCase
import com.example.unimarket.domain.usecase.review.SubmitReviewUseCase
import com.example.unimarket.presentation.util.localizedText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPurchasesViewModel @Inject constructor(
    private val getBuyerOrdersUseCase: GetBuyerOrdersUseCase,
    private val getBuyerReviewsUseCase: GetBuyerReviewsUseCase,
    private val submitReviewUseCase: SubmitReviewUseCase,
    private val createOrGetConversationUseCase: CreateOrGetConversationUseCase,
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val updateProductUseCase: UpdateProductUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPurchasesUiState(isLoading = true))
    val uiState: StateFlow<MyPurchasesUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<MyPurchasesEvent>()
    val events: SharedFlow<MyPurchasesEvent> = _events.asSharedFlow()

    init {
        loadOrders()
    }

    fun refresh() {
        loadOrders()
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun contactSeller(order: Order) {
        if (order.productId.isBlank() || order.sellerId.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = localizedText(
                        english = "Missing seller or product information to contact seller",
                        vietnamese = "Thiếu thông tin người bán hoặc sản phẩm để liên hệ"
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            createOrGetConversationUseCase(order.toChatProduct())
                .onSuccess { conversationId ->
                    _events.emit(MyPurchasesEvent.OpenConversation(conversationId))
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: localizedText(
                                english = "Failed to contact seller",
                                vietnamese = "Không thể liên hệ người bán"
                            )
                        )
                    }
                }
        }
    }

    fun submitReview(order: Order, rating: Int, comment: String) {
        if (rating !in 1..5) {
            _uiState.update {
                it.copy(
                    errorMessage = localizedText(
                        english = "Please select a rating from 1 to 5",
                        vietnamese = "Vui lòng chọn số sao từ 1 đến 5"
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    submittingReviewOrderId = order.id,
                    errorMessage = null,
                    successMessage = null
                )
            }

            submitReviewUseCase(order, rating, comment)
                .onSuccess { review ->
                    _uiState.update { current ->
                        current.copy(
                            orders = current.orders.map { existing ->
                                if (existing.id == order.id) {
                                    existing.copy(
                                        reviewRating = review.rating,
                                        reviewComment = review.comment,
                                        reviewCreatedAt = review.createdAt
                                    )
                                } else {
                                    existing
                                }
                            },
                            submittingReviewOrderId = null,
                            successMessage = localizedText(
                                english = "Thanks for rating this seller",
                                vietnamese = "Cảm ơn bạn đã đánh giá người bán"
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            submittingReviewOrderId = null,
                            errorMessage = error.message ?: localizedText(
                                english = "Failed to submit your rating",
                                vietnamese = "Không thể gửi đánh giá của bạn"
                            )
                        )
                    }
                }
        }
    }

    fun cancelPendingPayment(order: Order) {
        if (order.status != OrderStatus.WAITING_PAYMENT) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    cancellingOrderId = order.id,
                    errorMessage = null,
                    successMessage = null
                )
            }

            val cancelResult = updateOrderStatusUseCase(order, OrderStatus.CANCELLED)
            if (cancelResult.isFailure) {
                _uiState.update {
                    it.copy(
                        cancellingOrderId = null,
                        errorMessage = cancelResult.exceptionOrNull()?.message ?: localizedText(
                            english = "Failed to cancel payment",
                            vietnamese = "Không thể hủy thanh toán"
                        )
                    )
                }
                return@launch
            }

            val restockResult = runCatching {
                val product = getProductByIdUseCase(order.productId).getOrThrow()

                val updatedProduct = product.copy(
                    quantityAvailable = (product.quantityAvailable + order.quantity).coerceAtLeast(0)
                )
                updateProductUseCase(updatedProduct).getOrThrow()
            }

            if (restockResult.isFailure) {
                _uiState.update {
                    it.copy(
                        cancellingOrderId = null,
                        errorMessage = localizedText(
                            english = "Payment cancelled but failed to restock product",
                            vietnamese = "Đã hủy thanh toán nhưng không thể hoàn lại số lượng sản phẩm"
                        )
                    )
                }
                return@launch
            }

            _uiState.update { current ->
                current.copy(
                    cancellingOrderId = null,
                    orders = current.orders.filterNot { it.id == order.id },
                    successMessage = localizedText(
                        english = "Payment cancelled and order removed",
                        vietnamese = "Đã hủy thanh toán và xóa đơn hàng"
                    )
                )
            }
        }
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            getBuyerOrdersUseCase()
                .onSuccess { orders ->
                    getBuyerReviewsUseCase()
                        .onSuccess { reviews ->
                            val reviewByOrderId = reviews.associateBy { it.orderId }
                            _uiState.update {
                                it.copy(
                                    orders = orders.map { order ->
                                        val review = reviewByOrderId[order.id]
                                        if (review != null) {
                                            order.copy(
                                                reviewRating = review.rating,
                                                reviewComment = review.comment,
                                                reviewCreatedAt = review.createdAt
                                            )
                                        } else {
                                            order
                                        }
                                    },
                                    isLoading = false
                                )
                            }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    orders = orders,
                                    isLoading = false,
                                    errorMessage = error.message ?: localizedText(
                                        english = "Failed to load your ratings",
                                        vietnamese = "Không thể tải đánh giá của bạn"
                                    )
                                )
                            }
                        }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            orders = emptyList(),
                            isLoading = false,
                            errorMessage = error.message ?: localizedText(
                                english = "Failed to load your orders",
                                vietnamese = "Không thể tải đơn mua của bạn"
                            )
                        )
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
}

sealed interface MyPurchasesEvent {
    data class OpenConversation(val conversationId: String) : MyPurchasesEvent
}
