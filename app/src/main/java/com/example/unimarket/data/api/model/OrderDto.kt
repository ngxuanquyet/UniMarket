package com.example.unimarket.data.api.model

data class OrdersResponseDto(
    val orders: List<OrderDto> = emptyList()
)

data class OrderDto(
    val id: String,
    val documentPath: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val productId: String = "",
    val productName: String = "",
    val productDetail: String = "",
    val productImageUrl: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val deliveryMethod: String = "",
    val paymentMethod: String = "",
    val paymentMethodDetails: CheckoutPaymentMethodDto? = null,
    val meetingPoint: String = "",
    val buyerAddress: CheckoutAddressDto? = null,
    val sellerAddress: CheckoutAddressDto? = null,
    val transferContent: String = "",
    val paymentExpiresAt: Long = 0L,
    val paymentConfirmedAt: Long = 0L,
    val status: String = "",
    val statusLabel: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
