package org.example.domain.models.support

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.MapStringAnySerializer
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class TicketMessage(
    val id: String = Ulid.generate(),

    @SerialName(value = "ticket_id")
    val ticketId: String,

    @SerialName("sender_id")
    val senderId: String,

    val message: String,

    @SerialName("is_internal")
    val isInternal: Boolean = false,

    @Serializable(with = MapStringAnySerializer::class)
    val attachments: Map<String, Any>? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
