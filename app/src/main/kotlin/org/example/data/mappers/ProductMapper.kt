package org.example.data.mappers

import org.example.data.db.entities.CategoryEntity
import org.example.data.db.entities.DimensionEntity
import org.example.data.db.entities.MediaEntity
import org.example.data.db.entities.ProductEntity
import org.example.data.db.entities.ProductReviewEntity
import org.example.data.db.entities.SpecialOfferEntity
import org.example.domain.models.Category
import org.example.domain.models.Dimension
import org.example.domain.models.DimensionUnit
import org.example.domain.models.Media
import org.example.domain.models.OfferType
import org.example.domain.models.Product
import org.example.domain.models.ProductReview
import org.example.domain.models.SpecialOffer
import org.example.domain.models.StockInfo
import org.example.domain.repo.EntityMapper
import org.example.utils.generateProductSku

object CategoryMapper : EntityMapper<CategoryEntity, Category, String> {
    override fun toModel(entity: CategoryEntity): Category {
        return Category(
            id = entity.id.value,
            name = entity.name,
            slug = entity.slug,
            description = entity.description,
            imageUrl = entity.imageUrl,
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
        entity.displayOrder = model.displayOrder
        entity.tsv = "to_tsvector('english', '${entity.name} ${entity.slug} ${entity.description}')"
        return entity
    }
}

object ProductMapper : EntityMapper<ProductEntity, Product, String> {
    override fun toModel(entity: ProductEntity): Product {
        return Product(
            id = entity.id.value,
            sku = entity.sku,
            name = entity.name,
            description = entity.description,
            shortDescription = entity.shortDescription,
            basePrice = entity.basePrice,
            viewed = entity.viewed,
            categoryId = entity.category.id.value,
            stock = StockInfo(
                available = entity.stockAvailable,
                lowStockThreshold = entity.stockLowThreshold
            ),
            media = entity.media.map { MediaMapper.toModel(it) },
            dimension = entity.dimensions.firstOrNull()?.let { DimensionMapper.toModel(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(
        model: Product,
        entity: ProductEntity
    ): ProductEntity {
        entity.sku = generateProductSku(entity.name)
        entity.name = model.name
        entity.description = model.description
        entity.shortDescription = model.shortDescription
        entity.basePrice = model.basePrice
        entity.viewed = model.viewed
        entity.stockAvailable = model.stock.available
        entity.stockLowThreshold = model.stock.lowStockThreshold
        entity.tsv = "to_tsvector('english', '${entity.name} ${entity.description} ${entity.shortDescription}')"

        return entity
    }
}

object MediaMapper : EntityMapper<MediaEntity, Media, String> {
    override fun toModel(entity: MediaEntity): Media {
        return Media(
            id = entity.id.value,
            url = entity.url,
            type = entity.type,
            altText = entity.altText,
            isPrimary = entity.isPrimary,
            displayOrder = entity.displayOrder
        )
    }

    override fun toEntity(
        model: Media,
        entity: MediaEntity
    ): MediaEntity {
        entity.url = model.url
        entity.type = model.type
        entity.altText = model.altText
        entity.isPrimary = model.isPrimary
        entity.displayOrder = model.displayOrder

        return entity
    }
}

object DimensionMapper : EntityMapper<DimensionEntity, Dimension, String> {
    override fun toModel(entity: DimensionEntity): Dimension {
        return Dimension(
            id = entity.id.value,
            width = entity.width.toInt(),
            height = entity.height.toInt(),
            depth = entity.depth.toInt(),
            unit = entity.unit
        )
    }

    override fun toEntity(
        model: Dimension,
        entity: DimensionEntity
    ): DimensionEntity {
        return entity.apply {
            this.width = model.width.toBigDecimal()
            this.height = model.height.toBigDecimal()
            this.depth = model.depth.toBigDecimal()
            this.unit = model.unit
        }
    }
}

object SpecialOfferMapper : EntityMapper<SpecialOfferEntity, SpecialOffer, String> {
    override fun toModel(entity: SpecialOfferEntity): SpecialOffer {
        val products = entity.products
            .map { productEntity ->
                ProductMapper.toModel(productEntity)
            }

        return SpecialOffer(
            id = entity.id.value,
            name = entity.name,
            description = entity.description,
            type = entity.type,
            products = products,
            startDate = entity.startDate,
            endDate = entity.endDate,
            isActive = entity.isActive
        )
    }

    override fun toEntity(
        model: SpecialOffer,
        entity: SpecialOfferEntity
    ): SpecialOfferEntity {
        entity.name = model.name
        entity.description = model.description
        entity.type = model.type
        entity.startDate = model.startDate
        entity.endDate = model.endDate
        entity.isActive = model.isActive
        entity.tsv = "to_tsvector('english', '${entity.name} ${entity.description}')"

        return entity
    }
}

object ProductReviewMapper : EntityMapper<ProductReviewEntity, ProductReview, String> {
    override fun toModel(entity: ProductReviewEntity): ProductReview {
        return ProductReview(
            id = entity.id.value,
            productId = entity.product.id.value,
            userId = entity.user.id.value,
            rating = entity.rating,
            title = entity.title,
            content = entity.title,
            isApproved = entity.isApproved
        )
    }

    override fun toEntity(
        model: ProductReview,
        entity: ProductReviewEntity
    ): ProductReviewEntity {
        entity.rating = model.rating
        entity.title = model.title
        entity.content = model.content
        entity.isApproved = model.isApproved
        entity.tsv = "to_tsvector('english', '${entity.title} ${entity.content}')"

        return entity
    }
}

