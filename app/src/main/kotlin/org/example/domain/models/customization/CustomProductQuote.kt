package org.example.domain.models.customization

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.shortUUID
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class CustomProductQuote(
    val id: String = shortUUID(),

    @SerializedName("request_id")
    val requestId: String,

    @SerializedName("quoted_price")
    val quotedPrice: BigDecimal,

    val currency: String = "kes",

    @SerializedName("production_timeline")
    val productionTimeline: Int? = null,

    val description: String? = null,

    @SerializedName("valid_until")
    val validUntil: OffsetDateTime? = null,

    val status: String = "sent",
    val createdBy: String,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
): Serializable
