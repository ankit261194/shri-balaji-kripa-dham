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
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.util.AppUpdateManager
import com.example.shribalajikripadham.util.AshramManualPdfGenerator
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
    var settings by remember { mutableStateOf<com.example.shribalajikripadham.data.model.AshramSettings?>(null) }

    LaunchedEffect(Unit) {
        settings = repository.getSettings()
    }

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
            // 📖 BHAKT APP MARGDARSHIKA (USER MANUAL PDF) BANNER
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, Color(0xFFFFB300)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFECB3)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📖", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "भक्त संपूर्ण ऐप मार्गदर्शिका (PDF)" else "Devotee User Manual (PDF)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaroonPrimary
                        )
                        Text(
                            text = if (isHindi) "ऐप के सभी फीचर्स, टोकन नियम व विधियां - संपूर्ण विवरण PDF में पढ़ें व डाउनलोड करें" else "Read complete step-by-step user guide in official PDF",
                            fontSize = 10.5.sp,
                            color = TextSecondaryDark,
                            lineHeight = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val file = AshramManualPdfGenerator.generateDevoteeGuidePdf(context)
                            if (file != null) {
                                AshramManualPdfGenerator.openOrSharePdf(
                                    context,
                                    file,
                                    if (isHindi) "श्री बालाजी कृपा धाम - भक्त संपूर्ण मार्गदर्शिका" else "Shri Balaji Kripa Dham - Devotee User Manual"
                                )
                            } else {
                                Toast.makeText(context, if (isHindi) "PDF तैयार करने में असमर्थ" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isHindi) "PDF देखें" else "Open PDF",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // 1. FREE SERVICE MANIFESTO & PARICHAY
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "॥ निःशुल्क सेवा संकल्प एवं पावन परिचय ॥" else "|| 100% Free Service & Ashram Intro ||",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) {
                            settings?.ashramParichayHindi?.ifEmpty {
                                "श्री बालाजी कृपा धाम (ग्राम डुंगरा जाट, बुलंदशहर) में पूज्य गुरुजी तेजवीर सिंह जी के मार्गदर्शन में भूत-प्रेत, ऊपरी हवा एवं मानसिक समस्याओं से पीड़ित लोगों का इलाज पूर्णतः निःशुल्क (FREE) किया जाता है। यहाँ किसी भी प्रकार का कोई चढ़ावा या शुल्क अनिवार्य नहीं है।"
                            } ?: "श्री बालाजी कृपा धाम (ग्राम डुंगरा जाट, बुलंदशहर) में पूज्य गुरुजी तेजवीर सिंह जी के मार्गदर्शन में भूत-प्रेत, ऊपरी हवा एवं मानसिक समस्याओं से पीड़ित लोगों का इलाज पूर्णतः निःशुल्क (FREE) किया जाता है।"
                        } else {
                            settings?.ashramParichayEnglish?.ifEmpty {
                                "At Shri Balaji Kripa Dham (Gram Dungra Jaat, Bulandshahr), under Guruji Tejveer Singh Ji, healing for spiritual and mental ailments is 100% FREE. No fee or donation is ever demanded."
                            } ?: "At Shri Balaji Kripa Dham (Gram Dungra Jaat, Bulandshahr), under Guruji Tejveer Singh Ji, healing for spiritual and mental ailments is 100% FREE. No fee or donation is ever demanded."
                        },
                        fontSize = 13.sp,
                        color = TextPrimaryDark,
                        lineHeight = 18.sp
                    )

                    settings?.ashramHistoryHindi?.takeIf { it.isNotBlank() }?.let { history ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = history,
                            fontSize = 12.sp,
                            color = Color(0xFF5D4037),
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                        text = if (isHindi) "दरबार समय व नियम" else "Darbar Schedule & Rules",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "⏰ ${settings?.darbarTimings?.ifEmpty { "प्रत्येक रविवार प्रातः 7:00 बजे से" } ?: "प्रत्येक रविवार प्रातः 7:00 बजे से"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val customRules = settings?.ashramRulesHindi?.takeIf { it.isNotBlank() }
                    if (customRules != null) {
                        val ruleLines = customRules.lines().filter { it.isNotBlank() }
                        ruleLines.forEachIndexed { idx, line ->
                            RuleRow(
                                num = (idx + 1).toString(),
                                text = line.replace(Regex("^\\d+[.)\\s]+"), "")
                            )
                        }
                    } else {
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
                            val uri = Uri.parse("geo:28.3972915,78.1460410?q=28.3972915,78.1460410(Shri+Balaji+Kripa+Dham)")
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
                                updateStatusMsg = if (isHindi) "🔄 नवीनतम अपडेट जांच रहे हैं..." else "🔄 Checking online for updates..."
                                val currentCode = AppUpdateManager.getCurrentVersionCode(context)
                                val onlineInfo = AppUpdateManager.fetchLatestUpdateFromOnline()
                                if (onlineInfo != null && onlineInfo.versionCode > currentCode) {
                                    repository.updateAppUpdateConfig(
                                        latestVersionCode = onlineInfo.versionCode,
                                        latestVersionName = onlineInfo.versionName,
                                        updateNotes = if (isHindi) onlineInfo.updateNotesHindi else onlineInfo.updateNotesEnglish,
                                        apkDownloadUrl = onlineInfo.apkUrl,
                                        isForceUpdate = onlineInfo.isForce
                                    )
                                    updateStatusMsg = if (isHindi) "नया संस्करण v${onlineInfo.versionName} उपलब्ध है! डाउनलोड शुरू हो रहा है..." else "New version v${onlineInfo.versionName} available! Starting download..."
                                    AppUpdateManager.downloadAndInstallUpdate(context, onlineInfo.apkUrl)
                                } else {
                                    val s = repository.getSettings()
                                    if (AppUpdateManager.isUpdateAvailable(currentCode, s.latestVersionCode)) {
                                        updateStatusMsg = if (isHindi) "नया संस्करण v${s.latestVersionName} उपलब्ध है! डाउनलोड शुरू हो रहा है..." else "New version v${s.latestVersionName} available! Starting download..."
                                        AppUpdateManager.downloadAndInstallUpdate(context, s.apkDownloadUrl)
                                    } else {
                                        updateStatusMsg = if (isHindi) "✅ आपका ऐप पहले से ही नवीनतम संस्करण पर है (v${AppUpdateManager.getCurrentVersionName(context)})" else "✅ App is already up to date (v${AppUpdateManager.getCurrentVersionName(context)})"
                                    }
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
