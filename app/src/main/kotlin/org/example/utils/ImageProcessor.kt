package org.example.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.*

class ImageProcessor {

    suspend fun optimizeImage(originalImage: ByteArray, maxWidth: Int, quality: Int): Result<ByteArray> =
        safeImageOp {
            val format = detectFormat(originalImage)
            val image = Image.makeFromEncoded(originalImage)
            val aspectRatio = image.height.toDouble() / image.width.toDouble()
            val newHeight = (maxWidth * aspectRatio).toInt()

            val surface = Surface.makeRasterN32Premul(maxWidth, newHeight)
            val canvas = surface.canvas
            canvas.drawImageRect(image, Rect.makeWH(maxWidth.toFloat(), newHeight.toFloat()))

            surface.makeImageSnapshot().encodeToData(format, quality)?.bytes
                ?: throw ImageError.ProcessingFailed("Encoding failed")
        }

    suspend fun standardizeImage(imageBytes: ByteArray): Result<ByteArray> =
        safeImageOp {
            val format = detectFormat(imageBytes)
            val image = Image.makeFromEncoded(imageBytes)
            val targetSize = 800
            val scale = minOf(targetSize.toDouble() / image.width, targetSize.toDouble() / image.height)
            val newWidth = (image.width * scale).toInt()
            val newHeight = (image.height * scale).toInt()
            val x = (targetSize - newWidth) / 2
            val y = (targetSize - newHeight) / 2

            val surface = Surface.makeRasterN32Premul(targetSize, targetSize)
            val canvas = surface.canvas

            // White background
            canvas.clear(0xFFFFFFFF.toInt())
            canvas.drawImageRect(
                image,
                Rect.makeXYWH(x.toFloat(), y.toFloat(), newWidth.toFloat(), newHeight.toFloat())
            )

            surface.makeImageSnapshot().encodeToData(format, 90)?.bytes
                ?: throw ImageError.ProcessingFailed("Encoding failed")
        }

    suspend fun validateProductImage(imageBytes: ByteArray): ValidationResult = withContext(Dispatchers.IO) {
        // Skia requires wrapping the byte array into a Data object
        val data = Data.makeFromBytes(imageBytes)

        // Use Codec to decode metadata and format
        val codec = Codec.makeFromData(data)
        val image = Image.makeFromEncoded(data.bytes)

        ValidationResult(
            isValid = image.width >= 500 && image.height >= 500,
            aspectRatio = image.width.toDouble() / image.height.toDouble(),
            fileSize = imageBytes.size,
            format = codec.encodedImageFormat.name // e.g. "JPEG", "PNG", etc.
        )
    }

    private suspend inline fun safeImageOp(crossinline block: suspend () -> ByteArray): Result<ByteArray> =
        withContext(Dispatchers.Default) {
            try {
                Result.success(block())
            } catch (e: Exception) {
                Result.failure(ImageError.ProcessingFailed(e.message ?: "Unknown error"))
            }
        }

    private fun detectFormat(bytes: ByteArray): EncodedImageFormat {
        return try {
            val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
            codec.encodedImageFormat
        } catch (_: Exception) {
            EncodedImageFormat.JPEG
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val aspectRatio: Double,
    val fileSize: Int,
    val format: String
)

sealed class ImageError(message: String) : Exception(message) {
    class InvalidData : ImageError("Invalid image data")
    class ProcessingFailed(msg: String) : ImageError(msg)
}