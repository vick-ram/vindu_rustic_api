package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.CouponUsages
import org.example.data.db.tables.Coupons
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class CouponEntity(id: EntityID<String>) : CustomEntity(id, Coupons) {
    companion object : CustomEntityClass<CouponEntity>(Coupons)

    var code by Coupons.code
    var description by Coupons.description
    var discountType by Coupons.discountType
    var discountValue by Coupons.discountValue
    var minOrderAmount by Coupons.minOrderAmount
    var maxDiscountAmount by Coupons.maxDiscountAmount
    var usageLimit by Coupons.usageLimit
    var usageCount by Coupons.usageCount
    var appliesToType by Coupons.appliesToType
    var appliesToId by Coupons.appliesToId
    var isActive by Coupons.isActive
    var startsAt by Coupons.startsAt
    var endsAt by Coupons.endsAt

    val usages by CouponUsageEntity referrersOn CouponUsages.couponId
}