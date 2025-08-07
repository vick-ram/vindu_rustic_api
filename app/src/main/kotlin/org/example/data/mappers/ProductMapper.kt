package org.example.data.mappers

import org.example.data.db.entities.CategoryEntity
import org.example.data.db.entities.ProductEntity
import org.example.domain.models.Category
import org.example.domain.models.Media
import org.example.domain.models.MediaType
import org.example.domain.models.Product
import org.example.domain.models.SeoData
import org.example.domain.models.StockInfo
import org.example.domain.repo.EntityMapper

object CategoryMapper: EntityMapper<CategoryEntity, Category, String> {
    override fun toModel(entity: CategoryEntity): Category {
        return Category(
            id = entity.id.value,
            name = entity.name,
            slug = entity.slug,
            description = entity.description,
            imageUrl = entity.imageUrl,
            isActive = entity.isActive,
            displayOrder = entity.displayOrder
        )
    }

    override fun toEntity(
        model: Category,
        entity: CategoryEntity
    ): CategoryEntity {
        entity.name = model.name
        entity.slug = model.slug
        entity.description = model.description
        entity.imageUrl = model.imageUrl
        entity.isActive = model.isActive
        entity.displayOrder = model.displayOrder
        return entity
    }
}

object ProductMapper: EntityMapper<ProductEntity, Product, String> {
    override fun toModel(entity: ProductEntity): Product {
        return Product(
            id = entity.id.value,
            sku = entity.sku,
            name = entity.name,
            description = entity.description,
            shortDescription = entity.shortDescription,
            basePrice = entity.basePrice,
            viewed = entity.viewed,
            category = CategoryMapper.toModel(entity.category),
            stock = StockInfo(
                available = entity.stockAvailable,
                lowStockThreshold = entity.stockLowThreshold
            ),
            media = entity.media.map {
                Media(
                    id = it.id.value,
                    url = it.url,
                    type = MediaType.valueOf(it.type),
                    altText = it.altText,
                    isPrimary = it.isPrimary,
                    displayOrder = it.displayOrder
                )
            },
            seoData = SeoData(
                metaTitle = entity.metaTitle,
                metaDescription = entity.metaDescription,
                slug = entity.seoSlug,
                canonicalUrl = entity.canonicalUrl,
                keywords = entity.keywords
            ),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(
        model: Product,
        entity: ProductEntity
    ): ProductEntity {
        TODO("Not yet implemented")
    }
}