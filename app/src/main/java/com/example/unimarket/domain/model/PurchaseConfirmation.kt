package com.example.unimarket.domain.model

data class PurchaseConfirmation(
    val orderId: String,
    val remainingQuantity: Int
)
