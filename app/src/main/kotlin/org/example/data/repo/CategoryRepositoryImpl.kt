package org.example.data.repo

import org.example.data.db.entities.CategoryEntity
import org.example.data.mappers.CategoryMapper
import org.example.domain.models.Category

class CategoryRepositoryImpl(private val categoryMapper: CategoryMapper): CrudRepositoryImpl<CategoryEntity, Category>(CategoryEntity) {
    override fun CategoryEntity.toDomain(): Category {
        return categoryMapper.toModel(this)
    }

    override fun Category.toEntity(entity: CategoryEntity) {
        categoryMapper.toEntity(this, entity)
    }

}