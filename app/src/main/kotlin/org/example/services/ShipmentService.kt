package org.example.services

import org.example.data.cache.ShipmentCache
import org.example.data.repo.ShipmentEventRepository
import org.example.data.repo.ShipmentStats
import org.example.data.repo.ShipmentTimelineEvent
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.shipping.Shipment
import org.example.domain.models.shipping.ShipmentEvent
import java.time.OffsetDateTime

@Component
class ShipmentService @Inject constructor(
    private val shipmentCache: ShipmentCache,
    private val shipmentEventRepository: ShipmentEventRepository
) {
    suspend fun createShipment(shipment: Shipment): Shipment = shipmentCache.create(shipment)

    suspend fun getShipment(id: String): Shipment? = shipmentCache.read(id)

    suspend fun getShipments(
        offset: Int = 0,
        limit: Int = 20,
        queryParams: Map<String, String>? = null
    ): List<Shipment> = shipmentCache.readAll(offset, limit, queryParams)

    suspend fun updateShipment(id: String, shipment: Shipment): Shipment? = shipmentCache.update(id, shipment)

    suspend fun deleteShipment(id: String): Boolean = shipmentCache.delete(id)

    suspend fun getShipmentsByOrder(orderId: String): List<Shipment> = shipmentCache.findByOrderId(orderId)

    suspend fun getShipmentByTrackingNumber(trackingNumber: String): Shipment? =
        shipmentCache.findByTrackingNumber(trackingNumber)

    suspend fun getShipmentsByWarehouse(warehouseId: String, offset: Int = 0, limit: Int = 20): List<Shipment> =
        shipmentCache.findByWarehouse(warehouseId, offset, limit)

    suspend fun getShipmentsByCourier(courier: String, offset: Int = 0, limit: Int = 20): List<Shipment> =
        shipmentCache.findByCourier(courier, offset, limit)

    suspend fun getShipmentsByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        offset: Int = 0,
        limit: Int = 50
    ): List<Shipment> = shipmentCache.findByDateRange(startDate, endDate, offset, limit)

    suspend fun getInTransitShipments(limit: Int = 50): List<Shipment> = shipmentCache.getInTransitShipments(limit)

    suspend fun getOverdueShipments(): List<Shipment> = shipmentCache.getOverdueShipments()

    suspend fun getShipmentStats(startDate: OffsetDateTime? = null, endDate: OffsetDateTime? = null): ShipmentStats =
        shipmentCache.getShipmentStats(startDate, endDate)

    suspend fun updateTracking(
        id: String,
        trackingNumber: String,
        trackingUrl: String? = null,
        courier: String? = null
    ): Shipment? = shipmentCache.updateTracking(id, trackingNumber, trackingUrl, courier)

    suspend fun markDelivered(id: String, deliveredAt: OffsetDateTime = OffsetDateTime.now()): Shipment? =
        shipmentCache.markDelivered(id, deliveredAt)

    suspend fun updateShippingLabel(id: String, shippingLabelUrl: String): Shipment? =
        shipmentCache.updateShippingLabel(id, shippingLabelUrl)

    suspend fun createShipmentEvent(event: ShipmentEvent): ShipmentEvent {
        return shipmentEventRepository.create(event)
    }

    suspend fun createShipmentEvents(events: List<ShipmentEvent>): List<ShipmentEvent> {
        return shipmentEventRepository.bulkCreate(events)
    }

    suspend fun getShipmentEvent(id: String): ShipmentEvent? = shipmentEventRepository.read(id)

    suspend fun getShipmentEvents(shipmentId: String): List<ShipmentEvent> =
        shipmentEventRepository.findByShipmentId(shipmentId)

    suspend fun getLatestShipmentEvent(shipmentId: String): ShipmentEvent? =
        shipmentEventRepository.getLatestEvent(shipmentId)

    suspend fun getShipmentTimeline(shipmentId: String): List<ShipmentTimelineEvent> =
        shipmentEventRepository.getShipmentTimeline(shipmentId)

    suspend fun getShipmentEventsByType(eventType: String, offset: Int = 0, limit: Int = 50): List<ShipmentEvent> =
        shipmentEventRepository.findByEventType(eventType, offset, limit)

    suspend fun getShipmentEventsByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        shipmentId: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<ShipmentEvent> = shipmentEventRepository.findByDateRange(startDate, endDate, shipmentId, offset, limit)
}
