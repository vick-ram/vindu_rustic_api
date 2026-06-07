package org.example.data.db.tables

import org.example.data.db.config.CustomTable

object Payments : CustomTable("payments") {
    val orderId = reference("order_id", Orders)
    val provider = varchar("provider", 100)
    val amount = decimal("amount", 12, 2)
    val currency = varchar("currency", 10).default("KES")
    val status = varchar("status", 50).default("INITIATED")
    val paymentMethod = varchar("payment_method", 100).nullable()
}