package com.example.unimarket.domain.usecase.checkout

import com.example.unimarket.domain.model.PurchaseConfirmation
import com.example.unimarket.domain.model.PurchaseRequest
import com.example.unimarket.domain.repository.CheckoutRepository
import javax.inject.Inject

class ConfirmBuyNowPurchaseUseCase @Inject constructor(
    private val checkoutRepository: CheckoutRepository
) {
    suspend operator fun invoke(request: PurchaseRequest): Result<PurchaseConfirmation> {
        return checkoutRepository.confirmBuyNowPurchase(request)
    }
}
