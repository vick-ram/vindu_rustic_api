package org.example.domain.models.production

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class ProductionUpdate(
    val id: String = shortUUID(),

    @SerializedName("job_id")
    val jobId: String,

    val status: String,

    @SerializedName("stage_name")
    val stageName: String? = null,

    val description: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("posted_by")
    val postedBy: String,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable
