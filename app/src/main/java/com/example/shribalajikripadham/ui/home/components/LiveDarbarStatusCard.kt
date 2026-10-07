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
    val isDarbarLive = settings.isDarbarActive || settings.isDarbarLiveNow

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDarbarLive) Color(0xFFFFF3E0) else currentTheme.surfaceLight
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.2.dp,
            if (isDarbarLive) Color(0xFFFF9800) else currentTheme.cardBorderColor.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                            else currentTheme.primaryColor.copy(alpha = 0.10f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDarbarLive) "🔴" else "🎟️",
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isDarbarLive) {
                                if (isHindi) "दरबार लाइव चल रहा है" else "Darbar is LIVE Now"
                            } else {
                                if (isHindi) "रविवार दरबार स्थिति" else "Sunday Darbar Status"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDarbarLive) Color(0xFFBF360C) else currentTheme.primaryColor
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Pulse or Status Pill
                        Surface(
                            color = if (isDarbarLive) Color(0xFFFFCCBC) else Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isDarbarLive) "LIVE" else "ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarbarLive) Color(0xFFBF360C) else Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = if (isDarbarLive) {
                            val cur = settings.runningTokenNumber
                            if (cur > 0) {
                                if (isHindi) "वर्तमान सेवा टोकन: #$cur" else "Current Serving Token: #$cur"
                            } else {
                                if (isHindi) "भक्त दर्शन व अर्जी प्रारंभ" else "Darshan & Arzi in Progress"
                            }
                        } else {
                            when (scheduleState) {
                                is SundayScheduleState.Open -> if (isHindi) "टोकन पंजीकरण खुला है" else "Token Registration is OPEN"
                                is SundayScheduleState.CountdownActive -> if (isHindi) "टोकन शुरू होने में समय बाकी" else "Registration opening shortly"
                                else -> if (isHindi) "डूँगरा जाट • प्रत्येक रविवार" else "Dungra Jaat • Every Sunday"
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
