package org.example.plugins

import io.ktor.server.application.*
import org.example.data.db.config.DatabaseConfig
import org.example.data.db.config.DatabaseFactory
import org.example.data.db.config.TsVectorManager
import org.example.data.db.tables.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
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
                    DimensionTable,
                    OrderTable,
                    OrderItemTable,
                    AddressTable,
                    PaymentTable,
                    CartTable,
                    CartItemTable
                )

                // Create tables if they don't exist
                SchemaUtils.create(*tables)

                // Generate migration file
                if (environment.config.property("database.runMigrations").getString().toBoolean()) {
                    generateMigrationFile(*tables, withLogs = true)
                }

                // Create TSVECTOR triggers and populate data
                TsVectorManager.createAllTriggers()
                TsVectorManager.populateExistingData()
            }
        } else {
            // Run Flyway migrations (this will handle production)
            configureFlyaway(dbConfig)
        }
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
    val dbUrl = "jdbc:${dbConfig.driver}://localhost:${dbConfig.dbPort}/${dbConfig.dbName}"
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
private fun generateMigrationFile(vararg tables: Table, withLogs: Boolean) {
    val migrationDir = File(MIGRATION_DIRECTORY).apply { mkdirs() }

    // Find the next migration version
    val nextVersion = migrationDir.listFiles()
        ?.mapNotNull { Regex("""V(\d+)__""").find(it.name)?.groupValues?.get(1)?.toIntOrNull() }
        ?.maxOrNull()?.plus(1) ?: 1

    MigrationUtils.generateMigrationScript(
        tables = tables,
        scriptDirectory = MIGRATION_DIRECTORY,
        scriptName = "V${nextVersion}__auto_migration.sql",
        withLogs = withLogs
    )

    // Delete empty migration file
    val file = File(migrationDir, "V${nextVersion}__auto_migration.sql")
    if (file.exists() && file.readText().isBlank()) {
        file.delete()
    }
}
