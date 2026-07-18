package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.NotificationMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.NotificationChannel
import org.example.domain.models.system.Notification
import org.koin.core.annotation.Single
import java.time.OffsetDateTime

@Component
class NotificationRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    notificationMapper: NotificationMapper
) : CrudRepository<Notification, String>(
    connectionFactory = connectionFactory,
    tableName = "notifications",
    mapper = notificationMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    // Override update to handle updated_at
    override suspend fun update(id: String, model: Notification): Notification? {
        return super.update(id, model.copy(updatedAt = OffsetDateTime.now()))
    }

    // Get notifications for a user
    suspend fun findByUserId(
        userId: String,
        includeRead: Boolean = false,
        offset: Int = 0,
        limit: Int = 20
    ): List<Notification> {
        val readFilter = if (!includeRead) "AND is_read = false" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
            $readFilter
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "userId" to userId,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get notifications by type
    suspend fun findByType(
        userId: String,
        type: String,
        includeRead: Boolean = false,
        offset: Int = 0,
        limit: Int = 20
    ): List<Notification> {
        val readFilter = if (!includeRead) "AND is_read = false" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
              AND type = :type 
              $readFilter
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "userId" to userId,
            "type" to type,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get notifications by channel
    suspend fun findByChannel(
        userId: String,
        channel: NotificationChannel,
        includeRead: Boolean = false,
        offset: Int = 0,
        limit: Int = 20
    ): List<Notification> {
        val readFilter = if (!includeRead) "AND is_read = false" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
              AND channel = :channel 
              $readFilter
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "userId" to userId,
            "channel" to channel.name,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get notifications by reference
    suspend fun findByReference(
        userId: String,
        referenceType: String,
        referenceId: String,
        includeRead: Boolean = true
    ): List<Notification> {
        val readFilter = if (!includeRead) "AND is_read = false" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
              AND reference_type = :referenceType 
              AND reference_id = :referenceId 
              $readFilter
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "userId" to userId,
            "referenceType" to referenceType,
            "referenceId" to referenceId
        ))
    }

    // Get unread notification count
    suspend fun getUnreadCount(userId: String): Long {
        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName 
            WHERE user_id = :userId 
              AND is_read = false
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("userId", userId)
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("count", Long::class.java) }
                .awaitFirstOrNull() ?: 0L
        }
    }

    // Get unread count by type
    suspend fun getUnreadCountByType(userId: String): Map<String, Long?> {
        val sql = """
            SELECT 
                type,
                COUNT(*) as count
            FROM $tableName 
            WHERE user_id = :userId 
              AND is_read = false
            GROUP BY type
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("userId", userId)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    row.get("type", String::class.java) to
                            row.get("count", Long::class.java)
                }
                .asFlow()
                .toList()
                .toMap()
        }
    }

    // Mark notification as read
    suspend fun markAsRead(id: String): Notification? {
        val sql = """
            UPDATE $tableName 
            SET is_read = true,
                read_at = :readAt,
                updated_at = :updatedAt
            WHERE id = :id 
              AND is_read = false
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val now = OffsetDateTime.now()
            connection.createStatement(sql)
                .bind("id", id)
                .bind("readAt", now)
                .bind("updatedAt", now)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Mark all notifications as read for a user
    suspend fun markAllAsRead(userId: String): Int {
        val sql = """
            UPDATE $tableName 
            SET is_read = true,
                read_at = :readAt,
                updated_at = :updatedAt
            WHERE user_id = :userId 
              AND is_read = false
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val now = OffsetDateTime.now()
            connection.createStatement(sql)
                .bind("userId", userId)
                .bind("readAt", now)
                .bind("updatedAt", now)
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Mark notifications as read by type
    suspend fun markAllAsReadByType(userId: String, type: String): Int {
        val sql = """
            UPDATE $tableName 
            SET is_read = true,
                read_at = :readAt,
                updated_at = :updatedAt
            WHERE user_id = :userId 
              AND type = :type 
              AND is_read = false
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val now = OffsetDateTime.now()
            connection.createStatement(sql)
                .bind("userId", userId)
                .bind("type", type)
                .bind("readAt", now)
                .bind("updatedAt", now)
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Create notification with deduplication check
    suspend fun createIfNotDuplicate(
        notification: Notification,
        deduplicateWithinMinutes: Long = 5
    ): Notification? {
        // Check for duplicate notifications within the time window
        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName 
            WHERE user_id = :userId 
              AND type = :type 
              AND reference_type = :referenceType 
              AND reference_id = :referenceId 
              AND created_at > :cutoffTime
        """.trimIndent()

        val isDuplicate = connectionFactory.useConnection {
            createStatement(sql)
                .bind("userId", notification.userId)
                .bind("type", notification.type)
                .bind("referenceType", notification.referenceType ?: String::class.java)
                .bind("referenceId", notification.referenceId ?: String::class.java)
                .bind("cutoffTime", OffsetDateTime.now().minusMinutes(deduplicateWithinMinutes))
                .execute()
                .awaitSingle()
                .map { row, _ -> (row.get("count", Long::class.java)?: 0L) > 0 }
                .awaitFirstOrNull() ?: false
        }

        return if (!isDuplicate) {
            create(notification)
        } else {
            null
        }
    }

    // Bulk create notifications
    suspend fun bulkCreate(notifications: List<Notification>): List<Notification> {
        if (notifications.isEmpty()) return emptyList()

        val columns = listOf(
            "user_id", "type", "title", "body", "action_url",
            "reference_type", "reference_id", "metadata",
            "channel", "is_read"
        )

        val placeholders = List(notifications.size) { index ->
            "(${columns.joinToString(", ") { ":${it}_$index" }})"
        }

        val sql = """
            INSERT INTO $tableName (${columns.joinToString(", ")})
            VALUES ${placeholders.joinToString(", ")}
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val statement = connection.createStatement(sql)
            notifications.forEachIndexed { index, notification ->
                statement.bind("user_id_$index", notification.userId)
                statement.bind("type_$index", notification.type)
                statement.bind("title_$index", notification.title)
                statement.bind("body_$index", notification.body ?: String::class.java)
                statement.bind("action_url_$index", notification.actionUrl ?: String::class.java)
                statement.bind("reference_type_$index", notification.referenceType ?: String::class.java)
                statement.bind("reference_id_$index", notification.referenceId ?: String::class.java)
                statement.bind("metadata_$index", notification.metadata)
                statement.bind("channel_$index", notification.channel.name)
                statement.bind("is_read_$index", notification.isRead)
            }

            statement.execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    // Delete old read notifications
    suspend fun deleteOldNotifications(olderThanDays: Int): Int {
        val sql = """
            DELETE FROM $tableName 
            WHERE is_read = true 
              AND read_at < :cutoffDate
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("cutoffDate", OffsetDateTime.now().minusDays(olderThanDays.toLong()))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Delete all notifications for a user
    suspend fun deleteAllForUser(userId: String): Int {
        val sql = """
            DELETE FROM $tableName 
            WHERE user_id = :userId
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("userId", userId)
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Get notification statistics
    suspend fun getNotificationStats(userId: String): NotificationStats {
        val sql = """
            SELECT 
                COUNT(*) as total_notifications,
                COUNT(CASE WHEN is_read = false THEN 1 END) as unread,
                COUNT(CASE WHEN is_read = true THEN 1 END) as read_count,
                COUNT(DISTINCT type) as unique_types,
                COUNT(CASE WHEN channel = 'FCM' THEN 1 END) as fcm_count,
                COUNT(CASE WHEN channel = 'EMAIL' THEN 1 END) as email_count,
                COUNT(CASE WHEN channel = 'DATABASE' THEN 1 END) as database_count,
                COUNT(CASE WHEN channel = 'SMS' THEN 1 END) as sms_count
            FROM $tableName 
            WHERE user_id = :userId
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("userId", userId)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    NotificationStats(
                        totalNotifications = (row.get("total_notifications", Long::class.java) ?: 0L).toInt(),
                        unread = (row.get("unread", Long::class.java) ?: 0L).toInt(),
                        read = (row.get("read_count", Long::class.java) ?: 0L).toInt(),
                        uniqueTypes = (row.get("unique_types", Long::class.java) ?: 0L).toInt(),
                        fcmCount = (row.get("fcm_count", Long::class.java) ?: 0L).toInt(),
                        emailCount = (row.get("email_count", Long::class.java) ?: 0L).toInt(),
                        databaseCount = (row.get("database_count", Long::class.java) ?: 0L).toInt(),
                        smsCount =( row.get("sms_count", Long::class.java) ?: 0L).toInt()
                    )
                }
                .awaitFirstOrNull() ?: NotificationStats(0, 0, 0, 0, 0, 0, 0, 0)
        }
    }

    // Get notifications by date range
    suspend fun findByDateRange(
        userId: String,
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        type: String? = null,
        channel: NotificationChannel? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<Notification> {
        val conditions = mutableListOf(
            "user_id = :userId",
            "created_at BETWEEN :startDate AND :endDate"
        )
        val params = mutableMapOf<String, Any>(
            "userId" to userId,
            "startDate" to startDate,
            "endDate" to endDate,
            "limit" to limit,
            "offset" to offset.toLong()
        )

        type?.let {
            conditions.add("type = :type")
            params["type"] = it
        }

        channel?.let {
            conditions.add("channel = :channel")
            params["channel"] = it.name
        }

        val sql = """
            SELECT * FROM $tableName 
            WHERE ${conditions.joinToString(" AND ")} 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, params)
    }

    // Search notifications
    suspend fun searchNotifications(
        userId: String,
        query: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Notification> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
              AND (title ILIKE :query 
                   OR body ILIKE :query 
                   OR type ILIKE :query) 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "userId" to userId,
            "query" to "%$query%",
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get recent notifications with grouping
    suspend fun getRecentGrouped(
        userId: String,
        limit: Int = 20
    ): List<NotificationGroup> {
        val sql = """
            WITH recent_notifications AS (
                SELECT *,
                       DATE(created_at) as notification_date,
                       ROW_NUMBER() OVER (PARTITION BY DATE(created_at) ORDER BY created_at DESC) as rn
                FROM $tableName 
                WHERE user_id = :userId 
                ORDER BY created_at DESC 
                LIMIT :limit
            )
            SELECT 
                notification_date,
                COUNT(*) as count,
                ARRAY_AGG(
                    JSON_BUILD_OBJECT(
                        'id', id,
                        'type', type,
                        'title', title,
                        'body', body,
                        'is_read', is_read,
                        'created_at', created_at
                    ) ORDER BY created_at DESC
                ) as notifications_json
            FROM recent_notifications
            GROUP BY notification_date
            ORDER BY notification_date DESC
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("userId", userId)
                .bind("limit", limit)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    NotificationGroup(
                        date = row.get("notification_date", java.time.LocalDate::class.java),
                        count = row.get("count", Long::class.java).toInt(),
                        notifications = emptyList() // Parse JSON array if needed
                    )
                }
                .asFlow()
                .toList()
        }
    }
}

data class NotificationStats(
    val totalNotifications: Int,
    val unread: Int,
    val read: Int,
    val uniqueTypes: Int,
    val fcmCount: Int,
    val emailCount: Int,
    val databaseCount: Int,
    val smsCount: Int
)

data class NotificationGroup(
    val date: java.time.LocalDate,
    val count: Int,
    val notifications: List<Notification>
)