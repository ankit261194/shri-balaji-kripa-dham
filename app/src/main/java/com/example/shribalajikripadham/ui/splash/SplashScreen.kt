package com.example.shribalajikripadham.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.R
import com.example.shribalajikripadham.theme.*

@Composable
fun SplashScreen(
    onEnterDarbar: () -> Unit,
    isHindi: Boolean,
    onToggleLanguage: () -> Unit
) {
    // Breathing Divine Aura Animation for Balaji Maharaj Portrait
    val infiniteTransition = rememberInfiniteTransition(label = "DivineAuraTransition")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraScale"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF28000C), // Imperial Midnight Maroon
                        Color(0xFF420015),
                        Color(0xFF6B0524), // Royal Burgundy
                        Color(0xFF991400), // Sacred Sindoor
                        Color(0xFFD84315), // Deep Saffron
                        Color(0xFFE65100)
                    )
                )
            )
    ) {
        // Ambient soft golden top radial glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD54F).copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Elegant Language Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { onToggleLanguage() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isHindi) "🌐 English" else "🌐 हिंदी", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CENTER HERO: Animated Divine Aura & Sacred Portrait
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Pulsing Halo
                    Box(
                        modifier = Modifier
                            .size(195.dp)
                            .scale(auraScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        GoldLight.copy(alpha = auraAlpha),
                                        GoldSecondary.copy(alpha = auraAlpha * 0.6f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Secondary Golden Ring
                    Box(
                        modifier = Modifier
                            .size(175.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, GoldLight.copy(alpha = 0.9f), CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF800028),
                                        Color(0xFF4A0017)
                                    )
                                )
                            )
                    )

                    // Actual Divine Balaji Image
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .border(3.5.dp, Color.White, CircleShape)
                            .shadow(12.dp, CircleShape)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Shri Balaji Maharaj Divine Portrait",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Sacred Invocation Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, GoldSecondary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "🚩  ॥ श्री हनुमते नमः ॥  🚩",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldLight,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grand Main Title
                Text(
                    text = if (isHindi) "श्री बालाजी कृपा धाम" else "Shri Balaji Kripa Dham",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle / Divine Location Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = if (isHindi) "📍 डूँगरा जाट, बुलन्दशहर (उ.प्र.)" else "📍 Dungra Jaat, Bulandshahr (U.P.)",
                        fontSize = 13.sp,
                        color = Color(0xFFFFF8E1),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // GURUJI PROFILE & FREE TREATMENT TRUST CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                border = BorderStroke(1.5.dp, GoldSecondary.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("👑", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "परम पूज्य गुरुजी तेजवीर सिंह जी" else "Param Pujya Guruji Tejveer Singh Ji",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaroonAccent,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isHindi)
                            "॥ संकट कटे मिटे सब पीरा, जो सुमिरै हनुमत बलबीरा ॥"
                        else
                            "|| Sankat Kate Mite Sab Peera, Jo Sumirai Hanumat Balbeera ||",
                        fontSize = 12.sp,
                        color = SaffronDark,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = Color(0xFFEEEEEE),
                        thickness = 1.dp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🕊️", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "100% निःशुल्क आध्यात्मिक सेवा" else "100% Free Spiritual Healing",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = if (isHindi)
                                    "भूत-प्रेत व मानसिक कष्टों का निःशुल्क निवारण। कोई पैसा नहीं लिया जाता।"
                                else
                                    "Free treatment for spiritual afflictions & mental distress. Zero charges.",
                                fontSize = 11.sp,
                                color = TextSecondaryDark,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3 FEATURE PILLS ROW (Modern Floating Badges)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Feature 1
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "फेस टोकन" else "Face Token",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "< 1 Sec",
                            fontSize = 9.sp,
                            color = GoldLight,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Feature 2
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔢", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "रविवार कतार" else "Sunday Queue",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isHindi) "लाइव पास" else "Live Pass",
                            fontSize = 9.sp,
                            color = GoldLight,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Feature 3
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🪔", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "दिव्य आरती" else "Divine Aarti",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isHindi) "प्रातः 7 बजे" else "7:00 AM",
                            fontSize = 9.sp,
                            color = GoldLight,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // GRAND ROYAL CTA BUTTON (GOLDEN GRADIENT PILL) - 100% Reliable Surface Click
            Surface(
                onClick = onEnterDarbar,
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFE082), // Radiant Gold
                                    Color(0xFFFFB300), // Pure Gold
                                    Color(0xFFFF8F00)  // Deep Saffron Gold
                                )
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isHindi) "श्री दरबार में प्रवेश करें" else "Enter Holy Darbar",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4A0017)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "➔",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4A0017)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary instant skip button so user is never trapped on welcome screen
            Surface(
                onClick = onEnterDarbar,
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.12f),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isHindi) "⚡ सीधे मुख्य पृष्ठ पर जाएं (Skip)" else "⚡ Skip directly to Darbar",
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // DEVELOPER / CREATOR SIGNATURE
            Text(
                text = "Developer: Ankit Chaudhary • High-Performance Native Android",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
