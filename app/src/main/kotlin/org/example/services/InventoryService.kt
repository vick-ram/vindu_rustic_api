package org.example.services

import org.example.data.cache.InventoryCache
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.inventory.Inventory

@Component
class InventoryService @Inject constructor(private val cache: InventoryCache) {
    suspend fun createInventory(inventory: Inventory): Inventory = cache.create(inventory)
    suspend fun getInventory(id: String): Inventory? = cache.read(id)
    suspend fun getInventories(offset: Int = 0, limit: Int = 20, queryParams: Map<String, String>? = null): List<Inventory> =
        cache.readAll(offset, limit, queryParams)
    suspend fun updateInventory(id: String, inventory: Inventory): Inventory? = cache.update(id, inventory)
    suspend fun deleteInventory(id: String): Boolean = cache.delete(id)
    suspend fun getLowStockProducts(threshold: Int): List<Map<String, Any>> = cache.getLowStockProducts(threshold)
    suspend fun reserveStock(variantId: String, warehouseId: String, quantity: Int): Boolean = cache.reserveStock(variantId, warehouseId, quantity)
    suspend fun releaseReservation(variantId: String, warehouseId: String, quantity: Int): Boolean = cache.releaseReservation(variantId, warehouseId, quantity)
}
