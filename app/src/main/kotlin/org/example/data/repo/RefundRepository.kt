package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.RefundMapper
import org.example.domain.models.payments.Refund
import java.math.BigDecimal
import java.time.OffsetDateTime

class RefundRepository(
    connectionFactory: ConnectionFactory,
    refundMapper: RefundMapper
) : CrudRepository<Refund, String>(
    connectionFactory = connectionFactory,
    tableName = "refunds",
    mapper = refundMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get refunds for a payment
    suspend fun findByPaymentId(paymentId: String): List<Refund> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE payment_id = :paymentId 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("paymentId" to paymentId))
    }

    // Get refunds by status
    suspend fun findByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Refund> {
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

    // Get refund by transaction ID
    suspend fun findByTransactionId(transactionId: String): List<Refund> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE transaction_id = :transactionId 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("transactionId" to transactionId))
    }

    // Process refund (approve and complete)
    suspend fun processRefund(
        id: String,
        processedBy: String
    ): Refund? {
        val sql = """
            UPDATE $tableName 
            SET status = 'completed',
                processed_by = :processedBy,
                processed_at = :processedAt
            WHERE id = :id 
              AND status = 'requested'
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("processedBy", processedBy)
                .bind("processedAt", OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Reject refund
    suspend fun rejectRefund(
        id: String,
        processedBy: String
    ): Refund? {
        val sql = """
            UPDATE $tableName 
            SET status = 'rejected',
                processed_by = :processedBy,
                processed_at = :processedAt
            WHERE id = :id 
              AND status = 'requested'
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("processedBy", processedBy)
                .bind("processedAt", OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get total refunded amount for a payment
    suspend fun getTotalRefundedForPayment(paymentId: String): BigDecimal {
        val sql = """
            SELECT COALESCE(SUM(amount), 0) as total_refunded
            FROM $tableName 
            WHERE payment_id = :paymentId 
              AND status = 'completed'
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("paymentId", paymentId)
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("total_refunded", BigDecimal::class.java) ?: BigDecimal.ZERO }
                .awaitFirstOrNull() ?: BigDecimal.ZERO
        }
    }

    // Check if refund amount is valid (not exceeding payment amount)
    suspend fun isValidRefundAmount(paymentId: String, refundAmount: BigDecimal): Boolean {
        val sql = """
            SELECT 
                p.amount as payment_amount,
                COALESCE(SUM(r.amount), 0) as total_refunded
            FROM payments p
            LEFT JOIN $tableName r ON p.id = r.payment_id AND r.status = 'completed'
            WHERE p.id = :paymentId
            GROUP BY p.id, p.amount
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("paymentId", paymentId)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    val paymentAmount = row.get("payment_amount", BigDecimal::class.java) ?: BigDecimal.ZERO
                    val totalRefunded = row.get("total_refunded", BigDecimal::class.java) ?: BigDecimal.ZERO
                    (totalRefunded + refundAmount) <= paymentAmount
                }
                .awaitFirstOrNull() ?: false
        }
    }

    // Get refund summary for a date range
    suspend fun getRefundSummary(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime
    ): RefundSummary {
        val sql = """
            SELECT 
                COUNT(*) as total_count,
                COUNT(CASE WHEN status = 'completed' THEN 1 END) as completed_count,
                COUNT(CASE WHEN status = 'requested' THEN 1 END) as pending_count,
                COUNT(CASE WHEN status = 'rejected' THEN 1 END) as rejected_count,
                COALESCE(SUM(CASE WHEN status = 'completed' THEN amount ELSE 0 END), 0) as total_refunded_amount,
                COALESCE(SUM(amount), 0) as total_requested_amount
            FROM $tableName 
            WHERE created_at BETWEEN :startDate AND :endDate
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("startDate", startDate)
                .bind("endDate", endDate)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    RefundSummary(
                        totalCount = row.get("total_count", Long::class.java).toInt(),
                        completedCount = row.get("completed_count", Long::class.java).toInt(),
                        pendingCount = row.get("pending_count", Long::class.java).toInt(),
                        rejectedCount = row.get("rejected_count", Long::class.java).toInt(),
                        totalRefundedAmount = row.get("total_refunded_amount", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        totalRequestedAmount = row.get("total_requested_amount", BigDecimal::class.java) ?: BigDecimal.ZERO
                    )
                }
                .awaitFirstOrNull() ?: RefundSummary(0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO)
        }
    }

    // Get pending refunds
    suspend fun getPendingRefunds(limit: Int = 20): List<Refund> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = 'requested' 
            ORDER BY created_at ASC 
            LIMIT :limit
        """.trimIndent()

        return executeQuery(sql, mapOf("limit" to limit))
    }

    // Create refund with validation
    override suspend fun create(model: Refund): Refund {
        validateRefund(model)
        return super.create(model)
    }

    private suspend fun validateRefund(refund: Refund) {
        if (refund.amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Refund amount must be greater than zero")
        }

        // Validate that refund amount doesn't exceed payment amount
        if (!isValidRefundAmount(refund.paymentId, refund.amount)) {
            throw IllegalArgumentException("Refund amount exceeds available payment amount")
        }
    }
}

data class RefundSummary(
    val totalCount: Int,
    val completedCount: Int,
    val pendingCount: Int,
    val rejectedCount: Int,
    val totalRefundedAmount: BigDecimal,
    val totalRequestedAmount: BigDecimal
)