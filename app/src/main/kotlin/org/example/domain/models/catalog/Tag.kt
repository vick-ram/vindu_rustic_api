package org.example.domain.models.catalog

import com.google.gson.annotations.SerializedName
import io.ktor.http.Parameters
import org.example.data.db.config.Ulid
import java.io.Serializable
import java.time.OffsetDateTime

data class Tag(
    val id: String = Ulid.generate(),
    val name: String,
    val slug: String,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
): Serializable {
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
