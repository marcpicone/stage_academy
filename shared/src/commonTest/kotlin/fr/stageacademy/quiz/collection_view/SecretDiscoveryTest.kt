package fr.stageacademy.quiz.collection_view

import fr.stageacademy.quiz.challenge_view.ChallengeEnd
import fr.stageacademy.quiz.challenge_view.ChallengeMode
import fr.stageacademy.quiz.challenge_view.ChallengeState
import fr.stageacademy.quiz.settings_view.QuizAppearance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SecretDiscoveryTest {
    @Test
    fun theNewCardsFollowTheOriginalCardsAndTheImperialClueUsesStableNumbers() {
        assertEquals(42, QuizSecret.entries.size)
        assertEquals(21, QuizSecret.Crown.ordinal)
        assertEquals(18, QuizSecret.Branch.ordinal + 1)
        assertEquals(4, QuizSecret.Music.ordinal + 1)
        val progress = SecretDiscovery()
        assertNull(progress.openCard(QuizSecret.Music))
        assertNull(progress.openCard(QuizSecret.Branch))
        progress.enterCollection()
        assertNull(progress.openCard(QuizSecret.Music))
        assertNull(progress.openCard(QuizSecret.Branch))
        assertEquals(QuizSecret.ImperialBee, progress.openCard(QuizSecret.Music))
        assertNull(SecretDiscovery.forPlayerName("Napoléon III"))
        assertEquals(QuizSecret.Bicorne, SecretDiscovery.forPlayerName("  Napoléon "))
        assertEquals(QuizSecret.Bicorne, SecretDiscovery.forPlayerName("napoleon"))
    }

    @Test
    fun settingsAndModesRequireCompleteSequences() {
        val progress = SecretDiscovery()
        assertNull(progress.selectAppearance(QuizAppearance.Dark))
        assertNull(progress.selectAppearance(QuizAppearance.System))
        assertNull(progress.selectAppearance(QuizAppearance.Light))
        assertNull(progress.selectAppearance(QuizAppearance.Dark))
        assertEquals(QuizSecret.Chameleon, progress.selectAppearance(QuizAppearance.System))
        assertNull(progress.startChallenge(ChallengeMode.Timer))
        assertNull(progress.startChallenge(ChallengeMode.Timer))
        assertNull(progress.startNormal())
        assertNull(progress.startChallenge(ChallengeMode.Surprise))
        assertEquals(QuizSecret.Cartographer, progress.startChallenge(ChallengeMode.Streak))
    }

    @Test
    fun zigzagUsesOnlyTheOpeningAnswersOfTheCurrentRound() {
        val progress = SecretDiscovery()
        assertNull(progress.answerNormal(index = 0, correct = false))
        assertNull(progress.answerNormal(index = 1, correct = false))
        progress.resetNormalAnswers()
        repeat(4) { index -> assertNull(progress.answerNormal(index, correct = index % 2 == 0)) }
        assertEquals(QuizSecret.Zigzag, progress.answerNormal(index = 4, correct = true))
        assertNull(progress.answerNormal(index = 5, correct = false))
    }

    @Test
    fun challengeSecretsDistinguishWaitingSpeedAndAStreakEnding() {
        val timed = ChallengeState(ChallengeMode.Timer, List(5) { it })
        val timeout = timed.copy(end = ChallengeEnd.TimeUp)
        assertNull(SecretDiscovery.forChallenge(timeout.copy(choice = "A"), secondsLeft = 0))
        assertEquals(QuizSecret.Ice, SecretDiscovery.forChallenge(timeout, secondsLeft = 0))
        val perfect = timed.copy(index = 4, choice = "A", score = 5)
        assertNull(SecretDiscovery.forChallenge(perfect, secondsLeft = 14))
        assertEquals(QuizSecret.Comet, SecretDiscovery.forChallenge(perfect, secondsLeft = 15))
        val streak = timed.copy(mode = ChallengeMode.Streak, score = 3, end = ChallengeEnd.WrongAnswer)
        assertEquals(QuizSecret.Trapdoor, SecretDiscovery.forChallenge(streak, secondsLeft = 0))
        assertNull(SecretDiscovery.forChallenge(streak.copy(score = 4), secondsLeft = 0))
    }
}
