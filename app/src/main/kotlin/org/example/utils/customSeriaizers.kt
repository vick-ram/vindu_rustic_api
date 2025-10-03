package org.example.utils

import com.google.common.reflect.TypeToken
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
    ): JsonElement {
        return JsonPrimitive(formatter.format(p0))
    }

    override fun deserialize(
        p0: JsonElement,
        p1: Type?,
        p2: JsonDeserializationContext?
    ): LocalDateTime {
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
    ): JsonElement {
        return JsonPrimitive(formatter.format(p0))
    }

    override fun deserialize(
        p0: JsonElement?,
        p1: Type?,
        p2: JsonDeserializationContext?
    ): LocalDate {
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
    ): JsonElement {
        return JsonPrimitive(p0?.toPlainString())
    }

    override fun deserialize(
        p0: JsonElement?,
        p1: Type?,
        p2: JsonDeserializationContext?
    ): BigDecimal {
        return BigDecimal(p0?.asString)
    }
}

class MapTypeAdapter : JsonDeserializer<Map<String, Any>>, JsonSerializer<Map<String, Any>> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Map<String, Any> {
        return json.asJsonObject.entrySet().associate { (key, value) ->
            key to when {
                value.isJsonPrimitive -> value.asJsonPrimitive.let { primitive ->
                    when {
                        primitive.isBoolean -> primitive.asBoolean
                        primitive.isNumber -> {
                            val num = primitive.asNumber
                            // Try to preserve integer types
                            if (num.toDouble() == num.toLong().toDouble()) num.toLong() else num.toDouble()
                        }
                        else -> primitive.asString
                    }
                }
                value.isJsonObject -> context.deserialize<Map<String, Any>>(value, MAP_TYPE)
                value.isJsonArray -> context.deserialize<List<Any>>(value, LIST_TYPE)
                else -> null
            }
        }.filterValues { it != null }
            .mapValues { it.value!! }
    }

    override fun serialize(src: Map<String, Any>, typeOfT: Type, context: JsonSerializationContext): JsonElement {
        return context.serialize(src)
    }

    companion object {
        private val MAP_TYPE = object : TypeToken<Map<String, Any>>() {}.type
        private val LIST_TYPE = object : TypeToken<List<Any>>() {}.type
    }
}

object GsonFactory {
    val gson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(BigDecimal::class.java, BigDecimalAdapter())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .registerTypeAdapter(object : TypeToken<Map<String, Any>>() {}.type, MapTypeAdapter())
            .create()
    }
}
