package com.example.unimarket.domain.usecase.wallet

import com.example.unimarket.domain.model.TopUpPaymentCheckResult
import com.example.unimarket.domain.model.TopUpPaymentStatus
import com.example.unimarket.domain.repository.TopUpRepository
import com.example.unimarket.domain.usecase.auth.RefreshCurrentUserProfileUseCase
import javax.inject.Inject

class CheckTopUpTransferPaymentUseCase @Inject constructor(
    private val topUpRepository: TopUpRepository,
    private val refreshCurrentUserProfileUseCase: RefreshCurrentUserProfileUseCase
) {
    suspend operator fun invoke(
        amount: Long,
        transferContent: String
    ): Result<TopUpPaymentCheckResult> {
        val result = topUpRepository.checkTransferAndCreditTopUp(
            amount = amount,
            transferContent = transferContent
        )

        if (result.getOrNull()?.status == TopUpPaymentStatus.CONFIRMED) {
            refreshCurrentUserProfileUseCase()
        }

        return result
    }
}
