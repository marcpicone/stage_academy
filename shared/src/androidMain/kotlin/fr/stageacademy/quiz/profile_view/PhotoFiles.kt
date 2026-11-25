package fr.stageacademy.quiz.profile_view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

internal class PhotoFiles(private val context: Context) : ProfilePhotos<Uri, Bitmap, File> {
    private val directory = File(context.cacheDir, "quiz_photos")

    fun encodeAvatar(photo: File): ByteArray {
        val preview = loadPreview(photo)
        val scale = minOf(1f, 512f / maxOf(preview.width, preview.height))
        val image = Bitmap.createScaledBitmap(
            preview, (preview.width * scale).toInt().coerceAtLeast(1),
            (preview.height * scale).toInt().coerceAtLeast(1), true,
        )
        try {
            return ByteArrayOutputStream().use { output ->
                if (!image.compress(Bitmap.CompressFormat.JPEG, 82, output)) throw IOException("Image encoding failed")
                output.toByteArray()
            }
        } finally {
            if (image != preview) image.recycle()
            preview.recycle()
        }
    }

    override fun prepareCapture(): File {
        directory.mkdirs()
        return File.createTempFile("capture-", ".jpg", directory)
    }

    override fun isValidCapture(pendingPhoto: File): Boolean {
        if (pendingPhoto.length() == 0L) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(pendingPhoto.absolutePath, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    override fun importPhoto(source: Uri): Bitmap {
        val pending = prepareCapture()
        try {
            val stream = context.contentResolver.openInputStream(source) ?: throw IOException("Image unavailable")
            stream.use { input ->
                pending.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var remaining = 32 * 1024 * 1024
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        remaining -= count
                        if (remaining < 0) throw IOException("Image exceeds the import limit")
                        output.write(buffer, 0, count)
                    }
                }
            }
            return loadPreview(pending)
        } finally {
            pending.delete()
        }
    }

    override fun saveCrop(image: Bitmap, crop: PhotoCrop): File {
        val draft = prepareCapture()
        try {
            val square = Bitmap.createBitmap(720, 720, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(square)
                canvas.drawColor(android.graphics.Color.WHITE)
                crop.draw(canvas, image, square.width.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
                draft.outputStream().use { output ->
                    if (!square.compress(Bitmap.CompressFormat.JPEG, 92, output)) throw IOException("Image encoding failed")
                }
            } finally {
                square.recycle()
            }
            return draft
        } catch (failure: Exception) {
            draft.delete()
            throw failure
        }
    }

    override fun previousDraft(): File? = File(directory, "latest.jpg").takeIf(::isValidCapture)

    override fun discardDraft(photo: File?) {
        photo?.takeIf { it.parentFile == directory }?.delete()
    }

    override fun loadPreview(photo: File): Bitmap {
        if (!photo.isFile) throw FileNotFoundException("Photo no longer available")

        val maxSize = 1200
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(photo)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val size = info.size
                val largestSide = maxOf(size.width, size.height)
                if (largestSide > maxSize) {
                    val scale = maxSize.toFloat() / largestSide
                    decoder.setTargetSize(
                        (size.width * scale).toInt().coerceAtLeast(1),
                        (size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        }

        val path = photo.absolutePath
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        val largestSide = maxOf(options.outWidth, options.outHeight)
        var sampleSize = 1
        while (largestSide.toLong() > maxSize.toLong() * sampleSize) sampleSize *= 2
        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize
        val bitmap = BitmapFactory.decodeFile(path, options) ?: throw IOException("Invalid photo")

        val orientation = try {
            ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val transform = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    setRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    setRotate(-90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        return if (transform.isIdentity) bitmap else {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, true)
        }
    }
}
