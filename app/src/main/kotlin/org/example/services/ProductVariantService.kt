package org.example.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.example.data.cache.ProductVariantCache
import org.example.data.repo.PriceStats
import org.example.di.Component
import org.example.domain.models.catalog.ProductVariant
import org.example.exceptions.NotFoundException
import java.math.BigDecimal

@Component
class ProductVariantService(
    private val cache: ProductVariantCache
) {
    suspend fun createVariant(variant: ProductVariant): ProductVariant {
        return cache.create(variant)
    }

    suspend fun getVariant(id: String): ProductVariant? {
        return cache.read(id)
    }

    suspend fun updateVariant(id: String, variant: ProductVariant): ProductVariant? {
        return cache.update(id, variant)
    }

    suspend fun deleteVariant(id: String): Boolean {
        return cache.delete(id)
    }

    suspend fun getVariantsByProduct(productId: String): List<ProductVariant> {
        return cache.findByProductId(productId)
    }

    suspend fun getVariantBySku(sku: String): ProductVariant? {
        return cache.findBySku(sku)
    }

    suspend fun getVariantsByPriceRange(
        minPrice: BigDecimal,
        maxPrice: BigDecimal,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductVariant> {
        return cache.findByPriceRange(minPrice, maxPrice, offset, limit)
    }

    suspend fun getProductPriceStats(productId: String): PriceStats? {
        return cache.getPriceStats(productId)
    }

    suspend fun deactivateVariant(id: String): Boolean {
        return cache.deactivate(id)
    }

    suspend fun bulkUpdateProductPrices(
        productId: String,
        priceMultiplier: BigDecimal
    ): Int {
        return cache.bulkUpdatePrices(productId, priceMultiplier)
    }

    fun streamActiveVariants(productId: String): Flow<ProductVariant> = flow {
        val variants = cache.findByProductId(productId)
        variants.filter { it.isActive }.forEach { emit(it) }
    }

    suspend fun batchCreateVariants(variants: List<ProductVariant>): List<ProductVariant> {
        return variants.map { variant ->
                cache.create(variant)
            }
    }

    suspend fun getPriceComparison(variantId: String): Map<String, BigDecimal?> {
            val variant = cache.read(variantId)
                ?: throw NotFoundException("Variant $variantId not found")

            return mapOf(
                "price" to variant.price,
                "compare_at_price" to variant.compareAtPrice,
                "cost_price" to variant.costPrice,
                "margin" to variant.costPrice?.let { variant.price - it },
                "discount_percentage" to variant.compareAtPrice?.let {
                    if (it > BigDecimal.ZERO) {
                        ((it - variant.price) / it * BigDecimal(100)).setScale(2, java.math.RoundingMode.HALF_UP)
                    } else null
                }
            )
    }
}
