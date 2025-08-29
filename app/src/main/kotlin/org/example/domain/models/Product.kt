package org.example.domain.models

import kotlinx.datetime.LocalDateTime
import org.example.domain.validations.Validations
import org.example.plugins.ValidationException
import org.example.utils.now
import java.io.Serializable
import java.math.BigDecimal

data class Product(
    val id: String,
    val sku: String,
    val name: String,
    val description: String,
    val shortDescription: String,
    val basePrice: BigDecimal,
    val viewed: Boolean,
    val categoryId: String,
    val stock: StockInfo,
    val media: List<Media> = emptyList(),
    val dimension: Dimension?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) : Serializable {
    fun validate(): Product {
        Validations.validateAll(
            { Validations.validateNonEmpty(name, "Product name") },
            { Validations.validateMaxLength(description, 500, "Product description") },
            { Validations.validateMaxLength(shortDescription, 120, "Short description") },
            { if (basePrice < BigDecimal.ZERO) throw ValidationException("Price cannot be negative") },
            { if (stock.available < 0) throw ValidationException("Stock cannot be negative") },
            { dimension?.validate() }
        )
        return this
    }
}

data class StockInfo(
    val available: Int,
    val lowStockThreshold: Int = 3
)

enum class DimensionUnit { CM, INCH }

data class Dimension(
    val id: String,
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
    val id: String,
    val url: String,
    val type: MediaType,
    val altText: String? = null,
    val isPrimary: Boolean = false,
    val displayOrder: Int = 0,
)

enum class MediaType { IMAGE, VIDEO, DOCUMENT }

data class Category(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val displayOrder: Int = 0,
) : Serializable {
    fun validate(): Category {
        Validations.validateAll(
            { Validations.validateNonEmpty(name, "Category name") },
            { Validations.validateNonEmpty(slug, "Category slug") },
        )
        return this
    }
}

data class Discount(
    val id: String,
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
) : Serializable {
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
    val id: String,
    val name: String,
    val description: String,
    val type: OfferType,
    val products: List<Product> = emptyList(),
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
    val isApproved: Boolean = false
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
}

