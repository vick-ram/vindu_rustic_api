package org.example.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.Image
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.TextLine
import org.jetbrains.skia.Typeface

class ImageProcessor {
    suspend fun resize(imageBytes: ByteArray, width: Int, height: Int): Result<ByteArray> =
        safeImageOp {
            val format = detectFormat(imageBytes)
            val image = Image.makeFromEncoded(imageBytes)
            val surface = Surface.makeRasterN32Premul(width, height)
            val canvas = surface.canvas

            canvas.drawImageRect(image, Rect.makeWH(width.toFloat(), height.toFloat()))
            surface.makeImageSnapshot().encodeToData(format, 90)?.bytes
                ?: throw ImageError.ProcessingFailed("Encoding failed")
        }

    suspend fun crop(imageBytes: ByteArray, x: Int, y: Int, width: Int, height: Int): Result<ByteArray> =
        safeImageOp {
            val format = detectFormat(imageBytes)
            val image = Image.makeFromEncoded(imageBytes)

            val surface = Surface.makeRasterN32Premul(width, height)
            val srcRect = Rect.makeXYWH(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
            val dstRect = Rect.makeWH(width.toFloat(), height.toFloat())

            surface.canvas.drawImageRect(image, srcRect, dstRect)
            surface.makeImageSnapshot().encodeToData(format, 90)?.bytes
                ?: throw ImageError.ProcessingFailed("Encoding failed")
        }

    suspend fun rotate(imageBytes: ByteArray, degrees: Double): Result<ByteArray> =
        safeImageOp {
            val format = detectFormat(imageBytes)
            val image = Image.makeFromEncoded(imageBytes)
            val matrix = Matrix33.makeRotate(degrees.toFloat(), image.width / 2f, image.height / 2f)

            val surface = Surface.makeRasterN32Premul(image.width, image.height)
            val canvas = surface.canvas
            canvas.concat(matrix)
            canvas.drawImage(image, 0f, 0f)

            surface.makeImageSnapshot().encodeToData(format, 90)?.bytes
                ?: throw ImageError.ProcessingFailed("Encoding failed")
        }

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

    suspend fun addWatermark(imageBytes: ByteArray, watermarkText: String): Result<ByteArray> =
        safeImageOp {
            val format = detectFormat(imageBytes)
            val image = Image.makeFromEncoded(imageBytes)
            val surface = Surface.makeRasterN32Premul(image.width, image.height)
            val canvas = surface.canvas

            canvas.drawImage(image, 0f, 0f)

            val paint = Paint().apply {
                color = 0x80FFFFFF.toInt() // semi-transparent white
            }
            // TODO
            val font = Font(Typeface.makeEmpty(), 48f)

            canvas.drawString(watermarkText, 50f, 50f, font, paint)
            canvas.drawString(watermarkText, 150f, 150f, font, paint)
            canvas.drawString(watermarkText, 250f, 250f, font, paint)

            surface.makeImageSnapshot().encodeToData(format, 90)?.bytes
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

    suspend fun generateBadge(text: String, badgeColor: Int): Result<ByteArray> =
        safeImageOp {
            val width = 120
            val height = 40
            val surface = Surface.makeRasterN32Premul(width, height)
            val canvas = surface.canvas

            // Rounded rectangle background
            val paint = Paint().apply { color = badgeColor }
            canvas.drawRRect(RRect.makeXYWH(0f, 0f, width.toFloat(), height.toFloat(), 20f, 20f), paint)

            // White text
            val textPaint = Paint().apply { color = 0xFFFFFFFF.toInt() }
            val font = Font(Typeface.makeEmpty(), 16f)

            val textLine = TextLine.make(text, font)
            val textWidth = textLine.width
            val textHeight = textLine.height

            val x = (width - textWidth) / 2
            val y = (height + textHeight) / 2

            canvas.drawTextLine(textLine, x, y, textPaint)

            surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG, 100)?.bytes
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