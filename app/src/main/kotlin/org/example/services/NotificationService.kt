package org.example.services

import org.example.data.cache.NotificationCache
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.system.Notification
import java.math.BigDecimal

@Component
class NotificationService @Inject constructor(private val cache: NotificationCache) {
    suspend fun getNotification(id: String): Notification? = cache.read(id)
    suspend fun sendNotification(userId: String, type: String, title: String, body: String? = null, actionUrl: String? = null, referenceType: String? = null, referenceId: String? = null, metadata: Map<String, String> = emptyMap(), channels: List<String> = listOf("database")): List<Notification> =
        cache.sendNotification(userId, type, title, body, actionUrl, referenceType, referenceId, metadata, channels)
    suspend fun sendOrderStatusNotification(userId: String, orderId: String, orderNumber: String, status: String) = cache.sendOrderStatusNotification(userId, orderId, orderNumber, status)
    suspend fun sendPaymentConfirmation(userId: String, orderId: String, amount: BigDecimal, currency: String = "KES") = cache.sendPaymentConfirmation(userId, orderId, amount, currency)
    suspend fun sendShipmentNotification(userId: String, orderId: String, trackingNumber: String, courier: String?) = cache.sendShipmentNotification(userId, orderId, trackingNumber, courier)
    suspend fun sendPromotionNotification(userId: String, title: String, body: String, actionUrl: String? = null, campaignId: String? = null) = cache.sendPromotionNotification(userId, title, body, actionUrl, campaignId)
    suspend fun getUserNotifications(userId: String, type: String? = null, includeRead: Boolean = false, offset: Int = 0, limit: Int = 20): List<Notification> =
        cache.getUserNotifications(userId, type, includeRead, offset, limit)
    suspend fun markAsRead(notificationId: String): Notification? = cache.readNotification(notificationId)
    suspend fun markAllAsRead(userId: String, type: String? = null) = cache.readAllNotifications(userId, type)
    suspend fun getUnreadCount(userId: String): Long = cache.getUnreadCount(userId)
    suspend fun cleanupOldNotifications() = cache.cleanupOldNotifications()
    suspend fun deleteNotification(id: String): Boolean = cache.delete(id)
}
