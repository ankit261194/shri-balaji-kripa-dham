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
        Box(modifier = Modifier.fillMaxWidth()) {
            // Layer 1: Traditional Mandir Corner Brackets & Top Arch Canvas
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val gold = borderGoldColor.copy(alpha = 0.65f)
                val cornerSpan = 22f

                // A. Top-Left Corner Bracket (उत्तर-पश्चिम नक्काशी)
                drawLine(
                    color = gold,
                    start = Offset(14f, 14f),
                    end = Offset(14f + cornerSpan, 14f),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = gold,
                    start = Offset(14f, 14f),
                    end = Offset(14f, 14f + cornerSpan),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = gold, radius = 2.2f, center = Offset(14f, 14f), style = Fill)

                // B. Top-Right Corner Bracket (उत्तर-पूर्व नक्काशी)
                drawLine(
                    color = gold,
                    start = Offset(w - 14f, 14f),
                    end = Offset(w - 14f - cornerSpan, 14f),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = gold,
                    start = Offset(w - 14f, 14f),
                    end = Offset(w - 14f, 14f + cornerSpan),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = gold, radius = 2.2f, center = Offset(w - 14f, 14f), style = Fill)

                // C. Bottom-Left Corner Bracket (दक्षिण-पश्चिम नक्काशी)
                drawLine(
                    color = gold,
                    start = Offset(14f, h - 14f),
                    end = Offset(14f + cornerSpan, h - 14f),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = gold,
                    start = Offset(14f, h - 14f),
                    end = Offset(14f, h - 14f - cornerSpan),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = gold, radius = 2.2f, center = Offset(14f, h - 14f), style = Fill)

                // D. Bottom-Right Corner Bracket (दक्षिण-पूर्व नक्काशी)
                drawLine(
                    color = gold,
                    start = Offset(w - 14f, h - 14f),
                    end = Offset(w - 14f - cornerSpan, h - 14f),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = gold,
                    start = Offset(w - 14f, h - 14f),
                    end = Offset(w - 14f, h - 14f - cornerSpan),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = gold, radius = 2.2f, center = Offset(w - 14f, h - 14f), style = Fill)

                // E. Auspicious Top-Center Temple Arch Peak (मंदिर शिखर कलश)
                val cx = w / 2f
                val peakPath = Path().apply {
                    moveTo(cx - 16f, 0f)
                    lineTo(cx, 6f)
                    lineTo(cx + 16f, 0f)
                    close()
                }
                drawPath(path = peakPath, color = gold.copy(alpha = 0.4f), style = Fill)
            }

            // Layer 2: Card Content
            content()
        }
    }
}
