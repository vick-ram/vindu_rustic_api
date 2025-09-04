package org.example.data.mappers

import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.OrderEntity
import org.example.data.db.entities.PaymentEntity
import org.example.data.db.entities.UserEntity
import org.example.domain.models.Payment
import org.example.domain.repo.EntityMapper
import org.example.utils.now

object PaymentMapper : EntityMapper<PaymentEntity, Payment, String> {
    override fun toModel(entity: PaymentEntity): Payment {
        return Payment(
            id = entity.id.value,
            orderId = entity.order.id.value,
            userId = entity.user.id.value,
            amount = entity.amount,
            status = entity.status,
            method = entity.method,
            transactionReference = entity.transactionReference,
            paidAt = entity.paidAt,
            createdAt = entity.createdAt
        )
    }

    override fun toEntity(
        model: Payment,
        entity: PaymentEntity
    ): PaymentEntity {
        val order = OrderEntity[model.orderId]
        return entity.apply {
            this.order = order
            this.user = UserEntity[model.userId]
            this.amount = order.totalAmount
            this.status = model.status
            this.method = model.method
            this.transactionReference = model.transactionReference
            this.paidAt = LocalDateTime.now()
            this.createdAt = LocalDateTime.now()
        }
    }
}