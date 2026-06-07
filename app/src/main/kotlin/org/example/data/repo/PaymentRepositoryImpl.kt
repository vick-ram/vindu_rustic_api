package org.example.data.repo

import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.PaymentEntity
import org.example.data.db.tables.Orders
import org.example.data.db.tables.Payments
import org.example.data.mappers.PaymentMapper
import org.example.domain.models.payments.Payment
import org.example.domain.models.sales.PaymentStatus
import org.example.domain.repo.PaymentRepository
import org.example.plugins.NotFoundException
import org.example.utils.now
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.math.BigDecimal

class PaymentRepositoryImpl(private val paymentMapper: PaymentMapper) :
    CrudRepositoryImpl<PaymentEntity, Payment>(PaymentEntity, Payment::class),
    PaymentRepository {

    override suspend fun findByOrderId(orderId: String): List<Payment> = suspendTransaction {
        PaymentEntity.find { Payments.orderId eq orderId }
            .map { it.toDomain() }
    }

    override suspend fun processPayment(orderId: String, amount: BigDecimal, provider: String): Payment = suspendTransaction {
        PaymentEntity.new {
            this.orderId = EntityID(orderId, Orders)
            this.provider = provider
            this.amount = amount
            this.status = "INITIATED"
        }.toDomain()
    }

    override suspend fun updatePaymentStatus(paymentId: String, status: String): Boolean = suspendTransaction {
        val payment = PaymentEntity.findById(paymentId) ?: throw NotFoundException("Payment not found")
        payment.status = status
        true
    }

    override fun PaymentEntity.toDomain(): Payment = paymentMapper.toModel(this)
    override fun Payment.toEntity(entity: PaymentEntity) {
        paymentMapper.toEntity(this, entity)
    }
    override fun getId(domain: Payment): String = domain.id.toString()
}