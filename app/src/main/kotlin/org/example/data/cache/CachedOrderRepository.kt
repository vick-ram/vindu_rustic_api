package org.example.data.cache

import org.example.domain.models.sales.Order
import org.example.domain.repo.CrudRepository
import org.example.domain.repo.OrderRepository

class CachedOrderRepository(
    private val delegate: OrderRepository,
    private val cache: CrudRepository<Order, String>
) : OrderRepository {
    override suspend fun create(entity: Order): Order = cache.create(entity)
    override suspend fun read(id: String): Order? = cache.read(id)
    override suspend fun readAll(offset: Int, limit: Int, queryParams: Map<String, String>?): List<Order> =
        cache.readAll(offset, limit, queryParams)
    override suspend fun update(id: String, entity: Order): Order? = cache.update(id, entity)
    override suspend fun delete(id: String): Boolean = cache.delete(id)

    override suspend fun findByOrderNumber(orderNumber: String): Order? = delegate.findByOrderNumber(orderNumber)
    override suspend fun findByUserId(userId: String, offset: Int, limit: Int): List<Order> =
        delegate.findByUserId(userId, offset, limit)
    override suspend fun findByStatus(status: String, offset: Int, limit: Int): List<Order> =
        delegate.findByStatus(status, offset, limit)
    override suspend fun updateOrderStatus(orderId: String, status: String, changedBy: String?): Boolean =
        delegate.updateOrderStatus(orderId, status, changedBy)
    override suspend fun getPendingFulfillment(): List<Map<String, Any>> = delegate.getPendingFulfillment()
}