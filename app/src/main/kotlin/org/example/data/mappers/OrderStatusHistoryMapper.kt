package org.example.data.mappers

import org.example.data.db.entities.OrderStatusHistoryEntity
import org.example.data.db.tables.Orders
import org.example.data.db.tables.Users
import org.example.domain.models.sales.OrderStatusHistory
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object OrderStatusHistoryMapper : EntityMapper<OrderStatusHistoryEntity, OrderStatusHistory, String> {
    override fun toModel(entity: OrderStatusHistoryEntity): OrderStatusHistory {
        return OrderStatusHistory(
            id = entity.id.value,
            orderId = entity.orderId.value,
            oldStatus = entity.oldStatus,
            newStatus = entity.newStatus,
            changedBy = entity.changedBy?.value,
            comment = entity.comment,
            createdAt = entity.createdAt
        )
    }

    override fun toEntity(
        model: OrderStatusHistory,
        entity: OrderStatusHistoryEntity
    ): OrderStatusHistoryEntity {
        return entity.apply {
            this.orderId = EntityID(model.orderId, Orders)
            this.oldStatus = model.oldStatus
            this.newStatus = model.newStatus
            this.changedBy = model.changedBy?.let { EntityID(it, Users) }
            this.comment = model.comment
        }
    }
}