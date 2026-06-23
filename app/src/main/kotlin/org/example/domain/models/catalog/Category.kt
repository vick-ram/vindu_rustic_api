package org.example.domain.models.catalog

import io.ktor.http.content.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.OffsetDateTimeSerializer
import org.example.utils.saveMedia
import java.time.OffsetDateTime

@Serializable
data class Category(
    val id: String = Ulid.generate(),

    @SerialName(value = "parent_id")
    val parentId: String? = null,

    val name: String,
    val slug: String,
    val description: String? = null,

    @SerialName(value = "image_url")
    val imageUrl: String? = null,

    @SerialName(value = "is_active")
    val isActive: Boolean = false,

    @SerialName(value = "sort_order")
    val sortOrder: Int = 0,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) {
    companion object {

        suspend fun multipartFormData(multipart: MultiPartData): Category {
            var name: String? = null
            var slug: String? = null
            var description: String? = null
            var image: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "name" -> name = part.value
                            "slug" -> slug = part.value
                            "description" -> description = part.value
                        }
                    }

                    is PartData.FileItem -> {
                        if (part.name == "image") {
                            image = saveMedia("categories", part)
                        }
                    }

                    else -> {}
                }
                part.dispose()
            }
            return Category(
                name = name!!, slug = slug!!, description = description!!, imageUrl = image,
            )
        }

        val columns: List<Map<String, Any>> = listOf(
            mapOf("key" to "id", "label" to "id"),
            mapOf("key" to "name", "label" to "name", "sortable" to true),
            mapOf("key" to "slug", "label" to "slug", "sortable" to true),
            mapOf("key" to "description", "label" to "slug", "sortable" to true),
            mapOf("key" to "imageUrl", "label" to "imageUrl"),
            mapOf("key" to "isActive", "label" to "isActive", "sortable" to true),
            mapOf("key" to "createdAt", "label" to "createdAt", "sortable" to true)

        )

        fun toRow(categories: List<Category>): List<Map<String, Any?>> = categories.map { category ->
            mapOf(
                "id" to category.id,
                "name" to category.name,
                "slug" to category.slug,
                "description" to category.description,
                "imageUrl" to category.imageUrl,
                "isActive" to category.isActive,
                "createdAt" to category.createdAt
            )
        }
    }
}