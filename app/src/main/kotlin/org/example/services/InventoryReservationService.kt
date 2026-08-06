package org.example.services

import org.example.data.cache.InventoryReservationCache
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.inventory.InventoryReservation

@Component
class InventoryReservationService @Inject constructor(private val cache: InventoryReservationCache) {
    suspend fun getReservation(id: String): InventoryReservation? = cache.read(id)
    suspend fun getReservationsByVariant(variantId: String) = cache.findByVariantId(variantId)
    suspend fun getReservationsByCart(cartId: String) = cache.findByCartId(cartId)
    suspend fun getReservationsByOrder(orderId: String) = cache.findByOrderId(orderId)
    suspend fun getReservedQuantity(variantId: String, warehouseId: String): Int = cache.getReservedQuantity(variantId, warehouseId)
    suspend fun getAvailableQuantity(variantId: String, warehouseId: String, totalStock: Int): Int = cache.getAvailableQuantity(variantId, warehouseId, totalStock)
    suspend fun reserve(variantId: String, warehouseId: String, quantity: Int, cartId: String? = null, orderId: String? = null, durationMinutes: Long = 15): InventoryReservation =
        cache.reserve(variantId, warehouseId, quantity, cartId, orderId, durationMinutes)
    suspend fun confirmReservation(id: String, orderId: String) = cache.confirmReservation(id, orderId)
    suspend fun cancelReservation(id: String): Boolean = cache.cancelReservation(id)
    suspend fun cancelCartReservations(cartId: String): Int = cache.cancelCartReservations(cartId)
    suspend fun expireOldReservations(): Int = cache.expireOldReservations()
    suspend fun extendReservation(id: String, additionalMinutes: Long = 15) = cache.extendReservation(id, additionalMinutes)
}
