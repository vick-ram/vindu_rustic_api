package org.example.domain.models.sales

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class WishlistItem(
    val id: String = Ulid.generate(),

    @SerialName("wishlist_id")
    val wishlistId: String,

    @SerialName("product_id")
    val productId: String,

    @SerialName(value = "variant_id")
    val variantId: String? = null,

    val notes: String? = null,

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)