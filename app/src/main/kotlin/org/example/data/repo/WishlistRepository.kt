package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.example.data.mappers.WishlistMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.CartItem
import org.example.domain.models.sales.Wishlist
import org.example.domain.models.sales.WishlistItem
import org.example.domain.models.system.AuditLog
import org.example.exceptions.NotFoundException

@Component
class WishlistRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    wishlistMapper: WishlistMapper,
    private val wishlistItemRepository: WishlistItemRepository,
    private val shoppingCartRepository: ShoppingCartRepository,
    private val cartItemRepository: CartItemRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val auditLogRepository: AuditLogRepository,
) : CrudRepository<Wishlist, String>(
    connectionFactory = connectionFactory,
    tableName = "wishlists",
    mapper = wishlistMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    /**
     * Add product to wishlist
     */
    suspend fun addToWishlist(
        userId: String,
        productId: String,
        variantId: String? = null,
        wishlistName: String = "Default",
        notes: String? = null
    ): WishlistItem? {
        val wishlist = getOrCreateWishlist(userId, wishlistName)

        // Check if item already exists
        if (wishlistItemRepository.isProductInWishlist(wishlist.id, productId, variantId)) {
            throw IllegalStateException("Product already in wishlist")
        }

        val item = wishlistItemRepository.addItem(
            wishlistId = wishlist.id,
            productId = productId,
            variantId = variantId,
            notes = notes
        )

        // Log audit
        if (item != null) {
            auditLogRepository.create(
                AuditLog(
                    actorId = userId,
                    actorType = "user",
                    action = "ADD_TO_WISHLIST",
                    entityType = "WishlistItem",
                    entityId = item.id,
                    changes = buildJsonObject {
                        put("product_id", JsonPrimitive(productId))
                        put("variant_id", JsonPrimitive(variantId ?: "null"))
                        put("wishlist_name", JsonPrimitive(wishlistName))
                    }
                )
            )
        }

        return item
    }

    /**
     * Move single item from wishlist to cart
     */
    suspend fun moveToCart(
        userId: String,
        wishlistItemId: String,
        quantity: Int = 1
    ): CartItem {
        return connectionFactory.withTransaction { connection ->
            val wishlistItem = wishlistItemRepository.read(wishlistItemId)
                ?: throw NotFoundException("Wishlist item not found: $wishlistItemId")

            // Verify the variant is active and available
            val variantId = wishlistItem.variantId ?: getDefaultVariantId(wishlistItem.productId)
            ?: throw IllegalStateException("No active variant available for product")

            val variant = productVariantRepository.read(variantId)
                ?: throw NotFoundException("Variant not found: $variantId")

            if (!variant.isActive) {
                throw IllegalStateException("Product variant is no longer available")
            }

            // Get or create cart for user
            val cart = shoppingCartRepository.getOrCreateForUser(userId)

            // Add to cart
            val cartItem = cartItemRepository.addOrUpdateItem(
                cartId = cart.id,
                variantId = variantId,
                quantity = quantity,
                customizationDetails = null // Can be enhanced to preserve wishlist notes as customization
            )

            // Remove from wishlist
            wishlistItemRepository.delete(wishlistItemId)

            // Log audit
            auditLogRepository.create(
                AuditLog(
                    actorId = userId,
                    actorType = "user",
                    action = "MOVE_WISHLIST_TO_CART",
                    entityType = "CartItem",
                    entityId = cartItem.id,
                    changes = buildJsonObject {
                        put("wishlist_item_id", JsonPrimitive(wishlistItemId))
                        put("product_id", JsonPrimitive(wishlistItem.productId))
                        put("variant_id", JsonPrimitive(variantId))
                        put("quantity", JsonPrimitive(quantity))
                    }
                )
            )

            cartItem
        }
    }

    /**
     * Move all items from wishlist to cart
     */
    suspend fun moveAllToCart(
        userId: String,
        wishlistId: String
    ): WishlistMoveResult {
        return connectionFactory.withTransaction { connection ->
            val wishlist = read(wishlistId)
                ?: throw NotFoundException("Wishlist not found: $wishlistId")

            val items = wishlistItemRepository.findByWishlistId(wishlistId)
            val cart = shoppingCartRepository.getOrCreateForUser(userId)

            val movedItems = mutableListOf<CartItem>()
            val failedItems = mutableListOf<FailedMove>()

            items.forEach { item ->
                try {
                    val variantId = item.variantId ?: getDefaultVariantId(item.productId)

                    if (variantId != null) {
                        val variant = productVariantRepository.read(variantId)
                        if (variant?.isActive == true) {
                            // Add to cart
                            val cartItem = cartItemRepository.addOrUpdateItem(
                                cartId = cart.id,
                                variantId = variantId,
                                quantity = 1
                            )

                            // Remove from wishlist
                            wishlistItemRepository.delete(item.id)
                            movedItems.add(cartItem)
                        } else {
                            failedItems.add(
                                FailedMove(
                                    wishlistItemId = item.id,
                                    productId = item.productId,
                                    reason = "Variant is inactive"
                                )
                            )
                        }
                    } else {
                        failedItems.add(
                            FailedMove(
                                wishlistItemId = item.id,
                                productId = item.productId,
                                reason = "No active variant available"
                            )
                        )
                    }
                } catch (e: Exception) {
                    failedItems.add(
                        FailedMove(
                            wishlistItemId = item.id,
                            productId = item.productId,
                            reason = e.message ?: "Unknown error"
                        )
                    )
                }
            }

            // Log audit
            auditLogRepository.create(
                AuditLog(
                    actorId = userId,
                    actorType = "user",
                    action = "MOVE_ALL_WISHLIST_TO_CART",
                    entityType = "Wishlist",
                    entityId = wishlistId,
                    changes = buildJsonObject {
                        put("moved_count", JsonPrimitive(movedItems.size))
                        put("failed_count", JsonPrimitive(failedItems.size))
                    }
                )
            )

            WishlistMoveResult(
                wishlistId = wishlistId,
                cartId = cart.id,
                movedItems = movedItems,
                failedItems = failedItems
            )
        }
    }

    /**
     * Add entire wishlist to cart without removing from wishlist
     */
    suspend fun addWishlistToCart(
        userId: String,
        wishlistId: String
    ): List<CartItem> {
        return connectionFactory.withTransaction { connection ->
            val items = wishlistItemRepository.findByWishlistId(wishlistId)
            val cart = shoppingCartRepository.getOrCreateForUser(userId)

            items.mapNotNull { item ->
                try {
                    val variantId = item.variantId ?: getDefaultVariantId(item.productId)

                    if (variantId != null) {
                        val variant = productVariantRepository.read(variantId)
                        if (variant?.isActive == true) {
                            cartItemRepository.addOrUpdateItem(
                                cartId = cart.id,
                                variantId = variantId,
                                quantity = 1
                            )
                        } else null
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    private suspend fun getOrCreateWishlist(userId: String, name: String): Wishlist {
        return if (name.equals("Default", ignoreCase = true)) {
            getOrCreateDefault(userId)
        } else {
            val existingWishlists = findByUserId(userId)
            existingWishlists.find { it.name == name } ?: create(
                Wishlist(userId = userId, name = name)
            )
        }
    }

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
            createNamedStatement(sql, mapOf("userId" to userId))
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

    private suspend fun getDefaultVariantId(productId: String): String? {
        val sql = """
            SELECT id FROM product_variants 
            WHERE product_id = :productId 
              AND is_active = true 
            ORDER BY created_at ASC 
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("productId" to productId))
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("id", String::class.java) }
                .awaitFirstOrNull()
        }
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
            createNamedStatement(sql, mapOf("wishlistId" to wishlistId))
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

@Serializable
data class WishlistMoveResult(
    val wishlistId: String,
    val cartId: String,
    val movedItems: List<CartItem>,
    val failedItems: List<FailedMove>
)

@Serializable
data class FailedMove(
    val wishlistItemId: String,
    val productId: String,
    val reason: String
)