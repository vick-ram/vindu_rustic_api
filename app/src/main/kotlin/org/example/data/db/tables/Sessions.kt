package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.example.data.db.config.inet
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object Sessions: CustomTable("sessions") {
    val userId = reference("user_id", Users)
    val refreshTokenHash = text("refresh_token_hash")
    val ipAddress = inet("ip_address").nullable()
    val userAgent = text("user_agent").nullable()
    val expiresAt = timestampWithTimeZone("expires_at")
}