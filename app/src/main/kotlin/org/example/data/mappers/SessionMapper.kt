package org.example.data.mappers

import org.example.data.db.entities.SessionEntity
import org.example.data.db.tables.Users
import org.example.domain.models.identity.Session
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object SessionMapper : EntityMapper<SessionEntity, Session, String> {
    override fun toModel(entity: SessionEntity): Session {
        return Session(
            id = entity.id.value,
            userId = entity.userId.value,
            refreshToken = entity.refreshTokenHash,
            ipAddress = entity.ipAddress,
            userAgent = entity.userAgent,
            expiresAt = entity.expiresAt,
            createdAt = entity.createdAt
        )
    }

    override fun toEntity(model: Session, entity: SessionEntity): SessionEntity {
        entity.userId = EntityID(model.userId, Users)
        entity.refreshTokenHash = model.refreshToken
        entity.ipAddress = model.ipAddress
        entity.userAgent = model.userAgent
        entity.expiresAt = model.expiresAt
        return entity
    }
}