package org.example.domain.models.sales

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.domain.validations.GreaterThan
import org.example.utils.Ulid
import java.time.OffsetDateTime

@Serializable
data class CartItem(
    val id: String = Ulid.generate(),

    @SerialName(value = "cart_id")
    val cartId: String,

    @SerialName(value = "variant_id")
    val variantId: String,

    @field:GreaterThan(value = 0)
    val quantity: Int,

    @Contextual
    @SerialName("customization_details")
    val customizationDetails: Map<String, Any>? = null,

    @Contextual
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Contextual
    @SerialName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
)