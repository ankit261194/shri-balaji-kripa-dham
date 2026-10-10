package com.example.shribalajikripadham.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.shribalajikripadham.theme.SacredTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * 🌺 Sacred Theme Graphics Engine for Shri Balaji Kripa Dham
 * Renders authentic, high-definition vector watermarks, divine deity motifs,
 * golden temple frames, and festive torans across all 12 themes.
 */
object SacredThemeGraphics {

    fun drawThemeWatermark(
        drawScope: DrawScope,
        theme: SacredTheme,
        primaryColor: Color,
        accentColor: Color
    ) {
        with(drawScope) {
            when (theme) {
                SacredTheme.SHERAWALI_MAIYA -> drawSherawaliWatermark(primaryColor, accentColor)
                SacredTheme.DIVYA_DEEPAWALI -> drawDeepawaliWatermark(primaryColor, accentColor)
                SacredTheme.VEER_HANUMAN -> drawVeerHanumanWatermark(primaryColor, accentColor)
                SacredTheme.TIRANGA -> drawTirangaWatermark(primaryColor, accentColor)
                SacredTheme.SOOTHING_AMBER -> drawSandalwoodWatermark(primaryColor, accentColor)
                SacredTheme.CALM_SAGE_MINT -> drawTulsiWatermark(primaryColor, accentColor)
                SacredTheme.OCEAN_INDIGO -> drawOceanNarayanaWatermark(primaryColor, accentColor)
                SacredTheme.PEACEFUL_LAVENDER -> drawAparajitaWatermark(primaryColor, accentColor)
                SacredTheme.WHATSAPP_EMERALD -> drawGaneshaDurvaWatermark(primaryColor, accentColor)
                SacredTheme.TELEGRAM_BLUE -> drawCosmicOmWatermark(primaryColor, accentColor)
                SacredTheme.MINIMAL_SLATE -> drawDhyanMandalaWatermark(primaryColor, accentColor)
                SacredTheme.WHATSAPP_DARK -> drawNightMahadevWatermark(primaryColor, accentColor)
            }
        }
    }

    // =========================================================================
    // 1. 🦁 SHERAWALI MAIYA (Trishul, Lotus Mandala, Simha & Temple Jharokha)
    // =========================================================================
    private fun DrawScope.drawSherawaliWatermark(primaryColor: Color, accentColor: Color) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val watermarkAlpha = 0.11f
        val color = primaryColor.copy(alpha = watermarkAlpha)
        val gold = accentColor.copy(alpha = watermarkAlpha * 1.2f)

        // A. 8-Petaled Sacred Shakti Lotus Mandala
        val outerRadius = size.width * 0.38f
        val innerRadius = outerRadius * 0.45f
        drawCircle(color = gold, radius = outerRadius, center = Offset(cx, cy), style = Stroke(width = 2.5f))
        drawCircle(color = color, radius = innerRadius, center = Offset(cx, cy), style = Stroke(width = 1.5f))

        // Radiating 16 Ray Sunburst (सूर्य प्रभा / देवी तेज)
        for (i in 0 until 16) {
            val angle = Math.toRadians((i * 22.5).toDouble())
            val startR = innerRadius * 1.05f
            val endR = outerRadius * 0.95f
            val startX = (cx + startR * cos(angle)).toFloat()
            val startY = (cy + startR * sin(angle)).toFloat()
            val endX = (cx + endR * cos(angle)).toFloat()
            val endY = (cy + endR * sin(angle)).toFloat()
            drawLine(
                color = gold,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = if (i % 2 == 0) 2.5f else 1.2f,
                cap = StrokeCap.Round
            )
        }

        // B. Sacred Trishul (🔱) Silhouette
        val trishulHeight = outerRadius * 1.1f
        val topY = cy - trishulHeight * 0.55f
        val botY = cy + trishulHeight * 0.55f

        // Central Staff (त्रिशूल दंड)
        drawLine(
            color = color,
            start = Offset(cx, topY),
            end = Offset(cx, botY),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )

        // Center Spear Tip (मध्यम शूल)
        val centerSpear = Path().apply {
            moveTo(cx, topY - 28f)
            lineTo(cx - 12f, topY + 18f)
            lineTo(cx, topY + 6f)
            lineTo(cx + 12f, topY + 18f)
            close()
        }
        drawPath(path = centerSpear, color = gold, style = Fill)

