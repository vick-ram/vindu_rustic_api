package org.example.data.cache

import org.example.domain.models.marketing.Coupon
import org.example.domain.models.marketing.CouponUsage
import org.example.domain.repo.CouponRepository
import org.example.domain.repo.CrudRepository
import java.math.BigDecimal

class CachedCouponRepository(
    private val delegate: CouponRepository,
    private val cache: CrudRepository<Coupon, String>
) : CouponRepository {
    override suspend fun create(entity: Coupon): Coupon = cache.create(entity)
    override suspend fun read(id: String): Coupon? = cache.read(id)
    override suspend fun readAll(offset: Int, limit: Int, queryParams: Map<String, String>?): List<Coupon> =
        cache.readAll(offset, limit, queryParams)
    override suspend fun update(id: String, entity: Coupon): Coupon? = cache.update(id, entity)
    override suspend fun delete(id: String): Boolean = cache.delete(id)

    override suspend fun findByCode(code: String): Coupon? = delegate.findByCode(code)
    override suspend fun validateCoupon(code: String, orderAmount: BigDecimal): Coupon? =
        delegate.validateCoupon(code, orderAmount)
    override suspend fun applyCoupon(couponId: String, orderId: String, userId: String, discountAmount: BigDecimal): CouponUsage =
        delegate.applyCoupon(couponId, orderId, userId, discountAmount)
    override suspend fun incrementUsage(couponId: String): Boolean = delegate.incrementUsage(couponId)
}