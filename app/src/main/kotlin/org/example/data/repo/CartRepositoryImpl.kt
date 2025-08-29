package org.example.data.repo

import org.example.data.db.entities.CartEntity
import org.example.data.db.entities.CartItemEntity
import org.example.data.db.entities.ProductEntity
import org.example.data.db.entities.UserEntity
import org.example.data.db.tables.CartItemTable
import org.example.data.db.tables.CartTable
import org.example.data.mappers.CartMapper
import org.example.domain.models.Cart
import org.example.domain.repo.CartRepository
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.and

class CartRepositoryImpl(private val cartMapper: CartMapper) : CrudRepositoryImpl<CartEntity, Cart>(CartEntity, Cart::class), CartRepository {
    override fun CartEntity.toDomain(): Cart = cartMapper.toModel(this)

    override fun Cart.toEntity(entity: CartEntity) {
        cartMapper.toEntity(this, entity)
    }

    override suspend fun getCartByUser(userId: String): Cart? = suspendTransaction {
        CartEntity.find { CartTable.user.eq(userId) }.firstOrNull()?.toDomain()
    }

    override suspend fun addItem(
        userId: String,
        productId: String,
        quantity: Int
    ): Cart = suspendTransaction {
        val cart = CartEntity.find { CartTable.user.eq(userId) }
            .firstOrNull() ?: CartEntity.new {
                this.user = UserEntity[userId]
        }
        val product = ProductEntity[productId]
        CartItemEntity.new {
            this.cart = cart
            this.product = product
            this.quantity = quantity
        }
        cart.toDomain()
    }

    override suspend fun removeItem(userId: String, productId: String): Cart = suspendTransaction {
        val cart = CartEntity.find { CartTable.user.eq(userId) }.first()
        CartItemEntity.find {
            (CartItemTable.cart.eq(cart.id) and (CartItemTable.product.eq(productId)))
        }.firstOrNull()?.delete()
        cart.toDomain()
    }

    override suspend fun updateItem(
        userId: String,
        productId: String,
        quantity: Int
    ): Cart = suspendTransaction {
        val cart = CartEntity.find { CartTable.user.eq(userId) }.first()
        CartItemEntity.find {
            (CartItemTable.cart.eq(cart.id) and (CartItemTable.product.eq(productId)))
        }.firstOrNull()?.apply {
            this.quantity = quantity
        }
        cart.toDomain()
    }

    override suspend fun clearCart(userId: String) = suspendTransaction {
        val cart = CartEntity.find { CartTable.user.eq(userId) }.first()
        cart.items.forEach { it.delete() }
    }
}