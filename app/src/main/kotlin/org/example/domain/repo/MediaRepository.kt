package org.example.domain.repo

import org.example.domain.models.Media

interface MediaRepository {
    suspend fun addMediaToProduct(productId: String, media: Media): Media
    suspend fun findByProduct(productId: String): List<Media>
}

//class CachedMediaRepository(
//    private val delegate: MediaRepository,
//    private val cache: CrudRepository<Media, String>
//): MediaRepository {
//    override suspend fun addMediaToProduct(
//        productId: String,
//        media: Media
//    ): Media {
//        return delegate.addMediaToProduct(productId, media)
//    }
//
//    override suspend fun findByProduct(productId: String): List<Media> {
//        TODO("Not yet implemented")
//    }
//}