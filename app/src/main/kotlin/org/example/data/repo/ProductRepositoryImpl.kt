package org.example.data.repo

import io.ktor.http.content.*
import io.ktor.server.plugins.*
import org.example.data.db.entities.ProductEntity
import org.example.data.db.tables.Products
import org.example.domain.models.MediaType
import org.example.domain.models.catalog.Product
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.and
import java.time.OffsetDateTime

class ProductRepositoryImpl(private val productMapper: ProductMapper) : CrudRepository<ProductEntity, Product>(
    ProductEntity,
    Product::class
), ProductRepository {


    override fun getId(domain: Product): String = domain.id

    override suspend fun create(entity: Product): Product {
        return super.create(entity)
    }

    override fun ProductEntity.toDomain(): Product {
        return productMapper.toModel(this)
    }

    override fun Product.toEntity(entity: ProductEntity) {
        productMapper.toEntity(this, entity)
    }

    override suspend fun searchProducts(
        query: String,
        offset: Int,
        limit: Int
    ): List<Product> = suspendTransaction {
        ProductEntity.find { Products.tsv.match(query) }
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toDomain() }
            .sortedByDescending { it.createdAt.coerceAtLeast(it.updatedAt) }
    }

    private fun determineMediaType(fileItem: PartData.FileItem): MediaType {
        val contentType = fileItem.contentType?.toString() ?: ""
        return when {
            contentType.startsWith("image/") -> MediaType.IMAGE
            contentType.startsWith("video/") -> MediaType.VIDEO
            else -> MediaType.IMAGE
        }
    }

    override suspend fun findBySlug(slug: String): Product? = suspendTransaction {
        ProductEntity.find { (Products.slug eq slug) and (Products.deletedAt.isNull()) }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findByCategory(categoryId: String, offset: Int, limit: Int): List<Product> = suspendTransaction {
        ProductEntity.find {
            (Products.categoryId eq categoryId) and (Products.deletedAt.isNull())
        }
            .offset(offset.toLong())
            .limit(limit)
            .map { it.toDomain() }
    }

    override suspend fun findFeatured(): List<Product> = suspendTransaction {
        ProductEntity.find {
            (Products.isFeatured eq true) and (Products.deletedAt.isNull())
        }.map { it.toDomain() }
    }

    override suspend fun updateStatus(productId: String, status: String): Boolean = suspendTransaction {
        val product = ProductEntity.findById(productId) ?: throw NotFoundException("Product not found")
        product.status = status
        true
    }

    override suspend fun softDelete(productId: String): Boolean = suspendTransaction {
        val product = ProductEntity.findById(productId) ?: throw NotFoundException("Product not found")
        product.deletedAt = OffsetDateTime.now()
        true
    }
}