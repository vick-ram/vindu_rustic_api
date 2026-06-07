package org.example.data.mappers

import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.OrderEntity
import org.example.data.db.entities.PaymentEntity
import org.example.data.db.entities.UserEntity
import org.example.data.db.tables.Orders
import org.example.domain.models.payments.Payment
import org.example.domain.repo.EntityMapper
import org.example.utils.now
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object PaymentMapper : EntityMapper<PaymentEntity, Payment, String> {
    override fun toModel(entity: PaymentEntity): Payment {
        return Payment(
            id = entity.id.value,
            orderId = entity.orderId.value,
            provider = entity.provider,
            amount = entity.amount,
            currency = entity.currency,
            status = entity.status,
            paymentMethod = entity.paymentMethod,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(model: Payment, entity: PaymentEntity): PaymentEntity {
        entity.orderId = EntityID(model.orderId, Orders)
        entity.provider = model.provider
        entity.amount = model.amount
        entity.currency = model.currency
        entity.status = model.status
        entity.paymentMethod = model.paymentMethod
        return entity
    }
}