package org.example.domain.models.support

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class TicketMessage(
    val id: String = shortUUID(),

    @SerializedName(value = "ticket_id")
    val ticketId: String,

    @SerializedName("sender_id")
    val senderId: String,

    val message: String,

    @SerializedName("is_internal")
    val isInternal: Boolean = false,

    val attachments: Map<String, Any>? = null,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable
