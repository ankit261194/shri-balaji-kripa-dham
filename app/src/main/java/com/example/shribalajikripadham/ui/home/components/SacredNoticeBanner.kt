package com.example.shribalajikripadham.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pro-Tier Sacred Notice & Emergency Announcement Banner.
 * Clean, respectful notification banner with high legibility.
 */
@Composable
fun SacredNoticeBanner(
    isHindi: Boolean,
    noticeText: String,
    modifier: Modifier = Modifier
) {
    if (noticeText.isBlank()) return

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📢",
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 10.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHindi) "आश्रम पावन सूचना" else "Ashram Notice",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFFB78103)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = noticeText,
                    fontSize = 12.sp,
                    color = Color(0xFF424242),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
