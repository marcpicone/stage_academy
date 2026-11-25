package fr.stageacademy.quiz.splash_view

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.splash_loading
import fr.stageacademy.quiz.resources.splash_subtitle
import fr.stageacademy.quiz.resources.splash_title
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val MinimumSplashMillis = 1_500L
private val LogoGreen = Color(0xFF00CD5E)
private val SplashLight = Color(0xFFFCFDFC)
private val SplashDark = Color(0xFF080D0B)

@Composable
internal fun SplashScreen(darkTheme: Boolean, onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(MinimumSplashMillis)
        finish()
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .background(if (darkTheme) SplashDark else SplashLight)
            .safeDrawingPadding(),
    ) {
        val designWidth = minOf(maxWidth, maxHeight * 0.65f, 420.dp)
        val textColor = if (darkTheme) SplashLight else SplashDark
        Column(
            modifier = Modifier.align(Alignment.Center).offset(y = -maxHeight * 0.035f)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SplashLogo(Modifier.width(designWidth * 0.51f).aspectRatio(150f / 96f))
            Text(
                text = stringResource(Res.string.splash_title),
                modifier = Modifier.padding(top = designWidth * 0.045f),
                color = textColor,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = (designWidth.value * 0.151f).sp,
                lineHeight = (designWidth.value * 0.165f).sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.splash_subtitle),
                color = textColor,
                fontFamily = FontFamily.SansSerif,
                fontSize = (designWidth.value * 0.055f).sp,
                lineHeight = (designWidth.value * 0.071f).sp,
                textAlign = TextAlign.Center,
            )
        }
        LoadingDots(Modifier.align(Alignment.BottomCenter).padding(bottom = maxHeight * 0.13f))
    }
}

@Composable
private fun SplashLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 150f, size.height / 96f, pivot = Offset.Zero) {
            drawPath(LeftBracket, LogoGreen)
            drawPath(RightBracket, LogoGreen)
            drawPath(QuestionHook, LogoGreen)
            drawCircle(LogoGreen, radius = 7.4f, center = Offset(75f, 72f))
        }
    }
}

private val LeftBracket = Path().apply {
    moveTo(47f, 0f)
    lineTo(47f, 22f)
    lineTo(22f, 46f)
    quadraticTo(19f, 48f, 22f, 51f)
    lineTo(47f, 75f)
    lineTo(47f, 96f)
    lineTo(3f, 55f)
    quadraticTo(-2f, 49f, 2f, 43f)
    close()
}

private val RightBracket = Path().apply {
    moveTo(103f, 0f)
    lineTo(148f, 43f)
    quadraticTo(152f, 49f, 147f, 55f)
    lineTo(103f, 96f)
    lineTo(103f, 75f)
    lineTo(128f, 51f)
    quadraticTo(131f, 48f, 128f, 46f)
    lineTo(103f, 22f)
    close()
}

private val QuestionHook = Path().apply {
    moveTo(54f, 35f)
    cubicTo(56f, 23f, 63f, 16f, 75f, 16f)
    cubicTo(87f, 16f, 96f, 23f, 96f, 34f)
    cubicTo(96f, 42f, 91f, 46f, 86f, 50f)
    cubicTo(82f, 53f, 81f, 56f, 81f, 61f)
    lineTo(68f, 61f)
    cubicTo(68f, 51f, 70f, 47f, 76f, 42f)
    cubicTo(81f, 38f, 83f, 36f, 83f, 33f)
    cubicTo(83f, 29f, 80f, 27f, 75f, 27f)
    cubicTo(70f, 27f, 67f, 30f, 67f, 36f)
    close()
}

@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "Splash loading")
    val loadingLabel = stringResource(Res.string.splash_loading)
    Row(
        modifier = modifier.semantics { contentDescription = loadingLabel },
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        repeat(3) { index ->
            val pulse by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(430, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 135),
                ),
                label = "Loading dot",
            )
            Box(
                Modifier.size(9.dp)
                    .graphicsLayer {
                        alpha = 0.48f + 0.52f * pulse
                        translationY = -2.dp.toPx() * pulse
                    }
                    .background(LogoGreen, CircleShape),
            )
        }
    }
}
