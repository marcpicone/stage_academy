package fr.stageacademy.quiz.challenge_view

import org.jetbrains.compose.resources.StringResource
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.quiz_view.Question

internal val ChallengeMode.title: StringResource
    get() = when (this) {
        ChallengeMode.Timer -> Res.string.challenge_timer
        ChallengeMode.Streak -> Res.string.challenge_streak
        ChallengeMode.Surprise -> Res.string.challenge_surprise
    }

internal val ChallengeMode.description: StringResource
    get() = when (this) {
        ChallengeMode.Timer -> Res.string.challenge_timer_desc
        ChallengeMode.Streak -> Res.string.challenge_streak_desc
        ChallengeMode.Surprise -> Res.string.challenge_surprise_desc
    }

internal interface ChallengeContract {
    val state: ChallengeState?
    val question: Question?
    val secondsLeft: Int
    fun start(mode: ChallengeMode)
    fun answer(choice: String)
    fun next()
    fun tick()
    fun close()
}
