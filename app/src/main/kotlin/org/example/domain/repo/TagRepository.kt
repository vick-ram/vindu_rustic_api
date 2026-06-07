package org.example.domain.repo

import org.example.domain.models.catalog.ProductTag
import org.example.domain.models.catalog.Tag

interface TagRepository : CrudRepository<Tag, String> {
    suspend fun findBySlug(slug: String): Tag?
    suspend fun findProductTags(productId: String): List<Tag>
    suspend fun searchTags(query: String): List<Tag>
    suspend fun attachTag(productId: String, tagId: String): ProductTag
    suspend fun detachTag(productId: String, tagId: String): Boolean
}