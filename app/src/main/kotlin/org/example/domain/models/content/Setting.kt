package org.example.domain.models.content

import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.utils.MapStringAnySerializer
import org.example.utils.OffsetDateTimeSerializer
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class Setting(
    val id: String = Ulid.generate(),
    val key: String,

    @Contextual
    val value: Map<String, Any>,

    val description: String? = null,

    @Contextual
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
    @SerialName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {
    companion object {

        fun formParameters(parameters: Parameters): Setting {
            val key = parameters["key"].toString()
            val value = parameters.toMap()
            val description = parameters["description"].toString()

            return Setting(
                key = key,
                value = value.mapValues { it.value.first() },
                description = description
            )
        }

        val columns: List<Map<String, Any>>
            get() = listOf(
                mapOf("key" to "id", "label" to "ID"),
                mapOf("key" to "key", "label" to "Key", "sortable" to true),
                mapOf("key" to "value", "label" to "Value"),
                mapOf("key" to "description", "label" to "Description"),
                mapOf("key" to "created_at", "label" to "Created At", "sortable" to true),
                mapOf("key" to "updated_at", "label" to "Updated At", "sortable" to true)
            )

        fun toRows(settings: List<Setting>): List<Map<String, Any?>> {
            return settings.map { setting ->
                mapOf(
                    "id" to setting.id,
                    "key" to setting.key,
                    "value" to setting.value,
                    "description" to setting.description,
                    "created_at" to setting.createdAt,
                    "updated_at" to setting.updatedAt
                )
            }
        }

    }
}
