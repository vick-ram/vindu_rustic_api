package org.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log
import org.example.data.db.DatabaseConfig
import org.example.data.db.DatabaseFactory
import org.example.data.db.tables.AddressTable
import org.example.data.db.tables.CartItemTable
import org.example.data.db.tables.CartTable
import org.example.data.db.tables.CategoryTable
import org.example.data.db.tables.DiscountTable
import org.example.data.db.tables.MediaTable
import org.example.data.db.tables.OrderItemTable
import org.example.data.db.tables.OrderTable
import org.example.data.db.tables.PaymentTable
import org.example.data.db.tables.PermissionTable
import org.example.data.db.tables.ProductReviewTable
import org.example.data.db.tables.ProductTable
import org.example.data.db.tables.RolePermissionTable
import org.example.data.db.tables.RoleTable
import org.example.data.db.tables.SpecialOfferProductTable
import org.example.data.db.tables.SpecialOfferTable
import org.example.data.db.tables.UserTable
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.MigrationUtils
import java.io.File

const val MIGRATION_DIRECTORY = "app/src/main/resources/migrations"

fun Application.configureDatabase() {
    val dbConfig = DatabaseConfig(
        dbPort = environment.config.property("database.port").getString().toInt(),
        driver = environment.config.property("database.driver").getString(),
        dbName = environment.config.property("database.db").getString(),
        user = environment.config.property("database.user").getString(),
        password = environment.config.property("database.password").getString(),
        poolSize = environment.config.property("database.poolSize").getString().toInt(),
        connectionTimeout = environment.config.propertyOrNull("database.connectionTimeout")?.getString()?.toLong()
            ?: 30000,
        idleTimeout = environment.config.propertyOrNull("database.idleTimeout")?.getString()?.toLong() ?: 600000,
        maxLifetime = environment.config.propertyOrNull("database.maxLifetime")?.getString()?.toLong() ?: 1800000,
        minimumIdle = environment.config.propertyOrNull("database.minimumIdle")?.getString()?.toInt() ?: 5,
        leakDetectionThreshold = environment.config.propertyOrNull("database.leakDetectionThreshold")?.getString()
            ?.toLong() ?: 60000,
        cachePrepStmts = environment.config.propertyOrNull("database.cachePrepStmts")?.getString()?.toBoolean() ?: true,
        prepStmtCacheSize = environment.config.propertyOrNull("database.prepStmtCacheSize")?.getString()?.toInt()
            ?: 250,
        prepStmtCacheSqlLimit = environment.config.propertyOrNull("database.prepStmtCacheSqlLimit")?.getString()
            ?.toInt() ?: 2048,
        useServerPrepStmts = environment.config.propertyOrNull("database.useServerPrepStmts")?.getString()?.toBoolean()
            ?: true
    )

    try {
        //    Initialize connection pool
        DatabaseFactory.init(dbConfig)

        if (this.developmentMode) {
            transaction {
                val tables = arrayOf(
                    UserTable,
                    RoleTable,
                    RolePermissionTable,
                    PermissionTable,
                    CategoryTable,
                    ProductTable,
                    MediaTable,
                    DiscountTable,
                    SpecialOfferTable,
                    SpecialOfferProductTable,
                    ProductReviewTable,
                    OrderTable,
                    OrderItemTable,
                    AddressTable,
                    PaymentTable,
                    CartTable,
                    CartItemTable
                )
                tables.filter { it.exists() }.forEach { table ->
                    val statements = SchemaUtils.addMissingColumnsStatements(tables = tables, withLogs = true)
                    generateMigrationFile(tables = tables)
                    statements.forEach { exec(it) }
                }
            }
        }

        // Initialize migration
        configureFlyaway(dbConfig)
    } catch (e: Exception) {
        log.error("Failed to initialize database", e)
        throw e
    } finally {
        //    Register shutdown hook
        monitor.subscribe(ApplicationStopping) {
            DatabaseFactory.close()
        }
    }
}

private fun Application.configureFlyaway(dbConfig: DatabaseConfig) {
    val dbUrl = "jdbc:postgresql://localhost:${dbConfig.dbPort}/${dbConfig.dbName}"
    val flyaway = Flyway.configure()
        .dataSource(dbUrl, dbConfig.user, dbConfig.password)
        .locations("filesystem:$MIGRATION_DIRECTORY")
        .baselineOnMigrate(true)
        .load()

    try {
        flyaway.migrate()
    } catch (e: Exception) {
        log.error("Failed to run database migration", e)
        throw e
    }
}

@OptIn(ExperimentalDatabaseMigrationApi::class)
private fun generateMigrationFile(vararg tables: Table) {
    val migrationDir = File(MIGRATION_DIRECTORY).apply { mkdirs() }

    // Find the next migration version
    val nextVersion = migrationDir.listFiles()?.maxOfOrNull { it.name.substring(1, 2).toInt() }?.plus(1) ?: 1

    MigrationUtils.generateMigrationScript(
        tables = tables,
        scriptDirectory = MIGRATION_DIRECTORY,
        scriptName = "V${nextVersion}__auto_migration.sql"
    )
}