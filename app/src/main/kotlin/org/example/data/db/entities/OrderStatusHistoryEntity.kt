package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.OrderStatusHistories
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class OrderStatusHistoryEntity(id: EntityID<String>) : CustomEntity(id, OrderStatusHistories) {
    companion object : CustomEntityClass<OrderStatusHistoryEntity>(OrderStatusHistories)

    var orderId by OrderStatusHistories.orderId
    var oldStatus by OrderStatusHistories.oldStatus
    var newStatus by OrderStatusHistories.newStatus
    var changedBy by OrderStatusHistories.changedBy
    var comment by OrderStatusHistories.comment
}