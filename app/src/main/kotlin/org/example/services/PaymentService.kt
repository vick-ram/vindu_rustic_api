package org.example.services

import org.example.domain.models.Payment
import org.example.domain.models.PaymentStatus
import org.example.domain.repo.PaymentRepository

class PaymentService(private val paymentRepository: PaymentRepository) {
    suspend fun initiatePayment(payment: Payment): Payment? {
        return paymentRepository.create(payment)
    }

    suspend fun getPayment(id: String): Payment? {
        return paymentRepository.read(id)
    }

    suspend fun getPayments(offset: Int, limit: Int, queryParams: Map<String, String>?): List<Payment> {
        return paymentRepository.readAll(offset, limit, queryParams)
    }

    suspend fun getPaymentByOrder(orderId: String): Payment? {
        return paymentRepository.findByOrderId(orderId)
    }

    suspend fun getPaymentByTransactionRef(transactionRef: String): Payment? {
        return paymentRepository.findByTransactionRef(transactionRef)
    }

    suspend fun updatePaymentStatus(id: String, status: PaymentStatus): Payment? {
        return paymentRepository.updateStatus(id, status)
    }

    suspend fun deletePayment(id: String): Boolean {
        return paymentRepository.delete(id)
    }

}