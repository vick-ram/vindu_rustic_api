package org.example.data.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import java.util.concurrent.TimeUnit

object DatabaseFactory {
    lateinit var datasource: HikariDataSource

    fun init(config: DatabaseConfig) {
        datasource = hikariDataSource(config)
        Database.connect(datasource)
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
        jdbcUrl = "jdbc:${config.driver}://localhost:${config.dbPort}/${config.dbName}"
        driverClassName = when(config.driver) {
            "postgresql" -> "org.postgresql.Driver"
            else -> throw IllegalArgumentException("Unsupported driver: ${config.driver}")
        }
        
        // Log connection attempts for debugging
        println("Initializing HikariCP with jdbcUrl: $jdbcUrl")
        username = config.user
        password = config.password
        
        // Connection pool settings
        maximumPoolSize = config.poolSize
        minimumIdle = config.minimumIdle
        connectionTimeout = config.connectionTimeout
        idleTimeout = config.idleTimeout
        maxLifetime = config.maxLifetime
        leakDetectionThreshold = config.leakDetectionThreshold
        
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
