package org.example.plugins

import io.ktor.server.application.*
import org.example.config.AppConfig
import org.example.config.ApplicationPlugin
import org.flywaydb.core.Flyway

object DatabaseModule : ApplicationPlugin {
    override fun install(application: Application) {
        val config = AppConfig.load(application)
        // Generate migration file
        if (config.database.runMigrations) {
            runFlywayMigrations(config)
        }
    }
}

private fun runFlywayMigrations(config: AppConfig) {
    val dbUrl = "jdbc:postgresql://localhost:${config.database.dbPort}/${config.database.dbName}"
    val flyaway = Flyway.configure()
        .dataSource(dbUrl, config.database.user, config.database.password)
        .locations("classpath:migrations")
        .baselineOnMigrate(true)
        .load()

    try {
        flyaway.migrate()
    } catch (e: Exception) {
        throw e
    }
}
