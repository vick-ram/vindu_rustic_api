package org.example.domain.models.payments

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
import java.math.BigDecimal
import java.time.OffsetDateTime

@Serializable
data class Payment(
    val id: String = Ulid.generate(),

    @SerialName(value = "order_id")
    val orderId: String,

    val provider: String = "mpesa",

    @Contextual
    val amount: BigDecimal,

    val currency: String = "kes",

    @OneOf("initiated", "authorized", "successful", "failed", "voided")
    val status: String = "initiated",

    @SerialName(value = "payment_method")
    val paymentMethod: String? = null,

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
    @SerialName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)