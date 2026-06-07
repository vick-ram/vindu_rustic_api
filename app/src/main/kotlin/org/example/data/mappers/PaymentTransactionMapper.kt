package org.example.data.mappers

import org.example.data.db.entities.PaymentTransactionEntity
import org.example.data.db.tables.Payments
import org.example.domain.models.payments.PaymentTransaction
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object PaymentTransactionMapper : EntityMapper<PaymentTransactionEntity, PaymentTransaction, String> {
    override fun toModel(entity: PaymentTransactionEntity): PaymentTransaction {
        return PaymentTransaction(
            id = entity.id.value,
            paymentId = entity.paymentId.value,
            providerTransactionId = entity.providerTransactionId,
            transactionType = entity.transactionType,
            amount = entity.amount,
            currency = entity.currency,
            status = entity.status,
            providerResponse = entity.providerResponse,
            errorMessage = entity.errorMessage,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: PaymentTransaction, entity: PaymentTransactionEntity): PaymentTransactionEntity {
        entity.paymentId = EntityID(model.paymentId, Payments)
        entity.providerTransactionId = model.providerTransactionId
        entity.transactionType = model.transactionType
        entity.amount = model.amount
        entity.currency = model.currency
        entity.status = model.status
        entity.providerResponse = model.providerResponse
        entity.errorMessage = model.errorMessage
        return entity
    }
}