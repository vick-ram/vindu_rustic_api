package org.example.domain.models.marketing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.BigDecimalSerializer
import org.example.utils.OffsetDateTimeSerializer
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

    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("discount_amount")
    val discountAmount: BigDecimal? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
