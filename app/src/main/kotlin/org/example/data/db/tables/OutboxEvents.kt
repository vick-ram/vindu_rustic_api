package org.example.data.db.tables

import kotlinx.serialization.json.Json
import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import org.jetbrains.exposed.v1.json.jsonb

object OutboxEvents : CustomTable("outbox_events") {
    val aggregateType = varchar("aggregate_type", 100)
    val aggregateId = varchar("aggregate_id", 120)
    val eventType = varchar("event_type", 100)
    val payload = jsonb<Map<String, Any>>("payload", Json)
    val processed = bool("processed").default(false)
    val processedAt = timestampWithTimeZone("processed_at").nullable()
    val attempts = integer("attempts").default(0)
    val errorMessage = text("error_message").nullable()
}