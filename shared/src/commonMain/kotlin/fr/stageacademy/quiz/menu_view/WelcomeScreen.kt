package fr.stageacademy.quiz.menu_view

import fr.stageacademy.quiz.shared_view.withQuizClickSound

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.collection_view.QuizSecret
import fr.stageacademy.quiz.collection_view.secretGesture
import fr.stageacademy.quiz.shared_view.PanelShape
import fr.stageacademy.quiz.shared_view.QuizTag
import fr.stageacademy.quiz.ui.theme.QuizForest
import fr.stageacademy.quiz.ui.theme.QuizLime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTabs(profileSelected: Boolean, onHome: () -> Unit, onProfile: () -> Unit) {
    PrimaryTabRow(selectedTabIndex = if (profileSelected) 1 else 0) {
        Tab(
            selected = !profileSelected,
            onClick = withQuizClickSound(onHome),
            text = { Text(stringResource(Res.string.home)) },
        )
        Tab(
            selected = profileSelected,
            onClick = withQuizClickSound(onProfile),
            text = { Text(stringResource(Res.string.profile)) },
        )
    }
}

@Composable
internal fun WelcomePanel(questionCount: Int, onDiscover: (QuizSecret) -> Unit) {
    val darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Surface(shape = PanelShape, color = QuizForest, contentColor = Color.White) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CodeIllustration(
                Modifier.fillMaxWidth().height(152.dp).secretGesture(QuizSecret.Bug, onDiscover),
            )
            QuizTag(
                text = stringResource(Res.string.question_count, questionCount),
                color = QuizLime,
                contentColor = QuizForest,
                modifier = Modifier.secretGesture(QuizSecret.Calculator, onDiscover, taps = 3),
            )
            Text(
                text = stringResource(Res.string.welcome_title),
                modifier = Modifier.widthIn(max = 260.dp).semantics { heading() }
                    .secretGesture(QuizSecret.Firefly, onDiscover, taps = 2, enabled = darkTheme),
                style = MaterialTheme.typography.displaySmall,
            )
        }
    }

    Surface(shape = PanelShape) {
        Text(
            text = stringResource(Res.string.welcome_message),
            modifier = Modifier.fillMaxWidth().padding(22.dp).secretGesture(QuizSecret.Postcard, onDiscover),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

}

@Composable
private fun CodeIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val spacing = 16.dp.toPx()
        for (column in 0..(size.width / spacing).toInt()) {
            for (row in 0..(size.height / spacing).toInt()) {
                drawCircle(QuizLime.copy(alpha = 0.20f), 1.dp.toPx(), Offset(column * spacing, row * spacing))
            }
        }

        val tileWidth = 126.dp.toPx().coerceAtMost(size.width * 0.8f)
        val tileHeight = tileWidth * 0.72f
        val tileSize = Size(tileWidth, tileHeight)
        val tilePosition = Offset(center.x - tileWidth / 2, center.y - tileHeight / 2)
        val corner = CornerRadius(18.dp.toPx())
        rotate(-16f) {
            drawRoundRect(Color(0xFF285648), tilePosition, tileSize, corner)
            drawRoundRect(QuizLime.copy(alpha = 0.5f), tilePosition, tileSize, corner, style = Stroke(1.dp.toPx()))
        }
        rotate(9f) {
            drawRoundRect(QuizLime, tilePosition, tileSize, corner)
            val unit = tileWidth / 12f
            val code = Path().apply {
                moveTo(center.x - 2.1f * unit, center.y - unit)
                lineTo(center.x - 3.4f * unit, center.y)
                lineTo(center.x - 2.1f * unit, center.y + unit)
                moveTo(center.x + 0.7f * unit, center.y - 1.55f * unit)
                lineTo(center.x - 0.7f * unit, center.y + 1.55f * unit)
                moveTo(center.x + 2.1f * unit, center.y - unit)
                lineTo(center.x + 3.4f * unit, center.y)
                lineTo(center.x + 2.1f * unit, center.y + unit)
            }
            drawPath(code, QuizForest, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
