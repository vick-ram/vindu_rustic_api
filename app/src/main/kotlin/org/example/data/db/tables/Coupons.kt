package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object Coupons : CustomTable("coupons") {
    val code = varchar("code", 100).uniqueIndex()
    val description = text("description").nullable()
    val discountType = varchar("discount_type", 50)
    val discountValue = decimal("discount_value", 12, 2)
    val minOrderAmount = decimal("min_order_amount", 12, 2).nullable()
    val maxDiscountAmount = decimal("max_discount_amount", 12, 2).nullable()
    val usageLimit = integer("usage_limit").nullable()
    val usageCount = integer("usage_count").default(0)
    val appliesToType = varchar("applies_to_type", 50).nullable()
    val appliesToId = varchar("applies_to_id", 120).nullable()
    val isActive = bool("is_active").default(true)
    val startsAt = timestampWithTimeZone("starts_at").nullable()
    val endsAt = timestampWithTimeZone("ends_at").nullable()
}