package org.example.utils.notifications

import org.example.domain.models.AppNotification
import org.example.domain.repo.NotificationRepository
import org.example.services.NotificationService

class DatabaseNotificationService(
    private val notificationService: NotificationService
): NotificationRepository {
    override suspend fun send(appNotification: AppNotification) {
        notificationService.save(appNotification)
    }
}