package org.example.domain.models.sales

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.domain.validations.GreaterThan
import org.example.domain.validations.NotBlank
import org.example.utils.BigDecimalSerializer
import org.example.utils.MapStringAnySerializer
import org.example.utils.OffsetDateTimeSerializer
import java.math.BigDecimal
import java.time.OffsetDateTime

@Serializable
data class OrderItem(
    val id: String = Ulid.generate(),

    @SerialName(value = "order_id")
    val orderId: String,

    @SerialName(value = "product_id")
    val productId: String,

    @SerialName(value = "variant_id")
    val variantId: String,

    val warehouseId: String? = null,

    @field:NotBlank
    @field:GreaterThan(value = 0)
    val quantity: Int,

    @Serializable(with = BigDecimalSerializer::class)
    @SerialName(value = "unit_price")
    val unitPrice: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    @SerialName(value = "total_price")
    val totalPrice: BigDecimal,

    @Serializable(with = MapStringAnySerializer::class)
    @SerialName(value = "customization_snapshot")
    val customizationSnapshot: Map<String, Any>? = null,

    @Serializable(with = MapStringAnySerializer::class)
    @SerialName(value = "product_snapshot")
    val productSnapshot: Map<String, Any>,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)