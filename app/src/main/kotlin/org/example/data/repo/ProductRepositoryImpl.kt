package org.example.data.repo

import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.ProductEntity
import org.example.data.db.tables.ProductTable
import org.example.data.mappers.ProductMapper
import org.example.domain.models.Product
import org.example.domain.repo.ProductRepository
import org.example.utils.customMatch
import org.example.utils.now
import org.example.utils.suspendTransaction

class ProductRepositoryImpl(private val productMapper: ProductMapper) : CrudRepositoryImpl<ProductEntity, Product>(
    ProductEntity
), ProductRepository {
    override fun ProductEntity.toDomain(): Product {
        return productMapper.toModel(this)
    }

    override fun Product.toEntity(entity: ProductEntity) {
        productMapper.toEntity(this, entity)
    }

    override suspend fun findBySku(sku: String): Product? = suspendTransaction {
        ProductEntity.find { ProductTable.sku.eq(sku) }.firstOrNull()?.toDomain()
    }

    override suspend fun findByCategory(
        categoryId: String,
        offset: Int,
        limit: Int
    ): List<Product> = suspendTransaction {
        ProductEntity.find { ProductTable.category.eq(categoryId) }
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toDomain() }
    }

    override suspend fun searchProducts(
        query: String,
        offset: Int,
        limit: Int
    ): List<Product> = suspendTransaction {
        ProductEntity.find { ProductTable.tsv.customMatch(query) }
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toDomain() }
            .sortedByDescending { it.createdAt.coerceAtLeast(it.updatedAt) }
    }

    override suspend fun updateProductStock(
        productId: String,
        available: Int
    ): Product? = suspendTransaction {

        ProductEntity.findByIdAndUpdate(productId) { update ->
            update.stockAvailable += available
            update.updatedAt = LocalDateTime.now()
        }?.toDomain()
    }

    override suspend fun markProductViewed(productId: String): Product? = suspendTransaction {
        ProductEntity.findByIdAndUpdate(productId) { update ->
            update.viewed = true
            update.updatedAt = LocalDateTime.now()
        }?.toDomain()
    }

    override suspend fun getProductsWithLowStock(): List<Product> = suspendTransaction {
        ProductEntity.all()
            .filter { it.stockAvailable <= it.stockLowThreshold }
            .map { it.toDomain() }
    }

}