        // Left & Right Curved Prongs (वाम व दक्षिण शूल)
        val leftProng = Path().apply {
            moveTo(cx, topY + 45f)
            cubicTo(
                cx - 35f, topY + 45f,
                cx - 45f, topY + 10f,
                cx - 40f, topY - 15f
            )
            lineTo(cx - 40f, topY - 18f)
            cubicTo(
                cx - 30f, topY + 5f,
                cx - 20f, topY + 30f,
                cx, topY + 30f
            )
        }
        drawPath(path = leftProng, color = color, style = Stroke(width = 4.5f, cap = StrokeCap.Round))

        val rightProng = Path().apply {
            moveTo(cx, topY + 45f)
            cubicTo(
                cx + 35f, topY + 45f,
                cx + 45f, topY + 10f,
                cx + 40f, topY - 15f
            )
            lineTo(cx + 40f, topY - 18f)
            cubicTo(
                cx + 30f, topY + 5f,
                cx + 20f, topY + 30f,
                cx, topY + 30f
            )
        }
        drawPath(path = rightProng, color = color, style = Stroke(width = 4.5f, cap = StrokeCap.Round))

        // Sacred Damru Knot at base of spear
        val damruY = topY + 52f
        drawCircle(color = gold, radius = 7f, center = Offset(cx, damruY), style = Fill)
        drawCircle(color = color, radius = 10f, center = Offset(cx, damruY), style = Stroke(width = 2f))

