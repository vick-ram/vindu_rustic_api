package org.example.domain.models.sales

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.domain.validations.GreaterThan
import org.example.domain.validations.NotBlank
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime

data class OrderItem(
    val id: String = shortUUID(),

    @SerializedName(value = "order_id")
    val orderId: String,

    @SerializedName(value = "product_id")
    val productId: String,

    @SerializedName(value = "variant_id")
    val variantId: String,

    val warehouseId: String? = null,

    @field:NotBlank
    @field:GreaterThan(value = 0)
    val quantity: Int,

    @SerializedName(value = "unit_price")
    val unitPrice: BigDecimal,

    @SerializedName(value = "total_price")
    val totalPrice: BigDecimal,

    @SerializedName(value = "customization_snapshot")
    val customizationSnapshot: Map<String, Any>? = null,

    @SerializedName(value = "product_snapshot")
    val productSnapshot: Map<String, Any>,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable