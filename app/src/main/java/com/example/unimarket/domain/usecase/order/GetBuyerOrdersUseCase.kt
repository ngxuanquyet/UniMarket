package com.example.unimarket.domain.usecase.order

import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.repository.OrderRepository
import javax.inject.Inject

class GetBuyerOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(): Result<List<Order>> {
        return orderRepository.getBuyerOrders()
    }
}
