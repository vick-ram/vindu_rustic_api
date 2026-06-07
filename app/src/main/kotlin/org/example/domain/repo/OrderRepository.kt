package org.example.domain.repo

import org.example.domain.models.sales.Order
import org.example.domain.models.sales.OrderStatus

interface OrderRepository: CrudRepository<Order, String> {
    suspend fun findByUserId(userId: String, offset: Int = 0, limit: Int = 50): List<Order>
    suspend fun findByOrderNumber(orderNumber: String): Order?
    suspend fun findByStatus(status: String, offset: Int, limit: Int): List<Order>
    suspend fun updateOrderStatus(orderId: String, status: String, changedBy: String?): Boolean
    suspend fun getPendingFulfillment(): List<Map<String, Any>>
}