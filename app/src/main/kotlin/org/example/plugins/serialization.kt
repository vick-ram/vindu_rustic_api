package org.example.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.example.config.ApplicationPlugin
import org.example.utils.InetAddressSerializer
import org.example.utils.OffsetDateTimeSerializer

object SerializationModule : ApplicationPlugin {
    override fun install(application: Application) {
        application.install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                serializersModule = SerializersModule {
                    contextual(OffsetDateTimeSerializer)
                    contextual(InetAddressSerializer)
                }
            })
        }
    }
}