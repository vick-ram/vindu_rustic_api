package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.example.data.mappers.ProductVariantMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.catalog.ProductVariant
import java.math.BigDecimal
import java.time.OffsetDateTime

@Component
class ProductVariantRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    productVariantMapper: ProductVariantMapper,
) : CrudRepository<ProductVariant, String>(
    connectionFactory = connectionFactory,
    tableName = "product_variants",
    mapper = productVariantMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    // Get variants for a product
    suspend fun findByProductId(productId: String): List<ProductVariant> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE product_id = :productId AND is_active = true 
            ORDER BY created_at ASC
        """.trimIndent()

        return executeQuery(sql, mapOf("productId" to productId))
    }

    // Find variant by SKU
    suspend fun findBySku(sku: String): ProductVariant? {
        val sql = "SELECT * FROM $tableName WHERE sku = :sku"

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("sku" to sku))
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Search variants by price range
    suspend fun findByPriceRange(
        minPrice: BigDecimal,
        maxPrice: BigDecimal,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductVariant> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE price >= :minPrice AND price <= :maxPrice AND is_active = true 
            ORDER BY price ASC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "minPrice" to minPrice,
            "maxPrice" to maxPrice,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Deactivate a variant
    suspend fun deactivate(id: String): Boolean {
        val sql = """
            UPDATE $tableName 
            SET is_active = false, updated_at = :updatedAt 
            WHERE id = :id
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("id" to id, "updatedAt" to OffsetDateTime.now()))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle() > 0
        }
    }

    // Bulk update prices (e.g., for sales)
    suspend fun bulkUpdatePrices(
        productId: String,
        priceMultiplier: BigDecimal
    ): Int {
        val sql = """
            UPDATE $tableName 
            SET price = price * :multiplier, 
                updated_at = :updatedAt 
            WHERE product_id = :productId
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf(
                "productId" to productId,
                "multiplier" to priceMultiplier,
                "updatedAt" to OffsetDateTime.now()
            ))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Get inventory statistics
    suspend fun getPriceStats(productId: String): PriceStats? {
        val sql = """
            SELECT 
                MIN(price) as min_price,
                MAX(price) as max_price,
                AVG(price) as avg_price,
                COUNT(*) as variant_count
            FROM $tableName 
            WHERE product_id = :productId AND is_active = true
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("productId" to productId))
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    PriceStats(
                        minPrice = row.get("min_price", BigDecimal::class.java)!!,
                        maxPrice = row.get("max_price", BigDecimal::class.java)!!,
                        avgPrice = row.get("avg_price", BigDecimal::class.java)!!,
                        variantCount = row.get("variant_count", Long::class.java)!!.toInt()
                    )
                }
                .awaitFirstOrNull()
        }
    }
}

@Serializable
data class PriceStats(
    @Contextual val minPrice: BigDecimal,
    @Contextual val maxPrice: BigDecimal,
    @Contextual val avgPrice: BigDecimal,
    val variantCount: Int
)