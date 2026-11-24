package fr.stageacademy.quiz.quiz_view

interface QuizContract {
    fun getState(): QuizState
    fun submitAnswer(choice: String): QuizState
    fun goToNextQuestion(): QuizState
    fun restart(): QuizState
}

data class QuizState(
    val question: Question?,
    val questionIndex: Int,
    val questionCount: Int,
    val score: Int,
    val selectedChoice: String?,
    val isCorrect: Boolean,
)
