package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.ProductMedia
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ProductMediaEntity(id: EntityID<String>) : CustomEntity(id, ProductMedia) {
    companion object : CustomEntityClass<ProductMediaEntity>(ProductMedia)

    var productId by ProductMedia.productId
    var variantId by ProductMedia.variantId
    var mediaType by ProductMedia.mediaType
    var mediaUrl by ProductMedia.mediaUrl
    var altText by ProductMedia.altText
    var sortOrder by ProductMedia.sortOrder
}