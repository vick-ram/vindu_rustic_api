package org.example.domain.models.payments

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
import java.math.BigDecimal
import java.time.OffsetDateTime

@Serializable
data class Refund(
    val id: String = Ulid.generate(),

    @SerialName(value = "payment_id")
    val paymentId: String,

    @SerialName("transaction_id")
    val transactionId: String? = null,

    @Contextual
    val amount: BigDecimal,

    val reason: String? = null,

    @OneOf("requested", "approved", "processed", "rejected", "failed")
    val status: String = "requested",

    @SerialName("requested_by")
    val requestedBy: String? = null,

    @SerialName("processed_by")
    val processedBy: String? = null,

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
    @SerialName(value = "processed_at")
    val processedAt: OffsetDateTime? = null
)