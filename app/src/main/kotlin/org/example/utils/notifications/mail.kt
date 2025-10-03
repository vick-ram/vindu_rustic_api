package org.example.utils.notifications

import org.example.domain.models.AppNotification
import org.example.domain.repo.NotificationRepository
import org.example.utils.EmailService

class EmailNotificationService(private val emailService: EmailService): NotificationRepository {
    override suspend fun send(appNotification: AppNotification) {
        emailService.sendEmail(
            to = appNotification.metadata?.get("email").toString(),
            subject = appNotification.title,
            body = String.format(emailService.emailBody(), appNotification.message),
            isHtml = true
        )
    }
}