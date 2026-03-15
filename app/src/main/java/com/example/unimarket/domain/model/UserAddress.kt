package com.example.unimarket.domain.model

data class UserAddress(
    val id: String = "",
    val recipientName: String = "",
    val phoneNumber: String = "",
    val addressLine: String = "",
    val isDefault: Boolean = false
) {
    fun shortDisplay(): String = addressLine
}
