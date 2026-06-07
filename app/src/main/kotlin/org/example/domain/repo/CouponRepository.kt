package org.example.domain.repo

import org.example.domain.models.marketing.Coupon
import org.example.domain.models.marketing.CouponUsage
import java.math.BigDecimal

interface CouponRepository : CrudRepository<Coupon, String> {
    suspend fun findByCode(code: String): Coupon?
    suspend fun validateCoupon(code: String, orderAmount: BigDecimal): Coupon?
    suspend fun applyCoupon(couponId: String, orderId: String, userId: String, discountAmount: BigDecimal): CouponUsage
    suspend fun incrementUsage(couponId: String): Boolean
}