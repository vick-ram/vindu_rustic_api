package org.example.data.db.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.example.domain.models.DimensionUnit
import org.example.domain.models.DiscountAppliedTo
import org.example.domain.models.DiscountType
import org.example.domain.models.MediaType
import org.example.domain.models.OfferType
import org.example.domain.models.OrderStatus
import org.example.domain.models.PaymentMethod
import org.example.domain.models.PaymentStatus
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    lateinit var datasource: HikariDataSource

    fun init(config: DatabaseConfig) {
        datasource = hikariDataSource(config)
        Database.connect(datasource).apply { createEnums() }
    }

    fun close() {
        datasource.close()
    }
}

data class DatabaseConfig(
    val dbPort: Int,
    val driver: String,
    val dbName: String,
    val user: String,
    val password: String,
    val poolSize: Int = 10,
    val connectionTimeout: Long = 30000,
    val idleTimeout: Long = 600000,
    val maxLifetime: Long = 1800000,
    val minimumIdle: Int = 5,
    val leakDetectionThreshold: Long = 60000,
    val cachePrepStmts: Boolean = true,
    val prepStmtCacheSize: Int = 250,
    val prepStmtCacheSqlLimit: Int = 2048,
    val useServerPrepStmts: Boolean = true
)

fun hikariDataSource(config: DatabaseConfig): HikariDataSource {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:${config.dbPort}/${config.dbName}"
        driverClassName = "org.postgresql.Driver"
        username = config.user
        password = config.password
        // Connection pool settings
        maximumPoolSize = config.poolSize
        minimumIdle = config.minimumIdle
        connectionTimeout = config.connectionTimeout
        idleTimeout = config.idleTimeout
        maxLifetime = config.maxLifetime
        leakDetectionThreshold = config.leakDetectionThreshold
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        // Performance optimizations
        addDataSourceProperty("cachePrepStmts", config.cachePrepStmts.toString())
        addDataSourceProperty("prepStmtCacheSize", config.prepStmtCacheSize.toString())
        addDataSourceProperty("prepStmtCacheSqlLimit", config.prepStmtCacheSqlLimit.toString())
        addDataSourceProperty("useServerPrepStmts", config.useServerPrepStmts.toString())
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

fun createEnums() {
    val enums = listOf(
        Pair("orderstatus", OrderStatus.entries.map { it.name }),
        Pair("mediatype", MediaType.entries.map { it.name }),
        Pair("offertype", OfferType.entries.map { it.name }),
        Pair("discounttype", DiscountType.entries.map { it.name }),
        Pair("discountappliedto", DiscountAppliedTo.entries.map { it.name }),
        Pair("paymentstatus", PaymentStatus.entries.map { it.name }),
        Pair("paymentmethod", PaymentMethod.entries.map { it.name }),
        Pair("dimensionunit", DimensionUnit.entries.map { it.name }),
    )
    transaction {
        enums.forEach { (typeName, values) ->
            val valuesList = values.joinToString(", ") { "'$it'" }
            exec(
                """
                DO $$
                BEGIN
                    CREATE TYPE $typeName AS ENUM ($valuesList);
                EXCEPTION
                    WHEN duplicate_object THEN null;
                END $$
            """.trimIndent()
            )
        }
    }
}