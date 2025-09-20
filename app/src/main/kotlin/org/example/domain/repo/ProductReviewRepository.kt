package org.example.domain.repo

import org.example.domain.models.ProductReview

interface ProductReviewRepository: CrudRepository<ProductReview, String> {
    suspend fun fetchProductReviews(productId: String, offset: Int, limit: Int, queryParams: Map<String, String>?): List<ProductReview>
    suspend fun fetchUsersProductReviews(userId: String, offset: Int, limit: Int, queryParams: Map<String, String>?): List<ProductReview>
    suspend fun fetchApprovedReviews(offset: Int, limit: Int, queryParams: Map<String, String>?): List<ProductReview>
}

class CachedProductReview(
    private val delegate: ProductReviewRepository,
    private val cache: CrudRepository<ProductReview, String>
): ProductReviewRepository {
    override suspend fun fetchProductReviews(
        productId: String,
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<ProductReview> {
        return delegate.fetchProductReviews(productId, offset, limit, queryParams)
    }

    override suspend fun fetchUsersProductReviews(
        userId: String,
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<ProductReview> {
        return delegate.fetchUsersProductReviews(userId, offset, limit, queryParams)
    }

    override suspend fun fetchApprovedReviews(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<ProductReview> {
        return delegate.fetchApprovedReviews(offset, limit, queryParams)
    }

    override suspend fun create(entity: ProductReview): ProductReview {
        return cache.create(entity)
    }

    override suspend fun read(id: String): ProductReview? {
        return cache.read(id)
    }

    override suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<ProductReview> {
        return cache.readAll(offset, limit, queryParams)
    }

    override suspend fun update(
        id: String,
        entity: ProductReview
    ): ProductReview? {
        return cache.update(id, entity)
    }

    override suspend fun delete(id: String): Boolean {
        return cache.delete(id)
    }
}