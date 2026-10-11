package com.example.shribalajikripadham.ui.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pro-Tier Animated Sacred Hourglass (रेत-घड़ी).
 * - Rotates 180° smoothly on each cycle (घूमती हुई रेत-घड़ी).
 * - Sacred golden sand trickles continuously from the top bulb to the bottom bulb (गिरती हुई रेत/मिट्टी).
 * - Dynamic animated sand particles & growing sand mound at the bottom.
 */
@Composable
fun AnimatedSacredHourglass(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    primaryColor: Color = Color(0xFFFFD54F),
    sandColor: Color = Color(0xFFFFB300),
    sandDarkColor: Color = Color(0xFFE65100),
    glassColor: Color = Color(0x99FFFFFF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HourglassCycle")

    // Full cycle: 4000ms (3200ms sand trickling down, 800ms smooth 180° flip)
    val cycleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CycleProgress"
    )

    // Sand grains / particles dropping rapidly down through the neck
    val grainProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GrainProgress"
    )

    // Rotation angle: 0° for first 75% (0..3000ms), then smoothly flips 180° (3000..3800ms)
    val rotationAngle = when {
        cycleProgress < 0.75f -> 0f
        cycleProgress < 0.95f -> {
            val rotT = (cycleProgress - 0.75f) / 0.20f
            // FastOutSlowInEasing curve
            val eased = FastOutSlowInEasing.transform(rotT)
            eased * 180f
        }
        else -> 180f
    }

    // Sand flow progress (0f when top full, 1f when top empty)
    // Runs from 0.0 to 0.75 of the cycle
    val flowProgress = (cycleProgress / 0.75f).coerceIn(0f, 1f)
    val isTrickling = cycleProgress < 0.74f

    Canvas(
        modifier = modifier.size(size)
    ) {
        val w = this.size.width
        val h = this.size.height
        val centerX = w * 0.5f
        val centerY = h * 0.5f

        // Apply 180° rotation around center
        rotate(degrees = rotationAngle, pivot = Offset(centerX, centerY)) {
            // 1. Geometry Dimensions
            val topPlateY = h * 0.06f
            val topPlateH = h * 0.07f
            val bottomPlateH = h * 0.07f
            val bottomPlateY = h * 0.87f
            val plateLeft = w * 0.18f
            val plateRight = w * 0.82f
            val plateW = plateRight - plateLeft

            val glassTop = topPlateY + topPlateH
            val glassBottom = bottomPlateY
            val glassH = glassBottom - glassTop
            val neckW = w * 0.14f
            val neckHalfW = neckW * 0.5f
            val neckY = centerY

            val bulbMaxLeft = w * 0.22f
            val bulbMaxRight = w * 0.78f

            // 2. Draw Glass Contour (Hourglass Outer Shell)
            val glassPath = Path().apply {
                // Top rim
                moveTo(bulbMaxLeft, glassTop)
                lineTo(bulbMaxRight, glassTop)
                // Down right curve to neck
                cubicTo(
                    bulbMaxRight, glassTop + glassH * 0.25f,
                    centerX + neckHalfW * 1.5f, neckY - glassH * 0.1f,
                    centerX + neckHalfW, neckY
                )
                // Down right curve to bottom rim
                cubicTo(
                    centerX + neckHalfW * 1.5f, neckY + glassH * 0.1f,
                    bulbMaxRight, glassBottom - glassH * 0.25f,
                    bulbMaxRight, glassBottom
                )
                // Bottom rim
                lineTo(bulbMaxLeft, glassBottom)
                // Up left curve to neck
                cubicTo(
                    bulbMaxLeft, glassBottom - glassH * 0.25f,
                    centerX - neckHalfW * 1.5f, neckY + glassH * 0.1f,
                    centerX - neckHalfW, neckY
                )
                // Up left curve to top rim
                cubicTo(
                    centerX - neckHalfW * 1.5f, neckY - glassH * 0.1f,
                    bulbMaxLeft, glassTop + glassH * 0.25f,
                    bulbMaxLeft, glassTop
                )
                close()
            }

            // Glass Translucent Fill
            drawPath(
                path = glassPath,
                color = Color.White.copy(alpha = 0.10f),
                style = Fill
            )

            // 3. Draw Sand Inside Top Bulb
            if (flowProgress < 0.98f) {
                val topSandSurfaceY = glassTop + (neckY - glassTop) * flowProgress
                val surfaceRatio = flowProgress
                val currentLeft = bulbMaxLeft + (centerX - neckHalfW - bulbMaxLeft) * surfaceRatio
                val currentRight = bulbMaxRight - (bulbMaxRight - (centerX + neckHalfW)) * surfaceRatio

                val topSandPath = Path().apply {
                    moveTo(currentLeft, topSandSurfaceY)
                    // Slight concave depression in draining sand surface
                    quadraticTo(
                        centerX, topSandSurfaceY + h * 0.04f * (1f - surfaceRatio),
                        currentRight, topSandSurfaceY
                    )
                    // Down to neck
                    cubicTo(
                        currentRight, neckY - (neckY - topSandSurfaceY) * 0.5f,
                        centerX + neckHalfW, neckY - h * 0.02f,
                        centerX + neckHalfW, neckY
                    )
                    lineTo(centerX - neckHalfW, neckY)
                    cubicTo(
                        centerX - neckHalfW, neckY - h * 0.02f,
                        currentLeft, neckY - (neckY - topSandSurfaceY) * 0.5f,
                        currentLeft, topSandSurfaceY
                    )
                    close()
                }

                drawPath(
                    path = topSandPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(sandColor, sandDarkColor),
                        startY = topSandSurfaceY,
                        endY = neckY
                    ),
                    style = Fill
                )
            }

            // 4. Draw Sand in Bottom Bulb (Rising Mound)
            if (flowProgress > 0.02f) {
                val moundHeight = (glassBottom - neckY) * 0.85f * flowProgress
                val moundPeakY = glassBottom - moundHeight
                val spreadW = (bulbMaxRight - centerX) * flowProgress.coerceAtLeast(0.4f)

                val bottomSandPath = Path().apply {
                    moveTo(centerX - spreadW, glassBottom)
                    // Smooth conical sand mound peak
                    cubicTo(
                        centerX - spreadW * 0.5f, glassBottom - moundHeight * 0.3f,
                        centerX - neckHalfW * 0.8f, moundPeakY,
                        centerX, moundPeakY
                    )
                    cubicTo(
                        centerX + neckHalfW * 0.8f, moundPeakY,
                        centerX + spreadW * 0.5f, glassBottom - moundHeight * 0.3f,
                        centerX + spreadW, glassBottom
                    )
                    lineTo(centerX - spreadW, glassBottom)
                    close()
                }

                drawPath(
                    path = bottomSandPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(sandColor, sandDarkColor),
                        startY = moundPeakY,
                        endY = glassBottom
                    ),
                    style = Fill
                )
            }

            // 5. Falling Sand Trickle Stream & Dropping Particles
            if (isTrickling) {
                val currentMoundPeakY = glassBottom - ((glassBottom - neckY) * 0.85f * flowProgress)

                // Central Stream Line
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(sandColor, sandDarkColor),
                        startY = neckY,
                        endY = currentMoundPeakY
                    ),
                    start = Offset(centerX, neckY),
                    end = Offset(centerX, currentMoundPeakY),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Rapid Falling Grains / Particles
                val streamLength = (currentMoundPeakY - neckY).coerceAtLeast(1f)
                val grainRadii = listOf(0.8.dp.toPx(), 1.0.dp.toPx(), 0.8.dp.toPx())
                for (i in 0..2) {
                    val p = (grainProgress + i * 0.333f) % 1f
                    val grainY = neckY + p * streamLength
                    drawCircle(
                        color = primaryColor,
                        radius = grainRadii[i],
                        center = Offset(centerX, grainY)
                    )
                }

                // Impact Sparkle at the landing mound
                drawCircle(
                    color = primaryColor.copy(alpha = 0.85f),
                    radius = 1.2.dp.toPx(),
                    center = Offset(centerX, currentMoundPeakY)
                )
            }

            // 6. Draw Glass Outline (Delicate Golden Stroke)
            drawPath(
                path = glassPath,
                color = glassColor,
                style = Stroke(width = 1.3.dp.toPx())
            )

            // Glass Reflection / Shimmer highlight on left edge
            val shimmerPath = Path().apply {
                moveTo(bulbMaxLeft + w * 0.04f, glassTop + h * 0.06f)
                cubicTo(
                    bulbMaxLeft + w * 0.05f, glassTop + h * 0.15f,
                    centerX - neckHalfW * 1.8f, neckY - h * 0.08f,
                    centerX - neckHalfW * 1.5f, neckY - h * 0.02f
                )
            }
            drawPath(
                path = shimmerPath,
                color = Color.White.copy(alpha = 0.45f),
                style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round)
            )

            // 7. Top & Bottom Metallic Plates (Brass / Gold Base)
            val cornerRad = CornerRadius(2.dp.toPx())
            // Top Plate
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.85f), primaryColor, primaryColor.copy(alpha = 0.85f))
                ),
                topLeft = Offset(plateLeft, topPlateY),
                size = Size(plateW, topPlateH),
                cornerRadius = cornerRad
            )
            // Bottom Plate
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.85f), primaryColor, primaryColor.copy(alpha = 0.85f))
                ),
                topLeft = Offset(plateLeft, bottomPlateY),
                size = Size(plateW, bottomPlateH),
                cornerRadius = cornerRad
            )

            // 8. Side Pillars (Brass Support Rods)
            val pillarColor = primaryColor.copy(alpha = 0.90f)
            val pillarWidth = 1.2.dp.toPx()
            // Left Pillar
            drawLine(
                color = pillarColor,
                start = Offset(bulbMaxLeft - w * 0.02f, topPlateY + topPlateH),
                end = Offset(bulbMaxLeft - w * 0.02f, bottomPlateY),
                strokeWidth = pillarWidth
            )
            // Right Pillar
            drawLine(
                color = pillarColor,
                start = Offset(bulbMaxRight + w * 0.02f, topPlateY + topPlateH),
                end = Offset(bulbMaxRight + w * 0.02f, bottomPlateY),
                strokeWidth = pillarWidth
            )
        }
    }
}
