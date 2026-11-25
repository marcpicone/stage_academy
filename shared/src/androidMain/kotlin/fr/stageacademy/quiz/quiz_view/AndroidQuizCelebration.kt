package fr.stageacademy.quiz.quiz_view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import fr.stageacademy.quiz.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Composable
@OptIn(ExperimentalResourceApi::class)
internal actual fun QuizCelebration(celebration: Celebration, modifier: Modifier) {
    val fileName = when (celebration) {
        Celebration.CorrectAnswer -> "correct_answer_celebration.json"
        Celebration.PerfectScore -> "perfect_score_celebration.json"
    }
    val asset = remember(fileName) {
        runCatching { Res.getUri("files/$fileName").removePrefix("file:///android_asset/") }.getOrNull()
    }
    if (asset == null) {
        ComposeQuizCelebration(celebration, modifier)
        return
    }
    val result = rememberLottieComposition(LottieCompositionSpec.Asset(asset))
    if (result.isFailure) {
        ComposeQuizCelebration(celebration, modifier)
        return
    }
    val composition = result.value
    val progress = animateLottieCompositionAsState(composition, iterations = 1)
    LottieAnimation(composition, progress = { progress.value }, modifier = modifier)
}
