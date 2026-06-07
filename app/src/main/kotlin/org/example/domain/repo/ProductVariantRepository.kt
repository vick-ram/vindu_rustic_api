package org.example.domain.repo

import org.example.domain.models.catalog.ProductVariant

interface ProductVariantRepository : CrudRepository<ProductVariant, String> {
    suspend fun findByProductId(productId: String): List<ProductVariant>
    suspend fun findBySku(sku: String): ProductVariant?
    suspend fun findActiveByProductId(productId: String): List<ProductVariant>
    suspend fun updateStock(variantId: String, quantity: Int): Boolean
}