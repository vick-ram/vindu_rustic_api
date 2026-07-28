package org.example.data.repo

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.ProductMediaMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.catalog.ProductMedia
import org.koin.core.annotation.Single

@Component
class ProductMediaRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    productMediaMapper: ProductMediaMapper
) : CrudRepository<ProductMedia, String>(
    connectionFactory = connectionFactory, tableName = "product_media", idColumn = "id", mapper = productMediaMapper
) {
    override val generatedColumns: List<String> = listOf("id", "created_at")

    // Get media for a product
    suspend fun findByProductId(productId: String): List<ProductMedia> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE product_id = :productId 
            ORDER BY sort_order ASC, created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("productId" to productId))
    }

    // Get media for a variant
    suspend fun findByVariantId(variantId: String): List<ProductMedia> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE variant_id = :variantId 
            ORDER BY sort_order ASC
        """.trimIndent()

        return executeQuery(sql, mapOf("variantId" to variantId))
    }

    // Get media by type
    suspend fun findByMediaType(productId: String, mediaType: String): List<ProductMedia> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE product_id = :productId AND media_type = :mediaType 
            ORDER BY sort_order ASC
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "productId" to productId,
            "mediaType" to mediaType
        ))
    }

    // Update sort order for multiple media items
    suspend fun updateSortOrders(mediaOrders: Map<String, Int>) {
        connectionFactory.withTransaction { connection ->
            mediaOrders.forEach { (mediaId, order) ->
                val sql = """
                    UPDATE $tableName 
                    SET sort_order = :sortOrder 
                    WHERE id = :id
                """.trimIndent()

                connection.createNamedStatement(sql, mapOf("id" to mediaId, "sortOrder" to order))
                    .execute()
                    .awaitSingle()
            }
        }
    }

    // Delete all media for a product
    suspend fun deleteByProductId(productId: String): Int {
        val sql = "DELETE FROM $tableName WHERE product_id = :productId"

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, mapOf("productId" to productId))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }
}