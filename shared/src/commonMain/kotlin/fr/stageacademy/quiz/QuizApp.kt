package fr.stageacademy.quiz

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fr.stageacademy.quiz.challenge_view.ChallengePresenter
import fr.stageacademy.quiz.leaderboard_view.LeaderboardPresenter
import fr.stageacademy.quiz.navigation_view.QuizNavigation
import fr.stageacademy.quiz.navigation_view.QuizNavigationPresenter
import fr.stageacademy.quiz.quiz_view.QuizPresenter
import fr.stageacademy.quiz.quiz_view.QuizPresenterSaver
import fr.stageacademy.quiz.quiz_view.quizQuestions
import fr.stageacademy.quiz.settings_view.QuizAppearance
import fr.stageacademy.quiz.shared_view.LocalQuizSound
import fr.stageacademy.quiz.shared_view.QuizSoundPlayer
import fr.stageacademy.quiz.shared_view.rememberQuizSoundPlayer
import fr.stageacademy.quiz.splash_view.SplashScreen
import fr.stageacademy.quiz.ui.theme.QuizTheme

@Composable
fun QuizApp(onDarkThemeChanged: (Boolean) -> Unit = {}) {
    val preferences = rememberQuizPreferences()
    val soundPlayer = rememberQuizSoundPlayer()
    val sounds = remember(preferences, soundPlayer) {
        QuizSoundPlayer { sound ->
            if (preferences.settings().soundEnabled) soundPlayer.play(sound)
        }
    }
    val quiz = rememberSaveable(saver = QuizPresenterSaver) { QuizPresenter(quizQuestions) }
    val challenge = rememberSaveable(sounds, saver = ChallengePresenter.saver(sounds)) {
        ChallengePresenter(sounds = sounds)
    }
    val scope = rememberCoroutineScope()
    val leaderboardStore = rememberLeaderboardStore()
    val leaderboard = remember(leaderboardStore, scope) { LeaderboardPresenter(leaderboardStore, scope) }
    LaunchedEffect(leaderboard) { leaderboard.restore() }
    val profile = rememberProfile(scope, leaderboardStore, leaderboard.accountId)
    val presenter = rememberSaveable(
        quiz, challenge, leaderboard, preferences, sounds,
        saver = QuizNavigationPresenter.saver(quiz, challenge, leaderboard, profile, preferences, sounds),
    ) {
        QuizNavigationPresenter(quiz, challenge, leaderboard, profile, preferences, sounds = sounds)
    }
    SideEffect { presenter.updateProfile(profile) }

    var splashFinished by rememberSaveable { mutableStateOf(false) }
    val darkTheme = when (presenter.settings.appearance) {
        QuizAppearance.System -> isSystemInDarkTheme()
        QuizAppearance.Light -> false
        QuizAppearance.Dark -> true
    }
    SideEffect { onDarkThemeChanged(darkTheme) }
    if (!splashFinished) {
        SplashScreen(darkTheme) { splashFinished = true }
        return
    }
    QuizTheme(darkTheme = darkTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
        ) { innerPadding ->
            CompositionLocalProvider(LocalQuizSound provides sounds) {
                QuizNavigation(presenter, profile, modifier = Modifier.padding(innerPadding))
            }
        }
    }
}
