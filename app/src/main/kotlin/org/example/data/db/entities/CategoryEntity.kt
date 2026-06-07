package org.example.data.db.entities

import org.example.data.db.config.CustomEntity
import org.example.data.db.config.CustomEntityClass
import org.example.data.db.tables.Categories
import org.jetbrains.exposed.v1.core.dao.id.EntityID

class CategoryEntity(id: EntityID<String>): CustomEntity(id, Categories) {
    companion object : CustomEntityClass<CategoryEntity>(Categories)

    var parentId by Categories.parentId
    var name by Categories.name
    var slug by Categories.slug
    var description by Categories.description
    var imageUrl by Categories.imageUrl
    var isActive by Categories.isActive
    var sortOrder by Categories.sortOrder
}