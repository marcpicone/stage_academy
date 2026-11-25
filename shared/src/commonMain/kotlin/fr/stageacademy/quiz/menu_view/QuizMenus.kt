package fr.stageacademy.quiz.menu_view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.collection_view.QuizSecret
import fr.stageacademy.quiz.collection_view.secretGesture
import fr.stageacademy.quiz.quiz_view.QuizState
import fr.stageacademy.quiz.shared_view.MenuButton
import fr.stageacademy.quiz.shared_view.MenuCard

@Composable
internal fun PauseMenu(
    state: QuizState,
    keyAvailable: Boolean,
    onSettings: () -> Unit,
    onCollection: () -> Unit,
    onProfile: () -> Unit,
    onLeave: () -> Unit,
    onDiscover: (QuizSecret) -> Unit,
) {
    val questionNumber = state.questionIndex + 1
    MenuCard(
        title = stringResource(Res.string.quiz_paused),
        titleModifier = Modifier
            .secretGesture(QuizSecret.Hourglass, onDiscover, enabled = questionNumber == 5 || questionNumber == state.questionCount / 2)
            .secretGesture(QuizSecret.Key, onDiscover, taps = 2, enabled = keyAvailable),
    ) {
        Text(
            stringResource(Res.string.question_progress, questionNumber, state.questionCount),
            modifier = Modifier.secretGesture(QuizSecret.Bookmark, onDiscover),
        )
        Text(
            text = stringResource(Res.string.quiz_score, state.score, state.questionCount),
            modifier = Modifier.secretGesture(QuizSecret.Music, onDiscover, taps = 2),
        )
        MenuButton(label = stringResource(Res.string.settings), onClick = onSettings)
        MenuButton(label = stringResource(Res.string.secret_collection), onClick = onCollection)
        MenuButton(label = stringResource(Res.string.profile), onClick = onProfile)
        TextButton(onClick = onLeave, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(Res.string.leave_quiz))
        }
    }
}

@Composable
internal fun LeaveQuizDialog(onCancel: () -> Unit, onLeave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(Res.string.confirm_leave_quiz)) },
        text = { Text(stringResource(Res.string.leave_quiz_message)) },
        confirmButton = { TextButton(onClick = onLeave) { Text(stringResource(Res.string.leave_quiz)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) } },
    )
}
