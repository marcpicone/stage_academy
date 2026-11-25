package fr.stageacademy.quiz.profile_view

import androidx.compose.ui.geometry.Rect

internal data class PhotoCrop(
    val zoom: Float = 1f,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
) {
    fun adjust(width: Int, height: Int, zoom: Float, panX: Float = 0f, panY: Float = 0f): PhotoCrop {
        val current = bounds(width, height)
        val next = copy(
            zoom = zoom.coerceIn(1f, MaxZoom),
            centerX = (current.center.x - panX * current.width) / width,
            centerY = (current.center.y - panY * current.height) / height,
        )
        val clamped = next.bounds(width, height)
        return next.copy(centerX = clamped.center.x / width, centerY = clamped.center.y / height)
    }

    fun bounds(width: Int, height: Int): Rect {
        val side = minOf(width, height) / zoom
        val left = (centerX * width - side / 2f).coerceIn(0f, width - side)
        val top = (centerY * height - side / 2f).coerceIn(0f, height - side)
        return Rect(left, top, left + side, top + side)
    }

    companion object {
        const val MaxZoom = 4f
    }
}
