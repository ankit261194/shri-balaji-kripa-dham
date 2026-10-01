package com.example.shribalajikripadham.ui.status

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.R
import com.example.shribalajikripadham.data.model.BhaktiStatusItem
import com.example.shribalajikripadham.theme.LocalSacredStyle

@Composable
fun BhaktiStatusBar(
    statuses: List<BhaktiStatusItem>,
    currentDeviceId: String,
    isHindi: Boolean,
    onOpenCreateStatus: () -> Unit,
    onOpenStatus: (BhaktiStatusItem) -> Unit,
    onOpenAshramStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalSacredStyle.current.theme
    val myStatus = statuses.find { it.deviceId == currentDeviceId }
    val otherStatuses = statuses.filter { it.deviceId != currentDeviceId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Clean Header (WhatsApp Style: 'Status / स्टेटस' heading with 'नया स्टेटस' button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪔", fontSize = 15.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isHindi) "भक्ति स्टेटस" else "Bhakti Status",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentTheme.primaryColor
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = currentTheme.primaryColor.copy(alpha = 0.08f),
                border = BorderStroke(0.6.dp, currentTheme.primaryColor.copy(alpha = 0.25f)),
                modifier = Modifier.clickable { onOpenCreateStatus() }
            ) {
                Text(
                    text = if (isHindi) "➕ स्टेटस लगाएं" else "➕ Add Status",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentTheme.primaryColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // WhatsApp-style Clean Stories Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. DEDICATED PHONE STATUS ("मेरा स्टेटस" / My Status)
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clickable {
                            if (myStatus != null) onOpenStatus(myStatus) else onOpenCreateStatus()
                        }
                ) {
                    Box(
                        modifier = Modifier.size(62.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Story circle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(
                                    if (myStatus != null) {
                                        Brush.sweepGradient(
                                            listOf(
                                                Color(0xFF25D366),
                                                Color(0xFFFFB300),
                                                Color(0xFF25D366)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(
                                                currentTheme.primaryColor.copy(alpha = 0.12f),
                                                currentTheme.primaryColor.copy(alpha = 0.05f)
                                            )
                                        )
                                    }
                                )
                                .padding(if (myStatus != null) 2.5.dp else 0.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Text(
                                text = if (myStatus != null) "🚩" else "👤",
                                fontSize = 24.sp
                            )
                        }

                        // WhatsApp-style (+) badge at bottom-right of circle
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi) "मेरा स्टेटस" else "My Status",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (myStatus != null) (if (isHindi) "देखा गया" else "Active") else (if (isHindi) "टैप करें" else "Tap to add"),
                        fontSize = 9.5.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            // 2. DEDICATED ASHRAM OFFICIAL STORY ("आश्रम दर्शन" / Daily Sacred Darshan)
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clickable { onOpenAshramStatus() }
                ) {
                    Box(
                        modifier = Modifier.size(62.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(58.dp)
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
                            Image(
                                painter = painterResource(id = R.drawable.img_balaji_darshan),
                                contentDescription = "Ashram Darshan",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi) "आश्रम दर्शन" else "Ashram Story",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isHindi) "दैनिक कृपा" else "Daily Darshan",
                        fontSize = 9.5.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            // 3. OTHER DEVOTEES' POSTED STORIES
            items(otherStatuses) { status ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clickable { onOpenStatus(status) }
                ) {
                    Box(
                        modifier = Modifier.size(62.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFF25D366),
                                            Color(0xFF00B0FF),
                                            Color(0xFFFF9800),
                                            Color(0xFF25D366)
                                        )
                                    )
                                )
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Text(
                                text = if (status.isOfficial) "🚩" else "🙏",
                                fontSize = 24.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = status.userName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF263238),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (status.city.isNotBlank()) status.city else (if (isHindi) "भक्त" else "Devotee"),
                        fontSize = 9.5.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
