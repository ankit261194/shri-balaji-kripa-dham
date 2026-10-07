package com.example.shribalajikripadham.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.R
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SacredTheme

/**
 * Pro-Tier Elegant Sacred Top Bar.
 * Spacious, radiant, un-clipped header with prominent golden shining app name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    isHindi: Boolean,
    currentTheme: SacredTheme,
    onOpenDrawer: () -> Unit,
    onRefresh: () -> Unit,
    onOpenManualPdf: () -> Unit,
    onOpenSevadarChat: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    TopAppBar(
        navigationIcon = {
            // Sleek combined menu & emblem button (compact 52dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onOpenDrawer() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "☰",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, AmberGold, CircleShape)
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
            // Dedicated expansive title box with radiant gold shine
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 2.dp)
            ) {
                Text(
                    text = if (isHindi) "श्री बालाजी कृपा धाम" else "Shri Balaji Kripa Dham",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFFDE7),
                                Color(0xFFFFEE58),
                                Color(0xFFFFD54F),
                                Color(0xFFFFA000),
                                Color(0xFFFFD54F),
                                Color(0xFFFFFDE7)
                            )
                        ),
                        fontSize = 18.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp,
                        shadow = Shadow(
                            color = Color(0x99000000),
                            offset = Offset(1.5f, 2.5f),
                            blurRadius = 4f
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isHindi) "🚩 ग्राम डूँगरा जाट, बुलन्दशहर (उ.प्र.)" else "🚩 Dungra Jaat, Bulandshahr (U.P.)",
                    fontSize = 11.2.sp,
                    color = Color(0xFFFFE082),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            // 1. Language Toggle Pill (Compact 50dp)
            Surface(
                onClick = onToggleLanguage,
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.22f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.5f)),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = if (isHindi) "ENG" else "हिंदी",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }

            // 2. Sevadar Live Chat (💬)
            IconButton(
                onClick = onOpenSevadarChat,
                modifier = Modifier.size(36.dp)
            ) {
                Text("💬", fontSize = 18.sp)
            }

            // 3. More Actions Dropdown Menu (⋮)
            Box {
                IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("⋮", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔄", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isHindi) "डेटा रीफ्रेश करें" else "Refresh Data", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        onClick = {
                            showMoreMenu = false
                            onRefresh()
                        }
                    )



                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔐", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isHindi) "सेवादार / व्यवस्थापक प्रवेश" else "Admin / Sevadar Portal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                            }
                        },
                        onClick = {
                            showMoreMenu = false
                            onOpenAdmin()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = currentTheme.topBarColor
        )
    )
}
