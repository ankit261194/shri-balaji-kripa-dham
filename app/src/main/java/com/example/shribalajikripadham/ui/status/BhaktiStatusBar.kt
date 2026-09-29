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
                    Text("🚩", fontSize = 16.sp)
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
                        text = if (isHindi) "+ स्टेटस बनाएं" else "+ Create Story",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal Story Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item 0: Add My Story / Frame
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
                                .background(currentTheme.primaryColor.copy(alpha = 0.12f))
                                .border(2.dp, currentTheme.primaryColor, CircleShape)
                        ) {
                            Text("➕", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "अपना स्टेटस" else "My Status",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primaryColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Official Ashram Daily Status (if available in list)
                val officialStatus = statuses.firstOrNull { it.isOfficial }
                if (officialStatus != null) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(68.dp)
                                .clickable { onOpenStatus(officialStatus) }
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
                                                currentTheme.primaryColor,
                                                Color(0xFFFFD54F),
                                                Color(0xFFFFB300)
                                            )
                                        )
                                    )
                                    .padding(2.5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            ) {
                                Text("🚩", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHindi) "दैनिक धाम" else "Daily Ashram",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.primaryColor,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Devotee Statuses (excluding the official one which is already shown first)
                val devoteeStatuses = statuses.filter { !it.isOfficial }
                items(devoteeStatuses) { status ->
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
                            Text("🙏", fontSize = 22.sp)
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
