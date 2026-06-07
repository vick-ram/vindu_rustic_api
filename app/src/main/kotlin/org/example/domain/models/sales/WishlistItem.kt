package org.example.domain.models.sales

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class WishlistItem(
    val id: String = shortUUID(),

    @SerializedName("wishlist_id")
    val wishlistId: String,

    @SerializedName("product_id")
    val productId: String,

    @SerializedName(value = "variant_id")
    val variantId: String? = null,

    val notes: String? = null,

    @SerializedName("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable