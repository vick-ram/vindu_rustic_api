package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Serializable
import org.example.data.mappers.ShoppingCartMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.ShoppingCart
import org.koin.core.annotation.Single
import java.time.OffsetDateTime
import kotlin.uuid.Uuid

@Component
class ShoppingCartRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    shoppingCartMapper: ShoppingCartMapper
) : CrudRepository<ShoppingCart, String>(
    connectionFactory = connectionFactory,
    tableName = "shopping_carts",
    mapper = shoppingCartMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    // Find cart by user ID
    suspend fun findByUserId(userId: String): ShoppingCart? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
            ORDER BY updated_at DESC 
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

    // Find cart by guest token
    suspend fun findByGuestToken(guestToken: Uuid): ShoppingCart? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE guest_token = :guestToken 
            ORDER BY updated_at DESC 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("guestToken", guestToken)
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get or create cart for user
    suspend fun getOrCreateForUser(userId: String): ShoppingCart {
        return findByUserId(userId) ?: create(
            ShoppingCart(userId = userId)
        )
    }

    // Get or create cart for guest
    suspend fun getOrCreateForGuest(guestToken: Uuid): ShoppingCart {
        return findByGuestToken(guestToken) ?: create(
            ShoppingCart(guestToken = guestToken)
        )
    }

    // Merge guest cart into user cart
    suspend fun mergeCarts(guestToken: Uuid, userId: String): ShoppingCart {
        return connectionFactory.withTransaction { connection ->
            val guestCart = findByGuestToken(guestToken)
            val userCart = getOrCreateForUser(userId)

            if (guestCart != null && guestCart.id != userCart.id) {
                // Move all items from guest cart to user cart
                val moveItemsSql = """
                    UPDATE cart_items 
                    SET cart_id = :userCartId, 
                        updated_at = :updatedAt 
                    WHERE cart_id = :guestCartId
                """.trimIndent()

                connection.createStatement(moveItemsSql)
                    .bind("userCartId", userCart.id)
                    .bind("guestCartId", guestCart.id)
                    .bind("updatedAt", OffsetDateTime.now())
                    .execute()
                    .awaitSingle()

                // Delete guest cart
                delete(guestCart.id)
            }

            userCart
        }
    }

    // Touch cart (update updated_at)
    suspend fun touchCart(id: String): Boolean {
        val sql = """
            UPDATE $tableName 
            SET updated_at = :updatedAt 
            WHERE id = :id
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("updatedAt", OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle() > 0
        }
    }

    // Delete abandoned carts older than specified days
    suspend fun deleteAbandonedCarts(olderThanDays: Int): Int {
        val sql = """
            DELETE FROM $tableName 
            WHERE updated_at < :cutoffDate 
              AND user_id IS NULL
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("cutoffDate", OffsetDateTime.now().minusDays(olderThanDays.toLong()))
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()
                .toInt()
        }
    }

    // Get cart with item count
    suspend fun getCartWithItemCount(cartId: String): CartWithItemCount? {
        val sql = """
            SELECT c.*, COUNT(ci.id) as item_count
            FROM $tableName c
            LEFT JOIN cart_items ci ON c.id = ci.cart_id
            WHERE c.id = :cartId
            GROUP BY c.id
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("cartId", cartId)
                .execute()
                .awaitSingle()
                .map { row, rowMetadata ->
                    CartWithItemCount(
                        cart = rowMapper.apply(row, rowMetadata),
                        itemCount = (row.get("item_count", Long::class.java) ?: 0L).toInt()
                    )
                }
                .awaitFirstOrNull()
        }
    }
}

@Serializable
data class CartWithItemCount(
    val cart: ShoppingCart,
    val itemCount: Int
)