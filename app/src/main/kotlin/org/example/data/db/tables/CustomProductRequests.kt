package org.example.data.db.tables

import kotlinx.serialization.json.Json
import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.json.jsonb

object CustomProductRequests : CustomTable("custom_product_requests") {
    val userId = reference("user_id", Users)
    val title = varchar("title", 255)
    val description = text("description")
    val specifications = jsonb<Map<String, Any>>("specifications", Json).nullable()
    val estimatedBudgetMin = decimal("estimated_budget_min", 12, 2).nullable()
    val estimatedBudgetMax = decimal("estimated_budget_max", 12, 2).nullable()
    val status = varchar("status", 50).default("PENDING")
    val notes = text("admin_notes").nullable()
}
