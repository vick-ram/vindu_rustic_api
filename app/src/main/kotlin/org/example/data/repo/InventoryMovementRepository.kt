package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.InventoryMovementMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.inventory.InventoryMovement
import org.koin.core.annotation.Single
import java.time.OffsetDateTime

@Component
class InventoryMovementRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    inventoryMovementMapper: InventoryMovementMapper
) : CrudRepository<InventoryMovement, String>(
    connectionFactory = connectionFactory,
    tableName = "inventory_movements",
    idColumn = "id",
    mapper = inventoryMovementMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get movements for a variant
    suspend fun findByVariantId(
        variantId: String,
        offset: Int = 0,
        limit: Int = 50
    ): List<InventoryMovement> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE variant_id = :variantId 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "variantId" to variantId,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get movements by warehouse
    suspend fun findByWarehouse(
        warehouseId: String,
        offset: Int = 0,
        limit: Int = 50
    ): List<InventoryMovement> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE warehouse_id = :warehouseId 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "warehouseId" to warehouseId,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get movements by type
    suspend fun findByMovementType(
        movementType: String,
        offset: Int = 0,
        limit: Int = 50
    ): List<InventoryMovement> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE movement_type = :movementType 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "movementType" to movementType,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get movements by reference
    suspend fun findByReference(
        referenceType: String,
        referenceId: Long
    ): List<InventoryMovement> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE reference_type = :referenceType 
              AND reference_id = :referenceId 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "referenceType" to referenceType,
            "referenceId" to referenceId
        ))
    }

    // Get current stock for a variant in a warehouse
    suspend fun getCurrentStock(variantId: String, warehouseId: String): Int {
        val sql = """
            SELECT COALESCE(SUM(
                CASE 
                    WHEN movement_type IN ('inbound', 'return', 'adjustment_in') THEN quantity
                    WHEN movement_type IN ('outbound', 'damage', 'adjustment_out') THEN -quantity
                    ELSE 0
                END
            ), 0) as current_stock
            FROM $tableName 
            WHERE variant_id = :variantId 
              AND warehouse_id = :warehouseId
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("variantId", variantId)
                .bind("warehouseId", warehouseId)
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("current_stock", Int::class.java) ?: 0 }
                .awaitFirstOrNull() ?: 0
        }
    }

    // Get stock movement summary for a date range
    suspend fun getMovementSummary(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        warehouseId: String? = null
    ): MovementSummary {
        val warehouseFilter = if (warehouseId != null) "AND warehouse_id = :warehouseId" else ""

        val sql = """
            SELECT 
                movement_type,
                SUM(quantity) as total_quantity,
                COUNT(*) as movement_count
            FROM $tableName 
            WHERE created_at BETWEEN :startDate AND :endDate 
            $warehouseFilter
            GROUP BY movement_type
            ORDER BY movement_type
        """.trimIndent()

        val params = mutableMapOf<String, Any>(
            "startDate" to startDate,
            "endDate" to endDate
        )

        if (warehouseId != null) {
            params["warehouseId"] = warehouseId
        }

        val results = connectionFactory.useConnection {
            createStatement(sql).apply {
                params.forEach { (key, value) -> bind(key, value) }
            }
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    MovementTypeSummary(
                        movementType = row.get("movement_type", String::class.java)!!,
                        totalQuantity = row.get("total_quantity", Long::class.java)!!.toInt(),
                        movementCount = row.get("movement_count", Long::class.java)!!.toInt()
                    )
                }
                .asFlow()
                .toList()
        }

        return MovementSummary(results)
    }

    // Get low stock variants
    suspend fun getLowStockVariants(
        warehouseId: String,
        threshold: Int = 10
    ): List<LowStockVariant> {
        val sql = """
            WITH current_stock AS (
                SELECT 
                    variant_id,
                    warehouse_id,
                    COALESCE(SUM(
                        CASE 
                            WHEN movement_type IN ('inbound', 'return', 'adjustment_in') THEN quantity
                            WHEN movement_type IN ('outbound', 'damage', 'adjustment_out') THEN -quantity
                            ELSE 0
                        END
                    ), 0) as stock_level
                FROM $tableName 
                WHERE warehouse_id = :warehouseId
                GROUP BY variant_id, warehouse_id
                HAVING COALESCE(SUM(
                    CASE 
                        WHEN movement_type IN ('inbound', 'return', 'adjustment_in') THEN quantity
                        WHEN movement_type IN ('outbound', 'damage', 'adjustment_out') THEN -quantity
                        ELSE 0
                    END
                ), 0) <= :threshold
            )
            SELECT * FROM current_stock
            ORDER BY stock_level ASC
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("warehouseId", warehouseId)
                .bind("threshold", threshold)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    LowStockVariant(
                        variantId = row.get("variant_id", String::class.java)!!,
                        warehouseId = row.get("warehouse_id", String::class.java)!!,
                        stockLevel = row.get("stock_level", Int::class.java)!!
                    )
                }
                .asFlow()
                .toList()
        }
    }

    // Validate movement (check if stock would go negative)
    suspend fun validateMovement(movement: InventoryMovement): Boolean {
        if (movement.movementType in listOf("outbound", "damage", "adjustment_out")) {
            val currentStock = getCurrentStock(movement.variantId, movement.warehouseId)
            return (currentStock - movement.quantity) >= 0
        }
        return true
    }
}

data class MovementTypeSummary(
    val movementType: String,
    val totalQuantity: Int,
    val movementCount: Int
)

data class MovementSummary(
    val summaries: List<MovementTypeSummary>
)

data class LowStockVariant(
    val variantId: String,
    val warehouseId: String,
    val stockLevel: Int
)