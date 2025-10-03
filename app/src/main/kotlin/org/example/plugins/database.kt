package org.example.plugins

import io.ktor.server.application.*
import org.example.data.db.config.DatabaseFactory
import org.example.data.db.config.TsVectorManager
import org.example.data.db.tables.AddressTable
import org.example.data.db.tables.CartItemTable
import org.example.data.db.tables.CartTable
import org.example.data.db.tables.CategoryTable
import org.example.data.db.tables.DimensionTable
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
import org.example.utils.DatabaseConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.MigrationUtils
import java.io.File

const val MIGRATION_DIRECTORY = "app/src/main/resources/migrations"

fun Application.configureDatabase(dbConfig: DatabaseConfig) {

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

    //    Initialize connection pool
    DatabaseFactory.init(dbConfig)

    if (this.developmentMode) {
        transaction {
            // Create tables if they don't exist
            SchemaUtils.create(*tables)

            // Generate migration file
            if (environment.config.property("database.runMigrations").getString().toBoolean()) {
                generateMigrationFile(*tables)
            }

            // Create TSVECTOR triggers and populate data
            TsVectorManager.createAllTriggers()
            TsVectorManager.populateExistingData()
        }
    } else {
        // Run Flyway migrations (this will handle production)
        configureFlyaway(dbConfig)
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
    val nextVersion = migrationDir.listFiles()
        ?.mapNotNull { Regex("""V(\d+)__""").find(it.name)?.groupValues?.get(1)?.toIntOrNull() }
        ?.maxOrNull()?.plus(1) ?: 1

    MigrationUtils.generateMigrationScript(
        tables = tables,
        scriptDirectory = MIGRATION_DIRECTORY,
        scriptName = "V${nextVersion}__auto_migration.sql"
    )

    // Delete empty migration file
    val file = File(migrationDir, "V${nextVersion}__auto_migration.sql")
    if (file.exists() && file.readText().isBlank()) {
        file.delete()
    }
}
