package com.example.unimarket.data.api.model

data class BuyNowPurchaseRequestDto(
    val productId: String,
    val quantity: Int,
    val deliveryMethod: String,
    val paymentMethod: String,
    val paymentMethodDetails: CheckoutPaymentMethodDto? = null,
    val meetingPoint: String = "",
    val buyerAddress: CheckoutAddressDto? = null,
    val sellerAddress: CheckoutAddressDto? = null
)

data class CheckoutPaymentMethodDto(
    val id: String = "",
    val type: String = "",
    val label: String = "",
    val accountName: String = "",
    val accountNumber: String = "",
    val bankCode: String = "",
    val bankName: String = "",
    val phoneNumber: String = "",
    val note: String = "",
    val isDefault: Boolean = false
)

data class CheckoutAddressDto(
    val id: String = "",
    val recipientName: String = "",
    val phoneNumber: String = "",
    val addressLine: String = "",
    val isDefault: Boolean = false
)
