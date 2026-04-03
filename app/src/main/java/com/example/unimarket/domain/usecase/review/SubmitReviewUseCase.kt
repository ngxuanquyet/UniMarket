package com.example.unimarket.domain.usecase.review

import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.repository.ReviewRepository
import javax.inject.Inject

class SubmitReviewUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository
) {
    suspend operator fun invoke(order: Order, rating: Int, comment: String) =
        reviewRepository.submitReview(order, rating, comment)
}
