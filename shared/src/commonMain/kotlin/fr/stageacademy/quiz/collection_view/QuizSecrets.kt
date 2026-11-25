package fr.stageacademy.quiz.collection_view

import fr.stageacademy.quiz.shared_view.withQuizClickSound
import fr.stageacademy.quiz.shared_view.QuizTag
import fr.stageacademy.quiz.shared_view.MenuButton

import fr.stageacademy.quiz.elapsedMillis
import org.jetbrains.compose.resources.StringResource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.ui.theme.QuizForest
import fr.stageacademy.quiz.ui.theme.QuizLime

internal enum class QuizSecret(
    val title: StringResource,
    val description: StringResource,
    val hint: StringResource,
    val glyph: String? = null,
    val crateRarity: CrateRarity? = null,
) {
    Bug(Res.string.welcome_secret_title, Res.string.welcome_secret_message, Res.string.hint_bug),
    Semicolon(Res.string.settings_secret_title, Res.string.settings_secret_message, Res.string.hint_semicolon),
    Calculator(Res.string.secret_calculator, Res.string.story_calculator, Res.string.hint_calculator),
    Music(Res.string.secret_music, Res.string.story_music, Res.string.hint_music),
    Compass(Res.string.secret_compass, Res.string.story_compass, Res.string.hint_compass),
    Firefly(Res.string.secret_firefly, Res.string.story_firefly, Res.string.hint_firefly),
    Hourglass(Res.string.secret_hourglass, Res.string.story_hourglass, Res.string.hint_hourglass),
    Balance(Res.string.secret_balance, Res.string.story_balance, Res.string.hint_balance),
    Star(Res.string.secret_star, Res.string.story_star, Res.string.hint_star),
    Key(Res.string.secret_key, Res.string.story_key, Res.string.hint_key),
    Rocket(Res.string.secret_rocket, Res.string.story_rocket, Res.string.hint_rocket),
    Constellation(Res.string.secret_constellation, Res.string.story_constellation, Res.string.hint_constellation),
    Postcard(Res.string.secret_postcard, Res.string.story_postcard, Res.string.hint_postcard, "✉"),
    Prism(Res.string.secret_prism, Res.string.story_prism, Res.string.hint_prism, "◇"),
    Bookmark(Res.string.secret_bookmark, Res.string.story_bookmark, Res.string.hint_bookmark, "▣"),
    Medal(Res.string.secret_medal, Res.string.story_medal, Res.string.hint_medal, "◎"),
    Flag(Res.string.secret_flag, Res.string.story_flag, Res.string.hint_flag, "⚑"),
    Branch(Res.string.secret_branch, Res.string.story_branch, Res.string.hint_branch, "⑂"),
    Magnifier(Res.string.secret_magnifier, Res.string.story_magnifier, Res.string.hint_magnifier, "⌕"),
    Pixel(Res.string.secret_pixel, Res.string.story_pixel, Res.string.hint_pixel, "▦"),
    Lantern(Res.string.secret_lantern, Res.string.story_lantern, Res.string.hint_lantern, "✦"),
    Crown(Res.string.secret_crown, Res.string.story_crown, Res.string.hint_crown, "♛"),
    Carousel(Res.string.secret_carousel, Res.string.story_carousel, Res.string.hint_carousel),
    Chameleon(Res.string.secret_chameleon, Res.string.story_chameleon, Res.string.hint_chameleon),
    Boomerang(Res.string.secret_boomerang, Res.string.story_boomerang, Res.string.hint_boomerang),
    Bicorne(Res.string.secret_bicorne, Res.string.story_bicorne, Res.string.hint_bicorne),
    Ice(Res.string.secret_ice, Res.string.story_ice, Res.string.hint_ice),
    Cartographer(Res.string.secret_cartographer, Res.string.story_cartographer, Res.string.hint_cartographer),
    Trapdoor(Res.string.secret_trapdoor, Res.string.story_trapdoor, Res.string.hint_trapdoor),
    Zigzag(Res.string.secret_zigzag, Res.string.story_zigzag, Res.string.hint_zigzag),
    ImperialBee(Res.string.secret_imperial_bee, Res.string.story_imperial_bee, Res.string.hint_imperial_bee),
    Comet(Res.string.secret_comet, Res.string.story_comet, Res.string.hint_comet),
    Crosshair(Res.string.secret_crosshair, Res.string.story_crosshair, Res.string.hint_crosshair),
    Clutch(Res.string.secret_clutch, Res.string.story_clutch, Res.string.hint_clutch),
    NeverFadeAway(Res.string.secret_never_fade_away, Res.string.story_never_fade_away, Res.string.hint_never_fade_away),
    NightCitySignal(Res.string.secret_night_city, Res.string.story_night_city, Res.string.hint_night_city),
    RubberDuck(Res.string.secret_rubber_duck, Res.string.story_rubber_duck, Res.string.hint_crate, crateRarity = CrateRarity.Common),
    FloppyDisk(Res.string.secret_floppy_disk, Res.string.story_floppy_disk, Res.string.hint_crate, crateRarity = CrateRarity.Common),
    NeonJellyfish(Res.string.secret_neon_jellyfish, Res.string.story_neon_jellyfish, Res.string.hint_crate, crateRarity = CrateRarity.Rare),
    ClockworkOwl(Res.string.secret_clockwork_owl, Res.string.story_clockwork_owl, Res.string.hint_crate, crateRarity = CrateRarity.Rare),
    GoldenCartridge(Res.string.secret_golden_cartridge, Res.string.story_golden_cartridge, Res.string.hint_crate, crateRarity = CrateRarity.Epic),
    PocketGalaxy(Res.string.secret_pocket_galaxy, Res.string.story_pocket_galaxy, Res.string.hint_crate, crateRarity = CrateRarity.Epic),
}

