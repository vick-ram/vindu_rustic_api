package org.example.plugins

import io.ktor.server.application.*
import kotlinx.coroutines.launch
import org.example.config.AppConfig
import org.example.config.ApplicationPlugin
import org.example.data.db.config.CustomTable
import org.example.data.db.config.DatabaseFactory
import org.example.data.db.config.TsVectorManager
import org.example.data.db.tables.Roles
import org.example.data.db.tables.Users
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.migration.r2dbc.MigrationUtils
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import java.io.File

const val MIGRATION_DIRECTORY = "src/main/resources/migrations"

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
            application.monitor.subscribe(ApplicationStarted) { app ->
                app.launch {
                    val activeTables = CustomTable.tables

                    SchemaUtils.create(*activeTables)

                    // Generate migration file
                    if (config.database.runMigrations) {
                        generateMigrationFile(*tables)
                    }

                    // Create TSVECTOR triggers and populate data
                    TsVectorManager.setupFullTextSearch()
                }
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
private suspend fun generateMigrationFile(vararg tables: Table) {
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
