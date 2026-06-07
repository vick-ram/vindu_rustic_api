package org.example.plugins

import com.google.common.reflect.TypeToken
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.example.config.ApplicationPlugin
import org.example.utils.BigDecimalAdapter
import org.example.utils.GsonFactory
import org.example.utils.LocalDateAdapter
import org.example.utils.LocalDateTimeAdapter
import org.example.utils.MapTypeAdapter
import java.math.BigDecimal

object SerializationModule : ApplicationPlugin {
    override fun install(application: Application) {
        application.install(ContentNegotiation) {
            gson {
//                serializeNulls()
//                setPrettyPrinting()
//                registerTypeAdapter(BigDecimal::class.java, BigDecimalAdapter())
//                registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
//                registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
//                registerTypeAdapter(object : TypeToken<Map<String, Any>>() {}.type, MapTypeAdapter())
                GsonFactory.gson
            }
        }
    }
}