package org.example.domain.repo

import org.example.domain.models.Payment
import org.example.domain.models.PaymentStatus

interface PaymentRepository: CrudRepository<Payment, String> {
    suspend fun findByOrderId(orderId: String): Payment?
    suspend fun findByTransactionRef(transactionRef: String): Payment?
    suspend fun updateStatus(id: String, status: PaymentStatus): Payment?
}