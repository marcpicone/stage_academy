package fr.stageacademy.quiz.challenge_view

import fr.stageacademy.quiz.quiz_view.Question
import fr.stageacademy.quiz.quiz_view.quizQuestions

enum class ChallengeMode { Timer, Streak, Surprise }

enum class ChallengeEnd { Complete, TimeUp, WrongAnswer }

data class ChallengeState(
    val mode: ChallengeMode,
    val order: List<Int>,
    val index: Int = 0,
    val score: Int = 0,
    val choice: String? = null,
    val deadline: Long? = null,
    val end: ChallengeEnd? = null,
)

class ChallengeGame(
    private val elapsedMillis: () -> Long,
    initial: ChallengeState? = null,
) {
    var state: ChallengeState? = initial
        private set
    var secondsLeft: Int = 0
        private set
    val question: Question? get() = state?.let { quizQuestions[it.order[it.index]] }

    init {
        tick()
    }

    fun start(mode: ChallengeMode) {
        val order = quizQuestions.indices.shuffled()
        secondsLeft = 0
        state = ChallengeState(
            mode = mode,
            order = if (mode == ChallengeMode.Streak) order else order.take(5),
            deadline = if (mode == ChallengeMode.Timer) elapsedMillis() + 45_000L else null,
        )
        tick()
    }

    fun tick() {
        val current = state ?: return
        val deadline = current.deadline ?: return
        if (current.end != null) return
        val remaining = (deadline - elapsedMillis()).coerceAtLeast(0L)
        secondsLeft = ((remaining + 999L) / 1000L).toInt()
        if (remaining == 0L) state = current.copy(end = ChallengeEnd.TimeUp)
    }

    fun answer(choice: String) {
        tick()
        val current = state ?: return
        val question = question ?: return
        if (current.end != null || current.choice != null || choice !in question.choices) return
        val correct = choice == question.correctAnswer
        state = current.copy(
            choice = choice,
            score = current.score + if (correct) 1 else 0,
            deadline = current.deadline.takeUnless { current.index == current.order.lastIndex },
            end = ChallengeEnd.WrongAnswer.takeIf { !correct && current.mode == ChallengeMode.Streak },
        )
    }

    fun next() {
        tick()
        val current = state ?: return
        if (current.end != null || current.choice == null) return
        state = if (current.index == current.order.lastIndex) current.copy(end = ChallengeEnd.Complete)
        else current.copy(index = current.index + 1, choice = null)
    }

    fun close() {
        state = null
        secondsLeft = 0
    }
}
