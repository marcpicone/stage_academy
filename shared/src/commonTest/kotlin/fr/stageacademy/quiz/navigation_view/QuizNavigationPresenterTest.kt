package fr.stageacademy.quiz.navigation_view

import androidx.compose.runtime.saveable.SaverScope
import fr.stageacademy.quiz.QuizPreferences
import fr.stageacademy.quiz.challenge_view.ChallengeMode
import fr.stageacademy.quiz.challenge_view.ChallengePresenter
import fr.stageacademy.quiz.collection_view.QuizSecret
import fr.stageacademy.quiz.collection_view.CollectionProgress
import fr.stageacademy.quiz.collection_view.SecretCrate
import fr.stageacademy.quiz.leaderboard_view.LeaderboardPresenter
import fr.stageacademy.quiz.leaderboard_view.LeaderboardStore
import fr.stageacademy.quiz.leaderboard_view.PlayerRanking
import fr.stageacademy.quiz.profile_view.ProfileIdentity
import fr.stageacademy.quiz.quiz_view.QuizPresenter
import fr.stageacademy.quiz.quiz_view.QuizPresenterSaver
import fr.stageacademy.quiz.quiz_view.quizQuestions
import fr.stageacademy.quiz.settings_view.QuizAppearance
import fr.stageacademy.quiz.settings_view.QuizSettings
import fr.stageacademy.quiz.shared_view.QuizSound
import fr.stageacademy.quiz.shared_view.QuizSoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuizNavigationPresenterTest {
    private val scope = CoroutineScope(EmptyCoroutineContext)
    private val leaderboard = LeaderboardPresenter(object : LeaderboardStore {
        override fun rankings() = emptyList<PlayerRanking>()
        override fun record(id: String, player: String, points: Int) = listOf(PlayerRanking(player, points.toLong()))
    }, scope)
    private val profile = object : ProfileIdentity {
        override val savedName = "Camille"
        override fun load() = Unit
    }
    private val preferences = object : QuizPreferences {
        private var settings = QuizSettings()
        private var collection = CollectionProgress()
        private var signal = false
        override fun settings() = settings
        override fun collection() = collection
        override fun saveSettings(settings: QuizSettings) { this.settings = settings }
        override fun saveCollection(progress: CollectionProgress) { collection = progress }
        override fun hasNightCitySignal() = signal
        override fun saveNightCitySignal() { signal = true }
    }

    @Test
    fun restoringThePausedRoundKeepsTheAnswerAndItsReturnPath() {
        val quiz = QuizPresenter(quizQuestions)
        val challenge = ChallengePresenter()
        val presenter = QuizNavigationPresenter(quiz, challenge, leaderboard, profile, preferences)
        presenter.startNormal("Camille")
        val correctAnswer = assertNotNull(presenter.round.question).correctAnswer
        presenter.answerNormal(correctAnswer)
        presenter.pauseNormal()
        presenter.openCollection()

        val saverScope = SaverScope { true }
        val navigationSaver = QuizNavigationPresenter.saver(quiz, challenge, leaderboard, profile, preferences)
        val savedNavigation = assertNotNull(with(navigationSaver) { saverScope.save(presenter) })
        val savedQuiz = assertNotNull(with(QuizPresenterSaver) { saverScope.save(quiz) })
        val restoredQuiz = assertNotNull(QuizPresenterSaver.restore(savedQuiz))
        val restored = assertNotNull(
            QuizNavigationPresenter.saver(restoredQuiz, challenge, leaderboard, profile, preferences).restore(savedNavigation),
        )

        assertEquals(QuizPage.Collection, restored.navigation.page)
        assertEquals(presenter.navigation.roundId, restored.navigation.roundId)
        restored.goBack()
        assertEquals(QuizPage.Paused, restored.navigation.page)
        restored.goBack()
        assertEquals(QuizPage.Playing, restored.navigation.page)
        assertEquals(1, restored.round.score)
        assertEquals(correctAnswer, restored.round.selectedChoice)
        assertEquals(QuizSecret.Boomerang, restored.navigation.selectedSecret)
        assertTrue(QuizSecret.Boomerang in preferences.collection().floats)
    }

    @Test
    fun leavingAChallengeReturnsToModesAndThenHome() {
        val presenter = QuizNavigationPresenter(
            QuizPresenter(quizQuestions), ChallengePresenter(), leaderboard, profile, preferences,
        )
        presenter.openModes()
        presenter.startChallenge(ChallengeMode.Surprise)
        assertNotNull(presenter.challenge.state)
        presenter.goBack()
        assertEquals(QuizPage.Challenges, presenter.navigation.page)
        assertNull(presenter.challenge.state)
        presenter.goBack()
        assertEquals(QuizPage.Start, presenter.navigation.page)
    }

    @Test
    fun appearanceAndDiscoveryArePersistedThroughPreferences() {
        val presenter = QuizNavigationPresenter(
            QuizPresenter(quizQuestions), ChallengePresenter(), leaderboard, profile, preferences,
        )
        presenter.openSettings()
        presenter.selectAppearance(QuizAppearance.Light)
        presenter.selectAppearance(QuizAppearance.Dark)
        presenter.selectAppearance(QuizAppearance.System)

        assertEquals(QuizAppearance.System, preferences.settings().appearance)
        assertEquals(QuizSecret.Chameleon, presenter.navigation.selectedSecret)
        assertEquals(setOf(QuizSecret.Chameleon), preferences.collection().floats.keys)
    }

    @Test
    fun onlyAcceptedAnswersAndNewDiscoveriesPlaySounds() {
        val heard = mutableListOf<QuizSound>()
        val presenter = QuizNavigationPresenter(
            QuizPresenter(quizQuestions), ChallengePresenter(), leaderboard, profile, preferences,
            sounds = QuizSoundPlayer { heard.add(it) },
        )
        presenter.startNormal("Camille")
        val first = assertNotNull(presenter.round.question)
        presenter.answerNormal(first.correctAnswer)
        presenter.answerNormal(first.correctAnswer)
        presenter.continueNormal("Joueur")
        val second = assertNotNull(presenter.round.question)
        presenter.answerNormal(second.choices.first { it != second.correctAnswer })
        presenter.discoverSecret(QuizSecret.Bug)
        val originalFloat = assertNotNull(presenter.secretFloats[QuizSecret.Bug])
        presenter.discoverSecret(QuizSecret.Bug)
        assertEquals(originalFloat, presenter.secretFloats[QuizSecret.Bug])
        assertEquals(originalFloat, preferences.collection().floats[QuizSecret.Bug])
        assertEquals(1, preferences.collection().points)
        assertEquals(listOf(QuizSound.Correct, QuizSound.Incorrect, QuizSound.Secret), heard)
    }

    @Test
    fun aCompletedPerfectChallengePlaysItsSoundOnlyOnce() {
        val heard = mutableListOf<QuizSound>()
        val challenge = ChallengePresenter(sounds = QuizSoundPlayer { heard.add(it) })
        challenge.start(ChallengeMode.Surprise)
        repeat(5) {
            val answer = assertNotNull(challenge.question).correctAnswer
            challenge.answer(answer)
            challenge.answer(answer)
            challenge.next()
        }
        challenge.next()
        assertEquals(List(5) { QuizSound.Correct } + QuizSound.Perfect, heard)
    }

    @Test
    fun theNightCitySignalOnlyUnlocksOnANewLaunch() {
        val quiz = QuizPresenter(quizQuestions)
        val challenge = ChallengePresenter()
        val first = QuizNavigationPresenter(quiz, challenge, leaderboard, profile, preferences)
        first.openAbout()
        assertTrue(!preferences.hasNightCitySignal())

        first.updateSettings(QuizSettings(appearance = QuizAppearance.Dark))
        first.openAbout()
        first.openAbout()
        assertTrue(preferences.hasNightCitySignal())
        assertNull(first.navigation.selectedSecret)

        val second = QuizNavigationPresenter(quiz, challenge, leaderboard, profile, preferences)
        second.openAbout()
        assertEquals(QuizSecret.NightCitySignal, second.navigation.selectedSecret)
    }

    @Test
    fun fiveChallengeAnswersBuyARewardWithoutChangingTheScore() {
        val challenge = ChallengePresenter()
        val presenter = QuizNavigationPresenter(
            QuizPresenter(quizQuestions), challenge, leaderboard, profile, preferences,
        )
        presenter.openModes()
        presenter.startChallenge(ChallengeMode.Surprise)
        repeat(5) {
            val answer = assertNotNull(challenge.question).correctAnswer
            presenter.answerChallenge(answer)
            presenter.answerChallenge(answer)
            challenge.next()
        }
        assertEquals(5, preferences.collection().points)
        presenter.openHome()
        presenter.openCollection()
        presenter.openCrates()
        presenter.buyCrate(SecretCrate.Basic)
        val reward = assertNotNull(presenter.navigation.selectedSecret)
        assertNotNull(reward.crateRarity)
        assertEquals(0, preferences.collection().points)
        assertEquals(5, challenge.state?.score)
        assertTrue(reward in preferences.collection().floats)
        presenter.closeSecret()
        presenter.buyCrate(SecretCrate.Basic)
        assertNull(presenter.navigation.selectedSecret)
    }
}
