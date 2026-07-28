package org.example.domain.models.catalog

import io.ktor.http.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.utils.OffsetDateTimeSerializer
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class Tag(
    val id: String = Ulid.generate(),
    val name: String,
    val slug: String,

    @Contextual
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
