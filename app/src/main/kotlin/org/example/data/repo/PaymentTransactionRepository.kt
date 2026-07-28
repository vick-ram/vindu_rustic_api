package org.example.data.repo

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.example.data.mappers.PaymentTransactionMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.payments.PaymentTransaction
import org.koin.core.annotation.Single
import java.math.BigDecimal
import java.time.OffsetDateTime

@Component
class PaymentTransactionRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    paymentTransactionMapper: PaymentTransactionMapper
) : CrudRepository<PaymentTransaction, String>(
    connectionFactory = connectionFactory,
    tableName = "payment_transactions",
    idColumn = "id",
    mapper = paymentTransactionMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get transactions for a payment
    suspend fun findByPaymentId(paymentId: String, connection: Connection? = null): List<PaymentTransaction> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE payment_id = :paymentId 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("paymentId" to paymentId), connection)
    }

    // Get transaction by provider transaction ID
    suspend fun findByProviderTransactionId(providerTransactionId: String): PaymentTransaction? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE provider_transaction_id = :providerTransactionId 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("providerTransactionId" to providerTransactionId))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get transactions by type
    suspend fun findByTransactionType(
        transactionType: String,
        offset: Int = 0,
        limit: Int = 50
    ): List<PaymentTransaction> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE transaction_type = :transactionType 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "transactionType" to transactionType,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Update transaction status with provider response
    suspend fun updateTransactionStatus(
        id: String,
        status: String,
        providerResponse: Map<String, Any>? = null,
        errorMessage: String? = null
    ): PaymentTransaction? {
        val sql = """
            UPDATE $tableName 
            SET status = :status,
                provider_response = COALESCE(:providerResponse, provider_response),
                error_message = :errorMessage
            WHERE id = :id 
            RETURNING *
        """.trimIndent()
        val params = mapOf(
            "id" to id,
            "status" to status,
            "errorMessage" to errorMessage,
            "providerResponse" to providerResponse
        )

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, params)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get transaction statistics for a date range
    suspend fun getTransactionStats(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        transactionType: String? = null
    ): TransactionStats {
        val typeFilter = if (transactionType != null) "AND transaction_type = :transactionType" else ""

        val sql = """
            SELECT 
                COUNT(*) as total_count,
                COUNT(CASE WHEN status = 'completed' THEN 1 END) as successful_count,
                COUNT(CASE WHEN status = 'failed' THEN 1 END) as failed_count,
                COALESCE(SUM(CASE WHEN status = 'completed' THEN amount ELSE 0 END), 0) as total_successful_amount,
                COALESCE(SUM(amount), 0) as total_amount
            FROM $tableName 
            WHERE created_at BETWEEN :startDate AND :endDate 
            $typeFilter
        """.trimIndent()

        val params = mutableMapOf<String, Any>(
            "startDate" to startDate,
            "endDate" to endDate
        )

        if (transactionType != null) {
            params["transactionType"] = transactionType
        }

        return connectionFactory.useConnection {
            createNamedStatement(sql, params).execute()
                .awaitSingle()
                .map { row, _ ->
                    TransactionStats(
                        totalCount = row.get("total_count", Long::class.java)!!.toInt(),
                        successfulCount = row.get("successful_count", Long::class.java)!!.toInt(),
                        failedCount = row.get("failed_count", Long::class.java)!!.toInt(),
                        totalSuccessfulAmount = row.get("total_successful_amount", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        totalAmount = row.get("total_amount", BigDecimal::class.java) ?: BigDecimal.ZERO
                    )
                }
                .awaitFirstOrNull() ?: TransactionStats(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO)
        }
    }

    // Get latest transaction for a payment
    suspend fun getLatestTransaction(paymentId: String): PaymentTransaction? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE payment_id = :paymentId 
            ORDER BY created_at DESC 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("paymentId" to paymentId))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Check for duplicate transactions
    suspend fun isDuplicateTransaction(providerTransactionId: String): Boolean {
        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName 
            WHERE provider_transaction_id = :providerTransactionId
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("providerTransactionId" to providerTransactionId))
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("count", Long::class.java)!! > 0 }
                .awaitFirstOrNull() ?: false
        }
    }

    // Get transactions with errors
    suspend fun findFailedTransactions(
        offset: Int = 0,
        limit: Int = 50
    ): List<PaymentTransaction> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = 'failed' 
               OR error_message IS NOT NULL 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }
}

@Serializable
data class TransactionStats(
    val totalCount: Int,
    val successfulCount: Int,
    val failedCount: Int,
    @Contextual
    val totalSuccessfulAmount: BigDecimal,
    @Contextual
    val totalAmount: BigDecimal
)