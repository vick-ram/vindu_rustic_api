package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.example.data.db.config.gsonJsonb
import org.example.data.db.config.inet

object AuditLogs : CustomTable("audit_logs") {
    val actorId = reference("actor_id", Users).nullable()
    val actorType = varchar("actor_type", 50)
    val action = varchar("action", 100)
    val entityType = varchar("entity_type", 100)
    val entityId = varchar("entity_id", 120)
    val changes = gsonJsonb<Map<String, Any>>("changes").nullable()
    val metadata = gsonJsonb<Map<String, Any>>("metadata").nullable()
    val ipAddress = inet("ip_address").nullable()
    val userAgent = text("user_agent").nullable()
}