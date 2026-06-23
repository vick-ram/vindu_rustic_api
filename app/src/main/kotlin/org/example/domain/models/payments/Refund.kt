package org.example.domain.models.payments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.BigDecimalSerializer
import org.example.utils.OffsetDateTimeSerializer
import java.math.BigDecimal
import java.time.OffsetDateTime

@Serializable
data class Refund(
    val id: String = Ulid.generate(),

    @SerialName(value = "payment_id")
    val paymentId: String,

    @SerialName("transaction_id")
    val transactionId: String? = null,

    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,

    val reason: String? = null,
    val status: String = "requested",

    @SerialName("requested_by")
    val requestedBy: String? = null,

    @SerialName("processed_by")
    val processedBy: String? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "processed_at")
    val processedAt: OffsetDateTime? = null
)