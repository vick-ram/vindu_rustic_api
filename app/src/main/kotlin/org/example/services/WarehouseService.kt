package org.example.services

import org.example.data.cache.WarehouseCache
import org.example.data.repo.WarehouseStockSummary
import org.example.data.repo.WarehouseWithAddress
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.inventory.Warehouse

@Component
class WarehouseService @Inject constructor(private val cache: WarehouseCache) {
    suspend fun createWarehouse(warehouse: Warehouse): Warehouse = cache.create(warehouse)
    suspend fun getWarehouse(id: String): Warehouse? = cache.read(id)
    suspend fun getWarehouses(offset: Int = 0, limit: Int = 20, queryParams: Map<String, String>? = null): List<Warehouse> = cache.readAll(offset, limit, queryParams)
    suspend fun updateWarehouse(id: String, warehouse: Warehouse): Warehouse? = cache.update(id, warehouse)
    suspend fun deleteWarehouse(id: String): Boolean = cache.delete(id)
    suspend fun getActiveWarehouses(): List<Warehouse> = cache.findActive()
    suspend fun searchWarehouses(query: String, includeInactive: Boolean = false): List<Warehouse> = cache.searchByName(query, includeInactive)
    suspend fun getWarehouseWithAddress(id: String): WarehouseWithAddress? = cache.findWithAddress(id)
    suspend fun getWarehouseStockSummary(id: String): WarehouseStockSummary = cache.getWarehouseStockSummary(id)
    suspend fun isNameUnique(name: String, excludeId: String? = null): Boolean = cache.isNameUnique(name, excludeId)
    suspend fun activateWarehouse(id: String): Warehouse? = cache.activate(id)
    suspend fun deactivateWarehouse(id: String): Warehouse? = cache.deactivate(id)
}
