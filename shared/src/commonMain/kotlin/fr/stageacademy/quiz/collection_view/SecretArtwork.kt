package fr.stageacademy.quiz.collection_view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.sp

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
        QuizSecret.Bicorne -> Color(0xFFBBCAEE)
        QuizSecret.ImperialBee -> Color(0xFFF4D178)
        QuizSecret.Crosshair -> Color(0xFFACD6D0)
        QuizSecret.Clutch -> Color(0xFFF4C092)
        QuizSecret.NeverFadeAway -> Color(0xFFB1B2FF)
        QuizSecret.NightCitySignal -> Color(0xFFFF9ADA)
        QuizSecret.RubberDuck -> Color(0xFFFFDF89)
        QuizSecret.FloppyDisk -> Color(0xFFC0E0E5)
        QuizSecret.NeonJellyfish -> Color(0xFFB1E9F5)
        QuizSecret.ClockworkOwl -> Color(0xFFE7BF98)
        QuizSecret.GoldenCartridge -> Color(0xFFFFCE66)
        QuizSecret.PocketGalaxy -> Color(0xFFC5B6FF)
        else -> Color.hsv(((secret.ordinal - 12) * 34f) % 360f, 0.28f, 0.96f)
    }
    Box(modifier.clip(RoundedCornerShape(16.dp)).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.linearGradient(listOf(accent, Color.White.copy(alpha = 0.86f))))
            scale(size.minDimension / 80f, pivot = Offset.Zero) {
                drawCircle(Ink.copy(alpha = 0.09f), 29f, Offset(40f, 40f), style = Stroke(1f))
                drawCircle(Ink.copy(alpha = 0.24f), 2f, Offset(65f, 16f))
                drawCircle(Ink.copy(alpha = 0.24f), 1.5f, Offset(15f, 61f))
                drawSecret(secret)
            }
        }
        secret.glyph?.let { Text(it, color = Ink, fontSize = 28.sp) }
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
        QuizSecret.Carousel -> {
            for (x in listOf(15f, 32f, 49f)) {
                drawRoundRect(Ink, Offset(x, 30f), Size(16f, 22f), CornerRadius(3f), style = Outline)
            }
            trace(25f, 24f, 54f, 24f, 48f, 19f)
            trace(55f, 58f, 26f, 58f, 32f, 63f)
        }
        QuizSecret.Chameleon -> {
            drawOval(Ink, Offset(23f, 28f), Size(29f, 20f), style = Outline)
            drawCircle(Ink, 9f, Offset(54f, 29f), style = Outline)
            drawCircle(Ink, 2f, Offset(56f, 27f))
            drawArc(Ink, 38f, 305f, false, Offset(14f, 37f), Size(20f, 20f), style = Outline)
            trace(36f, 47f, 34f, 54f, 42f, 54f)
        }
        QuizSecret.Boomerang -> {
            trace(27f, 59f, 22f, 27f, 27f, 23f, 60f, 24f, 63f, 32f, 36f, 37f, 35f, 58f, 27f, 59f)
            trace(51f, 24f, 53f, 34f)
        }
        QuizSecret.Bicorne -> {
            val hat = Path().apply {
                moveTo(13f, 47f)
                quadraticTo(23f, 47f, 28f, 35f)
                quadraticTo(40f, 14f, 52f, 35f)
                quadraticTo(57f, 47f, 67f, 47f)
                quadraticTo(62f, 57f, 40f, 55f)
                quadraticTo(18f, 57f, 13f, 47f)
                close()
            }
            drawPath(hat, Ink)
            drawCircle(Color(0xFFB5484C), 7f, Offset(40f, 41f))
            drawCircle(Color.White, 4.5f, Offset(40f, 41f))
            drawCircle(Color(0xFF3D64A7), 2.5f, Offset(40f, 41f))
        }
        QuizSecret.Ice -> {
            trace(40f, 18f, 61f, 29f, 61f, 51f, 40f, 63f, 19f, 51f, 19f, 29f, 40f, 18f)
            trace(19f, 29f, 40f, 41f, 61f, 29f)
            trace(40f, 41f, 40f, 63f)
            trace(46f, 49f, 54f, 44f)
        }
        QuizSecret.Cartographer -> {
            trace(16f, 27f, 31f, 22f, 49f, 29f, 64f, 23f, 64f, 54f, 49f, 60f, 31f, 53f, 16f, 58f, 16f, 27f)
            trace(31f, 22f, 31f, 53f)
            trace(49f, 29f, 49f, 60f)
            drawCircle(Ink, 3f, Offset(40f, 42f))
        }
        QuizSecret.Trapdoor -> {
            trace(17f, 48f, 30f, 33f, 52f, 32f, 64f, 47f, 51f, 58f, 17f, 48f)
            trace(30f, 33f, 34f, 18f, 55f, 20f, 52f, 32f)
            trace(39f, 25f, 47f, 26f)
            trace(45f, 39f, 45f, 49f, 41f, 45f)
        }
        QuizSecret.Zigzag -> {
            trace(16f, 54f, 27f, 27f, 39f, 53f, 50f, 27f, 62f, 51f)
            drawCircle(Ink, 3f, Offset(62f, 51f))
        }
        QuizSecret.ImperialBee -> {
            drawOval(Color(0xFFE1AE3A), Offset(30f, 35f), Size(20f, 24f))
            drawOval(Ink, Offset(30f, 35f), Size(20f, 24f), style = Outline)
            drawOval(Ink, Offset(20f, 31f), Size(14f, 13f), style = Outline)
            drawOval(Ink, Offset(46f, 31f), Size(14f, 13f), style = Outline)
            trace(30f, 44f, 50f, 44f)
            trace(32f, 51f, 48f, 51f)
            drawCircle(Ink, 5f, Offset(40f, 31f))
            trace(34f, 24f, 32f, 17f, 40f, 21f, 48f, 17f, 46f, 24f)
        }
        QuizSecret.Comet -> {
            drawCircle(Ink, 9f, Offset(51f, 31f), style = Outline)
            trace(44f, 35f, 20f, 46f)
            trace(51f, 40f, 29f, 57f)
            trace(43f, 27f, 27f, 35f)
        }
        QuizSecret.Crosshair -> {
            drawCircle(Ink, 21f, Offset(40f, 40f), style = Outline)
            drawCircle(Color(0xFF087348), 4.5f, Offset(40f, 40f))
            trace(40f, 14f, 40f, 27f)
            trace(40f, 53f, 40f, 66f)
            trace(14f, 40f, 27f, 40f)
            trace(53f, 40f, 66f, 40f)
        }
        QuizSecret.Clutch -> {
            drawRoundRect(Ink, Offset(17f, 24f), Size(46f, 32f), CornerRadius(8f), style = Outline)
            trace(30f, 33f, 25f, 33f, 25f, 47f, 31f, 47f, 31f, 33f)
            drawCircle(Ink, 1.6f, Offset(39f, 37f))
            drawCircle(Ink, 1.6f, Offset(39f, 44f))
            trace(48f, 35f, 52f, 32f, 52f, 48f)
        }
        QuizSecret.NeverFadeAway -> {
            drawCircle(Ink, 22f, Offset(40f, 40f), style = Outline)
            drawCircle(Ink.copy(alpha = 0.35f), 17f, Offset(40f, 40f), style = Stroke(1f))
            drawCircle(Ink.copy(alpha = 0.35f), 12f, Offset(40f, 40f), style = Stroke(1f))
            trace(21f, 40f, 28f, 40f, 34f, 30f, 40f, 51f, 45f, 37f, 51f, 40f, 59f, 40f)
        }
        QuizSecret.NightCitySignal -> {
            trace(16f, 57f, 16f, 44f, 27f, 44f, 27f, 33f, 37f, 33f, 37f, 58f)
            trace(43f, 58f, 43f, 39f, 51f, 39f, 51f, 30f, 58f, 30f, 58f, 47f, 65f, 47f, 65f, 58f)
            drawCircle(Color(0xFF9B176E), 3f, Offset(40f, 20f))
            drawArc(Ink, 210f, 120f, false, Offset(32f, 12f), Size(16f, 16f), style = Stroke(2f))
            drawArc(Ink, 210f, 120f, false, Offset(26f, 6f), Size(28f, 28f), style = Stroke(2f))
        }
        QuizSecret.RubberDuck -> {
            drawOval(Ink, Offset(17f, 38f), Size(41f, 22f), style = Outline)
            drawCircle(Ink, 12f, Offset(49f, 35f), style = Outline)
            drawCircle(Ink, 2f, Offset(53f, 32f))
            trace(60f, 36f, 67f, 40f, 59f, 43f)
        }
        QuizSecret.FloppyDisk -> {
            trace(21f, 20f, 52f, 20f, 60f, 28f, 60f, 60f, 21f, 60f, 21f, 20f)
            trace(30f, 20f, 30f, 34f, 49f, 34f, 49f, 20f)
            drawRoundRect(Ink, Offset(29f, 43f), Size(23f, 17f), CornerRadius(2f), style = Outline)
        }
        QuizSecret.NeonJellyfish -> {
            drawArc(Ink, 180f, 180f, false, Offset(20f, 20f), Size(40f, 36f), style = Outline)
            trace(20f, 38f, 60f, 38f)
            trace(29f, 39f, 31f, 47f, 27f, 55f, 30f, 62f)
            trace(40f, 39f, 42f, 47f, 38f, 55f, 41f, 64f)
            trace(51f, 39f, 53f, 47f, 49f, 55f, 52f, 62f)
        }
        QuizSecret.ClockworkOwl -> {
            drawOval(Ink, Offset(22f, 24f), Size(36f, 38f), style = Outline)
            trace(25f, 30f, 24f, 20f, 33f, 25f)
            trace(47f, 25f, 56f, 20f, 55f, 30f)
            drawCircle(Ink, 5f, Offset(32f, 37f), style = Outline)
            drawCircle(Ink, 5f, Offset(48f, 37f), style = Outline)
            trace(36f, 48f, 40f, 51f, 44f, 48f)
        }
        QuizSecret.GoldenCartridge -> {
            drawRoundRect(Ink, Offset(21f, 23f), Size(38f, 34f), CornerRadius(5f), style = Outline)
            trace(27f, 47f, 53f, 47f)
            trace(40f, 29f, 43f, 34f, 49f, 36f, 43f, 38f, 40f, 43f, 37f, 38f, 31f, 36f, 37f, 34f, 40f, 29f)
        }
        QuizSecret.PocketGalaxy -> {
            drawCircle(Ink, 8f, Offset(40f, 40f))
            rotate(-28f, pivot = Offset(40f, 40f)) {
                drawOval(Ink, Offset(15f, 30f), Size(50f, 20f), style = Outline)
            }
            drawCircle(Ink, 3f, Offset(25f, 26f))
            drawCircle(Ink, 2f, Offset(57f, 51f))
            trace(53f, 17f, 53f, 25f)
            trace(49f, 21f, 57f, 21f)
        }
        else -> Unit
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
