package fr.stageacademy.quiz.collection_view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

@Composable
internal fun SecretArtwork(secret: QuizSecret, modifier: Modifier = Modifier) {
    val accent = when (secret) {
        QuizSecret.Bug -> Color(0xFFBDE88B)
        QuizSecret.Semicolon -> Color(0xFFC8CBFA)
        QuizSecret.Calculator -> Color(0xFF9DD9D1)
        QuizSecret.Music -> Color(0xFFF5AECF)
        QuizSecret.Compass -> Color(0xFF9ECFF4)
        QuizSecret.Firefly -> Color(0xFFE7EF99)
        QuizSecret.Hourglass -> Color(0xFFF5CD91)
        QuizSecret.Balance -> Color(0xFFF4AF9D)
        QuizSecret.Star -> Color(0xFFFBE383)
        QuizSecret.Key -> Color(0xFFC5DDA9)
        QuizSecret.Rocket -> Color(0xFFDBBFF8)
        QuizSecret.Constellation -> Color(0xFFA9B9F5)
    }
    Canvas(modifier.clip(RoundedCornerShape(16.dp)).clearAndSetSemantics {}) {
        drawRect(Brush.linearGradient(listOf(accent, Color.White.copy(alpha = 0.86f))))
        scale(size.minDimension / 80f, pivot = Offset.Zero) {
            drawCircle(Ink.copy(alpha = 0.09f), 29f, Offset(40f, 40f), style = Stroke(1f))
            drawCircle(Ink.copy(alpha = 0.24f), 2f, Offset(65f, 16f))
            drawCircle(Ink.copy(alpha = 0.24f), 1.5f, Offset(15f, 61f))
            drawSecret(secret)
        }
    }
}

private val Ink = Color(0xFF223C37)
private val Outline = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.drawSecret(secret: QuizSecret) {
    when (secret) {
        QuizSecret.Bug -> {
            drawOval(Ink, Offset(29f, 27f), Size(22f, 31f), style = Outline)
            trace(40f, 35f, 40f, 57f)
            trace(31f, 29f, 27f, 23f)
            trace(49f, 29f, 53f, 23f)
            trace(22f, 36f, 29f, 39f, 51f, 39f, 58f, 36f)
            trace(22f, 51f, 29f, 48f, 51f, 48f, 58f, 51f)
        }
        QuizSecret.Semicolon -> {
            drawCircle(Ink, 5f, Offset(41f, 28f))
            drawCircle(Ink, 6f, Offset(41f, 44f))
            trace(45f, 45f, 43f, 52f, 36f, 57f)
        }
        QuizSecret.Calculator -> {
            drawRoundRect(Ink, Offset(24f, 20f), Size(32f, 40f), CornerRadius(5f), style = Outline)
            drawRoundRect(Ink, Offset(31f, 27f), Size(18f, 7f), CornerRadius(2f))
            for (x in listOf(33f, 40f, 47f)) {
                drawCircle(Ink, 2f, Offset(x, 43f))
                drawCircle(Ink, 2f, Offset(x, 51f))
            }
        }
        QuizSecret.Music -> {
            trace(34f, 50f, 34f, 28f, 55f, 23f, 55f, 45f)
            trace(34f, 34f, 55f, 29f)
            drawOval(Ink, Offset(23f, 46f), Size(12f, 9f))
            drawOval(Ink, Offset(44f, 41f), Size(12f, 9f))
        }
        QuizSecret.Compass -> {
            drawCircle(Ink, 21f, Offset(40f, 40f), style = Outline)
            trace(49f, 30f, 44f, 44f, 31f, 50f, 36f, 36f, 49f, 30f, 40f, 40f)
        }
        QuizSecret.Firefly -> {
            drawOval(Ink, Offset(33f, 31f), Size(14f, 26f), style = Outline)
            drawOval(Ink, Offset(23f, 31f), Size(12f, 13f), style = Outline)
            drawOval(Ink, Offset(45f, 31f), Size(12f, 13f), style = Outline)
            drawCircle(Ink, 3f, Offset(40f, 25f))
            trace(27f, 52f, 24f, 56f)
            trace(53f, 52f, 56f, 56f)
        }
        QuizSecret.Hourglass -> {
            trace(24f, 22f, 56f, 22f)
            trace(24f, 58f, 56f, 58f)
            trace(28f, 22f, 30f, 31f, 40f, 40f, 50f, 49f, 52f, 58f)
            trace(52f, 22f, 50f, 31f, 40f, 40f, 30f, 49f, 28f, 58f)
            trace(34f, 53f, 40f, 48f, 46f, 53f, 34f, 53f)
        }
        QuizSecret.Balance -> {
            trace(40f, 23f, 40f, 57f, 30f, 57f, 50f, 57f)
            trace(23f, 30f, 57f, 30f)
            trace(23f, 30f, 16f, 45f, 30f, 45f, 23f, 30f)
            trace(57f, 30f, 50f, 45f, 64f, 45f, 57f, 30f)
        }
        QuizSecret.Star -> trace(40f, 19f, 46f, 33f, 61f, 34f, 50f, 44f, 54f, 59f, 40f, 51f,
            26f, 59f, 30f, 44f, 19f, 34f, 34f, 33f, 40f, 19f)
        QuizSecret.Key -> {
            drawCircle(Ink, 10f, Offset(31f, 32f), style = Outline)
            trace(38f, 39f, 55f, 56f, 59f, 52f)
            trace(49f, 50f, 53f, 46f)
        }
        QuizSecret.Rocket -> rotate(30f, pivot = Offset(40f, 40f)) {
            trace(40f, 17f, 48f, 26f, 49f, 48f, 31f, 48f, 32f, 26f, 40f, 17f)
            drawCircle(Ink, 4f, Offset(40f, 34f), style = Outline)
            trace(31f, 40f, 25f, 51f, 31f, 49f)
            trace(49f, 40f, 55f, 51f, 49f, 49f)
            trace(36f, 54f, 40f, 63f, 44f, 54f)
        }
        QuizSecret.Constellation -> {
            trace(20f, 48f, 30f, 28f, 43f, 38f, 58f, 23f, 56f, 54f, 43f, 38f)
            listOf(Offset(20f, 48f), Offset(30f, 28f), Offset(43f, 38f), Offset(58f, 23f), Offset(56f, 54f))
                .forEach { drawCircle(Ink, 3.5f, it) }
        }
    }
}

private fun DrawScope.trace(vararg coordinates: Float) {
    val path = Path().apply {
        moveTo(coordinates[0], coordinates[1])
        for (index in 2 until coordinates.size step 2) {
            lineTo(coordinates[index], coordinates[index + 1])
        }
    }
    drawPath(path, Ink, style = Outline)
}
