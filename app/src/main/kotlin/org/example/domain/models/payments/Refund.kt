package org.example.domain.models.payments

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.data.db.config.Ulid
import org.example.utils.now
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class Refund(
    val id: String = Ulid.generate(),

    @SerializedName(value = "payment_id")
    val paymentId: String,

    @SerializedName("transaction_id")
    val transactionId: String? = null,

    val amount: BigDecimal,
    val reason: String? = null,
    val status: String = "requested",

    @SerializedName("requested_by")
    val requestedBy: String? = null,

    @SerializedName("processed_by")
    val processedBy: String? = null,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName(value = "processed_at")
    val processedAt: OffsetDateTime? = null
): Serializable