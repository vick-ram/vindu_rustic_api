package org.example

import io.ktor.client.*
import io.ktor.server.application.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.example.di.configureDI
import org.example.plugins.*
import org.example.utils.RedisService
import org.example.utils.cleanOldCache
import java.io.File
import kotlin.time.Duration.Companion.hours

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

@OptIn(DelicateCoroutinesApi::class)
@Suppress("unused")
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
    configureWebSockets()
    configureRouting(issuer, audience, secret)
//    configureFirebase()
    configureFrontend()

    val cleanJob = launch {
        while (true) {
            delay(24.hours)
            cleanOldCache(File("build/cache"), 7)
        }
    }

    GlobalScope.launch {
        val redisRunning = RedisService.healthCheck()
        if (redisRunning) {
            this@module.environment.log.info("Redis is running")
        } else {
            this@module.environment.log.info("Redis is not running...")
        }
    }

    monitor.subscribe(ApplicationStopped) {
        cleanJob.cancel()
    }

}

