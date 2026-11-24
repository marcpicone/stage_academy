package fr.stageacademy.quiz.about_view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.stageacademy.quiz.R
import fr.stageacademy.quiz.shared_view.MenuCard

@Composable
internal fun AboutScreen() {
    MenuCard(title = stringResource(R.string.about)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
