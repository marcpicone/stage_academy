package fr.stageacademy.quiz.quiz_view

import fr.stageacademy.quiz.challenge_view.ChallengeEnd
import fr.stageacademy.quiz.challenge_view.ChallengeGame
import fr.stageacademy.quiz.challenge_view.ChallengeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class QuizRulesTest {
    @Test
    fun normalKeepsTheFirstAnswerAndRestartsFromZero() {
        val presenter = QuizPresenter(quizQuestions)
        val question = assertNotNull(presenter.getState().question)
        val wrong = question.choices.first { it != question.correctAnswer }
        assertEquals(0, presenter.goToNextQuestion().questionIndex)
        assertEquals(1, presenter.submitAnswer(question.correctAnswer).score)
        assertEquals(question.correctAnswer, presenter.submitAnswer(wrong).selectedChoice)
        assertEquals(1, presenter.goToNextQuestion().questionIndex)
        val restarted = presenter.restart()
        assertEquals(0, restarted.questionIndex)
        assertEquals(0, restarted.score)
        assertNull(restarted.selectedChoice)
    }

    @Test
    fun timerEndsAfterFortyFiveSecondsAndRejectsLateAnswers() {
        var now = 0L
        val game = ChallengeGame({ now })
        game.start(ChallengeMode.Timer)
        assertEquals(5, game.state?.order?.size)
        assertEquals(45, game.secondsLeft)
        val question = assertNotNull(game.question)

        now = 45_000L
        game.answer(question.correctAnswer)
        assertEquals(ChallengeEnd.TimeUp, game.state?.end)
        assertEquals(0, game.secondsLeft)
        assertEquals(0, game.state?.score)
        assertNull(game.state?.choice)
    }

    @Test
    fun streakEndsOnTheFirstMistakeAndSurprisePlaysFiveQuestions() {
        val game = ChallengeGame({ 0L })
        game.start(ChallengeMode.Streak)
        val question = assertNotNull(game.question)
        game.answer(question.choices.first { it != question.correctAnswer })
        assertEquals(ChallengeEnd.WrongAnswer, game.state?.end)
        assertEquals(0, game.state?.score)

        game.start(ChallengeMode.Surprise)
        repeat(5) {
            val current = assertNotNull(game.question)
            game.answer(current.correctAnswer)
            game.next()
        }
        assertEquals(ChallengeEnd.Complete, game.state?.end)
        assertEquals(5, game.state?.score)
    }
}
