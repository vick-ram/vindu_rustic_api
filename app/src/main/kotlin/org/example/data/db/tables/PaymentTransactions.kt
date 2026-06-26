package org.example.data.db.tables

import kotlinx.serialization.json.Json
import org.example.data.db.config.CustomTable
import org.jetbrains.exposed.v1.json.jsonb

object PaymentTransactions : CustomTable("payment_transactions") {
    val paymentId = reference("payment_id", Payments)
    val providerTransactionId = varchar("provider_transaction_id", 255).nullable()
    val transactionType = varchar("transaction_type", 50)
    val amount = decimal("amount", 12, 2)
    val currency = varchar("currency", 10).default("KES")
    val status = varchar("status", 50)
    val providerResponse = jsonb<Map<String, Any>>("provider_response", Json).nullable()
    val errorMessage = text("error_message").nullable()
}