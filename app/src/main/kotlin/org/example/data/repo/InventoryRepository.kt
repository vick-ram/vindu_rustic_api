package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.InventoryMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.inventory.Inventory
import org.example.plugins.NotFoundException
import org.koin.core.annotation.Single
import java.time.OffsetDateTime

@Component
class InventoryRepository @Inject constructor(connectionFactory: ConnectionFactory, inventoryMapper: InventoryMapper) :
    CrudRepository<Inventory, String>(connectionFactory = connectionFactory, tableName = "inventories", mapper = inventoryMapper){

    suspend fun findByVariantAndWarehouse(variantId: String, warehouseId: String): Inventory? {
        val sql = "SELECT * FROM inventories WHERE variant_id = :variantId AND warehouse_id = :warehouseId"
        return executeQuery(
            sql = sql,
            params = mapOf("variantId" to variantId, "warehouseId" to warehouseId),
            mapper = rowMapper
        ).firstOrNull()
    }

    suspend fun getLowStockProducts(threshold: Int): List<Map<String, Any>> {
        val sql = "SELECT * FROM low_stock_products WHERE total_available <= :threshold"

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("threshold" to threshold))
                .execute()
                .awaitSingle()
                .map { row, metadata ->
                    val rowMap = mutableMapOf<String, Any>()
                    metadata.columnMetadatas.forEach { column ->
                        val label = column.name
                        rowMap[label] = row.get(label) ?: ""
                    }
                    rowMap
                }
                .asFlow()
                .toList()
        }
    }

    suspend fun reserveStock(variantId: String, warehouseId: String, quantity: Int): Boolean {
        val sql = """
            UPDATE inventories 
            SET available_quantity = available_quantity - :quantity,
                reserved_quantity = reserved_quantity + :quantity,
                updated_at = :now
            WHERE variant_id = :variantId 
              AND warehouse_id = :warehouseId 
              AND available_quantity >= :quantity
        """.trimIndent()

        val rowsUpdated = connectionFactory.useConnection {
            createNamedStatement(sql, mapOf(
                "quantity" to quantity,
                "now" to OffsetDateTime.now(),
                "variantId" to variantId,
                "warehouseId" to warehouseId
            ))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
        }

        if (rowsUpdated == 0L) {
            // Verify if it didn't exist at all, or simply lacked sufficient stock
            val currentStock = findByVariantAndWarehouse(variantId, warehouseId)
                ?: throw NotFoundException("Inventory not found")

            if (currentStock.availableQuantity < quantity) {
                throw IllegalStateException("Insufficient stock")
            }
        }

        return true
    }

    suspend fun releaseReservation(variantId: String, warehouseId: String, quantity: Int): Boolean {
        val sql = """
            UPDATE inventories 
            SET available_quantity = available_quantity + :quantity,
                reserved_quantity = reserved_quantity - :quantity,
                updated_at = :now
            WHERE variant_id = :variantId 
              AND warehouse_id = :warehouseId
        """.trimIndent()

        val rowsUpdated = connectionFactory.useConnection {
            createNamedStatement(sql, mapOf(
                "quantity" to quantity,
                "now" to OffsetDateTime.now(),
                "variantId" to variantId,
                "warehouseId" to warehouseId
            ))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
        }

        if (rowsUpdated == 0L) {
            throw NotFoundException("Inventory record not found")
        }

        return true
    }
}