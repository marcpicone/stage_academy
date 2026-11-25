package fr.stageacademy.quiz.collection_view

import fr.stageacademy.quiz.challenge_view.ChallengeEnd
import fr.stageacademy.quiz.challenge_view.ChallengeMode
import fr.stageacademy.quiz.challenge_view.ChallengeState
import fr.stageacademy.quiz.settings_view.QuizAppearance

internal class SecretDiscovery {
    private val startedChallenges = mutableSetOf<ChallengeMode>()
    private var startedNormal = false
    private var nextZigzagAnswer = 0
    private var nextAppearance = 0
    private var lastCollectionCard: QuizSecret? = null

    fun startNormal(): QuizSecret? {
        startedNormal = true
        return allModesSecret()
    }

    fun startChallenge(mode: ChallengeMode): QuizSecret? {
        startedChallenges += mode
        return allModesSecret()
    }

    private fun allModesSecret(): QuizSecret? =
        QuizSecret.Cartographer.takeIf { startedNormal && startedChallenges.size == ChallengeMode.entries.size }

    fun resetNormalAnswers() {
        nextZigzagAnswer = 0
    }

    fun answerNormal(index: Int, correct: Boolean): QuizSecret? {
        if (index != nextZigzagAnswer || index !in ZigzagPattern.indices) return null
        nextZigzagAnswer = if (correct == ZigzagPattern[index]) index + 1 else -1
        return QuizSecret.Zigzag.takeIf { nextZigzagAnswer == ZigzagPattern.size }
    }

    fun enterSettings() {
        nextAppearance = 0
    }

    fun selectAppearance(appearance: QuizAppearance): QuizSecret? {
        nextAppearance = when {
            appearance == AppearanceOrder.getOrNull(nextAppearance) -> nextAppearance + 1
            appearance == AppearanceOrder.first() -> 1
            else -> 0
        }
        return QuizSecret.Chameleon.takeIf { nextAppearance == AppearanceOrder.size }
    }

    fun enterCollection() {
        lastCollectionCard = null
    }

    fun openCard(secret: QuizSecret): QuizSecret? {
        val unlocksBee = lastCollectionCard == QuizSecret.Branch && secret == QuizSecret.Music
        lastCollectionCard = secret
        return QuizSecret.ImperialBee.takeIf { unlocksBee }
    }

    companion object {
        private val ZigzagPattern = listOf(true, false, true, false, true)
        private val AppearanceOrder = listOf(QuizAppearance.Light, QuizAppearance.Dark, QuizAppearance.System)

        fun forPlayerName(name: String): QuizSecret? = when (name.trim().lowercase()) {
            "napoléon", "napoleon" -> QuizSecret.Bicorne
            else -> null
        }

        fun forTimedAnswer(mode: ChallengeMode, secondsLeft: Int): QuizSecret? =
            QuizSecret.Clutch.takeIf { mode == ChallengeMode.Timer && secondsLeft in 1..3 }

        fun forChallenge(game: ChallengeState, secondsLeft: Int): QuizSecret? = when (game.mode) {
            ChallengeMode.Timer -> when {
                game.end == ChallengeEnd.TimeUp && game.index == 0 && game.choice == null -> QuizSecret.Ice
                game.end == null && game.index == game.order.lastIndex && game.choice != null &&
                    game.score == game.order.size && secondsLeft >= 15 -> QuizSecret.Comet
                else -> null
            }
            ChallengeMode.Streak -> QuizSecret.Trapdoor.takeIf {
                game.end == ChallengeEnd.WrongAnswer && game.score == 3
            }
            ChallengeMode.Surprise -> null
        }
    }
}
