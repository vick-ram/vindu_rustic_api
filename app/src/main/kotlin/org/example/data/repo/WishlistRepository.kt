package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Serializable
import org.example.data.mappers.WishlistMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.Wishlist
import org.koin.core.annotation.Single

@Component
class WishlistRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    wishlistMapper: WishlistMapper
) : CrudRepository<Wishlist, String>(
    connectionFactory = connectionFactory,
    tableName = "wishlists",
    mapper = wishlistMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Find wishlists by user
    suspend fun findByUserId(userId: String): List<Wishlist> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("userId" to userId))
    }

    // Find default wishlist for user
    suspend fun findDefaultByUserId(userId: String): Wishlist? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId AND name = 'default' 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("userId", userId)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get or create default wishlist
    suspend fun getOrCreateDefault(userId: String): Wishlist {
        return findDefaultByUserId(userId) ?: create(
            Wishlist(userId = userId, name = "default")
        )
    }

    // Find public wishlists
    suspend fun findPublicWishlists(
        offset: Int = 0,
        limit: Int = 20
    ): List<Wishlist> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE is_public = true 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(sql, mapOf(
            "limit" to limit,
            "offset" to offset.toLong()
        ))
    }

    // Get wishlist with item count
    suspend fun getWishlistWithItemCount(wishlistId: String): WishlistWithItemCount? {
        val sql = """
            SELECT w.*, COUNT(wi.id) as item_count
            FROM $tableName w
            LEFT JOIN wishlist_items wi ON w.id = wi.wishlist_id
            WHERE w.id = :wishlistId
            GROUP BY w.id
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("wishlistId", wishlistId)
                .execute()
                .awaitSingle()
                .map { row, rowMetadata ->
                    WishlistWithItemCount(
                        wishlist = rowMapper.apply(row, rowMetadata),
                        itemCount = row.get("item_count", Long::class.java)!!.toInt()
                    )
                }
                .awaitFirstOrNull()
        }
    }
}

@Serializable
data class WishlistWithItemCount(
    val wishlist: Wishlist,
    val itemCount: Int
)