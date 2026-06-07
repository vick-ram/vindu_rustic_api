package org.example.domain.models.catalog

import com.google.gson.annotations.SerializedName
import kotlinx.datetime.LocalDateTime
import org.example.utils.now
import org.example.utils.shortUUID
import java.time.OffsetDateTime

data class ProductMedia(
    val id: String = shortUUID(),

    @SerializedName(value = "product_id")
    val productId: String,

    @SerializedName(value = "variant_id")
    val variantId: String? = null,

    @SerializedName(value = "media_type")
    val mediaType: String,

    @SerializedName(value = "media_url")
    val mediaUrl: String,

    @SerializedName(value = "alt_text")
    val altText: String? = null,

    @SerializedName(value = "sort_order")
    val sortOrder: Int = 0,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)