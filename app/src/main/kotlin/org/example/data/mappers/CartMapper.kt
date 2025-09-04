package org.example.data.mappers

import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.CartEntity
import org.example.data.db.entities.CartItemEntity
import org.example.data.db.entities.ProductEntity
import org.example.data.db.entities.UserEntity
import org.example.domain.models.Cart
import org.example.domain.models.CartItem
import org.example.domain.repo.EntityMapper
import org.example.utils.now

object CartMapper : EntityMapper<CartEntity, Cart, String> {
    override fun toModel(entity: CartEntity): Cart {
        return Cart(
            id = entity.id.value,
            userId = entity.user.id.value,
            items = entity.items.map { CartItemMapper.toModel(it) },
            totalQuantity = entity.totalQuantity,
            totalPrice = entity.totalPrice,
            discount = entity.discount?.let { DiscountMapper.toModel(it) },
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    override fun toEntity(
        model: Cart,
        entity: CartEntity
    ): CartEntity {
        val entityDiscount = entity.discount
        val modelDiscount = model.discount
        entity.user = UserEntity[model.userId]
        entity.totalQuantity = model.totalQuantity
        entity.totalPrice = model.totalPrice
        entity.discount = modelDiscount?.let { mod -> DiscountMapper.toEntity(mod, entityDiscount!!) }

        return entity
    }
}

object CartItemMapper : EntityMapper<CartItemEntity, CartItem, String> {
    override fun toModel(entity: CartItemEntity): CartItem {
        return CartItem(
            id = entity.id.value,
            cartId = entity.cart.id.value,
            productId = entity.product.id.value,
            quantity = entity.quantity,
            unitPrice = entity.unitPrice,
            addedAt = entity.createdAt
        )
    }

    override fun toEntity(
        model: CartItem,
        entity: CartItemEntity
    ): CartItemEntity {
        entity.product = ProductEntity[model.productId]
        entity.quantity = model.quantity
        entity.unitPrice = model.unitPrice
        entity.createdAt = model.addedAt

        return entity
    }
}