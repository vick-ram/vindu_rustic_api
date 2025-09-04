package org.example.data.repo

import org.example.data.db.entities.CategoryEntity
import org.example.data.db.tables.CategoryTable
import org.example.data.mappers.CategoryMapper
import org.example.domain.models.Category
import org.example.domain.repo.CategoryRepository
import org.example.utils.suspendTransaction

class CategoryRepositoryImpl(private val categoryMapper: CategoryMapper) :
    CrudRepositoryImpl<CategoryEntity, Category>(CategoryEntity, Category::class), CategoryRepository {
    override fun CategoryEntity.toDomain(): Category {
        return categoryMapper.toModel(this)
    }

    override fun Category.toEntity(entity: CategoryEntity) {
        categoryMapper.toEntity(this, entity)
    }

    override fun getId(domain: Category): String = domain.id

    override suspend fun findBySlug(slug: String): Category? = suspendTransaction {
        CategoryEntity.find { CategoryTable.slug.eq(slug) }.firstOrNull()?.toDomain()
    }

}