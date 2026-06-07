package org.example.data.mappers

import org.example.data.db.entities.CouponUsageEntity
import org.example.data.db.tables.Coupons
import org.example.data.db.tables.Orders
import org.example.data.db.tables.Users
import org.example.domain.models.marketing.CouponUsage
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object CouponUsageMapper : EntityMapper<CouponUsageEntity, CouponUsage, String> {
    override fun toModel(entity: CouponUsageEntity): CouponUsage {
        return CouponUsage(
            id = entity.id.value,
            couponId = entity.couponId.value,
            orderId = entity.orderId.value,
            userId = entity.userId.value,
            discountAmount = entity.discountAmount,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: CouponUsage, entity: CouponUsageEntity): CouponUsageEntity {
        entity.couponId = EntityID(model.couponId, Coupons)
        entity.orderId = EntityID(model.orderId, Orders)
        entity.userId = EntityID(model.userId, Users)
        entity.discountAmount = model.discountAmount
        return entity
    }
}