package org.example.data.db.config

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import kotlinx.coroutines.reactive.awaitSingle
import org.example.config.AppConfig
import org.example.data.db.tables.Orders
import org.example.data.db.tables.ProductVariants
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import java.time.Duration

@Module
object DatabaseFactory {

    @Single
    private fun connectionFactory(config: AppConfig): ConnectionPool {
        return ConnectionPool(
            ConnectionPoolConfiguration.builder()
                .connectionFactory(
                    ConnectionFactories.get(
                        ConnectionFactoryOptions.builder()
                            .option(ConnectionFactoryOptions.DRIVER, "postgresql")
                            .option(ConnectionFactoryOptions.HOST, config.database.dbHost)
                            .option(ConnectionFactoryOptions.PORT, config.database.dbPort)
                            .option(ConnectionFactoryOptions.DATABASE, config.database.dbName)
                            .option(ConnectionFactoryOptions.USER, config.database.user)
                            .option(ConnectionFactoryOptions.PASSWORD, config.database.password)
                            .build()
                    )
                )
                .maxSize(config.database.poolSize)
                .maxIdleTime(Duration.ofMinutes(30))
                .validationQuery("SELECT 1")
                .build()
        )
    }

    suspend fun createViews(connection: Connection) {
        val lowStockAlert = """
                CREATE OR REPLACE VIEW low_stock_products AS
                SELECT
                    pv.variant_id,
                    pv.sku,
                    p.title AS product_title,
                    pv.title AS variant_title,
                    COALESCE(SUM(i.available_quantity), 0) AS total_available,
                    COALESCE(SUM(i.reserved_quantity), 0) AS total_reserved,
                    pv.quantity_in_stock
                FROM ${ProductVariants.tableName} pv
                JOIN products p ON p.product_id = pv.product_id
                LEFT JOIN inventory i ON i.variant_id = pv.variant_id
                WHERE pv.is_active = TRUE AND p.deleted_at IS NULL
                GROUP BY pv.variant_id, pv.sku, p.title, pv.title, pv.quantity_in_stock
                HAVING COALESCE(SUM(i.available_quantity), 0) <= pv.quantity_in_stock * 0.2;
            """.trimIndent()

        val orderFulfillmentSummary = """
        CREATE OR REPLACE VIEW pending_fulfillment AS
        SELECT 
            o.order_id,
            o.order_number,
            o.status,
            o.fulfillment_status,
            COUNT(oi.order_item_id) AS total_items,
            COUNT(pj.job_id) FILTER (WHERE pj.status = 'QUEUED') AS queued_jobs,
            COUNT(pj.job_id) FILTER (WHERE pj.status = 'IN_PROGRESS') AS in_progress_jobs,
            COUNT(pj.job_id) FILTER (WHERE pj.status = 'COMPLETED') AS completed_jobs,
            o.placed_at
        FROM ${Orders.tableName} o
        JOIN order_items oi ON oi.order_id = o.order_id
        LEFT JOIN production_jobs pj ON pj.order_item_id = oi.order_item_id
        WHERE o.fulfillment_status IN ('UNFULFILLED', 'PARTIAL')
        GROUP BY o.order_id, o.order_number, o.status, o.fulfillment_status, o.placed_at;
    """.trimIndent()

        connection.createStatement(lowStockAlert).execute().awaitSingle().rowsUpdated.awaitSingle()
        connection.createStatement(orderFulfillmentSummary).execute().awaitSingle().rowsUpdated.awaitSingle()
    }
}
