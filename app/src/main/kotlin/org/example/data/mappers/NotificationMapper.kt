package org.example.data.mappers

import org.example.data.db.entities.NotificationEntity
import org.example.domain.models.Notification
import org.example.domain.repo.EntityMapper

object NotificationMapper: EntityMapper<NotificationEntity, Notification, String> {
    override fun toModel(entity: NotificationEntity): Notification {
        return Notification(
            id = entity.id.value,
            title = entity.title,
            message = entity.message,
            recipient = entity.recipient,
            channel = entity.channel,
            createdAt = entity.createdAt
        )
    }

    override fun toEntity(
        model: Notification,
        entity: NotificationEntity
    ): NotificationEntity {
        return entity.apply {
            this.title = model.title
            this.message = model.message
            this.recipient = model.recipient
            this.channel = model.channel
        }
    }
}