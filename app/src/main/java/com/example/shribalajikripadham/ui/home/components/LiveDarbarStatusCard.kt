package com.example.shribalajikripadham.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.theme.SacredTheme
import com.example.shribalajikripadham.ui.theme.SacredTempleCard
import com.example.shribalajikripadham.util.SundayScheduleState

/**
 * Pro-Tier Live Darbar & Real-Time Token Queue Status Card.
 * Clean, glanceable live indicator for devotees.
 */
@Composable
fun LiveDarbarStatusCard(
    isHindi: Boolean,
    settings: AshramSettings,
    scheduleState: SundayScheduleState,
    currentTheme: SacredTheme,
    onNavigateToLiveDarbar: () -> Unit,
    onNavigateToToken: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Strictly LIVE ONLY when tokens are actually being distributed / registration is OPEN
    val isDarbarLive = scheduleState is SundayScheduleState.Open
    val isCountdown = scheduleState is SundayScheduleState.CountdownActive

    SacredTempleCard(
        currentTheme = currentTheme,
        containerColor = if (isDarbarLive) Color(0xFFFFF3E0) else if (isCountdown) Color(0xFFFFFDE7) else currentTheme.surfaceLight,
        borderGoldColor = if (isDarbarLive) Color(0xFFFF9800) else if (isCountdown) Color(0xFFFFB300) else currentTheme.accentGold,
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isDarbarLive) onNavigateToLiveDarbar() else onNavigateToToken()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status Icon / Pulse
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDarbarLive) Color(0xFFE65100)
                            else if (isCountdown) Color(0xFFFF8F00).copy(alpha = 0.18f)
                            else currentTheme.primaryColor.copy(alpha = 0.10f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDarbarLive) "🔴" else if (isCountdown) "⏳" else "🎟️",
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isDarbarLive) {
                                if (isHindi) "दरबार लाइव चल रहा है" else "Darbar is LIVE Now"
                            } else if (isCountdown) {
                                if (isHindi) "⏳ टोकन उल्टी गिनती जारी" else "⏳ Token Countdown Active"
                            } else {
                                if (isHindi) "टोकन पंजीकरण अभी बंद है" else "Token Registration Closed"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDarbarLive) Color(0xFFBF360C) else if (isCountdown) Color(0xFFE65100) else currentTheme.primaryColor
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Pulse or Status Pill
                        Surface(
                            color = if (isDarbarLive) Color(0xFFFFCCBC) else if (isCountdown) Color(0xFFFFE082) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isDarbarLive) "LIVE" else if (isCountdown) "COUNTDOWN" else "CLOSED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarbarLive) Color(0xFFBF360C) else if (isCountdown) Color(0xFFE65100) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = if (isDarbarLive) {
                            val cur = settings.runningTokenNumber
                            if (cur > 0) {
                                if (isHindi) "वर्तमान सेवा टोकन: #$cur • दर्शन जारी" else "Serving Token: #$cur • In Progress"
                            } else {
                                if (isHindi) "टोकन पंजीकरण खुला है • अर्जी प्रारंभ" else "Token Registration is OPEN"
                            }
                        } else {
                            when (scheduleState) {
                                is SundayScheduleState.CountdownActive -> {
                                    val clock = com.example.shribalajikripadham.util.SundayTokenScheduleHelper.formatCountdown(scheduleState.remainingMillis)
                                    if (isHindi) "⏰ शेष समय: $clock • ${scheduleState.formattedTarget} स्वतः खुलेंगे"
                                    else "⏰ Time left: $clock • Opens at ${scheduleState.formattedTarget}"
                                }
                                is SundayScheduleState.SundayBeforeStart -> if (isHindi) "आज रविवार प्रातः 8:30 बजे से टोकन खुलेंगे" else "Tokens open today at 8:30 AM"
                                is SundayScheduleState.SundayClosedEvening -> if (isHindi) "आज का टोकन समय समाप्त • डूँगरा जाट" else "Today's Darbar session closed"
                                is SundayScheduleState.NonSunday -> if (isHindi) "डूँगरा जाट • प्रत्येक रविवार प्रातः 8:30 से" else "Dungra Jaat • Every Sunday from 8:30 AM"
                                else -> if (isHindi) "डूँगरा जाट • प्रत्येक रविवार प्रातः 8:30 से" else "Dungra Jaat • Every Sunday from 8:30 AM"
                            }
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                }
            }

            // Arrow CTA
            Surface(
                color = currentTheme.primaryColor.copy(alpha = 0.08f),
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "➔",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor
                    )
                }
            }
        }
    }
}
