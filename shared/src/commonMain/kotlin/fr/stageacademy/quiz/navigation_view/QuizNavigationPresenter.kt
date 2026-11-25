package fr.stageacademy.quiz.navigation_view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import fr.stageacademy.quiz.QuizPreferences
import fr.stageacademy.quiz.challenge_view.ChallengeMode
import fr.stageacademy.quiz.challenge_view.ChallengePresenter
import fr.stageacademy.quiz.collection_view.QuizSecret
import fr.stageacademy.quiz.collection_view.CollectionPresenter
import fr.stageacademy.quiz.collection_view.SecretCrate
import fr.stageacademy.quiz.collection_view.SecretDiscovery
import fr.stageacademy.quiz.collection_view.SecretFloat
import fr.stageacademy.quiz.leaderboard_view.LeaderboardPresenter
import fr.stageacademy.quiz.profile_view.ProfileIdentity
import fr.stageacademy.quiz.quiz_view.QuizContract
import fr.stageacademy.quiz.settings_view.QuizAppearance
import fr.stageacademy.quiz.settings_view.QuizSettings
import fr.stageacademy.quiz.shared_view.QuizSound
import fr.stageacademy.quiz.shared_view.QuizSoundPlayer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal enum class QuizPage { Start, Profile, Playing, Paused, Settings, Collection, Crates, About, Leaderboard, Challenges }

@OptIn(ExperimentalUuidApi::class)
private fun newRoundId() = Uuid.random().toString()

internal data class QuizNavigationState(
    val page: QuizPage = QuizPage.Start,
    val menuOrigin: QuizPage = QuizPage.Start,
    val playerName: String? = null,
    val roundId: String = newRoundId(),
    val quizStarted: Boolean = false,
    val choosingPlayer: Boolean = false,
    val confirmLeave: Boolean = false,
    val leaderboardError: Boolean = false,
    val selectedSecret: QuizSecret? = null,
    val visitedSettingsDuringQuiz: Boolean = false,
    val visitedCollectionDuringPause: Boolean = false,
    val nightCitySignalFromPreviousLaunch: Boolean = false,
)

