package org.example.domain.repo

import org.example.domain.models.Discount

interface DiscountRepository : CrudRepository<Discount, String> {
    suspend fun findByCode(code: String): Discount?
    suspend fun findActiveDiscounts(): List<Discount>
    suspend fun findDiscountsByProduct(productId: String): List<Discount>
    suspend fun incrementUses(discountId: String): Boolean
}

class CachedDiscountRepository(
    private val delegate: DiscountRepository,
    private val cache: CrudRepository<Discount, String>
): DiscountRepository {
    override suspend fun findByCode(code: String): Discount? {
        return delegate.findByCode(code)
    }

    override suspend fun findActiveDiscounts(): List<Discount> {
        return delegate.findActiveDiscounts()
    }

    override suspend fun findDiscountsByProduct(productId: String): List<Discount> {
        return delegate.findDiscountsByProduct(productId)
    }

    override suspend fun incrementUses(discountId: String): Boolean {
        return delegate.incrementUses(discountId)
    }

    override suspend fun create(entity: Discount): Discount {
        return cache.create(entity)
    }

    override suspend fun read(id: String): Discount? {
        return cache.read(id)
    }

    override suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<Discount> {
        return cache.readAll(offset, limit, queryParams)
    }

    override suspend fun update(
        id: String,
        entity: Discount
    ): Discount? {
        return cache.update(id, entity)
    }

    override suspend fun delete(id: String): Boolean {
        return cache.delete(id)
    }
}