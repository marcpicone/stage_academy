package fr.stageacademy.quiz.collection_view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.shared_view.MenuButton
import fr.stageacademy.quiz.shared_view.MenuCard
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SecretCratesScreen(progress: CollectionProgress, onOpen: (SecretCrate) -> Unit, purchaseEnabled: Boolean) {
    MenuCard(title = stringResource(Res.string.crate_title)) {
        Text(
            stringResource(Res.string.crate_balance, progress.points),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(Res.string.crate_instructions),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    for (crate in SecretCrate.entries) {
        val odds = crate.odds(progress.floats.keys)
        MenuCard(title = stringResource(crate.title)) {
            if (odds.isEmpty()) {
                Text(stringResource(Res.string.crate_sold_out), style = MaterialTheme.typography.bodyMedium)
            } else {
                for ((rarity, chance) in odds) {
                    Text(
                        stringResource(Res.string.crate_odds, stringResource(rarity.label), "$chance %"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                MenuButton(
                    label = stringResource(Res.string.crate_open, crate.cost),
                    enabled = purchaseEnabled && progress.points >= crate.cost,
                    onClick = { onOpen(crate) },
                )
            }
        }
    }
}
