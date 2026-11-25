package fr.stageacademy.quiz.profile_view

import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import javax.imageio.ImageIO

internal class DesktopPhotoFiles(root: File) : ProfilePhotos<File, BufferedImage, File> {
    private val directory = File(root, "quiz_photos")

    fun encodeAvatar(photo: File): ByteArray {
        val preview = loadPreview(photo)
        val scale = minOf(1.0, 512.0 / maxOf(preview.width, preview.height))
        val image = BufferedImage(
            (preview.width * scale).toInt().coerceAtLeast(1),
            (preview.height * scale).toInt().coerceAtLeast(1),
            BufferedImage.TYPE_INT_RGB,
        )
        val graphics = image.createGraphics()
        try {
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, image.width, image.height)
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.drawImage(preview, 0, 0, image.width, image.height, null)
        } finally {
            graphics.dispose()
        }
        return ByteArrayOutputStream().use { output ->
            if (!ImageIO.write(image, "jpg", output)) throw IOException("Image encoding failed")
            output.toByteArray()
        }
    }

    override fun prepareCapture(): File {
        Files.createDirectories(directory.toPath())
        return File.createTempFile("capture-", ".jpg", directory).apply { deleteOnExit() }
    }

    override fun isValidCapture(pendingPhoto: File): Boolean = pendingPhoto.isFile && pendingPhoto.length() > 0

    override fun previousDraft(): File? = null

    override fun discardDraft(photo: File?) {
        photo?.takeIf { it.parentFile == directory }?.delete()
    }

    override fun importPhoto(source: File): BufferedImage {
        if (source.length() > 32 * 1024 * 1024) throw IOException("Image exceeds the import limit")
        return loadPreview(source)
    }

    override fun loadPreview(photo: File): BufferedImage {
        if (!photo.isFile) throw FileNotFoundException("Photo no longer available")
        val input = ImageIO.createImageInputStream(photo) ?: throw IOException("Image unavailable")
        input.use {
            val readers = ImageIO.getImageReaders(input)
            if (!readers.hasNext()) throw IOException("Invalid image")
            val reader = readers.next()
            try {
                reader.setInput(input, true, true)
                val largestSide = maxOf(reader.getWidth(0), reader.getHeight(0))
                val sample = ((largestSide.toLong() + 1199) / 1200).toInt().coerceAtLeast(1)
                val options = reader.defaultReadParam.apply { setSourceSubsampling(sample, sample, 0, 0) }
                return reader.read(0, options) ?: throw IOException("Invalid image")
            } finally {
                reader.dispose()
            }
        }
    }

    override fun saveCrop(image: BufferedImage, crop: PhotoCrop): File {
        val draft = prepareCapture()
        var written = false
        try {
            val square = BufferedImage(720, 720, BufferedImage.TYPE_INT_RGB)
            val bounds = crop.bounds(image.width, image.height)
            val scale = square.width.toDouble() / bounds.width
            val transform = AffineTransform.getScaleInstance(scale, scale)
            transform.translate(-bounds.left.toDouble(), -bounds.top.toDouble())
            val graphics = square.createGraphics()
            try {
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, square.width, square.height)
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                graphics.drawImage(image, transform, null)
            } finally {
                graphics.dispose()
            }
            written = ImageIO.write(square, "jpg", draft)
            if (!written) throw IOException("Image encoding failed")
            return draft
        } finally {
            if (!written) draft.delete()
        }
    }
}
