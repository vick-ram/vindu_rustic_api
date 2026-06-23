package org.example.data.db.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.example.config.AppConfig
import org.example.config.DatabaseConfig
import org.example.data.db.tables.Orders
import org.example.data.db.tables.ProductVariants
import org.example.data.db.tables.Users
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration

object DatabaseFactory {
    lateinit var datasource: HikariDataSource
    lateinit var db: Database

    lateinit var connectionFactory: ConnectionFactory

    fun init(config: AppConfig) {
        datasource = hikariDataSource(config)
        db = Database.connect(datasource)
        connectionFactory = createConnectionPool(config)
    }

    private fun createConnectionPool(config: AppConfig): ConnectionPool {
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

    fun hikariDataSource(config: AppConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://localhost:${config.database.dbPort}/${config.database.dbName}"
            driverClassName = "org.postgresql.Driver"
            username = config.database.user
            password = config.database.password
            // Connection pool settings
            maximumPoolSize = config.database.poolSize
            minimumIdle = config.database.minimumIdle
            connectionTimeout = config.database.connectionTimeout
            idleTimeout = config.database.idleTimeout
            maxLifetime = config.database.maxLifetime
            leakDetectionThreshold = config.database.leakDetectionThreshold
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            // Performance optimizations
            addDataSourceProperty("cachePrepStmts", config.database.cachePrepStmts.toString())
            addDataSourceProperty("prepStmtCacheSize", config.database.prepStmtCacheSize.toString())
            addDataSourceProperty("prepStmtCacheSqlLimit", config.database.prepStmtCacheSqlLimit.toString())
            addDataSourceProperty("useServerPrepStmts", config.database.useServerPrepStmts.toString())
            // Additional optimizations for PostgreSQL
            addDataSourceProperty("useLocalSessionState", "true")
            addDataSourceProperty("rewriteBatchedStatements", "true")
            addDataSourceProperty("cacheResultSetMetadata", "true")
            addDataSourceProperty("cacheServerConfiguration", "true")
            addDataSourceProperty("elideSetAutoCommits", "true")
            addDataSourceProperty("maintainTimeStats", "false")
            // Connection testing
            connectionTestQuery = "SELECT 1"
            // Enable metrics collection
            metricsTrackerFactory = null  // Default metrics tracker
            validate()
        }

        return HikariDataSource(hikariConfig)
    }

    fun createViews(dataSource: HikariDataSource) {
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

        dataSource.connection.use { conn ->
            conn.createStatement().use { statement ->
                statement.executeUpdate(lowStockAlert)
                statement.executeUpdate(orderFulfillmentSummary)
            }
        }
    }

    fun close() {
        datasource.close()
    }
}
