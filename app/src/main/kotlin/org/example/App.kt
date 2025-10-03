package org.example

import io.ktor.client.*
import io.ktor.server.application.*
import kotlinx.coroutines.DelicateCoroutinesApi
import org.example.di.configureDI
import org.example.plugins.*
import org.example.utils.DatabaseConfig
import org.example.utils.SecurityConfig
import org.koin.ktor.ext.inject

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

@OptIn(DelicateCoroutinesApi::class)
@Suppress("unused")
fun Application.module(httpClient: HttpClient = appHttpClient) {

    configureDI()

    val dbConfig by inject<DatabaseConfig>()
    val securityConfig by inject<SecurityConfig>()

    configureStatusPages()
    configureDatabase(dbConfig)
    configureCors()
    configureSerialization()
    configureWebjar()
    configureRouteLogging()
    configureSecurity(securityConfig, httpClient)
    configureWebSockets()
    configureRouting(securityConfig)
    configureFrontend()
}

