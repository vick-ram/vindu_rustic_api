package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.ProductTags
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class ProductTagEntity(id: EntityID<String>): CustomEntity(id, ProductTags) {

    companion object : CustomEntityClass<ProductTagEntity>(ProductTags)

    var productId by ProductTags.productId
    var tagId by ProductTags.tagId
}