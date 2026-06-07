package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object ProductionJobs : CustomTable("production_jobs") {
    val orderItemId = reference("order_item_id", OrderItems)
    val productId = reference("product_id", Products)
    val assignedTo = reference("assigned_to", Users).nullable()
    val status = varchar("status", 50).default("QUEUED")
    val priority = varchar("priority", 50).default("NORMAL")
    val quantity = integer("quantity")
    val notes = text("notes").nullable()
    val startedAt = timestampWithTimeZone("started_at").nullable()
    val completedAt = timestampWithTimeZone("completed_at").nullable()
    val estimatedCompletionAt = timestampWithTimeZone("estimated_completion_at").nullable()
}