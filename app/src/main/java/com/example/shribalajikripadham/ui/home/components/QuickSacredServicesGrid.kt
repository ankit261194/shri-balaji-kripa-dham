package com.example.shribalajikripadham.ui.home.components

import androidx.compose.foundation.BorderStroke
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
import com.example.shribalajikripadham.theme.SacredTheme

data class SacredServiceItem(
    val id: String,
    val icon: String,
    val titleHindi: String,
    val titleEnglish: String,
    val subtitleHindi: String,
    val subtitleEnglish: String,
    val onClick: () -> Unit
)

/**
 * Pro-Tier 2x2 Quick Sacred Services Grid.
 * Replaces cluttered 12-item buttons with 4 polished, genuine sacred services.
 */
@Composable
fun QuickSacredServicesGrid(
    isHindi: Boolean,
    currentTheme: SacredTheme,
    onNavigateToAarti: () -> Unit,
    onNavigateToGranth: () -> Unit,
    onNavigateToPanchang: () -> Unit,
    onNavigateToTravelGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val services = listOf(
        SacredServiceItem(
            id = "aarti",
            icon = "📿",
            titleHindi = "नित्य आरती व भजन",
            titleEnglish = "Aarti & Bhajans",
            subtitleHindi = "15 पावन पाठ व ऑडियो",
            subtitleEnglish = "15 Devotional Tracks",
            onClick = onNavigateToAarti
        ),
        SacredServiceItem(
            id = "granth",
            icon = "📖",
            titleHindi = "पावन ग्रन्थ व चालीसा",
            titleEnglish = "Sacred Granth",
            subtitleHindi = "हनुमान चालीसा, बजरंग बाण",
            subtitleEnglish = "Chalisa & Stotras",
            onClick = onNavigateToGranth
        ),
        SacredServiceItem(
            id = "panchang",
            icon = "📅",
            titleHindi = "दैनिक पंचांग",
            titleEnglish = "Daily Panchang",
            subtitleHindi = "तिथि, वार व शुभ मुहूर्त",
            subtitleEnglish = "Tithi & Choghadiya",
            onClick = onNavigateToPanchang
        ),
        SacredServiceItem(
            id = "guide",
            icon = "🗺️",
            titleHindi = "आश्रम मार्ग गाइड",
            titleEnglish = "Travel Guide",
            subtitleHindi = "डूँगरा जाट कैसे पहुँचें",
            subtitleEnglish = "Route & Directions",
            onClick = onNavigateToTravelGuide
        )
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (isHindi) "धाम पावन सेवाएं" else "Ashram Sacred Services",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = currentTheme.primaryColor,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // 2x2 Grid using 2 Rows
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (rowIndex in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (colIndex in 0..1) {
                        val item = services[rowIndex * 2 + colIndex]
                        SacredServiceTile(
                            item = item,
                            isHindi = isHindi,
                            currentTheme = currentTheme,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SacredServiceTile(
    item: SacredServiceItem,
    isHindi: Boolean,
    currentTheme: SacredTheme,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceLight),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, currentTheme.cardBorderColor.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Surface(
                color = currentTheme.primaryColor.copy(alpha = 0.08f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = item.icon, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isHindi) item.titleHindi else item.titleEnglish,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (isHindi) item.subtitleHindi else item.subtitleEnglish,
                fontSize = 10.5.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }
    }
}
