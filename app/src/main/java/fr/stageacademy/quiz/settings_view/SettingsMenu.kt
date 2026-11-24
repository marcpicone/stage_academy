package fr.stageacademy.quiz.settings_view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.R
import fr.stageacademy.quiz.collection_view.QuizSecret
import fr.stageacademy.quiz.collection_view.secretGesture
import fr.stageacademy.quiz.shared_view.MenuButton
import fr.stageacademy.quiz.shared_view.MenuCard

internal enum class QuizAppearance(val label: Int) {
    System(R.string.appearance_system),
    Light(R.string.appearance_light),
    Dark(R.string.appearance_dark),
}

internal data class QuizSettings(
    val appearance: QuizAppearance = QuizAppearance.System,
    val showCorrections: Boolean = true,
    val horizontalAnswers: Boolean = false,
)

@Composable
internal fun SettingsMenu(
    settings: QuizSettings,
    onSettingsChange: (QuizSettings) -> Unit,
    onDiscover: (QuizSecret) -> Unit,
    onAbout: () -> Unit,
) {
    MenuCard(
        title = stringResource(R.string.settings),
        titleModifier = Modifier.secretGesture(QuizSecret.Semicolon, onDiscover, taps = 5),
    ) {
        Column(Modifier.selectableGroup()) {
            Text(
                text = stringResource(R.string.appearance),
                style = MaterialTheme.typography.titleMedium,
            )
            QuizAppearance.entries.forEach { appearance ->
                val selected = settings.appearance == appearance
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onSettingsChange(settings.copy(appearance = appearance)) },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(stringResource(appearance.label))
                }
            }
        }
        SettingsSwitch(
            title = stringResource(R.string.show_corrections),
            description = stringResource(R.string.show_corrections_description),
            checked = settings.showCorrections,
            onCheckedChange = { onSettingsChange(settings.copy(showCorrections = it)) },
        )
        SettingsSwitch(
            title = stringResource(R.string.horizontal_answers),
            description = stringResource(R.string.horizontal_answers_description),
            checked = settings.horizontalAnswers,
            onCheckedChange = { onSettingsChange(settings.copy(horizontalAnswers = it)) },
        )
        MenuButton(label = stringResource(R.string.about), onClick = onAbout)
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
