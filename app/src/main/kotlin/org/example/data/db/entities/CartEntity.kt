package org.example.data.db.entities

import org.example.data.db.tables.CartItemTable
import org.example.data.db.tables.CartTable
import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class CartEntity(id: EntityID<String>): CustomEntity(id, CartTable) {
    companion object : CustomEntityClass<CartEntity>(CartTable)

    var user by UserEntity referencedOn CartTable.user
    var totalQuantity by CartTable.totalQuantity
    var totalPrice by CartTable.totalPrice
    var discount by DiscountEntity optionalReferencedOn CartTable.discount

    val items by CartItemEntity referrersOn CartTable.id
}

class CartItemEntity(id: EntityID<String>): CustomEntity(id, CartTable) {
    companion object : CustomEntityClass<CartItemEntity>(CartTable)

    var cart by CartEntity referencedOn CartItemTable.cart
    var product by ProductEntity referencedOn CartItemTable.product
    var quantity by CartItemTable.quantity
    var unitPrice by CartItemTable.unitPrice
    var totalPrice by CartItemTable.totalPrice
}