package org.example.domain.models

import io.ktor.http.*
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import kotlinx.datetime.LocalDateTime
import org.example.data.db.entities.ProductEntity
import org.example.data.db.entities.UserEntity
import org.example.data.mappers.ProductMapper
import org.example.data.mappers.UserMapper
import org.example.domain.validations.Validations
import org.example.plugins.NotFoundException
import org.example.plugins.ValidationException
import org.example.utils.now
import org.example.utils.saveMedia
import org.example.utils.shortUUID
import org.example.utils.toCustomFormat
import java.io.Serializable
import java.math.BigDecimal

data class Product(
    val id: String = shortUUID(),
    val sku: String = "",
    val name: String,
    val description: String,
    val shortDescription: String,
    val basePrice: BigDecimal,
    val viewed: Boolean = false,
    val categoryId: String,
    val stock: StockInfo,
    val media: List<Media> = emptyList(),
    val dimensions: List<Dimension>? = null, // variations
    val isFavorite: Boolean = false, // Added is favorite
//    val isActive: Boolean = false,
//    val tags: List<String> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun validate(): Product {
        Validations.validateAll(
            { Validations.validateNonEmpty(name, "Product name") },
            { Validations.validateMaxLength(description, 500, "Product description") },
            { Validations.validateMaxLength(shortDescription, 120, "Short description") },
            { if (basePrice < BigDecimal.ZERO) throw ValidationException("Price cannot be negative") },
            { if (stock.available < 0) throw ValidationException("Stock cannot be negative") },
            { dimensions?.forEach { it.validate() } }
        )
        return this
    }

    companion object {
        val columns: List<Map<String, Any>> = listOf(
            mapOf("id" to "id", "label" to "id")
        )

        fun toRows(products: List<Product>): List<Map<String, Any?>> = products.map {
            mapOf()
        }
    }
}

data class StockInfo(
    val available: Int,
    val lowStockThreshold: Int = 3
)

enum class ProductStatus { ACTIVE, INACTIVE, OUT_OF_STOCK, DISCONTINUED }

enum class DimensionUnit { CM, INCH }

data class Dimension(
    val id: String = shortUUID(),
    val width: Int,
    val height: Int,
    val depth: Int,
    val unit: DimensionUnit
) {
    fun validate(): Dimension {
        Validations.validateAll(
            { Validations.validateGreaterThan(width, 0, "Width") },
            { Validations.validateGreaterThan(height, 0, "Height") },
            { Validations.validateGreaterThan(depth, 0, "Depth") },
            { Validations.validateEnum<DimensionUnit>(unit, "Dimension unit") }
        )
        return this
    }
}

data class Media(
    val id: String = shortUUID(),
    val url: String,
    val type: MediaType,
    val altText: String? = null,
    val isPrimary: Boolean = false,
    val displayOrder: Int = 0,
) {
    fun validate(): Media {
        Validations.validateAll(
            { Validations.validateEnum<MediaType>(type, "Media type") }
        )
        return this
    }
}

enum class MediaType { IMAGE, VIDEO, DOCUMENT }

data class Category(
    val id: String = shortUUID(),
    val name: String = "",
    val slug: String = "",
    val description: String? = null,
    val imageUrl: String? = null,
    val displayOrder: Int = 0,
) {
    fun validate(): Category {
        Validations.validateAll(
            { Validations.validateNonEmpty(name, "Category name") },
            { Validations.validateNonEmpty(slug, "Category slug") },
        )
        return this
    }

    companion object {
        fun formParameters(parameters: Parameters): Category {
            val name = parameters["name"].toString()
            val slug = parameters["slug"].toString()
            val description = parameters["description"].toString()
            val image = parameters["image"].toString()

            return Category(name = name, slug = slug, description = description, imageUrl = image)
        }

        suspend fun multipartFormData(multipart: MultiPartData): Category {
            var name: String? = null
            var slug: String? = null
            var description: String? = null
            var image: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "name" -> name = part.value
                            "slug" -> slug = part.value
                            "description" -> description = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        if (part.name == "image") {
                            image = saveMedia("categories", part)
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }
            return Category(name = name!!, slug = slug!!, description = description!!, imageUrl = image)
        }

        val columns: List<Map<String, Any>> = listOf(
            mapOf("key" to "id", "label" to "id"),
            mapOf("key" to "name", "label" to "name", "sortable" to true),
            mapOf("key" to "slug", "label" to "slug", "sortable" to true),
            mapOf("key" to "description", "label" to "slug", "sortable" to true),
            mapOf("key" to "imageUrl", "label" to "imageUrl")
        )

        fun toRow(categories: List<Category>): List<Map<String, Any?>> = categories.map { category ->
            mapOf(
                "id" to category.id,
                "name" to category.name,
                "slug" to category.slug,
                "description" to category.description,
                "imageUrl" to category.imageUrl
            )
        }
    }
}

