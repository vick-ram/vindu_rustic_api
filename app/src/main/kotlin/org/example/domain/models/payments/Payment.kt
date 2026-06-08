package org.example.domain.models.payments

import com.google.gson.annotations.SerializedName
import org.example.data.db.config.Ulid
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class Payment(
    val id: String = Ulid.generate(),

    @SerializedName(value = "order_id")
    val orderId: String,

    val provider: String = "mpesa",

    val amount: BigDecimal,
    val currency: String = "kes",
    val status: String = "initiated",

    @SerializedName(value = "payment_method")
    val paymentMethod: String? = null,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
): Serializable