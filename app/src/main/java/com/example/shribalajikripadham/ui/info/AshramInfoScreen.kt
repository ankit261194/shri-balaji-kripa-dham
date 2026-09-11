package com.example.shribalajikripadham.ui.info

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.util.AppUpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AshramInfoScreen(
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    val scope = rememberCoroutineScope()
    var updateStatusMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "आश्रम परिचय व नियम" else "Ashram Info & Rules",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "⬅", color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaroonAccent)
            )
        },
        containerColor = SacredBackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. FREE SERVICE MANIFESTO
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "॥ निःशुल्क सेवा संकल्प ॥" else "|| 100% Free Service Pledge ||",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi)
                            "श्री बालाजी कृपा धाम (ग्राम डुंगरा जाट, बुलंदशहर) में पूज्य गुरुजी तेजवीर सिंह जी के मार्गदर्शन में भूत-प्रेत, ऊपरी हवा एवं मानसिक समस्याओं से पीड़ित लोगों का इलाज पूर्णतः निःशुल्क (FREE) किया जाता है। यहाँ किसी भी प्रकार का कोई चढ़ावा या शुल्क अनिवार्य नहीं है।"
                        else
                            "At Shri Balaji Kripa Dham (Gram Dungra Jaat, Bulandshahr), under Guruji Tejveer Singh Ji, healing for spiritual and mental ailments is 100% FREE. No fee or donation is ever demanded.",
                        fontSize = 13.sp,
                        color = TextPrimaryDark,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. DARBAR TIMINGS & RULES
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "रविवार दरबार समय व नियम" else "Sunday Darbar Schedule & Rules",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonAccent
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    RuleRow(
                        num = "1",
                        text = if (isHindi) "प्रत्येक रविवार प्रातःकाल से डुंगरा जाट आश्रम पर दिव्य दरबार प्रारंभ होता है।" else "Every Sunday morning, Darbar commences at Dungra Jaat Ashram."
                    )
                    RuleRow(
                        num = "2",
                        text = if (isHindi) "टोकन केवल आश्रम परिसर (200m परिधि) में भौतिक रूप से उपस्थित होने पर ही मिलेगा।" else "Tokens are issued only upon physical arrival inside Ashram premises."
                    )
                    RuleRow(
                        num = "3",
                        text = if (isHindi) "एक मोबाइल डिवाइस से केवल 1 मरीज का टोकन लग सकता है।" else "Strictly 1 token per mobile device on each Sunday."
                    )
                    RuleRow(
                        num = "4",
                        text = if (isHindi) "मरीजों को केवल भगवान की पूजा-पाठ, पाठ-जाप एवं शुद्ध सात्विक नियम बताए जाते हैं।" else "Devotees are guided only with prayers, holy chanting, and spiritual disciplines."
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. ADDRESS & GOOGLE MAPS NAVIGATION
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "आश्रम का पता एवं मार्ग" else "Ashram Address & Directions",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi)
                            "श्री बालाजी कृपा धाम\nग्राम डुंगरा जाट, जिला बुलंदशहर, उत्तर प्रदेश"
                        else
                            "Shri Balaji Kripa Dham\nGram Dungra Jaat, District Bulandshahr, Uttar Pradesh",
                        fontSize = 14.sp,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val uri = Uri.parse("geo:28.4089,77.8789?q=Gram+Dungra+Jaat+Bulandshahr")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isHindi) "🗺️ गूगल मैप्स पर दिशा देखें" else "🗺️ Open in Google Maps",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. APP DEVELOPMENT & DEVELOPER CREDIT (PRD Requirement)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "तकनीकी विकास एवं क्रेडिट" else "App Architecture & Credits",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Developer: Ankit Chaudhary",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaffronDark
                    )
                    Text(
                        text = "Agency / Architecture: Anti Gravity",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi)
                            "श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) के संपूर्ण डिजिटल प्रबंधन, सुपरफास्ट फेस टोकन सिस्टम, बस सेवा एवं एंटी-फ्रॉड सुरक्षा हेतु आधिकारिक रूप से विकसित।"
                        else
                            "Officially engineered for Shri Balaji Kripa Dham digital Darbar operations, smart face tokens, bus management, and fraud protection.",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        lineHeight = 16.sp
                    )
                }
            }

            // App Update Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "📲 ऐप संस्करण एवं ऑटो-अपडेट" else "📲 App Version & Auto-Update",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "स्थापित संस्करण (Installed): v${AppUpdateManager.getCurrentVersionName(context)} (Build #${AppUpdateManager.getCurrentVersionCode(context)})",
                        fontSize = 13.sp,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val s = repository.getSettings()
                                val currentCode = AppUpdateManager.getCurrentVersionCode(context)
                                if (AppUpdateManager.isUpdateAvailable(currentCode, s.latestVersionCode)) {
                                    updateStatusMsg = if (isHindi) "नया संस्करण उपलब्ध है! डाउनलोड शुरू हो रहा है..." else "New version available! Starting download..."
                                    AppUpdateManager.downloadAndInstallUpdate(context, s.apkDownloadUrl)
                                } else {
                                    updateStatusMsg = if (isHindi) "आपका ऐप पहले से ही नवीनतम संस्करण पर है (v${s.latestVersionName})" else "App is already up to date (v${s.latestVersionName})"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isHindi) "🔄 अपडेट जांचें (Check for Updates)" else "🔄 Check for Updates", fontWeight = FontWeight.Bold)
                    }
                    if (updateStatusMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(updateStatusMsg!!, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun RuleRow(num: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(SaffronPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(text = num, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, fontSize = 13.sp, color = TextPrimaryDark, lineHeight = 17.sp)
    }
}
