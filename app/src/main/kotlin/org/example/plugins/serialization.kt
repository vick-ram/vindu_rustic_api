package org.example.plugins

import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.datetime.LocalDateTime
import org.example.utils.BigDecimalAdapter
import org.example.utils.LocalDateTimeAdapter
import java.math.BigDecimal

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            registerTypeAdapter(BigDecimal::class.java, BigDecimalAdapter())
            registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        }
    }
}