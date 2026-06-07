package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.ProductMedia
import org.example.data.db.tables.ProductTags
import org.example.data.db.tables.ProductVariants
import org.example.data.db.tables.Products
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ProductEntity(id: EntityID<String>) : CustomEntity(id, Products) {
    companion object : CustomEntityClass<ProductEntity>(Products)

    var categoryId by Products.categoryId
    var title by Products.title
    var slug by Products.slug
    var shortDescription by Products.shortDescription
    var description by Products.description
    var status by Products.status
    var productType by Products.productType
    var brand by Products.brand
    var isCustomizable by Products.isCustomizable
    var isFeatured by Products.isFeatured
    var seoTitle by Products.seoTitle
    var seoDescription by Products.seoDescription
    var deletedAt by Products.deletedAt

    val variants by ProductVariantEntity referrersOn ProductVariants.productId
    val media by ProductMediaEntity referrersOn ProductMedia.productId
    val tags by TagEntity via ProductTags
}
