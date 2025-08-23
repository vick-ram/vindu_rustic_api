package org.example.domain.repo

import org.example.domain.models.Order
import org.example.domain.models.OrderStatus

interface OrderRepository: CrudRepository<Order, String> {
    suspend fun createOrder(userId: String): Order
    suspend fun findByUserId(userId: String, offset: Int = 0, limit: Int = 50): List<Order>
    suspend fun findByOrderNumber(orderNumber: String): Order?
    suspend fun updateStatus(orderId: String, status: OrderStatus): Order?
}