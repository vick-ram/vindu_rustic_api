package org.example.domain.models.catalog

import com.google.gson.annotations.SerializedName
import org.example.utils.shortUUID
import java.io.Serializable
import java.time.OffsetDateTime

data class Product(
    val id: String = shortUUID(),

    @SerializedName(value = "category_id")
    val categoryId: String? = null,

    val title: String,
    val slug: String,

    @SerializedName(value = "short_description")
    val shortDescription: String? = null,

    val description: String? = null,
    val status: String = "draft",
    val productType: String = "standard",

    val brand: String? = null,

    @SerializedName(value = "is_customizable")
    val isCustomizable: Boolean = false,


    @SerializedName(value = "is_featured")
    val isFeatured: Boolean = false,

    @SerializedName(value = "seo_title")
    val seoTitle: String? =null,

    @SerializedName(value = "seo_description")
    val seoDescription: String? = null,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName(value = "updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @SerializedName(value = "deleted_at")
    val deletedAt: OffsetDateTime? = null
): Serializable {
    companion object {
        val columns: List<Map<String, Any>> = listOf(
            mapOf("id" to "id", "label" to "id")
        )

        fun toRows(products: List<Product>): List<Map<String, Any?>> = products.map {
            mapOf()
        }
    }
}

