package org.example.data.repo

import org.example.data.db.entities.InventoryEntity
import org.example.data.db.tables.Inventories
import org.example.data.mappers.InventoryMapper
import org.example.domain.models.inventory.Inventory
import org.example.domain.repo.InventoryRepository
import org.example.plugins.NotFoundException
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.and

class InventoryRepositoryImpl(private val inventoryMapper: InventoryMapper) :
    CrudRepositoryImpl<InventoryEntity, Inventory>(InventoryEntity, Inventory::class),
    InventoryRepository {

    override suspend fun findByVariantAndWarehouse(variantId: String, warehouseId: String): Inventory? = suspendTransaction {
        InventoryEntity.find {
            (Inventories.variantId eq variantId) and (Inventories.warehouseId eq warehouseId)
        }.firstOrNull()?.toDomain()
    }

    override suspend fun getLowStockProducts(threshold: Int): List<Map<String, Any>> = suspendTransaction {
        exec("SELECT * FROM low_stock_products WHERE total_available <= $threshold") { rs ->
            val results = mutableListOf<Map<String, Any>>()
            while (rs.next()) {
                val row = mutableMapOf<String, Any>()
                for (i in 1..rs.metaData.columnCount) {
                    row[rs.metaData.getColumnName(i)] = rs.getObject(i) ?: ""
                }
                results.add(row)
            }
            results
        } ?: emptyList()
    }

    override suspend fun reserveStock(variantId: String, warehouseId: String, quantity: Int): Boolean = suspendTransaction {
        val inventory = InventoryEntity.find {
            (Inventories.variantId eq variantId) and (Inventories.warehouseId eq warehouseId)
        }.firstOrNull() ?: throw NotFoundException("Inventory not found")

        if (inventory.availableQuantity < quantity) {
            throw IllegalStateException("Insufficient stock")
        }

        inventory.availableQuantity -= quantity
        inventory.reservedQuantity += quantity
        true
    }

    override suspend fun releaseReservation(variantId: String, warehouseId: String, quantity: Int): Boolean = suspendTransaction {
        val inventory = InventoryEntity.find {
            (Inventories.variantId eq variantId) and (Inventories.warehouseId eq warehouseId)
        }.firstOrNull() ?: throw NotFoundException("Inventory not found")

        inventory.availableQuantity += quantity
        inventory.reservedQuantity -= quantity
        true
    }

    override fun InventoryEntity.toDomain(): Inventory = inventoryMapper.toModel(this)
    override fun Inventory.toEntity(entity: InventoryEntity) {
        inventoryMapper.toEntity(this, entity)
    }
    override fun getId(domain: Inventory): String = domain.id.toString()
}