package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object OrderStatusHistories : CustomTable("order_status_history") {
    val orderId = reference("order_id", Orders)
    val oldStatus = varchar("old_status", 50).nullable()
    val newStatus = varchar("new_status", 50)
    val changedBy = reference("changed_by", Users).nullable()
    val comment = text("comment").nullable()
}