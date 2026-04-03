package com.example.unimarket.domain.usecase.review

import com.example.unimarket.domain.repository.ReviewRepository
import javax.inject.Inject

class GetBuyerReviewsUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository
) {
    suspend operator fun invoke() = reviewRepository.getBuyerReviews()
}
