package fr.stageacademy.quiz.quiz_view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal enum class Celebration { CorrectAnswer, PerfectScore }

@Composable
internal expect fun QuizCelebration(celebration: Celebration, modifier: Modifier)
