package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Serializable
import org.example.data.mappers.ShoppingCartMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.CartItem
import org.example.domain.models.sales.ShoppingCart
import org.example.exceptions.InsufficientInventoryException
import org.example.exceptions.NotFoundException
import java.time.OffsetDateTime
import kotlin.uuid.Uuid

@Component
class ShoppingCartRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    shoppingCartMapper: ShoppingCartMapper,
    private val cartItemRepository: CartItemRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val inventoryRepository: InventoryRepository,
    private val auditLogRepository: AuditLogRepository
) : CrudRepository<ShoppingCart, String>(
    connectionFactory = connectionFactory,
    tableName = "shopping_carts",
    mapper = shoppingCartMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    /**
     * Add to cart with optional wishlist save
     */
    suspend fun addToCart(
        userId: String?,
        guestToken: Uuid?,
        variantId: String,
        quantity: Int = 1,
    ): AddToCartResult {
        return connectionFactory.withTransaction { _ ->
            val variant = productVariantRepository.read(variantId)
                ?: throw NotFoundException("Variant not found: $variantId")

            if (!variant.isActive) {
                throw IllegalStateException("Product variant is not available")
            }

            // Check inventory
            val availableInventory = inventoryRepository.findByVariantAndWarehouse(
                variantId,
                getDefaultWarehouse()
            )

            if (availableInventory != null && availableInventory.availableQuantity < quantity) {
                throw InsufficientInventoryException(
                    variantId = variantId,
                    required = quantity,
                    available = availableInventory.availableQuantity
                )
            }

            // Get or create cart
            val cart = if (userId != null) {
                getOrCreateForUser(userId)
            } else if (guestToken != null) {
                getOrCreateForGuest(guestToken)
            } else {
                throw IllegalArgumentException("Either userId or guestToken must be provided")
            }

            // Add to cart
            val cartItem = cartItemRepository.addOrUpdateItem(
                cartId = cart.id,
                variantId = variantId,
                quantity = quantity
            )

            // Touch cart
            touchCart(cart.id)

            AddToCartResult(
                cartItem = cartItem,
                cartId = cart.id
            )
        }
    }

    private suspend fun getDefaultWarehouse(): String {
        // Get first available warehouse or default
        val sql = "SELECT id FROM warehouses LIMIT 1"
        return connectionFactory.useConnection {
            createStatement(sql)
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("id", String::class.java)!! }
                .awaitFirstOrNull() ?: throw NotFoundException("No warehouse configured")
        }
    }

    // Find cart by user ID
    suspend fun findByUserId(userId: String): ShoppingCart? {
        val sql = """
            SELECT * FROM $tableName 
            WHERE user_id = :userId 
            ORDER BY updated_at DESC 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("userId" to userId))
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
            createNamedStatement(sql, mapOf("guestToken" to guestToken))
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

                connection.createNamedStatement(
                    moveItemsSql,
                    mapOf(
                        "userCartId" to userCart.id,
                        "guestCartId" to guestCart.id,
                        "updatedAt" to OffsetDateTime.now()
                    )
                )
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
            connection.createNamedStatement(sql, mapOf("id" to id, "updatedAt" to OffsetDateTime.now()))
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
            connection.createNamedStatement(
                sql,
                mapOf("cutoffDate" to OffsetDateTime.now().minusDays(olderThanDays.toLong()))
            )
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
            createNamedStatement(sql, mapOf("cartId" to cartId))
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

@Serializable
data class AddToCartResult(
    val cartItem: CartItem,
    val cartId: String
)