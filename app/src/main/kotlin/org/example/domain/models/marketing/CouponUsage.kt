package org.example.domain.models.marketing

import com.google.gson.annotations.SerializedName
import org.example.data.db.config.Ulid
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class CouponUsage(
    val id: String = Ulid.generate(),

    @SerializedName("coupon_id")
    val couponId: String,

    @SerializedName("order_id")
    val orderId: String,

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("discount_amount")
    val discountAmount: BigDecimal? = null,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable
