package com.example.unimarket.data.api.model

data class OrderStatusUpdateRequestDto(
    val status: String
)

data class OrderStatusUpdateResponseDto(
    val ok: Boolean,
    val orderId: String,
    val status: String
)
