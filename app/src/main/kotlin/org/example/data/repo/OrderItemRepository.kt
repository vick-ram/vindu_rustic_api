package org.example.data.repo

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.example.data.mappers.OrderItemMapper
import org.example.domain.models.sales.OrderItem
import java.math.BigDecimal
import java.time.OffsetDateTime

class OrderItemRepository(
    connectionFactory: ConnectionFactory,
    orderItemMapper: OrderItemMapper
) : CrudRepository<OrderItem, String>(
    connectionFactory = connectionFactory,
    tableName = "order_items",
    mapper = orderItemMapper
) {
    override val generatedColumns = listOf("id", "created_at")

    // Get items for an order
    suspend fun findByOrderId(orderId: String): List<OrderItem> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE order_id = :orderId 
            ORDER BY created_at ASC
        """.trimIndent()

        return executeQuery(sql, mapOf("orderId" to orderId))
    }

    // Get items by product
    suspend fun findByProductId(
        productId: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<OrderItem> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE product_id = :productId 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "productId" to productId,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Get items by variant
    suspend fun findByVariantId(
        variantId: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<OrderItem> {
        val sql = """
            SELECT * FROM $tableName 
            WHERE variant_id = :variantId 
            ORDER BY created_at DESC 
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return executeQuery(
            sql, mapOf(
                "variantId" to variantId,
                "limit" to limit,
                "offset" to offset.toLong()
            )
        )
    }

    // Get order items with product details
    suspend fun findByOrderIdWithProductDetails(orderId: String): List<OrderItemWithProduct> {
        val sql = """
            SELECT oi.*, 
                   p.title as product_title,
                   p.slug as product_slug,
                   pv.sku as variant_sku,
                   pv.title as variant_title
            FROM $tableName oi
            LEFT JOIN products p ON oi.product_id = p.id
            LEFT JOIN product_variants pv ON oi.variant_id = pv.id
            WHERE oi.order_id = :orderId
            ORDER BY oi.created_at ASC
        """.trimIndent()

        return connectionFactory.useConnection {
            createStatement(sql)
                .bind("orderId", orderId)
                .execute()
                .awaitSingle()
                .map { row, rowMetadata ->
                    OrderItemWithProduct(
                        orderItem = rowMapper.apply(row, rowMetadata),
                        productTitle = row.get("product_title", String::class.java),
                        productSlug = row.get("product_slug", String::class.java),
                        variantSku = row.get("variant_sku", String::class.java),
                        variantTitle = row.get("variant_title", String::class.java)
                    )
                }
                .asFlow()
                .toList()
        }
    }

    // Get top selling products
    suspend fun getTopSellingProducts(
        limit: Int = 10,
        startDate: OffsetDateTime? = null,
        endDate: OffsetDateTime? = null
    ): List<TopSellingProduct> {
        val dateFilter = if (startDate != null && endDate != null) {
            "AND o.placed_at BETWEEN :startDate AND :endDate"
        } else ""

        val sql = """
            SELECT 
                oi.product_id,
                COUNT(DISTINCT oi.order_id) as order_count,
                SUM(oi.quantity) as total_quantity_sold,
                SUM(oi.total_price) as total_revenue
            FROM $tableName oi
            INNER JOIN orders o ON oi.order_id = o.id
            WHERE o.status NOT IN ('cancelled', 'refunded') 
            $dateFilter
            GROUP BY oi.product_id
            ORDER BY total_quantity_sold DESC 
            LIMIT :limit
        """.trimIndent()

        return connectionFactory.useConnection {
            val statement = createStatement(sql)
                .bind("limit", limit)

            if (startDate != null && endDate != null) {
                statement.bind("startDate", startDate)
                statement.bind("endDate", endDate)
            }

            statement.execute()
                .awaitSingle()
                .map { row, _ ->
                    TopSellingProduct(
                        productId = row.get("product_id", String::class.java)!!,
                        orderCount = row.get("order_count", Long::class.java)!!.toInt(),
                        totalQuantitySold = row.get("total_quantity_sold", Long::class.java)!!.toInt(),
                        totalRevenue = row.get("total_revenue", BigDecimal::class.java) ?: BigDecimal.ZERO
                    )
                }
                .asFlow()
                .toList()
        }
    }

    // Bulk create order items
    suspend fun bulkCreate(items: List<OrderItem>): List<OrderItem> {
        if (items.isEmpty()) return emptyList()

        val columns = listOf(
            "order_id", "product_id", "variant_id", "warehouse_id",
            "quantity", "unit_price", "total_price",
            "customization_snapshot", "product_snapshot"
        )

        val placeholders = List(items.size) { index ->
            "(${columns.joinToString(", ") { ":${it}_$index" }})"
        }

        val sql = """
            INSERT INTO $tableName (${columns.joinToString(", ")})
            VALUES ${placeholders.joinToString(", ")}
            RETURNING *
        """.trimIndent()

        return connectionFactory.withTransaction { connection ->
            val statement = connection.createStatement(sql)
            items.forEachIndexed { index, item ->
                statement.bind("order_id_$index", item.orderId)
                statement.bind("product_id_$index", item.productId)
                statement.bind("variant_id_$index", item.variantId)
                if (item.warehouseId != null) statement.bind(
                    "warehouse_id_$index",
                    item.warehouseId
                ) else statement.bind("warehouse_id_$index", String::class.java)
                statement.bind("quantity_$index", item.quantity)
                statement.bind("unit_price_$index", item.unitPrice)
                statement.bind("total_price_$index", item.totalPrice)
                if (item.customizationSnapshot != null) statement.bind(
                    "customization_snapshot_$index",
                    item.customizationSnapshot
                ) else statement.bind("customization_snapshot_$index", Map::class.java)
                statement.bind("product_snapshot_$index", item.productSnapshot)
            }

            statement.execute()
                .awaitSingle()
                .map(rowMapper)
                .asFlow()
                .toList()
        }
    }
}

data class OrderItemWithProduct(
    val orderItem: OrderItem,
    val productTitle: String?,
    val productSlug: String?,
    val variantSku: String?,
    val variantTitle: String?
)

data class TopSellingProduct(
    val productId: String,
    val orderCount: Int,
    val totalQuantitySold: Int,
    val totalRevenue: BigDecimal
)