package org.example.data.repo

import org.example.data.db.entities.MediaEntity
import org.example.data.db.entities.ProductEntity
import org.example.data.db.tables.MediaTable
import org.example.data.mappers.MediaMapper
import org.example.domain.models.Media
import org.example.domain.repo.MediaRepository
import org.example.utils.suspendTransaction

class MediaRepositoryImpl(private val mediaMapper: MediaMapper) : CrudRepositoryImpl<MediaEntity, Media>(MediaEntity),
    MediaRepository {
    override fun MediaEntity.toDomain(): Media {
        return mediaMapper.toModel(this)
    }

    override fun Media.toEntity(entity: MediaEntity) {
        mediaMapper.toEntity(this, entity)
    }

    override suspend fun addMediaToProduct(
        productId: String,
        media: Media
    ): Media {
        val product = ProductEntity.findById(productId)
        TODO()
    }

    override suspend fun findByProduct(productId: String): List<Media> = suspendTransaction {
        MediaEntity.find { MediaTable.product.eq(productId) }.map { it.toDomain() }
    }
}