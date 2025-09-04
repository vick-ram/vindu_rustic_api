package org.example.utils

import io.ktor.http.content.PartData
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File
import java.util.UUID

suspend fun saveMedia(uploadDir: String, fileItem: PartData.FileItem): String {
    val folder = File(uploadDir)
    if (!folder.exists()) {
        folder.mkdirs()
    }

    val fileExtension = fileItem.originalFileName?.substringAfterLast('.') ?: "bin"
    val fileName = "${UUID.randomUUID()}.$fileExtension"
    val file = File(folder, fileName)

    fileItem.provider().copyAndClose(file.writeChannel())

    return fileName
}