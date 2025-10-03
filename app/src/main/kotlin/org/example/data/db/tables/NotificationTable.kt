package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.example.data.db.config.gsonJsonb
import org.example.domain.models.NotificationChannel
import org.example.domain.models.Platform
import org.example.utils.PGEnum
import org.jetbrains.exposed.v1.json.jsonb

object NotificationTable: CustomTable("notifications") {
    val title = varchar("title", 255)
    val message = text("message")
    val topic = varchar("topic", 100).nullable()
    val channel = customEnumeration(
        name = "channel",
        sql = "NotificationChannel",
        fromDb = { value -> NotificationChannel.valueOf(value as String) },
        toDb = { PGEnum("NotificationChannel", it) })
    val imageUrl = varchar("image_url", 100).nullable()
    val metadata = gsonJsonb<Map<String, Any>>("metadata").nullable()
}

object DeviceTokenTable: CustomTable("device_tokens") {
    val user = reference("user", UserTable)
    val token = varchar("token", 200)
    val platform = customEnumeration(
        name = "platform",
        sql = "Platform",
        fromDb = { value -> Platform.valueOf(value as String) },
        toDb = { PGEnum("Platform", it) })
}