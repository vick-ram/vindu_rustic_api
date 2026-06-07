package org.example.data.mappers

import org.example.data.db.entities.ProductReviewEntity
import org.example.data.db.tables.Products
import org.example.data.db.tables.Users
import org.example.domain.models.catalog.ProductReview
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ProductReviewMapper: EntityMapper<ProductReviewEntity, ProductReview, String> {
    override fun toModel(entity: ProductReviewEntity): ProductReview {
        return ProductReview(
            id = entity.id.value,
            productId = entity.productId.value,
            userId = entity.userId.value,
            orderItemId = entity.orderItemId.value,
            rating = entity.rating,
            title = entity.title,
            review = entity.review,
            isVerifiedPurchase = entity.isVerifiedPurchase,
            createdAt = entity.createdAt
        )
    }

    override fun toEntity(
        model: ProductReview,
        entity: ProductReviewEntity
    ): ProductReviewEntity {
        return entity.apply {
            productId = EntityID(model.productId, Products)
            userId = EntityID(model.userId, Users)
            orderItemId = EntityID(model.orderItemId, Products)
            rating = model.rating
            title = model.title
            review = model.review
            isVerifiedPurchase = model.isVerifiedPurchase
        }
    }
}