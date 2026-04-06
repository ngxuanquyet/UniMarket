package com.example.unimarket.domain.model

data class OrderPaymentCheckResult(
    val orderId: String,
    val status: OrderStatus,
    val statusLabel: String = status.label,
    val paymentExpiresAt: Long = 0L,
    val paymentConfirmedAt: Long = 0L
)
