package fr.stageacademy.quiz.navigation_view

import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.PlatformBackHandler
import fr.stageacademy.quiz.profile_view.ProfilePanel
import fr.stageacademy.quiz.profile_view.ProfileAccountCard
import fr.stageacademy.quiz.about_view.AboutScreen
import fr.stageacademy.quiz.challenge_view.ChallengeActions
import fr.stageacademy.quiz.challenge_view.ChallengeScreen
import fr.stageacademy.quiz.challenge_view.PlayModeMenu
import fr.stageacademy.quiz.challenge_view.title
import fr.stageacademy.quiz.collection_view.QuizSecret
import fr.stageacademy.quiz.collection_view.SecretCollection
import fr.stageacademy.quiz.collection_view.SecretCratesScreen
import fr.stageacademy.quiz.collection_view.SecretEntryDialog
import fr.stageacademy.quiz.menu_view.LeaveQuizDialog
import fr.stageacademy.quiz.menu_view.HomeTabs
import fr.stageacademy.quiz.menu_view.PauseMenu
import fr.stageacademy.quiz.menu_view.WelcomePanel
import fr.stageacademy.quiz.leaderboard_view.LeaderboardScreen
import fr.stageacademy.quiz.leaderboard_view.PlayerDialog
import fr.stageacademy.quiz.quiz_view.QuestionActions
import fr.stageacademy.quiz.quiz_view.QuestionPanel
import fr.stageacademy.quiz.quiz_view.QuizHeader
import fr.stageacademy.quiz.quiz_view.ResultPanel
import fr.stageacademy.quiz.settings_view.SettingsMenu
import fr.stageacademy.quiz.shared_view.MenuButton
import fr.stageacademy.quiz.shared_view.QuizActionBar

