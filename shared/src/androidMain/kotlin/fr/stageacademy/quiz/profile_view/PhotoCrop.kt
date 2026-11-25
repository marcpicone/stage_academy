package fr.stageacademy.quiz.profile_view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

internal fun PhotoCrop.draw(canvas: Canvas, image: Bitmap, size: Float, paint: Paint) {
    val crop = bounds(image.width, image.height)
    val scale = size / crop.width
    val destination = RectF(
        -crop.left * scale, -crop.top * scale,
        (image.width - crop.left) * scale, (image.height - crop.top) * scale,
    )
    canvas.drawBitmap(image, null, destination, paint)
}
