package com.example.shribalajikripadham.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.R
import com.example.shribalajikripadham.theme.SacredTheme

/**
 * Pro-Tier Elegant Sacred Top Bar.
 * Clean, lightweight, professional header with minimal visual noise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    isHindi: Boolean,
    currentTheme: SacredTheme,
    onOpenDrawer: () -> Unit,
    onRefresh: () -> Unit,
    onOpenManualPdf: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 6.dp)
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Text(
                        text = "☰",
                        fontSize = 22.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, currentTheme.secondaryColor.copy(alpha = 0.8f), CircleShape)
                        .clickable { onOpenDrawer() }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Ashram Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        title = {
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = if (isHindi) "श्री बालाजी कृपा धाम" else "Shri Balaji Kripa Dham",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = if (isHindi) "डूँगरा जाट, बुलन्दशहर (उ.प्र.)" else "Dungra Jaat, Bulandshahr (U.P.)",
                    fontSize = 10.5.sp,
                    color = Color.White.copy(alpha = 0.82f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        },
        actions = {
            // Fast Refresh
            IconButton(onClick = onRefresh) {
                Text("🔄", fontSize = 17.sp)
            }

            // PDF Guide
            IconButton(onClick = onOpenManualPdf) {
                Text("📖", fontSize = 17.sp)
            }

            // Discreet Sevadar Admin Key
            IconButton(onClick = onOpenAdmin) {
                Text("🔐", fontSize = 16.sp)
            }

            // Language Toggle
            Button(
                onClick = onToggleLanguage,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.20f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = if (isHindi) "English" else "हिंदी",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = currentTheme.topBarColor
        )
    )
}
