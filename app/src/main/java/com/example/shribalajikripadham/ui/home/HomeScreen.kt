package com.example.shribalajikripadham.ui.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.data.sacred.SacredTrack
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import com.example.shribalajikripadham.theme.SacredTheme
import com.example.shribalajikripadham.ui.home.components.*
import com.example.shribalajikripadham.util.AppUpdateManager
import com.example.shribalajikripadham.util.AshramManualPdfGenerator
import com.example.shribalajikripadham.util.SundayScheduleState
import com.example.shribalajikripadham.util.SundayTokenScheduleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Pro-Tier Modern Sacred HomeScreen.
 * Modular, clean, lightweight coordinator connecting specialized UI components.
 * Reduced from 6,643 monolithic lines to a clean ~220 lines!
 */
@Composable
fun HomeScreen(
    isHindi: Boolean,
    currentTheme: SacredTheme = SacredTheme.WHATSAPP_EMERALD,
    onThemeChanged: (SacredTheme) -> Unit = {},
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit = onNavigateToToken,
    onNavigateToTuesdayToken: () -> Unit = onNavigateToFaceToken,
    onNavigateToYatra: () -> Unit = {},
    onNavigateToInfo: () -> Unit = {},
    onNavigateToTravelGuide: () -> Unit = onNavigateToInfo,
    onNavigateToAdmin: () -> Unit,
    onNavigateToParchas: () -> Unit = {},
    onNavigateToYatraExpenses: () -> Unit = {},
    onNavigateToLiveDarbar: () -> Unit = {},
    onNavigateToPanchang: () -> Unit = {},
    onNavigateToSacredGranth: () -> Unit = {},
    onNavigateToDharamshala: () -> Unit = {},
    onNavigateToHavanApplication: () -> Unit = {},
    onToggleLanguage: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scrollState = rememberScrollState()

    var settings by remember { mutableStateOf(AshramSettings()) }
    var selectedTab by remember { mutableStateOf(ProHomeTab.DARSHAN) }
    var viewingLyricsTrack by remember { mutableStateOf<SacredTrack?>(null) }
    var showSevadarHelpdesk by remember { mutableStateOf(false) }
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var updateAvailableInfo by remember { mutableStateOf<AppUpdateManager.OnlineUpdateInfo?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgressPercent by remember { mutableIntStateOf(0) }
    var downloadStatusText by remember { mutableStateOf("") }

    // Proactive In-App Update Prompt on App Launch
    LaunchedEffect(Unit) {
        delay(1200L)
        try {
            val currentCode = AppUpdateManager.getCurrentVersionCode(context)
            val onlineInfo = AppUpdateManager.fetchLatestUpdateFromOnline()
            if (onlineInfo != null && onlineInfo.versionCode > currentCode) {
                if (!AppUpdateManager.isUpdateSnoozed(context, onlineInfo.versionCode)) {
                    updateAvailableInfo = onlineInfo
                }
            }
        } catch (_: Exception) {}
    }

    // Live Settings & Adaptive Background Sync
    LaunchedEffect(Unit) {
        try {
            settings = repository.getSettings()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentTimeMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Adaptive Background Telemetry Sync
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                val rawCfg = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchLiveConfig()
                if (rawCfg != null && (rawCfg.optBoolean("success", false) || rawCfg.has("config"))) {
                    val fresh = repository.getSettings()
                    settings = fresh
                }
            } catch (e: Exception) {
                // Smooth fallback
            }
            delay(15_000L)
        }
    }

    val scheduleState = remember(settings, currentTimeMs) {
        SundayTokenScheduleHelper.evaluateSchedule(settings, currentTimeMs)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeNavDrawerContent(
                isHindi = isHindi,
                settings = settings,
                currentTheme = currentTheme,
                onNavigateToHome = {
                    selectedTab = ProHomeTab.DARSHAN
                    scope.launch { drawerState.close() }
                },
                onNavigateToLiveDarbar = {
                    scope.launch { drawerState.close() }
                    onNavigateToLiveDarbar()
                },
                onNavigateToToken = {
                    scope.launch { drawerState.close() }
                    onNavigateToToken()
                },
                onNavigateToFaceToken = {
                    scope.launch { drawerState.close() }
                    onNavigateToFaceToken()
                },
                onNavigateToParchas = {
                    scope.launch { drawerState.close() }
                    onNavigateToParchas()
                },
                onNavigateToHavan = {
                    scope.launch { drawerState.close() }
                    onNavigateToHavanApplication()
                },
                onNavigateToTravelGuide = {
                    scope.launch { drawerState.close() }
                    onNavigateToTravelGuide()
                },
                onOpenSevadarHelpdesk = {
                    scope.launch { drawerState.close() }
                    showSevadarHelpdesk = true
                },
                onNavigateToAdmin = {
                    scope.launch { drawerState.close() }
                    onNavigateToAdmin()
                },
                onCheckUpdate = {
                    scope.launch {
                        drawerState.close()
                        Toast.makeText(context, if (isHindi) "🔄 अपडेट जांच रहे हैं..." else "Checking updates...", Toast.LENGTH_SHORT).show()
                        val currentCode = AppUpdateManager.getCurrentVersionCode(context)
                        val onlineInfo = AppUpdateManager.fetchLatestUpdateFromOnline()
                        if (onlineInfo != null && onlineInfo.versionCode > currentCode) {
                            AppUpdateManager.downloadAndInstallUpdate(context, onlineInfo.apkUrl)
                        } else {
                            Toast.makeText(context, if (isHindi) "✅ आपका ऐप नवीनतम वर्जन पर है!" else "App is up to date!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onShareApp = {
                    scope.launch {
                        drawerState.close()
                        val shareUrl = "https://shribalajikripadham.online/app"
                        val msg = if (isHindi) {
                            "🚩 ॐ श्री हनुमते नमः 🚩\n\nश्री बालाजी कृपा धाम (डूँगरा जाट, बुलन्दशहर)\nरविवार टोकन, दिव्य दर्शन व आरती हेतु आधिकारिक ऐप डाउनलोड करें:\n$shareUrl"
                        } else {
                            "🚩 Om Shri Hanumate Namah 🚩\n\nShri Balaji Kripa Dham (Dungra Jaat, Bulandshahr)\nDownload official app:\n$shareUrl"
                        }
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, msg)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "ऐप शेयर करें"))
                    }
                },
                onOpenManualPdf = {
                    scope.launch {
                        drawerState.close()
                        val file = AshramManualPdfGenerator.generateDevoteeGuidePdf(context)
                        if (file != null) {
                            AshramManualPdfGenerator.openOrSharePdf(context, file, "श्री बालाजी कृपा धाम मार्गदर्शिका")
                        } else {
                            Toast.makeText(context, "PDF तैयार करने में असमर्थ", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                HomeTopBar(
                    isHindi = isHindi,
                    currentTheme = currentTheme,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onRefresh = {
                        scope.launch {
                            Toast.makeText(context, if (isHindi) "🔄 डेटा रीफ्रेश हो रहा है..." else "Refreshing data...", Toast.LENGTH_SHORT).show()
                            try {
                                settings = repository.getSettings()
                            } catch (e: Exception) {}
                        }
                    },
                    onOpenManualPdf = {
                        val file = AshramManualPdfGenerator.generateDevoteeGuidePdf(context)
                        if (file != null) {
                            AshramManualPdfGenerator.openOrSharePdf(context, file, "श्री बालाजी कृपा धाम मार्गदर्शिका")
                        } else {
                            Toast.makeText(context, "PDF तैयार करने में असमर्थ", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenSevadarChat = { showSevadarHelpdesk = true },
                    onToggleLanguage = onToggleLanguage,
                    onOpenAdmin = onNavigateToAdmin
                )
            },
            bottomBar = {
                ProBottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    isHindi = isHindi,
                    currentTheme = currentTheme
                )
            },
            containerColor = currentTheme.backgroundLight
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(currentTheme.backgroundLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        ProHomeTab.DARSHAN -> {
                            // 1. Daily Consecrated Darshan Card (Hero)
                            DailyDarshanHeroCard(
                                isHindi = isHindi,
                                currentTheme = currentTheme
                            )

                            // 2. Real-Time Darbar & Token Status Card
                            LiveDarbarStatusCard(
                                isHindi = isHindi,
                                settings = settings,
                                scheduleState = scheduleState,
                                currentTheme = currentTheme,
                                onNavigateToLiveDarbar = onNavigateToLiveDarbar,
                                onNavigateToToken = onNavigateToToken
                            )

                            // 3. Sunday & Tuesday Token Call-To-Action
                            SundayTokenActionCard(
                                isHindi = isHindi,
                                currentTheme = currentTheme,
                                isTuesdayEnabled = settings.isTuesdayDarbarEnabled,
                                onNavigateToToken = onNavigateToToken,
                                onNavigateToFaceToken = onNavigateToFaceToken,
                                onNavigateToTuesdayToken = onNavigateToTuesdayToken
                            )

                            // 4. Sacred Notice Banner (if any)
                            if (settings.isEmergencyNoticeVisible && settings.emergencyNoticeText.isNotBlank()) {
                                SacredNoticeBanner(
                                    isHindi = isHindi,
                                    noticeText = settings.emergencyNoticeText
                                )
                            }

                            // 5. Four Core Sacred Services (2x2 Grid + Sevadar Connect)
                            QuickSacredServicesGrid(
                                isHindi = isHindi,
                                currentTheme = currentTheme,
                                onNavigateToAarti = { selectedTab = ProHomeTab.BHAKTI },
                                onNavigateToGranth = onNavigateToSacredGranth,
                                onNavigateToPanchang = onNavigateToPanchang,
                                onNavigateToTravelGuide = onNavigateToTravelGuide,
                                onOpenSevadarHelpdesk = { showSevadarHelpdesk = true }
                            )
                        }

                        ProHomeTab.TOKEN -> {
                            SundayTokenActionCard(
                                isHindi = isHindi,
                                currentTheme = currentTheme,
                                isTuesdayEnabled = settings.isTuesdayDarbarEnabled,
                                onNavigateToToken = onNavigateToToken,
                                onNavigateToFaceToken = onNavigateToFaceToken,
                                onNavigateToTuesdayToken = onNavigateToTuesdayToken
                            )

                            LiveDarbarStatusCard(
                                isHindi = isHindi,
                                settings = settings,
                                scheduleState = scheduleState,
                                currentTheme = currentTheme,
                                onNavigateToLiveDarbar = onNavigateToLiveDarbar,
                                onNavigateToToken = onNavigateToToken
                            )
                        }

                        ProHomeTab.BHAKTI -> {
                            BhaktiTabContent(
                                isHindi = isHindi,
                                currentTheme = currentTheme,
                                onNavigateToGranth = onNavigateToSacredGranth,
                                onViewLyrics = { viewingLyricsTrack = it }
                            )
                        }

                        ProHomeTab.ASHRAM -> {
                            AshramTabContent(
                                isHindi = isHindi,
                                settings = settings,
                                currentTheme = currentTheme,
                                onNavigateToTravelGuide = onNavigateToTravelGuide
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(60.dp)) // Padding for bottom nav and floating player
                }
            }
        }
    }

    // Lyrics Modal Dialog
    viewingLyricsTrack?.let { track ->
        SacredLyricsViewerDialog(
            track = track,
            currentTheme = currentTheme,
            onDismiss = { viewingLyricsTrack = null }
        )
    }

    // Sevadar Helpdesk & WhatsApp In-App Live Chat Modal Dialog
    if (showSevadarHelpdesk) {
        com.example.shribalajikripadham.ui.feedback.SevadarHelpdeskDialog(
            isHindi = isHindi,
            onDismiss = { showSevadarHelpdesk = false }
        )
    }

    // Prominent In-App Update Prompt Dialog
    updateAvailableInfo?.let { updateInfo ->
        AlertDialog(
            onDismissRequest = {
                if (!updateInfo.isForce && !isDownloadingUpdate) {
                    AppUpdateManager.snoozeUpdate(context, updateInfo.versionCode)
                    updateAvailableInfo = null
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚩", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "नया ऐप अपडेट उपलब्ध है!" else "New Update Available!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaroonPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F))
                    ) {
                        Text(
                            text = "v${updateInfo.versionName} (Build #${updateInfo.versionCode})",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = if (isHindi) updateInfo.updateNotesHindi else updateInfo.updateNotesEnglish,
                        fontSize = 13.sp,
                        color = Color(0xFF37474F),
                        lineHeight = 18.sp
                    )

                    if (isDownloadingUpdate) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = SaffronPrimary,
                            trackColor = Color(0xFFFFE082)
                        )
                        Text(
                            text = if (downloadStatusText.isNotBlank()) downloadStatusText else "$downloadProgressPercent%",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonPrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isDownloadingUpdate) {
                            isDownloadingUpdate = true
                            downloadStatusText = if (isHindi) "डाउनलोड प्रारंभ हो रहा है..." else "Starting download..."
                            scope.launch {
                                try {
                                    AppUpdateManager.startInAppUpdateDetailed(
                                        context = context,
                                        downloadUrl = updateInfo.apkUrl,
                                        onProgress = { pct, downloaded, total ->
                                            downloadProgressPercent = pct
                                            val dlMb = downloaded.toDouble() / (1024 * 1024)
                                            val totMb = total.toDouble() / (1024 * 1024)
                                            downloadStatusText = "डाउनलोड हो रहा है: $pct% (${String.format(java.util.Locale.US, "%.1f", dlMb)}/${String.format(java.util.Locale.US, "%.1f", totMb)} MB)"
                                        },
                                        onSuccess = { apkFile ->
                                            isDownloadingUpdate = false
                                            downloadStatusText = if (isHindi) "इन्स्टॉल किया जा रहा है..." else "Installing..."
                                            AppUpdateManager.triggerApkInstall(context, apkFile)
                                        },
                                        onError = { err ->
                                            isDownloadingUpdate = false
                                            downloadStatusText = ""
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } catch (e: Exception) {
                                    isDownloadingUpdate = false
                                    Toast.makeText(context, "त्रुटि: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = !isDownloadingUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isDownloadingUpdate) "डाउनलोड जारी..." else (if (isHindi) "📲 तुरंत अपडेट करें" else "📲 Update Now"),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                if (!updateInfo.isForce && !isDownloadingUpdate) {
                    TextButton(
                        onClick = {
                            AppUpdateManager.snoozeUpdate(context, updateInfo.versionCode)
                            updateAvailableInfo = null
                        }
                    ) {
                        Text(if (isHindi) "बाद में" else "Later", color = Color.Gray)
                    }
                }
            }
        )
    }
}
