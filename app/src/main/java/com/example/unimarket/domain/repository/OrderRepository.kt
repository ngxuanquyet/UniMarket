package com.example.unimarket.domain.repository

import com.example.unimarket.domain.model.Order
import com.example.unimarket.domain.model.OrderPaymentCheckResult
import com.example.unimarket.domain.model.OrderStatus

interface OrderRepository {
    suspend fun getBuyerOrders(): Result<List<Order>>
    suspend fun getSellerOrders(): Result<List<Order>>
    suspend fun updateOrderStatus(order: Order, status: OrderStatus): Result<Unit>
    suspend fun checkTransferPayment(orderId: String): Result<OrderPaymentCheckResult>
}
