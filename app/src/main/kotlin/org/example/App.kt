package org.example

import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.di.configureDI
import org.example.plugins.appHttpClient
import org.example.plugins.configureCors
import org.example.plugins.configureDatabase
import org.example.plugins.configureFirebase
import org.example.plugins.configureFrontend
import org.example.plugins.configureRouteLogging
import org.example.plugins.configureRouting
import org.example.plugins.configureSecurity
import org.example.plugins.configureSerialization
import org.example.plugins.configureStatusPages
import org.example.plugins.configureWebjar
import org.example.utils.cleanOldCache
import java.io.File
import kotlin.time.Duration.Companion.hours

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module(httpClient: HttpClient = appHttpClient) {
    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val realm = environment.config.property("jwt.realm").getString()
    val clientID = environment.config.property("google.clientID").getString()
    val clientSecret = environment.config.property("google.clientSecret").getString()

    configureStatusPages()
    configureDatabase()
    configureDI()
    configureCors()
    configureSerialization()
    configureWebjar()
    configureRouteLogging()
    configureSecurity(
        realm,
        secret,
        issuer,
        audience,
        httpClient,
        clientID,
        clientSecret,
    )
    configureRouting(issuer, audience, secret)
    configureFirebase()
    configureFrontend()

    val cleanJob = launch {
        while (true) {
            delay(24.hours)
            cleanOldCache(File("build/cache"), 7)
        }
    }

    monitor.subscribe(ApplicationStopped) {
        cleanJob.cancel()
    }
}
