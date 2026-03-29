package com.example.unimarket.domain.model

data class PurchaseRequest(
    val productId: String,
    val quantity: Int,
    val deliveryMethod: DeliveryMethod,
    val paymentMethod: String,
    val meetingPoint: String = "",
    val buyerAddress: UserAddress? = null,
    val sellerAddress: UserAddress? = null
)
