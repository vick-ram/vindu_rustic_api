package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.Serializable
import org.example.data.mappers.ProductMapper
import org.example.data.repo.AuditLogsRepository
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.ProductMediaRepository
import org.example.data.repo.ProductRepository
import org.example.data.repo.ProductReviewRepository
import org.example.data.repo.ProductStats
import org.example.data.repo.ProductVariantRepository
import org.example.domain.models.catalog.Product
import org.example.domain.models.catalog.ProductMedia
import org.example.domain.models.catalog.ProductReview
import org.example.domain.models.catalog.ProductVariant
import org.koin.core.annotation.Single

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Single
class ProductCache(
    redis: RedisCoroutinesCommands<String, String>,
    private val productRepository: ProductRepository,
    private val auditLogRepository: AuditLogsRepository,
    private val productMediaRepository: ProductMediaRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val productReviewRepository: ProductReviewRepository,
    productMapper: ProductMapper
): CrudCache<Product, String>(
    redis = redis,
    delegate = productRepository,
    getId = {product -> productMapper.getId(product) as String},
    serializer = Product.serializer(),
    config = object : CacheConfig {
        override val cacheName: String = "productCache"
        override val ttl: Long = 1800L
    }
) {
    suspend fun createProduct(
        title: String,
        slug: String,
        categoryId: String? = null,
        description: String? = null,
        shortDescription: String? = null,
        productType: String = "standard",
        brand: String? = null,
        isCustomizable: Boolean = false,
        seoTitle: String? = null,
        seoDescription: String? = null,
        actorId: String? = null
    ) : Product {
        if (!productRepository.isSlugUnique(slug)) {
            throw IllegalArgumentException("Product slug '$slug' already exists")
        }

        val product = productRepository.create(
            Product(
                categoryId = categoryId,
                title = title,
                slug = slug,
                description = description,
                shortDescription = shortDescription,
                productType = productType,
                brand = brand,
                isCustomizable = isCustomizable,
                seoTitle = seoTitle,
                seoDescription = seoDescription
            )
        )

        // Log product creation
        auditLogRepository.logAction(
            actorId = actorId,
            actorType = "user",
            action = "product_created",
            entityType = "product",
            entityId = product.id,
            metadata = mapOf(
                "title" to title,
                "slug" to slug,
                "product_type" to productType
            )
        )
        return product
    }

    suspend fun updateProduct(
        id: String,
        title: String? = null,
        slug: String? = null,
        categoryId: String? = null,
        description: String? = null,
        shortDescription: String? = null,
        brand: String? = null,
        isCustomizable: Boolean? = null,
        isFeatured: Boolean? = null,
        seoTitle: String? = null,
        seoDescription: String? = null,
        actorId: String? = null
    ): Product? {
        val existingProduct = productRepository.read(id) ?: return null

        // Validate slug uniqueness if changed
        if (slug != null && slug != existingProduct.slug) {
            if (!productRepository.isSlugUnique(slug, id)) {
                throw IllegalArgumentException("Product slug '$slug' already exists")
            }
        }

        val updatedProduct = productRepository.update(
            id,
            existingProduct.copy(
                title = title ?: existingProduct.title,
                slug = slug ?: existingProduct.slug,
                categoryId = categoryId ?: existingProduct.categoryId,
                description = description ?: existingProduct.description,
                shortDescription = shortDescription ?: existingProduct.shortDescription,
                brand = brand ?: existingProduct.brand,
                isCustomizable = isCustomizable ?: existingProduct.isCustomizable,
                isFeatured = isFeatured ?: existingProduct.isFeatured,
                seoTitle = seoTitle ?: existingProduct.seoTitle,
                seoDescription = seoDescription ?: existingProduct.seoDescription
            )
        )

        // Log product update
        if (updatedProduct != null) {
            auditLogRepository.logAction(
                actorId = actorId,
                actorType = "user",
                action = "product_updated",
                entityType = "product",
                entityId = id,
                changes = mapOf(
                    "old" to mapOf(
                        "title" to existingProduct.title,
                        "slug" to existingProduct.slug,
                        "status" to existingProduct.status
                    ),
                    "new" to mapOf(
                        "title" to updatedProduct.title,
                        "slug" to updatedProduct.slug,
                        "status" to updatedProduct.status
                    )
                )
            )
        }

        return updatedProduct
    }

    suspend fun publishProduct(id: String, actorId: String? = null): Product? {
        val product = productRepository.updateStatus(id, "published")

        if (product != null) {
            auditLogRepository.logAction(
                actorId = actorId,
                actorType = "user",
                action = "product_published",
                entityType = "product",
                entityId = id,
                metadata = mapOf("title" to product.title)
            )
        }

        return product
    }

    suspend fun deleteProduct(id: String, actorId: String? = null): Product? {
        val product = productRepository.softDelete(id)

        if (product != null) {
            auditLogRepository.logAction(
                actorId = actorId,
                actorType = "user",
                action = "product_deleted",
                entityType = "product",
                entityId = id,
                metadata = mapOf("title" to product.title)
            )
        }

        return product
    }

    suspend fun getProductDetails(productId: String): ProductDetails? {
        val product = productRepository.read(productId) ?: return null
        val media = productMediaRepository.findByProductId(productId)
        val variants = productVariantRepository.findByProductId(productId)
        val reviews = productReviewRepository.findByProductId(productId)
        val averageRating = productReviewRepository.getAverageRating(productId)
        val relatedProducts = productRepository.findRelated(productId)

        return ProductDetails(
            product = product,
            media = media,
            variants = variants,
            reviews = reviews,
            averageRating = averageRating,
            relatedProducts = relatedProducts
        )
    }

    suspend fun searchProducts(
        query: String,
        categoryId: String? = null,
        brand: String? = null,
        productType: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        return productRepository.search(
            query = query,
            categoryId = categoryId,
            brand = brand,
            productType = productType,
            offset = offset,
            limit = limit
        )
    }

    suspend fun getFeaturedProducts(limit: Int = 10): List<Product> {
        return productRepository.findFeatured(0, limit)
    }

    suspend fun getLatestProducts(limit: Int = 10): List<Product> {
        return productRepository.getLatest(limit)
    }

    suspend fun getProductStats(): ProductStats {
        return productRepository.getProductStats()
    }
}

@Serializable
data class ProductDetails(
    val product: Product,
    val media: List<ProductMedia>,
    val variants: List<ProductVariant>,
    val reviews: List<ProductReview>,
    val averageRating: Double?,
    val relatedProducts: List<Product>
)