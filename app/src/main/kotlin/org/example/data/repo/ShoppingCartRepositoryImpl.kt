package org.example.data.repo

import org.example.data.db.entities.ShoppingCartEntity
import org.example.data.db.tables.CartItems
import org.example.data.db.tables.ShoppingCarts
import org.example.data.db.tables.Users
import org.example.data.mappers.ShoppingCartMapper
import org.example.domain.models.sales.ShoppingCart
import org.example.domain.repo.ShoppingCartRepository
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import java.util.UUID

class ShoppingCartRepositoryImpl(private val cartMapper: ShoppingCartMapper) :
    CrudRepositoryImpl<ShoppingCartEntity, ShoppingCart>(ShoppingCartEntity, ShoppingCart::class),
    ShoppingCartRepository {

    override suspend fun findByUserId(userId: String): ShoppingCart? = suspendTransaction {
        ShoppingCartEntity.find { ShoppingCarts.userId eq userId }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findByGuestToken(token: UUID): ShoppingCart? = suspendTransaction {
        ShoppingCartEntity.find { ShoppingCarts.guestToken eq token }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun getOrCreateCart(userId: String?, guestToken: UUID?): ShoppingCart = suspendTransaction {
        val existingCart = when {
            userId != null -> findByUserId(userId)
            guestToken != null -> findByGuestToken(guestToken)
            else -> null
        }

        existingCart ?: ShoppingCartEntity.new {
            this.userId = userId?.let { EntityID(it, Users) }
            this.guestToken = guestToken
        }.toDomain()
    }

    override suspend fun clearCart(cartId: String): Boolean = suspendTransaction {
        CartItems.deleteWhere { CartItems.cartId eq cartId }
        true
    }

    override fun ShoppingCartEntity.toDomain(): ShoppingCart = cartMapper.toModel(this)
    override fun ShoppingCart.toEntity(entity: ShoppingCartEntity) {
        cartMapper.toEntity(this, entity)
    }
    override fun getId(domain: ShoppingCart): String = domain.id
}