package org.example.data.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.example.data.repo.CacheConfig
import org.example.data.repo.CrudCache
import org.example.data.repo.CustomProductAttachmentRepository
import org.example.di.Inject
import org.example.di.Injectable
import org.example.domain.models.customization.CustomProductAttachment

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Injectable
class CustomProductAttachmentCache @Inject constructor(
    redis: RedisCoroutinesCommands<String, String>,
    private val delegate: CustomProductAttachmentRepository
) : CrudCache<CustomProductAttachment, String>(
    redis = redis,
    delegate = delegate,
    getId = { it.id },
    serializer = CustomProductAttachment.serializer(),
    config = object : CacheConfig {
        override val cacheName: String = "customProductAttachment"
        override val ttl: Long = 3600L
    }
) {

    // Delegate to repository - these queries don't benefit from individual entity caching
    suspend fun findByRequestId(requestId: String): List<CustomProductAttachment> {
        return delegate.findByRequestId(requestId)
    }

    suspend fun findByRequestAndType(
        requestId: String,
        fileType: String
    ): List<CustomProductAttachment> {
        return delegate.findByRequestAndType(requestId, fileType)
    }

    suspend fun getTotalFileSize(requestId: String): Long {
        return delegate.getTotalFileSize(requestId)
    }

    // Use parent's create for individual caching
    suspend fun bulkCreate(attachments: List<CustomProductAttachment>): List<CustomProductAttachment> {
        val created = delegate.bulkCreate(attachments)

        // Use parent's putInCache for each created attachment
        created.forEach { attachment ->
            putInCache(attachment.id, attachment)
        }

        // Invalidate collection caches since data changed
        invalidateCollectionCaches()

        return created
    }

    suspend fun deleteByRequestId(requestId: String): Int {
        // Get attachments before deletion to clear their individual caches
        val attachments = delegate.findByRequestId(requestId)
        val deletedCount = delegate.deleteByRequestId(requestId)

        // Use parent's removeFromCache for each attachment
        attachments.forEach { attachment ->
            super.delete(attachment.id) // This will call parent's delete which handles cache removal
        }

        return deletedCount
    }

    suspend fun existsForRequest(requestId: String, fileUrl: String): Boolean {
        return delegate.existsForRequest(requestId, fileUrl)
    }

    // Override create to invalidate collection caches
    override suspend fun create(model: CustomProductAttachment): CustomProductAttachment {
        val created = super.create(model)
        invalidateCollectionCaches()
        return created
    }

    // Override update to invalidate collection caches
    override suspend fun update(id: String, entity: CustomProductAttachment): CustomProductAttachment? {
        val updated = super.update(id, entity)
        invalidateCollectionCaches()
        return updated
    }

    // Override delete to invalidate collection caches
    override suspend fun delete(id: String): Boolean {
        val deleted = super.delete(id)
        if (deleted) {
            invalidateCollectionCaches()
        }
        return deleted
    }
}
