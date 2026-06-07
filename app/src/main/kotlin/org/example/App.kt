package org.example

import io.ktor.server.application.*
import kotlinx.coroutines.DelicateCoroutinesApi
import org.example.config.PluginRegistry
import org.example.config.configureOpenAPI
import org.example.di.configureDI
import org.example.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

@OptIn(DelicateCoroutinesApi::class)
@Suppress("unused")
fun Application.module() {
    configureDI()

    PluginRegistry.register(
        LoggingModule,
        StatusPagesModule,
        SerializationModule,
        DatabaseModule,
        SecurityModule,
        CorsModule,
        RoutingModule,
        FrontendModule
    )
    configureOpenAPI()

    PluginRegistry.installAll(this)
}


