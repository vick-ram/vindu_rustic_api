package org.example.services

import org.example.data.cache.CouponCache
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.marketing.Coupon
import org.example.domain.models.marketing.CouponUsage
import java.math.BigDecimal

@Component
class CouponService @Inject constructor(private val couponCache: CouponCache) {

    suspend fun getCouponByCode(code: String): Coupon? {
        return couponCache.findByCode(code)
    }

    suspend fun validateCoupon(code: String, orderAmount: BigDecimal): Coupon? {
        return couponCache.validateCoupon(code, orderAmount)
    }

    suspend fun incrementUsage(couponId: String): Boolean {
        return couponCache.incrementUsage(couponId)
    }

    suspend fun applyCoupon(couponId: String, orderId: String, userId: String, discountAmount: BigDecimal): CouponUsage {
        return couponCache.applyCoupon(couponId, orderId, userId, discountAmount)
    }
}