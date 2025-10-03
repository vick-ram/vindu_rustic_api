package org.example.data.mappers

import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.DeviceTokenEntity
import org.example.data.db.entities.NotificationEntity
import org.example.data.db.entities.UserEntity
import org.example.domain.models.AppNotification
import org.example.domain.models.DeviceToken
import org.example.domain.repo.EntityMapper
import org.example.utils.now

object NotificationMapper: EntityMapper<NotificationEntity, AppNotification, String> {
    override fun toModel(entity: NotificationEntity): AppNotification {
        return AppNotification(
            id = entity.id.value,
            title = entity.title,
            message = entity.message,
            topic = entity.topic,
            channel = entity.channel,
            metadata = entity.metadata,
            createdAt = entity.createdAt
        )
    }

    override fun toEntity(
        model: AppNotification,
        entity: NotificationEntity
    ): NotificationEntity {
        return entity.apply {
            this.title = model.title
            this.message = model.message
            this.topic = model.topic
            this.channel = model.channel
            this.metadata = model.metadata
            this.createdAt = LocalDateTime.now()
        }
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
            this.createdAt = LocalDateTime.now()
        }
    }
}