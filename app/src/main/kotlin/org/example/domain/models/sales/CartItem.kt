package org.example.domain.models.sales

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.domain.validations.GreaterThan
import org.example.utils.MapStringAnySerializer
import org.example.utils.OffsetDateTimeSerializer
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

    @Serializable(with = MapStringAnySerializer::class)
    @SerialName("customization_details")
    val customizationDetails: Map<String, Any>? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
)