data class Discount(
    val id: String = shortUUID(),
    val name: String,
    val description: String? = null,
    val type: DiscountType,
    val value: BigDecimal,
    val code: String? = null,
    val appliedTo: DiscountAppliedTo,
    val minimumOrderAmount: BigDecimal? = null,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val maxUses: Int? = null,
    val currentUses: Int = 0,
    val isActive: Boolean = false,
) {
    fun validate(): Discount {
        Validations.validateAll(
            { Validations.validateNonEmpty(name, "Discount name") },
            { Validations.validateEnum<DiscountType>(type, "Discount type") },
            { Validations.validateNonEmpty(value.toString(), "Discount value") },
            { Validations.validateEnum<DiscountAppliedTo>(appliedTo, "Discount applied to") },
            {
                Validations.validateMinLength(
                    minimumOrderAmount?.toPlainString() ?: "",
                    minimumOrderAmount?.toInt() ?: 0,
                    "Minimum order amount"
                )
            },
            { Validations.validateDateTime(dateTimeStr = startDate.toString(), fieldName = "Start Date") },
            { Validations.validateFutureDateTime(startDate, "Start Date") },
            { Validations.validateDateTime(dateTimeStr = endDate.toString(), fieldName = "End Date") },
            { Validations.validateFutureDateTime(endDate, "End Date") },

            )
        return this
    }

    companion object {
        fun formParameters(parameters: Parameters): Discount {
            val name = parameters["name"].toString()
            val description = parameters["description"].toString()
            val type = parameters["type"].toString()
            val value = parameters["value"].toString()
            val code = parameters["code"].toString()
            val appliedTo = parameters["code"].toString()
            val minimumOrderAmount = parameters["code"].toString()
            val startDate = parameters["startDate"].toString()
            val endDate = parameters["endDate"].toString()
            val isActive = parameters["isActive"].toBoolean()

            return Discount(
                name = name,
                description = description,
                type = DiscountType.valueOf(type),
                value = BigDecimal(value),
                code = code,
                appliedTo = DiscountAppliedTo.valueOf(appliedTo),
                minimumOrderAmount = BigDecimal(minimumOrderAmount),
                startDate = LocalDateTime.parse(startDate),
                endDate = LocalDateTime.parse(endDate),
                isActive = isActive
            )
        }
    }
}

enum class DiscountType {
    PERCENTAGE_OFF, // 10% off
    FIXED_AMOUNT_OFF,
    BUY_X_GET_Y_FREE,
    FREE_SHIPPING
}

enum class DiscountAppliedTo {
    ALL_PRODUCTS,
    SPECIFIC_PRODUCTS,
    SPECIFIC_CATEGORIES,
    ORDER_TOTAL
}

