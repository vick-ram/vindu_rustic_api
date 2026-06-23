package org.example.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.math.BigDecimal
import java.net.InetAddress
import java.time.OffsetDateTime

object OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("OffsetDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: OffsetDateTime) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): OffsetDateTime {
        return OffsetDateTime.parse(decoder.decodeString())
    }
}

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("java.math.BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }
}

object MapStringAnySerializer : KSerializer<Map<String, Any>> {
    private val delegateSerializer = MapSerializer(String.serializer(), JsonElement.serializer())

    override val descriptor: SerialDescriptor
        get() = delegateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: Map<String, Any>
    ) {
        val jsonMap = value.mapValues { (_, item) -> item.toJsonElement()  }
        encoder.encodeSerializableValue(delegateSerializer, jsonMap)
    }

    override fun deserialize(decoder: Decoder): Map<String, Any> {
        // Decode into Map<String, JsonElement> and convert back to Map<String, Any>
        val jsonMap = decoder.decodeSerializableValue(delegateSerializer)
        return jsonMap.mapValues { (_, jsonElement) -> jsonElement.toNativeAny() as Any }
    }

    // Helper: Recursively convert native types to JsonElements
    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is JsonElement -> this
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Map<*, *> -> JsonObject(this.entries.associate { it.key.toString() to it.value.toJsonElement() })
        is List<*> -> JsonArray(this.map { it.toJsonElement() })
        else -> JsonPrimitive(this.toString()) // Fallback for unhandled types
    }

    // Helper: Recursively convert JsonElements back to primitive Kotlin types
    private fun JsonElement.toNativeAny(): Any? = when (this) {
        is JsonNull -> null
        is JsonPrimitive -> {
            if (this.isString) {
                this.content
            } else {
                this.booleanOrNull ?: this.longOrNull ?: this.doubleOrNull ?: this.content
            }
        }
        is JsonObject -> this.mapValues { it.value.toNativeAny() }
        is JsonArray -> this.map { it.toNativeAny() }
    }
}

object InetAddressSerializer: KSerializer<InetAddress> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("java.net.InetAddress", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: InetAddress) {
        encoder.encodeString(value.hostAddress)
    }

    override fun deserialize(decoder: Decoder): InetAddress {
        return InetAddress.getByName(decoder.decodeString())
    }
}

