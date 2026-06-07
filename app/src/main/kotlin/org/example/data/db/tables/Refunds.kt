package org.example.data.db.tables

import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object Refunds : CustomTable("refunds") {
    val paymentId = reference("payment_id", Payments)
    val transactionId = reference("transaction_id", PaymentTransactions).nullable()
    val amount = decimal("amount", 12, 2)
    val reason = text("reason").nullable()
    val status = varchar("status", 50).default("REQUESTED")
    val requestedBy = reference("requested_by", Users).nullable()
    val processedBy = reference("processed_by", Users).nullable()
    val processedAt = timestampWithTimeZone("processed_at").nullable()
}