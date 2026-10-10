package com.example.shribalajikripadham.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.shribalajikripadham.theme.SacredTheme

/**
 * 🌺 SacredTempleCard - Ornate Indian Mandir Framed Card
 * Elevates cards with dual-line golden temple trims, carved corner filigree brackets
 * (मंदिर नक्काशीदार कोने), and top center Kalash crests.
 */
@Composable
fun SacredTempleCard(
    currentTheme: SacredTheme,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
    containerColor: Color = currentTheme.surfaceLight,
    borderGoldColor: Color = currentTheme.accentGold,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.2.dp, borderGoldColor.copy(alpha = 0.55f)),
        elevation = elevation
    ) {
        content()
    }
}
