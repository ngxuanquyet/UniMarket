package com.example.unimarket.domain.model

data class AiListingInput(
    val title: String,
    val description: String,
    val category: String,
    val condition: String,
    val price: String,
    val quantity: String,
    val isNegotiable: Boolean,
    val specifications: Map<String, String>,
    val deliveryMethodsAvailable: List<DeliveryMethod>
)

data class AiListingSuggestion(
    val title: String,
    val description: String,
    val specifications: Map<String, String> = emptyMap()
)
