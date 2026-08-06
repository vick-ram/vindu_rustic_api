package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.example.data.mappers.WishlistItemMapper
import org.example.di.Component
import org.example.di.Inject
import org.example.domain.models.sales.WishlistItem
import java.math.BigDecimal

@Component
class WishlistItemRepository @Inject constructor(
    connectionFactory: ConnectionFactory,
    wishlistItemMapper: WishlistItemMapper,
    private val cartItemRepo: CartItemRepository
) : CrudRepository<WishlistItem, String>(
    connectionFactory = connectionFactory,
    tableName = "wishlist_items",
    mapper = wishlistItemMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get items for a wishlist
    suspend fun findByWishlistId(wishlistId: String): List<WishlistItem> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE wishlist_id = :wishlistId 
            ORDER BY created_at DESC
        """.trimIndent()

        return executeQuery(sql, mapOf("wishlistId" to wishlistId))
    }

    // Check if product is in wishlist
    suspend fun isProductInWishlist(
        wishlistId: String,
        productId: String,
        variantId: String? = null
    ): Boolean {
        val params = mutableMapOf<String, Any>("wishlistId" to wishlistId, "productId" to productId)

        val variantFilter = if (variantId != null) {
            params["variantId"] = variantId
            "AND (variant_id = :variantId OR variant_id IS NULL)"
        } else {
            "AND variant_id IS NULL"
        }

        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName 
            WHERE wishlist_id = :wishlistId 
              AND product_id = :productId 
              $variantFilter
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, params)
            .execute()
                .awaitSingle()
                .map { row, _ -> row.get("count", Long::class.java)!! > 0 }
                .awaitFirstOrNull() ?: false
        }
    }

    // Add item to wishlist (if not already present)
    suspend fun addItem(
        wishlistId: String,
        productId: String,
        variantId: String? = null,
        notes: String? = null
    ): WishlistItem? {
        if (isProductInWishlist(wishlistId, productId, variantId)) {
            return null // Already in wishlist
        }

        return create(
            WishlistItem(
                wishlistId = wishlistId,
                productId = productId,
                variantId = variantId,
                notes = notes
            )
        )
    }

    // Remove item from wishlist
    suspend fun removeItem(
        wishlistId: String,
        productId: String,
        variantId: String? = null
    ): Boolean {
        val params = mutableMapOf<String, Any>("wishlistId" to wishlistId, "productId" to productId)

        val variantFilter = if (variantId != null) {
            params["variantId"] = variantId
            "AND (variant_id = :variantId OR variant_id IS NULL)"
        } else {
            "AND variant_id IS NULL"
        }

        val sql = """
            DELETE FROM $tableName 
            WHERE wishlist_id = :wishlistId 
              AND product_id = :productId 
              $variantFilter
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createNamedStatement(sql, params)
            .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle() > 0
        }
    }

    // Move item from wishlist to cart
    suspend fun moveToCart(
        wishlistItemId: String,
        cartId: String
    ): Boolean {
        return connectionFactory.withTransaction {
            val item = read(wishlistItemId) ?: return@withTransaction false

             cartItemRepo.addOrUpdateItem(cartId, item.variantId ?: item.productId, 1)

            // Remove from wishlist
            delete(wishlistItemId)

            true
        }
    }

    // Get wishlist items with product details
    suspend fun findByWishlistIdWithProductDetails(wishlistId: String): List<WishlistItemWithProduct> {
        val sql = """
            SELECT wi.*, 
                   p.title as product_title,
                   p.slug as product_slug,
                   pv.sku as variant_sku,
                   pv.title as variant_title,
                   pv.price as variant_price
            FROM $tableName wi
            LEFT JOIN products p ON wi.product_id = p.id
            LEFT JOIN product_variants pv ON wi.variant_id = pv.id
            WHERE wi.wishlist_id = :wishlistId
            ORDER BY wi.created_at DESC
        """.trimIndent()

        return connectionFactory.useConnection {
            createNamedStatement(sql, mapOf("wishlistId" to wishlistId))
                .execute()
                .awaitSingle()
                .map { row, rowMetadata ->
                    WishlistItemWithProduct(
                        wishlistItem = rowMapper.apply(row, rowMetadata),
                        productTitle = row.get("product_title", String::class.java),
                        productSlug = row.get("product_slug", String::class.java),
                        variantSku = row.get("variant_sku", String::class.java),
                        variantTitle = row.get("variant_title", String::class.java),
                        variantPrice = row.get("variant_price", BigDecimal::class.java)
                    )
                }
                .asFlow()
                .toList()
        }
    }
}

@Serializable
data class WishlistItemWithProduct(
    val wishlistItem: WishlistItem,
    val productTitle: String?,
    val productSlug: String?,
    val variantSku: String?,
    val variantTitle: String?,
    @Contextual
    val variantPrice: BigDecimal?
)