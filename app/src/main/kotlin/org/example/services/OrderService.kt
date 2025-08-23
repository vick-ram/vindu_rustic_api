package org.example.services

import org.example.domain.models.Order
import org.example.domain.models.OrderStatus
import org.example.domain.repo.OrderRepository

class OrderService(private val orderRepository: OrderRepository) {
    suspend fun createOrder(userId: String): Order {
        return orderRepository.createOrder(userId)
    }

    suspend fun getOrderById(id: String): Order? {
        return orderRepository.read(id)
    }

    suspend fun getOrderByUser(userId: String, offset: Int = 0, limit: Int = 50): List<Order> {
        return orderRepository.findByUserId(userId, offset, limit)
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Order? {
        return orderRepository.updateStatus(orderId, OrderStatus.valueOf(status))
    }

    suspend fun deleteOrder(orderId: String): Boolean {
        return orderRepository.delete(orderId)
    }
}