@Composable
internal fun Modifier.secretGesture(
    secret: QuizSecret,
    onDiscover: ((QuizSecret) -> Unit)?,
    taps: Int? = null,
    enabled: Boolean = true,
): Modifier {
    val discover by rememberUpdatedState(onDiscover)
    if (!enabled || onDiscover == null) return this

    return pointerInput(secret, taps) {
        if (taps == null) {
            detectTapGestures(onLongPress = { discover?.invoke(secret) })
        } else {
            var tapCount = 0
            var lastTapAt = 0L
            detectTapGestures(onTap = {
                val now = elapsedMillis()
                tapCount = if (now - lastTapAt <= 1_200L) tapCount + 1 else 1
                lastTapAt = now
                if (tapCount == taps) {
                    tapCount = 0
                    discover?.invoke(secret)
                }
            })
        }
    }
}

private enum class CollectionFilter(val label: StringResource) {
    All(Res.string.collection_all),
    Found(Res.string.collection_discovered),
    Locked(Res.string.collection_remaining),
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
internal fun SecretCollection(
    secretFloats: Map<QuizSecret, SecretFloat>,
    onOpen: (QuizSecret) -> Unit,
    onDiscover: (QuizSecret) -> Unit,
    onCrates: () -> Unit,
) {
    val foundSecrets = secretFloats.keys
    val secrets = QuizSecret.entries
    var filter by rememberSaveable { mutableStateOf(CollectionFilter.All) }
    val visibleSecrets = when (filter) {
        CollectionFilter.All -> secrets
        CollectionFilter.Found -> secrets.filter { it in foundSecrets }
        CollectionFilter.Locked -> secrets.filterNot { it in foundSecrets }
    }
    val originalSecrets = secrets.take(QuizSecret.Constellation.ordinal)
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = QuizForest, contentColor = Color.White) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(Res.string.secret_collection),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
            Column(
                modifier = Modifier.fillMaxWidth().secretGesture(
                    QuizSecret.Constellation,
                    onDiscover,
                    enabled = foundSecrets.containsAll(originalSecrets) && QuizSecret.Constellation !in foundSecrets,
                ),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = stringResource(Res.string.secret_collection_count, foundSecrets.size, secrets.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = QuizLime,
                )
                LinearProgressIndicator(
                    progress = { foundSecrets.size.toFloat() / secrets.size },
                    modifier = Modifier.fillMaxWidth(),
                    color = QuizLime,
                    trackColor = Color.White.copy(alpha = 0.23f),
                )
            }
        }
    }
    MenuButton(stringResource(Res.string.crate_title), onCrates)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CollectionFilter.entries.forEach { option ->
            FilterChip(
                selected = filter == option,
                onClick = withQuizClickSound { filter = option },
                label = { Text(stringResource(option.label)) },
            )
        }
    }
    if (visibleSecrets.isEmpty()) {
        Text(
            stringResource(if (filter == CollectionFilter.Found) Res.string.collection_no_discoveries else Res.string.collection_complete),
            modifier = Modifier.padding(12.dp),
            color = colors.onBackground,
        )
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 256.dp && LocalDensity.current.fontScale < 1.5f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            visibleSecrets.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { secret ->
                        SecretTile(secret, secretFloats[secret], { onOpen(secret) }, Modifier.weight(1f))
                    }
                    if (row.size < columns) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SecretTile(secret: QuizSecret, value: SecretFloat?, onOpen: () -> Unit, modifier: Modifier) {
    val found = value != null
    val status = stringResource(if (found) Res.string.secret_found else Res.string.secret_locked)
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = withQuizClickSound(onOpen),
        modifier = modifier.semantics { stateDescription = status },
        shape = RoundedCornerShape(20.dp),
        color = if (found) colors.primaryContainer else colors.surface,
        border = BorderStroke(1.dp, if (found) colors.primary.copy(alpha = 0.25f) else colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 117.dp).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (found) {
                SecretArtwork(secret, Modifier.size(52.dp))
            } else {
                val darkSurface = colors.surface.luminance() < 0.5f
                Surface(
                    modifier = Modifier.size(52.dp).clearAndSetSemantics {},
                    shape = RoundedCornerShape(15.dp),
                    color = if (darkSurface) Color(0xFF383D3B) else Color(0xFFE7E9E8),
                    contentColor = if (darkSurface) Color(0xFFCAD0CD) else Color(0xFF5F6965),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(stringResource(Res.string.secret_question_mark), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
            Text(
                text = if (found) stringResource(secret.title) else stringResource(Res.string.secret_number, (secret.ordinal + 1).toString().padStart(2, '0')),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
            if (value != null) {
                SecretFloatLabel(value, showFullName = false)
            } else {
                Text(
                    stringResource(Res.string.collection_hint_action),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
internal fun SecretEntryDialog(
    secret: QuizSecret,
    secretFloats: Map<QuizSecret, SecretFloat>,
    onDismiss: () -> Unit,
    onDiscover: (QuizSecret) -> Unit,
) {
    val value = secretFloats[secret]
    val found = value != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (found) stringResource(secret.title) else stringResource(Res.string.secret_hint_title, (secret.ordinal + 1).toString().padStart(2, '0')))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (value != null) {
                    SecretArtwork(
                        secret,
                        Modifier.size(82.dp).secretGesture(
                            QuizSecret.Rocket,
                            onDiscover,
                            enabled = secret == QuizSecret.Star && secretFloats.size >= 8,
                        ),
                    )
                    SecretFloatLabel(value, showFullName = true)
                    secret.crateRarity?.let { rarity ->
                        Text(stringResource(Res.string.crate_rarity, stringResource(rarity.label)))
                    }
                }
                Text(stringResource(if (found) secret.description else secret.hint))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) } },
    )
}

@Composable
private fun SecretFloatLabel(value: SecretFloat, showFullName: Boolean) {
    val quality = value.quality
    val badge = when (quality) {
        SecretQuality.FactoryNew -> Color(0xFFFBE18F)
        SecretQuality.MinimalWear -> Color(0xFFC8D9FF)
        SecretQuality.FieldTested -> Color(0xFFAEE3CD)
        SecretQuality.WellWorn -> Color(0xFFE8D7C5)
        SecretQuality.BattleScarred -> Color(0xFFE2E5E3)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = if (showFullName) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        QuizTag(
            text = if (showFullName) "${quality.abbreviation} · ${quality.displayName}" else quality.abbreviation,
            color = badge,
            contentColor = QuizForest,
        )
        Text(
            text = if (showFullName) stringResource(Res.string.secret_float_value, value.decimal) else value.decimal,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
