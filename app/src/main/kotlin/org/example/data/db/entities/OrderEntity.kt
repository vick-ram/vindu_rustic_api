package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.OrderItems
import org.example.data.db.tables.OrderStatusHistories
import org.example.data.db.tables.Orders
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class OrderEntity(id: EntityID<String>) : CustomEntity(id, Orders) {
    companion object : CustomEntityClass<OrderEntity>(Orders)

    var orderNumber by Orders.orderNumber
    var userId by Orders.userId
    var email by Orders.email
    var shippingAddressId by Orders.shippingAddressId
    var billingAddressId by Orders.billingAddressId
    var status by Orders.status
    var paymentStatus by Orders.paymentStatus
    var fulfillmentStatus by Orders.fulfillmentStatus
    var currency by Orders.currency
    var subtotal by Orders.subtotal
    var shippingCost by Orders.shippingCost
    var taxAmount by Orders.taxAmount
    var discountAmount by Orders.discountAmount
    var totalAmount by Orders.totalAmount
    var couponCode by Orders.couponCode
    var notes by Orders.notes
    var ipAddress by Orders.ipAddress
    var userAgent by Orders.userAgent
    var placedAt by Orders.placedAt

    val items by OrderItemEntity referrersOn OrderItems.orderId
    val statusHistory by OrderStatusHistoryEntity referrersOn OrderStatusHistories.orderId
}
