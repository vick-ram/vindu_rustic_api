package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.ShipmentEventMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.shipping.ShipmentEvent
import org.koin.core.annotation.Single
import java.time.OffsetDateTime

@Component
class ShipmentEventRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    shipmentEventMapper: ShipmentEventMapper
) : CrudRepository<ShipmentEvent, String>(
    connectionFactory = connectionFactory,
    tableName = "shipment_events",
    mapper = shipmentEventMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get events for a shipment
    suspend fun findByShipmentId(shipmentId: String): List<ShipmentEvent> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE shipment_id = :shipmentId 
            ORDER BY occurred_at DESC, created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("shipmentId" to shipmentId))
    }

    // Get events by type
    suspend fun findByEventType(
        eventType: String,
        offset: Int = 0,
        limit: Int = 50
    ): List<ShipmentEvent> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE event_type = :eventType 
            ORDER BY occurred_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "eventType" to eventType,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Get latest event for a shipment
    suspend fun getLatestEvent(shipmentId: String): ShipmentEvent? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE shipment_id = :shipmentId 
            ORDER BY occurred_at DESC, created_at DESC 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("shipmentId" to shipmentId))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get event timeline for a shipment
    suspend fun getShipmentTimeline(shipmentId: String): List<ShipmentTimelineEvent> {
        val sql = """
            SELECT 
                se.*,
                LAG(se.status) OVER (ORDER BY se.occurred_at, se.created_at) as previous_status,
                LAG(se.location) OVER (ORDER BY se.occurred_at, se.created_at) as previous_location,
                EXTRACT(EPOCH FROM (se.occurred_at - LAG(se.occurred_at) 
                    OVER (ORDER BY se.occurred_at, se.created_at))) / 3600.0 as hours_since_last_event
            FROM $tableName se
            WHERE se.shipment_id = :shipmentId
            ORDER BY se.occurred_at DESC, se.created_at DESC
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("shipmentId" to shipmentId))
                .execute()
                .awaitSingle()
                .map { row, rowMetadata ->
                    ShipmentTimelineEvent(
                        event = rowMapper.apply(row, rowMetadata),
                        previousStatus = row.get("previous_status", String::class.java),
                        previousLocation = row.get("previous_location", String::class.java),
                        hoursSinceLastEvent = row.get("hours_since_last_event", Double::class.java)
                    )
                }
                .asFlow()
                .toList()
        }
    }

    // Bulk create events
    suspend fun bulkCreate(events: List<ShipmentEvent>): List<ShipmentEvent> {
        if (events.isEmpty()) return emptyList()

        val columns = listOf("shipment_id", "event_type", "status", "location", "description", "occurred_at")
        val placeholders = List(events.size) { index ->
            "(${columns.joinToString(", ") { ":${it}_$index" }})"
        }

        val sql = """
            INSERT INTO $tableName (${columns.joinToString(", ")})
            VALUES ${placeholders.joinToString(", ")}
            RETURNING *
        """.trimIndent()

        val params = mutableMapOf<String, Any?>()
        events.forEachIndexed { index, event ->
            params["shipment_id_$index"] = event.shipmentId
            params["event_type_$index"] = event.eventType
            params["status_$index"] = event.status
            params["location_$index"] = event.location
            params["description_$index"] = event.description
            params["occurred_at_$index"] = event.occurredAt
        }

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, params)
            .execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }

    // Get events by date range
    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        shipmentId: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<ShipmentEvent> {
        val shipmentFilter = if (shipmentId != null) "AND shipment_id = :shipmentId" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE (occurred_at BETWEEN :startDate AND :endDate 
                   OR created_at BETWEEN :startDate AND :endDate) 
            $shipmentFilter
            ORDER BY occurred_at DESC, created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf<String, Any>(
            "startDate" to startDate,
            "endDate" to endDate,
            "limit" to limit,
            "offset" to offset.toLong()
        )

        if (shipmentId != null) {
            params["shipmentId"] = shipmentId
        }

        return executeQuery(sql, params)
    }
}

data class ShipmentTimelineEvent(
    val event: ShipmentEvent,
    val previousStatus: String?,
    val previousLocation: String?,
    val hoursSinceLastEvent: Double?
)