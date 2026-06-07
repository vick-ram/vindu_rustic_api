package org.example.data.mappers

import org.example.data.db.entities.OrderEntity
import org.example.data.db.tables.Addresses
import org.example.data.db.tables.Users
import org.example.domain.models.sales.Order
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object OrderMapper : EntityMapper<OrderEntity, Order, String> {
    override fun toModel(entity: OrderEntity): Order {
        return Order(
            id = entity.id.value,
            orderNumber = entity.orderNumber,
            userId = entity.userId?.value,
            email = entity.email,
            shippingAddressId = entity.shippingAddressId.value,
            billingAddressId = entity.billingAddressId?.value,
            status = entity.status,
            paymentStatus = entity.paymentStatus,
            fulfillmentStatus = entity.fulfillmentStatus,
            currency = entity.currency,
            subtotal = entity.subtotal,
            shippingCost = entity.shippingCost,
            taxAmount = entity.taxAmount,
            discountAmount = entity.discountAmount,
            totalAmount = entity.totalAmount,
            couponCode = entity.couponCode,
            notes = entity.notes,
            ipAddress = entity.ipAddress,
            userAgent = entity.userAgent,
            placedAt = entity.placedAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(model: Order, entity: OrderEntity): OrderEntity {
        entity.orderNumber = model.orderNumber
        entity.userId = model.userId?.let { EntityID(it, Users) }
        entity.email = model.email
        entity.shippingAddressId = EntityID(model.shippingAddressId, Addresses)
        entity.billingAddressId = model.billingAddressId?.let { EntityID(it, Addresses) }
        entity.status = model.status
        entity.paymentStatus = model.paymentStatus
        entity.fulfillmentStatus = model.fulfillmentStatus
        entity.currency = model.currency
        entity.subtotal = model.subtotal
        entity.shippingCost = model.shippingCost
        entity.taxAmount = model.taxAmount
        entity.discountAmount = model.discountAmount
        entity.totalAmount = model.totalAmount
        entity.couponCode = model.couponCode
        entity.notes = model.notes
        entity.ipAddress = model.ipAddress
        entity.userAgent = model.userAgent
        entity.placedAt = model.placedAt
        return entity
    }
}