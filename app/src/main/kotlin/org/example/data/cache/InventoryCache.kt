package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.data.mappers.InventoryMapper
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.InventoryRepository
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.inventory.Inventory

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Component
class InventoryCache @Inject constructor(
    redis: RedisCoroutinesCommands<String, String>,
    inventoryMapper: InventoryMapper,
    private val inventoryRepository: InventoryRepository
): CrudCache<Inventory, String>(
    redis = redis,
    delegate = inventoryRepository,
    getId = {inventory -> inventoryMapper.getId(inventory) as String},
    serializer = Inventory.serializer(),
    config = object : CacheConfig {
        override val cacheName: String = "InventoryCache"
        override val ttl: Long = 3600L
    }
) {

    suspend fun getLowStockProducts(threshold: Int): List<Map<String, Any>> {
        return inventoryRepository.getLowStockProducts(threshold)
    }

    suspend fun reserveStock(variantId: String, warehouseId: String, quantity: Int): Boolean {
        return inventoryRepository.reserveStock(variantId, warehouseId, quantity)
    }

    suspend fun releaseReservation(variantId: String, warehouseId: String, quantity: Int): Boolean {
        return inventoryRepository.releaseReservation(variantId, warehouseId, quantity)
    }
}