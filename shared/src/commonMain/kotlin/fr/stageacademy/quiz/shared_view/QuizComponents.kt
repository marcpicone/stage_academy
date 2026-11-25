package fr.stageacademy.quiz.shared_view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal val PanelShape = RoundedCornerShape(28.dp)
internal val ControlShape = RoundedCornerShape(16.dp)

@Composable
internal fun MenuButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = withQuizClickSound(onClick),
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = ControlShape,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun MenuCard(
    title: String,
    titleModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(shape = PanelShape) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = title,
                modifier = titleModifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
            content()
        }
    }
}

@Composable
internal fun QuizActionBar(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    secondaryAction: (@Composable () -> Unit)? = null,
    feedback: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            feedback?.invoke()
            Button(
                onClick = withQuizClickSound(onClick),
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                enabled = enabled,
                shape = ControlShape,
            ) {
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }
            secondaryAction?.invoke()
        }
    }
}

@Composable
internal fun QuizTag(text: String, color: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = color, contentColor = contentColor) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
