package org.example.domain.repo

import org.example.domain.models.inventory.Inventory

interface InventoryRepository : CrudRepository<Inventory, String> {
    suspend fun findByVariantAndWarehouse(variantId: String, warehouseId: String): Inventory?
    suspend fun getLowStockProducts(threshold: Int): List<Map<String, Any>>
    suspend fun reserveStock(variantId: String, warehouseId: String, quantity: Int): Boolean
    suspend fun releaseReservation(variantId: String, warehouseId: String, quantity: Int): Boolean
}