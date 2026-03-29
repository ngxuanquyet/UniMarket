package com.example.unimarket.data.api.model

data class BuyNowPurchaseRequestDto(
    val productId: String,
    val quantity: Int,
    val deliveryMethod: String,
    val paymentMethod: String,
    val meetingPoint: String = "",
    val buyerAddress: CheckoutAddressDto? = null,
    val sellerAddress: CheckoutAddressDto? = null
)

data class CheckoutAddressDto(
    val id: String = "",
    val recipientName: String = "",
    val phoneNumber: String = "",
    val addressLine: String = "",
    val isDefault: Boolean = false
)
