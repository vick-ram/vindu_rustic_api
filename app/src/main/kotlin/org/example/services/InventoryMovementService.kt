package org.example.services

import org.example.data.cache.InventoryMovementCache
import org.example.data.repo.LowStockVariant
import org.example.data.repo.MovementSummary
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.inventory.InventoryMovement
import java.time.OffsetDateTime

@Component
class InventoryMovementService @Inject constructor(private val cache: InventoryMovementCache) {
    suspend fun createMovement(movement: InventoryMovement): InventoryMovement = cache.create(movement)
    suspend fun getMovement(id: String): InventoryMovement? = cache.read(id)
    suspend fun getMovements(offset: Int = 0, limit: Int = 50, queryParams: Map<String, String>? = null): List<InventoryMovement> = cache.readAll(offset, limit, queryParams)
    suspend fun getMovementsByVariant(variantId: String, offset: Int = 0, limit: Int = 50) = cache.findByVariantId(variantId, offset, limit)
    suspend fun getMovementsByWarehouse(warehouseId: String, offset: Int = 0, limit: Int = 50) = cache.findByWarehouse(warehouseId, offset, limit)
    suspend fun getMovementsByType(movementType: String, offset: Int = 0, limit: Int = 50) = cache.findByMovementType(movementType, offset, limit)
    suspend fun getMovementsByReference(referenceType: String, referenceId: Long) = cache.findByReference(referenceType, referenceId)
    suspend fun getCurrentStock(variantId: String, warehouseId: String): Int = cache.getCurrentStock(variantId, warehouseId)
    suspend fun getMovementSummary(startDate: OffsetDateTime, endDate: OffsetDateTime, warehouseId: String? = null): MovementSummary = cache.getMovementSummary(startDate, endDate, warehouseId)
    suspend fun getLowStockVariants(warehouseId: String, threshold: Int = 10): List<LowStockVariant> = cache.getLowStockVariants(warehouseId, threshold)
    suspend fun validateMovement(movement: InventoryMovement): Boolean = cache.validateMovement(movement)
}
