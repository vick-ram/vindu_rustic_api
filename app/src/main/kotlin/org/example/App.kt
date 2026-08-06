package org.example

import io.ktor.server.application.*
import org.example.config.AppConfig
import org.example.config.DynamicRouteFactory
import org.example.config.PluginRegistry
import org.example.config.configureOpenAPI
import org.example.di.AutoWireScanner
import org.example.di.celeryModule
import org.example.di.databaseModule
import org.example.di.redisModule
import org.example.plugins.*
import org.koin.dsl.module
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused")
fun Application.module() {
    val factory = DynamicRouteFactory()
    val appConfig = AppConfig.load(this)

    install(Koin) {
        modules(
            module { single<AppConfig> { appConfig } },
            redisModule,
            databaseModule,
            celeryModule
        )
    }

    val scanner = AutoWireScanner(
        this,
        listOf(
            "org.example.data.repo",
            "org.example.domain.repo",
            "org.example.data.mappers",
            "org.example.data.cache",
            "org.example.config.security",
            "org.example.services",
            "org.example.utils.notifications",
            "org.example.controllers.frontend",
            "org.example.controllers.frontend.admin",
            "org.example.controllers.frontend.ecommerce"
        )
    )
    getKoin().loadModules(scanner.scanAndCreateModules())

    PluginRegistry.register(
        LoggingModule,
        ErrorHandlingPlugin(),
        TemplateContextPlugin(),
        SerializationModule,
        DatabaseModule,
        SecurityModule,
        RoutingModule(factory),
        FrontendModule
    )

    PluginRegistry.installAll(this)

    try {
        configureOpenAPI(
            application = this,
            factory = factory,
        )
    } catch (e: Exception) {
        log.error("Failed to build OpenAPI YAML file at startup", e)
    }
}

