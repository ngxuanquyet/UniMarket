package com.example.unimarket.domain.usecase.order

import com.example.unimarket.domain.model.OrderPaymentCheckResult
import com.example.unimarket.domain.repository.OrderRepository
import javax.inject.Inject

class CheckTransferPaymentUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: String): Result<OrderPaymentCheckResult> {
        return orderRepository.checkTransferPayment(orderId)
    }
}
