package com.example.shribalajikripadham.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.shribalajikripadham.theme.SacredTheme

/**
 * 🌺 MandirToranHeader - Traditional Consecrated Indian Temple Festoon
 * Renders a festive garland of marigold flowers (गेंदे के फूल), mango leaves (आम के पत्ते),
 * and divine brass bells (पीतल की घंटियां) across the top header.
 */
@Composable
fun MandirToranHeader(
    currentTheme: SacredTheme,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
    ) {
        val width = size.width
        val ropeY = 4f

        // 1. Golden Cord / Sacred Kalava Thread spanning full width (मौली धागा / स्वर्ण रज्जु)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFD4AF37),
                    Color(0xFFFFD700),
                    Color(0xFFE65100),
                    Color(0xFFFFD700),
                    Color(0xFFD4AF37)
                )
            ),
            start = Offset(0f, ropeY),
            end = Offset(width, ropeY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        // 2. Garland Elements spaced evenly (Leaves, Marigolds, Bells / Diyas)
        val itemCount = 13
        val spacing = width / itemCount

        for (i in 0..itemCount) {
            val itemX = i * spacing + (spacing / 2f)

            when (i % 3) {
                // A. Sacred Mango Leaf (आम का पत्ता)
                0 -> {
                    drawMangoLeaf(itemX, ropeY, currentTheme)
                }
                // B. Layered Marigold Flower (गेंदे का फूल)
                1 -> {
                    drawMarigoldFlower(itemX, ropeY + 2f, currentTheme)
                }
                // C. Temple Brass Bell or Diya (घंटी / दीपक)
                2 -> {
                    if (currentTheme == SacredTheme.DIVYA_DEEPAWALI) {
                        drawMiniDiya(itemX, ropeY + 4f)
                    } else {
                        drawTempleBell(itemX, ropeY + 2f, currentTheme)
                    }
                }
            }
        }
    }
}

// Draw Mango Leaf
private fun DrawScope.drawMangoLeaf(x: Float, startY: Float, theme: SacredTheme) {
    val leafColor = if (theme == SacredTheme.WHATSAPP_DARK) Color(0xFF2E7D32) else Color(0xFF388E3C)
    val leafGold = Color(0xFF81C784)
    val leafLength = 18f
    val leafWidth = 6.5f

    val path = Path().apply {
        moveTo(x, startY)
        cubicTo(x - leafWidth, startY + leafLength * 0.4f, x - leafWidth * 0.8f, startY + leafLength * 0.8f, x, startY + leafLength)
        cubicTo(x + leafWidth * 0.8f, startY + leafLength * 0.8f, x + leafWidth, startY + leafLength * 0.4f, x, startY)
        close()
    }
    drawPath(path = path, color = leafColor, style = Fill)
    drawLine(
        color = leafGold,
        start = Offset(x, startY),
        end = Offset(x, startY + leafLength * 0.85f),
        strokeWidth = 1f
    )
}

// Draw Dense Textured Marigold Flower (गेंदे का फूल)
private fun DrawScope.drawMarigoldFlower(x: Float, startY: Float, theme: SacredTheme) {
    val centerColor = when (theme) {
        SacredTheme.SHERAWALI_MAIYA -> Color(0xFFD32F2F) // Deep crimson red center
        SacredTheme.VEER_HANUMAN -> Color(0xFFFF5722) // Saffron Sinduri
        else -> Color(0xFFFF8F00) // Rich orange
    }
    val outerColor = Color(0xFFFFD54F) // Radiant gold yellow

    val flowerCenterY = startY + 9f
    // Petal clusters
    drawCircle(color = outerColor, radius = 7f, center = Offset(x, flowerCenterY), style = Fill)
    drawCircle(color = centerColor, radius = 4.5f, center = Offset(x, flowerCenterY), style = Fill)
    drawCircle(color = Color(0xFFFFF176), radius = 2f, center = Offset(x, flowerCenterY), style = Fill)
}

// Draw Temple Brass Bell (पीतल की घंटी)
private fun DrawScope.drawTempleBell(x: Float, startY: Float, theme: SacredTheme) {
    val brassGold = Color(0xFFFFD700)
    val brassShadow = Color(0xFFB8860B)
    val bellTopY = startY + 4f
    val bellHeight = 12f

    // Hanging string
    drawLine(color = brassShadow, start = Offset(x, startY), end = Offset(x, bellTopY), strokeWidth = 1.2f)

    // Bell dome
    val bellPath = Path().apply {
        moveTo(x, bellTopY)
        cubicTo(x - 5f, bellTopY + bellHeight * 0.5f, x - 7f, bellTopY + bellHeight * 0.85f, x - 6.5f, bellTopY + bellHeight)
        lineTo(x + 6.5f, bellTopY + bellHeight)
        cubicTo(x + 7f, bellTopY + bellHeight * 0.85f, x + 5f, bellTopY + bellHeight * 0.5f, x, bellTopY)
        close()
    }
    drawPath(path = bellPath, color = brassGold, style = Fill)
    drawPath(path = bellPath, color = brassShadow, style = Stroke(width = 1.2f))

    // Clapper at base
    drawCircle(color = brassShadow, radius = 1.8f, center = Offset(x, bellTopY + bellHeight + 2f), style = Fill)
}

// Draw Mini Diya for Deepawali (दीपक)
private fun DrawScope.drawMiniDiya(x: Float, startY: Float) {
    val clayColor = Color(0xFFD84315)
    val flameColor = Color(0xFFFFD700)
    val diyaY = startY + 8f

    // String
    drawLine(color = Color(0xFFB8860B), start = Offset(x, startY), end = Offset(x, diyaY), strokeWidth = 1f)

    // Diya bowl
    val diyaPath = Path().apply {
        moveTo(x - 6f, diyaY)
        cubicTo(x - 4f, diyaY + 5f, x + 4f, diyaY + 5f, x + 6f, diyaY)
        close()
    }
    drawPath(path = diyaPath, color = clayColor, style = Fill)

    // Flame
    val flamePath = Path().apply {
        moveTo(x, diyaY)
        cubicTo(x - 2.5f, diyaY - 3f, x - 1f, diyaY - 7f, x, diyaY - 8.5f)
        cubicTo(x + 1f, diyaY - 7f, x + 2.5f, diyaY - 3f, x, diyaY)
        close()
    }
    drawPath(path = flamePath, color = flameColor, style = Fill)
}
