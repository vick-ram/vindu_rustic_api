package org.example.domain.repo

import org.example.domain.models.Order
import org.example.domain.models.OrderStatus

interface OrderRepository: CrudRepository<Order, String> {
    suspend fun createOrder(userId: String): Order
    suspend fun findByUserId(userId: String, offset: Int = 0, limit: Int = 50): List<Order>
    suspend fun findByOrderNumber(orderNumber: String): Order?
    suspend fun updateStatus(orderId: String, status: OrderStatus): Order?
}
class CachedOrder(
    private val delegate: OrderRepository,
    private val cache: CrudRepository<Order, String>
): OrderRepository {
    override suspend fun createOrder(userId: String): Order {
        return delegate.createOrder(userId)
    }

    override suspend fun findByUserId(
        userId: String,
        offset: Int,
        limit: Int
    ): List<Order> {
        return delegate.findByUserId(userId, offset, limit)
    }

    override suspend fun findByOrderNumber(orderNumber: String): Order? {
        return delegate.findByOrderNumber(orderNumber)
    }

    override suspend fun updateStatus(
        orderId: String,
        status: OrderStatus
    ): Order? {
        return delegate.updateStatus(orderId, status)
    }

    override suspend fun create(entity: Order): Order {
        return cache.create(entity)
    }

    override suspend fun read(id: String): Order? {
        return cache.read(id)
    }

    override suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<Order> {
        return cache.readAll(offset, limit, queryParams)
    }

    override suspend fun update(
        id: String,
        entity: Order
    ): Order? {
        return cache.update(id, entity)
    }

    override suspend fun delete(id: String): Boolean {
        return cache.delete(id)
    }
}