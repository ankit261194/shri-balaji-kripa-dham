package com.example.shribalajikripadham.ui.home.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.R
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.theme.SacredTheme
import com.example.shribalajikripadham.util.AppUpdateManager

data class DrawerMenuItem(
    val icon: String,
    val titleHindi: String,
    val titleEnglish: String,
    val action: () -> Unit
)

/**
 * Pro-Tier Modern Sacred Navigation Drawer Content.
 * Clean, frictionless navigation links with zero visual clutter.
 */
@Composable
fun HomeNavDrawerContent(
    isHindi: Boolean,
    settings: AshramSettings,
    currentTheme: SacredTheme,
    onNavigateToHome: () -> Unit,
    onNavigateToLiveDarbar: () -> Unit,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToParchas: () -> Unit,
    onNavigateToHavan: () -> Unit,
    onNavigateToTravelGuide: () -> Unit,
    onOpenSevadarHelpdesk: () -> Unit = {},
    onNavigateToAdmin: () -> Unit,
    onCheckUpdate: () -> Unit,
    onShareApp: () -> Unit,
    onOpenManualPdf: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        drawerContainerColor = currentTheme.surfaceLight,
        modifier = Modifier.width(310.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(currentTheme.primaryColor)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Ashram Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = if (isHindi) "श्री बालाजी कृपा धाम" else "Shri Balaji Kripa Dham",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isHindi) "डूँगरा जाट, बुलन्दशहर" else "Dungra Jaat, Bulandshahr",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Menu Items List
            val items = listOf(
                DrawerMenuItem("🏠", "मुख्य पृष्ठ (दर्शन व टोकन)", "Home", onNavigateToHome),
                DrawerMenuItem("🔴", "लाइव दर्शन व आरती/भजन", "Live Darbar", onNavigateToLiveDarbar),
                DrawerMenuItem("🎟️", "रविवार दरबार टोकन", "Sunday Token", onNavigateToToken),
                DrawerMenuItem("🤳", "फेस वेरिफिकेशन टोकन", "Face Token", onNavigateToFaceToken),
                DrawerMenuItem("📜", "डिजिटल पावन पर्चा", "Digital Parchas", onNavigateToParchas),
                DrawerMenuItem("👥", "सेवादार संपर्क व लाइव चैट", "Sevadar Helpdesk", onOpenSevadarHelpdesk),
                DrawerMenuItem("🔥", "हवन कराने हेतु आवेदन", "Sacred Havan", onNavigateToHavan),

                DrawerMenuItem("🔄", "ऐप अपडेट जांचें (Live)", "Check Updates", onCheckUpdate),
                DrawerMenuItem("📲", "ऐप शेयर करें (भक्तों को भेजें)", "Share App", onShareApp),
                DrawerMenuItem("🔐", "प्रबंधक / सेवादार लॉगिन", "Sevadar Portal", onNavigateToAdmin)
            )

            items.forEach { item ->
                NavigationDrawerItem(
                    icon = { Text(item.icon, fontSize = 18.sp) },
                    label = {
                        Text(
                            text = if (isHindi) item.titleHindi else item.titleEnglish,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1F2937)
                        )
                    },
                    selected = false,
                    onClick = item.action,
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))

            // Version Info
            Text(
                text = "v${AppUpdateManager.getCurrentVersionName(context)} • श्री बालाजी कृपा धाम",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
