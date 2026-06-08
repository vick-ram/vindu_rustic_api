package org.example.domain.models.sales

import com.google.gson.annotations.SerializedName
import org.example.data.db.config.Ulid
import org.example.domain.validations.GreaterThan
import java.io.Serializable
import java.time.OffsetDateTime

data class CartItem(
    val id: String = Ulid.generate(),

    @SerializedName(value = "cart_id")
    val cartId: String,

    @SerializedName(value = "variant_id")
    val variantId: String,

    @field:GreaterThan(value = 0)
    val quantity: Int,

    @SerializedName("customization_details")
    val customizationDetails: Map<String, Any>? = null,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
): Serializable