package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.KSerializer
import org.example.data.mappers.CategoryMapper
import org.example.data.repo.CacheConfig
import org.example.data.repo.CategoryRepository
import org.example.data.repo.CrudCache
import org.example.di.Injectable
import org.example.di.Qualifier
import org.example.domain.models.catalog.Category
import org.koin.core.annotation.Single

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Injectable
@Qualifier("categoryCache")
class CategoryCache(
    redis: RedisCoroutinesCommands<String, String>,
    private val categoryRepository: CategoryRepository,
    categoryMapper: CategoryMapper,
    categorySerializer: KSerializer<Category>
) : CrudCache<Category, String>(
    redis = redis,
    delegate = categoryRepository,
    getId = { category -> categoryMapper.getId(category) as String },
    serializer = categorySerializer,
    config = object : CacheConfig {
        override val cacheName: String
            get() = "category"
        override val ttl: Long
            get() = 3600L
    }
) {

    suspend fun findBySlug(slug: String): Category? {
        return categoryRepository.findBySlug(slug)
    }

    suspend fun findChildren(parentId: String): List<Category> {
        return categoryRepository.findChildren(parentId)
    }

    suspend fun findActive(): List<Category> {
        return categoryRepository.findActive()
    }

    suspend fun searchCategories(query: String, offset: Int, limit: Int): List<Category> {
        return categoryRepository.searchCategories(query, offset, limit)
    }
}