package org.example.data.repo

import org.example.data.db.entities.WishlistEntity
import org.example.data.db.entities.WishlistItemEntity
import org.example.data.db.tables.ProductVariants
import org.example.data.db.tables.Products
import org.example.data.db.tables.WishlistItems
import org.example.data.db.tables.Wishlists
import org.example.data.mappers.WishlistMapper
import org.example.domain.models.sales.Wishlist
import org.example.domain.models.sales.WishlistItem
import org.example.domain.repo.WishlistRepository
import org.example.plugins.AlreadyExistsException
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class WishlistRepositoryImpl(private val wishlistMapper: WishlistMapper) :
    CrudRepositoryImpl<WishlistEntity, Wishlist>(WishlistEntity, Wishlist::class),
    WishlistRepository {

    override suspend fun findByUserId(userId: String): List<Wishlist> = suspendTransaction {
        WishlistEntity.find { Wishlists.userId eq userId }
            .map { it.toDomain() }
    }

    override suspend fun addItem(wishlistId: String, productId: String, variantId: String?): WishlistItem = suspendTransaction {
        val exists = WishlistItems.selectAll().where {
            (WishlistItems.wishlistId eq wishlistId) and (WishlistItems.productId eq productId)
        }.count() > 0

        if (exists) throw AlreadyExistsException("Product already in wishlist")

        WishlistItemEntity.new {
            this.wishlistId = EntityID(wishlistId, Wishlists)
            this.productId = EntityID(productId, Products)
            this.variantId = variantId?.let { EntityID(it, ProductVariants) }
        }.let {
            WishlistItem(
                id = it.id.value,
                wishlistId = it.wishlistId.value,
                productId = it.productId.value,
                variantId = it.variantId?.value,
                notes = it.notes,
                createdAt = it.createdAt
            )
        }
    }

    override suspend fun removeItem(wishlistId: String, productId: String): Boolean = suspendTransaction {
        WishlistItems.deleteWhere {
            (WishlistItems.wishlistId eq wishlistId) and (WishlistItems.productId eq productId)
        } > 0
    }

    override fun WishlistEntity.toDomain(): Wishlist = wishlistMapper.toModel(this)
    override fun Wishlist.toEntity(entity: WishlistEntity) {
        wishlistMapper.toEntity(this, entity)
    }
    override fun getId(domain: Wishlist): String = domain.id
}