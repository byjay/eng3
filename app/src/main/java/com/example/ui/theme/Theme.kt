package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AdventureEmerald,
    onPrimary = Color.White,
    primaryContainer = AdventureEmeraldSoft,
    onPrimaryContainer = Color(0xFF1F5400),
    secondary = SkyQuestBlue,
    onSecondary = Color.White,
    secondaryContainer = SkyQuestBlueSoft,
    onSecondaryContainer = Color(0xFF004B70),
    tertiary = TrophyGold,
    onTertiary = Color(0xFF3E2C00),
    tertiaryContainer = TrophyGoldSoft,
    onTertiaryContainer = Color(0xFF5A4000),
    error = SpartaRed,
    onError = Color.White,
    errorContainer = SpartaRedSoft,
    onErrorContainer = Color(0xFF8C1414),
    background = CreamBackground,
    onBackground = DeepInk,
    surface = PureCardSurface,
    onSurface = DeepInk,
    surfaceVariant = Color(0xFFEEF3F8),
    onSurfaceVariant = MutedSlate,
    outline = SoftBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = AdventureEmerald,
    onPrimary = Color(0xFF0C2600),
    primaryContainer = Color(0xFF214A05),
    onPrimaryContainer = AdventureEmeraldSoft,
    secondary = SkyQuestBlue,
    onSecondary = Color(0xFF00263B),
    secondaryContainer = Color(0xFF083F5C),
    onSecondaryContainer = SkyQuestBlueSoft,
    tertiary = TrophyGold,
    onTertiary = Color(0xFF332400),
    tertiaryContainer = Color(0xFF523C00),
    onTertiaryContainer = TrophyGoldSoft,
    error = SpartaRed,
    onError = Color.White,
    errorContainer = Color(0xFF5F1414),
    onErrorContainer = SpartaRedSoft,
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = Color(0xFF233842),
    onSurfaceVariant = NightMuted,
    outline = NightBorder
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
