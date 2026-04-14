package com.example.unimarket.domain.usecase.wallet

import com.example.unimarket.domain.model.SellerPaymentMethod
import com.example.unimarket.domain.repository.PayoutRepository
import javax.inject.Inject

class CreateWithdrawalRequestUseCase @Inject constructor(
    private val payoutRepository: PayoutRepository
) {
    suspend operator fun invoke(
        amount: Long,
        receiverMethod: SellerPaymentMethod
    ): Result<String> {
        return payoutRepository.createWithdrawalRequest(amount, receiverMethod)
    }
}
