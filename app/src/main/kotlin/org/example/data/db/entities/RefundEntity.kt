package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.Refunds
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class RefundEntity(id: EntityID<String>) : CustomEntity(id, Refunds) {
    companion object : CustomEntityClass<RefundEntity>(Refunds)

    var paymentId by Refunds.paymentId
    var transactionId by Refunds.transactionId
    var amount by Refunds.amount
    var reason by Refunds.reason
    var status by Refunds.status
    var requestedBy by Refunds.requestedBy
    var processedBy by Refunds.processedBy
    var processedAt by Refunds.processedAt
}