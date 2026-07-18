package org.example.di

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.example.config.AppConfig
import org.koin.dsl.module
import java.time.Duration

val databaseModule = module {
    single<ConnectionFactory> {
        val config = get<AppConfig>()
        ConnectionPool(
            ConnectionPoolConfiguration.builder()
                .connectionFactory(
                    ConnectionFactories.get(
                        ConnectionFactoryOptions.builder()
                            .option(ConnectionFactoryOptions.DRIVER, "postgresql")
                            .option(ConnectionFactoryOptions.HOST, config.database.dbHost)
                            .option(ConnectionFactoryOptions.PORT, config.database.dbPort)
                            .option(ConnectionFactoryOptions.DATABASE, config.database.dbName)
                            .option(ConnectionFactoryOptions.USER, config.database.user)
                            .option(ConnectionFactoryOptions.PASSWORD, config.database.password)
                            .build()
                    )
                )
                .maxSize(config.database.poolSize)
                .maxIdleTime(Duration.ofMinutes(30))
                .validationQuery("SELECT 1")
                .build()
        )
    }
}