package org.example.domain.models.customization

import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.data.db.config.Ulid
import org.example.utils.OffsetDateTimeSerializer
import org.example.utils.saveMedia
import java.time.OffsetDateTime

@Serializable
data class CustomProductAttachment(
    val id: String = Ulid.generate(),

    @SerialName(value = "request_id")
    val requestId: String,

    @SerialName(value = "file_url")
    val fileUrl: String,

    @SerialName(value = "file_type")
    val fileType: String? = null,

    @SerialName(value = "file_size")
    val fileSize: Long? = null, // bytes

    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) {
    companion object {
        suspend fun multipartForm(multipart: MultiPartData): CustomProductAttachment {
            var requestId: String? = null
            var fileUrl: String? = null
            var fileType: String? = null
            var fileSize: Long? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "request_id" -> requestId = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        when (part.name) {
                            "file" -> {
                                fileUrl = saveMedia("/resources/images/", part)
                            }
                            "file_type" -> {
                                fileType = part.contentType?.toString()
                            }
                            "file_size" -> {
                                fileSize = part.provider().readRemaining().readByteArray().size.toLong()
                            }
                        }
                    }
                    else -> {}
                }
            }
            return CustomProductAttachment(
                requestId = requestId ?: throw IllegalArgumentException("request_id is required"),
                fileUrl = fileUrl ?: throw IllegalArgumentException(),
                fileType = fileType,
                fileSize = fileSize
            )
        }
    }
}