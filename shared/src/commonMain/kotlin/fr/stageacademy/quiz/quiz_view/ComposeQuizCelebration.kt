package fr.stageacademy.quiz.quiz_view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun ComposeQuizCelebration(celebration: Celebration, modifier: Modifier) {
    val progress = remember(celebration) { Animatable(0f) }
    LaunchedEffect(celebration) {
        val duration = if (celebration == Celebration.CorrectAnswer) 550 else 1_200
        progress.animateTo(1f, tween(duration, easing = LinearEasing))
    }
    Canvas(modifier) {
        val frame = progress.value
        scale(size.width / 128f, size.height / 128f, pivot = Offset.Zero) {
            when (celebration) {
                Celebration.CorrectAnswer -> drawCorrectAnswer(frame)
                Celebration.PerfectScore -> drawPerfectScore(frame)
            }
        }
    }
}

private val Center = Offset(64f, 64f)
private val Green = Color(0xFF087348)
private val Gold = Color(0xFFFFD171)
private val TrophyInk = Color(0xFF533609)
private val Confetti = listOf(Gold, Color(0xFF95E4B8), Color(0xFFF3AEC4), Color(0xFFAEC4FE))

private fun DrawScope.drawCorrectAnswer(frame: Float) {
    scale(popScale(frame), pivot = Center) {
        drawCircle(Green, radius = 48f, center = Center)
        val checkProgress = ((frame - 0.18f) / 0.48f).coerceIn(0f, 1f)
        val start = Offset(41f, 64f)
        val corner = Offset(57f, 80f)
        val end = Offset(87f, 47f)
        if (checkProgress > 0f) {
            val firstSegment = (checkProgress / 0.34f).coerceAtMost(1f)
            drawLine(Color.White, start, start + (corner - start) * firstSegment, 9f, StrokeCap.Round)
        }
        if (checkProgress > 0.34f) {
            val lastSegment = (checkProgress - 0.34f) / 0.66f
            drawLine(Color.White, corner, corner + (end - corner) * lastSegment, 9f, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawPerfectScore(frame: Float) {
    val burst = ((frame - 0.08f) / 0.86f).coerceIn(0f, 1f)
    val opacity = 1f - (burst * burst)
    repeat(12) { index ->
        val angle = index * PI / 6
        val distance = 29f + burst * (if (index % 2 == 0) 27f else 34f)
        val position = Center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * distance
        drawCircle(Confetti[index % Confetti.size], 2f + index % 3, position, alpha = opacity)
    }
    scale(popScale((frame * 1.8f).coerceAtMost(1f)), pivot = Center) {
        drawCircle(Gold, radius = 36f, center = Center)
        drawPath(TrophyCup, TrophyInk)
        drawLine(TrophyInk, Offset(64f, 69f), Offset(64f, 81f), 5f, StrokeCap.Round)
        drawRect(TrophyInk, Offset(53f, 80f), Size(22f, 5f))
    }
}

private fun popScale(frame: Float): Float =
    if (frame < 0.42f) 0.52f + 0.57f * frame / 0.42f else 1.09f - 0.09f * (frame - 0.42f) / 0.58f

private val TrophyCup = Path().apply {
    moveTo(51f, 46f)
    lineTo(77f, 46f)
    lineTo(77f, 52f)
    lineTo(84f, 52f)
    quadraticTo(84f, 64f, 74f, 66f)
    quadraticTo(71f, 72f, 64f, 73f)
    quadraticTo(57f, 72f, 54f, 66f)
    quadraticTo(44f, 64f, 44f, 52f)
    lineTo(51f, 52f)
    close()
}
