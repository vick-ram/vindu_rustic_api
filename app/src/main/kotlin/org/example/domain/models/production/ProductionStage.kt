package org.example.domain.models.production

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.OneOf
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class ProductionStage(
    val id: String = Ulid.generate(),

    @SerialName("job_id")
    val jobId: String,

    @SerialName("stage_name")
    val stageName: String,

    @OneOf("pending", "in_progress", "completed", "failed")
    val status: String = "pending",

    @Contextual
    @SerialName("started_at")
    val startedAt: OffsetDateTime? = null,

    @Contextual
    @SerialName("completed_at")
    val completedAt: OffsetDateTime? = null,
)
