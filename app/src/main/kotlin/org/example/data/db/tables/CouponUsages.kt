package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object CouponUsages : CustomTable("coupon_usages") {
    val couponId = reference("coupon_id", Coupons)
    val orderId = reference("order_id", Orders)
    val userId = reference("user_id", Users)
    val discountAmount = decimal("discount_amount", 12, 2).nullable()

    init {
        uniqueIndex(couponId, orderId)
    }
}