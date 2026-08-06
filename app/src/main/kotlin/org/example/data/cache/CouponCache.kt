package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.data.mappers.CouponMapper
import org.example.data.repo.CacheConfig
import org.example.data.repo.CouponRepository
import org.example.data.repo.CrudCache
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.marketing.Coupon
import org.example.domain.models.marketing.CouponUsage
import java.math.BigDecimal

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Component
class CouponCache @Inject constructor(
    redis: RedisCoroutinesCommands<String, String>,
    private val couponRepository: CouponRepository,
    couponMapper: CouponMapper
): CrudCache<Coupon, String>(
    redis = redis,
    delegate = couponRepository,
    getId = {coupon -> couponMapper.getId(coupon) as String},
    serializer = Coupon.serializer(),
    config = object : CacheConfig {
        override val cacheName: String  = "couponCache"
        override val ttl: Long = 3600L
    }
) {

    suspend fun findByCode(code: String): Coupon? {
        return couponRepository.findByCode(code)
    }

    suspend fun validateCoupon(code: String, orderAmount: BigDecimal): Coupon? {
        return couponRepository.validateCoupon(code, orderAmount)
    }

    suspend fun applyCoupon(couponId: String, orderId: String, userId: String, discount: BigDecimal): CouponUsage {
        return couponRepository.applyCoupon(couponId, orderId, userId, discount)
    }

    suspend fun incrementUsage(couponId: String): Boolean {
        return couponRepository.incrementUsage(couponId)
    }
}