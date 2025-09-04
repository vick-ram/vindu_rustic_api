package org.example.data.db.entities

import org.example.data.db.tables.CategoryTable
import org.example.data.db.tables.DimensionTable
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
    var displayOrder by CategoryTable.displayOrder
    var tsv by CategoryTable.tsv
}

class ProductEntity(id: EntityID<String>) : CustomEntity(id, ProductTable) {
    companion object : CustomEntityClass<ProductEntity>(ProductTable)

    var sku by ProductTable.sku
    var name by ProductTable.name
    var description by ProductTable.description
    var shortDescription by ProductTable.shortDescription
    var basePrice by ProductTable.basePrice
    var viewed by ProductTable.viewed
    var stockAvailable by ProductTable.stockAvailable
    var stockLowThreshold by ProductTable.stockLowThreshold
    var isFavorite by ProductTable.isFavorite
    var tsv by ProductTable.tsv

    var category by CategoryEntity referencedOn ProductTable.category
    val media by MediaEntity referrersOn MediaTable.product
    val dimensions by DimensionEntity referrersOn DimensionTable.product
}

class DimensionEntity(id: EntityID<String>) : CustomEntity(id, DimensionTable) {
    companion object: CustomEntityClass<DimensionEntity>(DimensionTable)

    var product by ProductEntity referencedOn DimensionTable.product
    var width by DimensionTable.width
    var height by DimensionTable.height
    var depth by DimensionTable.depth
    var unit by DimensionTable.unit
}

class MediaEntity(id: EntityID<String>) : CustomEntity(id, MediaTable) {
    companion object : CustomEntityClass<MediaEntity>(MediaTable)

    var url by MediaTable.url
    var type by MediaTable.type
    var altText by MediaTable.altText
    var isPrimary by MediaTable.isPrimary
    var displayOrder by MediaTable.displayOrder

    var product by ProductEntity referencedOn MediaTable.product
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
    var tsv by DiscountTable.tsv
}

class SpecialOfferEntity(id: EntityID<String>) : CustomEntity(id, SpecialOfferTable) {
    companion object : CustomEntityClass<SpecialOfferEntity>(SpecialOfferTable)

    var name by SpecialOfferTable.name
    var description by SpecialOfferTable.description
    var type by SpecialOfferTable.type
    var startDate by SpecialOfferTable.startDate
    var endDate by SpecialOfferTable.endDate
    var isActive by SpecialOfferTable.isActive
    var tsv by SpecialOfferTable.tsv

    val products by ProductEntity referrersOn ProductTable
}

class ProductReviewEntity(id: EntityID<String>) : CustomEntity(id, ProductReviewTable) {
    companion object : CustomEntityClass<ProductReviewEntity>(ProductReviewTable)

    var rating by ProductReviewTable.rating
    var title by ProductReviewTable.title
    var content by ProductReviewTable.content
    var isApproved by ProductReviewTable.isApproved
    var tsv by ProductReviewTable.tsv

    val user by UserEntity referencedOn ProductReviewTable.user
    val product by ProductEntity referencedOn ProductReviewTable.product
}
