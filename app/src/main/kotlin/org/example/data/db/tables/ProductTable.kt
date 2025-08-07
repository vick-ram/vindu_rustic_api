package org.example.data.db.tables

import org.example.domain.models.DiscountAppliedTo
import org.example.domain.models.DiscountType
import org.example.domain.models.OfferType
import org.example.utils.CustomTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.datetime.datetime

object CategoryTable : CustomTable("categories") {
    val name = varchar("name", 255).uniqueIndex()
    val slug = varchar("slug", 255).uniqueIndex()
    val description = varchar("description", 255).nullable()
    val imageUrl = varchar("image_url", 255).nullable()
    val isActive = bool("is_active")
    val displayOrder = integer("display_order")
}

object ProductTable : CustomTable("products") {
    val sku = varchar("sku", 100).uniqueIndex()
    val name = varchar("name", 255)
    val description = varchar("description", 255)
    val shortDescription = varchar("short_description", 255)
    val basePrice = decimal("base_price", 10, 2)
    val viewed = bool("viewed").default(false)
    val category = reference("category_id", CategoryTable, ReferenceOption.CASCADE)
    val stockAvailable = integer("stock_available")
    val stockLowThreshold = integer("stock_low_threshold").default(3)

    // SEO fields
    val metaTitle = varchar("meta_title", 255)
    val metaDescription = varchar("meta_description", 255)
    val seoSlug = varchar("seo_slug", 255)
    val canonicalUrl = varchar("canonical_url", 255).nullable()
    val keywords = array<String>("keywords")

}

object MediaTable : CustomTable("media") {
    val url = varchar("url", 255)
    val type = varchar("type", 50) // Assuming MediaType is stored as a string
    val altText = varchar("alt_text", 255).nullable()
    val isPrimary = bool("is_primary").default(false)
    val displayOrder = integer("display_order").default(0)
    val product = reference("product_id", ProductTable, ReferenceOption.CASCADE)
}

object DiscountTable : CustomTable("discounts") {
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val type = enumerationByName("type", 50, DiscountType::class)
    val value = decimal("value", 12, 2)
    val code = varchar("code", 100).nullable()
    val appliedTo = enumerationByName("applied_to", 50, DiscountAppliedTo::class)
    val minimumOrderAmount = decimal("minimum_order_amount", 12, 2).nullable()
    val startDate = datetime("start_date")
    val endDate = datetime("end_date")
    val maxUses = integer("max_uses").nullable()
    val currentUses = integer("current_uses").default(0)
    val isActive = bool("is_active")
}

object SpecialOfferTable : CustomTable("special_offers") {
    val name = varchar("name", 255)
    val description = text("description")
    val type = enumerationByName("type", 50, OfferType::class)
    val startDate = datetime("start_date")
    val endDate = datetime("end_date")
    val isActive = bool("is_active")
}

object SpecialOfferProductTable : CustomTable("special_offer_products") {
    val specialOffer = reference("special_offer", SpecialOfferTable, ReferenceOption.CASCADE)
    val product = reference("product", ProductTable, ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(specialOffer, product)
}

object ProductReviewTable : CustomTable("product_reviews") {
    val product = reference("product", ProductTable, ReferenceOption.CASCADE)
    val user = reference("user", UserTable, ReferenceOption.CASCADE)
    val rating = integer("rating")
    val title = varchar("title", 255)
    val content = text("content")
    val isApproved = bool("is_approved").default(false)
}
