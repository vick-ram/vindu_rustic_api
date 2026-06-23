package org.example.plugins

import io.ktor.server.application.*
import org.example.config.AppConfig
import org.example.config.ApplicationPlugin
import org.example.data.db.config.DatabaseFactory
import org.example.data.db.config.TsVectorManager
import org.example.data.db.tables.Roles
import org.example.data.db.tables.Users
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.MigrationUtils
import java.io.File

const val MIGRATION_DIRECTORY = "app/src/main/resources/migrations"

object DatabaseModule : ApplicationPlugin {
    override fun install(application: Application) {
        val config = AppConfig.load(application)

        val tables = arrayOf(
            Users,
            Roles,

        )

        //    Initialize connection pool
        DatabaseFactory.init(config)

        if (config.server.development) {
            transaction {
                // Create tables if they don't exist
                SchemaUtils.create(*tables)

                // Generate migration file
                if (config.database.runMigrations) {
                    generateMigrationFile(*tables)
                }

                // Create TSVECTOR triggers and populate data
                TsVectorManager.setupFullTextSearch()
            }
        } else {
            // Run Flyway migrations (this will handle production)
            configureFlyaway(config)
        }
    }
}

private fun configureFlyaway(config: AppConfig) {
    val dbUrl = "jdbc:postgresql://localhost:${config.database.dbPort}/${config.database.dbName}"
    val flyaway = Flyway.configure()
        .dataSource(dbUrl, config.database.user, config.database.password)
        .locations("filesystem:$MIGRATION_DIRECTORY")
        .baselineOnMigrate(true)
        .load()

    try {
        flyaway.migrate()
    } catch (e: Exception) {
//        log.error("Failed to run database migration", e)
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
