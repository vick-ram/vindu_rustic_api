package org.example.data.mappers

import org.example.data.db.entities.ProductMediaEntity
import org.example.data.db.tables.ProductVariants
import org.example.data.db.tables.Products
import org.example.domain.models.catalog.ProductMedia
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ProductMediaMapper : EntityMapper<ProductMediaEntity, ProductMedia, String> {
    override fun toModel(entity: ProductMediaEntity): ProductMedia {
        return ProductMedia(
            id = entity.id.value,
            productId = entity.productId.value,
            variantId = entity.variantId?.value,
            mediaType = entity.mediaType,
            mediaUrl = entity.mediaUrl,
            altText = entity.altText,
            sortOrder = entity.sortOrder,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: ProductMedia, entity: ProductMediaEntity): ProductMediaEntity {
        entity.productId = EntityID(model.productId, Products)
        entity.variantId = model.variantId?.let { EntityID(it, ProductVariants) }
        entity.mediaType = model.mediaType
        entity.mediaUrl = model.mediaUrl
        entity.altText = model.altText
        entity.sortOrder = model.sortOrder
        return entity
    }
}