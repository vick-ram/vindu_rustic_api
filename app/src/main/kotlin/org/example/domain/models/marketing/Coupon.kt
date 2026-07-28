package org.example.domain.models.marketing

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
import java.math.BigDecimal
import java.time.OffsetDateTime

@Serializable
data class Coupon(
    val id: String = Ulid.generate(),
    val code: String,
    val description: String? = null,

    @OneOf("percentage", "fixed_amount", "free_shipping")
    @SerialName("discount_type")
    val discountType: String,

    @Contextual
    @SerialName("discount_value")
    val discountValue: BigDecimal,

    @Contextual
    @SerialName("min_order_amount")
    val minOrderAmount: BigDecimal? = null,

    @Contextual
    @SerialName("max_discount_amount")
    val maxDiscountAmount: BigDecimal? = null,

    @SerialName(value = "usage_limit")
    val usageLimit: Int? = null,

    @SerialName(value = "usage_count")
    val usageCount: Int = 0,

    @OneOf("all", "category", "product", "variant")
    @SerialName(value = "applies_to_type")
    val appliesToType: String? = null,

    @SerialName(value = "applies_to_id")
    val appliesToId: String? = null,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @Contextual
    @SerialName(value = "starts_at")
    val startsAt: OffsetDateTime? = null,

    @Contextual
    @SerialName(value = "ends_at")
    val endsAt: OffsetDateTime? = null,

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)