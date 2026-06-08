package org.example.domain.models.customization

import com.google.gson.annotations.SerializedName
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import org.example.data.db.config.Ulid
import org.example.utils.saveMedia
import java.io.Serializable
import java.time.OffsetDateTime

data class CustomProductAttachment(
    val id: String = Ulid.generate(),

    @SerializedName(value = "request_id")
    val requestId: String,

    @SerializedName(value = "file_url")
    val fileUrl: String,

    @SerializedName(value = "file_type")
    val fileType: String? = null,

    @SerializedName(value = "file_size")
    val fileSize: Long? = null, // bytes

    @SerializedName(value = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
): Serializable {
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