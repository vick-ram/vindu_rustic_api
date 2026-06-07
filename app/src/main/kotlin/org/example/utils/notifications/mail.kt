package org.example.utils.notifications

import org.example.domain.models.system.Notification
import org.example.domain.repo.NotificationRepository
import org.example.utils.EmailService

class EmailNotificationService(private val emailService: EmailService): NotificationRepository {
    override suspend fun send(notification: Notification) {
        emailService.sendEmail(
            to = notification.metadata?.get("email").toString(),
            subject = notification.title,
            body = String.format(emailService.emailBody(), notification.body),
            isHtml = true
        )
    }
}