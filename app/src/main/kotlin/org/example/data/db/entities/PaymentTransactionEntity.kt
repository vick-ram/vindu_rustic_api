package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.PaymentTransactions
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class PaymentTransactionEntity(id: EntityID<String>) : CustomEntity(id, PaymentTransactions) {
    companion object : CustomEntityClass<PaymentTransactionEntity>(PaymentTransactions)

    var paymentId by PaymentTransactions.paymentId
    var providerTransactionId by PaymentTransactions.providerTransactionId
    var transactionType by PaymentTransactions.transactionType
    var amount by PaymentTransactions.amount
    var currency by PaymentTransactions.currency
    var status by PaymentTransactions.status
    var providerResponse by PaymentTransactions.providerResponse
    var errorMessage by PaymentTransactions.errorMessage
}