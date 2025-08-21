package org.example.data.mappers

import org.example.data.db.entities.CategoryEntity
import org.example.data.db.entities.DiscountEntity
import org.example.data.db.entities.MediaEntity
import org.example.data.db.entities.ProductEntity
import org.example.data.db.entities.ProductReviewEntity
import org.example.data.db.entities.SpecialOfferEntity
import org.example.domain.models.Category
import org.example.domain.models.Discount
import org.example.domain.models.Media
import org.example.domain.models.MediaType
import org.example.domain.models.Product
import org.example.domain.models.ProductReview
import org.example.domain.models.SeoData
import org.example.domain.models.SpecialOffer
import org.example.domain.models.StockInfo
import org.example.domain.repo.EntityMapper

object CategoryMapper : EntityMapper<CategoryEntity, Category, String> {
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
            category = CategoryMapper.toModel(entity.category),
            stock = StockInfo(
                available = entity.stockAvailable,
                lowStockThreshold = entity.stockLowThreshold
            ),
            media = entity.media.map {
                Media(
                    id = it.id.value,
                    url = it.url,
                    type = it.type,
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
        val categoryEntity = entity.category
        val categoryModel = model.category

        val categoryMapper = CategoryMapper.toEntity(categoryModel, categoryEntity)

        entity.sku = model.sku
        entity.name = model.name
        entity.description = model.description
        entity.shortDescription = model.shortDescription
        entity.basePrice = model.basePrice
        entity.viewed = model.viewed
        entity.category = categoryMapper
        entity.stockAvailable = model.stock.available
        entity.stockLowThreshold = model.stock.lowStockThreshold
        entity.metaTitle = model.seoData.metaTitle
        entity.metaDescription = model.seoData.metaDescription
        entity.seoSlug = model.seoData.slug
        entity.canonicalUrl = model.seoData.canonicalUrl
        entity.keywords = model.seoData.keywords

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

//object DiscountMapper : EntityMapper<DiscountEntity, Discount, String> {
//    override fun toModel(entity: DiscountEntity): Discount {
//        return Discount(
//            id = entity.id.value,
//            name = entity.name,
//            description = entity.description,
//            type = entity.type,
//            value = entity.value,
//            code = entity.code,
//            appliedTo = entity.appliedTo,
//            minimumOrderAmount = entity.minimumOrderAmount,
//            startDate = entity.startDate,
//            endDate = entity.endDate,
//            maxUses = entity.maxUses,
//            currentUses = entity.currentUses,
//            isActive = entity.isActive
//        )
//    }
//
//    override fun toEntity(
//        model: Discount,
//        entity: DiscountEntity
//    ): DiscountEntity {
//        entity.name = model.name
//        entity.description = model.description
//        entity.type = model.type
//        entity.value = model.value
//        entity.code = model.code
//        entity.appliedTo = model.appliedTo
//        entity.minimumOrderAmount = model.minimumOrderAmount
//        entity.startDate = model.startDate
//        entity.endDate = model.endDate
//        entity.maxUses = model.maxUses
//        entity.currentUses = model.currentUses
//        entity.isActive = model.isActive
//
//        return entity
//    }
//}

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

        return entity
    }
}
