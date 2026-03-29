package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.PurchaseConfirmation
import com.example.unimarket.domain.model.PurchaseRequest

interface CheckoutRepository {
    suspend fun confirmBuyNowPurchase(request: PurchaseRequest): Result<PurchaseConfirmation>
}
