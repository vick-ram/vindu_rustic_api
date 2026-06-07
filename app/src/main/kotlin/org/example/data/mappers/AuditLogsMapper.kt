package org.example.data.mappers

import org.example.data.db.entities.AuditLogsEntity
import org.example.data.db.tables.Users
import org.example.domain.models.system.AuditLogs
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object AuditLogsMapper : EntityMapper<AuditLogsEntity, AuditLogs, String> {
    override fun toModel(entity: AuditLogsEntity): AuditLogs {
        return AuditLogs(
            id = entity.id.value,
            actorId = entity.actorId?.value,
            actorType = entity.actorType,
            action = entity.action,
            entityType = entity.entityType,
            entityId = entity.entityId,
            changes = entity.changes,
            metadata = entity.metadata,
            ipAddress = entity.ipAddress,
            userAgent = entity.userAgent,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: AuditLogs, entity: AuditLogsEntity): AuditLogsEntity {
        entity.actorId = model.actorId?.let { EntityID(it, Users) }
        entity.actorType = model.actorType
        entity.action = model.action
        entity.entityType = model.entityType
        entity.entityId = model.entityId
        entity.changes = model.changes
        entity.metadata = model.metadata
        entity.ipAddress = model.ipAddress
        entity.userAgent = model.userAgent
        return entity
    }
}