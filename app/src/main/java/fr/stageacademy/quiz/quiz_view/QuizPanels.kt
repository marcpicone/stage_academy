package fr.stageacademy.quiz.quiz_view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.R
import fr.stageacademy.quiz.collection_view.QuizSecret
import fr.stageacademy.quiz.collection_view.secretGesture
import fr.stageacademy.quiz.shared_view.ControlShape
import fr.stageacademy.quiz.shared_view.PanelShape
import fr.stageacademy.quiz.shared_view.QuizActionBar
import fr.stageacademy.quiz.shared_view.QuizTag
import fr.stageacademy.quiz.ui.theme.QuizForest
import fr.stageacademy.quiz.ui.theme.QuizLime

@Composable
internal fun QuizHeader(modifier: Modifier = Modifier, onPause: (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.welcome_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (onPause != null) {
            TextButton(onClick = onPause, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.pause_quiz))
            }
        }
    }
}

@Composable
internal fun QuestionPanel(
    question: Question,
    questionNumber: Int,
    questionCount: Int,
    progress: () -> Float,
    score: Int,
    selectedChoice: String?,
    isCorrect: Boolean,
    horizontalAnswers: Boolean,
    onChoice: (String) -> Unit,
    onDiscover: (QuizSecret) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = PanelShape) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier.secretGesture(QuizSecret.Compass, onDiscover, enabled = selectedChoice != null),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.question_progress, questionNumber, questionCount),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                    )
                    QuizTag(
                        text = stringResource(R.string.quiz_score, score, questionCount),
                        color = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                    )
                }
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    trackColor = colors.primaryContainer,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
            val promptVisibility = remember { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(
                visibleState = promptVisibility,
                enter = fadeIn(tween(durationMillis = 220)) + slideInVertically(tween(durationMillis = 220)) { it / 5 },
            ) {
                Text(
                    text = question.text,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            AnswerChoices(question.choices, selectedChoice, isCorrect, horizontalAnswers, onChoice)
        }
    }
}

@Composable
private fun AnswerChoices(
    choices: List<String>,
    selectedChoice: String?,
    isCorrect: Boolean,
    horizontalAnswers: Boolean,
    onChoice: (String) -> Unit,
) {
    val answer: @Composable (Int, String, Modifier) -> Unit = { index, choice, modifier ->
        AnswerChoice(
            letter = 'A' + index,
            choice = choice,
            isSelected = choice == selectedChoice,
            isCorrect = isCorrect,
            enabled = selectedChoice == null,
            horizontal = horizontalAnswers,
            onClick = { onChoice(choice) },
            modifier = modifier,
        )
    }
    if (horizontalAnswers) {
        // Keep a preview of the next card on screen, with wider cards for large text.
        val cardWidthFraction = if (LocalDensity.current.fontScale > 1.3f) 0.82f else 0.43f
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.scroll_answers),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(choices) { index, choice ->
                    answer(index, choice, Modifier.fillParentMaxWidth(cardWidthFraction))
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            choices.forEachIndexed { index, choice -> answer(index, choice, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
internal fun QuestionActions(
    selectedChoice: String?,
    isCorrect: Boolean,
    correctAnswer: String?,
    isLastQuestion: Boolean,
    onContinue: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val canGoToNextQuestion = selectedChoice != null
    val feedbackText = when {
        isCorrect -> stringResource(R.string.correct_feedback)
        correctAnswer == null -> stringResource(R.string.incorrect_feedback_without_correction)
        else -> stringResource(R.string.incorrect_feedback, correctAnswer)
    }
    val feedbackColor = if (isCorrect) colors.onPrimaryContainer else colors.onErrorContainer
    val feedback = feedbackText to feedbackColor
    val visibility = remember { MutableTransitionState(canGoToNextQuestion) }
    visibility.targetState = canGoToNextQuestion

    // Keep the previous answer visible until its exit animation finishes.
    var previousFeedback by remember { mutableStateOf(feedback) }
    if (canGoToNextQuestion) {
        SideEffect { previousFeedback = feedback }
    }

    QuizActionBar(
        label = stringResource(if (isLastQuestion) R.string.show_results else R.string.next_question),
        onClick = onContinue,
        enabled = canGoToNextQuestion,
    ) {
        Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            if (!canGoToNextQuestion && visibility.isIdle) {
                Text(
                    text = stringResource(R.string.choose_answer),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visibleState = visibility,
                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300)),
            ) {
                val (text, color) = if (canGoToNextQuestion) feedback else previousFeedback
                Text(text = text, style = MaterialTheme.typography.labelLarge, color = color)
            }
        }
    }
}

@Composable
private fun AnswerChoice(
    letter: Char,
    choice: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    enabled: Boolean,
    horizontal: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (targetColor, contentColor) = when {
        !isSelected -> colors.surface to colors.onSurface
        isCorrect -> colors.primaryContainer to colors.onPrimaryContainer
        else -> colors.errorContainer to colors.onErrorContainer
    }
    val containerColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 220),
        label = "Answer background",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) contentColor else colors.outlineVariant,
        animationSpec = tween(durationMillis = 220),
        label = "Answer outline",
    )
    val label: @Composable () -> Unit = {
        Text(
            text = choice,
            textAlign = if (horizontal) TextAlign.Center else TextAlign.Start,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        )
    }
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { selected = isSelected },
        enabled = enabled,
        shape = ControlShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        if (horizontal) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp).padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                ChoiceLetter(letter, contentColor)
                label()
            }
        } else {
            Row(
                modifier = Modifier.heightIn(min = 54.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ChoiceLetter(letter, contentColor)
                label()
            }
        }
    }
}

@Composable
private fun ChoiceLetter(letter: Char, color: Color) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.08f)) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            Text(
                text = letter.toString(),
                modifier = Modifier.clearAndSetSemantics {},
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        }
    }
}

@Composable
internal fun ResultPanel(score: Int, questionCount: Int, onDiscover: (QuizSecret) -> Unit) {
    Surface(shape = PanelShape, color = QuizForest, contentColor = Color.White) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.quiz_finished),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.final_score, score, questionCount),
                modifier = Modifier.secretGesture(QuizSecret.Star, onDiscover, enabled = score == questionCount),
                style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Serif),
                color = QuizLime,
            )
            Text(
                text = stringResource(R.string.final_score_label),
                modifier = Modifier.secretGesture(QuizSecret.Balance, onDiscover, taps = 2, enabled = score * 2 == questionCount),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
