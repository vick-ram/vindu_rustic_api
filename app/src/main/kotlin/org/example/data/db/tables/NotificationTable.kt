package org.example.data.db.tables

import org.example.domain.models.NotificationChannel
import org.example.utils.CustomTable
import org.example.utils.PGEnum

object NotificationTable: CustomTable("notifications") {
    val title = varchar("title", 255)
    val message = text("message")
    val recipient = varchar("recipient", 255)
    val channel = customEnumeration(
        name = "channel",
        sql = "NotificationChannel",
        fromDb = { value -> NotificationChannel.valueOf(value as String) },
        toDb = { PGEnum("NotificationChannel", it) })
}