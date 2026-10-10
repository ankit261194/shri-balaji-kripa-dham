package com.example.shribalajikripadham.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.example.shribalajikripadham.data.model.Token
import com.example.shribalajikripadham.ui.token.PremiumRoyalTokenCard
import com.example.shribalajikripadham.hardware.DeviceFingerprintManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.data.sacred.SacredTrack
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import com.example.shribalajikripadham.theme.SacredTheme
import com.example.shribalajikripadham.ui.home.components.*
import com.example.shribalajikripadham.ui.theme.SacredThematicBackground
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
    var myActiveToken by remember { mutableStateOf<Token?>(null) }
    var selectedTab by remember { mutableStateOf(ProHomeTab.DARSHAN) }
    var viewingLyricsTrack by remember { mutableStateOf<SacredTrack?>(null) }
    var showSevadarHelpdesk by remember { mutableStateOf(false) }
    var showThemeChooserDialog by remember { mutableStateOf(false) }
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var updateAvailableInfo by remember { mutableStateOf<AppUpdateManager.OnlineUpdateInfo?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgressPercent by remember { mutableIntStateOf(0) }
    var downloadStatusText by remember { mutableStateOf("") }

    // Proactive In-App Update Prompt on App Launch
    LaunchedEffect(Unit) {
        delay(800L)
        try {
            val currentCode = AppUpdateManager.getCurrentVersionCode(context)
            val onlineInfo = AppUpdateManager.fetchLatestUpdateFromOnline()
            if (onlineInfo != null && onlineInfo.versionCode > currentCode) {
                updateAvailableInfo = onlineInfo
            }
        } catch (_: Exception) {}
    }

    // Live Settings & Adaptive Background Sync
    LaunchedEffect(Unit) {
        try {
            repository.syncLiveConfigFromGitHub()
            settings = repository.getSettings()
        } catch (e: Exception) {
            try {
                settings = repository.getSettings()
            } catch (_: Exception) {}
        }
    }

    // Auto-Adopt Super Admin Festival Broadcast Theme if Enforced Nationwide
    LaunchedEffect(settings.isFestivalThemeEnforced, settings.currentThemeId) {
        if (settings.isFestivalThemeEnforced && settings.currentThemeId.isNotBlank()) {
            val festivalTheme = SacredTheme.fromId(settings.currentThemeId)
            if (currentTheme != festivalTheme) {
                onThemeChanged(festivalTheme)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentTimeMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Devotee Personal Token Auto-Recovery & Active Tracker
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                val devId = com.example.shribalajikripadham.hardware.DeviceFingerprintManager.getDeviceId(context)
                val targetDate = if (settings.darbarDate.isNotBlank()) settings.darbarDate else com.example.shribalajikripadham.data.local.DatabaseHelper.getTodayDateString()
                var tok = repository.checkDeviceRegisteredToday(devId, targetDate)
                if (tok == null) {
                    val savedPhone = try {
                        context.getSharedPreferences("sbkd_devotee_my_token_prefs", Context.MODE_PRIVATE).getString("my_phone_number", "") ?: ""
                    } catch (_: Exception) { "" }
                    tok = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.checkDeviceRegisteredOnServer(devId, targetDate, savedPhone)
                }
                if (tok == null) {
                    tok = com.example.shribalajikripadham.hardware.PersistentTokenReceiptHelper.readPersistentReceipt(devId, targetDate)
                }
                myActiveToken = tok
            } catch (e: Exception) {}
            delay(5_000L)
        }
    }

    // Adaptive Background Telemetry Sync
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                repository.syncLiveConfigFromGitHub()
                settings = repository.getSettings()
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
                            updateAvailableInfo = onlineInfo
                        } else {
                            Toast.makeText(context, if (isHindi) "✅ आपका ऐप नवीनतम वर्जन पर है (Build #$currentCode)!" else "App is up to date (Build #$currentCode)!", Toast.LENGTH_LONG).show()
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
                            "🚩 Om Shri Hanumate Namah 🚩\n\nShri Balaji Kripa धाम (Dungra Jaat, Bulandshahr)\nDownload official app:\n$shareUrl"
                        }
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, msg)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "ऐप शेयर करें"))
                    }
                },
                onOpenManualPdf = {},
                onThemeChanged = onThemeChanged
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
                    onOpenManualPdf = {},
                    onOpenSevadarChat = { showSevadarHelpdesk = true },
                    onToggleLanguage = onToggleLanguage,
                    onOpenAdmin = onNavigateToAdmin,
                    onThemeChanged = onThemeChanged
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
            SacredThematicBackground(
                currentTheme = currentTheme,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                showToran = true
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
                            // 0. Dynamic Devotional Theme Aura Banner (Deity Motifs & Sacred Chants)
                            SacredThemeAuraBanner(
                                isHindi = isHindi,
                                currentTheme = currentTheme,
                                onOpenThemeChooser = { showThemeChooserDialog = true }
                            )

                            // 1. Daily Consecrated Darshan Card (Hero)
                            DailyDarshanHeroCard(
                                isHindi = isHindi,
                                currentTheme = currentTheme
                            )

                            // 1b. Devotee's Active Personal Token Card (Hero Prominence)
                            myActiveToken?.let { tok ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToToken() },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
                                    border = BorderStroke(1.5.dp, Color(0xFFD4AF37)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaroonPrimary,
                                                modifier = Modifier.size(46.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "#${tok.tokenNumber}",
                                                        color = Color(0xFFFFD700),
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "🚩 आपका सक्रिय टोकन: #${tok.tokenNumber}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaroonPrimary
                                                )
                                                Text(
                                                    text = "${tok.patientName} • दिनांक: ${tok.darbarDate}",
                                                    fontSize = 11.sp,
                                                    color = Color.DarkGray
                                                )
                                                Text(
                                                    text = if (settings.runningTokenNumber > 0) "वर्तमान में सेवारत: #${settings.runningTokenNumber}" else "कतार में प्रतीक्षारत",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (settings.runningTokenNumber == tok.tokenNumber) Color(0xFF2E7D32) else Color(0xFFE65100)
                                                )
                                            }
                                        }
                                        Surface(
                                            color = MaroonPrimary,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "रसीद देखें ➔",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

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
                                scheduleState = scheduleState,
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
                                onNavigateToHavan = onNavigateToHavanApplication,
                                onOpenSevadarHelpdesk = { showSevadarHelpdesk = true }
                            )

                            // 6. Official WhatsApp Channel Card (1-Click Devotee Follow)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val channelUrl = if (settings.whatsappChannelUrl.isNotBlank() && !settings.whatsappChannelUrl.contains("chat.whatsapp.com")) {
                                            settings.whatsappChannelUrl
                                        } else {
                                            "https://whatsapp.com/channel/0029VaCZJTmJ3jv2UwiMtY1w"
                                        }
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(channelUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, if (isHindi) "व्हाट्सएप खोलने में असमर्थ" else "Unable to open WhatsApp", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                border = BorderStroke(1.5.dp, Color(0xFF25D366)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF25D366),
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("💬", fontSize = 24.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (isHindi) "आधिकारिक व्हाट्सएप चैनल" else "Official WhatsApp Channel",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.5.sp,
                                                    color = Color(0xFF0F5132)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF25D366).copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "OFFICIAL 🌿",
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF0F5132),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isHindi) "आरती दर्शन, दरबार सूचनाएं व ताजा अपडेट सीधे व्हाट्सएप पर पाएं" else "Get live darshan, aarti & notices on WhatsApp",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF2B5329),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                    Surface(
                                        color = Color(0xFF25D366),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isHindi) "फॉलो करें ➔" else "Follow ➔",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 6.1 Official WhatsApp Group Card (1-Click Devotee Join)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val groupUrl = if (settings.whatsappGroupUrl.isNotBlank() && !settings.whatsappGroupUrl.contains("/channel/")) {
                                            settings.whatsappGroupUrl
                                        } else {
                                            "https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0"
                                        }
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(groupUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, if (isHindi) "व्हाट्सएप खोलने में असमर्थ" else "Unable to open WhatsApp", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF9)),
                                border = BorderStroke(1.5.dp, Color(0xFF128C7E)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF128C7E),
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("👥", fontSize = 24.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (isHindi) "आधिकारिक व्हाट्सएप ग्रुप" else "Official WhatsApp Group",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.5.sp,
                                                    color = Color(0xFF064E3B)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF128C7E).copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "COMMUNITY 🤝",
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF064E3B),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isHindi) "धाम संगत व भक्तों के आधिकारिक ग्रुप में शामिल हों" else "Join official devotee community group",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF134E4A),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                    Surface(
                                        color = Color(0xFF128C7E),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isHindi) "ग्रुप से जुड़ें ➔" else "Join ➔",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }

                        ProHomeTab.TOKEN -> {
                            if (myActiveToken != null) {
                                PremiumRoyalTokenCard(
                                    token = myActiveToken!!,
                                    settings = settings,
                                    isHindi = isHindi,
                                    onBackToHome = { selectedTab = ProHomeTab.DARSHAN }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onNavigateToToken,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.2.dp, MaroonPrimary)
                                ) {
                                    Text(
                                        text = if (isHindi) "➕ अन्य परिजन हेतु नया टोकन बनाएं" else "➕ Issue Another Token for Family",
                                        fontWeight = FontWeight.Bold,
                                        color = MaroonPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
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

    // Sacred Theme Chooser Dialog
    if (showThemeChooserDialog) {
        com.example.shribalajikripadham.ui.theme.SacredThemeChooserDialog(
            currentTheme = currentTheme,
            isHindi = isHindi,
            onThemeSelected = { newTheme ->
                onThemeChanged(newTheme)
                showThemeChooserDialog = false
            },
            onDismissRequest = { showThemeChooserDialog = false }
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
