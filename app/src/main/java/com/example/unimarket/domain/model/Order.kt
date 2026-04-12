package com.example.unimarket.domain.model

data class Order(
    val id: String,
    val documentPath: String = "",
    val buyerId: String,
    val buyerName: String = "",
    val sellerId: String,
    val storeName: String,
    val productId: String,
    val productName: String,
    val productDetail: String = "",
    val productImageUrl: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val deliveryMethod: String = "",
    val paymentMethod: String = "",
    val paymentMethodDetails: SellerPaymentMethod? = null,
    val meetingPoint: String = "",
    val buyerAddress: UserAddress? = null,
    val sellerAddress: UserAddress? = null,
    val transferContent: String = "",
    val paymentExpiresAt: Long = 0L,
    val paymentConfirmedAt: Long = 0L,
    val status: OrderStatus = OrderStatus.UNKNOWN,
    val statusLabel: String = status.label,
    val reviewRating: Int? = null,
    val reviewComment: String = "",
    val reviewCreatedAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

enum class OrderStatus(val label: String) {
    WAITING_PAYMENT("WAIT PAYMENT"),
    WAITING_CONFIRMATION("WAITING"),
    WAITING_PICKUP("WAIT PICKUP"),
    SHIPPING("SHIPPING"),
    IN_TRANSIT("IN TRANSIT"),
    OUT_FOR_DELIVERY("OUT FOR DELIVERY"),
    DELIVERED("DELIVERED"),
    CANCELLED("CANCELLED"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromRaw(raw: String?): OrderStatus {
            val normalized = raw
                .orEmpty()
                .trim()
                .uppercase()
                .replace("-", "_")
                .replace(" ", "_")

            return when (normalized) {
                "WAIT_PAYMENT",
                "WAITING_PAYMENT",
                "WAIT_FOR_PAYMENT",
                "PENDING_PAYMENT",
                "AWAITING_PAYMENT" -> WAITING_PAYMENT

                "WAIT_CONFIRMATION",
                "WAITING",
                "WAITING_CONFIRMATION",
                "WAIT_FOR_CONFIRMATION",
                "CONFIRMED",
                "PENDING",
                "PENDING_CONFIRMATION" -> WAITING_CONFIRMATION

                "WAIT_PICKUP",
                "WAITING_PICKUP",
                "WAIT_FOR_PICKUP",
                "READY_FOR_PICKUP",
                "PICKUP_READY" -> WAITING_PICKUP

                "SHIPPING",
                "SHIPPED" -> SHIPPING

                "IN_TRANSIT" -> IN_TRANSIT

                "OUT_FOR_DELIVERY" -> OUT_FOR_DELIVERY

                "DELIVERED",
                "COMPLETED",
                "SUCCESS" -> DELIVERED

                "CANCELLED",
                "CANCELED",
                "FAILED" -> CANCELLED

                else -> UNKNOWN
            }
        }
    }
}
