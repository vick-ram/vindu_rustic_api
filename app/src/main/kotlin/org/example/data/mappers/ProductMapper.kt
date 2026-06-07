package org.example.data.mappers

import org.example.data.db.entities.*
import org.example.domain.models.*
import org.example.domain.models.catalog.Product
import org.example.domain.repo.EntityMapper

object ProductMapper : EntityMapper<ProductEntity, Product, String> {
    override fun toModel(entity: ProductEntity): Product {
        return Product(
            id = entity.id.value,
            categoryId = entity.categoryId?.value,
            title = entity.title,
            slug = entity.slug,
            shortDescription = entity.shortDescription,
            description = entity.description,
            status = entity.status,
            productType = entity.productType,
            brand = entity.brand,
            isCustomizable = entity.isCustomizable,
            isFeatured = entity.isFeatured,
            seoTitle = entity.seoTitle,
            seoDescription = entity.seoDescription,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt
        )
    }

    override fun toEntity(model: Product, entity: ProductEntity): ProductEntity {
        entity.categoryId = model.categoryId?.let { EntityID(it, Categories) }
        entity.title = model.title
        entity.slug = model.slug
        entity.shortDescription = model.shortDescription
        entity.description = model.description
        entity.status = model.status
        entity.productType = model.productType
        entity.brand = model.brand
        entity.isCustomizable = model.isCustomizable
        entity.isFeatured = model.isFeatured
        entity.seoTitle = model.seoTitle
        entity.seoDescription = model.seoDescription
        entity.deletedAt = model.deletedAt
        return entity
    }
}

