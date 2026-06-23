package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.example.data.db.config.gsonJsonb
import org.example.domain.models.NotificationChannel
import org.example.domain.models.Platform
import org.example.data.db.config.PGEnum
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object Notifications: CustomTable("notifications") {
    val userId = reference("user_id", Users)
    val type = varchar("type", 100)
    val title = varchar("title", 255)
    val body = text("body").nullable()
    val actionUrl = text("action_url").nullable()
    val referenceType = varchar("reference_type", 100).nullable()
    val referenceId = varchar("reference_id", 120).nullable()
    val isRead = bool("is_read").default(false)
    val readAt = timestampWithTimeZone("read_at").nullable()
    val channel = customEnumeration(
        name = "channel",
        sql = "NotificationChannel",
        fromDb = { value -> NotificationChannel.valueOf(value as String) },
        toDb = { PGEnum("NotificationChannel", it) })
    val metadata = gsonJsonb<Map<String, Any>>("metadata").nullable()
}

object DeviceTokenTable: CustomTable("device_tokens") {
    val user = reference("user", Users)
    val token = varchar("token", 200)
    val platform = customEnumeration(
        name = "platform",
        sql = "Platform",
        fromDb = { value -> Platform.valueOf(value as String) },
        toDb = { PGEnum("Platform", it) })
    val isActive = bool("is_active").default(true)
}