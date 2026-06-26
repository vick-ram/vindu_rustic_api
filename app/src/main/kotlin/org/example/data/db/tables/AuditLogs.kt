package org.example.data.db.tables

import kotlinx.serialization.json.Json
import org.example.data.db.config.CustomTable
import org.example.data.db.config.inet
import org.jetbrains.exposed.v1.json.jsonb

object AuditLogs : CustomTable("audit_logs") {
    val actorId = reference("actor_id", Users).nullable()
    val actorType = varchar("actor_type", 50)
    val action = varchar("action", 100)
    val entityType = varchar("entity_type", 100)
    val entityId = varchar("entity_id", 120)
    val changes = jsonb<Map<String, Any>>("changes", Json).nullable()
    val metadata = jsonb<Map<String, Any>>("metadata", Json).nullable()
    val ipAddress = inet("ip_address").nullable()
    val userAgent = text("user_agent").nullable()
}