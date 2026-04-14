package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.SellerPaymentMethod

interface PayoutRepository {
    suspend fun createWithdrawalRequest(
        amount: Long,
        receiverMethod: SellerPaymentMethod
    ): Result<String>
}
