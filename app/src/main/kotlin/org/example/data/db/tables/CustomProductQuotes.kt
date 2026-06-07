package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object CustomProductQuotes : CustomTable("custom_product_quotes") {
    val requestId = reference("request_id", CustomProductRequests)
    val quotedPrice = decimal("quoted_price", 12, 2)
    val currency = varchar("currency", 10).default("KES")
    val productionTimelineDays = integer("production_timeline_days").nullable()
    val description = text("description").nullable()
    val validUntil = timestampWithTimeZone("valid_until").nullable()
    val status = varchar("status", 50).default("SENT")
    val createdBy = reference("created_by", Users)
}