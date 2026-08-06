package org.example.services

import kotlinx.coroutines.flow.toList
import org.example.data.repo.OrderConfirmation
import org.example.data.repo.OrderDetails
import org.example.data.repo.OrderRepository
import org.example.domain.models.sales.Order

class OrderService(
    private val orderRepository: OrderRepository
) {
    suspend fun placeOrder(
        userId: String?,
        cartId: String,
        shippingAddressId: String,
        billingAddressId: String? = null,
        couponCode: String? = null,
        notes: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ): OrderConfirmation {
        return orderRepository.createOrder(
            userId,
            cartId,
            shippingAddressId,
            billingAddressId,
            couponCode,
            notes,
            ipAddress,
            userAgent
        )
    }

    suspend fun getOrders(offset: Int, limit: Int, queryParams: Map<String, String>?): List<Order> {
        return orderRepository.readAll(offset, limit, queryParams).toList()
    }

    suspend fun getOrderById(id: String): OrderDetails? {
        return orderRepository.getOrderWithDetails(id)
    }

    suspend fun getOrderByUser(userId: String, offset: Int = 0, limit: Int = 50): List<Order> {
        return orderRepository.findByUserId(userId, offset, limit)
    }

    suspend fun getOrdersByPaymentStatus(
        paymentStatus: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Order> {
        return orderRepository.findByPaymentStatus(paymentStatus, offset, limit)
    }

    suspend fun updateOrderStatus(
        id: String,
        status: String,
        paymentStatus: String? = null,
        fulfillmentStatus: String? = null
    ): Order? {
        return orderRepository.updateStatus(id, status, paymentStatus, fulfillmentStatus)
    }

    suspend fun deleteOrder(orderId: String): Boolean {
        return orderRepository.delete(orderId)
    }
}

