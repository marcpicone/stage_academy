package fr.stageacademy.quiz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val QuizForest = Color(0xFF12382D)
internal val QuizLime = Color(0xFFD9F397)

private val DayColors = lightColorScheme(
    primary = QuizForest,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1EEDC),
    onPrimaryContainer = Color(0xFF183D30),
    background = Color(0xFFF6F2E8),
    onBackground = Color(0xFF172E26),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF172E26),
    onSurfaceVariant = Color(0xFF4E6259),
    outlineVariant = Color(0xFFD7DED4),
    errorContainer = Color(0xFFFCE5DC),
    onErrorContainer = Color(0xFF7B332A),
)

private val NightColors = darkColorScheme(
    primary = QuizLime,
    onPrimary = QuizForest,
    primaryContainer = Color(0xFF264D3E),
    onPrimaryContainer = Color(0xFFE2F4E5),
    background = Color(0xFF101C18),
    onBackground = Color(0xFFF0F6EE),
    surface = Color(0xFF1B2C25),
    onSurface = Color(0xFFF0F6EE),
    onSurfaceVariant = Color(0xFFBDCDC2),
    outlineVariant = Color(0xFF475E51),
    errorContainer = Color(0xFF63332B),
    onErrorContainer = Color(0xFFFFDFD6),
)

private val DefaultTypography = Typography()
private val QuizTypography = Typography(
    displaySmall = DefaultTypography.displaySmall.copy(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
    ),
    headlineMedium = DefaultTypography.headlineMedium.copy(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = DefaultTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
    labelLarge = DefaultTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
)

@Composable
fun QuizTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) NightColors else DayColors,
        typography = QuizTypography,
        content = content,
    )
}
