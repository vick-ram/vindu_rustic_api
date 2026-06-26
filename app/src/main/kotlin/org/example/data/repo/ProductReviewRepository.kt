package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.ProductReviewMapper
import org.example.domain.models.catalog.ProductReview

class ProductReviewRepository(
    connectionFactory: ConnectionFactory,
    private val productReviewMapper: ProductReviewMapper
            ) : CrudRepository<ProductReview, String>(
        connectionFactory = connectionFactory, tableName = "product_reviews", mapper = productReviewMapper
    ){

    suspend fun findByProductId(
        productId: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<ProductReview> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE product_id = :productId 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "productId" to productId,
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    suspend fun findByUserId(userId: String): List<ProductReview> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("userId" to userId))
    }

    suspend fun findVerifiedByProductId(productId: String): List<ProductReview> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE product_id = :productId AND is_verified_purchase = true 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("productId" to productId))
    }

    suspend fun getAverageRating(productId: String): Double? {
        val sql = """
            SELECT AVG(rating) as avg_rating 
            FROM $tableName 
            WHERE product_id = :productId
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("productId", productId)
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("avg_rating", Double::class.java) }
                .awaitFirstOrNull()
        }
    }

    suspend fun getRatingDistribution(productId: String): Map<Int, Int> {
        val sql = """
            SELECT rating, COUNT(*) as count 
            FROM $tableName 
            WHERE product_id = :productId 
            GROUP BY rating 
            ORDER BY rating DESC
        """.trimIndent()

        val results = connectionFactory.useConnection {
            createStatement(sql)
                .bind("productId", productId)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    row.get("rating", Int::class.java) to row.get("count", Long::class.java).toInt()
                }
                .asFlow()
                .toList()
        }

        return results.toMap()
    }
}