package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.InventoryReservationMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.inventory.InventoryReservation
import java.time.OffsetDateTime

@Component
class InventoryReservationRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    inventoryReservationMapper: InventoryReservationMapper
) : CrudRepository<InventoryReservation, String>(
    connectionFactory = connectionFactory,
    tableName = "inventory_reservations",
    mapper = inventoryReservationMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get reservations for a variant
    suspend fun findByVariantId(variantId: String): List<InventoryReservation> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE variant_id = :variantId 
              AND status = 'active'
              AND expires_at > :now
            ORDER BY expires_at ASC
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "variantId" to variantId,
            "now" to OffsetDateTime.now()
        ))
    }

    // Get reservations for a cart
    suspend fun findByCartId(cartId: String): List<InventoryReservation> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE cart_id = :cartId 
              AND status = 'active'
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("cartId" to cartId))
    }

    // Get reservations for an order
    suspend fun findByOrderId(orderId: String): List<InventoryReservation> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE order_id = :orderId 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("orderId" to orderId))
    }

    // Get reserved quantity for a variant in a warehouse
    suspend fun getReservedQuantity(
        variantId: String,
        warehouseId: String
    ): Int {
        val sql = """
            SELECT COALESCE(SUM(quantity), 0) as reserved_quantity
            FROM $tableName 
            WHERE variant_id = :variantId 
              AND warehouse_id = :warehouseId 
              AND status = 'active'
              AND expires_at > :now
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf(
                "variantId" to variantId,
                "warehouseId" to warehouseId,
                "now" to OffsetDateTime.now()
            ))
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("reserved_quantity", Int::class.java) ?: 0 }
                .awaitFirstOrNull() ?: 0
        }
    }

    // Reserve inventory
    suspend fun reserve(
        variantId: String,
        warehouseId: String,
        quantity: Int,
        cartId: String? = null,
        orderId: String? = null,
        durationMinutes: Long = 15
    ): InventoryReservation {
        val reservation = InventoryReservation(
            variantId = variantId,
            warehouseId = warehouseId,
            cartId = cartId,
            orderId = orderId,
            quantity = quantity,
            expiresAt = OffsetDateTime.now().plusMinutes(durationMinutes)
        )
        return create(reservation)
    }

    // Confirm reservation (convert to order)
    suspend fun confirmReservation(id: String, orderId: String): InventoryReservation? {
        val sql = """
            UPDATE $tableName 
            SET order_id = :orderId,
                cart_id = NULL,
                expires_at = NULL,
                status = 'confirmed'
            WHERE id = :id 
              AND status = 'active'
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("id" to id, "orderId" to orderId))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Cancel reservation
    suspend fun cancelReservation(id: String): Boolean {
        val sql = """
            UPDATE $tableName 
            SET status = 'cancelled' 
            WHERE id = :id
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("id" to id))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle() > 0
        }
    }

    // Cancel all reservations for a cart
    suspend fun cancelCartReservations(cartId: String): Int {
        val sql = """
            UPDATE $tableName 
            SET status = 'cancelled' 
            WHERE cart_id = :cartId 
              AND status = 'active'
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("cartId" to cartId))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Expire old reservations
    suspend fun expireOldReservations(): Int {
        val sql = """
            UPDATE $tableName 
            SET status = 'expired' 
            WHERE status = 'active' 
              AND expires_at <= :now
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("now" to OffsetDateTime.now()))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Get available quantity (excluding reservations)
    suspend fun getAvailableQuantity(
        variantId: String,
        warehouseId: String,
        totalStock: Int
    ): Int {
        val reserved = getReservedQuantity(variantId, warehouseId)
        return totalStock - reserved
    }

    // Extend reservation time
    suspend fun extendReservation(id: String, additionalMinutes: Long = 15): InventoryReservation? {
        val sql = """
            UPDATE $tableName 
            SET expires_at = expires_at + make_interval(mins => :additionalMinutes)
            WHERE id = :id 
              AND status = 'active'
              AND expires_at > :now
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("id" to id, "additionalMinutes" to additionalMinutes, "now" to OffsetDateTime.now()))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }
}