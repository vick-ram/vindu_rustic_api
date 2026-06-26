package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.CartItemMapper
import org.example.domain.models.sales.CartItem
import java.time.OffsetDateTime

class CartItemRepository(
    connectionFactory: ConnectionFactory,
    cartItemMapper: CartItemMapper
) : CrudRepository<CartItem, String>(
    connectionFactory = connectionFactory,
    tableName = "cart_items",
    mapper = cartItemMapper
) {
    override val generatedColumns = listOf("id", "created_at", "updated_at")

    // Get items for a cart
    suspend fun findByCartId(cartId: String): List<CartItem> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE cart_id = :cartId 
            ORDER BY created_at ASC
        """.trimIndent()

        return executeQuery(sql, mapOf("cartId" to cartId))
    }

    // Find item by cart and variant
    suspend fun findByCartAndVariant(
        cartId: String,
        variantId: String,
        customizationDetails: Map<String, Any>? = null
    ): CartItem? {
        val customizationFilter = if (customizationDetails != null) {
            "AND customization_details = :customizationDetails"
        } else {
            "AND customization_details IS NULL"
        }

        val sql = """
            SELECT * FROM $tableName 
            WHERE cart_id = :cartId 
              AND variant_id = :variantId 
              $customizationFilter
            LIMIT 1
        """.trimIndent()

        return connectionFactory.useConnection {
            val statement = createStatement(sql)
                .bind("cartId", cartId)
                .bind("variantId", variantId)

            if (customizationDetails != null) {
                statement.bind("customizationDetails", customizationDetails)
            }

            statement.execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Add item to cart (or update quantity if exists)
    suspend fun addOrUpdateItem(
        cartId: String,
        variantId: String,
        quantity: Int,
        customizationDetails: Map<String, Any>? = null
    ): CartItem {
        val existingItem = findByCartAndVariant(cartId, variantId, customizationDetails)

        return if (existingItem != null) {
            val newQuantity = existingItem.quantity + quantity
            update(existingItem.id, existingItem.copy(quantity = newQuantity))!!
        } else {
            create(
                CartItem(
                    cartId = cartId,
                    variantId = variantId,
                    quantity = quantity,
                    customizationDetails = customizationDetails
                )
            )
        }
    }

    // Update item quantity
    suspend fun updateQuantity(id: String, quantity: Int): CartItem? {
        if (quantity <= 0) {
            delete(id)
            return null
        }

        val sql = """
            UPDATE $tableName 
            SET quantity = :quantity, 
                updated_at = :updatedAt 
            WHERE id = :id 
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("id", id)
                .bind("quantity", quantity)
                .bind("updatedAt", OffsetDateTime.now())
                .execute()
                .awaitSingle()
                .map(rowMapper)
                .awaitFirstOrNull()
        }
    }

    // Get cart total
    suspend fun getCartTotal(cartId: String): CartTotal? {
        val sql = """
            SELECT 
                COUNT(*) as item_count,
                COALESCE(SUM(ci.quantity), 0) as total_quantity
            FROM $tableName ci
            WHERE ci.cart_id = :cartId
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("cartId", cartId)
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    CartTotal(
                        itemCount = row.get("item_count", Long::class.java)!!.toInt(),
                        totalQuantity = row.get("total_quantity", Long::class.java)!!.toInt()
                    )
                }
                .awaitFirstOrNull() ?: CartTotal(0, 0)
        }
    }

    // Clear cart
    suspend fun clearCart(cartId: String): Int {
        val sql = "DELETE FROM $tableName WHERE cart_id = :cartId"

        return connectionFactory.withTransaction { connection ->
            connection.createStatement(sql)
                .bind("cartId", cartId)
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle() as Int
        }
    }

    // Check if variant is in any cart
    suspend fun isVariantInAnyCart(variantId: String): Boolean {
        val sql = """
            SELECT COUNT(*) as count 
            FROM $tableName 
            WHERE variant_id = :variantId
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("variantId", variantId)
                .execute()
                .awaitSingle()
                .map { row, _ -> row.get("count", Long::class.java)!! > 0 }
                .awaitFirstOrNull() ?: false
        }
    }
}

data class CartTotal(
    val itemCount: Int,
    val totalQuantity: Int
)