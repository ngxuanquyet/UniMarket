package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.Review

interface ReviewRepository {
    suspend fun getBuyerReviews(): Result<List<Review>>
    suspend fun submitReview(order: Order, rating: Int, comment: String): Result<Review>
}
