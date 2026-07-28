package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.data.mappers.NotificationMapper
import org.example.data.repo.*
import org.example.domain.models.system.DispatchResult
import org.example.domain.models.system.Notification
import org.example.plugins.NotFoundException
import org.example.utils.notifications.EmailNotificationService
import org.example.utils.notifications.FcmNotificationService
import org.example.utils.notifications.SMSService
import org.koin.core.annotation.Single
import java.math.BigDecimal

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Single
class NotificationCache(
    redis: RedisCoroutinesCommands<String, String>,
    notificationMapper: NotificationMapper,
    private val notificationRepository: NotificationRepository,
    private val auditLogRepository: AuditLogRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val userRepository: UserRepository,
    private val pushService: FcmNotificationService,
    private val emailService: EmailNotificationService,
    private val smsService: SMSService
) : CrudCache<Notification, String>(
    redis = redis,
    delegate = notificationRepository,
    getId = { notification -> notificationMapper.getId(notification) as String },
    serializer = Notification.serializer(),
    config = object : CacheConfig {
        override val cacheName: String = "NotificationCache"
        override val ttl: Long = 1800L
    }
) {

    // Send notification to user via multiple channels
    suspend fun sendNotification(
        userId: String,
        type: String,
        title: String,
        body: String? = null,
        actionUrl: String? = null,
        referenceType: String? = null,
        referenceId: String? = null,
        metadata: Map<String, String> = emptyMap(),
        channels: List<String> = listOf("database")
    ): List<Notification> {
        val notifications = channels.map { channel ->
            Notification(
                userId = userId,
                type = type,
                title = title,
                body = body,
                actionUrl = actionUrl,
                referenceType = referenceType,
                referenceId = referenceId,
                metadata = metadata,
                channel = channel
            )
        }

        val createdNotifications = mutableListOf<Notification>()

        notifications.forEach { notification ->
            // Create notification with deduplication
            val created = notificationRepository.createIfNotDuplicate(notification)
            if (created != null) {
                createdNotifications.add(created)

                // Handle additional channel-specific logic
                when (notification.channel) {
                    "fcm" -> {
                        sendPushNotification(notification.id, userId, title, body ?: "", metadata)
                    }

                    "email" -> {
                        sendEmailNotification(notification.id, userId, title, body ?: "", actionUrl)
                    }

                    "sms" -> {
                        sendSmsNotification(notification.id, userId, body ?: title)
                    }

                    "database" -> {
                        // Already stored in database
                    }
                }
            }
        }

        // Log notification sending
        auditLogRepository.logAction(
            actorId = null,
            actorType = "system",
            action = "notification_sent",
            entityType = "notification",
            entityId = userId,
            metadata = mapOf(
                "type" to type,
                "channels" to channels.map { it },
                "count" to createdNotifications.size
            )
        )

        return createdNotifications
    }

    // Send order status notification
    suspend fun sendOrderStatusNotification(
        userId: String,
        orderId: String,
        orderNumber: String,
        status: String
    ) {
        val title = "Order $orderNumber Status Update"
        val body = "Your order #$orderNumber is now $status"

        sendNotification(
            userId = userId,
            type = "order_status",
            title = title,
            body = body,
            actionUrl = "/orders/$orderId",
            referenceType = "order",
            referenceId = orderId,
            channels = listOf(
                "database",
                "fcm",
                "email"
            )
        )
    }

    // Send payment confirmation
    suspend fun sendPaymentConfirmation(
        userId: String,
        orderId: String,
        amount: BigDecimal,
        currency: String = "KES"
    ) {
        val title = "Payment Confirmed"
        val body = "Your payment of $currency $amount has been confirmed"

        sendNotification(
            userId = userId,
            type = "payment_confirmation",
            title = title,
            body = body,
            actionUrl = "/orders/$orderId",
            referenceType = "order",
            referenceId = orderId,
            channels = listOf(
                "database", "email"
            )
        )
    }

    // Send shipment notification
    suspend fun sendShipmentNotification(
        userId: String,
        orderId: String,
        trackingNumber: String,
        courier: String?
    ) {
        val title = "Order Shipped!"
        val body = "Your order has been shipped${courier?.let { " via $it" } ?: ""}. Tracking number: $trackingNumber"

        sendNotification(
            userId = userId,
            type = "shipment",
            title = title,
            body = body,
            actionUrl = "/orders/$orderId/tracking",
            referenceType = "order",
            referenceId = orderId,
            channels = listOf(
                "database", "fcm", "email"
            )
        )
    }

    // Send promotional notification
    suspend fun sendPromotionNotification(
        userId: String,
        title: String,
        body: String,
        actionUrl: String? = null,
        campaignId: String? = null
    ) {
        sendNotification(
            userId = userId,
            type = "promotion",
            title = title,
            body = body,
            actionUrl = actionUrl,
            referenceType = "campaign",
            referenceId = campaignId,
            channels = listOf(
                "database", "fcm"
            )
        )
    }

    // Get user notifications with filtering
    suspend fun getUserNotifications(
        userId: String,
        type: String? = null,
        includeRead: Boolean = false,
        offset: Int = 0,
        limit: Int = 20
    ): List<Notification> {
        return if (type != null) {
            notificationRepository.findByType(userId, type, includeRead, offset, limit)
        } else {
            notificationRepository.findByUserId(userId, includeRead, offset, limit)
        }
    }

    // Mark notification as read
    suspend fun readNotification(notificationId: String): Notification? {
        return notificationRepository.markAsRead(notificationId)
    }

    // Mark all notifications as read
    suspend fun readAllNotifications(userId: String, type: String? = null) {
        if (type != null) {
            notificationRepository.markAllAsReadByType(userId, type)
        } else {
            notificationRepository.markAllAsRead(userId)
        }
    }

    // Get notification badge count
    suspend fun getUnreadCount(userId: String): Long {
        return notificationRepository.getUnreadCount(userId)
    }

    // Cleanup old notifications
    suspend fun cleanupOldNotifications() {
        val deleted = notificationRepository.deleteOldNotifications(30) // Delete notifications older than 30 days

        auditLogRepository.logSystemAction(
            action = "notification_cleanup",
            entityType = "notification",
            entityId = "cleanup",
            metadata = mapOf(
                "deleted_count" to deleted,
                "older_than_days" to 30
            )
        )
    }

    // Private helper methods
    private suspend fun sendPushNotification(
        notificationId: String,
        userId: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): DispatchResult {
        return try {
            val tokens = deviceTokenRepository.findByUserId(userId).map { it.token }

            return pushService.send(
                notificationId = notificationId,
                tokens = tokens,
                title = title,
                body = body,
                data = data,
                deviceTokenRepository = deviceTokenRepository
            )
        } catch (e: Exception) {
            // Log failure but don't throw - other channels should still work
            auditLogRepository.logSystemAction(
                action = "push_notification_failed",
                entityType = "notification",
                entityId = userId,
                metadata = mapOf(
                    "error" to (e.message ?: "Unknown error"),
                )
            )

            DispatchResult(
                notificationId = notificationId,
                channel = "fcm", // Assumes PUSH exists in your enum
                success = false,
                message = e.message ?: "Unknown push notification error"
            )
        }
    }

    private suspend fun sendEmailNotification(
        notificationId: String,
        userId: String,
        subject: String,
        body: String,
        actionUrl: String?
    ): DispatchResult {
        return try {
            val user = userRepository.read(userId) ?: throw NotFoundException("user with $userId not found")
            return emailService.send(notificationId, to = user.email, subject = subject, body = body, actionUrl = actionUrl)
        } catch (e: Exception) {
            auditLogRepository.logSystemAction(
                action = "email_notification_failed",
                entityType = "notification",
                entityId = userId,
                metadata = mapOf(
                    "error" to (e.message ?: "Unknown error")
                )
            )

            DispatchResult(
                notificationId = notificationId,
                channel = "email",
                success = false,
                message = e.message ?: "Unknown email notification error"
            )
        }
    }

    private suspend fun sendSmsNotification(
        notificationId: String,
        userId: String,
        message: String
    ): DispatchResult {
        return try {
            // Get user phone and send SMS
            val user = userRepository.read(userId) ?: throw NotFoundException("user with $userId not found")
            smsService.send(notificationId, user.phoneNumber, message)
        } catch (e: Exception) {
            auditLogRepository.logSystemAction(
                action = "sms_notification_failed",
                entityType = "notification",
                entityId = userId,
                metadata = mapOf(
                    "error" to (e.message ?: "Unknown error")
                )
            )

            DispatchResult(
                notificationId = notificationId,
                channel = "sms",
                success = false,
                message = e.message ?: "Unknown sms notification error"
            )
        }
    }
}