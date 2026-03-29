package com.example.unimarket.domain.usecase.order

import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderStatus
import com.example.unimarket.domain.repository.OrderRepository
import javax.inject.Inject

class UpdateOrderStatusUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(order: Order, status: OrderStatus): Result<Unit> {
        return orderRepository.updateOrderStatus(order, status)
    }
}
