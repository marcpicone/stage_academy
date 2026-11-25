package fr.stageacademy.quiz.challenge_view

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.jetbrains.compose.resources.stringResource
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.quiz_view.QuestionActions
import fr.stageacademy.quiz.quiz_view.QuestionPanel
import fr.stageacademy.quiz.quiz_view.ResultPanel
import fr.stageacademy.quiz.shared_view.MenuButton
import fr.stageacademy.quiz.shared_view.QuizActionBar
import fr.stageacademy.quiz.shared_view.QuizTag
import kotlinx.coroutines.delay

@Composable
internal fun ChallengeScreen(
    presenter: ChallengeContract,
    horizontalAnswers: Boolean,
    progress: () -> Float,
    onAnswer: (String) -> Unit,
) {
    val game = presenter.state ?: return
    val question = presenter.question ?: return
    LaunchedEffect(game.deadline, game.end) {
        while (presenter.state?.deadline != null && presenter.state?.end == null) {
            presenter.tick()
            delay(100)
        }
    }

    val result = when (game.end) {
        ChallengeEnd.Complete -> Res.string.challenge_complete
        ChallengeEnd.TimeUp -> Res.string.challenge_time_up
        ChallengeEnd.WrongAnswer -> Res.string.challenge_streak_end
        null -> null
    }
    if (result != null) {
        ResultPanel(
            score = game.score,
            questionCount = game.order.size,
            title = stringResource(result),
            detail = if (game.end == ChallengeEnd.WrongAnswer) {
                stringResource(Res.string.incorrect_feedback, question.correctAnswer)
            } else null,
        )
    } else {
        QuestionPanel(
            question = question,
            questionNumber = game.index + 1,
            questionCount = game.order.size,
            progress = progress,
            score = game.score,
            selectedChoice = game.choice,
            isCorrect = game.choice == question.correctAnswer,
            horizontalAnswers = horizontalAnswers,
            onChoice = onAnswer,
            status = if (game.deadline != null) {
                {
                    val colors = MaterialTheme.colorScheme
                    val nearlyOver = presenter.secondsLeft <= 10
                    QuizTag(
                        text = stringResource(Res.string.challenge_seconds, presenter.secondsLeft),
                        color = if (nearlyOver) colors.errorContainer else colors.primaryContainer,
                        contentColor = if (nearlyOver) colors.onErrorContainer else colors.onPrimaryContainer,
                    )
                }
            } else null,
        )
    }
}

@Composable
internal fun ChallengeActions(presenter: ChallengeContract) {
    val game = presenter.state ?: return
    if (game.end != null) {
        QuizActionBar(
            label = stringResource(Res.string.restart_quiz),
            onClick = { presenter.start(game.mode) },
            secondaryAction = {
                MenuButton(stringResource(Res.string.challenge_choose_another), presenter::close)
            },
        )
    } else {
        val answer = presenter.question?.correctAnswer
        QuestionActions(
            selectedChoice = game.choice,
            isCorrect = game.choice != null && game.choice == answer,
            correctAnswer = answer,
            isLastQuestion = game.index == game.order.lastIndex,
            onContinue = presenter::next,
        )
    }
}
