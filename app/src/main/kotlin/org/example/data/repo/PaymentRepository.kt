package org.example.data.repo

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.example.data.mappers.PaymentMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.payments.Payment
import org.example.domain.models.payments.PaymentTransaction
import org.example.domain.models.payments.Refund
import org.example.domain.models.sales.Order
import org.example.domain.models.sales.OrderStatusHistory
import org.example.plugins.DuplicateTransactionException
import org.example.plugins.NotFoundException
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@Component
class PaymentRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    paymentMapper: PaymentMapper,
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val orderRepository: OrderRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val refundRepository: RefundRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val auditLogRepository: AuditLogRepository
) : CrudRepository<Payment, String>(
    connectionFactory = connectionFactory,
    tableName = "payments",
    mapper = paymentMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    /**
     * Initialize payment for order
     */
    suspend fun initializePayment(
        order: Order,
        provider: String = "mpesa",
        paymentMethod: String? = null
    ): Payment {
        val payment = Payment(
            orderId = order.id,
            provider = provider,
            amount = order.totalAmount,
            currency = order.currency,
            status = "initiated",
            paymentMethod = paymentMethod ?: "pending",
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        val createdPayment = create(payment)

        // Emit payment initiated event
        outboxEventRepository.publishEvent(
            aggregateType = "Payment",
            aggregateId = createdPayment.id,
            eventType = "PaymentInitiated",
            payload = buildJsonObject {
                put("payment_id", JsonPrimitive(createdPayment.id))
                put("order_id", JsonPrimitive(order.id))
                put("amount", JsonPrimitive(createdPayment.amount.toString()))
                put("currency", JsonPrimitive(createdPayment.currency))
                put("provider", JsonPrimitive(provider))
            }
        )

        return createdPayment
    }

    /**
     * Process payment authorization
     */
    suspend fun authorizePayment(
        paymentId: String,
        providerTransactionId: String,
        amount: BigDecimal,
        providerResponse: Map<String, Any>? = null
    ): PaymentTransaction {
        return connectionFactory.withTransaction { connection ->
            val payment = getPayment(connection, paymentId)

            // Check for duplicate transactions on same connection
            val isDuplicate = connection.createNamedStatement(
                "SELECT COUNT(*) FROM payment_transactions WHERE provider_transaction_id = :tid",
                mapOf("tid" to providerTransactionId)
            ).execute()
                .awaitSingle()
                .map { row, _ -> row.get(0, Long::class.java)!! }
                .awaitSingle() > 0

            if (isDuplicate) {
                throw DuplicateTransactionException("Transaction already processed: $providerTransactionId")
            }

            // Update payment status on same connection
            connection.createNamedStatement(
                "UPDATE payments SET status = :status, updated_at = :updated_at WHERE id = :id",
                mapOf(
                    "id" to paymentId,
                    "status" to "authorized",
                    "updated_at" to OffsetDateTime.now()
                )
            ).execute().awaitSingle()

            // Create authorization transaction on same connection
            val transactionId = UUID.randomUUID().toString()
            connection.createNamedStatement(
                """
            INSERT INTO payment_transactions 
            (id, payment_id, provider_transaction_id, transaction_type, amount, currency, status, provider_response)
            VALUES (:id, :payment_id, :provider_tid, :type, :amount, :currency, :status, :provider_response)
            RETURNING *
            """.trimIndent(),
                mapOf(
                    "id" to transactionId,
                    "payment_id" to paymentId,
                    "provider_tid" to providerTransactionId,
                    "type" to "authorize",
                    "amount" to amount,
                    "currency" to payment.currency,
                    "status" to "success",
                    "provider_response" to providerResponse?.toString()
                )
            ).execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitSingle()

            // Update order status on same connection
            connection.createNamedStatement(
                "UPDATE orders SET status = :status, payment_status = :payment_status WHERE id = :order_id",
                mapOf(
                    "order_id" to payment.orderId,
                    "status" to "confirmed",
                    "payment_status" to "authorized"
                )
            ).execute().awaitSingle()

            // Record order status history on same connection
            connection.createNamedStatement(
                """
            INSERT INTO order_status_history 
            (order_id, old_status, new_status, comment)
            VALUES (:order_id, :old_status, :new_status, :comment)
            """,
                mapOf(
                    "order_id" to payment.orderId,
                    "old_status" to "pending",
                    "new_status" to "confirmed",
                    "comment" to "Payment authorized: $providerTransactionId"
                )
            ).execute().awaitSingle()

            // Publish event on same connection
            connection.createNamedStatement(
                """
            INSERT INTO outbox_events 
            (aggregate_type, aggregate_id, event_type, payload)
            VALUES (:agg_type, :agg_id, :event_type, :payload)
            """,
                mapOf(
                    "agg_type" to "Payment",
                    "agg_id" to paymentId,
                    "event_type" to "PaymentAuthorized",
                    "payload" to buildJsonObject {
                        put("payment_id", JsonPrimitive(paymentId))
                        put("order_id", JsonPrimitive(payment.orderId))
                        put("transaction_id", JsonPrimitive(transactionId))
                        put("provider_transaction_id", JsonPrimitive(providerTransactionId))
                        put("amount", JsonPrimitive(amount.toString()))
                    }.toString()
                )
            ).execute().awaitSingle()

            // Log audit on same connection
            connection.createNamedStatement(
                """
            INSERT INTO audit_logs 
            (actor_id, actor_type, action, entity_type, entity_id, changes)
            VALUES (:actor_id, :actor_type, :action, :entity_type, :entity_id, :changes)
            """,
                mapOf(
                    "actor_id" to null,
                    "actor_type" to "system",
                    "action" to "PAYMENT_AUTHORIZED",
                    "entity_type" to "Payment",
                    "entity_id" to paymentId,
                    "changes" to buildJsonObject {
                        put("status", JsonPrimitive("authorized"))
                        put("transaction_id", JsonPrimitive(providerTransactionId))
                        put("amount", JsonPrimitive(amount.toString()))
                    }.toString()
                )
            ).execute().awaitSingle()

            // Return the transaction (you'd need to reconstruct or re-fetch it)
            PaymentTransaction(
                id = transactionId,
                paymentId = paymentId,
                providerTransactionId = providerTransactionId,
                transactionType = "authorize",
                amount = amount,
                currency = payment.currency,
                status = "success",
                providerResponse = providerResponse?.let {
                    JsonObject(it.mapValues { entry -> JsonPrimitive(entry.value.toString()) })
                }
            )
        }
    }

    /**
     * Capture payment (complete the charge)
     */
    suspend fun capturePayment(
        paymentId: String,
        amount: BigDecimal? = null,
        providerResponse: Map<String, Any>? = null
    ): PaymentTransaction {
        return connectionFactory.withTransaction { connection ->
            val payment = read(paymentId, connection)
                ?: throw NotFoundException("Payment not found: $paymentId")

            if (payment.status != "authorized") {
                throw IllegalStateException("Payment must be authorized before capture. Current status: ${payment.status}")
            }

            val captureAmount = amount ?: payment.amount

            // Update payment status
            updateStatus(paymentId, "successful", connection)

            // Create capture transaction
            val transaction = paymentTransactionRepository.create(
                PaymentTransaction(
                    paymentId = paymentId,
                    providerTransactionId = generateProviderTransactionId("capture"),
                    transactionType = "capture",
                    amount = captureAmount,
                    currency = payment.currency,
                    status = "success",
                    providerResponse = providerResponse?.let {
                        JsonObject(it.mapValues { entry -> JsonPrimitive(entry.value.toString()) })
                    }
                ),
                connection
            )

            // Update order payment status
            orderRepository.updateStatus(
                id = payment.orderId,
                status = "processing",
                paymentStatus = "paid"
            )

            // Record order status history
            orderStatusHistoryRepository.create(
                OrderStatusHistory(
                    orderId = payment.orderId,
                    oldStatus = "confirmed",
                    newStatus = "processing",
                    comment = "Payment captured: ${transaction.id}"
                ),
                connection
            )

            // Emit event
            outboxEventRepository.publishEvent(
                aggregateType = "Payment",
                aggregateId = paymentId,
                eventType = "PaymentCaptured",
                payload = buildJsonObject {
                    put("payment_id", JsonPrimitive(paymentId))
                    put("order_id", JsonPrimitive(payment.orderId))
                    put("transaction_id", JsonPrimitive(transaction.id))
                    put("amount", JsonPrimitive(captureAmount.toString()))
                    put("status", JsonPrimitive("successful"))
                },
                connection
            )

            // Log audit
            auditLogRepository.logAction(
                actorId = null,
                actorType = "system",
                action = "PAYMENT_CAPTURED",
                entityType = "Payment",
                entityId = paymentId,
                changes = buildJsonObject {
                    put("status", JsonPrimitive("successful"))
                    put("amount", JsonPrimitive(captureAmount.toString()))
                },
                connection = connection
            )

            transaction
        }
    }

    /**
     * Handle payment failure
     */
    suspend fun handlePaymentFailure(
        paymentId: String,
        providerTransactionId: String?,
        errorMessage: String,
        providerResponse: Map<String, Any>? = null
    ): PaymentTransaction {
        return connectionFactory.withTransaction { connection ->
            val payment = read(paymentId, connection)
                ?: throw NotFoundException("Payment not found: $paymentId")

            // Update payment status
            updateStatus(paymentId, "failed", connection)

            // Create failed transaction
            val transaction = paymentTransactionRepository.create(
                PaymentTransaction(
                    paymentId = paymentId,
                    providerTransactionId = providerTransactionId,
                    transactionType = "charge",
                    amount = payment.amount,
                    currency = payment.currency,
                    status = "failure",
                    errorMessage = errorMessage,
                    providerResponse = providerResponse?.let {
                        JsonObject(it.mapValues { entry -> JsonPrimitive(entry.value.toString()) })
                    }
                ),
                connection
            )

            // Update order payment status
            orderRepository.updateStatus(
                id = payment.orderId,
                status = "pending",
                paymentStatus = "failed",
                connection = connection
            )

            // Record order status history
            orderStatusHistoryRepository.create(
                OrderStatusHistory(
                    orderId = payment.orderId,
                    oldStatus = "pending",
                    newStatus = "pending",
                    comment = "Payment failed: $errorMessage"
                ),
                connection
            )

            // Emit event
            outboxEventRepository.publishEvent(
                aggregateType = "Payment",
                aggregateId = paymentId,
                eventType = "PaymentFailed",
                payload = buildJsonObject {
                    put("payment_id", JsonPrimitive(paymentId))
                    put("order_id", JsonPrimitive(payment.orderId))
                    put("error", JsonPrimitive(errorMessage))
                    put("transaction_id", JsonPrimitive(transaction.id))
                },
                connection = connection
            )

            // Log audit
            auditLogRepository.logAction(
                actorId = null,
                actorType = "system",
                action = "PAYMENT_FAILED",
                entityType = "Payment",
                entityId = paymentId,
                changes = buildJsonObject {
                    put("status", JsonPrimitive("failed"))
                    put("error", JsonPrimitive(errorMessage))
                },
                connection = connection
            )

            transaction
        }
    }

    /**
     * Process refund
     */
    suspend fun processRefund(
        paymentId: String,
        amount: BigDecimal,
        reason: String,
        requestedBy: String
    ): Refund {
        return connectionFactory.withTransaction { connection ->
            val payment = read(paymentId, connection)
                ?: throw NotFoundException("Payment not found: $paymentId")

            if (payment.status != "successful") {
                throw IllegalStateException("Can only refund successful payments")
            }

            // Check refund amount
            val totalRefunded = getTotalRefundedAmount(paymentId, connection)
            val availableForRefund = payment.amount - totalRefunded

            if (amount > availableForRefund) {
                throw IllegalStateException(
                    "Refund amount ($amount) exceeds available amount ($availableForRefund)"
                )
            }

            // Create refund transaction
            val refundTransaction = paymentTransactionRepository.create(
                PaymentTransaction(
                    paymentId = paymentId,
                    providerTransactionId = generateProviderTransactionId("refund"),
                    transactionType = "refund",
                    amount = amount,
                    currency = payment.currency,
                    status = "success"
                ),
                connection
            )

            // Create refund record
            val refund = Refund(
                paymentId = paymentId,
                transactionId = refundTransaction.id,
                amount = amount,
                reason = reason,
                status = "processed",
                requestedBy = requestedBy,
                processedAt = OffsetDateTime.now()
            )

            val createdRefund = refundRepository.create(refund, connection)

            // Update payment status
            val newPaymentStatus = if (amount >= availableForRefund) "refunded" else "partially_refunded"
            updateStatus(paymentId, newPaymentStatus, connection)

            // Update order status
            val newOrderStatus = if (newPaymentStatus == "refunded") "refunded" else "partially_refunded"
            orderRepository.updateStatus(
                id = payment.orderId,
                status = newOrderStatus,
                paymentStatus = newPaymentStatus,
                connection = connection
            )

            // Record order status history
            orderStatusHistoryRepository.create(
                OrderStatusHistory(
                    orderId = payment.orderId,
                    oldStatus = "processing",
                    newStatus = newOrderStatus,
                    comment = "Refund processed: $reason"
                ),
                connection
            )

            // Emit event
            outboxEventRepository.publishEvent(
                aggregateType = "Refund",
                aggregateId = createdRefund.id,
                eventType = "RefundProcessed",
                payload = buildJsonObject {
                    put("refund_id", JsonPrimitive(createdRefund.id))
                    put("payment_id", JsonPrimitive(paymentId))
                    put("order_id", JsonPrimitive(payment.orderId))
                    put("amount", JsonPrimitive(amount.toString()))
                    put("reason", JsonPrimitive(reason))
                },
                connection = connection
            )

            // Log audit
            auditLogRepository.logAction(
                actorId = requestedBy,
                actorType = "user",
                action = "REFUND_PROCESSED",
                entityType = "Refund",
                entityId = createdRefund.id,
                changes = buildJsonObject {
                    put("payment_id", JsonPrimitive(paymentId))
                    put("amount", JsonPrimitive(amount.toString()))
                    put("reason", JsonPrimitive(reason))
                }
            )

            createdRefund
        }
    }

    /**
     * Get payment with all transactions
     */
    suspend fun getPaymentWithTransactions(paymentId: String): PaymentWithTransactions? {
        val payment = read(paymentId) ?: return null
        val transactions = paymentTransactionRepository.findByPaymentId(paymentId)

        return PaymentWithTransactions(
            payment = payment,
            transactions = transactions
        )
    }

    /**
     * Get order payment status
     */
    suspend fun getOrderPaymentStatus(orderId: String): OrderPaymentStatus {
        val payments = findByOrderId(orderId)
        val order = orderRepository.read(orderId) ?: throw NotFoundException("Order not found: $orderId")

        val totalPaid = payments
            .filter { it.status == "successful" }
            .sumOf { it.amount }

        val totalRefunded = payments.sumOf { payment ->
            paymentTransactionRepository.findByPaymentId(payment.id)
                .filter { it.transactionType == "refund" && it.status == "success" }
                .sumOf { it.amount }
        }

        val remainingBalance = order.totalAmount - totalPaid + totalRefunded

        return OrderPaymentStatus(
            orderId = orderId,
            totalAmount = order.totalAmount,
            totalPaid = totalPaid,
            totalRefunded = totalRefunded,
            remainingBalance = remainingBalance,
            paymentStatus = order.paymentStatus,
            payments = payments
        )
    }

    /**
     * Retry failed payment
     */
    suspend fun retryPayment(paymentId: String): Payment {
        return connectionFactory.withTransaction { connection ->
            val payment = read(paymentId, connection)
                ?: throw NotFoundException("Payment not found: $paymentId")

            if (payment.status != "failed") {
                throw IllegalStateException("Can only retry failed payments")
            }

            // Reset payment status
            val updatedPayment = updateStatus(paymentId, "initiated", connection)!!

            // Emit retry event
            outboxEventRepository.publishEvent(
                aggregateType = "Payment",
                aggregateId = paymentId,
                eventType = "PaymentRetry",
                payload = buildJsonObject {
                    put("payment_id", JsonPrimitive(paymentId))
                    put("order_id", JsonPrimitive(payment.orderId))
                    put("amount", JsonPrimitive(payment.amount.toString()))
                },
                connection = connection
            )

            updatedPayment
        }
    }

    /**
     * Void payment
     */
    suspend fun voidPayment(paymentId: String): Payment {
        return connectionFactory.withTransaction { connection ->
            val payment = read(paymentId, connection)
                ?: throw NotFoundException("Payment not found: $paymentId")

            if (payment.status !in listOf("initiated", "authorized")) {
                throw IllegalStateException("Can only void initiated or authorized payments")
            }

            // Update payment status
            val updatedPayment = updateStatus(paymentId, "voided", connection)!!

            // Create void transaction
            paymentTransactionRepository.create(
                PaymentTransaction(
                    paymentId = paymentId,
                    providerTransactionId = generateProviderTransactionId("void"),
                    transactionType = "void",
                    amount = payment.amount,
                    currency = payment.currency,
                    status = "success"
                ),
                connection = connection
            )

            // 3. Determine the correct new order status
            val remainingPaidAmount = getTotalPaidForOrder(payment.orderId, connection)
            val hasCompletedPayment = hasCompletedPayment(payment.orderId, connection)

            val newOrderStatus = when {
                remainingPaidAmount > BigDecimal.ZERO -> "partially_paid"
                hasCompletedPayment -> "paid"
                else -> "unpaid"
            }

            // Update order payment status
            orderRepository.updateStatus(
                id = payment.orderId,
                status = newOrderStatus,
                paymentStatus = "voided",
                connection = connection
            )

            // Emit event
            outboxEventRepository.publishEvent(
                aggregateType = "Payment",
                aggregateId = paymentId,
                eventType = "PaymentVoided",
                payload = buildJsonObject {
                    put("payment_id", JsonPrimitive(paymentId))
                    put("order_id", JsonPrimitive(payment.orderId))
                    put("amount", JsonPrimitive(payment.amount.toString()))
                    put("new_order_status", JsonPrimitive(newOrderStatus))
                },
                connection = connection
            )

            updatedPayment
        }
    }

    // Helper methods

    private suspend fun getTotalRefundedAmount(paymentId: String, connection: Connection): BigDecimal {
        val transactions = paymentTransactionRepository.findByPaymentId(paymentId, connection)
        return transactions
            .filter { it.transactionType == "refund" && it.status == "success" }
            .sumOf { it.amount }
    }

    private fun generateProviderTransactionId(prefix: String): String {
        return "$prefix-${UUID.randomUUID().toString().take(8).uppercase()}"
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

        return executeQuery(
            sql, mapOf(
                "status" to status,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
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

        return executeQuery(
            sql, mapOf(
                "provider" to provider,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Update payment status
    suspend fun updateStatus(
        id: String,
        status: String,
        connection: Connection? = null
    ): Payment? {

        val sql = """
            UPDATE $tableName 
            SET status = :status, 
                updated_at = :updatedAt 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()
        val params = mapOf(
            "id" to id,
            "status" to status,
            "updatedAt" to OffsetDateTime.now()
        )
        val conn = connection ?: return connectionFactory.useConnection {
            createNamedStatement(sql, params)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }

        return conn.createNamedStatement(sql, params)
            .execute()
            .awaitSingle()
            .map(rowMapper)
            .awaitFirstOrNull()

    }

    // Get total payments for an order
    suspend fun getTotalPaidForOrder(orderId: String, connection: Connection? = null): BigDecimal {
        val sql = """
            SELECT COALESCE(SUM(amount), 0) as total_paid
            FROM $tableName 
            WHERE order_id = :orderId 
              AND status IN ('completed', 'processing')
        """.trimIndent()

        val conn = connection ?: return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("orderId" to orderId))
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("total_paid", BigDecimal::class.java) ?: BigDecimal.ZERO }
                .awaitFirstOrNull() ?: BigDecimal.ZERO
        }
        return conn.createNamedStatement(sql, mapOf("orderId" to orderId))
            .execute()
            .awaitSingle()
            .map { row, _ -> row.get("total_paid", BigDecimal::class.java) ?: BigDecimal.ZERO }
            .awaitFirstOrNull() ?: BigDecimal.ZERO
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
            createNamedStatement(sql, params).execute()
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
    suspend fun hasCompletedPayment(orderId: String, connection: Connection? = null): Boolean {
        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName 
            WHERE order_id = :orderId AND status = 'completed'
        """.trimIndent()
        val params = mapOf("orderId" to orderId)

        val conn = connection ?: return connectionFactory.useConnection {
            createNamedStatement(sql, params)
                .execute()
                .awaitSingle()
                .map { row, _ -> (row.get("count", Long::class.java) ?: 0L) > 0 }
                .awaitFirstOrNull() ?: false
        }
        return conn.createNamedStatement(sql, params)
            .execute()
            .awaitSingle()
            .map { row, _ -> (row.get("count", Long::class.java) ?: 0L) > 0 }
            .awaitFirstOrNull() ?: false
    }

    // Cancel payment
    suspend fun cancelPayment(id: String, connection: Connection? = null): Payment? {
        return updateStatus(id, "cancelled", connection)
    }

    private suspend fun getPayment(connection: Connection, paymentId: String): Payment {
        return connection.createNamedStatement(
            "SELECT * FROM payments WHERE id = :id",
            mapOf("id" to paymentId)
        ).execute()
            .awaitSingle()
            .map(rowMapper)
            .awaitFirstOrNull()
            ?: throw NotFoundException("Payment not found: $paymentId")
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

@Serializable
data class OrderPaymentStatus(
    val orderId: String,
    @Contextual val totalAmount: BigDecimal,
    @Contextual val totalPaid: BigDecimal,
    @Contextual val totalRefunded: BigDecimal,
    @Contextual val remainingBalance: BigDecimal,
    val paymentStatus: String,
    val payments: List<Payment>
)


@Serializable
data class PaymentWithTransactions(
    val payment: Payment,
    val transactions: List<PaymentTransaction>
)