@Composable
internal fun QuizNavigation(
    presenter: QuizNavigationPresenter,
    profile: ProfilePanel,
    modifier: Modifier = Modifier,
) {
    val navigation = presenter.navigation
    val page = navigation.page
    val state = presenter.round
    val settings = presenter.settings
    val foundSecrets = presenter.foundSecrets
    val challenge = presenter.challenge
    val challengeGame = challenge.state
    val leaderboard = presenter.leaderboard
    val guestName = stringResource(Res.string.leaderboard_guest)
    val currentPlayer = navigation.playerName ?: guestName
    val question = state.question
    val questionNumber = state.questionIndex + 1
    val questionProgress = animateFloatAsState(
        targetValue = if (question == null) 1f else questionNumber.toFloat() / state.questionCount,
        animationSpec = tween(durationMillis = 350),
        label = "Quiz progress",
    )
    val challengeProgress = animateFloatAsState(
        targetValue = challengeGame?.let { (it.index + 1f) / it.order.size } ?: 0f,
        animationSpec = tween(durationMillis = 350),
        label = "Challenge progress",
    )
    val colors = MaterialTheme.colorScheme

    PlatformBackHandler(enabled = page != QuizPage.Start, onBack = presenter::goBack)
    LaunchedEffect(page) {
        if (page == QuizPage.Leaderboard) leaderboard.load()
    }
    LaunchedEffect(leaderboard.accountId) { presenter.collections.useAccount(leaderboard.accountId) }
    LaunchedEffect(page, leaderboard.accountId) {
        if (leaderboard.accountId != null && (page == QuizPage.Collection || page == QuizPage.Crates)) {
            while (true) {
                presenter.collections.refresh()
                delay(15_000)
            }
        }
    }
    LaunchedEffect(challengeGame) { presenter.checkChallengeDiscovery() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.background, colors.primaryContainer))),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().fillMaxHeight()) {
            if (page == QuizPage.Start || page == QuizPage.Profile) {
                HomeTabs(
                    profileSelected = page == QuizPage.Profile,
                    onHome = presenter::openHome,
                    onProfile = presenter::openProfile,
                )
            }
            if (page == QuizPage.Playing) {
                QuizHeader(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    onPause = if (question != null && !leaderboard.recording) presenter::pauseNormal else null,
                )
            } else if (page == QuizPage.Challenges && challengeGame != null) {
                QuizHeader(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    title = stringResource(challengeGame.mode.title),
                    onPause = presenter::goBack,
                    actionLabel = stringResource(Res.string.back),
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = if (page == QuizPage.Start) Alignment.Center else Alignment.TopCenter,
            ) {
                if (page == QuizPage.Profile) {
                    key(profile) {
                        profile.Content(isProfileSelected = { presenter.navigation.page == QuizPage.Profile }) {
                            ProfileAccountCard(leaderboard)
                        }
                    }
                } else key(page, state.questionIndex, challengeGame?.let { Triple(it.mode, it.index, it.end) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        when (page) {
                            QuizPage.Start -> WelcomePanel(state.questionCount, presenter::discoverSecret)
                            QuizPage.Profile -> Unit
                            QuizPage.Paused -> PauseMenu(
                                state = state,
                                keyAvailable = presenter.pauseSecretAvailable,
                                onSettings = presenter::openSettings,
                                onCollection = presenter::openCollection,
                                onProfile = presenter::openProfile,
                                onLeave = presenter::requestLeave,
                                onDiscover = presenter::discoverSecret,
                            )
                            QuizPage.Settings -> SettingsMenu(
                                settings = settings,
                                onSettingsChange = presenter::updateSettings,
                                onAppearanceSelected = presenter::selectAppearance,
                                onDiscover = presenter::discoverSecret,
                                onAbout = presenter::openAbout,
                                onLeaderboard = presenter::openLeaderboard,
                            )
                            QuizPage.About -> AboutScreen(
                                onCircle = (presenter::discoverCrosshair).takeUnless { QuizSecret.Crosshair in foundSecrets },
                                onRhythm = (presenter::discoverRhythm).takeUnless { QuizSecret.NeverFadeAway in foundSecrets },
                            )
                            QuizPage.Challenges -> {
                                if (challengeGame == null) {
                                    val canResumeNormal = presenter.canResumeNormal
                                    PlayModeMenu(
                                        canResumeNormal = canResumeNormal,
                                        onNormal = presenter::chooseNormal,
                                        onChallenge = presenter::startChallenge,
                                    )
                                } else {
                                    ChallengeScreen(
                                        challenge,
                                        settings.horizontalAnswers,
                                        progress = { challengeProgress.value },
                                        onAnswer = presenter::answerChallenge,
                                    )
                                }
                            }
                            QuizPage.Leaderboard -> LeaderboardScreen(leaderboard, presenter::openProfile)
                            QuizPage.Collection -> {
                                CollectionStatus(presenter)
                                SecretCollection(
                                    secretFloats = presenter.secretFloats,
                                    onOpen = presenter::openSecret,
                                    onDiscover = presenter::discoverSecret,
                                    onCrates = presenter::openCrates,
                                )
                            }
                            QuizPage.Crates -> {
                                CollectionStatus(presenter)
                                SecretCratesScreen(
                                    presenter.collection,
                                    presenter::buyCrate,
                                    purchaseEnabled = !presenter.collections.syncing && !presenter.collections.unavailable,
                                )
                            }
                            QuizPage.Playing -> {
                                if (question == null) {
                                    ResultPanel(state.score, state.questionCount, presenter::discoverSecret)
                                    Text(stringResource(Res.string.leaderboard_result_player, currentPlayer))
                                } else {
                                    QuestionPanel(
                                        question = question,
                                        questionNumber = questionNumber,
                                        questionCount = state.questionCount,
                                        progress = { questionProgress.value },
                                        score = state.score,
                                        selectedChoice = state.selectedChoice,
                                        isCorrect = state.isCorrect,
                                        horizontalAnswers = settings.horizontalAnswers,
                                        onChoice = presenter::answerNormal,
                                        onDiscover = presenter::discoverSecret,
                                        onCarouselTrip = if (QuizSecret.Carousel !in foundSecrets) {
                                            presenter::discoverCarousel
                                        } else null,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            NavigationActions(presenter, guestName)
        }
    }
    NavigationDialogs(presenter)
}

@Composable
private fun CollectionStatus(presenter: QuizNavigationPresenter) {
    if (presenter.leaderboard.accountId == null) return
    val collection = presenter.collections
    when {
        collection.syncing -> Text(stringResource(Res.string.collection_syncing))
        collection.unavailable -> {
            Text(stringResource(Res.string.collection_offline))
            TextButton(onClick = collection::refresh) { Text(stringResource(Res.string.collection_retry)) }
        }
    }
}

@Composable
private fun NavigationActions(presenter: QuizNavigationPresenter, guestName: String) {
    val state = presenter.round
    val question = state.question
    val settings = presenter.settings
    val challenge = presenter.challenge
    val challengeGame = challenge.state
    val leaderboard = presenter.leaderboard
    val page = presenter.navigation.page
    when (page) {
        QuizPage.Start -> QuizActionBar(
            label = stringResource(Res.string.play),
            onClick = presenter::openModes,
            secondaryAction = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuButton(stringResource(Res.string.secret_collection), presenter::openCollection, Modifier.weight(1f))
                    MenuButton(stringResource(Res.string.settings), presenter::openSettings, Modifier.weight(1f))
                }
            },
        )
        QuizPage.Profile -> Unit
        QuizPage.Paused -> QuizActionBar(
            label = stringResource(Res.string.resume_quiz),
            onClick = presenter::resumeNormal,
        )
        QuizPage.Settings, QuizPage.Collection, QuizPage.Crates, QuizPage.About, QuizPage.Leaderboard -> QuizActionBar(
            label = stringResource(Res.string.back),
            onClick = presenter::goBack,
        )
        QuizPage.Challenges -> {
            if (challengeGame == null) {
                QuizActionBar(label = stringResource(Res.string.back), onClick = presenter::goBack)
            } else {
                ChallengeActions(challenge)
            }
        }
        QuizPage.Playing -> {
            if (question == null) {
                QuizActionBar(
                    label = stringResource(Res.string.restart_quiz),
                    onClick = presenter::restartNormal,
                    secondaryAction = {
                        MenuButton(stringResource(Res.string.main_menu), presenter::openHome)
                    },
                )
            } else {
                QuestionActions(
                    selectedChoice = state.selectedChoice,
                    isCorrect = state.isCorrect,
                    correctAnswer = question.correctAnswer.takeIf { settings.showCorrections },
                    isLastQuestion = state.questionIndex + 1 == state.questionCount,
                    recording = leaderboard.recording,
                    onContinue = { presenter.continueNormal(guestName) },
                )
            }
        }
    }
}

@Composable
private fun NavigationDialogs(presenter: QuizNavigationPresenter) {
    val navigation = presenter.navigation
    val leaderboard = presenter.leaderboard
    if (navigation.choosingPlayer) {
        PlayerDialog(presenter.suggestedPlayer, leaderboard.rankings, presenter::cancelPlayerChoice, presenter::startNormal)
    }
    if (navigation.leaderboardError) {
        AlertDialog(
            onDismissRequest = presenter::dismissLeaderboardError,
            title = { Text(stringResource(Res.string.leaderboard)) },
            text = { Text(stringResource(Res.string.leaderboard_save_error)) },
            confirmButton = {
                TextButton(onClick = presenter::dismissLeaderboardError) { Text(stringResource(Res.string.close)) }
            },
        )
    }

    if (navigation.confirmLeave) {
        LeaveQuizDialog(
            onCancel = presenter::cancelLeave,
            onLeave = presenter::leaveNormal,
        )
    }

    navigation.selectedSecret?.let { secret ->
        SecretEntryDialog(secret, presenter.secretFloats, presenter::closeSecret, presenter::discoverSecret)
    }
}
