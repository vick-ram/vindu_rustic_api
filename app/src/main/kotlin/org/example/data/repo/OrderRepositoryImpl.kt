package org.example.data.repo

import org.example.data.db.entities.OrderEntity
import org.example.data.db.entities.OrderStatusHistoryEntity
import org.example.data.db.tables.Orders
import org.example.data.db.tables.Users
import org.example.data.mappers.OrderMapper
import org.example.data.mappers.OrderStatusHistoryMapper
import org.example.domain.models.sales.Order
import org.example.domain.repo.OrderRepository
import org.example.plugins.NotFoundException
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class OrderRepositoryImpl(private val orderMapper: OrderMapper, private val orderStatusHistoryMapper: OrderStatusHistoryMapper) : CrudRepositoryImpl<OrderEntity, Order>(
    OrderEntity,
    Order::class
),
    OrderRepository {
    override fun OrderEntity.toDomain(): Order = orderMapper.toModel(this)

    override fun Order.toEntity(entity: OrderEntity) {
        orderMapper.toEntity(this, entity)
    }

    override fun getId(domain: Order): String = domain.id

    override suspend fun findByOrderNumber(orderNumber: String): Order? = suspendTransaction {
        OrderEntity.find { Orders.orderNumber eq orderNumber }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findByUserId(userId: String, offset: Int, limit: Int): List<Order> = suspendTransaction {
        OrderEntity.find { Orders.userId eq userId }
            .orderBy(Orders.placedAt to SortOrder.DESC)
            .offset(offset.toLong())
            .limit(limit)
            .map { it.toDomain() }
    }

    override suspend fun findByStatus(status: String, offset: Int, limit: Int): List<Order> = suspendTransaction {
        OrderEntity.find { Orders.status eq status }
            .orderBy(Orders.placedAt to SortOrder.DESC)
            .offset(offset.toLong())
            .limit(limit)
            .map { it.toDomain() }
    }

    override suspend fun updateOrderStatus(orderId: String, status: String, changedBy: String?): Boolean = suspendTransaction {
        val order = OrderEntity.findById(orderId) ?: throw NotFoundException("Order not found")
        val oldStatus = order.status
        order.status = status

        // Record status change
        OrderStatusHistoryEntity.new {
            this.orderId = order.id
            this.oldStatus = oldStatus
            this.newStatus = status
            this.changedBy = changedBy?.let { EntityID(it, Users) }
        }
        true
    }

    override suspend fun getPendingFulfillment(): List<Map<String, Any>> = suspendTransaction {
        exec("SELECT * FROM pending_fulfillment") { rs ->
            val results = mutableListOf<Map<String, Any>>()
            while (rs.next()) {
                val row = mutableMapOf<String, Any>()
                for (i in 1..rs.metaData.columnCount) {
                    row[rs.metaData.getColumnName(i)] = rs.getObject(i) ?: ""
                }
                results.add(row)
            }
            results
        } ?: emptyList()
    }
}