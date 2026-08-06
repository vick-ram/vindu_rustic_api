package org.example.services

import kotlinx.serialization.Serializable
import org.example.data.cache.CustomProductAttachmentCache
import org.example.di.Injectable
import org.example.domain.models.customization.CustomProductAttachment
import org.example.domain.repo.FileStorageService
import org.slf4j.LoggerFactory

@Injectable
class CustomProductAttachmentService(
    private val cache: CustomProductAttachmentCache,
    private val fileStorageService: FileStorageService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(CustomProductAttachmentService::class.java)
    }

    private val maxFileSize = 10 * 1024 * 1024 // 10MB
    private val allowedFileTypes = setOf("image/jpeg", "image/png", "application/pdf", "image/webp")

    suspend fun getRequestAttachments(requestId: String): List<CustomProductAttachment> {
        validateRequestId(requestId)
        return cache.findByRequestId(requestId)
    }

    suspend fun getAttachmentsByType(
        requestId: String,
        fileType: String
    ): List<CustomProductAttachment> {
        validateRequestId(requestId)
        validateFileType(fileType)
        return cache.findByRequestAndType(requestId, fileType)
    }

    suspend fun uploadAttachment(
        requestId: String,
        fileData: ByteArray,
        fileName: String,
        fileType: String
    ): CustomProductAttachment {
        validateRequestId(requestId)
        validateFile(fileData, fileType)

        // Upload to file storage
        val fileUrl = fileStorageService.uploadFile(fileData, fileName, fileType)

        val attachment = CustomProductAttachment(
            requestId = requestId,
            fileUrl = fileUrl,
            fileType = fileType,
            fileSize = fileData.size.toLong()
        )

        logger.info("Creating attachment for request: $requestId")
        return cache.create(attachment) // Uses parent's create with caching
    }

    suspend fun uploadBulkAttachments(
        requestId: String,
        files: List<FileUpload>
    ): List<CustomProductAttachment> {
        validateRequestId(requestId)

        val attachments = files.map { file ->
            validateFile(file.data, file.fileType)
            val fileUrl = fileStorageService.uploadFile(file.data, file.fileName, file.fileType)

            CustomProductAttachment(
                requestId = requestId,
                fileUrl = fileUrl,
                fileType = file.fileType,
                fileSize = file.data.size.toLong()
            )
        }

        logger.info("Bulk creating ${attachments.size} attachments for request: $requestId")
        return cache.bulkCreate(attachments)
    }

    suspend fun getAttachment(attachmentId: String): CustomProductAttachment? {
        return cache.read(attachmentId) // Uses parent's cached read
    }

    suspend fun updateAttachment(
        attachmentId: String,
        fileData: ByteArray?,
        fileName: String?,
        fileType: String?
    ): CustomProductAttachment? {
        val existing = cache.read(attachmentId) ?: return null

        // Upload new file if provided
        val fileUrl = if (fileData != null && fileName != null && fileType != null) {
            validateFile(fileData, fileType)
            // Delete old file
            try {
                fileStorageService.deleteFile(existing.fileUrl)
            } catch (e: Exception) {
                logger.error("Failed to delete old file: ${existing.fileUrl}", e)
            }
            fileStorageService.uploadFile(fileData, fileName, fileType)
        } else {
            existing.fileUrl
        }

        val updated = existing.copy(
            fileUrl = fileUrl,
            fileType = fileType ?: existing.fileType,
            fileSize = fileData?.size?.toLong() ?: existing.fileSize
        )

        return cache.update(attachmentId, updated) // Uses parent's update with caching
    }

    suspend fun deleteAttachment(attachmentId: String): Boolean {
        val attachment = cache.read(attachmentId) ?: return false

        // Delete from file storage first
        try {
            fileStorageService.deleteFile(attachment.fileUrl)
        } catch (e: Exception) {
            logger.error("Failed to delete file from storage: ${attachment.fileUrl}", e)
        }

        logger.info("Deleting attachment: $attachmentId")
        return cache.delete(attachmentId) // Uses parent's delete with cache removal
    }

    suspend fun deleteAllRequestAttachments(requestId: String): Int {
        validateRequestId(requestId)

        // Get attachments to delete files from storage
        val attachments = cache.findByRequestId(requestId)
        attachments.forEach { attachment ->
            try {
                fileStorageService.deleteFile(attachment.fileUrl)
            } catch (e: Exception) {
                logger.error("Failed to delete file from storage: ${attachment.fileUrl}", e)
            }
        }

        logger.info("Deleting all attachments for request: $requestId")
        return cache.deleteByRequestId(requestId)
    }

    suspend fun getTotalFileSize(requestId: String): Long {
        validateRequestId(requestId)
        return cache.getTotalFileSize(requestId)
    }

    suspend fun attachmentExists(requestId: String, fileUrl: String): Boolean {
        validateRequestId(requestId)
        return cache.existsForRequest(requestId, fileUrl)
    }

    suspend fun clearAllCache() {
        logger.info("Clearing all attachment caches")
        cache.clearCache() // Uses parent's clearCache
    }

    // Validation methods
    private fun validateRequestId(requestId: String) {
        require(requestId.isNotBlank()) { "Request ID must not be blank" }
    }

    private fun validateFileType(fileType: String?) {
        require(fileType != null && fileType in allowedFileTypes) {
            "File type $fileType is not allowed. Allowed types: ${allowedFileTypes.joinToString()}"
        }
    }

    private fun validateFile(fileData: ByteArray, fileType: String?) {
        require(fileData.isNotEmpty()) { "File data must not be empty" }
        require(fileData.size <= maxFileSize) {
            "File size ${fileData.size} exceeds maximum allowed size $maxFileSize"
        }
        validateFileType(fileType)
    }
}

@Serializable
data class FileUpload(
    val data: ByteArray,
    val fileName: String,
    val fileType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileUpload

        if (fileName != other.fileName) return false
        if (fileType != other.fileType) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + fileType.hashCode()
        // contentHashCode processes the array's contents
        result = 31 * result + data.contentHashCode()
        return result
    }
}