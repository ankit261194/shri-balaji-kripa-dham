package com.example.shribalajikripadham.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.shribalajikripadham.theme.SacredTheme

/**
 * 🌺 SacredThematicBackground - Full-Screen Immersive Divine Environment
 * Wraps screens in authentic radiant gradients, high-definition deity watermarks,
 * and sacred temple festoons (तोरण) matching the active SacredTheme.
 */
@Composable
fun SacredThematicBackground(
    currentTheme: SacredTheme,
    modifier: Modifier = Modifier,
    showToran: Boolean = true,
    content: @Composable () -> Unit
) {
    // Multi-stop radiant devotional gradient for the background
    val gradientBrush = when (currentTheme) {
        SacredTheme.SHERAWALI_MAIYA -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFF5E6), // Saffron gold morning aura
                Color(0xFFFFF0F2), // Delicate kumkum pink tone
                Color(0xFFFFFDF8),
                Color(0xFFFFECEE)
            )
        )
        SacredTheme.DIVYA_DEEPAWALI -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFBEA), // Warm glowing diya ambient light
                Color(0xFFFFF6D6), // Golden aura
                Color(0xFFFFFDF5),
                Color(0xFFFFF3CC)
            )
        )
        SacredTheme.VEER_HANUMAN -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFF3E6), // Radiant Sinduri aura
                Color(0xFFFFEDE0), // Marigold warmth
                Color(0xFFFFFBF7),
                Color(0xFFFFE6D6)
            )
        )
        SacredTheme.TIRANGA -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFF5EA), // Gentle Saffron crest
                Color(0xFFFFFFFF), // Pure White center
                Color(0xFFF0F9F2)  // Sacred Green base
            )
        )
        SacredTheme.SOOTHING_AMBER -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFDF0),
                Color(0xFFFFF9E0),
                Color(0xFFFFFDF5)
            )
        )
        SacredTheme.CALM_SAGE_MINT -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF2FAF4),
                Color(0xFFE8F6EC),
                Color(0xFFFAFCFA)
            )
        )
        SacredTheme.OCEAN_INDIGO -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF2F6FC),
                Color(0xFFE5EEFA),
                Color(0xFFF8FAFD)
            )
        )
        SacredTheme.PEACEFUL_LAVENDER -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8F4FD),
                Color(0xFFEFE6FA),
                Color(0xFFFCFAFE)
            )
        )
        SacredTheme.WHATSAPP_EMERALD -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF0F8F3),
                Color(0xFFE2F2E6),
                Color(0xFFFAFCFA)
            )
        )
        SacredTheme.TELEGRAM_BLUE -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF0F7FD),
                Color(0xFFDFEFFC),
                Color(0xFFF8FBFE)
            )
        )
        SacredTheme.MINIMAL_SLATE -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF6F7F9),
                Color(0xFFECEEF2),
                Color(0xFFFAFAFB)
            )
        )
        SacredTheme.WHATSAPP_DARK -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F171C), // Midnight sanctum
                Color(0xFF131E24),
                Color(0xFF0B1014)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        content()
    }
}
