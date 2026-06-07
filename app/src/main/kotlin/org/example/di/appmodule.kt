package org.example.di

import io.ktor.server.application.*
import org.example.config.AppConfig
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDI() {
    val config = AppConfig.load(this)

    val allModules = listOf(
        applicationClassModule,
        redisModule,
        serviceModule,
        roleModule,
        userModule,
        cartModule,
        orderModule,
        productModule,
        frontendModule
    )

    install(Koin) {
        slf4jLogger()

        modules(
            module {
                single { config }
                single { config.database }
                single { config.security }
                single { config.cors }
                single { config.logging }
                single { config.email }
                single { config.fcm }
            },
            *allModules.toTypedArray(),
            module {
                single { this@configureDI } // binds Application instance
            }
        )
    }
}
