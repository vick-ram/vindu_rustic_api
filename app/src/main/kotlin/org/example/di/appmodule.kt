package org.example.di

import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDI() {
    install(Koin) {
        slf4jLogger()
        modules(
            applicationClassModule,
            configModule,
            serviceModule,
            roleModule,
            userModule,
            cartModule,
            orderModule,
            productModule,
            frontendModule
        )
        modules(
            module {
                single { this@configureDI } // binds Application instance
            }
        )
    }
}
