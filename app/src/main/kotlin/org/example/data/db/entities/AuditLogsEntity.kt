package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.AuditLogs
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class AuditLogsEntity(id: EntityID<String>) : CustomEntity(id, AuditLogs) {
    companion object : CustomEntityClass<AuditLogsEntity>(AuditLogs)

    var actorId by AuditLogs.actorId
    var actorType by AuditLogs.actorType
    var action by AuditLogs.action
    var entityType by AuditLogs.entityType
    var entityId by AuditLogs.entityId
    var changes by AuditLogs.changes
    var metadata by AuditLogs.metadata
    var ipAddress by AuditLogs.ipAddress
    var userAgent by AuditLogs.userAgent
}