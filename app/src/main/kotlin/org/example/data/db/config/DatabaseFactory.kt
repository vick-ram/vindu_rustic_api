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
import org.example.data.db.tables.Users
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration

object DatabaseFactory {
    lateinit var datasource: HikariDataSource
    lateinit var db: Database

    fun init(config: AppConfig) {
        datasource = hikariDataSource(config)
        db = Database.connect(datasource).apply { createEnums() } //Postgres requires u to reate enums before u assign it as a type
    }

    fun close() {
        datasource.close()
    }
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

fun createEnums() {
//    transaction {
//        enums.forEach { (typeName, values) ->
//            val valuesList = values.joinToString(", ") { "'$it'" }
//            exec(
//                """
//                DO $$
//                BEGIN
//                    CREATE TYPE $typeName AS ENUM ($valuesList);
//                EXCEPTION
//                    WHEN duplicate_object THEN null;
//                END $$
//            """.trimIndent()
//            )
//        }
//    }
}

//class DatabaseFactory(
//    private val config: DatabaseConfig
//) {
//    // JDBC - For Exposed DSL and complex operations
//    val jdbcDataSource: HikariDataSource by lazy {
//        createHikariDataSource()
//    }
//
//    val jdbcDatabase: Database by lazy {
//        Database.connect(jdbcDataSource).apply {
//            // Setup schema on connection
//            TransactionManager.manager.defaultIsolationLevel =
//                java.sql.Connection.TRANSACTION_READ_COMMITTED
//            setupSchema()
//        }
//    }
//
//    // R2DBC - For reactive operations
//    val r2dbcConnectionFactory: ConnectionFactory by lazy {
//        createR2dbcConnectionFactory()
//    }
//
//    val r2dbcPool: ConnectionPool by lazy {
//        ConnectionPool(
//            ConnectionPoolConfiguration.builder()
//                .connectionFactory(r2dbcConnectionFactory)
//                .maxSize(config.poolSize)
//                .maxIdleTime(Duration.ofMinutes(30))
//                .validationQuery(if (config.isDevMode) "SELECT 1" else "SELECT 1")
//                .build()
//        )
//    }
//
//    private fun createHikariDataSource(): HikariDataSource {
//        return HikariDataSource(HikariConfig().apply {
//            jdbcUrl = config.jdbcUrl
//            username = config.user
//            password = config.password
//            driverClassName = when {
//                config.driver == "h2" -> "org.h2.Driver"
//                else -> "org.postgresql.Driver"
//            }
//            maximumPoolSize = config.poolSize
//            minimumIdle = 5
//            idleTimeout = 30000
//            connectionTimeout = 10000
//            maxLifetime = 1800000
//
//            // H2 specific
//            if (config.driver == "h2") {
//                addDataSourceProperty("MODE", "PostgreSQL")
//                addDataSourceProperty("DB_CLOSE_DELAY", "-1")
//            }
//        })
//    }
//
//    private fun createR2dbcConnectionFactory(): ConnectionFactory {
//        return if (config.driver == "h2") {
//            // H2 R2DBC
//            ConnectionFactories.get(
//                ConnectionFactoryOptions.builder()
//                    .option(ConnectionFactoryOptions.DRIVER, "h2")
//                    .option(ConnectionFactoryOptions.PROTOCOL, "mem")
//                    .option(ConnectionFactoryOptions.DATABASE, config.dbName)
//                    .option(ConnectionFactoryOptions.HOST, "")
//                    .build()
//            )
//        } else {
//            // PostgreSQL R2DBC
//            ConnectionFactories.get(
//                ConnectionFactoryOptions.builder()
//                    .option(ConnectionFactoryOptions.DRIVER, "postgresql")
//                    .option(ConnectionFactoryOptions.HOST, config.dbHost)
//                    .option(ConnectionFactoryOptions.PORT, config.dbPort)
//                    .option(ConnectionFactoryOptions.USER, config.user)
//                    .option(ConnectionFactoryOptions.PASSWORD, config.password)
//                    .option(ConnectionFactoryOptions.DATABASE, config.dbName)
//                    .build()
//            )
//        }
//    }
//
//    private fun setupSchema() {
//        // Only for H2 - auto create schema
//        if (config.driver == "h2") {
//            transaction(jdbcDatabase) {
//                SchemaUtils.create(
//                    Users
//                )
//            }
//        }
//    }
//
//    fun close() {
//        jdbcDataSource.close()
//        r2dbcPool.dispose()
//    }
//}