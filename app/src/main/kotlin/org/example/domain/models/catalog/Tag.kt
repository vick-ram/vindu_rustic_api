package org.example.domain.models.catalog

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class Tag(
    val id: String = Ulid.generate(),
    val name: String,
    val slug: String,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
    companion object {
        fun formParameters(parameters: Parameters) : Tag {
            val name = parameters["name"].toString()
            val slug = parameters["slug"].toString()

            return Tag(
                name = name,
                slug = slug
            )
        }
    }
}
