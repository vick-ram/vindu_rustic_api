package org.example.data.repo

import org.example.data.db.entities.ProductEntity
import org.example.data.db.tables.ProductTable
import org.example.data.mappers.ProductMapper
import org.example.domain.models.Product
import org.example.domain.repo.ProductRepository
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.or

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
    ): List<Product> {
        ProductEntity.find { ProductTable.category.eq(categoryId) }
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toDomain() }
    }

    override suspend fun searchProducts(
        query: String,
        offset: Int,
        limit: Int
    ): List<Product> {
        ProductEntity.find { (ProductTable.name.like("%$query%") or (ProductTable.description.like("%$query%"))) }
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toDomain() }
    }

}