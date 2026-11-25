package fr.stageacademy.quiz.challenge_view

import fr.stageacademy.quiz.shared_view.withQuizClickSound

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.shared_view.ControlShape

@Composable
internal fun PlayModeMenu(
    canResumeNormal: Boolean,
    onNormal: () -> Unit,
    onChallenge: (ChallengeMode) -> Unit,
) {
    Text(
        stringResource(Res.string.play_mode_title),
        modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
    )
    PlayModeCard(
        title = stringResource(Res.string.play_mode_normal),
        description = stringResource(if (canResumeNormal) Res.string.play_mode_resume else Res.string.play_mode_normal_desc),
        delayMillis = 0,
        onClick = onNormal,
    )
    ChallengeMode.entries.forEachIndexed { index, mode ->
        PlayModeCard(
            title = stringResource(mode.title),
            description = stringResource(mode.description),
            delayMillis = 45 * (index + 1),
            onClick = { onChallenge(mode) },
        )
    }
}

@Composable
private fun PlayModeCard(title: String, description: String, delayMillis: Int, onClick: () -> Unit) {
    val visibility = remember { MutableTransitionState(false).apply { targetState = true } }
    val colors = MaterialTheme.colorScheme
    AnimatedVisibility(
        visibleState = visibility,
        enter = fadeIn(tween(200, delayMillis)) +
            slideInVertically(tween(240, delayMillis)) { it / 8 },
    ) {
        Surface(
            onClick = withQuizClickSound(onClick),
            modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
            shape = ControlShape,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                Text("›", modifier = Modifier.clearAndSetSemantics {}, color = colors.primary, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
