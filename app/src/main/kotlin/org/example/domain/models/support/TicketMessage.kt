package org.example.domain.models.support

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.utils.Ulid
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

    val attachments: Map<String, @Contextual Any>? = null,

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
