package org.example.domain.models.customization

import com.google.gson.annotations.SerializedName
import io.ktor.http.Parameters
import org.example.data.db.config.Ulid
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class CustomProductQuote(
    val id: String = Ulid.generate(),

    @SerializedName("request_id")
    val requestId: String,

    @SerializedName("quoted_price")
    val quotedPrice: BigDecimal,

    val currency: String = "kes",

    @SerializedName("production_timeline")
    val productionTimeline: Int? = null,

    val description: String? = null,

    @SerializedName("valid_until")
    val validUntil: OffsetDateTime? = null,

    val status: String = "sent",
    val createdBy: String,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
) : Serializable {
    companion object {
        fun formParameters(parameters: Parameters): CustomProductQuote {
            val requestId = parameters["request_id"].toString()
            val quotedPrice = parameters["quoted_price"]?.toBigDecimal() ?: BigDecimal.ZERO
            val productionTimeline = parameters["production_timeline"]?.toInt()
            val description = parameters["description"].toString()
            val validUntil = parameters["valid_until"]?.let { OffsetDateTime.parse(it) }
            val status = parameters["status"].toString()
            val createdBy = parameters["created_by"].toString()

            return CustomProductQuote(
                requestId = requestId,
                quotedPrice = quotedPrice,
                productionTimeline = productionTimeline,
                description = description,
                validUntil = validUntil,
                status = status,
                createdBy = createdBy
            )
        }

        val columns: List<Map<String, Any>> = listOf(
            mapOf("key" to "id", "label" to "ID"),
            mapOf("key" to "request_id", "label" to "Request ID"),
            mapOf("key" to "quoted_price", "label" to "Quoted Price"),
            mapOf("key" to "production_timeline", "label" to "Production Timeline"),
            mapOf("key" to "description", "label" to "Description"),
            mapOf("key" to "valid_until", "label" to "Valid Until"),
            mapOf("key" to "status", "label" to "Status"),
            mapOf("key" to "created_by", "label" to "Created By"),
            mapOf("key" to "created_at", "label" to "Created At"),
        )

        fun toRows(customProductQuotes: List<CustomProductQuote>): List<Map<String, Any?>> =
            customProductQuotes.map { customProductQuote ->
                mapOf(
                    "id" to customProductQuote.id,
                    "request_id" to customProductQuote.requestId,
                    "quoted_price" to customProductQuote.quotedPrice,
                    "production_timeline" to customProductQuote.productionTimeline,
                    "description" to customProductQuote.description,
                    "valid_until" to customProductQuote.validUntil,
                    "status" to customProductQuote.status,
                    "created_by" to customProductQuote.createdBy,
                    "created_at" to customProductQuote.createdAt,
                )
            }
    }
}
