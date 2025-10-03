package org.example.services

import org.example.domain.models.SpecialOffer
import org.example.domain.repo.SpecialOfferRepository

class SpecialOfferService(private val specialOfferRepository: SpecialOfferRepository) {
    suspend fun createSpecialOffer(specialOffer: SpecialOffer): SpecialOffer {
        return specialOfferRepository.create(specialOffer)
    }

    suspend fun getSpecialOffer(id: String): SpecialOffer? {
        return specialOfferRepository.read(id)
    }

    suspend fun getSpecialOffers(offset: Int, limit: Int): List<SpecialOffer> {
        return specialOfferRepository.readAll(offset, limit, emptyMap())
    }

    suspend fun getActiveOffers(): List<SpecialOffer> {
        return specialOfferRepository.findActiveOffers()
    }

    suspend fun getProductOffers(productId: String): List<SpecialOffer> {
        return specialOfferRepository.findOffersByProduct(productId)
    }

    suspend fun updateSpecialOffer(id: String, specialOffer: SpecialOffer): SpecialOffer? {
        return specialOfferRepository.update(id, specialOffer)
    }

    suspend fun deleteSpecialOffer(id: String): Boolean {
        return specialOfferRepository.delete(id)
    }
}