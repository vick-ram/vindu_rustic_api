package org.example.domain.models.marketing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.BigDecimalSerializer
import org.example.utils.OffsetDateTimeSerializer
import java.math.BigDecimal
import java.time.OffsetDateTime

@Serializable
data class Coupon(
    val id: String = Ulid.generate(),
    val code: String,
    val description: String? = null,

    @SerialName("discount_type")
    val discountType: String,

    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("discount_value")
    val discountValue: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("min_order_amount")
    val minOrderAmount: BigDecimal? = null,

    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("max_discount_amount")
    val maxDiscountAmount: BigDecimal? = null,

    @SerialName(value = "usage_limit")
    val usageLimit: Int? = null,

    @SerialName(value = "usage_count")
    val usageCount: Int = 0,

    @SerialName(value = "applies_to_type")
    val appliesToType: String? = null,

    @SerialName(value = "applies_to_id")
    val appliesToId: String? = null,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "starts_at")
    val startsAt: OffsetDateTime? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "ends_at")
    val endsAt: OffsetDateTime? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)