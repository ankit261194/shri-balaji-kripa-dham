package com.example.shribalajikripadham.ui.status

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.BhaktiStatusItem
import com.example.shribalajikripadham.theme.LocalSacredStyle

@Composable
fun BhaktiStatusBar(
    statuses: List<BhaktiStatusItem>,
    isHindi: Boolean,
    onOpenCreateStatus: () -> Unit,
    onOpenStatus: (BhaktiStatusItem) -> Unit,
    onOpenPoster: (posterType: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalSacredStyle.current.theme

    Card(
        shape = currentTheme.cardShape,
        colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceLight),
        border = BorderStroke(1.dp, currentTheme.cardBorderColor),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🪔", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "भक्ति स्टेटस व WhatsApp स्टोरीज़" else "Bhakti Stories & WhatsApp Status",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = currentTheme.primaryColor.copy(alpha = 0.1f),
                    border = BorderStroke(0.8.dp, currentTheme.primaryColor.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { onOpenCreateStatus() }
                ) {
                    Text(
                        text = if (isHindi) "➕ स्टेटस बनाएं" else "➕ Create Story",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // WhatsApp / Instagram-style Visual Story Posters Reel
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Add My Custom Status Bubble
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { onOpenCreateStatus() }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(currentTheme.primaryColor.copy(alpha = 0.10f))
                                .border(2.dp, currentTheme.primaryColor, CircleShape)
                        ) {
                            Text("➕", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "मेरा स्टेटस" else "My Status",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primaryColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 2. Daily Consecrated Darshan Poster Bubble
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { onOpenPoster("DARSHAN") }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFFF9800),
                                            currentTheme.primaryColor,
                                            Color(0xFFFFD54F),
                                            Color(0xFFFF9800)
                                        )
                                    )
                                )
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Text("🪔", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "दैनिक दर्शन" else "Daily Darshan",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primaryColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 3. Today's Guru Vichar Poster Bubble
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { onOpenPoster("SUVICHAR") }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFFFB300),
                                            Color(0xFF6A1B9A),
                                            Color(0xFFFFD54F),
                                            Color(0xFFFFB300)
                                        )
                                    )
                                )
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Text("🌅", fontSize = 23.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "आज का विचार" else "Guru Vichar",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primaryColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 4. Hanuman Chalisa Chaupai Poster Bubble
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { onOpenPoster("CHALISA") }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFD32F2F),
                                            Color(0xFFFF7043),
                                            Color(0xFFFFD54F),
                                            Color(0xFFD32F2F)
                                        )
                                    )
                                )
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Text("🚩", fontSize = 23.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "हनुमान चालीसा" else "Chalisa",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 5. Bajrang Baan Protection Shield Poster Bubble
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { onOpenPoster("BAJRANG_BAAN") }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFF1A237E),
                                            Color(0xFF0288D1),
                                            Color(0xFFFFD54F),
                                            Color(0xFF1A237E)
                                        )
                                    )
                                )
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Text("🛡️", fontSize = 23.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "बजरंग बाण" else "Bajrang Baan",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A237E),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Devotee Statuses (Real submitted stories)
                items(statuses) { status ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { onOpenStatus(status) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                currentTheme.primaryColor,
                                                currentTheme.secondaryColor
                                            )
                                        )
                                    ),
                                    CircleShape
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(currentTheme.primaryColor.copy(alpha = 0.15f))
                        ) {
                            Text(if (status.isOfficial) "🚩" else "🙏", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = status.userName,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF263238),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
