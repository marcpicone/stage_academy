package fr.stageacademy.quiz.shared_view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

internal enum class QuizSound(val fileName: String) {
    Click("quiz_click.wav"),
    Correct("quiz_correct.wav"),
    Incorrect("quiz_incorrect.wav"),
    Secret("quiz_secret.wav"),
    Perfect("quiz_perfect.wav"),
}

internal fun interface QuizSoundPlayer {
    fun play(sound: QuizSound)
}

internal val LocalQuizSound = staticCompositionLocalOf<QuizSoundPlayer> { QuizSoundPlayer {} }

@Composable
internal expect fun rememberQuizSoundPlayer(): QuizSoundPlayer

@Composable
internal fun withQuizClickSound(action: () -> Unit): () -> Unit {
    val sounds = LocalQuizSound.current
    return {
        sounds.play(QuizSound.Click)
        action()
    }
}
