package org.example.data.repo

import org.example.data.db.entities.CategoryEntity
import org.example.data.db.tables.Categories
import org.example.data.mappers.CategoryMapper
import org.example.domain.models.catalog.Category
import org.example.domain.repo.CategoryRepository
import org.example.utils.suspendTransaction
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or

class CategoryRepositoryImpl(private val categoryMapper: CategoryMapper) :
    CrudRepositoryImpl<CategoryEntity, Category>(CategoryEntity, Category::class), CategoryRepository {

    override suspend fun findBySlug(slug: String): Category? = suspendTransaction {
        CategoryEntity.find { Categories.slug eq slug }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findChildren(parentId: String): List<Category> = suspendTransaction {
        CategoryEntity.find { Categories.parentId eq parentId }
            .map { it.toDomain() }
    }

    override suspend fun findActive(): List<Category> = suspendTransaction {
        CategoryEntity.find { Categories.isActive eq true }
            .map { it.toDomain() }
    }

    override suspend fun searchCategories(query: String, offset: Int, limit: Int): List<Category> = suspendTransaction {
        CategoryEntity.find {
            (Categories.name.lowerCase() like "%${query.lowercase()}%") or
                    (Categories.description.lowerCase() like "%${query.lowercase()}%")
        }
            .offset(offset.toLong())
            .limit(limit)
            .map { it.toDomain() }
    }

    override fun CategoryEntity.toDomain(): Category = categoryMapper.toModel(this)
    override fun Category.toEntity(entity: CategoryEntity) {
        categoryMapper.toEntity(this, entity)
    }

    override fun getId(domain: Category): String = domain.id

}