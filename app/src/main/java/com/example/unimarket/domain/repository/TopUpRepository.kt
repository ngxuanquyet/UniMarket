package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.TopUpPaymentCheckResult

interface TopUpRepository {
    suspend fun checkTransferAndCreditTopUp(
        amount: Long,
        transferContent: String
    ): Result<TopUpPaymentCheckResult>
}
