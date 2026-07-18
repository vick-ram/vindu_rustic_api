package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.example.data.mappers.OrderMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.Order
import java.math.BigDecimal
import java.time.OffsetDateTime

@Component
class OrderRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    orderMapper: OrderMapper
) : CrudRepository<Order, String>(
    connectionFactory = connectionFactory,
    tableName = "orders",
    idColumn = "id",
    mapper = orderMapper
) {
    override val generatedColumns = listOf("id", "placed_at", "updated_at")

    // Find order by order number
    suspend fun findByOrderNumber(orderNumber: String): Order? {
        val sql = "SELECT * FROM $tableName WHERE order_number = :orderNumber"

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("orderNumber", orderNumber)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Find orders by user
    suspend fun findByUserId(
        userId: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Order> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
            ORDER BY placed_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "userId" to userId,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Find orders by email
    suspend fun findByEmail(
        email: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Order> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE email = :email 
            ORDER BY placed_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "email" to email,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Find orders by status
    suspend fun findByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Order> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = :status 
            ORDER BY placed_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "status" to status,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Find orders by payment status
    suspend fun findByPaymentStatus(
        paymentStatus: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Order> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE payment_status = :paymentStatus 
            ORDER BY placed_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "paymentStatus" to paymentStatus,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Find orders by fulfillment status
    suspend fun findByFulfillmentStatus(
        fulfillmentStatus: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Order> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE fulfillment_status = :fulfillmentStatus 
            ORDER BY placed_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "fulfillmentStatus" to fulfillmentStatus,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Update order status
    suspend fun updateStatus(
        id: String,
        status: String,
        paymentStatus: String? = null,
        fulfillmentStatus: String? = null
    ): Order? {
        val setClauses = mutableListOf("status = :status", "updated_at = :updatedAt")
        val params = mutableMapOf<String, Any>(
            "id" to id,
            "status" to status,
            "updatedAt" to OffsetDateTime.now()
        )

        paymentStatus?.let {
            setClauses.add("payment_status = :paymentStatus")
            params["paymentStatus"] = it
        }

        fulfillmentStatus?.let {
            setClauses.add("fulfillment_status = :fulfillmentStatus")
            params["fulfillmentStatus"] = it
        }

        val sql = """
            UPDATE $tableName 
            SET ${setClauses.joinToString(", ")} 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val statement = connection.createStatement(sql)
            params.forEach { (key, value) -> statement.bind(key, value) }

            statement.execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Search orders
    suspend fun searchOrders(
        query: String,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): List<Order> {
        val statusFilter = if (status != null) "AND status = :status" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE (order_number ILIKE :query 
                   OR email ILIKE :query 
                   OR notes ILIKE :query) 
            $statusFilter
            ORDER BY placed_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf(
            "query" to "%$query%",
            "limit" to limit,
            "offset" to offset.toLong()
        )

        if (status != null) {
            params["status"] = status
        }

        return executeQuery(sql, params)
    }

    // Get orders by date range
    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<Order> {
        val statusFilter = if (status != null) "AND status = :status" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE placed_at BETWEEN :startDate AND :endDate 
            $statusFilter
            ORDER BY placed_at DESC 
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

    // Get order statistics
    suspend fun getOrderStats(
        startDate: OffsetDateTime? = null,
        endDate: OffsetDateTime? = null
    ): OrderStats {
        val dateFilter = if (startDate != null && endDate != null) {
            "WHERE placed_at BETWEEN :startDate AND :endDate"
        } else ""

        val sql = """
            SELECT 
                COUNT(*) as total_orders,
                COALESCE(SUM(total_amount), 0) as total_revenue,
                COALESCE(AVG(total_amount), 0) as average_order_value,
                COUNT(CASE WHEN status = 'pending' THEN 1 END) as pending,
                COUNT(CASE WHEN status = 'confirmed' THEN 1 END) as confirmed,
                COUNT(CASE WHEN status = 'processing' THEN 1 END) as processing,
                COUNT(CASE WHEN status = 'shipped' THEN 1 END) as shipped,
                COUNT(CASE WHEN status = 'delivered' THEN 1 END) as delivered,
                COUNT(CASE WHEN status = 'cancelled' THEN 1 END) as cancelled
            FROM $tableName 
            $dateFilter
        """.trimIndent()

        return connectionFactory.useConnection {
            val statement = createStatement(sql)
            if (startDate != null && endDate != null) {
                statement.bind("startDate", startDate)
                statement.bind("endDate", endDate)
            }

            statement.execute()
                .awaitSingle()
                .map { row, _ ->
                    OrderStats(
                        totalOrders = row.get("total_orders", Long::class.java)!!.toInt(),
                        totalRevenue = row.get("total_revenue", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        averageOrderValue = row.get("average_order_value", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        pending = row.get("pending", Long::class.java)!!.toInt(),
                        confirmed = row.get("confirmed", Long::class.java)!!.toInt(),
                        processing = row.get("processing", Long::class.java)!!.toInt(),
                        shipped = row.get("shipped", Long::class.java)!!.toInt(),
                        delivered = row.get("delivered", Long::class.java)!!.toInt(),
                        cancelled = row.get("cancelled", Long::class.java)!!.toInt()
                    )
                }
                .awaitFirstOrNull() ?: OrderStats(0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0, 0, 0, 0)
        }
    }

    // Generate order number
    suspend fun generateOrderNumber(): String {
        val prefix = "ORD"
        val date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        val sql = """
            SELECT COUNT(*) + 1 as next_sequence 
            FROM $tableName 
            WHERE DATE(placed_at) = CURRENT_DATE
        """.trimIndent()

        val sequence = connectionFactory.useConnection {
            createStatement(sql)
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("next_sequence", Long::class.java)!!.toInt() }
                .awaitFirstOrNull() ?: 1
        }

        return "$prefix-$date-${sequence.toString().padStart(4, '0')}"
    }
}

@Serializable
data class OrderStats(
    val totalOrders: Int,
    @Contextual
    val totalRevenue: BigDecimal,
    @Contextual
    val averageOrderValue: BigDecimal,
    val pending: Int,
    val confirmed: Int,
    val processing: Int,
    val shipped: Int,
    val delivered: Int,
    val cancelled: Int
)