package com.example.shribalajikripadham.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.theme.SacredTheme
import com.example.shribalajikripadham.util.SundayScheduleState
import com.example.shribalajikripadham.util.SundayTokenScheduleHelper

/**
 * Pro-Tier Sunday & Tuesday Token Call-to-Action Card.
 * Prominent, unmissable, clean registration gateway for devotees.
 */
@Composable
fun SundayTokenActionCard(
    isHindi: Boolean,
    currentTheme: SacredTheme,
    isTuesdayEnabled: Boolean,
    scheduleState: SundayScheduleState = SundayScheduleState.Open,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToTuesdayToken: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = currentTheme.primaryColor
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            currentTheme.headerGradientStart,
                            currentTheme.headerGradientEnd
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isHindi) "श्री बालाजी दिव्य दरबार टोकन" else "Shri Balaji Darbar Token",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isHindi) "रविवार दरबार (डूँगरा जाट)" else "Sunday Darbar (Dungra Jaat)",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isHindi) "निशुल्क सेवा" else "Free Seva",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Real-Time Countdown Box if within 12-hour Sunday Window
            if (scheduleState is SundayScheduleState.CountdownActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, Color(0xFFFFD54F).copy(alpha = 0.85f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "⏳ ",
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isHindi) "टोकन उल्टी गिनती प्रारंभ (12 घंटे पूर्व)" else "Token Registration Countdown",
                                color = Color(0xFFFFE082),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Large Digital Ticking Clock
                        val remainingClock = SundayTokenScheduleHelper.formatCountdown(scheduleState.remainingMillis)
                        Surface(
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = remainingClock,
                                color = Color(0xFFFFD54F),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isHindi) {
                                "${scheduleState.formattedTarget} टोकन स्वतः खुल जाएंगे"
                            } else {
                                "Opens automatically at ${scheduleState.formattedTarget}"
                            },
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (scheduleState is SundayScheduleState.Open) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = "🟢 ", fontSize = 12.sp)
                    Text(
                        text = if (isHindi) "टोकन पंजीकरण सक्रिय है • अर्जी स्वीकार हो रही है" else "Token Registration is LIVE Now",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Main Booking Button
                Button(
                    onClick = onNavigateToToken,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = currentTheme.primaryColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (scheduleState is SundayScheduleState.CountdownActive) {
                            if (isHindi) "⏳ टोकन उल्टी गिनती" else "⏳ Countdown Active"
                        } else if (scheduleState is SundayScheduleState.Open) {
                            if (isHindi) "🎟️ तुरंत टोकन लें (LIVE)" else "🎟️ Get Token (LIVE)"
                        } else {
                            if (isHindi) "🎟️ टोकन प्राप्त करें" else "🎟️ Get Token"
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor
                    )
                }

                // Face Token Button
                OutlinedButton(
                    onClick = onNavigateToFaceToken,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isHindi) "🤳 फेस टोकन" else "🤳 Face Token",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Tuesday Darbar option if enabled
            if (isTuesdayEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onNavigateToTuesdayToken,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isHindi) "🚩 मंगलवार दरबार टोकन (बुलन्दशहर) ➔" else "🚩 Tuesday Darbar Token (Bulandshahr) ➔",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }
        }
    }
}
