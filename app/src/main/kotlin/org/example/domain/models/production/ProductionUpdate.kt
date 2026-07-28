package org.example.domain.models.production

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class ProductionUpdate(
    val id: String = Ulid.generate(),

    @SerialName("job_id")
    val jobId: String,

    val status: String,

    @SerialName("stage_name")
    val stageName: String? = null,

    val description: String? = null,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("posted_by")
    val postedBy: String,

    @Contextual
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
