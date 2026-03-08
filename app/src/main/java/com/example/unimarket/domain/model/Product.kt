package com.example.unimarket.domain.model

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrls: List<String>,
    val categoryId: String,
    val condition: String,
    val sellerName: String,
    val rating: Double,
    val location: String,
    val timeAgo: String,
    val isFavorite: Boolean = false
)
