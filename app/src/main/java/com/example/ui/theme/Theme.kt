package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = StudioCobalt,
    onPrimary = Color.White,
    primaryContainer = StudioCobaltSoft,
    onPrimaryContainer = Color(0xFF1B2B7A),
    secondary = GlacierCyan,
    onSecondary = Color.White,
    secondaryContainer = GlacierCyanSoft,
    onSecondaryContainer = Color(0xFF07466B),
    tertiary = StudioCoral,
    onTertiary = Color.White,
    tertiaryContainer = StudioCoralSoft,
    onTertiaryContainer = Color(0xFF7A2816),
    error = RoseCrimson,
    onError = Color.White,
    errorContainer = RoseCrimsonSoft,
    onErrorContainer = Color(0xFF7F162C),
    background = CreamBackground,
    onBackground = DeepInk,
    surface = PureCardSurface,
    onSurface = DeepInk,
    surfaceVariant = Color(0xFFEAF0FC),
    onSurfaceVariant = MutedSlate,
    outline = SoftBorder,
    outlineVariant = Color(0xFFCFD9EE)
)

private val DarkColorScheme = darkColorScheme(
    primary = StudioCobalt,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E2952),
    onPrimaryContainer = StudioCobaltSoft,
    secondary = GlacierCyan,
    onSecondary = Color(0xFF041C2B),
    secondaryContainer = Color(0xFF0E2C42),
    onSecondaryContainer = GlacierCyanSoft,
    tertiary = StudioCoral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF42211A),
    onTertiaryContainer = StudioCoralSoft,
    error = RoseCrimson,
    onError = Color.White,
    errorContainer = Color(0xFF4C1724),
    onErrorContainer = RoseCrimsonSoft,
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceElevated,
    onSurfaceVariant = NightMuted,
    outline = NightBorder,
    outlineVariant = Color(0xFF232836)
)

@Composable
fun SpeakQuestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
