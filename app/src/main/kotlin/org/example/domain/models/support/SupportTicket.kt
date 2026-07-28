package org.example.domain.models.support

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.NotBlank
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
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

    @OneOf("open", "in_progress", "pending_customer", "resolved", "closed")
    val status: String = "open",

    @OneOf("low", "normal", "high", "urgent")
    val priority: String = "normal",

    @SerialName("ticket_type")
    val ticketType: String? = null,

    @SerialName("assigned_to")
    val assignedTo: String? = null,

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
    @SerialName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
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