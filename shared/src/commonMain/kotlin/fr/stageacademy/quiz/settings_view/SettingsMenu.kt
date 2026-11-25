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
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.collection_view.QuizSecret
import fr.stageacademy.quiz.collection_view.secretGesture
import fr.stageacademy.quiz.shared_view.MenuButton
import fr.stageacademy.quiz.shared_view.MenuCard
import fr.stageacademy.quiz.shared_view.LocalQuizSound
import fr.stageacademy.quiz.shared_view.QuizSound

internal enum class QuizAppearance(val label: StringResource) {
    System(Res.string.appearance_system),
    Light(Res.string.appearance_light),
    Dark(Res.string.appearance_dark),
}

internal data class QuizSettings(
    val appearance: QuizAppearance = QuizAppearance.System,
    val showCorrections: Boolean = true,
    val horizontalAnswers: Boolean = false,
    val soundEnabled: Boolean = true,
)

@Composable
internal fun SettingsMenu(
    settings: QuizSettings,
    onSettingsChange: (QuizSettings) -> Unit,
    onAppearanceSelected: (QuizAppearance) -> Unit,
    onDiscover: (QuizSecret) -> Unit,
    onAbout: () -> Unit,
    onLeaderboard: () -> Unit,
) {
    val sounds = LocalQuizSound.current
    MenuCard(
        title = stringResource(Res.string.settings),
        titleModifier = Modifier.secretGesture(QuizSecret.Semicolon, onDiscover, taps = 5),
    ) {
        Column(Modifier.selectableGroup()) {
            Text(
                text = stringResource(Res.string.appearance),
                modifier = Modifier.secretGesture(QuizSecret.Prism, onDiscover, taps = 3),
                style = MaterialTheme.typography.titleMedium,
            )
            QuizAppearance.entries.forEach { appearance ->
                val selected = settings.appearance == appearance
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = {
                            sounds.play(QuizSound.Click)
                            onAppearanceSelected(appearance)
                        },
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
            title = stringResource(Res.string.show_corrections),
            description = stringResource(Res.string.show_corrections_description),
            checked = settings.showCorrections,
            onCheckedChange = {
                sounds.play(QuizSound.Click)
                onSettingsChange(settings.copy(showCorrections = it))
            },
        )
        SettingsSwitch(
            title = stringResource(Res.string.horizontal_answers),
            description = stringResource(Res.string.horizontal_answers_description),
            checked = settings.horizontalAnswers,
            onCheckedChange = {
                sounds.play(QuizSound.Click)
                onSettingsChange(settings.copy(horizontalAnswers = it))
            },
        )
        SettingsSwitch(
            title = stringResource(Res.string.sound_effects),
            description = stringResource(Res.string.sound_effects_description),
            checked = settings.soundEnabled,
            onCheckedChange = { enabled ->
                onSettingsChange(settings.copy(soundEnabled = enabled))
                if (enabled) sounds.play(QuizSound.Click)
            },
        )
        MenuButton(label = stringResource(Res.string.leaderboard), onClick = onLeaderboard)
        MenuButton(label = stringResource(Res.string.about), onClick = onAbout)
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
