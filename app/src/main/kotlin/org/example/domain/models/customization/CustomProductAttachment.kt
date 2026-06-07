package org.example.domain.models.customization

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class CustomProductAttachment(
    val id: String = shortUUID(),

    @SerializedName(value = "request_id")
    val requestId: String,

    @SerializedName(value = "file_url")
    val fileUrl: String,

    @SerializedName(value = "file_type")
    val fileType: String? = null,

    @SerializedName(value = "file_size")
    val fileSize: Long? = null, // bytes

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable