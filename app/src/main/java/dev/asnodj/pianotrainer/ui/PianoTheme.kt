@file:OptIn(ExperimentalTextApi::class)

package dev.asnodj.pianotrainer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.asnodj.pianotrainer.R

/**
 * App palette: a piano on a dark stage. Ivory (the white keys) is the text
 * color; each hand owns a color (right = sky blue, left = violet); amber is
 * reserved for the "play me next" halo and is never used as a fill elsewhere.
 */
object PianoPalette {
    val background = Color(0xFF0F1418)
    val surface = Color(0xFF1A2126)
    val surfaceVariant = Color(0xFF232C33)
    val ivory = Color(0xFFF5F1E8)
    val ivoryDim = Color(0xFFB8B2A6)
    val rightHand = Color(0xFF4FC3F7)
    val leftHand = Color(0xFFB388FF)
    val expectedHalo = Color(0xFFFFC107)
    val correct = Color(0xFF66BB6A)
    val wrong = Color(0xFFEF5350)
    val highway = Color(0xFF0B1013)
}

private val nunito = FontFamily(
    Font(R.font.nunito_variable, weight = FontWeight.Normal),
    Font(
        R.font.nunito_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.nunito_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800)),
    ),
)

private val darkScheme = darkColorScheme(
    background = PianoPalette.background,
    surface = PianoPalette.surface,
    surfaceVariant = PianoPalette.surfaceVariant,
    primary = PianoPalette.expectedHalo,
    onPrimary = Color(0xFF231A00),
    secondary = PianoPalette.rightHand,
    onSecondary = Color(0xFF00232F),
    tertiary = PianoPalette.leftHand,
    onBackground = PianoPalette.ivory,
    onSurface = PianoPalette.ivory,
    onSurfaceVariant = PianoPalette.ivoryDim,
    error = PianoPalette.wrong,
)

/**
 * Builds the app typography: Nunito (rounded, friendly, readable from a music
 * stand) for titles, platform default for running text.
 */
private fun buildTypography(): Typography {
    val defaults = Typography()
    return defaults.copy(
        headlineLarge = defaults.headlineLarge.copy(fontFamily = nunito, fontWeight = FontWeight.ExtraBold),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = nunito, fontWeight = FontWeight.ExtraBold),
        titleLarge = defaults.titleLarge.copy(fontFamily = nunito, fontWeight = FontWeight.SemiBold),
        titleMedium = defaults.titleMedium.copy(fontFamily = nunito, fontWeight = FontWeight.SemiBold),
    )
}

/**
 * App-wide Material theme: always dark (a practice app used next to a piano,
 * often in the evening), ivory-on-charcoal with per-hand accent colors.
 *
 * @param content The app UI.
 */
@Composable
fun PianoTrainerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkScheme,
        typography = buildTypography(),
        content = content,
    )
}
