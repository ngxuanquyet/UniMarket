package com.example.unimarket.data.api.model

data class SepayTransactionsResponseDto(
    val status: Int,
    val error: String?,
    val messages: SepayMessagesDto?,
    val transactions: List<SepayTransactionDto>
)

data class SepayMessagesDto(
    val success: Boolean
)

data class SepayTransactionDto(
    val id: String,
    val bank_brand_name: String,
    val account_number: String,
    val transaction_date: String,
    val amount_out: String,
    val amount_in: String,
    val accumulated: String,
    val transaction_content: String,
    val reference_number: String?,
    val code: String?,
    val sub_account: String?,
    val bank_account_id: String?
)
