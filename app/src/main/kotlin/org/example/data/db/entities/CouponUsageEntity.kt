package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.CouponUsages
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class CouponUsageEntity(id: EntityID<String>) : CustomEntity(id, CouponUsages) {
    companion object : CustomEntityClass<CouponUsageEntity>(CouponUsages)

    var couponId by CouponUsages.couponId
    var orderId by CouponUsages.orderId
    var userId by CouponUsages.userId
    var discountAmount by CouponUsages.discountAmount
}