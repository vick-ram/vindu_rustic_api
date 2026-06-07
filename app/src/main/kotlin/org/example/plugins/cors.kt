package org.example.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import org.example.config.AppConfig
import org.example.config.ApplicationPlugin


object CorsModule : ApplicationPlugin {
    override fun install(application: Application) {
        val config = AppConfig.load(application)

        application.install(CORS) {
            config.cors.allowedMethods.forEach { method ->
                allowMethod(HttpMethod.parse(method))
            }
            config.cors.allowedHeaders.forEach { header ->
                allowHeader(header)
            }
            allowCredentials = config.cors.allowCredentials
            maxAgeInSeconds = config.cors.maxAgeSeconds
            anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
        }
    }
}