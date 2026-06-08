package org.example.domain.models.catalog

import com.google.gson.annotations.SerializedName
import io.ktor.http.*
import org.example.data.db.config.Ulid
import org.example.utils.Json
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class ProductVariant(
    val id: String = Ulid.generate(),

    @SerializedName(value = "product_id")
    val productId: String,

    val sku: String,
    val title: String? = null,
    val price: BigDecimal,

    @SerializedName(value = "compare_at_price")
    val compareAtPrice: BigDecimal? = null,

    @SerializedName(value = "cost_price")
    val costPrice: BigDecimal? = null,

    val currency: String = "kes",

    @SerializedName(value = "weight_grams")
    val weightGrams: Int? = null,

    val dimensions: Map<String, Any> = emptyMap(),
    val attributes: Map<String, Any> = emptyMap(),

    @SerializedName(value = "is_active")
    val isActive: Boolean = true,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
): Serializable {
    companion object {

        val columns: List<Map<String, Any>>
            get() = listOf(
                mapOf("key" to "id", "label" to "id"),
                mapOf("key" to "product_id", "label" to "product id"),
                mapOf("key" to "sku", "label" to "sku"),
                mapOf("key" to "title", "label" to "title"),
                mapOf("key" to "price", "label" to "price"),
                mapOf("key" to "compare_at_price", "label" to "compare at"),
                mapOf("key" to "cost_price", "label" to "cost price"),
                mapOf("key" to "currency", "label" to "currency"),
                mapOf("key" to "weight_grams", "label" to "weight"),
                mapOf("key" to "dimensions", "label" to "dimensions"),
                mapOf("key" to "attributes", "label" to "attributes"),
                mapOf("key" to "is_active", "label" to "is active"),
                mapOf("key" to "created_at", "label" to "created at"),
                mapOf("key" to "updated_at", "label" to "updated at")
            )

        fun formParameters(parameters: Parameters): ProductVariant {
            val productId = parameters["product_id"] ?: throw IllegalArgumentException("product_id is required")
            val sku = parameters["sku"] ?: throw IllegalArgumentException("sku is required")
            val title = parameters["title"]
            val price = parameters["price"]?.toBigDecimalOrNull()
                ?: throw IllegalArgumentException("price is required and must be a valid decimal")
            val compareAtPrice = parameters["compare_at_price"]?.toBigDecimalOrNull()
            val costPrice = parameters["cost_price"]?.toBigDecimalOrNull()
            val currency = parameters["currency"] ?: "kes"
            val weightGrams = parameters["weight_grams"]?.toIntOrNull()
            val isActive = parameters["is_active"]?.toBooleanStrictOrNull() ?: true

            // Parse dimensions from JSON string if provided
            val dimensions = parameters["dimensions"]?.let { jsonString ->
                try {
                    Json.decodeFromString<Map<String, Any>>(jsonString)
                } catch (_: Exception) {
                    emptyMap()
                }
            } ?: emptyMap()

            // Parse attributes from JSON string if provided
            val attributes = parameters["attributes"]?.let { jsonString ->
                try {
                    Json.decodeFromString<Map<String, Any>>(jsonString)
                } catch (_: Exception) {
                    emptyMap()
                }
            } ?: emptyMap()

            return ProductVariant(
                productId = productId,
                sku = sku,
                title = title,
                price = price,
                compareAtPrice = compareAtPrice,
                costPrice = costPrice,
                currency = currency,
                weightGrams = weightGrams,
                dimensions = dimensions,
                attributes = attributes,
                isActive = isActive
            )
        }

        fun toRows(productVariants: List<ProductVariant>): List<Map<String, Any?>> {
            return productVariants.map { productVariant ->
                mapOf(
                    "id" to productVariant.id,
                    "product_id" to productVariant.productId,
                    "sku" to productVariant.sku,
                    "title" to productVariant.title,
                    "price" to productVariant.price,
                    "compare_at_price" to productVariant.compareAtPrice,
                    "cost_price" to productVariant.costPrice,
                    "currency" to productVariant.currency,
                    "weight_grams" to productVariant.weightGrams,
                    "dimensions" to productVariant.dimensions,
                    "attributes" to productVariant.attributes,
                    "is_active" to productVariant.isActive,
                    "created_at" to productVariant.createdAt,
                    "updated_at" to productVariant.updatedAt
                )
            }
        }
    }
}