package org.example.domain.repo

import org.example.domain.models.payments.Payment
import org.example.domain.models.sales.PaymentStatus
import java.math.BigDecimal

interface PaymentRepository : CrudRepository<Payment, String> {
    suspend fun findByOrderId(orderId: String): List<Payment>
    suspend fun processPayment(orderId: String, amount: BigDecimal, provider: String): Payment
    suspend fun updatePaymentStatus(paymentId: String, status: String): Boolean
}