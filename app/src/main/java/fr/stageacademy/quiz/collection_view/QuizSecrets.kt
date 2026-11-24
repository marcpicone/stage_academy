package fr.stageacademy.quiz.collection_view

import android.os.SystemClock
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.R

internal enum class QuizSecret(val title: Int, val description: Int, val hint: Int) {
    Bug(R.string.welcome_secret_title, R.string.welcome_secret_message, R.string.hint_bug),
    Semicolon(R.string.settings_secret_title, R.string.settings_secret_message, R.string.hint_semicolon),
    Calculator(R.string.secret_calculator, R.string.story_calculator, R.string.hint_calculator),
    Music(R.string.secret_music, R.string.story_music, R.string.hint_music),
    Compass(R.string.secret_compass, R.string.story_compass, R.string.hint_compass),
    Firefly(R.string.secret_firefly, R.string.story_firefly, R.string.hint_firefly),
    Hourglass(R.string.secret_hourglass, R.string.story_hourglass, R.string.hint_hourglass),
    Balance(R.string.secret_balance, R.string.story_balance, R.string.hint_balance),
    Star(R.string.secret_star, R.string.story_star, R.string.hint_star),
    Key(R.string.secret_key, R.string.story_key, R.string.hint_key),
    Rocket(R.string.secret_rocket, R.string.story_rocket, R.string.hint_rocket),
    Constellation(R.string.secret_constellation, R.string.story_constellation, R.string.hint_constellation),
}

@Composable
internal fun Modifier.secretGesture(
    secret: QuizSecret,
    onDiscover: (QuizSecret) -> Unit,
    taps: Int? = null,
    enabled: Boolean = true,
): Modifier {
    val discover by rememberUpdatedState(onDiscover)
    if (!enabled) return this

    return pointerInput(secret, taps) {
        if (taps == null) {
            detectTapGestures(onLongPress = { discover(secret) })
        } else {
            var tapCount = 0
            var lastTapAt = 0L
            detectTapGestures(onTap = {
                val now = SystemClock.uptimeMillis()
                tapCount = if (now - lastTapAt <= 1_200L) tapCount + 1 else 1
                lastTapAt = now
                if (tapCount == taps) {
                    tapCount = 0
                    discover(secret)
                }
            })
        }
    }
}

@Composable
internal fun SecretCollection(
    foundSecrets: Set<QuizSecret>,
    onOpen: (QuizSecret) -> Unit,
    onDiscover: (QuizSecret) -> Unit,
) {
    val secrets = QuizSecret.entries
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.secret_collection),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
            Column(
                modifier = Modifier.fillMaxWidth().secretGesture(
                    QuizSecret.Constellation,
                    onDiscover,
                    enabled = foundSecrets.size == secrets.size - 1 && QuizSecret.Constellation !in foundSecrets,
                ),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = stringResource(R.string.secret_collection_count, foundSecrets.size, secrets.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { foundSecrets.size.toFloat() / secrets.size },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = colors.primaryContainer,
                )
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 256.dp && LocalDensity.current.fontScale < 1.5f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            secrets.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { secret ->
                        SecretTile(secret, secret in foundSecrets, { onOpen(secret) }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SecretTile(secret: QuizSecret, found: Boolean, onOpen: () -> Unit, modifier: Modifier) {
    val status = stringResource(if (found) R.string.secret_found else R.string.secret_locked)
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onOpen,
        modifier = modifier.semantics { stateDescription = status },
        shape = RoundedCornerShape(20.dp),
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
                        Text(stringResource(R.string.secret_question_mark), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
            Text(
                text = if (found) stringResource(secret.title) else stringResource(R.string.secret_number, secret.ordinal + 1),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun SecretEntryDialog(
    secret: QuizSecret,
    foundSecrets: Set<QuizSecret>,
    onDismiss: () -> Unit,
    onDiscover: (QuizSecret) -> Unit,
) {
    val found = secret in foundSecrets
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (found) stringResource(secret.title) else stringResource(R.string.secret_hint_title, secret.ordinal + 1))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (found) {
                    SecretArtwork(
                        secret,
                        Modifier.size(82.dp).secretGesture(
                            QuizSecret.Rocket,
                            onDiscover,
                            enabled = secret == QuizSecret.Star && foundSecrets.size >= 8,
                        ),
                    )
                }
                Text(stringResource(if (found) secret.description else secret.hint))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
