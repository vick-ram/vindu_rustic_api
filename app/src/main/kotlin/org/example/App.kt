package org.example

import io.ktor.server.application.*
import kotlinx.coroutines.DelicateCoroutinesApi
import org.example.config.DynamicRouteFactory
import org.example.config.PluginRegistry
import org.example.config.configureOpenAPI
import org.example.di.AutoWireScanner
import org.example.di.databaseModule
import org.example.di.redisModule
import org.example.plugins.*
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

@OptIn(DelicateCoroutinesApi::class)
@Suppress("unused")
fun Application.module() {
    val factory = DynamicRouteFactory()

    install(Koin) {
        modules(databaseModule, redisModule)
    }

    val scanner = AutoWireScanner(
        this,
        listOf("org.example.data.repo", "org.example.data.mappers", "org.example.services", "org.example.data.cache")
    )
    getKoin().loadModules(scanner.scanAndCreateModules())

    PluginRegistry.register(
        LoggingModule,
        StatusPagesModule,
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


