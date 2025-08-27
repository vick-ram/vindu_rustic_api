package org.example.utils.notifications

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.example.domain.models.Notification
import org.example.domain.repo.NotificationRepository

class FcmNotificationService(): NotificationRepository {
    override suspend fun send(notification: Notification) {
        val message = Message.builder()
            .setToken(notification.recipient)
            .putData("title", notification.title)
            .putData("body", notification.message)
            .build()
        val response = FirebaseMessaging.getInstance().send(message)
        println("✅ Sent FCM message: $response")
    }
}