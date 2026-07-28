package org.example.domain.models.content

import io.ktor.http.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class Page(
    val id: String = Ulid.generate(),
    val title: String,
    val slug: String,
    val content: String? = null,

    @SerialName("meta_title")
    val metaTitle: String? = null,

    @SerialName("meta_description")
    val metaDescription: String? = null,

    @OneOf("draft", "published", "archived")
    val status: String = "draft",

    @SerialName("created_by")
    val createdBy: String? = null,

    @Contextual
    @SerialName("published_at")
    val publishedAt: OffsetDateTime? = null,

    @Contextual
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
    @SerialName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {
    companion object {

        fun formParameters(parameters: Parameters): Page {
            val title = parameters["title"].toString()
            val slug = parameters["slug"].toString()
            val content = parameters["content"].toString()

            return Page(
                title = title,
                slug = slug,
                content = content
            )
        }

        val columns: List<Map<String, Any>>
            get() = listOf(
                mapOf("key" to "id", "label" to "ID"),
                mapOf("key" to "title", "label" to "Title", "sortable" to true),
                mapOf("key" to "slug", "label" to "Slug", "sortable" to true),
                mapOf("key" to "status", "label" to "Status", "sortable" to true),
                mapOf("key" to "published_at", "label" to "Published At", "sortable" to true),
                mapOf("key" to "created_at", "label" to "Created At", "sortable" to true)
            )

        fun toRows(pages: List<Page>): List<Map<String, Any?>> {
            return pages.map { page ->
                mapOf(
                    "id" to page.id,
                    "title" to page.title,
                    "slug" to page.slug,
                    "content" to page.content,
                    "meta_title" to page.metaTitle,
                    "meta_description" to page.metaDescription,
                    "status" to page.status,
                    "created_by" to page.createdBy,
                    "published_at" to page.publishedAt,
                    "created_at" to page.createdAt,
                    "updated_at" to page.updatedAt
                )
            }
        }
    }
}
