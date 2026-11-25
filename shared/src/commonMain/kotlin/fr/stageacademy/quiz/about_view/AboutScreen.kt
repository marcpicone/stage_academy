package fr.stageacademy.quiz.about_view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.collection_view.secretCircle
import fr.stageacademy.quiz.collection_view.secretRhythm
import org.jetbrains.compose.resources.stringResource
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.shared_view.MenuCard

@Composable
internal fun AboutScreen(onCircle: (() -> Unit)?, onRhythm: (() -> Unit)?) {
    MenuCard(title = stringResource(Res.string.about), titleModifier = Modifier.secretRhythm(onRhythm)) {
        Column(
            modifier = Modifier.fillMaxWidth().secretCircle(onCircle),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.about_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
