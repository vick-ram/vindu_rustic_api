package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.ShipmentMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.shipping.Shipment
import org.koin.core.annotation.Single
import java.math.BigDecimal
import java.time.OffsetDateTime

@Component
class ShipmentRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    shipmentMapper: ShipmentMapper
) : CrudRepository<Shipment, String>(
    connectionFactory = connectionFactory,
    tableName = "shipments",
    mapper = shipmentMapper
) {
    override val generatedColumns = listOf("id", "shipped_at")

    // Get shipments for an order
    suspend fun findByOrderId(orderId: String): List<Shipment> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE order_id = :orderId 
            ORDER BY shipped_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("orderId" to orderId))
    }

    // Get shipment by tracking number
    suspend fun findByTrackingNumber(trackingNumber: String): Shipment? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE tracking_number = :trackingNumber 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("trackingNumber", trackingNumber)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get shipments by warehouse
    suspend fun findByWarehouse(
        warehouseId: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Shipment> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE warehouse_id = :warehouseId 
            ORDER BY shipped_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "warehouseId" to warehouseId,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get shipments by courier
    suspend fun findByCourier(
        courier: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Shipment> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE courier = :courier 
            ORDER BY shipped_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "courier" to courier,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Update tracking information
    suspend fun updateTracking(
        id: String,
        trackingNumber: String,
        trackingUrl: String? = null,
        courier: String? = null
    ): Shipment? {
        val setClauses = mutableListOf(
            "tracking_number = :trackingNumber"
        )
        val params = mutableMapOf<String, Any>(
            "id" to id,
            "trackingNumber" to trackingNumber
        )

        trackingUrl?.let {
            setClauses.add("tracking_url = :trackingUrl")
            params["trackingUrl"] = it
        }

        courier?.let {
            setClauses.add("courier = :courier")
            params["courier"] = it
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

    // Mark shipment as delivered
    suspend fun markDelivered(
        id: String,
        deliveredAt: OffsetDateTime = OffsetDateTime.now()
    ): Shipment? {
        val sql = """
            UPDATE $tableName 
            SET delivered_at = :deliveredAt 
            WHERE id = :id 
              AND delivered_at IS NULL
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("deliveredAt", deliveredAt)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get shipments by date range
    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        offset: Int = 0,
        limit: Int = 50
    ): List<Shipment> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE shipped_at BETWEEN :startDate AND :endDate 
            ORDER BY shipped_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "startDate" to startDate,
            "endDate" to endDate,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get shipments that are in transit (shipped but not delivered)
    suspend fun getInTransitShipments(limit: Int = 50): List<Shipment> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE shipped_at IS NOT NULL 
              AND delivered_at IS NULL 
            ORDER BY shipped_at DESC 
            LIMIT :limit
        """.trimIndent()

        return executeQuery(sql, mapOf("limit" to limit))
    }

    // Get overdue shipments (past estimated delivery date)
    suspend fun getOverdueShipments(): List<Shipment> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE delivered_at IS NULL 
              AND estimated_delivery_at IS NOT NULL 
              AND estimated_delivery_at < :now 
            ORDER BY estimated_delivery_at ASC
        """.trimIndent()

        return executeQuery(sql, mapOf("now" to OffsetDateTime.now()))
    }

    // Get shipment statistics
    suspend fun getShipmentStats(
        startDate: OffsetDateTime? = null,
        endDate: OffsetDateTime? = null
    ): ShipmentStats {
        val dateFilter = if (startDate != null && endDate != null) {
            "WHERE shipped_at BETWEEN :startDate AND :endDate"
        } else ""

        val sql = """
            SELECT 
                COUNT(*) as total_shipments,
                COUNT(CASE WHEN delivered_at IS NOT NULL THEN 1 END) as delivered,
                COUNT(CASE WHEN delivered_at IS NULL THEN 1 END) as in_transit,
                COALESCE(AVG(EXTRACT(EPOCH FROM (delivered_at - shipped_at)) / 3600.0), 0) as avg_delivery_hours,
                COALESCE(SUM(cost), 0) as total_shipping_cost,
                COUNT(DISTINCT courier) as unique_couriers
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
                    ShipmentStats(
                        totalShipments = row.get("total_shipments", Long::class.java)!!.toInt(),
                        delivered = row.get("delivered", Long::class.java)!!.toInt(),
                        inTransit = row.get("in_transit", Long::class.java)!!.toInt(),
                        avgDeliveryHours = row.get("avg_delivery_hours", Double::class.java) ?: 0.0,
                        totalShippingCost = row.get("total_shipping_cost", BigDecimal::class.java) ?: BigDecimal.ZERO,
                        uniqueCouriers = row.get("unique_couriers", Long::class.java)!!.toInt()
                    )
                }
                .awaitFirstOrNull() ?: ShipmentStats(0, 0, 0, 0.0, BigDecimal.ZERO, 0)
        }
    }

    // Update shipping label URL
    suspend fun updateShippingLabel(id: String, shippingLabelUrl: String): Shipment? {
        val sql = """
            UPDATE $tableName 
            SET shipping_label_url = :shippingLabelUrl 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("shippingLabelUrl", shippingLabelUrl)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }
}

data class ShipmentStats(
    val totalShipments: Int,
    val delivered: Int,
    val inTransit: Int,
    val avgDeliveryHours: Double,
    val totalShippingCost: BigDecimal,
    val uniqueCouriers: Int
)