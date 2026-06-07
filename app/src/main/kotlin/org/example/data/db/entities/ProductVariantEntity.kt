package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.ProductVariants
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ProductVariantEntity(id: EntityID<String>) : CustomEntity(id, ProductVariants) {
    companion object : CustomEntityClass<ProductVariantEntity>(ProductVariants)

    var productId by ProductVariants.productId
    var sku by ProductVariants.sku
    var title by ProductVariants.title
    var price by ProductVariants.price
    var compareAtPrice by ProductVariants.compareAtPrice
    var costPrice by ProductVariants.costPrice
    var currency by ProductVariants.currency
    var quantityInStock by ProductVariants.quantityInStock
    var reservedQuantity by ProductVariants.reservedQuantity
    var weightGrams by ProductVariants.weightGrams
    var dimensions by ProductVariants.dimensions
    var attributes by ProductVariants.attributes
    var barcode by ProductVariants.barcode
    var isActive by ProductVariants.isActive
}