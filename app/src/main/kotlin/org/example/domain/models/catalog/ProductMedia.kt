package org.example.domain.models.catalog

import com.google.gson.annotations.SerializedName
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import org.example.data.db.config.Ulid
import org.example.utils.saveMedia
import java.io.File
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class ProductMedia(
    val id: String = Ulid.generate(),

    @SerializedName(value = "product_id")
    val productId: String,

    @SerializedName(value = "variant_id")
    val variantId: String? = null,

    @SerializedName(value = "media_type")
    val mediaType: String,

    @SerializedName(value = "media_url")
    val mediaUrl: String,

    @SerializedName(value = "alt_text")
    val altText: String? = null,

    @SerializedName(value = "sort_order")
    val sortOrder: Int = 0,

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
): Serializable {
    companion object {
        suspend fun multipartForm(multipart: MultiPartData): ProductMedia {
            var productId: String? = null
            var variantId: String? = null
            var mediaType: String? = null
            var mediaUrl: String? = null
            var altText: String? = null
            var sortOrder: Int? = null
            var fileBytes: ByteArray? = null
            var fileExtension: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "product_id" -> productId = part.value
                            "variant_id" -> variantId = part.value.takeIf { it.isNotBlank() }
                            "media_type" -> mediaType = part.value
                            "alt_text" -> altText = part.value.takeIf { it.isNotBlank() }
                            "sort_order" -> sortOrder = part.value.toIntOrNull()
                        }
                    }
                    is PartData.FileItem -> {
                        if (part.name == "media_file") {
                             mediaUrl = saveMedia("/resoures/static/images", part)
                        } else if (part.name == "media_url") {
                            mediaUrl = part.originalFileName
                        }
                    }

                    else -> {}
                }
                part.dispose()
            }

            return ProductMedia(
                productId = productId ?: throw IllegalArgumentException("product_id is required"),
                variantId = variantId,
                mediaType = mediaType ?: throw IllegalArgumentException("media_type is required"),
                mediaUrl = mediaUrl ?: throw IllegalArgumentException("media_url is required"),
                altText = altText,
                sortOrder = sortOrder ?: 0
            )
        }
    }
}