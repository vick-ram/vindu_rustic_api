package org.example.domain.models.content

import com.google.gson.annotations.SerializedName
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class Setting(
    val id: String = shortUUID(),
    val key: String,
    val value: Map<String, Any>,
    val description: String? = null,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
): Serializable
