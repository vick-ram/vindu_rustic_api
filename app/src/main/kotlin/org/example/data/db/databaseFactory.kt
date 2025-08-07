package org.example.data.db

import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database

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
)

fun hikariDataSource(config: DatabaseConfig): HikariDataSource {
    return HikariDataSource().apply {
        jdbcUrl = "jdbc:${config.driver}://${config.dbName}:${config.dbPort}/${config.dbName}"
        username = config.user
        password = config.password
        maximumPoolSize = config.poolSize
        validate()
    }
}
