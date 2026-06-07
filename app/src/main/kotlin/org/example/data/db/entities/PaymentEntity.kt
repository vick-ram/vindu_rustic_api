package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.PaymentTransactions
import org.example.data.db.tables.Payments
import org.example.data.db.tables.Refunds
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class PaymentEntity(id: EntityID<String>) : CustomEntity(id, Payments) {
    companion object : CustomEntityClass<PaymentEntity>(Payments)

    var orderId by Payments.orderId
    var provider by Payments.provider
    var amount by Payments.amount
    var currency by Payments.currency
    var status by Payments.status
    var paymentMethod by Payments.paymentMethod

    val transactions by PaymentTransactionEntity referrersOn PaymentTransactions.paymentId
    val refunds by RefundEntity referrersOn Refunds.paymentId
}