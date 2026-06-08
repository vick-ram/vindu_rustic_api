package org.example.domain.models.customization

import com.google.gson.annotations.SerializedName
import io.ktor.http.Parameters
import org.example.data.db.config.Ulid
import org.example.utils.Json
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class CustomProductRequest(
    val id: String = Ulid.generate(),

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
): Serializable {
    companion object {

        fun formParameters(parameters: Parameters): CustomProductRequest {
            val userId = parameters["user_id"].toString()
            val title = parameters["title"].toString()
            val description = parameters["description"].toString()
            val specifications = parameters["specifications"]?.let { Json.decodeFromString<Map<String, Any>>(it) }
            val estimatedBudgetMin = parameters["estimated_budget_min"]?.toBigDecimalOrNull()
            val estimatedBudgetMax = parameters["estimated_budget_max"]?.toBigDecimalOrNull()
            val status = parameters["status"].toString()
            val notes = parameters["notes"].toString()

            return CustomProductRequest(
                userId = userId,
                title = title,
                description = description,
                specifications = specifications,
                estimatedBudgetMin = estimatedBudgetMin,
                estimatedBudgetMax = estimatedBudgetMax,
                status = status,
                notes = notes
            )
        }

        val columns: List<Map<String, Any>> = listOf(
            mapOf("key" to "id", "label" to "ID"),
            mapOf("key" to "user_id", "label" to "User ID"),
            mapOf("key" to "title", "label" to "Title", "sortable" to true),
            mapOf("key" to "status", "label" to "Status", "sortable" to true),
            mapOf("key" to "budget", "label" to "Budget Range"),
            mapOf("key" to "created_at", "label" to "Created At", "sortable" to true),
        )

        fun toRows(requests: List<CustomProductRequest>): List<Map<String, Any?>> =
            requests.map { request ->
                mapOf(
                    "id" to request.id,
                    "user_id" to request.userId,
                    "title" to request.title,
                    "description" to request.description,
                    "specifications" to request.specifications,
                    "budget" to "${request.estimatedBudgetMin ?: 0} - ${request.estimatedBudgetMax ?: "N/A"}",
                    "status" to request.status,
                    "notes" to request.notes,
                    "created_at" to request.createdAt,
                    "updated_at" to request.updatedAt
                )
            }
    }
}