package org.example.domain.models.system

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.domain.models.NotificationChannel
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class Notification(
    val id: String = shortUUID(),
    val userId: String,
    val type: String,
    val title: String,
    val body: String? = null,
    val actionUrl: String? = null,
    val referenceType: String? = null,
    val referenceId: String? = null,

    val isRead: Boolean = false,
    val readAt: OffsetDateTime? = null,

    val channel: NotificationChannel = NotificationChannel.DATABASE,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable