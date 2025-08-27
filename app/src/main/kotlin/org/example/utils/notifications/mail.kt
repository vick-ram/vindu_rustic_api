package org.example.utils.notifications

import org.example.domain.models.Notification
import org.example.domain.repo.NotificationRepository
import org.example.utils.EmailService

class EmailNotificationService(private val emailService: EmailService): NotificationRepository {
    override suspend fun send(notification: Notification) {
        emailService.sendEmail(
            to = notification.recipient,
            subject = notification.title,
            body = notification.message
        )
    }
}