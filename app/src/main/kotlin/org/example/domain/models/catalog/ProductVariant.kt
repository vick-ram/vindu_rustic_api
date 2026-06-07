package org.example.domain.models.catalog

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class ProductVariant(
    val id: String = shortUUID(),

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

    @SerializedName(value = "quantity_in_stock")
    val quantityInStock: Int = 0,

    @SerializedName(value = "reserved_quantity")
    val reservedQuantity: Int = 0,

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
): Serializable