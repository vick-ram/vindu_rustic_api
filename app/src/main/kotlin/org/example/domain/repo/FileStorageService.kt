package org.example.domain.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.config.AppConfig
import org.example.di.Inject
import org.example.di.Injectable
import org.example.plugins.FileStorageException
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@Injectable
class FileStorageService @Inject constructor(
    appConfig: AppConfig
) {
    val storageConfig = appConfig.storage

   companion object {
       private val logger = LoggerFactory.getLogger(javaClass)
   }

    suspend fun uploadFile(fileData: ByteArray, fileName: String, fileType: String): String {
        val fileExtension = getFileExtension(fileName, fileType)
        val uniqueFileName = "${UUID.randomUUID()}.$fileExtension"
        val filePath = Paths.get(storageConfig.uploadDir, uniqueFileName)

        try {
            withContext(Dispatchers.IO) {
                Files.createDirectories(filePath.parent)
                Files.write(filePath, fileData)
            }
            logger.info("File uploaded successfully: $uniqueFileName")
            return "/uploads/$uniqueFileName"
        } catch (e: Exception) {
            logger.error("Failed to upload file: $fileName", e)
            throw FileStorageException("Failed to upload file: $fileName", e)
        }
    }

    suspend fun deleteFile(fileUrl: String) {
        try {
            val fileName = fileUrl.substringAfterLast("/")
            val filePath = Paths.get(storageConfig.uploadDir, fileName)

            withContext(Dispatchers.IO) {
                Files.deleteIfExists(filePath)
            }
            logger.info("File deleted successfully: $fileName")
        } catch (e: Exception) {
            logger.error("Failed to delete file: $fileUrl", e)
            throw FileStorageException("Failed to delete file: $fileUrl", e)
        }
    }

    suspend fun getFile(fileUrl: String): ByteArray? {
        return try {
            val fileName = fileUrl.substringAfterLast("/")
            val filePath = Paths.get(storageConfig.uploadDir, fileName)

            if (Files.exists(filePath)) {
                withContext(Dispatchers.IO) {
                    Files.readAllBytes(filePath)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to read file: $fileUrl", e)
            null
        }
    }

    suspend fun fileExists(fileUrl: String): Boolean {
        val fileName = fileUrl.substringAfterLast("/")
        val filePath = Paths.get(storageConfig.uploadDir, fileName)
        return Files.exists(filePath)
    }

    private fun getFileExtension(fileName: String, fileType: String): String {
        return when (fileType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "application/pdf" -> "pdf"
            else -> {
                // Try to get extension from original filename
                val extension = fileName.substringAfterLast(".", "")
                if (extension.isNotEmpty()) extension else "bin"
            }
        }
    }
}