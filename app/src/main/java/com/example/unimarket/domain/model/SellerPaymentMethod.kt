package com.example.unimarket.domain.model

import java.io.Serializable

enum class SellerPaymentMethodType(val storageValue: String) {
    CASH_ON_DELIVERY("CASH_ON_DELIVERY"),
    BANK_TRANSFER("BANK_TRANSFER"),
    MOMO("MOMO"),
    WALLET("WALLET");

    companion object {
        fun fromRaw(raw: String?): SellerPaymentMethodType {
            return when (raw.orEmpty().trim().uppercase()) {
                "CASH_ON_DELIVERY",
                "CASH ON DELIVERY",
                "COD" -> CASH_ON_DELIVERY

                "BANK_TRANSFER",
                "BANK TRANSFER" -> BANK_TRANSFER

                "MOMO" -> MOMO
                "WALLET",
                "WALLET_PAYMENT" -> WALLET

                else -> CASH_ON_DELIVERY
            }
        }
    }
}

data class SellerPaymentMethod(
    val id: String = "",
    val type: SellerPaymentMethodType = SellerPaymentMethodType.BANK_TRANSFER,
    val label: String = "",
    val accountName: String = "",
    val accountNumber: String = "",
    val bankCode: String = "",
    val bankName: String = "",
    val phoneNumber: String = "",
    val note: String = "",
    val isDefault: Boolean = false
) : Serializable {
    val displayTitle: String
        get() = label.ifBlank {
            when (type) {
                SellerPaymentMethodType.BANK_TRANSFER -> bankName.ifBlank { "Bank transfer" }
                SellerPaymentMethodType.MOMO -> "MoMo"
                SellerPaymentMethodType.CASH_ON_DELIVERY -> "Cash on delivery"
                SellerPaymentMethodType.WALLET -> "Wallet"
            }
        }

    val shortSubtitle: String
        get() = when (type) {
            SellerPaymentMethodType.BANK_TRANSFER -> listOf(bankName, accountNumber.maskedSuffix())
                .filter { it.isNotBlank() }
                .joinToString(" • ")

            SellerPaymentMethodType.MOMO -> phoneNumber.maskedSuffix()
            SellerPaymentMethodType.CASH_ON_DELIVERY -> ""
            SellerPaymentMethodType.WALLET -> ""
        }

    fun supportsQr(): Boolean {
        return type == SellerPaymentMethodType.BANK_TRANSFER &&
            bankCode.isNotBlank() &&
            accountNumber.isNotBlank()
    }
}

private fun String.maskedSuffix(): String {
    if (length <= 4) return this
    return "••••" + takeLast(4)
}
