package org.example.data.repo

import org.example.data.db.entities.CouponEntity
import org.example.data.db.entities.CouponUsageEntity
import org.example.data.db.tables.Coupons
import org.example.data.db.tables.Orders
import org.example.data.db.tables.Users
import org.example.data.mappers.CouponMapper
import org.example.domain.models.marketing.Coupon
import org.example.domain.models.marketing.CouponUsage
import org.example.domain.repo.CouponRepository
import org.example.plugins.NotFoundException
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.or
import java.math.BigDecimal
import java.time.OffsetDateTime

class CouponRepositoryImpl(private val couponMapper: CouponMapper) :
    CrudRepositoryImpl<CouponEntity, Coupon>(CouponEntity, Coupon::class),
    CouponRepository {

    override suspend fun findByCode(code: String): Coupon? = suspendTransaction {
        CouponEntity.find { Coupons.code eq code }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun validateCoupon(code: String, orderAmount: BigDecimal): Coupon? = suspendTransaction {
        val coupon = CouponEntity.find {
            (Coupons.code eq code) and
                    (Coupons.isActive eq true) and
                    (Coupons.startsAt.isNull() or (Coupons.startsAt lessEq OffsetDateTime.now())) and
                    (Coupons.endsAt.isNull() or (Coupons.endsAt greater OffsetDateTime.now()))
        }.firstOrNull() ?: return@suspendTransaction null

        // Check minimum order amount
        coupon.minOrderAmount?.let { minAmount ->
            if (orderAmount < minAmount) return@suspendTransaction null
        }

        // Check usage limit
        coupon.usageLimit?.let { limit ->
            if (coupon.usageCount >= limit) return@suspendTransaction null
        }

        coupon.toDomain()
    }

    override suspend fun applyCoupon(couponId: String, orderId: String, userId: String, discountAmount: BigDecimal): CouponUsage = suspendTransaction {
        CouponUsageEntity.new {
            this.couponId = EntityID(couponId, Coupons)
            this.orderId = EntityID(orderId, Orders)
            this.userId = EntityID(userId, Users)
            this.discountAmount = discountAmount
        }.let {
            CouponUsage(
                id = it.id.value,
                couponId = it.couponId.value,
                orderId = it.orderId.value,
                userId = it.userId.value,
                discountAmount = it.discountAmount
            )
        }
    }

    override suspend fun incrementUsage(couponId: String): Boolean = suspendTransaction {
        val coupon = CouponEntity.findById(couponId) ?: throw NotFoundException("Coupon not found")
        coupon.usageCount += 1
        true
    }

    override fun CouponEntity.toDomain(): Coupon = couponMapper.toModel(this)
    override fun Coupon.toEntity(entity: CouponEntity) {
        couponMapper.toEntity(this, entity)
    }
    override fun getId(domain: Coupon): String = domain.id
}