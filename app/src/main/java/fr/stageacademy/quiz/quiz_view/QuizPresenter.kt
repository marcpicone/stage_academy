package fr.stageacademy.quiz.quiz_view

class QuizPresenter private constructor(
    private val questions: List<Question>,
    private var questionIndex: Int,
    private var score: Int,
    private var selectedChoice: String?,
) : QuizContract {
    constructor(questions: List<Question>) : this(questions, 0, 0, null)

    companion object {
        internal fun restore(
            questions: List<Question>,
            questionIndex: Int,
            score: Int,
            selectedChoice: String?,
        ): QuizPresenter {
            val index = questionIndex.coerceIn(0, questions.size)
            val choice = selectedChoice?.takeIf { it in questions.getOrNull(index)?.choices.orEmpty() }
            return QuizPresenter(questions, index, score.coerceIn(0, questions.size), choice)
        }
    }

    override fun getState(): QuizState {
        val question = currentQuestion()
        return QuizState(
            question = question,
            questionIndex = questionIndex,
            questionCount = questions.size,
            score = score,
            selectedChoice = selectedChoice,
            isCorrect = question != null && selectedChoice == question.correctAnswer,
        )
    }

    override fun submitAnswer(choice: String): QuizState {
        val question = currentQuestion() ?: return getState()
        if (selectedChoice != null) return getState()

        selectedChoice = choice
        if (choice == question.correctAnswer) score++
        return getState()
    }

    override fun goToNextQuestion(): QuizState {
        if (selectedChoice != null) {
            questionIndex++
            selectedChoice = null
        }
        return getState()
    }

    override fun restart(): QuizState {
        questionIndex = 0
        selectedChoice = null
        score = 0
        return getState()
    }

    private fun currentQuestion(): Question? = questions.getOrNull(questionIndex)
}
