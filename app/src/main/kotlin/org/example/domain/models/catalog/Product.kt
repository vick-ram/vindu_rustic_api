package org.example.domain.models.catalog

import com.google.gson.annotations.SerializedName
import io.ktor.http.Parameters
import org.example.data.db.config.Ulid
import java.io.Serializable
import java.time.OffsetDateTime

data class Product(
    val id: String = Ulid.generate(),

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

        fun formParameters(parameters: Parameters): Product {
            val categoryId = parameters["categoryId"].toString()
            val title = parameters["title"].toString()
            val slug = parameters["slug"].toString()
            val shortDescription = parameters["shortDescription"].toString()
            val description = parameters["description"].toString()

            return Product(
                categoryId = categoryId,
                title = title,
                slug = slug,
                shortDescription = shortDescription,
                description = description
            )
        }

        val columns: List<Map<String, Any>> = listOf(
            mapOf("id" to "id", "label" to "id", "sortable" to true),
            mapOf("id" to "title", "label" to "title", "sortable" to true),
            mapOf("id" to "status", "label" to "Status", "sortable" to true),
            mapOf("id" to "productType", "label" to "Type", "sortable" to true),
            mapOf("id" to "brand", "label" to "Brand", "sortable" to true),
            mapOf("id" to "isCustomizable", "label" to "Customizable", "sortable" to true),
            mapOf("id" to "isFeatured", "label" to "Featured", "sortable" to true),
            mapOf("id" to "createdAt", "label" to "CreatedAt", "sortable" to true),
            mapOf("id" to "updatedAt", "label" to "UpdatedAt", "sortable" to true),
            mapOf("id" to "deletedAt", "label" to "DeletedAt", "sortable" to true)
        )

        fun toRows(products: List<Product>): List<Map<String, Any?>> = products.map {
            mapOf(
                "id" to it.id,
                "title" to it.title,
                "status" to it.status,
                "productType" to it.productType,
                "brand" to it.brand,
                "isCustomizable" to it.isCustomizable,
                "isFeatured" to it.isFeatured,
                "createdAt" to it.createdAt,
                "updatedAt" to it.updatedAt,
                "deletedAt" to it.deletedAt
            )
        }
    }
}

