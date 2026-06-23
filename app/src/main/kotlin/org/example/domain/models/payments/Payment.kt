package org.example.domain.models.payments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.BigDecimalSerializer
import org.example.utils.OffsetDateTimeSerializer
import java.math.BigDecimal
import java.time.OffsetDateTime

@Serializable
data class Payment(
    val id: String = Ulid.generate(),

    @SerialName(value = "order_id")
    val orderId: String,

    val provider: String = "mpesa",

    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,

    val currency: String = "kes",
    val status: String = "initiated",

    @SerialName(value = "payment_method")
    val paymentMethod: String? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)