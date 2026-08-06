package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Serializable
import org.example.data.mappers.WarehouseMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.inventory.Warehouse

@Component
class WarehouseRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    warehouseMapper: WarehouseMapper
) : CrudRepository<Warehouse, String>(
    connectionFactory = connectionFactory,
    tableName = "warehouses",
    idColumn = "id",
    mapper = warehouseMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Find active warehouses
    suspend fun findActive(): List<Warehouse> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE is_active = true 
            ORDER BY name ASC
        """.trimIndent()

        return executeQuery(sql)
    }

    // Search warehouses by name
    suspend fun searchByName(
        query: String,
        includeInactive: Boolean = false
    ): List<Warehouse> {
        val activeFilter = if (!includeInactive) "AND is_active = true" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE name ILIKE :query $activeFilter
            ORDER BY name ASC
        """.trimIndent()

        return executeQuery(sql, mapOf("query" to "%$query%"))
    }

    // Get warehouse with address details
    suspend fun findWithAddress(warehouseId: String): WarehouseWithAddress? {
        val sql = """
            SELECT w.*, 
                   a.recipient_name as address_name,
                   a.address_line1,
                   a.address_line2,
                   a.city,
                   a.state,
                   a.country,
                   a.postal_code
            FROM $tableName w
            LEFT JOIN addresses a ON w.address_id = a.id
            WHERE w.id = :warehouseId
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("warehouseId" to warehouseId))
                .execute()
                .awaitSingle()
                .map { row, metadata ->
                    WarehouseWithAddress(
                        warehouse = rowMapper.apply(row, metadata),
                        addressName = row.get("address_name", String::class.java),
                        addressLine1 = row.get("address_line1", String::class.java),
                        addressLine2 = row.get("address_line2", String::class.java),
                        city = row.get("city", String::class.java),
                        state = row.get("state", String::class.java),
                        country = row.get("country", String::class.java),
                        postalCode = row.get("postal_code", String::class.java)
                    )
                }
                .awaitFirstOrNull()
        }
    }

    // Activate warehouse
    suspend fun activate(id: String): Warehouse? {
        val sql = """
            UPDATE $tableName 
            SET is_active = true 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("id" to id))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Deactivate warehouse
    suspend fun deactivate(id: String): Warehouse? {
        val sql = """
            UPDATE $tableName 
            SET is_active = false 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("id" to id))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get warehouse stock summary
    suspend fun getWarehouseStockSummary(warehouseId: String): WarehouseStockSummary {
        val sql = """
            WITH stock_levels AS (
                SELECT 
                    variant_id,
                    COALESCE(SUM(
                        CASE 
                            WHEN movement_type IN ('inbound', 'return', 'adjustment_in') THEN quantity
                            WHEN movement_type IN ('outbound', 'damage', 'adjustment_out') THEN -quantity
                            ELSE 0
                        END
                    ), 0) as stock_level
                FROM inventory_movements 
                WHERE warehouse_id = :warehouseId
                GROUP BY variant_id
            )
            SELECT 
                COUNT(*) as total_products,
                COUNT(CASE WHEN stock_level > 0 THEN 1 END) as in_stock,
                COUNT(CASE WHEN stock_level = 0 THEN 1 END) as out_of_stock,
                COUNT(CASE WHEN stock_level < 10 THEN 1 END) as low_stock,
                SUM(stock_level) as total_units
            FROM stock_levels
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("warehouseId" to warehouseId))
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    WarehouseStockSummary(
                        totalProducts = (row.get("total_products", Long::class.java) ?: 0L).toInt(),
                        inStock = (row.get("in_stock", Long::class.java) ?: 0L).toInt(),
                        outOfStock = (row.get("out_of_stock", Long::class.java) ?: 0L).toInt(),
                        lowStock = (row.get("low_stock", Long::class.java) ?: 0L).toInt(),
                        totalUnits = (row.get("total_units", Long::class.java) ?: 0L).toInt()
                    )
                }
                .awaitFirstOrNull() ?: WarehouseStockSummary(0, 0, 0, 0, 0)
        }
    }

    // Validate warehouse name uniqueness
    suspend fun isNameUnique(name: String, excludeId: String? = null): Boolean {
        val params = mutableMapOf<String, Any>("name" to name)

        val sql = if (excludeId != null) {
            params["excludeId"] to excludeId
            "SELECT COUNT(*) as count FROM $tableName WHERE LOWER(name) = LOWER(:name) AND id != :excludeId"
        } else {
            "SELECT COUNT(*) as count FROM $tableName WHERE LOWER(name) = LOWER(:name)"
        }

        return connectionFactory.useConnection {
             createNamedStatement(sql, params)
            .execute()
                .awaitSingle()
                .map { row, _ -> row.get("count", Long::class.java) == 0L }
                .awaitFirstOrNull() ?: true
        }
    }
}

@Serializable
data class WarehouseWithAddress(
    val warehouse: Warehouse,
    val addressName: String?,
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val postalCode: String?
)

@Serializable
data class WarehouseStockSummary(
    val totalProducts: Int,
    val inStock: Int,
    val outOfStock: Int,
    val lowStock: Int,
    val totalUnits: Int
)