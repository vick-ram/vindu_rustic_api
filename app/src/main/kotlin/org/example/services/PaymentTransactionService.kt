package org.example.services

import org.example.data.cache.PaymentTransactionCache
import org.example.data.repo.TransactionStats
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.payments.PaymentTransaction
import java.time.OffsetDateTime

@Component
class PaymentTransactionService @Inject constructor(private val cache: PaymentTransactionCache) {
    suspend fun createTransaction(transaction: PaymentTransaction): PaymentTransaction = cache.create(transaction)
    suspend fun getTransaction(id: String): PaymentTransaction? = cache.read(id)
    suspend fun updateTransaction(id: String, transaction: PaymentTransaction): PaymentTransaction? = cache.update(id, transaction)
    suspend fun deleteTransaction(id: String): Boolean = cache.delete(id)
    suspend fun updateTransactionStatus(id: String, status: String, providerResponse: Map<String, Any>? = null, errorMessage: String? = null) =
        cache.updateTransactionStatus(id, status, providerResponse, errorMessage)
    suspend fun getTransactionsByPayment(paymentId: String): List<PaymentTransaction> = cache.findByPaymentId(paymentId)
    suspend fun getTransactionByProviderId(providerTransactionId: String): PaymentTransaction? = cache.findByProviderTransactionId(providerTransactionId)
    suspend fun getLatestTransaction(paymentId: String): PaymentTransaction? = cache.getLatestTransaction(paymentId)
    suspend fun isDuplicateTransaction(providerTransactionId: String): Boolean = cache.isDuplicateTransaction(providerTransactionId)
    suspend fun getTransactionsByType(transactionType: String, offset: Int = 0, limit: Int = 50) = cache.findByTransactionType(transactionType, offset, limit)
    suspend fun getFailedTransactions(offset: Int = 0, limit: Int = 50) = cache.findFailedTransactions(offset, limit)
    suspend fun getTransactionStats(startDate: OffsetDateTime, endDate: OffsetDateTime, transactionType: String? = null): TransactionStats = cache.getTransactionStats(startDate, endDate, transactionType)
}
