package org.example.domain.repo

import org.example.domain.models.SpecialOffer

interface SpecialOfferRepository : CrudRepository<SpecialOffer, String> {
    suspend fun findActiveOffers(): List<SpecialOffer>
    suspend fun findOffersByProduct(productId: String): List<SpecialOffer>
}

class CachedSpecialOfferRepository(
    private val delegate: SpecialOfferRepository,
    private val cache: CrudRepository<SpecialOffer, String>
): SpecialOfferRepository {
    override suspend fun findActiveOffers(): List<SpecialOffer> {
        return delegate.findActiveOffers()
    }

    override suspend fun findOffersByProduct(productId: String): List<SpecialOffer> {
        return delegate.findOffersByProduct(productId)
    }

    override suspend fun create(entity: SpecialOffer): SpecialOffer {
        return cache.create(entity)
    }

    override suspend fun read(id: String): SpecialOffer? {
        return cache.read(id)
    }

    override suspend fun readAll(
        offset: Int,
        limit: Int,
        queryParams: Map<String, String>?
    ): List<SpecialOffer> {
        return cache.readAll(offset, limit, queryParams)
    }

    override suspend fun update(
        id: String,
        entity: SpecialOffer
    ): SpecialOffer? {
        return cache.update(id, entity)
    }

    override suspend fun delete(id: String): Boolean {
        return cache.delete(id)
    }
}