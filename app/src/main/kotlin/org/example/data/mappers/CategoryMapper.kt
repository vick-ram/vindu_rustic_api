package org.example.data.mappers

import org.example.data.db.entities.CategoryEntity
import org.example.data.db.tables.Categories
import org.example.domain.models.catalog.Category
import org.example.domain.repo.EntityMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object CategoryMapper : EntityMapper<CategoryEntity, Category, String> {
    override fun toModel(entity: CategoryEntity): Category {
        return Category(
            id = entity.id.value,
            parentId = entity.parentId?.value,
            name = entity.name,
            slug = entity.slug,
            description = entity.description,
            imageUrl = entity.imageUrl,
            isActive = entity.isActive,
            sortOrder = entity.sortOrder,
            createdAt = entity.createdAt,
        )
    }

    override fun toEntity(model: Category, entity: CategoryEntity): CategoryEntity {
        entity.parentId = model.parentId?.let { EntityID(it, Categories) }
        entity.name = model.name
        entity.slug = model.slug
        entity.description = model.description
        entity.imageUrl = model.imageUrl
        entity.isActive = model.isActive
        entity.sortOrder = model.sortOrder
        return entity
    }
}