package org.example.data.mappers

import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.DeviceTokenEntity
import org.example.data.db.entities.NotificationEntity
import org.example.data.db.entities.UserEntity
import org.example.data.db.tables.Users
import org.example.domain.models.system.Notification
import org.example.domain.models.system.DeviceToken
import org.example.domain.repo.EntityMapper
import org.example.utils.now
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.time.OffsetDateTime

object NotificationMapper : EntityMapper<NotificationEntity, Notification, String> {
    override fun toModel(entity: NotificationEntity): Notification {
        return Notification(
            id = entity.id.value,
            userId = entity.userId.value,
            type = entity.type,
            title = entity.title,
            body = entity.body,
            actionUrl = entity.actionUrl,
            referenceType = entity.referenceType,
            referenceId = entity.referenceId,
            isRead = entity.isRead,
            readAt = entity.readAt,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: Notification, entity: NotificationEntity): NotificationEntity {
        entity.userId = EntityID(model.userId, Users)
        entity.type = model.type
        entity.title = model.title
        entity.body = model.body
        entity.actionUrl = model.actionUrl
        entity.referenceType = model.referenceType
        entity.referenceId = model.referenceId
        entity.isRead = model.isRead
        entity.readAt = model.readAt
        return entity
    }
}

object DeviceTokenMapper : EntityMapper<DeviceTokenEntity, DeviceToken, String> {
    override fun toModel(entity: DeviceTokenEntity): DeviceToken {
        return DeviceToken(
            id = entity.id.value,
            userId = entity.user.id.value,
            token = entity.token,
            platform = entity.platform,
            createdAt = entity.createdAt
        )
    }

    override fun toEntity(
        model: DeviceToken,
        entity: DeviceTokenEntity
    ): DeviceTokenEntity {
        return entity.apply {
            this.user = UserEntity[model.userId]
            this.token = model.token
            this.platform = model.platform
            this.createdAt = OffsetDateTime.now()
        }
    }
}