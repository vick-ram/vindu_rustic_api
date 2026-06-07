package org.example.data.mappers

import org.example.data.db.entities.CouponEntity
import org.example.domain.models.marketing.Coupon
import org.example.domain.repo.EntityMapper

object CouponMapper : EntityMapper<CouponEntity, Coupon, String> {
    override fun toModel(entity: CouponEntity): Coupon {
        return Coupon(
            id = entity.id.value,
            code = entity.code,
            description = entity.description,
            discountType = entity.discountType,
            discountValue = entity.discountValue,
            minOrderAmount = entity.minOrderAmount,
            maxDiscountAmount = entity.maxDiscountAmount,
            usageLimit = entity.usageLimit,
            usageCount = entity.usageCount,
            appliesToType = entity.appliesToType,
            appliesToId = entity.appliesToId,
            isActive = entity.isActive,
            startsAt = entity.startsAt,
            endsAt = entity.endsAt,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: Coupon, entity: CouponEntity): CouponEntity {
        entity.code = model.code
        entity.description = model.description
        entity.discountType = model.discountType
        entity.discountValue = model.discountValue
        entity.minOrderAmount = model.minOrderAmount
        entity.maxDiscountAmount = model.maxDiscountAmount
        entity.usageLimit = model.usageLimit
        entity.usageCount = model.usageCount
        entity.appliesToType = model.appliesToType
        entity.appliesToId = model.appliesToId
        entity.isActive = model.isActive
        entity.startsAt = model.startsAt
        entity.endsAt = model.endsAt
        return entity
    }
}