        // C. Two Royal Simha (Lion) Arch Flourishes at sides
        val lionLeftX = cx - outerRadius * 0.85f
        val lionRightX = cx + outerRadius * 0.85f
        val lionY = cy + outerRadius * 0.4f
        drawCircle(color = gold, radius = 16f, center = Offset(lionLeftX, lionY), style = Stroke(width = 2f))
        drawCircle(color = gold, radius = 16f, center = Offset(lionRightX, lionY), style = Stroke(width = 2f))
    }

    // =========================================================================
    // 2. 🪔 DEEPAWALI (Glowing Diyas, Circular Rangoli Mandala & Sparkling Lights)
    // =========================================================================
    private fun DrawScope.drawDeepawaliWatermark(primaryColor: Color, accentColor: Color) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val watermarkAlpha = 0.12f
        val gold = accentColor.copy(alpha = watermarkAlpha * 1.3f)
        val orange = primaryColor.copy(alpha = watermarkAlpha)

        // A. Grand Rangoli Mandala Rings (भव्य रंगोली)
        val maxR = size.width * 0.38f
        drawCircle(color = gold, radius = maxR, center = Offset(cx, cy), style = Stroke(width = 2f))
        drawCircle(color = orange, radius = maxR * 0.8f, center = Offset(cx, cy), style = Stroke(width = 1.5f))
        drawCircle(color = gold, radius = maxR * 0.55f, center = Offset(cx, cy), style = Stroke(width = 2f))
        drawCircle(color = orange, radius = maxR * 0.3f, center = Offset(cx, cy), style = Stroke(width = 1.2f))

        // Rangoli 12-Fold Petals
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30.0).toDouble())
            val petalR = maxR * 0.65f
            val px = (cx + petalR * cos(angle)).toFloat()
            val py = (cy + petalR * sin(angle)).toFloat()
            drawCircle(color = gold, radius = 12f, center = Offset(px, py), style = Stroke(width = 1.8f))
        }

        // B. Large Majestic Consecrated Earthen Diya at Center (प्रज्वलित महा-दीप)
        val diyaWidth = maxR * 0.75f
        val diyaHeight = diyaWidth * 0.38f
        val diyaY = cy + 18f

        // Clay Bowl (दीपक का पात्र)
        val diyaBowl = Path().apply {
            moveTo(cx - diyaWidth / 2f, diyaY)
            cubicTo(
                cx - diyaWidth / 3f, diyaY + diyaHeight,
                cx + diyaWidth / 3f, diyaY + diyaHeight,
                cx + diyaWidth / 2f, diyaY
            )
            cubicTo(
                cx + diyaWidth / 4f, diyaY + 4f,
                cx - diyaWidth / 4f, diyaY + 4f,
                cx - diyaWidth / 2f, diyaY
            )
            close()
        }
        drawPath(path = diyaBowl, color = orange, style = Fill)
        drawPath(path = diyaBowl, color = gold, style = Stroke(width = 3f))

        // Sacred Radiant Flame (दिव्य ज्योति की लौ)
        val flameBaseY = diyaY
        val flameTipY = diyaY - diyaHeight * 2.2f
        val flamePath = Path().apply {
            moveTo(cx, flameBaseY)
            cubicTo(
                cx - diyaWidth * 0.22f, flameBaseY - diyaHeight * 0.8f,
                cx - diyaWidth * 0.12f, flameTipY + diyaHeight * 0.6f,
                cx, flameTipY
            )
            cubicTo(
                cx + diyaWidth * 0.12f, flameTipY + diyaHeight * 0.6f,
                cx + diyaWidth * 0.22f, flameBaseY - diyaHeight * 0.8f,
                cx, flameBaseY
            )
            close()
        }
        drawPath(path = flamePath, color = gold, style = Fill)

        // Inner Golden Core Flame (अखंड ज्योति)
        val innerFlame = Path().apply {
            val innerTipY = flameTipY + diyaHeight * 0.5f
            moveTo(cx, flameBaseY - 2f)
            cubicTo(
                cx - diyaWidth * 0.09f, flameBaseY - diyaHeight * 0.6f,
                cx - diyaWidth * 0.05f, innerTipY + diyaHeight * 0.3f,
                cx, innerTipY
            )
            cubicTo(
                cx + diyaWidth * 0.05f, innerTipY + diyaHeight * 0.3f,
                cx + diyaWidth * 0.09f, flameBaseY - diyaHeight * 0.6f,
                cx, flameBaseY - 2f
            )
            close()
        }
        drawPath(path = innerFlame, color = Color(0xFFFFEB3B).copy(alpha = watermarkAlpha * 1.5f), style = Fill)

        // Ambient Flame Glow Rings
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(gold.copy(alpha = watermarkAlpha * 1.8f), Color.Transparent),
                center = Offset(cx, flameTipY + 20f),
                radius = maxR * 0.5f
            ),
            radius = maxR * 0.5f,
            center = Offset(cx, flameTipY + 20f)
        )

        // C. Auspicious Sparkles (शुभ प्रकाश कण / 4-Point Stars)
        val sparklePoints = listOf(
            Offset(cx - maxR * 0.7f, cy - maxR * 0.6f),
            Offset(cx + maxR * 0.7f, cy - maxR * 0.6f),
            Offset(cx - maxR * 0.8f, cy + maxR * 0.5f),
            Offset(cx + maxR * 0.8f, cy + maxR * 0.5f),
            Offset(cx, cy - maxR * 0.9f)
        )
        for (pt in sparklePoints) {
            drawSparkleStar(pt.x, pt.y, 14f, gold)
        }
    }

    // =========================================================================
    // 3. 🚩 VEER HANUMAN (Vajra Gada, Solar Halo, Sinduri Dhwaja & Temple Arch)
    // =========================================================================
    private fun DrawScope.drawVeerHanumanWatermark(primaryColor: Color, accentColor: Color) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val watermarkAlpha = 0.11f
        val saffron = primaryColor.copy(alpha = watermarkAlpha)
        val gold = accentColor.copy(alpha = watermarkAlpha * 1.2f)

        val radius = size.width * 0.38f

        // A. Solar Halo (सूर्य आभामंडल - बाल समय रबि भक्ष लियो)
        drawCircle(color = gold, radius = radius, center = Offset(cx, cy), style = Stroke(width = 2.5f))
        drawCircle(color = saffron, radius = radius * 0.82f, center = Offset(cx, cy), style = Stroke(width = 1.2f))

        // 24 Solar Rays
        for (i in 0 until 24) {
            val angle = Math.toRadians((i * 15.0).toDouble())
            val startR = radius * 0.84f
            val endR = radius * 0.98f
            drawLine(
                color = saffron,
                start = Offset((cx + startR * cos(angle)).toFloat(), (cy + startR * sin(angle)).toFloat()),
                end = Offset((cx + endR * cos(angle)).toFloat(), (cy + endR * sin(angle)).toFloat()),
                strokeWidth = if (i % 3 == 0) 3f else 1.5f,
                cap = StrokeCap.Round
            )
        }

        // B. Majestic Vajra Gada (महाबली हनुमान जी की वज्र गदा)
        val gadaTotalH = radius * 1.4f
        val gadaTopY = cy - gadaTotalH * 0.5f
        val gadaBotY = cy + gadaTotalH * 0.5f

        // Fluted Dome Head of Gada (गदा का शीर्ष गोलार्ध)
        val headRadius = radius * 0.32f
        val headCenterY = gadaTopY + headRadius * 1.1f
        drawCircle(color = saffron, radius = headRadius, center = Offset(cx, headCenterY), style = Fill)
        drawCircle(color = gold, radius = headRadius, center = Offset(cx, headCenterY), style = Stroke(width = 3.5f))

        // Spiked Flutes on Gada Head
        for (step in -2..2) {
            val offsetSpan = step * (headRadius * 0.28f)
            drawLine(
                color = gold,
                start = Offset(cx + offsetSpan, headCenterY - headRadius * 0.9f),
                end = Offset(cx + offsetSpan, headCenterY + headRadius * 0.9f),
                strokeWidth = 2f
            )
        }

        // Crown Finial of Gada (गदा का कलश शिखर)
        val finialPath = Path().apply {
            moveTo(cx, headCenterY - headRadius - 20f)
            lineTo(cx - 10f, headCenterY - headRadius)
            lineTo(cx + 10f, headCenterY - headRadius)
            close()
        }
        drawPath(path = finialPath, color = gold, style = Fill)

        // Sturdy Ornamental Handle (गदा का दंड)
        val handleStartY = headCenterY + headRadius
        drawLine(
            color = saffron,
            start = Offset(cx, handleStartY),
            end = Offset(cx, gadaBotY),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = gold,
            start = Offset(cx, handleStartY),
            end = Offset(cx, gadaBotY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // Handle Ring Grips & Base Pommel (दंड मुद्रिका व तल चक्र)
        drawCircle(color = gold, radius = 12f, center = Offset(cx, gadaBotY), style = Fill)
        drawCircle(color = saffron, radius = 16f, center = Offset(cx, gadaBotY), style = Stroke(width = 3f))

        // C. Two Sacred Ram-Dhwajas (विजयी सिंदूरी ध्वज)
        val flagY = cy - radius * 0.45f
        drawSacredFlag(cx - radius * 0.65f, flagY, 40f, saffron, gold, isLeft = true)
        drawSacredFlag(cx + radius * 0.65f, flagY, 40f, saffron, gold, isLeft = false)
    }

    // =========================================================================
    // 4. 🇮🇳 TIRANGA (24-Spoke Ashoka Chakra & Sacred Tricolor Ribbons)
    // =========================================================================
    private fun DrawScope.drawTirangaWatermark(primaryColor: Color, accentColor: Color) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val navy = Color(0xFF0038A8).copy(alpha = 0.12f)
        val chakraRadius = size.width * 0.32f

        // Outer Rim & Inner Hub
        drawCircle(color = navy, radius = chakraRadius, center = Offset(cx, cy), style = Stroke(width = 3.5f))
        drawCircle(color = navy, radius = chakraRadius * 0.22f, center = Offset(cx, cy), style = Fill)

        // Exactly 24 Sacred Dharma Spokes (24 धर्म चक्र तीलियां)
        for (i in 0 until 24) {
            val angle = Math.toRadians((i * 15.0).toDouble())
            val startX = (cx + chakraRadius * 0.22f * cos(angle)).toFloat()
            val startY = (cy + chakraRadius * 0.22f * sin(angle)).toFloat()
            val endX = (cx + chakraRadius * 0.98f * cos(angle)).toFloat()
            val endY = (cy + chakraRadius * 0.98f * sin(angle)).toFloat()
            drawLine(
                color = navy,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }

    // =========================================================================
    // 5. 🪵 SANDALWOOD AMBER (Tripundra Tilak & Vishnu Mandala)
    // =========================================================================
    private fun DrawScope.drawSandalwoodWatermark(primaryColor: Color, accentColor: Color) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val amber = primaryColor.copy(alpha = 0.12f)
        val gold = accentColor.copy(alpha = 0.14f)
        val r = size.width * 0.34f

        drawCircle(color = gold, radius = r, center = Offset(cx, cy), style = Stroke(width = 2f))
        drawCircle(color = amber, radius = r * 0.7f, center = Offset(cx, cy), style = Stroke(width = 1.5f))

        // Urdhva Pundra Tilak (वैष्णव चंदन तिलक)
        val tilakPath = Path().apply {
            moveTo(cx - 30f, cy - 60f)
            lineTo(cx - 20f, cy + 30f)
            cubicTo(cx - 15f, cy + 60f, cx + 15f, cy + 60f, cx + 20f, cy + 30f)
            lineTo(cx + 30f, cy - 60f)
            lineTo(cx + 14f, cy - 60f)
            lineTo(cx + 8f, cy + 20f)
            cubicTo(cx + 5f, cy + 30f, cx - 5f, cy + 30f, cx - 8f, cy + 20f)
            lineTo(cx - 14f, cy - 60f)
            close()
        }
        drawPath(path = tilakPath, color = gold, style = Fill)

        // Red Kumkum Center Bindi
        drawCircle(color = Color(0xFFD32F2F).copy(alpha = 0.14f), radius = 7f, center = Offset(cx, cy + 10f), style = Fill)
    }

    // =========================================================================
    // 6. 🌿 SAGE MINT (Holy Tulsi Leaves & Ganga Jal Ripples)
    // =========================================================================
    private fun DrawScope.drawTulsiWatermark(primaryColor: Color, accentColor: Color) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val mint = primaryColor.copy(alpha = 0.12f)
        val r = size.width * 0.35f

        // Concentric Water Ripples (गंगाजल तरंगें)
        for (step in 1..4) {
            drawCircle(color = mint, radius = r * (step / 4f), center = Offset(cx, cy), style = Stroke(width = 1.5f))
        }

        // Holy Tulsi Sprig (पावन तुलसी दल)
        drawTulsiLeaf(cx, cy - 25f, 32f, mint, 0f)
        drawTulsiLeaf(cx - 28f, cy + 5f, 26f, mint, -45f)
        drawTulsiLeaf(cx + 28f, cy + 5f, 26f, mint, 45f)
        drawLine(color = mint, start = Offset(cx, cy - 35f), end = Offset(cx, cy + 45f), strokeWidth = 3f, cap = StrokeCap.Round)
    }

    // =========================================================================
    // 7. 🌊 OCEAN INDIGO (Ksheer Sagar Waves & Sudarshan Chakra)
    // =========================================================================
    private fun DrawScope.drawOceanNarayanaWatermark(primaryColor: Color, accentColor: Color) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val indigo = primaryColor.copy(alpha = 0.12f)
        val gold = accentColor.copy(alpha = 0.12f)
        val r = size.width * 0.35f

        drawCircle(color = indigo, radius = r, center = Offset(cx, cy), style = Stroke(width = 2f))
        drawCircle(color = gold, radius = r * 0.5f, center = Offset(cx, cy), style = Stroke(width = 2f))

        // 8 Sudarshan serrated teeth
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45.0).toDouble())
            val px = (cx + r * cos(angle)).toFloat()
            val py = (cy + r * sin(angle)).toFloat()
            drawCircle(color = gold, radius = 9f, center = Offset(px, py), style = Fill)
        }
    }

    // =========================================================================
    // 8. 🪻 LAVENDER (Aparajita Petals & Cosmic Calm)
    // =========================================================================
    private fun DrawScope.drawAparajitaWatermark(primaryColor: Color, accentColor: Color) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val purple = primaryColor.copy(alpha = 0.12f)
        val r = size.width * 0.34f

        drawCircle(color = purple, radius = r, center = Offset(cx, cy), style = Stroke(width = 2f))
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60.0).toDouble())
            val px = (cx + r * 0.6f * cos(angle)).toFloat()
            val py = (cy + r * 0.6f * sin(angle)).toFloat()
            drawCircle(color = purple, radius = r * 0.35f, center = Offset(px, py), style = Stroke(width = 1.5f))
        }
    }

    // =========================================================================
    // 9. 💬 WHATSAPP EMERALD (Holy Durva & Ganesha Mandala)
    // =========================================================================
    private fun DrawScope.drawGaneshaDurvaWatermark(primaryColor: Color, accentColor: Color) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val emerald = primaryColor.copy(alpha = 0.12f)
        val r = size.width * 0.34f

        drawCircle(color = emerald, radius = r, center = Offset(cx, cy), style = Stroke(width = 2f))
        drawCircle(color = emerald, radius = r * 0.6f, center = Offset(cx, cy), style = Stroke(width = 1.5f))
        // 3 Durva Grass blades
        drawLine(color = emerald, start = Offset(cx, cy + 30f), end = Offset(cx - 24f, cy - 35f), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(color = emerald, start = Offset(cx, cy + 30f), end = Offset(cx, cy - 45f), strokeWidth = 3.5f, cap = StrokeCap.Round)
        drawLine(color = emerald, start = Offset(cx, cy + 30f), end = Offset(cx + 24f, cy - 35f), strokeWidth = 3f, cap = StrokeCap.Round)
    }

    // =========================================================================
    // 10. 🌐 TELEGRAM BLUE (Akash Ganga Cosmos & Brahm Naad)
    // =========================================================================
    private fun DrawScope.drawCosmicOmWatermark(primaryColor: Color, accentColor: Color) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val blue = primaryColor.copy(alpha = 0.12f)
        val r = size.width * 0.36f

        for (step in 1..5) {
            drawCircle(color = blue, radius = r * (step / 5f), center = Offset(cx, cy), style = Stroke(width = 1.2f))
        }
    }

    // =========================================================================
    // 11. 🪨 MINIMAL SLATE (Dhyana Mandala & Zen Serenity)
    // =========================================================================
    private fun DrawScope.drawDhyanMandalaWatermark(primaryColor: Color, accentColor: Color) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val slate = primaryColor.copy(alpha = 0.10f)
        val r = size.width * 0.32f

        drawCircle(color = slate, radius = r, center = Offset(cx, cy), style = Stroke(width = 3f))
        drawCircle(color = slate, radius = r * 0.85f, center = Offset(cx, cy), style = Stroke(width = 1f))
        drawCircle(color = slate, radius = r * 0.12f, center = Offset(cx, cy), style = Fill)
    }

    // =========================================================================
    // 12. 🌙 WHATSAPP DARK (Midnight Temple Sanctum & Eternal Jyoti)
    // =========================================================================
    private fun DrawScope.drawNightMahadevWatermark(primaryColor: Color, accentColor: Color) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val gold = accentColor.copy(alpha = 0.14f)
        val r = size.width * 0.36f

        drawCircle(color = gold, radius = r, center = Offset(cx, cy), style = Stroke(width = 2f))
        // Crescent Moon (अर्धचंद्र)
        val moonPath = Path().apply {
            moveTo(cx, cy - r * 0.5f)
            cubicTo(cx - 35f, cy, cx - 35f, cy + 40f, cx, cy + r * 0.5f)
            cubicTo(cx - 15f, cy + 30f, cx - 15f, cy - 20f, cx, cy - r * 0.5f)
            close()
        }
        drawPath(path = moonPath, color = gold, style = Fill)
    }

    // Helper: 4-Point Sparkle Star
    private fun DrawScope.drawSparkleStar(cx: Float, cy: Float, size: Float, color: Color) {
        val path = Path().apply {
            moveTo(cx, cy - size)
            cubicTo(cx, cy - size * 0.2f, cx - size * 0.2f, cy, cx - size, cy)
            cubicTo(cx - size * 0.2f, cy, cx, cy + size * 0.2f, cx, cy + size)
            cubicTo(cx, cy + size * 0.2f, cx + size * 0.2f, cy, cx + size, cy)
            cubicTo(cx + size * 0.2f, cy, cx, cy - size * 0.2f, cx, cy - size)
            close()
        }
        drawPath(path = path, color = color, style = Fill)
    }

    // Helper: Sacred Flag (ध्वज)
    private fun DrawScope.drawSacredFlag(
        x: Float,
        y: Float,
        size: Float,
        bodyColor: Color,
        goldColor: Color,
        isLeft: Boolean
    ) {
        val poleDir = if (isLeft) -1f else 1f
        drawLine(
            color = goldColor,
            start = Offset(x, y - size),
            end = Offset(x, y + size),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        val flagPath = Path().apply {
            moveTo(x, y - size)
            lineTo(x + poleDir * size * 1.2f, y - size * 0.3f)
            lineTo(x, y + size * 0.1f)
            close()
        }
        drawPath(path = flagPath, color = bodyColor, style = Fill)
        drawPath(path = flagPath, color = goldColor, style = Stroke(width = 2f))
    }

    // Helper: Tulsi Leaf
    private fun DrawScope.drawTulsiLeaf(
        cx: Float,
        cy: Float,
        length: Float,
        color: Color,
        rotationDeg: Float
    ) {
        val path = Path().apply {
            moveTo(cx, cy - length)
            cubicTo(cx - length * 0.45f, cy - length * 0.4f, cx - length * 0.45f, cy + length * 0.4f, cx, cy + length)
            cubicTo(cx + length * 0.45f, cy + length * 0.4f, cx + length * 0.45f, cy - length * 0.4f, cx, cy - length)
            close()
        }
        drawPath(path = path, color = color, style = Fill)
    }
}
