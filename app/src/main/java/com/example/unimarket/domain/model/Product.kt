package com.example.unimarket.domain.model

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val description: String,
    val imageUrls: List<String>,
    val categoryId: String,
    val condition: String,
    val sellerName: String,
    val rating: Double,
    val location: String,
    val timeAgo: String,
    val postedAt: Long = 0L,
    val isFavorite: Boolean = false,
    val isNegotiable: Boolean = false,
    val quantityAvailable: Int = 1,
    val userId: String,
    val specifications: Map<String, String> = emptyMap(),
    val deliveryMethodsAvailable: List<DeliveryMethod> = emptyList()
)
