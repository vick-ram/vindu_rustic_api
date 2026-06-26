package org.example.services

import org.example.domain.models.sales.Order
import org.example.domain.models.sales.OrderStatus

class OrderService(private val orderRepository: OrderRepository) {
    suspend fun createOrder(userId: String): Order {
        return orderRepository.createOrder(userId)
    }

    suspend fun getOrders(offset: Int, limit: Int, queryParams: Map<String, String>?): List<Order> {
        return orderRepository.readAll(offset, limit, queryParams)
    }

    suspend fun getOrderById(id: String): Order? {
        return orderRepository.read(id)
    }

    suspend fun getOrderByUser(userId: String, offset: Int = 0, limit: Int = 50): List<Order> {
        return orderRepository.findByUserId(userId, offset, limit)
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Order? {
        return orderRepository.updateStatus(orderId, status)
    }

    suspend fun deleteOrder(orderId: String): Boolean {
        return orderRepository.delete(orderId)
    }
}