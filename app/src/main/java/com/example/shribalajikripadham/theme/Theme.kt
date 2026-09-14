package com.example.shribalajikripadham.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

    // Dynamic 360-degree typography adapted to the active theme font
    val themeTypography = try {
        getSacredTypography(sacredTheme.fontFamily)
    } catch (e: Exception) {
        Typography
    }

    // Dynamic 360-degree shapes for Cards, Buttons, Dialogs adapted to the active theme
    val themeShapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = sacredTheme.buttonShape,
        medium = sacredTheme.cardShape,
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp)
    )

    val activeStyle = SacredStyle(
        theme = sacredTheme,
        fontFamily = sacredTheme.fontFamily,
        cardShape = sacredTheme.cardShape,
        buttonShape = sacredTheme.buttonShape,
        cardBorderWidth = sacredTheme.cardBorderWidth,
        cardElevation = sacredTheme.cardElevation,
        cardBorderColor = sacredTheme.cardBorderColor,
        isDark = sacredTheme.isDark
    )

    CompositionLocalProvider(LocalSacredStyle provides activeStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = themeTypography,
            shapes = themeShapes,
            content = content
        )
    }
}

/**
 * Universal High-Contrast Input Field Colors for Shri Balaji Kripa Dham.
 * Guarantees crystal-clear, deep black text and visible borders across ALL themes and modes.
 */
@Composable
fun sacredOutlinedTextFieldColors(
    containerColor: Color = Color(0xFFFAFAFA),
    focusedContainerColor: Color = Color.White,
    textColor: Color = Color(0xFF111111),
    focusedBorderColor: Color = Color(0xFF8B0000),
    unfocusedBorderColor: Color = Color(0xFF757575),
    labelColor: Color = Color(0xFF333333)
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = textColor,
    unfocusedTextColor = textColor,
    focusedContainerColor = focusedContainerColor,
    unfocusedContainerColor = containerColor,
    focusedLabelColor = focusedBorderColor,
    unfocusedLabelColor = labelColor,
    focusedBorderColor = focusedBorderColor,
    unfocusedBorderColor = unfocusedBorderColor,
    cursorColor = focusedBorderColor,
    focusedPlaceholderColor = Color(0xFF757575),
    unfocusedPlaceholderColor = Color(0xFF757575),
    focusedLeadingIconColor = focusedBorderColor,
    unfocusedLeadingIconColor = Color(0xFF616161),
    focusedTrailingIconColor = focusedBorderColor,
    unfocusedTrailingIconColor = Color(0xFF616161)
)
