package org.example.domain.models.support

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.domain.validations.NotBlank
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class SupportTicket(
    val id: String = Ulid.generate(),

    @SerialName(value = "user_id")
    @field:NotBlank(message = "User ID must not be blank")
    val userId: String,

    @SerialName(value = "order_id")
    val orderId: String? =null,

    val subject: String,
    val status: String = "open",
    val priority: String = "normal",

    @SerialName("ticket_type")
    val ticketType: String? = null,

    @SerialName("assigned_to")
    val assignedTo: String? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("resolved_at")
    val resolvedAt: OffsetDateTime? = null
) {
    companion object {
//        fun formParameters(parameters: Parameters): SupportTicket {
//            val userId = parameters["userId"] as String
//            val orderId = parameters["orderId"] as String
//            val subject = parameters["subject"] as String
//
//            return SupportTicket(
//                userId = userId,
//                orderId = orderId,
//                subject = subject
//            )
//        }
    }
}