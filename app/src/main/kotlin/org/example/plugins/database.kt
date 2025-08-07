package org.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log
import org.example.data.db.DatabaseConfig
import org.example.data.db.DatabaseFactory
import org.example.data.db.tables.CategoryTable
import org.example.data.db.tables.ProductTable
import org.example.data.db.tables.UserTable
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.MigrationUtils
import java.io.File

const val MIGRATION_DIRECTORY = "app/src/main/resources/migrations"

suspend fun Application.configureDatabase() {
    val dbConfig = DatabaseConfig(
        dbPort = environment.config.property("database.port").getString().toInt(),
        driver = environment.config.property("database.driver").getString(),
        dbName = environment.config.property("database.db").getString(),
        user = environment.config.property("database.user").getString(),
        password = environment.config.property("database.password").getString(),
        poolSize = environment.config.property("database.poolSize").getString().toInt(),
    )

    try {
        //    Initialize connection pool
        DatabaseFactory.init(dbConfig)

        if (this.developmentMode) {
            transaction {
                val tables = arrayOf(UserTable, CategoryTable, ProductTable)
                val statements = SchemaUtils.addMissingColumnsStatements(tables = tables, withLogs = true)
                generateMigrationFile(UserTable, CategoryTable, ProductTable)
                statements.forEach { exec(it) }
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
    val dbUrl = "r2dbc:postgresql://localhost:${dbConfig.dbPort}/${dbConfig.dbName}"
    val flyaway = Flyway.configure()
        .dataSource(dbUrl, dbConfig.user, dbConfig.password)
        .locations("filesystem:$MIGRATION_DIRECTORY")
        .baselineOnMigrate(true)
        .driver(dbConfig.driver)
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