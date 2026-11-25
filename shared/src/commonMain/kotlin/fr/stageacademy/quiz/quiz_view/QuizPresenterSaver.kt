package fr.stageacademy.quiz.quiz_view

import androidx.compose.runtime.saveable.listSaver

internal val QuizPresenterSaver = listSaver<QuizContract, Any>(
    save = { presenter ->
        val state = presenter.getState()
        listOf(state.questionIndex, state.score, state.selectedChoice.orEmpty())
    },
    restore = { saved ->
        QuizPresenter.restore(
            questions = quizQuestions,
            questionIndex = saved[0] as Int,
            score = saved[1] as Int,
            selectedChoice = (saved[2] as String).takeIf { it.isNotEmpty() },
        )
    },
)
