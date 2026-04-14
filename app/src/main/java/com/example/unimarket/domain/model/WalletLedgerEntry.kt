package com.example.unimarket.domain.model

data class WalletLedgerEntry(
    val id: String,
    val type: String,
    val status: String,
    val amount: Double,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

