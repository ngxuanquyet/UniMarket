package com.example.unimarket.presentation.sellerreviews

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.domain.usecase.review.GetSellerReviewsUseCase
import com.example.unimarket.presentation.util.localizedText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SellerReviewsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSellerReviewsUseCase: GetSellerReviewsUseCase
) : ViewModel() {

    private val sellerId: String = savedStateHandle.get<String>("sellerId").orEmpty()

    private val _uiState = MutableStateFlow(SellerReviewsUiState(sellerId = sellerId))
    val uiState = _uiState.asStateFlow()

    init {
        loadReviews()
    }

    fun refresh() {
        loadReviews()
    }

    private fun loadReviews() {
        if (sellerId.isBlank()) {
            _uiState.value = SellerReviewsUiState(
                isLoading = false,
                sellerId = sellerId,
                errorMessage = localizedText(
                    english = "Seller not found",
                    vietnamese = "Không tìm thấy người bán"
                )
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            getSellerReviewsUseCase(sellerId)
                .onSuccess { reviews ->
                    val ratingCount = reviews.size
                    val averageRating = if (ratingCount > 0) {
                        reviews.map { it.rating }.average()
                    } else {
                        0.0
                    }

                    _uiState.value = SellerReviewsUiState(
                        isLoading = false,
                        sellerId = sellerId,
                        reviews = reviews,
                        ratingCount = ratingCount,
                        averageRating = averageRating
                    )
                }
                .onFailure { error ->
                    _uiState.value = SellerReviewsUiState(
                        isLoading = false,
                        sellerId = sellerId,
                        errorMessage = error.message ?: localizedText(
                            english = "Unable to load seller reviews",
                            vietnamese = "Không thể tải đánh giá của người bán"
                        )
                    )
                }
        }
    }
}
