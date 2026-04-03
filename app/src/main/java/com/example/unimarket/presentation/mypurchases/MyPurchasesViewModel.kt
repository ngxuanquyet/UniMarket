package com.example.unimarket.presentation.mypurchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.usecase.order.GetBuyerOrdersUseCase
import com.example.unimarket.domain.usecase.review.GetBuyerReviewsUseCase
import com.example.unimarket.domain.usecase.review.SubmitReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPurchasesViewModel @Inject constructor(
    private val getBuyerOrdersUseCase: GetBuyerOrdersUseCase,
    private val getBuyerReviewsUseCase: GetBuyerReviewsUseCase,
    private val submitReviewUseCase: SubmitReviewUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPurchasesUiState(isLoading = true))
    val uiState: StateFlow<MyPurchasesUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun refresh() {
        loadOrders()
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun submitReview(order: Order, rating: Int, comment: String) {
        if (rating !in 1..5) {
            _uiState.update { it.copy(errorMessage = "Please select a rating from 1 to 5") }
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
                            successMessage = "Thanks for rating this seller"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            submittingReviewOrderId = null,
                            errorMessage = error.message ?: "Failed to submit your rating"
                        )
                    }
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
                                    errorMessage = error.message ?: "Failed to load your ratings"
                                )
                            }
                        }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            orders = emptyList(),
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load your orders"
                        )
                    }
                }
        }
    }
}
