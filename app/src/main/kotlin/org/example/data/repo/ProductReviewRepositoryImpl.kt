package org.example.data.repo

import org.example.data.db.entities.ProductReviewEntity
import org.example.data.db.tables.ProductReviewTable
import org.example.data.mappers.ProductReviewMapper
import org.example.domain.models.ProductReview
import org.example.domain.repo.ProductReviewRepository
import org.example.utils.suspendTransaction

class ProductReviewRepositoryImpl(private val productReviewMapper: ProductReviewMapper) :
    CrudRepositoryImpl<ProductReviewEntity, ProductReview>(ProductReviewEntity, ProductReview::class),
    ProductReviewRepository {
    override fun ProductReviewEntity.toDomain(): ProductReview {
        return productReviewMapper.toModel(this)
    }

    override fun getId(domain: ProductReview): String = domain.id

    override fun ProductReview.toEntity(entity: ProductReviewEntity) {
        productReviewMapper.toEntity(this, entity)
    }

    override suspend fun fetchProductReviews(
        productId: String,
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<ProductReview> = suspendTransaction {
        ProductReviewEntity.find { ProductReviewTable.product.eq(productId) }
            .offset(offset.toLong())
            .limit(limit)
            .filter { entity ->
                queryParams?.all { (key, value) ->
                    val property = entity::class.members.find { it.name == key }
                    property?.call(entity).toString().contains(value, ignoreCase = true)
                } == true
            }
            .map { it.toDomain() }
    }

    override suspend fun fetchUsersProductReviews(
        userId: String,
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<ProductReview> = suspendTransaction {
        ProductReviewEntity.find { ProductReviewTable.user.eq(userId) }
            .offset(offset.toLong())
            .limit(limit)
            .filter { entity ->
                queryParams?.all { (key, value) ->
                    val property = entity::class.members.find { it.name == key }
                    property?.call(entity).toString().contains(value, ignoreCase = true)
                } == true
            }
            .map { it.toDomain() }
    }

    override suspend fun fetchApprovedReviews(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<ProductReview> = suspendTransaction {
        ProductReviewEntity.find { ProductReviewTable.isApproved.eq(true) }
            .offset(offset.toLong())
            .limit(limit)
            .filter { entity ->
                queryParams?.all { (key, value) ->
                    val property = entity::class.members.find { it.name == key }
                    property?.call(entity).toString().contains(value, ignoreCase = true)
                } == true
            }
            .map { it.toDomain() }
    }
}