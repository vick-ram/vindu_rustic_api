package org.example.data.mappers

import org.example.data.db.entities.RefundEntity
import org.example.data.db.tables.PaymentTransactions
import org.example.data.db.tables.Payments
import org.example.data.db.tables.Users
import org.example.domain.models.payments.Refund
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object RefundMapper : EntityMapper<RefundEntity, Refund, String> {
    override fun toModel(entity: RefundEntity): Refund {
        return Refund(
            id = entity.id.value,
            paymentId = entity.paymentId.value,
            transactionId = entity.transactionId?.value,
            amount = entity.amount,
            reason = entity.reason,
            status = entity.status,
            requestedBy = entity.requestedBy?.value,
            processedBy = entity.processedBy?.value,
            createdAt = entity.createdAt,
            processedAt = entity.processedAt
        )
    }

    override fun toEntity(model: Refund, entity: RefundEntity): RefundEntity {
        entity.paymentId = EntityID(model.paymentId, Payments)
        entity.transactionId = model.transactionId?.let { EntityID(it, PaymentTransactions) }
        entity.amount = model.amount
        entity.reason = model.reason
        entity.status = model.status
        entity.requestedBy = model.requestedBy?.let { EntityID(it, Users) }
        entity.processedBy = model.processedBy?.let { EntityID(it, Users) }
        entity.processedAt = model.processedAt
        return entity
    }
}