package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.ProductTags
import org.example.data.db.tables.Tags
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class TagEntity(id: EntityID<String>) : CustomEntity(id, Tags) {
    companion object : CustomEntityClass<TagEntity>(Tags)

    var name by Tags.name
    var slug by Tags.slug

    val products by ProductEntity via ProductTags
}