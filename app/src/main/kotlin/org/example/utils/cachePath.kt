package org.example.utils

import java.io.File
import java.util.UUID

fun cachePath(filePath: File?): File? {
    val uniqueStoragePath = filePath
        ?.let { File(it, UUID.randomUUID().toString()) }
    return uniqueStoragePath
}

fun cleanOldCache(storageFile: File, days: Int) {
    cachePath(storageFile)?.listFiles()?.forEach { file ->
        if (file.lastModified() < System.currentTimeMillis() - days * 86400000L) {
            file.deleteRecursively()
        }
    }
}