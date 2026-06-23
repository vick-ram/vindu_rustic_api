package org.example.data.mappers

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.example.data.db.entities.CartItemEntity
import org.example.data.db.tables.ProductVariants
import org.example.data.db.tables.ShoppingCarts
import org.example.domain.models.sales.CartItem
import org.example.domain.repo.EntityMapper
import org.example.domain.repo.RowMapper
import org.example.domain.repo.toModel
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object CartItemMapper : EntityMapper<CartItemEntity, CartItem, String> {
    override fun toModel(entity: CartItemEntity): CartItem {
        return CartItem(
            id = entity.id.value,
            cartId = entity.cartId.value,
            variantId = entity.variantId.value,
            quantity = entity.quantity,
            customizationDetails = entity.customizationDetails,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(model: CartItem, entity: CartItemEntity): CartItemEntity {
        entity.cartId = EntityID(model.cartId, ShoppingCarts)
        entity.variantId = EntityID(model.variantId, ProductVariants)
        entity.quantity = model.quantity
        entity.customizationDetails = model.customizationDetails
        return entity
    }
}

object CartItemMapper2 : RowMapper<Row, CartItem> {
    override fun toModel(
        row: Row,
        metadata: RowMetadata
    ): CartItem {
        return row.toModel(metadata)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toRow(model: CartItem): Map<String, Any?> {
        return Properties.encodeToMap(model)
    }

    override fun getId(model: CartItem): Any = model.id
}