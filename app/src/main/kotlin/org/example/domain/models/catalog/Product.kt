package org.example.domain.models.catalog

import io.ktor.http.Parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.OffsetDateTimeSerializer
import java.time.OffsetDateTime

@Serializable
data class Product(
    val id: String = Ulid.generate(),

    @SerialName(value = "category_id")
    val categoryId: String? = null,

    val title: String,
    val slug: String,

    @SerialName(value = "short_description")
    val shortDescription: String? = null,

    val description: String? = null,
    val status: String = "draft",
    val productType: String = "standard",

    val brand: String? = null,

    @SerialName(value = "is_customizable")
    val isCustomizable: Boolean = false,


    @SerialName(value = "is_featured")
    val isFeatured: Boolean = false,

    @SerialName(value = "seo_title")
    val seoTitle: String? =null,

    @SerialName(value = "seo_description")
    val seoDescription: String? = null,

    @SerialName(value = "created_at")
    @Serializable(with = OffsetDateTimeSerializer::class)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @SerialName(value = "updated_at")
    @Serializable(with = OffsetDateTimeSerializer::class)
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @SerialName(value = "deleted_at")
    @Serializable(with = OffsetDateTimeSerializer::class)
    val deletedAt: OffsetDateTime? = null
) {
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

