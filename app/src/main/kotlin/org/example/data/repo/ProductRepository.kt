package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Serializable
import org.example.data.mappers.ProductMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.catalog.Product
import org.koin.core.annotation.Single
import java.time.OffsetDateTime

@Component
class ProductRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    productMapper: ProductMapper
) : CrudRepository<Product, String>(
    connectionFactory = connectionFactory,
    tableName = "products",
    mapper = productMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    // Override update to handle updated_at
    override suspend fun update(id: String, model: Product): Product? {
        validateProduct(model)
        return super.update(id, model)
    }

    // Create with validation
    override suspend fun create(model: Product): Product {
        validateProduct(model)
        return super.create(model)
    }

    // Find product by slug
    suspend fun findBySlug(slug: String, includeDeleted: Boolean = false): Product? {
        val deletedFilter = if (!includeDeleted) "AND deleted_at IS NULL" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE slug = :slug 
            $deletedFilter
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("slug", slug)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Find products by category
    suspend fun findByCategoryId(
        categoryId: String,
        status: String? = "published",
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        val statusFilter = if (status != null) "AND status = :status" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE category_id = :categoryId 
              AND deleted_at IS NULL 
              $statusFilter
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf(
            "categoryId" to categoryId,
            "limit" to limit,
            "offset" to offset.toLong()
        )

        if (status != null) {
            params["status"] = status
        }

        return executeQuery(sql, params)
    }

    // Find products by status
    suspend fun findByStatus(
        status: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = :status 
              AND deleted_at IS NULL 
            ORDER BY updated_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "status" to status,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Find featured products
    suspend fun findFeatured(
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE is_featured = true 
              AND status = 'published' 
              AND deleted_at IS NULL 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Find customizable products
    suspend fun findCustomizable(
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE is_customizable = true 
              AND status = 'published' 
              AND deleted_at IS NULL 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Find products by brand
    suspend fun findByBrand(
        brand: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE brand = :brand 
              AND status = 'published' 
              AND deleted_at IS NULL 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "brand" to brand,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Find products by type
    suspend fun findByProductType(
        productType: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE product_type = :productType 
              AND deleted_at IS NULL 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "productType" to productType,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Search products
    suspend fun search(
        query: String,
        categoryId: String? = null,
        status: String? = "published",
        productType: String? = null,
        brand: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): List<Product> {
        val conditions = mutableListOf(
            "(title ILIKE :query OR short_description ILIKE :query OR description ILIKE :query)",
            "deleted_at IS NULL"
        )
        val params = mutableMapOf<String, Any>(
            "query" to "%$query%",
            "limit" to limit,
            "offset" to offset.toLong()
        )

        status?.let {
            conditions.add("status = :status")
            params["status"] = it
        }

        categoryId?.let {
            conditions.add("category_id = :categoryId")
            params["categoryId"] = it
        }

        productType?.let {
            conditions.add("product_type = :productType")
            params["productType"] = it
        }

        brand?.let {
            conditions.add("brand = :brand")
            params["brand"] = it
        }

        val sql = """
            SELECT * FROM $tableName 
            WHERE ${conditions.joinToString(" AND ")} 
            ORDER BY 
                CASE WHEN is_featured = true THEN 1 ELSE 2 END,
                created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, params)
    }

    // Full-text search with ranking
    suspend fun fullTextSearch(
        query: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductSearchResult> {
        val sql = """
            SELECT 
                p.*,
                ts_rank(to_tsvector('english', p.title || ' ' || COALESCE(p.short_description, '') || ' ' || COALESCE(p.description, '')), 
                        plainto_tsquery('english', :query)) as relevance
            FROM $tableName p
            WHERE p.deleted_at IS NULL 
              AND p.status = 'published'
              AND to_tsvector('english', p.title || ' ' || COALESCE(p.short_description, '') || ' ' || COALESCE(p.description, '')) 
                  @@ plainto_tsquery('english', :query)
            ORDER BY relevance DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("query", query)
                .bind("limit", limit)
                .bind("offset", offset.toLong())
                .execute()
                .awaitSingle()
                .map { row, rowMetadata ->
                    ProductSearchResult(
                        product = rowMapper.apply(row, rowMetadata),
                        relevance = row.get("relevance", Double::class.java) ?: 0.0
                    )
                }
                .asFlow()
                .toList()
        }
    }

    // Update product status
    suspend fun updateStatus(id: String, status: String): Product? {
        val validStatuses = listOf("draft", "published", "archived")
        if (status !in validStatuses) {
            throw IllegalArgumentException("Invalid product status: $status. Must be one of: ${validStatuses.joinToString()}")
        }

        val sql = """
            UPDATE $tableName 
            SET status = :status,
                updated_at = :updatedAt 
            WHERE id = :id 
              AND deleted_at IS NULL 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("status", status)
                .bind("updatedAt", OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Soft delete product
    suspend fun softDelete(id: String): Product? {
        val sql = """
            UPDATE $tableName 
            SET deleted_at = :deletedAt,
                updated_at = :updatedAt,
                status = 'archived'
            WHERE id = :id 
              AND deleted_at IS NULL 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val now = OffsetDateTime.now()
            connection.createStatement(sql)
                .bind("id", id)
                .bind("deletedAt", now)
                .bind("updatedAt", now)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Restore soft-deleted product
    suspend fun restore(id: String): Product? {
        val sql = """
            UPDATE $tableName 
            SET deleted_at = NULL,
                updated_at = :updatedAt,
                status = 'draft'
            WHERE id = :id 
              AND deleted_at IS NOT NULL 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("updatedAt", OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Toggle featured status
    suspend fun toggleFeatured(id: String): Product? {
        val sql = """
            UPDATE $tableName 
            SET is_featured = NOT is_featured,
                updated_at = :updatedAt 
            WHERE id = :id 
              AND deleted_at IS NULL 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("updatedAt", OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get related products (same category or brand)
    suspend fun findRelated(
        productId: String,
        limit: Int = 10
    ): List<Product> {
        val sql = """
            WITH current_product AS (
                SELECT category_id, brand 
                FROM $tableName 
                WHERE id = :productId
            )
            SELECT p.* 
            FROM $tableName p, current_product cp
            WHERE p.id != :productId 
              AND p.deleted_at IS NULL 
              AND p.status = 'published'
              AND (p.category_id = cp.category_id OR p.brand = cp.brand)
            ORDER BY 
                CASE WHEN p.category_id = cp.category_id AND p.brand = cp.brand THEN 1
                     WHEN p.category_id = cp.category_id THEN 2
                     ELSE 3
                END,
                p.is_featured DESC,
                p.created_at DESC 
            LIMIT :limit
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "productId" to productId,
            "limit" to limit
        ))
    }

    // Get product statistics
    suspend fun getProductStats(): ProductStats {
        val sql = """
            SELECT 
                COUNT(*) as total_products,
                COUNT(CASE WHEN status = 'published' AND deleted_at IS NULL THEN 1 END) as published,
                COUNT(CASE WHEN status = 'draft' AND deleted_at IS NULL THEN 1 END) as draft,
                COUNT(CASE WHEN status = 'archived' OR deleted_at IS NOT NULL THEN 1 END) as archived,
                COUNT(CASE WHEN is_featured = true AND deleted_at IS NULL THEN 1 END) as featured,
                COUNT(CASE WHEN is_customizable = true AND deleted_at IS NULL THEN 1 END) as customizable,
                COUNT(DISTINCT brand) as unique_brands,
                COUNT(DISTINCT category_id) as unique_categories,
                COUNT(DISTINCT product_type) as unique_product_types
            FROM $tableName
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    ProductStats(
                        totalProducts = (row.get("total_products", Long::class.java) ?: 0L).toInt(),
                        published = (row.get("published", Long::class.java) ?: 0L).toInt(),
                        draft = (row.get("draft", Long::class.java) ?: 0L).toInt(),
                        archived = (row.get("archived", Long::class.java) ?: 0L).toInt(),
                        featured = (row.get("featured", Long::class.java) ?: 0L).toInt(),
                        customizable = (row.get("customizable", Long::class.java) ?: 0L).toInt(),
                        uniqueBrands = (row.get("unique_brands", Long::class.java) ?: 0L).toInt(),
                        uniqueCategories = (row.get("unique_categories", Long::class.java) ?: 0L).toInt(),
                        uniqueProductTypes = (row.get("unique_product_types", Long::class.java) ?: 0L).toInt()
                    )
                }
                .awaitFirstOrNull() ?: ProductStats(0, 0, 0, 0, 0, 0, 0, 0, 0)
        }
    }

    // Get products by date range
    suspend fun findByDateRange(
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): List<Product> {
        val statusFilter = if (status != null) "AND status = :status" else ""

        val sql = """
            SELECT * FROM $tableName 
            WHERE created_at BETWEEN :startDate AND :endDate 
              AND deleted_at IS NULL 
              $statusFilter
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = mutableMapOf(
            "startDate" to startDate,
            "endDate" to endDate,
            "limit" to limit,
            "offset" to offset.toLong()
        )

        if (status != null) {
            params["status"] = status
        }

        return executeQuery(sql, params)
    }

    // Bulk update status
    suspend fun bulkUpdateStatus(ids: List<String>, status: String): Int {
        if (ids.isEmpty()) return 0

        val validStatuses = listOf("draft", "published", "archived")
        if (status !in validStatuses) {
            throw IllegalArgumentException("Invalid product status: $status")
        }

        val placeholders = List(ids.size) { index -> ":id$index" }

        val sql = """
            UPDATE $tableName 
            SET status = :status,
                updated_at = :updatedAt 
            WHERE id IN (${placeholders.joinToString(", ")}) 
              AND deleted_at IS NULL
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val statement = connection.createStatement(sql)
                .bind("status", status)
                .bind("updatedAt", OffsetDateTime.now())

            ids.forEachIndexed { index, id ->
                statement.bind("id$index", id)
            }

            statement.execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Get latest products
    suspend fun getLatest(
        limit: Int = 10,
        status: String = "published"
    ): List<Product> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE status = :status 
              AND deleted_at IS NULL 
            ORDER BY created_at DESC 
            LIMIT :limit
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "status" to status,
            "limit" to limit
        ))
    }

    // Check if slug is unique
    suspend fun isSlugUnique(slug: String, excludeId: String? = null): Boolean {
        val excludeFilter = if (excludeId != null) "AND id != :excludeId" else ""

        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName 
            WHERE slug = :slug 
              AND deleted_at IS NULL 
              $excludeFilter
        """.trimIndent()

        return connectionFactory.useConnection {
            val statement = createStatement(sql)
                .bind("slug", slug)

            if (excludeId != null) {
                statement.bind("excludeId", excludeId)
            }

            statement.execute()
                .awaitSingle()
                .map { row, _ -> row.get("count", Long::class.java) == 0L }
                .awaitFirstOrNull() ?: true
        }
    }

    // Get products with low stock (requires joining with inventory)
    suspend fun getLowStockProducts(
        warehouseId: String? = null,
        threshold: Int = 10,
        limit: Int = 20
    ): List<ProductWithStock> {
        val warehouseFilter = if (warehouseId != null) "AND im.warehouse_id = :warehouseId" else ""

        val sql = """
            WITH stock_levels AS (
                SELECT 
                    pv.product_id,
                    COALESCE(SUM(
                        CASE 
                            WHEN im.movement_type IN ('inbound', 'return', 'adjustment_in') THEN im.quantity
                            WHEN im.movement_type IN ('outbound', 'damage', 'adjustment_out') THEN -im.quantity
                            ELSE 0
                        END
                    ), 0) as total_stock
                FROM product_variants pv
                LEFT JOIN inventory_movements im ON pv.id = im.variant_id
                WHERE 1=1 $warehouseFilter
                GROUP BY pv.product_id
            )
            SELECT 
                p.*,
                COALESCE(sl.total_stock, 0) as stock_level
            FROM $tableName p
            LEFT JOIN stock_levels sl ON p.id = sl.product_id
            WHERE p.deleted_at IS NULL 
              AND p.status = 'published'
              AND COALESCE(sl.total_stock, 0) <= :threshold
            ORDER BY COALESCE(sl.total_stock, 0) ASC 
            LIMIT :limit
        """.trimIndent()

        val params = mutableMapOf<String, Any>(
            "threshold" to threshold,
            "limit" to limit
        )

        if (warehouseId != null) {
            params["warehouseId"] = warehouseId
        }

        return connectionFactory.useConnection {
            val statement = createStatement(sql)
            params.forEach { (key, value) -> statement.bind(key, value) }

            statement.execute()
                .awaitSingle()
                .map { row, rowMetadata ->
                    ProductWithStock(
                        product = rowMapper.apply(row, rowMetadata),
                        stockLevel = row.get("stock_level", Int::class.java) ?: 0
                    )
                }
                .asFlow()
                .toList()
        }
    }

    private fun validateProduct(product: Product) {
        if (product.title.isBlank()) {
            throw IllegalArgumentException("Product title cannot be blank")
        }

        if (product.slug.isBlank()) {
            throw IllegalArgumentException("Product slug cannot be blank")
        }

        val validStatuses = listOf("draft", "published", "archived")
        if (product.status !in validStatuses) {
            throw IllegalArgumentException("Invalid product status: ${product.status}")
        }

        val validProductTypes = listOf("standard", "customizable", "digital", "service")
        if (product.productType !in validProductTypes) {
            throw IllegalArgumentException("Invalid product type: ${product.productType}")
        }
    }
}

@Serializable
data class ProductSearchResult(
    val product: Product,
    val relevance: Double
)

@Serializable
data class ProductStats(
    val totalProducts: Int,
    val published: Int,
    val draft: Int,
    val archived: Int,
    val featured: Int,
    val customizable: Int,
    val uniqueBrands: Int,
    val uniqueCategories: Int,
    val uniqueProductTypes: Int
)

@Serializable
data class ProductWithStock(
    val product: Product,
    val stockLevel: Int
)