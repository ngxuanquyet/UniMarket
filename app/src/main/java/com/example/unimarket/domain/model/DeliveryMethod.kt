package com.example.unimarket.domain.model

enum class DeliveryMethod(
    val title: String,
    val subtitle: String
) {
    DIRECT_MEET(
        title = "Gặp trực tiếp",
        subtitle = "Hai bên hẹn gặp trực tiếp để giao nhận"
    ),
    BUYER_TO_SELLER(
        title = "Người mua đến lấy",
        subtitle = "Người mua tự đến lấy hàng tại địa chỉ của người bán"
    ),
    SELLER_TO_BUYER(
        title = "Người bán giao tận nơi",
        subtitle = "Người bán mang hàng đến địa chỉ người mua đã chọn"
    ),
    SHIPPING(
        title = "Gửi qua đơn vị vận chuyển",
        subtitle = "Giao qua đơn vị vận chuyển đến địa chỉ người mua"
    )
}

fun DeliveryMethod.toStorageValue(): String = name

fun deliveryMethodsFromStorage(values: List<String>): List<DeliveryMethod> {
    return values.mapNotNull { value ->
        DeliveryMethod.entries.firstOrNull { it.name == value }
    }
}
