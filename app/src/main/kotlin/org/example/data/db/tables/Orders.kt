package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.example.data.db.config.inet
import org.example.utils.tsVector
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import java.math.BigDecimal

object Orders : CustomTable("orders") {
    val orderNumber = varchar("order_number", 50).uniqueIndex()
    val userId = reference("user_id", Users).nullable()
    val email = varchar("email", 255)
    val shippingAddressId = reference("shipping_address_id", Addresses)
    val billingAddressId = reference("billing_address_id", Addresses).nullable()
    val status = varchar("status", 50).default("PENDING")
    val paymentStatus = varchar("payment_status", 50).default("PENDING")
    val fulfillmentStatus = varchar("fulfillment_status", 50).default("UNFULFILLED")
    val currency = varchar("currency", 10).default("KES")
    val subtotal = decimal("subtotal", 12, 2)
    val shippingCost = decimal("shipping_cost", 12, 2).default(BigDecimal.ZERO)
    val taxAmount = decimal("tax_amount", 12, 2).default(BigDecimal.ZERO)
    val discountAmount = decimal("discount_amount", 12, 2).default(BigDecimal.ZERO)
    val totalAmount = decimal("total_amount", 12, 2)
    val couponCode = varchar("coupon_code", 100).nullable()
    val notes = text("notes").nullable()
    val ipAddress = inet("ip_address").nullable()
    val userAgent = text("user_agent").nullable()
    val placedAt = timestampWithTimeZone("placed_at")
    val tsv = tsVector("tsv")
}



