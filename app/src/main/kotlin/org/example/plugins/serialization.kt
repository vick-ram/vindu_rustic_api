package org.example.plugins

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.datetime.LocalDateTime
import org.example.utils.BigDecimalAdapter
import org.example.utils.LocalDateTimeAdapter
import java.math.BigDecimal

fun Application.configureSerialization() {
    install(ContentNegotiation) {
//        gson(createGson())
        createGson()
    }
}


fun createGson(): Gson {
    return GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(BigDecimal::class.java, BigDecimalAdapter())
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()
}