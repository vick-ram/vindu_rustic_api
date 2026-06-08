package org.example.domain.models.support

import com.google.gson.annotations.SerializedName
import org.example.data.db.config.Ulid
import org.example.domain.validations.NotBlank
import java.io.Serializable
import java.time.OffsetDateTime

data class SupportTicket(
    val id: String = Ulid.generate(),

    @SerializedName(value = "user_id")
    @field:NotBlank(message = "User ID must not be blank")
    val userId: String,

    @SerializedName(value = "order_id")
    val orderId: String? =null,

    val subject: String,
    val status: String = "open",
    val priority: String = "normal",

    @SerializedName("ticket_type")
    val ticketType: String? = null,

    @SerializedName("assigned_to")
    val assignedTo: String? = null,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName("resolved_at")
    val resolvedAt: OffsetDateTime? = null
): Serializable {
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