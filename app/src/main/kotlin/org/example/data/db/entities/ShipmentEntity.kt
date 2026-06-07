package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.ShipmentEvents
import org.example.data.db.tables.Shipments
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ShipmentEntity(id: EntityID<String>) : CustomEntity(id, Shipments) {
    companion object : CustomEntityClass<ShipmentEntity>(Shipments)

    var orderId by Shipments.orderId
    var warehouseId by Shipments.warehouseId
    var courier by Shipments.courier
    var serviceLevel by Shipments.serviceLevel
    var trackingNumber by Shipments.trackingNumber
    var trackingUrl by Shipments.trackingUrl
    var shippingLabelUrl by Shipments.shippingLabelUrl
    var cost by Shipments.cost
    var estimatedDeliveryAt by Shipments.estimatedDeliveryAt

    val events by ShipmentEventEntity referrersOn ShipmentEvents.shipmentId
}