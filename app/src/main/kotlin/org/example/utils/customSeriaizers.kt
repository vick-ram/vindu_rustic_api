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
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import io.grpc.internal.ReadableBuffers.readArray
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.io.IOException
import java.lang.reflect.Type
import java.math.BigDecimal
import java.net.InetAddress
import java.time.OffsetDateTime

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

class OffsetDateTimeAdapter : JsonSerializer<OffsetDateTime>, JsonDeserializer<OffsetDateTime> {
    override fun serialize(
        p0: OffsetDateTime,
        p1: Type?,
        p2: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(p0.toString())
    }

    override fun deserialize(
        p0: JsonElement,
        p1: Type?,
        p2: JsonDeserializationContext?
    ): OffsetDateTime {
        try {
            return OffsetDateTime.parse(p0.asString)
        } catch (e: Exception) {
            throw JsonParseException("Failed to parse OffsetDateTime: ${p0.asString}", e)
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

class MapStringAnyAdapter : TypeAdapter<Map<String, Any>>() {

    @Throws(IOException::class)
    override fun write(
        out: JsonWriter?,
        value: Map<String, Any>?
    ) {
        if (value == null) {
            out?.nullValue()
            return
        }
        out?.beginObject()
        for ((key, fieldValue) in value) {
            out?.name(key)
            writeElement(out, fieldValue)
        }
        out?.endObject()
    }

    private fun writeElement(out: JsonWriter?, value: Any?) {
        when (value) {
            null -> out?.nullValue()
            is Boolean -> out?.value(value)
            is Number -> out?.value(value)
            is String -> out?.value(value)
            is Map<*, *> -> {
                out?.beginObject()
                for ((k, v) in value) {
                    out?.name(k.toString())
                    writeElement(out, v)
                }
                out?.endObject()
            }
            is List<*> -> {
                out?.beginArray()
                for (item in value) {
                    writeElement(out, item)
                }
                out?.endArray()
            }
            else -> out?.value(value.toString())
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader?): Map<String, Any>? {
        if (`in`?.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        return readObject(`in`)
    }

    private fun readObject(reader: JsonReader?): Map<String, Any> {
        val map = LinkedHashMap<String, Any>()
        reader?.beginObject()
        while (reader?.hasNext() == true) {
            val key = reader.nextName()
            val value = readElement(reader)
            if (value != null) {
                map[key] = value
            }
        }
        reader?.endObject()
        return map
    }

    private fun readElement(reader: JsonReader?): Any? {
        return when (reader?.peek()) {
            JsonToken.BEGIN_OBJECT -> readObject(reader)
            JsonToken.BEGIN_ARRAY -> {
                val list = ArrayList<Any?>()
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(readElement(reader))
                }
                reader.endArray()
                list
            }

            JsonToken.STRING -> reader.nextString()
            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NUMBER -> {
                val numberStr = reader.nextString()
                if (numberStr.contains(".") || numberStr.contains("e") || numberStr.contains("E")) {
                    numberStr.toDouble()
                } else {
                    numberStr.toLong().let { if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else it }
                }
            }

            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            else -> throw IllegalStateException("Unexpected token type: ${reader?.peek()}")
        }
    }
}

class InetAddressAdapter: TypeAdapter<InetAddress>() {
    override fun write(out: JsonWriter?, value: InetAddress?) {
        if (value == null) {
            out?.nullValue()
        } else {
            out?.value(value.hostAddress)
        }
    }

    override fun read(`in`: JsonReader?): InetAddress? {
        if (`in`?.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        return InetAddress.getByName(`in`?.nextString())
    }
}

object GsonFactory {
    val mapType = object : TypeToken<Map<String, Any>>() {}.type
    val gson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .registerTypeAdapter(BigDecimal::class.java, BigDecimalAdapter())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .registerTypeAdapter(InetAddress::class.java, InetAddressAdapter())
            .registerTypeAdapter(mapType, MapStringAnyAdapter())
            .create()
    }
}
