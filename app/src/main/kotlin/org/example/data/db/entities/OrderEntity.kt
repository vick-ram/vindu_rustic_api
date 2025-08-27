package org.example.data.db.entities

import org.example.data.db.tables.AddressTable
import org.example.data.db.tables.OrderItemTable
import org.example.data.db.tables.OrderTable
import org.example.data.db.tables.PaymentTable
import org.example.utils.CustomEntity
import org.example.utils.CustomEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class OrderEntity(id: EntityID<String>): CustomEntity(id, OrderTable) {
    companion object : CustomEntityClass<OrderEntity>(OrderTable)

    var user by UserEntity referencedOn OrderTable.user
    var orderNumber by OrderTable.orderNumber
    var status by OrderTable.status
    var paymentStatus by OrderTable.paymentStatus
    var shippingAddress by AddressEntity referencedOn OrderTable.shippingAddress
    var totalAmount by OrderTable.totalAmount
    var discount by DiscountEntity optionalReferencedOn OrderTable.discount
    var shippingFee by OrderTable.shippingFee
    var notes by OrderTable.notes
    var tsv by OrderTable.tsv

    val orderItems by OrderItemEntity referrersOn OrderItemTable.order
}


class OrderItemEntity(id: EntityID<String>): CustomEntity(id, OrderItemTable) {
    companion object : CustomEntityClass<OrderItemEntity>(OrderTable)

    var order by OrderEntity referencedOn OrderItemTable.order
    var product by ProductEntity referencedOn OrderItemTable.product
    var quantity by OrderItemTable.quantity
    var unitPrice by OrderItemTable.unitPrice
    var totalPrice by OrderItemTable.totalPrice
}

class AddressEntity(id: EntityID<String>): CustomEntity(id, AddressTable) {
    companion object : CustomEntityClass<AddressEntity>(AddressTable)

    var user by UserEntity referencedOn AddressTable.user
    var fullName by AddressTable.fullName
    var phone by AddressTable.phone
    var email by AddressTable.email
    var street by AddressTable.street
    var county by AddressTable.county
    var region by AddressTable.region
    var postalCode by AddressTable.postalCode
}


class PaymentEntity(id: EntityID<String>): CustomEntity(id, PaymentTable) {
    companion object : CustomEntityClass<PaymentEntity>(PaymentTable)

    var order by OrderEntity referencedOn PaymentTable.order
    var user by UserEntity referencedOn PaymentTable.user
    var amount by PaymentTable.amount
    var status by PaymentTable.status
    var method by PaymentTable.method
    var transactionReference by PaymentTable.transactionReference
    var paidAt by PaymentTable.paidAt
}