package org.example.domain.models

import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

data class Product(
    val id: String,
    val sku: String,
    val name: String,
    val description: String,
    val shortDescription: String,
    val basePrice: BigDecimal,
    val viewed: Boolean,
    val category: Category,
    val stock: StockInfo,
    val media: List<Media> = emptyList(),
    val seoData: SeoData,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class StockInfo(
    val available: Int,
    val lowStockThreshold: Int = 3
)

enum class DimensionUnit { CM, INCH }

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
    val isActive: Boolean = false,
    val displayOrder: Int = 0,

    )

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
)

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
)

enum class OfferType {
    FLASH_SALE,
    BUNDLE_OFFER,
    GIFT_WITH_PURCHASE,
    LIMITED_TIME_OFFER,
}

data class SeoData(
    val metaTitle: String,
    val metaDescription: String,
    val slug: String,
    val canonicalUrl: String? = null,
    val keywords: List<String> = emptyList(),
)

data class ProductReview(
    val id: String,
    val productId: String,
    val userId: String,
    val rating: Int, // 1-5
    val title: String,
    val content: String,
    val isApproved: Boolean = false
)

