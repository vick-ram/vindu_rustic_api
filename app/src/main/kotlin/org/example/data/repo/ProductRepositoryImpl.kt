package org.example.data.repo

import io.ktor.http.content.PartData
import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.ProductEntity
import org.example.data.db.tables.ProductTable
import org.example.data.mappers.ProductMapper
import org.example.domain.models.CreateProductRequest
import org.example.domain.models.Dimension
import org.example.domain.models.Media
import org.example.domain.models.MediaType
import org.example.domain.models.Product
import org.example.domain.models.StockInfo
import org.example.domain.repo.ProductRepository
import org.example.utils.customMatch
import org.example.utils.generateProductSku
import org.example.utils.now
import org.example.utils.saveMedia
import org.example.utils.suspendTransaction

class ProductRepositoryImpl(private val productMapper: ProductMapper) : CrudRepositoryImpl<ProductEntity, Product>(
    ProductEntity,
    Product::class
), ProductRepository {
    override suspend fun createProduct(
        request: CreateProductRequest,
        mediaFiles: List<PartData.FileItem>?
    ): Product? {
        // Create dimension if provided
        val dimensions =
            request.dimensions?.map { dimension ->
                Dimension(
                    width = dimension.width,
                    height = dimension.height,
                    depth = dimension.depth,
                    unit = dimension.unit
                ).validate()
            }

        // Process media files
        val mediaList = mediaFiles?.mapIndexed { index, item ->
            val fileName = saveMedia("uploads/products/", item)
            Media(
                url = "/media/products/$fileName",
                type = determineMediaType(item),
                altText = request.name,
                isPrimary = index == 0,
                displayOrder = index
            )
        } ?: emptyList()

        // Create product
        val product = Product(
            sku = generateProductSku(),
            name = request.name,
            description = request.description,
            shortDescription = request.shortDescription,
            basePrice = request.basePrice,
            viewed = false,
            categoryId = request.categoryId,
            stock = StockInfo(
                available = request.availableStock,
                lowStockThreshold = request.lowStockThreshold
            ),
            media = mediaList,
            dimensions = dimensions,
            isFavorite = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        ).validate()

        val newProduct = create(product)
        return newProduct
    }

    override suspend fun markProductAsFavorite(productId: String): Product? = suspendTransaction {
        ProductEntity.findByIdAndUpdate(productId) { update ->
            update.isFavorite = true
            update.updatedAt = LocalDateTime.now()
        }?.toDomain()
    }

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

    private fun determineMediaType(fileItem: PartData.FileItem): MediaType {
        val contentType = fileItem.contentType?.toString() ?: ""
        return when {
            contentType.startsWith("image/") -> MediaType.IMAGE
            contentType.startsWith("video/") -> MediaType.VIDEO
            else -> MediaType.DOCUMENT
        }
    }

}