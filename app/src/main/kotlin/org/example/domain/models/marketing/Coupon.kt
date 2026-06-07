package org.example.domain.models.marketing

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class Coupon(
    val id: String = shortUUID(),
    val code: String,
    val description: String? = null,

    @SerializedName("discount_type")
    val discountType: String,

    @SerializedName("discount_value")
    val discountValue: BigDecimal,

    @SerializedName("min_order_amount")
    val minOrderAmount: BigDecimal? = null,

    @SerializedName("max_discount_amount")
    val maxDiscountAmount: BigDecimal? = null,

    @SerializedName(value = "usage_limit")
    val usageLimit: Int? = null,

    @SerializedName(value = "usage_count")
    val usageCount: Int = 0,

    @SerializedName(value = "applies_to_type")
    val appliesToType: String? = null,

    @SerializedName(value = "applies_to_id")
    val appliesToId: String? = null,

    @SerializedName("is_active")
    val isActive: Boolean = true,

    @SerializedName(value = "starts_at")
    val startsAt: OffsetDateTime? = null,

    @SerializedName(value = "ends_at")
    val endsAt: OffsetDateTime? = null,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable