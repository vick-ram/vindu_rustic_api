package org.example.data.repo

import org.example.data.db.entities.NotificationEntity
import org.example.data.mappers.NotificationMapper
import org.example.domain.models.Notification

class NotificationRepositoryImpl(private val notificationMapper: NotificationMapper) :
    CrudRepositoryImpl<NotificationEntity, Notification>(
        NotificationEntity
    ) {
    override fun NotificationEntity.toDomain(): Notification {
        return notificationMapper.toModel(this)
    }

    override fun Notification.toEntity(entity: NotificationEntity) {
        notificationMapper.toEntity(this, entity)
    }
}