package dev.nheox.mintal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.nheox.mintal.data.prefs.ThemeChoice
import dev.nheox.mintal.ui.theme.MintalPaletteValues as P

private val LightScheme = lightColorScheme(
    primary = P.Mint40,
    onPrimary = Color.White,
    primaryContainer = P.Mint90,
    onPrimaryContainer = P.Mint10,
    secondary = P.Sky40,
    onSecondary = Color.White,
    secondaryContainer = P.Sky90,
    onSecondaryContainer = P.Sky20,
    tertiary = P.Mint60,
    onTertiary = Color.White,
    background = P.Mint99,
    onBackground = P.Ink,
    surface = P.Paper,
    onSurface = P.Ink,
    surfaceVariant = P.Mint95,
    onSurfaceVariant = P.InkMuted,
    outline = P.Mint60,
    outlineVariant = P.Mint80,
    error = Color(0xFF8E3B2F),
    onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = P.Mint80,
    onPrimary = P.Mint10,
    primaryContainer = P.Mint30,
    onPrimaryContainer = P.Mint90,
    secondary = P.Sky80,
    onSecondary = P.Sky20,
    secondaryContainer = P.Sky30,
    onSecondaryContainer = P.Sky90,
    tertiary = P.Mint60,
    onTertiary = P.Mint10,
    background = P.Night,
    onBackground = P.NightInk,
    surface = P.NightRaised,
    onSurface = P.NightInk,
    surfaceVariant = P.Mint20,
    onSurfaceVariant = P.Mint80,
    outline = P.Mint60,
    outlineVariant = P.Mint30,
    error = Color(0xFFE8A294),
    onError = Color(0xFF3B100A),
)

val LocalMintalPalette = staticCompositionLocalOf { MintalLightPalette }

/** Mintal's own semantic tokens, alongside the Material ones. */
val mintalPalette: MintalPalette
    @Composable @ReadOnlyComposable get() = LocalMintalPalette.current

@Composable
fun MintalTheme(
    theme: ThemeChoice = ThemeChoice.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (theme) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    CompositionLocalProvider(
        LocalMintalPalette provides if (dark) MintalDarkPalette else MintalLightPalette,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = MintalTypography,
            content = content,
        )
    }
}