internal class QuizNavigationPresenter(
    private val quiz: QuizContract,
    val challenge: ChallengePresenter,
    val leaderboard: LeaderboardPresenter,
    private var profile: ProfileIdentity,
    private val preferences: QuizPreferences,
    initial: QuizNavigationState = QuizNavigationState(
        nightCitySignalFromPreviousLaunch = preferences.hasNightCitySignal(),
    ),
    private val sounds: QuizSoundPlayer = QuizSoundPlayer {},
) {
    private val discovery = SecretDiscovery()
    var navigation by mutableStateOf(initial)
        private set
    var round by mutableStateOf(quiz.getState())
        private set
    var settings by mutableStateOf(preferences.settings())
        private set
    val collections = CollectionPresenter(preferences, leaderboard.store, leaderboard.scope)
    val collection get() = collections.progress
    val secretFloats: Map<QuizSecret, SecretFloat> get() = collection.floats
    val foundSecrets: Set<QuizSecret> get() = secretFloats.keys

    val suggestedPlayer: String get() = profile.savedName
    val canResumeNormal: Boolean get() = navigation.quizStarted && round.question != null
    val pauseSecretAvailable: Boolean
        get() = navigation.visitedSettingsDuringQuiz && !settings.showCorrections &&
            round.questionIndex + 1 == round.questionCount

    fun updateProfile(value: ProfileIdentity) { profile = value }

    fun openHome() { navigation = navigation.copy(page = QuizPage.Start) }
    fun openProfile() { navigation = navigation.copy(page = QuizPage.Profile) }
    fun openModes() { navigation = navigation.copy(page = QuizPage.Challenges) }
    fun openAbout() {
        navigation = navigation.copy(page = QuizPage.About)
        if (settings.appearance != QuizAppearance.Dark || QuizSecret.NightCitySignal in foundSecrets) return
        if (navigation.nightCitySignalFromPreviousLaunch) unlock(QuizSecret.NightCitySignal)
        else preferences.saveNightCitySignal()
    }
    fun openLeaderboard() { navigation = navigation.copy(page = QuizPage.Leaderboard) }

    fun openSettings() {
        discovery.enterSettings()
        navigation = navigation.copy(
            page = QuizPage.Settings,
            menuOrigin = navigation.page,
            visitedSettingsDuringQuiz = navigation.visitedSettingsDuringQuiz || navigation.page == QuizPage.Paused,
        )
    }

    fun openCollection() {
        discovery.enterCollection()
        navigation = navigation.copy(
            page = QuizPage.Collection,
            menuOrigin = navigation.page,
            visitedCollectionDuringPause = navigation.visitedCollectionDuringPause || navigation.page == QuizPage.Paused,
        )
    }

    fun openCrates() { navigation = navigation.copy(page = QuizPage.Crates) }

    fun buyCrate(crate: SecretCrate) {
        if (navigation.page != QuizPage.Crates || navigation.selectedSecret != null) return
        collections.buy(crate) { secret ->
            sounds.play(QuizSound.Secret)
            if (navigation.page == QuizPage.Crates && navigation.selectedSecret == null) {
                navigation = navigation.copy(selectedSecret = secret)
            }
        }
    }

    fun updateSettings(value: QuizSettings) {
        settings = value
        preferences.saveSettings(value)
    }

    fun selectAppearance(appearance: QuizAppearance) {
        updateSettings(settings.copy(appearance = appearance))
        unlock(discovery.selectAppearance(appearance))
    }

    fun openSecret(secret: QuizSecret) {
        if (!unlock(discovery.openCard(secret))) navigation = navigation.copy(selectedSecret = secret)
    }

    fun discoverSecret(secret: QuizSecret) {
        saveDiscoveries(listOf(secret))
        navigation = navigation.copy(selectedSecret = secret)
    }

    fun closeSecret() { navigation = navigation.copy(selectedSecret = null) }

    fun discoverCarousel() { unlock(QuizSecret.Carousel) }
    fun discoverCrosshair() { unlock(QuizSecret.Crosshair) }
    fun discoverRhythm() { unlock(QuizSecret.NeverFadeAway) }

    private fun unlock(vararg candidates: QuizSecret?): Boolean {
        val unlocked = candidates.filterNotNull().filterNot { it in foundSecrets }
        val first = unlocked.firstOrNull() ?: return false
        saveDiscoveries(unlocked)
        navigation = navigation.copy(selectedSecret = first)
        return true
    }

    private fun saveDiscoveries(secrets: List<QuizSecret>) {
        val newlyFound = secrets.distinct().filterNot { it in secretFloats }.associateWith { SecretFloat.roll() }
        if (newlyFound.isNotEmpty()) {
            collections.discover(newlyFound)
            sounds.play(QuizSound.Secret)
        }
    }

    fun chooseNormal() {
        if (canResumeNormal) {
            navigation = navigation.copy(page = QuizPage.Playing)
        } else {
            profile.load()
            leaderboard.load()
            navigation = navigation.copy(choosingPlayer = true)
        }
    }

    fun cancelPlayerChoice() { navigation = navigation.copy(choosingPlayer = false) }

    fun startNormal(name: String) {
        restartNormal()
        navigation = navigation.copy(page = QuizPage.Playing, playerName = name, choosingPlayer = false)
        unlock(SecretDiscovery.forPlayerName(name), discovery.startNormal())
    }

    fun restartNormal() {
        discovery.resetNormalAnswers()
        round = quiz.restart()
        navigation = navigation.copy(
            roundId = newRoundId(),
            quizStarted = true,
            visitedSettingsDuringQuiz = false,
            visitedCollectionDuringPause = false,
        )
    }

    fun startChallenge(mode: ChallengeMode) {
        challenge.start(mode)
        unlock(discovery.startChallenge(mode))
    }

    fun answerChallenge(choice: String) {
        val previousChoice = challenge.state?.choice
        challenge.answer(choice)
        val game = challenge.state ?: return
        if (previousChoice == null && game.choice != null && game.choice == challenge.question?.correctAnswer) {
            collections.earnPoint()
            unlock(SecretDiscovery.forTimedAnswer(game.mode, challenge.secondsLeft))
        }
    }

    fun checkChallengeDiscovery() {
        val game = challenge.state ?: return
        unlock(SecretDiscovery.forChallenge(game, challenge.secondsLeft))
    }

    fun answerNormal(choice: String) {
        val unanswered = round.selectedChoice == null
        round = quiz.submitAnswer(choice)
        if (unanswered && round.selectedChoice != null) {
            if (round.isCorrect) collections.earnPoint()
            if (!unlock(discovery.answerNormal(round.questionIndex, round.isCorrect))) {
                sounds.play(if (round.isCorrect) QuizSound.Correct else QuizSound.Incorrect)
            }
        }
    }

    fun continueNormal(guestName: String) {
        if (round.questionIndex + 1 == round.questionCount && round.selectedChoice != null) {
            leaderboard.completeGame(
                navigation.roundId, navigation.playerName ?: guestName, round.score,
                onRecorded = {
                    round = quiz.goToNextQuestion()
                    if (round.score == round.questionCount) sounds.play(QuizSound.Perfect)
                },
                onFailure = { navigation = navigation.copy(leaderboardError = true) },
            )
        } else {
            round = quiz.goToNextQuestion()
        }
    }

    fun dismissLeaderboardError() { navigation = navigation.copy(leaderboardError = false) }

    fun pauseNormal() {
        navigation = navigation.copy(page = QuizPage.Paused, visitedCollectionDuringPause = false)
    }

    fun resumeNormal() {
        if (navigation.visitedCollectionDuringPause) unlock(QuizSecret.Boomerang)
        navigation = navigation.copy(page = QuizPage.Playing, visitedCollectionDuringPause = false)
    }

    fun requestLeave() { navigation = navigation.copy(confirmLeave = true) }
    fun cancelLeave() { navigation = navigation.copy(confirmLeave = false) }

    fun leaveNormal() {
        restartNormal()
        navigation = navigation.copy(page = QuizPage.Start, confirmLeave = false, quizStarted = false)
    }

    fun goBack() {
        if (leaderboard.recording) return
        when (navigation.page) {
            QuizPage.Start, QuizPage.Profile -> openHome()
            QuizPage.Challenges -> if (challenge.state == null) openHome() else challenge.close()
            QuizPage.Playing -> if (round.question == null) openHome() else pauseNormal()
            QuizPage.Paused -> resumeNormal()
            QuizPage.Settings, QuizPage.Collection -> navigation = navigation.copy(page = navigation.menuOrigin)
            QuizPage.Crates -> navigation = navigation.copy(page = QuizPage.Collection)
            QuizPage.About, QuizPage.Leaderboard -> navigation = navigation.copy(page = QuizPage.Settings)
        }
    }

    companion object {
        fun saver(
            quiz: QuizContract,
            challenge: ChallengePresenter,
            leaderboard: LeaderboardPresenter,
            profile: ProfileIdentity,
            preferences: QuizPreferences,
            sounds: QuizSoundPlayer = QuizSoundPlayer {},
        ) = mapSaver(
            save = { presenter: QuizNavigationPresenter ->
                with(presenter.navigation) {
                    mapOf(
                        "page" to page.name,
                        "menuOrigin" to menuOrigin.name,
                        "playerName" to playerName,
                        "roundId" to roundId,
                        "quizStarted" to quizStarted,
                        "choosingPlayer" to choosingPlayer,
                        "confirmLeave" to confirmLeave,
                        "leaderboardError" to leaderboardError,
                        "selectedSecret" to selectedSecret?.name,
                        "visitedSettings" to visitedSettingsDuringQuiz,
                        "visitedCollection" to visitedCollectionDuringPause,
                        "nightCitySignalFromPreviousLaunch" to nightCitySignalFromPreviousLaunch,
                    )
                }
            },
            restore = { saved ->
                val state = QuizNavigationState(
                    page = QuizPage.entries.firstOrNull { it.name == saved["page"] } ?: QuizPage.Start,
                    menuOrigin = QuizPage.entries.firstOrNull { it.name == saved["menuOrigin"] } ?: QuizPage.Start,
                    playerName = saved["playerName"] as? String,
                    roundId = saved["roundId"] as? String ?: newRoundId(),
                    quizStarted = saved["quizStarted"] == true,
                    choosingPlayer = saved["choosingPlayer"] == true,
                    confirmLeave = saved["confirmLeave"] == true,
                    leaderboardError = saved["leaderboardError"] == true,
                    selectedSecret = QuizSecret.entries.firstOrNull { it.name == saved["selectedSecret"] },
                    visitedSettingsDuringQuiz = saved["visitedSettings"] == true,
                    visitedCollectionDuringPause = saved["visitedCollection"] == true,
                    nightCitySignalFromPreviousLaunch = saved["nightCitySignalFromPreviousLaunch"] == true,
                )
                QuizNavigationPresenter(quiz, challenge, leaderboard, profile, preferences, state, sounds)
            },
        )
    }
}
