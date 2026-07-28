package org.example.domain.models.marketing

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.utils.Ulid
import java.math.BigDecimal
import java.time.OffsetDateTime

@Serializable
data class CouponUsage(
    val id: String = Ulid.generate(),

    @SerialName("coupon_id")
    val couponId: String,

    @SerialName("order_id")
    val orderId: String,

    @SerialName("user_id")
    val userId: String,

    @Contextual
    @SerialName("discount_amount")
    val discountAmount: BigDecimal? = null,

    @Contextual
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
