package org.example.domain.models.content

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class Page(
    val id: String = shortUUID(),
    val title: String,
    val slug: String,
    val content: String? = null,

    @SerializedName("meta_title")
    val metaTitle: String? = null,

    @SerializedName("meta_description")
    val metaDescription: String? = null,

    val status: String = "draft",

    @SerializedName("created_by")
    val createdBy: String? = null,

    @SerializedName("published_at")
    val publishedAt: OffsetDateTime? = null,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
): Serializable
