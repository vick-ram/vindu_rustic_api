package org.example.services

import org.example.domain.models.catalog.Discount
import org.example.domain.repo.DiscountRepository

class DiscountService(private val discountRepository: DiscountRepository) {
    suspend fun createDiscount(discount: Discount): Discount {
        return discountRepository.create(discount)
    }

    suspend fun getDiscounts(offset: Int, limit: Int, queryParams: Map<String, String>?): List<Discount> {
        return discountRepository.readAll(offset, limit, queryParams)
    }

    suspend fun getDiscount(id: String): Discount? {
        return discountRepository.read(id)
    }

    suspend fun updateDiscount(discountId: String, discount: Discount): Discount? {
        return discountRepository.update(discountId, discount)
    }

    suspend fun deleteDiscount(discountId: String): Boolean {
        return discountRepository.delete(discountId)
    }

    suspend fun getByCode(code: String): Discount? {
        return discountRepository.findByCode(code)
    }

    suspend fun getActiveDiscounts(): List<Discount> {
        return discountRepository.findActiveDiscounts()
    }

    suspend fun incrementUsage(id: String): Boolean {
        return discountRepository.incrementUses(id)
    }
}