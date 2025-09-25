package org.example.domain.repo

import org.example.domain.models.SpecialOffer

interface SpecialOfferRepository : CrudRepository<SpecialOffer, String> {
    suspend fun findActiveOffers(): List<SpecialOffer>
    suspend fun findOffersByProduct(productId: String): List<SpecialOffer>
}