package com.example.unimarket.data.api.model

data class OrderPaymentCheckResponseDto(
    val ok: Boolean = false,
    val orderId: String = "",
    val status: String = "",
    val statusLabel: String = "",
    val paymentExpiresAt: Long = 0L,
    val paymentConfirmedAt: Long = 0L
)
