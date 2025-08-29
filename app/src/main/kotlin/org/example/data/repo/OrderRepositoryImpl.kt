package org.example.data.repo

import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.CartEntity
import org.example.data.db.entities.OrderEntity
import org.example.data.db.entities.OrderItemEntity
import org.example.data.db.tables.CartTable
import org.example.data.db.tables.OrderTable
import org.example.data.mappers.OrderMapper
import org.example.domain.models.Order
import org.example.domain.models.OrderStatus
import org.example.domain.repo.OrderRepository
import org.example.plugins.NotFoundException
import org.example.utils.now
import org.example.utils.suspendTransaction

class OrderRepositoryImpl(private val orderMapper: OrderMapper) : CrudRepositoryImpl<OrderEntity, Order>(
    OrderEntity,
    Order::class
),
    OrderRepository {
    override fun OrderEntity.toDomain(): Order = orderMapper.toModel(this)

    override fun Order.toEntity(entity: OrderEntity) {
        orderMapper.toEntity(this, entity)
    }

    override suspend fun createOrder(userId: String): Order = suspendTransaction {
        val cart = CartEntity.find { CartTable.user.eq(userId) }
            .firstOrNull() ?: throw NotFoundException("Cart not found for user $userId")

        if (cart.items.empty()) throw IllegalArgumentException("Cannot create order from empty cart")

        val order = OrderEntity.new {
            user = cart.user
            totalAmount = cart.items.sumOf { it.product.basePrice * it.quantity.toBigDecimal() }
            status = OrderStatus.PENDING
        }

        cart.items.forEach { cartItem ->
            OrderItemEntity.new {
                this.order = order
                this.product = cartItem.product
                this.quantity = cartItem.quantity
                this.unitPrice = cartItem.product.basePrice
            }
        }

        cart.items.forEach { it.delete() }
        order.toDomain()
    }

    override suspend fun findByUserId(
        userId: String,
        offset: Int,
        limit: Int
    ): List<Order> = suspendTransaction {
        OrderEntity.find { OrderTable.user.eq(userId) }
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toDomain() }
    }

    override suspend fun findByOrderNumber(orderNumber: String): Order? = suspendTransaction {
        OrderEntity.find { OrderTable.orderNumber.eq(orderNumber) }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun updateStatus(
        orderId: String,
        status: OrderStatus
    ): Order? = suspendTransaction {
        OrderEntity.findByIdAndUpdate(orderId) { order ->
            order.status = status
            order.updatedAt = LocalDateTime.now()
        }?.toDomain()
    }
}