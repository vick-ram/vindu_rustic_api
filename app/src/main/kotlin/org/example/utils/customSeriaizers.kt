package org.example.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.lang.reflect.Type
import java.math.BigDecimal

class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    private val formatter = LocalDateTime.Formats.ISO

    override fun serialize(
        p0: LocalDateTime,
        p1: Type?,
        p2: JsonSerializationContext?
    ): JsonElement? {
        return JsonPrimitive(formatter.format(p0))
    }

    override fun deserialize(
        p0: JsonElement,
        p1: Type?,
        p2: JsonDeserializationContext?
    ): LocalDateTime? {
        try {
            val dateString = p0.asString
            return formatter.parse(dateString)
        } catch (e: JsonParseException) {
            throw JsonParseException("Failed to parse LocalDateTime: ${p0?.asString}", e)
        }
    }
}

class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    private val formatter = LocalDate.Formats.ISO

    override fun serialize(
        p0: LocalDate,
        p1: Type?,
        p2: JsonSerializationContext?
    ): JsonElement? {
        return JsonPrimitive(formatter.format(p0))
    }

    override fun deserialize(
        p0: JsonElement?,
        p1: Type?,
        p2: JsonDeserializationContext?
    ): LocalDate? {
        try {
            val dateString = p0?.asString!!
            return formatter.parse(dateString)
        } catch (e: JsonParseException) {
            throw JsonParseException("Failed to parse LocalDateTime: ${p0?.asString}", e)
        }
    }
}

class BigDecimalAdapter : JsonSerializer<BigDecimal>, JsonDeserializer<BigDecimal> {
    override fun serialize(
        p0: BigDecimal?,
        p1: Type?,
        p2: JsonSerializationContext?
    ): JsonElement? {
        return JsonPrimitive(p0?.toPlainString())
    }

    override fun deserialize(
        p0: JsonElement?,
        p1: Type?,
        p2: JsonDeserializationContext?
    ): BigDecimal? {
        return BigDecimal(p0?.asString)
    }
}



object GsonFactory {
    val gson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(BigDecimal::class.java, BigDecimalAdapter())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .create()
    }
}
