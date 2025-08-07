package org.example.data.db.entities

import org.example.data.db.tables.CategoryTable
import org.example.data.db.tables.DiscountTable
import org.example.data.db.tables.MediaTable
import org.example.data.db.tables.ProductReviewTable
import org.example.data.db.tables.ProductTable
import org.example.data.db.tables.SpecialOfferTable
import org.example.utils.CustomEntity
import org.example.utils.CustomEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class CategoryEntity(id: EntityID<String>) : CustomEntity(id, CategoryTable) {
    companion object : CustomEntityClass<CategoryEntity>(CategoryTable)

    var name by CategoryTable.name
    var slug by CategoryTable.slug
    var description by CategoryTable.description
    var imageUrl by CategoryTable.imageUrl
    var isActive by CategoryTable.isActive
    var displayOrder by CategoryTable.displayOrder
}

class ProductEntity(id: EntityID<String>) : CustomEntity(id, ProductTable) {
    companion object : CustomEntityClass<ProductEntity>(ProductTable)

    var sku by ProductTable.sku
    var name by ProductTable.name
    var description by ProductTable.description
    var shortDescription by ProductTable.shortDescription
    var basePrice by ProductTable.basePrice
    var viewed by ProductTable.viewed
    var categoryId by ProductTable.category
    var stockAvailable by ProductTable.stockAvailable
    var stockLowThreshold by ProductTable.stockLowThreshold

    var metaTitle by ProductTable.metaTitle
    var metaDescription by ProductTable.metaDescription
    var seoSlug by ProductTable.seoSlug
    var canonicalUrl by ProductTable.canonicalUrl
    var keywords by ProductTable.keywords

    val category by CategoryEntity referencedOn ProductTable.category
    val media by MediaEntity referrersOn MediaTable.id

}

class MediaEntity(id: EntityID<String>) : CustomEntity(id, MediaTable) {
    companion object : CustomEntityClass<MediaEntity>(MediaTable)

    var url by MediaTable.url
    var type by MediaTable.type
    var altText by MediaTable.altText
    var isPrimary by MediaTable.isPrimary
    var displayOrder by MediaTable.displayOrder
    var productId by MediaTable.product

    val product by ProductEntity referencedOn MediaTable.product
}

class DiscountEntity(id: EntityID<String>) : CustomEntity(id, DiscountTable) {
    companion object : CustomEntityClass<DiscountEntity>(DiscountTable)

    var name by DiscountTable.name
    var description by DiscountTable.description
    var type by DiscountTable.type
    var value by DiscountTable.value
    var code by DiscountTable.code
    var appliedTo by DiscountTable.appliedTo
    var minimumOrderAmount by DiscountTable.minimumOrderAmount
    var startDate by DiscountTable.startDate
    var endDate by DiscountTable.endDate
    var maxUses by DiscountTable.maxUses
    var currentUses by DiscountTable.currentUses
    var isActive by DiscountTable.isActive
}

class SpecialOfferEntity(id: EntityID<String>) : CustomEntity(id, SpecialOfferTable) {
    companion object : CustomEntityClass<SpecialOfferEntity>(SpecialOfferTable)

    var name by SpecialOfferTable.name
    var description by SpecialOfferTable.description
    var type by SpecialOfferTable.type
    var startDate by SpecialOfferTable.startDate
    var endDate by SpecialOfferTable.endDate
    var isActive by SpecialOfferTable.isActive
}

class ProductReviewEntity(id: EntityID<String>) : CustomEntity(id, ProductReviewTable) {
    companion object : CustomEntityClass<ProductReviewEntity>(ProductReviewTable)

    var productId by ProductReviewTable.product
    var userId by ProductReviewTable.user
    var rating by ProductReviewTable.rating
    var title by ProductReviewTable.title
    var content by ProductReviewTable.content
    var isApproved by ProductReviewTable.isApproved

    val product by ProductEntity referencedOn ProductReviewTable.product
}
