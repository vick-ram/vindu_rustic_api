package org.example.domain.models.customization

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class CustomProductRequest(
    val id: String = shortUUID(),

    @SerializedName(value = "user_id")
    val userId: String,

    val title: String,
    val description: String,
    val specifications: Map<String, Any>? = null,

    @SerializedName(value = "estimated_budget_min")
    val estimatedBudgetMin: BigDecimal? = null,

    @SerializedName(value = "estimated_budget_max")
    val estimatedBudgetMax: BigDecimal? = null,

    val status: String = "pending",
    val notes: String? = null,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
): Serializable