package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.example.data.mappers.PaymentMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.payments.Payment
import org.koin.core.annotation.Single
import java.math.BigDecimal
import java.time.OffsetDateTime

@Component
class PaymentRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    paymentMapper: PaymentMapper
) : CrudRepository<Payment, String>(
    connectionFactory = connectionFactory,
    tableName = "payments",
    mapper = paymentMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    // Override update to handle updated_at
    override suspend fun update(id: String, model: Payment): Payment? {
        validatePayment(model)
        return super.update(id, model)
    }

    // Get payments for an order
    suspend fun findByOrderId(orderId: String): List<Payment> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE order_id = :orderId 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("orderId" to orderId))
    }

    // Get payments by status
    suspend fun findByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Payment> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = :status 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "status" to status,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get payments by provider
    suspend fun findByProvider(
        provider: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Payment> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE provider = :provider 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "provider" to provider,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Update payment status
    suspend fun updateStatus(
        id: String,
        status: String
    ): Payment? {
        val validStatuses = listOf("initiated", "processing", "completed", "failed", "cancelled", "refunded")
        if (status !in validStatuses) {
            throw IllegalArgumentException("Invalid payment status: $status. Must be one of: ${validStatuses.joinToString()}")
        }

        val sql = """
            UPDATE $tableName 
            SET status = :status, 
                updated_at = :updatedAt 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("status", status)
                .bind("updatedAt", OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get total payments for an order
    suspend fun getTotalPaidForOrder(orderId: String): BigDecimal {
        val sql = """
            SELECT COALESCE(SUM(amount), 0) as total_paid
            FROM $tableName 
            WHERE order_id = :orderId 
              AND status IN ('completed', 'processing')
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("orderId", orderId)
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("total_paid", BigDecimal::class.java) ?: BigDecimal.ZERO }
                .awaitFirstOrNull() ?: BigDecimal.ZERO
        }
    }

    // Get payment summary for a date range
    suspend fun getPaymentSummary(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        provider: String? = null
    ): PaymentSummary {
        val providerFilter = if (provider != null) "AND provider = :provider" else ""

        val sql = """
            SELECT 
                COUNT(*) as total_count,
                COALESCE(SUM(CASE WHEN status = 'completed' THEN amount ELSE 0 END), 0) as completed_amount,
                COUNT(CASE WHEN status = 'completed' THEN 1 END) as completed_count,
                COALESCE(SUM(CASE WHEN status = 'failed' THEN amount ELSE 0 END), 0) as failed_amount,
                COUNT(CASE WHEN status = 'failed' THEN 1 END) as failed_count,
                COALESCE(SUM(CASE WHEN status IN ('initiated', 'processing') THEN amount ELSE 0 END), 0) as pending_amount,
                COUNT(CASE WHEN status IN ('initiated', 'processing') THEN 1 END) as pending_count
            FROM $tableName 
            WHERE created_at BETWEEN :startDate AND :endDate 
            $providerFilter
        """.trimIndent()

        val params = mutableMapOf<String, Any>(
            "startDate" to startDate,
            "endDate" to endDate
        )

        if (provider != null) {
            params["provider"] = provider
        }

        return connectionFactory.useConnection {
            val statement = createStatement(sql)
            params.forEach { (key, value) -> statement.bind(key, value) }

            statement.execute()
                .awaitSingle()
                .map { row, _ ->
                    PaymentSummary(
                        totalCount = row.get("total_count", Long::class.java)!!.toInt(),
                        completedAmount = row.get("completed_amount", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        completedCount = row.get("completed_count", Long::class.java)!!.toInt(),
                        failedAmount = row.get("failed_amount", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        failedCount = row.get("failed_count", Long::class.java)!!.toInt(),
                        pendingAmount = row.get("pending_amount", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        pendingCount = row.get("pending_count", Long::class.java)!!.toInt()
                    )
                }
                .awaitFirstOrNull() ?: PaymentSummary(0, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0)
        }
    }

    // Get payments by date range
    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<Payment> {
        val statusFilter = if (status != null) "AND status = :status" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE created_at BETWEEN :startDate AND :endDate 
            $statusFilter
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf(
            "startDate" to startDate,
            "endDate" to endDate,
            "limit" to limit,
            "offset" to offset.toLong()
        )

        if (status != null) {
            params["status"] = status
        }

        return executeQuery(sql, params)
    }

    // Check if order has any successful payments
    suspend fun hasCompletedPayment(orderId: String): Boolean {
        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName 
            WHERE order_id = :orderId AND status = 'completed'
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("orderId", orderId)
                .execute()
                .awaitSingle()
                .map { row, _ -> (row.get("count", Long::class.java) ?:0L) > 0 }
                .awaitFirstOrNull() ?: false
        }
    }

    // Cancel payment
    suspend fun cancelPayment(id: String): Payment? {
        return updateStatus(id, "cancelled")
    }

    private fun validatePayment(payment: Payment) {
        if (payment.amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Payment amount must be greater than zero")
        }

        val validProviders = listOf("mpesa", "stripe", "paypal", "bank_transfer", "cash")
        if (payment.provider !in validProviders) {
            throw IllegalArgumentException("Invalid payment provider: ${payment.provider}")
        }
    }
}

@Serializable
data class PaymentSummary(
    val totalCount: Int,
    @Contextual
    val completedAmount: BigDecimal,
    val completedCount: Int,
    @Contextual
    val failedAmount: BigDecimal,
    val failedCount: Int,
    @Contextual
    val pendingAmount: BigDecimal,
    val pendingCount: Int
)