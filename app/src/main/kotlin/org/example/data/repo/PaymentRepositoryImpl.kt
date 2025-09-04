package org.example.data.repo

import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.PaymentEntity
import org.example.data.db.tables.PaymentTable
import org.example.data.mappers.PaymentMapper
import org.example.domain.models.Payment
import org.example.domain.models.PaymentStatus
import org.example.domain.repo.PaymentRepository
import org.example.utils.now
import org.example.utils.suspendTransaction

class PaymentRepositoryImpl(
    private val paymentMapper: PaymentMapper,
) :
    CrudRepositoryImpl<PaymentEntity, Payment>(PaymentEntity, Payment::class), PaymentRepository {

    override fun PaymentEntity.toDomain(): Payment {
        return paymentMapper.toModel(this)
    }

    override fun getId(domain: Payment): String = domain.id

    override fun Payment.toEntity(entity: PaymentEntity) {
        paymentMapper.toEntity(this, entity)
    }

    override suspend fun findByOrderId(orderId: String): Payment? = suspendTransaction {
        PaymentEntity.find { PaymentTable.order.eq(orderId) }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findByTransactionRef(transactionRef: String): Payment? = suspendTransaction {
        PaymentEntity.find { PaymentTable.transactionReference.eq(transactionRef) }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun updateStatus(
        id: String,
        status: PaymentStatus
    ): Payment? = suspendTransaction {
        PaymentEntity.findByIdAndUpdate(id) { update ->
            update.status = status
            update.updatedAt = LocalDateTime.now()
        }
            ?.toDomain()
    }

    private fun mapOrderStatus() {}
}