package org.example.domain.models.sales

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.GreaterThan
import org.example.domain.validations.NotBlank
import org.example.utils.Ulid
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

    @Contextual
    @SerialName(value = "unit_price")
    val unitPrice: BigDecimal,

    @Contextual
    @SerialName(value = "total_price")
    val totalPrice: BigDecimal,

    @SerialName(value = "customization_snapshot")
    val customizationSnapshot: Map<String, @Contextual Any>? = null,

    @SerialName(value = "product_snapshot")
    val productSnapshot: Map<String, @Contextual Any>,

    @Contextual
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)