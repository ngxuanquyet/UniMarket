package com.example.unimarket.domain.model

data class Review(
    val orderId: String,
    val buyerId: String,
    val sellerId: String,
    val productId: String,
    val productName: String,
    val rating: Int,
    val comment: String = "",
    val createdAt: Long = 0L
)
