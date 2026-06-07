package org.example.domain.models.catalog

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class Tag(
    val id: String = shortUUID(),
    val name: String,
    val slug: String,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
): Serializable
