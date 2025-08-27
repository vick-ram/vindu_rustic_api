package org.example.domain.repo

import org.example.domain.models.Media

interface MediaRepository {
    suspend fun addMediaToProduct(productId: String, media: Media): Media
    suspend fun findByProduct(productId: String): List<Media>
}