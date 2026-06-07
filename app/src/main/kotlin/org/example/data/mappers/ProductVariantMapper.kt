package org.example.data.mappers

import org.example.data.db.entities.ProductVariantEntity
import org.example.data.db.tables.Products
import org.example.domain.models.catalog.ProductVariant
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ProductVariantMapper : EntityMapper<ProductVariantEntity, ProductVariant, String> {
    override fun toModel(entity: ProductVariantEntity): ProductVariant {
        return ProductVariant(
            id = entity.id.value,
            productId = entity.productId.value,
            sku = entity.sku,
            title = entity.title,
            price = entity.price,
            compareAtPrice = entity.compareAtPrice,
            costPrice = entity.costPrice,
            currency = entity.currency,
            quantityInStock = entity.quantityInStock,
            reservedQuantity = entity.reservedQuantity,
            weightGrams = entity.weightGrams,
            dimensions = entity.dimensions,
            attributes = entity.attributes,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(model: ProductVariant, entity: ProductVariantEntity): ProductVariantEntity {
        entity.productId = EntityID(model.productId, Products)
        entity.sku = model.sku
        entity.title = model.title
        entity.price = model.price
        entity.compareAtPrice = model.compareAtPrice
        entity.costPrice = model.costPrice
        entity.currency = model.currency
        entity.quantityInStock = model.quantityInStock
        entity.reservedQuantity = model.reservedQuantity
        entity.weightGrams = model.weightGrams
        entity.dimensions = model.dimensions
        entity.attributes = model.attributes
        entity.isActive = model.isActive
        return entity
    }
}