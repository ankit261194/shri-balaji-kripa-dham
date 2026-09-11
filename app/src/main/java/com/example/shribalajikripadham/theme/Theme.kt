package com.example.shribalajikripadham.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SaffronLight,
    onPrimary = Color.White,
    secondary = GoldLight,
    onSecondary = Color.Black,
    tertiary = MaroonAccent,
    background = Color(0xFF141210),
    surface = Color(0xFF1F1C18),
    onBackground = Color(0xFFEDE0D4),
    onSurface = Color(0xFFEDE0D4)
)

private val LightColorScheme = lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = SaffronDark,
    secondary = GoldSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFF8E1),
    onSecondaryContainer = Color(0xFF5D4037),
    tertiary = MaroonAccent,
    onTertiary = Color.White,
    background = SacredBackgroundLight,
    surface = SacredSurfaceLight,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

@Composable
fun ShriBalajiKripaDhamTheme(
    sacredTheme: SacredTheme = SacredTheme.ROYAL_MAROON,
    content: @Composable () -> Unit
) {
    val colorScheme = if (sacredTheme.isDark) {
        darkColorScheme(
            primary = sacredTheme.primaryColor,
            onPrimary = Color.Black,
            secondary = sacredTheme.secondaryColor,
            onSecondary = Color.Black,
            tertiary = sacredTheme.accentGold,
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onBackground = Color(0xFFEDE0D4),
            onSurface = Color(0xFFEDE0D4)
        )
    } else {
        lightColorScheme(
            primary = sacredTheme.primaryColor,
            onPrimary = Color.White,
            primaryContainer = sacredTheme.secondaryColor.copy(alpha = 0.2f),
            onPrimaryContainer = sacredTheme.primaryColor,
            secondary = sacredTheme.secondaryColor,
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFFFFF8E1),
            onSecondaryContainer = Color(0xFF5D4037),
            tertiary = sacredTheme.accentGold,
            onTertiary = Color.Black,
            background = SacredBackgroundLight,
            surface = SacredSurfaceLight,
            onBackground = TextPrimaryDark,
            onSurface = TextPrimaryDark
        )
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
