package org.example.data.mappers

import org.example.data.db.entities.ShipmentEntity
import org.example.data.db.tables.Orders
import org.example.data.db.tables.Warehouses
import org.example.domain.models.shipping.Shipment
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ShipmentMapper : EntityMapper<ShipmentEntity, Shipment, String> {
    override fun toModel(entity: ShipmentEntity): Shipment {
        return Shipment(
            id = entity.id.value,
            orderId = entity.orderId.value,
            warehouseId = entity.warehouseId?.value,
            courier = entity.courier,
            serviceLevel = entity.serviceLevel,
            trackingNumber = entity.trackingNumber,
            trackingUrl = entity.trackingUrl,
            shippingLabelUrl = entity.shippingLabelUrl,
            cost = entity.cost,
            estimatedDeliveryAt = entity.estimatedDeliveryAt,
            shippedAt = entity.createdAt,
            deliveredAt = entity.updatedAt
        )
    }

    override fun toEntity(model: Shipment, entity: ShipmentEntity): ShipmentEntity {
        entity.orderId = EntityID(model.orderId, Orders)
        entity.warehouseId = model.warehouseId?.let { EntityID(it, Warehouses) }
        entity.courier = model.courier
        entity.serviceLevel = model.serviceLevel
        entity.trackingNumber = model.trackingNumber
        entity.trackingUrl = model.trackingUrl
        entity.shippingLabelUrl = model.shippingLabelUrl
        entity.cost = model.cost
        entity.estimatedDeliveryAt = model.estimatedDeliveryAt
        return entity
    }
}