data class SpecialOffer(
    val id: String = shortUUID(),
    val name: String,
    val description: String,
    val type: OfferType,
    val products: List<String> = emptyList(),
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val isActive: Boolean = false,
) {
    fun validate(): SpecialOffer {
        Validations.validateAll(
            { Validations.validateNonEmpty(name, "Name") },
            { Validations.validateNonEmpty(description, "Description") },
            { Validations.validateEnum<OfferType>(type, "Offer type") },
            { Validations.validateDateTime(startDate.toString(), "Start date") },
            { Validations.validateFutureDateTime(startDate, "Start date") },
            { Validations.validateDateTime(endDate.toString(), "End date") },
            { Validations.validateFutureDateTime(endDate, "End date") },
        )
        return this
    }

    companion object {
        fun formParameters(parameters: Parameters): SpecialOffer {
            val name = parameters["name"].toString()
            val description = parameters["description"].toString()
            val type = parameters["type"].toString()
            val products = parameters.getAll("products")?.map { it } ?: emptyList()
            val startDate = parameters["startDate"].toString()
            val endDate = parameters["endDate"].toString()
            val isActive = parameters["isActive"].toBoolean()

            return SpecialOffer(
                name = name,
                description = description,
                type = OfferType.valueOf(type),
                products = products,
                startDate = LocalDateTime.parse(startDate),
                endDate = LocalDateTime.parse(endDate),
                isActive = isActive
            )
        }
    }
}

enum class OfferType {
    FLASH_SALE,
    BUNDLE_OFFER,
    GIFT_WITH_PURCHASE,
    LIMITED_TIME_OFFER,
}

data class ProductReview(
    val id: String,
    val productId: String,
    val userId: String,
    val rating: Int, // 1-5
    val title: String,
    val content: String,
    val isApproved: Boolean = false,
    val date: LocalDateTime
) {
    fun validate(): ProductReview {
        Validations.validateAll(
            { Validations.validateGreaterThan(rating, 1, "Rating") },
            { Validations.validateLessThan(rating, 5, "Rating") },
            { Validations.validateNonEmpty(title, "Title") },
            { Validations.validateNonEmpty(content, "Content") },
            { Validations.validateMinLength(content, 3, "Content") }
        )

        return this
    }

    companion object {
        // Helper functions
        private fun getReviewUser(userId: String): User {
            val userEntity = UserEntity.findById(userId) ?: throw NotFoundException("User with id: $userId not found")
            val user = UserMapper.toModel(userEntity)
            return user
        }

        private fun getReviewProduct(productId: String): Product {
            val productEntity =
                ProductEntity.findById(productId) ?: throw NotFoundException("Product with id: $productId not found")
            val product = ProductMapper.toModel(productEntity)
            return product
        }

        val columns: List<Map<String, Any>> = listOf(
            mapOf("key" to "product", "label" to "product"),
            mapOf("key" to "reviewer", "label" to "reviewer", "sortable" to true),
            mapOf("key" to "review", "label" to "review", "sortable" to true),
            mapOf("key" to "date", "label" to "date", "sortable" to true),
            mapOf("key" to "status", "label" to "status", "sortable" to true),
            mapOf("key" to "actions", "label" to "actions")
        )

        fun toRows(reviews: List<ProductReview>): List<Map<String, Any?>> = reviews.map { review ->
            mapOf(
                "product" to getReviewProduct(review.productId),
                "reviewer" to getReviewUser(review.userId),
                "review" to review,
                "date" to review.date.toCustomFormat(),
                "status" to if (review.isApproved) "Published" else "Pending"
            )
        }
    }
}

data class CreateProductRequest(
    val name: String = "",
    val description: String = "",
    val shortDescription: String = "",
    val basePrice: BigDecimal = BigDecimal.ZERO,
    val categoryId: String = "",
    val availableStock: Int = 0,
    val lowStockThreshold: Int = 3,
    val dimensions: List<Dimension>? = null
) {
    fun validate(): CreateProductRequest {
        Validations.validateAll(
            { Validations.validateNonEmpty(name, "Product name") },
            { Validations.validateNonEmpty(description, "Product description") },
            { Validations.validateNonEmpty(shortDescription, "Short description") },
            { Validations.validateNonEmpty(basePrice.toString(), "Base price") },
            { Validations.validateNonEmpty(categoryId, "Category ID") },
            { Validations.validateNonEmpty(availableStock.toString(), "Available stock") },
            { if (basePrice < BigDecimal.ZERO) throw ValidationException("Price cannot be negative") },
            { if (availableStock < 0) throw ValidationException("Stock cannot be negative") },
            { dimensions?.forEach { it.validate() } }
        )
        return this
    }
}

