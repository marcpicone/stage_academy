package fr.stageacademy.quiz.quiz_view

data class Question(
    val text: String,
    val choices: List<String>,
    val correctAnswer: String,
)
