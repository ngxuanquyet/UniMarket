package com.example.unimarket.domain.model

data class TopUpPaymentCheckResult(
    val status: TopUpPaymentStatus
)

enum class TopUpPaymentStatus {
    PENDING,
    CONFIRMED
}
