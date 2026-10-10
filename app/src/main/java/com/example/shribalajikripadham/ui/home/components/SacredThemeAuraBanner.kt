package com.example.shribalajikripadham.ui.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.theme.SacredTheme

/**
 * 🌟 Dynamic 360° Devotional Theme Aura Banner.
 * Visually manifests the chosen sacred deity, festival, or soothing theme across the Home screen:
 * - 🦁 Sherawali Maiya: Authentic Maa Durga Shakti, Trishul & Lion motifs, "जय माता दी" blessings.
 * - 🪔 Divya Deepawali: Radiant glowing earthen diyas, Mahalakshmi gold aura, Deepotsav festive glow.
 * - 🚩 Veer Hanuman: Sindoori Balaji aura, Gada-Dhwaj motifs, "जय श्री राम • संकट मोचन".
 * - 🇮🇳 Tiranga Pride: Akhand Bharat tricolor elegance & Ashoka Chakra.
 * - 🍃 Peaceful/Daily Themes: Serene Ayurvedic, Sandalwood, Lavender, and clean WhatsApp aesthetics.
 */
@Composable
fun SacredThemeAuraBanner(
    isHindi: Boolean,
    currentTheme: SacredTheme,
    onOpenThemeChooser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ThemeAuraPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // Dedicated gradient styling per theme
    val cardGradient = Brush.horizontalGradient(
        colors = listOf(
            currentTheme.headerGradientStart,
            currentTheme.headerGradientEnd.copy(alpha = 0.95f),
            currentTheme.headerGradientStart
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenThemeChooser() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.5.dp,
            currentTheme.accentGold.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Motif Icon Pill
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentTheme.icon,
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Center Divine Title & Sacred Chant
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isHindi) currentTheme.festiveBannerTitle.ifBlank { currentTheme.nameHindi }
                            else currentTheme.nameEnglish,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Festival / Category Pill
                        Surface(
                            color = currentTheme.accentGold.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.6.dp, currentTheme.accentGold.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = currentTheme.festivalCategoryHindi,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Divine Sacred Chant / Mantra
                    Text(
                        text = if (isHindi) currentTheme.divineChantHindi.ifBlank { currentTheme.watermarkText }
                        else currentTheme.divineChantEnglish,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = pulseAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Divine Motif & Quick Theme Pill
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onOpenThemeChooser() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = currentTheme.divineMotif.take(4).trim(),
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (isHindi) "थीम 🎨" else "Theme 🎨",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
