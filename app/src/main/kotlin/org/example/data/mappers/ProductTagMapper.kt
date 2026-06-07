package org.example.data.mappers

import org.example.data.db.entities.ProductTagEntity
import org.example.data.db.tables.Products
import org.example.data.db.tables.Tags
import org.example.domain.models.catalog.ProductTag
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ProductTagMapper : EntityMapper<ProductTagEntity, ProductTag, String> {
    override fun toModel(entity: ProductTagEntity): ProductTag {
        return ProductTag(
            productId = entity.productId.value,
            tagId = entity.tagId.value,
        )
    }

    override fun toEntity(model: ProductTag, entity: ProductTagEntity): ProductTagEntity {
        entity.productId = EntityID(model.productId, Products)
        entity.tagId = EntityID(model.tagId, Tags)
        return entity
    }
}