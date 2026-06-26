package org.example.utils.notifications

import org.example.data.db.entities.NotificationEntity
import org.example.data.db.tables.Notifications
import org.example.data.mappers.NotificationMapper
import org.example.data.repo.CrudRepository
import org.example.domain.models.NotificationChannel
import org.example.domain.models.system.DispatchResult
import org.example.domain.models.system.Notification
import org.example.domain.repo.NotificationRepository
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

class DatabaseNotificationService(private val notificationMapper: NotificationMapper): CrudRepository<NotificationEntity, Notification>(
    NotificationEntity,
    Notification::class
), NotificationRepository {
    private val logger = LoggerFactory.getLogger(DatabaseNotificationService::class.java)

    override fun NotificationEntity.toDomain(): Notification {
        return notificationMapper.toModel(this)
    }

    override fun Notification.toEntity(entity: NotificationEntity) {
        notificationMapper.toEntity(this, entity)
    }

    override fun getId(domain: Notification): String = domain.id

    override suspend fun send(
        notification: Notification,
        email: String?,
        phoneNumber: String?
    ): DispatchResult = suspendTransaction {
        create(notification)
        logger.info("Notification ${notification.id} stored in database")
         DispatchResult(
            notificationId = notification.id,
            channel = NotificationChannel.DATABASE,
            success = true,
            message = "Stored in database"
        )
    }

    override suspend fun markAsRead(notificationId: String, userId: String) {
        suspendTransaction {
            val existingNotification = read(notificationId)
            existingNotification?.let {
                update(notificationId, it.copy(
                    isRead = true,
                    readAt = OffsetDateTime.now(),
                ))
            }
        }
    }

    override suspend fun getUnreadCount(userId: String): Long = suspendTransaction {
        Notifications.selectAll()
            .where { Notifications.isRead eq false  }
            .count()
    }

}