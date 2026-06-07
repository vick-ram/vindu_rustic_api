package org.example.domain.models.catalog

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ProductTag(
    @SerializedName("product_id")
    val productId: String,

    @SerializedName("tag_id")
    val tagId: String,
): Serializable
