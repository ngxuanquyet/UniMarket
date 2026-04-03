package com.example.unimarket.domain.model

import android.net.Uri

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

data class AiImageListingInput(
    val imageUri: Uri,
    val title: String,
    val description: String,
    val category: String,
    val condition: String,
    val specifications: Map<String, String>
)

data class AiImageListingSuggestion(
    val title: String,
    val description: String,
    val category: String = "",
    val specifications: Map<String, String> = emptyMap()
)
