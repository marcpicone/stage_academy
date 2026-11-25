package fr.stageacademy.quiz.collection_view

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.elapsedMillis
import kotlin.math.abs
import kotlin.math.atan2

@Composable
internal fun Modifier.secretCircle(onDiscover: (() -> Unit)?): Modifier {
    val discover by rememberUpdatedState(onDiscover)
    if (onDiscover == null) return this
    return pointerInput(Unit) {
        val points = mutableListOf<Offset>()
        val minimumSize = 48.dp.toPx()
        detectDragGestures(
            onDragStart = { start ->
                points.clear()
                points.add(start)
            },
            onDragEnd = {
                if (isSecretCircle(points, minimumSize)) discover?.invoke()
            },
            onDrag = { change, _ -> points.add(change.position) },
        )
    }
}

internal fun isSecretCircle(points: List<Offset>, minimumSize: Float): Boolean {
    if (points.size < 6) return false
    val left = points.minOf { it.x }
    val right = points.maxOf { it.x }
    val top = points.minOf { it.y }
    val bottom = points.maxOf { it.y }
    if (right - left < minimumSize || bottom - top < minimumSize) return false
    if ((points.first() - points.last()).getDistance() > minimumSize * 0.85f) return false
    val center = Offset((left + right) / 2, (top + bottom) / 2)
    val turn = points.zipWithNext().sumOf { (previous, next) ->
        val a = previous - center
        val b = next - center
        atan2((a.x * b.y - a.y * b.x).toDouble(), (a.x * b.x + a.y * b.y).toDouble())
    }
    return abs(turn) >= 5.1
}

@Composable
internal fun Modifier.secretRhythm(onDiscover: (() -> Unit)?): Modifier {
    val discover by rememberUpdatedState(onDiscover)
    if (onDiscover == null) return this
    return pointerInput(Unit) {
        val rhythm = SecretRhythm()
        detectTapGestures(onTap = {
            if (rhythm.tap(elapsedMillis())) discover?.invoke()
        })
    }
}

internal class SecretRhythm {
    private var lastTap: Long? = null
    private var nextBeat = 0

    fun tap(now: Long): Boolean {
        val previous = lastTap
        lastTap = now
        if (previous == null) return false
        if (now - previous in BeatWindows[nextBeat]) {
            nextBeat++
            if (nextBeat == BeatWindows.size) {
                lastTap = null
                nextBeat = 0
                return true
            }
        } else {
            nextBeat = 0
        }
        return false
    }

    private companion object {
        val BeatWindows = listOf(110L..475L, 600L..1_450L, 110L..475L)
    }
}
