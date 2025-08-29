package org.example.data.db.tables

import org.example.domain.models.DimensionUnit
import org.example.domain.models.DiscountAppliedTo
import org.example.domain.models.DiscountType
import org.example.domain.models.MediaType
import org.example.domain.models.OfferType
import org.example.utils.CustomTable
import org.example.utils.PGEnum
import org.example.utils.tsVector
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.datetime.datetime

object CategoryTable : CustomTable("categories") {
    val name = varchar("name", 255).uniqueIndex()
    val slug = varchar("slug", 255).uniqueIndex()
    val description = varchar("description", 255).nullable().index()
    val imageUrl = varchar("image_url", 255).nullable()
    val displayOrder = integer("display_order")
    val tsv = tsVector("tsv")
}

object ProductTable : CustomTable("products") {
    val sku = varchar("sku", 100).uniqueIndex()
    val name = varchar("name", 255).index()
    val description = varchar("description", 255).index()
    val shortDescription = varchar("short_description", 255).index()
    val basePrice = decimal("base_price", 10, 2)
    val viewed = bool("viewed").default(false)
    val category = reference("category_id", CategoryTable, ReferenceOption.CASCADE)
    val stockAvailable = integer("stock_available")
    val stockLowThreshold = integer("stock_low_threshold").default(3)
    val tsv = tsVector("tsv")
}

object DimensionTable: CustomTable("dimensions") {
    val product = reference("product", ProductTable, onDelete = ReferenceOption.CASCADE)
    val width = decimal("width", 10, 2)
    val height = decimal("height", 10, 2)
    val depth = decimal("depth", 10, 2)
    val unit = customEnumeration(
        name = "unit",
        sql = "DimensionUnit",
        fromDb = { value -> DimensionUnit.valueOf(value as String) },
        toDb = { PGEnum("DimensionUnit", it) })
}

object MediaTable : CustomTable("media") {
    val url = varchar("url", 255)
    val type = customEnumeration(
        name = "type",
        sql = "MediaType",
        fromDb = { value -> MediaType.valueOf(value as String) },
        toDb = { PGEnum("MediaType", it) })
    val altText = varchar("alt_text", 255).nullable()
    val isPrimary = bool("is_primary").default(false)
    val displayOrder = integer("display_order").default(0)
    val product = reference("product", ProductTable, ReferenceOption.CASCADE)
}

object DiscountTable : CustomTable("discounts") {
    val name = varchar("name", 255).index()
    val description = text("description").nullable().index()
    val type = customEnumeration(
        name = "type",
        sql = "DiscountType",
        fromDb = { value -> DiscountType.valueOf(value as String) },
        toDb = { PGEnum("DiscountType", it) })
    val value = decimal("value", 12, 2)
    val code = varchar("code", 100).nullable()
    val appliedTo = customEnumeration(
        name = "applied_to",
        sql = "DiscountAppliedTo",
        fromDb = { value -> DiscountAppliedTo.valueOf(value as String) },
        toDb = { PGEnum("DiscountAppliedTo", it) })
    val minimumOrderAmount = decimal("minimum_order_amount", 12, 2).nullable()
    val startDate = datetime("start_date")
    val endDate = datetime("end_date")
    val maxUses = integer("max_uses").nullable()
    val currentUses = integer("current_uses").default(0)
    val isActive = bool("is_active")
    val tsv = tsVector("tsv")
}

object SpecialOfferTable : CustomTable("special_offers") {
    val name = varchar("name", 255).index()
    val description = text("description").index()
    val type = customEnumeration(
        name = "type",
        sql = "OfferType",
        fromDb = { value -> OfferType.valueOf(value as String) },
        toDb = { PGEnum("OfferType", it) })
    val startDate = datetime("start_date")
    val endDate = datetime("end_date")
    val isActive = bool("is_active")
    val tsv = tsVector("tsv")
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
    val title = varchar("title", 255).index()
    val content = text("content").index()
    val isApproved = bool("is_approved").default(false)
    val tsv = tsVector("tsv")
}
