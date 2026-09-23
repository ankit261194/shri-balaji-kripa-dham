package com.example.shribalajikripadham.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.R
import com.example.shribalajikripadham.data.model.UiSectionConfig
import com.example.shribalajikripadham.data.model.Admin
import com.example.shribalajikripadham.data.model.AshramDataDefaults
import com.example.shribalajikripadham.data.model.AshramEvent
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.SevadarProfile
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.ui.common.SacredAvatar
import com.example.shribalajikripadham.util.AppUpdateManager
import com.example.shribalajikripadham.util.NotificationHelper
import com.example.shribalajikripadham.util.SundayTokenScheduleHelper
import com.example.shribalajikripadham.util.SundayScheduleState
import com.example.shribalajikripadham.util.DistanceCalculatorService
import android.widget.Toast
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import com.example.shribalajikripadham.data.sacred.SACRED_TRACKS
import com.example.shribalajikripadham.data.sacred.SacredTrack
import com.example.shribalajikripadham.service.BhajanAudioService
import com.example.shribalajikripadham.util.DevotionalAudioCacheManager
import android.content.ClipData
import android.content.ClipboardManager

enum class HomeTab(val titleHindi: String, val titleEnglish: String, val icon: String) {
    DARSHAN_TOKEN("दर्शन व टोकन", "Darshan & Token", "🚩"),
    BHAKTI_AARTI("नित्य भक्ति", "Bhakti & Aarti", "📿"),
    DHARAMSHALA_YATRA("आवास व यात्रा", "Stay & Yatra", "🏨"),
    ASHRAM_ABOUT("आश्रम परिचय", "About Ashram", "📜")
}

fun openSocialMediaLink(
    context: Context,
    rawUrl: String?,
    defaultUrl: String,
    isWhatsApp: Boolean = false,
    errorMessage: String = "लिंक या ऐप खोलने में असमर्थ"
) {
    val trimmed = (rawUrl ?: "").trim()
    val finalUrl = when {
        trimmed.isEmpty() -> defaultUrl
        isWhatsApp -> {
            val cleanDigits = trimmed.replace("+", "").replace("-", "").replace(" ", "").replace("(", "").replace(")", "")
            if (cleanDigits.all { it.isDigit() } && cleanDigits.length in 10..15) {
                val fullNumber = if (cleanDigits.length == 10) "91$cleanDigits" else cleanDigits
                "https://wa.me/$fullNumber"
            } else if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
        }
        else -> {
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
        }
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            if (finalUrl != defaultUrl && defaultUrl.isNotBlank()) {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(defaultUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                return
            }
        } catch (ignored: Exception) {}
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}

fun shareAppContent(context: Context, shareMessage: String, title: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "शेयर करने में असमर्थ", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isHindi: Boolean,
    currentTheme: SacredTheme = SacredTheme.ROYAL_MAROON,
    onThemeChanged: (SacredTheme) -> Unit = {},
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit = onNavigateToToken,
    onNavigateToYatra: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToParchas: () -> Unit = {},
    onNavigateToYatraExpenses: () -> Unit = {},
    onNavigateToLiveDarbar: () -> Unit = {},
    onNavigateToPanchang: () -> Unit = {},
    onNavigateToSacredGranth: () -> Unit = {},
    onNavigateToDharamshala: () -> Unit = {},
    onToggleLanguage: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val screenWidthDp = configuration.screenWidthDp
    val isCompact = screenHeightDp < 740
    val isUltraCompact = screenHeightDp < 660
    val responsivePadding = if (isCompact) (if (isUltraCompact) 8.dp else 10.dp) else 16.dp
    val sectionSpacing = if (isCompact) (if (isUltraCompact) 6.dp else 8.dp) else 14.dp
    val repository = remember { AshramRepository(context) }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(AshramSettings()) }
    var dynamicEvents by remember { mutableStateOf<List<AshramEvent>>(emptyList()) }
    var activeSevadars by remember { mutableStateOf<List<Admin>>(emptyList()) }
    var sevadarProfiles by remember { mutableStateOf<List<SevadarProfile>>(emptyList()) }
    var activeLayout by remember { mutableStateOf(AppUiLayout.CLASSIC_DARBAR) }
    var uiSectionConfigs by remember { mutableStateOf<List<UiSectionConfig>>(UiSectionConfig.defaultSections()) }

    var showUpdatePopup by remember { mutableStateOf(false) }
    var showUpToDateDialog by remember { mutableStateOf(false) }
    var onlineCheckedVerCode by remember { mutableIntStateOf(0) }
    var onlineCheckedVerName by remember { mutableStateOf("") }
    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadDownloadedBytes by remember { mutableLongStateOf(0L) }
    var downloadTotalBytes by remember { mutableLongStateOf(0L) }
    var downloadedApkFile by remember { mutableStateOf<java.io.File?>(null) }
    var downloadErrorMsg by remember { mutableStateOf<String?>(null) }

    var selectedHomeTab by remember { mutableStateOf(HomeTab.DARSHAN_TOKEN) }
    val homeScrollState = rememberScrollState()
    var viewingLyricsTrack by remember { mutableStateOf<SacredTrack?>(null) }

    LaunchedEffect(Unit) {
        var s = settings
        try {
            s = repository.getSettings()
            settings = s
            activeLayout = AppUiLayout.fromId(s.activeUiLayout)
            val customDists = repository.getAllCustomCityDistances()
            DistanceCalculatorService.loadCustomDistances(customDists.map { Pair(it.cityName, it.distanceKm) })
            val evs = repository.getAllEvents()
            if (evs.isNotEmpty()) {
                dynamicEvents = evs
            }
            activeSevadars = repository.getAllActiveSevadars()
            sevadarProfiles = repository.getAllSevadars()
            val sections = repository.getUiSectionConfigs()
            if (sections.isNotEmpty()) uiSectionConfigs = sections
        } catch (e: Exception) {
            android.util.Log.e("HomeScreenInit", "Safe fallback on initial data load", e)
        }

        // 🔄 Real-time Background Sync from GitHub Live Config (Instant, non-blocking)
        scope.launch {
            try {
                val (synced, liveConfig) = repository.syncLiveConfigFromGitHub()
                try { repository.syncAdminsFromGitHub() } catch (e: Exception) {}
                try { repository.syncLiveParchasFromGitHub() } catch (e: Exception) {}
                try {
                    repository.syncSevadarsFromCloud()
                    activeSevadars = repository.getAllActiveSevadars()
                    sevadarProfiles = repository.getAllSevadars()
                } catch (e: Exception) {}
                if (synced && liveConfig != null) {
                    if (liveConfig.sections.isNotEmpty()) {
                        uiSectionConfigs = liveConfig.sections
                    }
                    val freshSettings = repository.getSettings()
                    settings = freshSettings
                    if (freshSettings.isUiLayoutEnforced) {
                        activeLayout = AppUiLayout.fromId(freshSettings.activeUiLayout)
                    }
                    val evs = repository.getAllEvents()
                    if (evs.isNotEmpty()) dynamicEvents = evs
                    activeSevadars = repository.getAllActiveSevadars()
                    sevadarProfiles = repository.getAllSevadars()
                }
            } catch (e: Exception) {
                // Smooth fallback to local SQLite cache
            }
        }

        // 🔄 Continuous live sync loop (every 20 seconds) while screen is open
        // Immediate zero-cache online update check on startup
        scope.launch {
            try {
                val currentCode = AppUpdateManager.getCurrentVersionCode(context)
                val onlineInfo = AppUpdateManager.fetchLatestUpdateFromOnline()
                if (onlineInfo != null) {
                    if (onlineInfo.webhookUrl.isNotBlank()) {
                        com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager.saveWebhookUrl(context, onlineInfo.webhookUrl)
                    }
                    if (onlineInfo.versionCode > currentCode) {
                        repository.updateAppUpdateConfig(
                            latestVersionCode = onlineInfo.versionCode,
                            latestVersionName = onlineInfo.versionName,
                            updateNotes = if (isHindi) onlineInfo.updateNotesHindi else onlineInfo.updateNotesEnglish,
                            apkDownloadUrl = onlineInfo.apkUrl,
                            isForceUpdate = onlineInfo.isForce
                        )
                        settings = repository.getSettings()
                        val updatePrefs = context.getSharedPreferences("sbkd_update_snooze", android.content.Context.MODE_PRIVATE)
                        val snoozedCode = updatePrefs.getInt("snoozed_version_code", 0)
                        val snoozeUntil = updatePrefs.getLong("snooze_until_timestamp", 0L)
                        val isSnoozed = (snoozedCode >= onlineInfo.versionCode && System.currentTimeMillis() < snoozeUntil)
                        if (!isSnoozed || onlineInfo.isForce) {
                            showUpdatePopup = true
                        }
                    }
                } else {
                    val fresh = repository.getSettings()
                    if (AppUpdateManager.isUpdateAvailable(currentCode, fresh.latestVersionCode)) {
                        settings = fresh
                        val updatePrefs = context.getSharedPreferences("sbkd_update_snooze", android.content.Context.MODE_PRIVATE)
                        val snoozedCode = updatePrefs.getInt("snoozed_version_code", 0)
                        val snoozeUntil = updatePrefs.getLong("snooze_until_timestamp", 0L)
                        val isSnoozed = (snoozedCode >= fresh.latestVersionCode && System.currentTimeMillis() < snoozeUntil)
                        if (!isSnoozed || fresh.isForceUpdate) {
                            showUpdatePopup = true
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeScreenInit", "Update check error on startup", e)
            }
        }

        // ⚡ ADAPTIVE INTELLIGENT LIVE BROADCAST SYNC:
        // Adjusts polling frequency adaptively:
        // - 5s when darbar is active or devotee holds today's token (rapid alerts!)
        // - 15s to 18s during regular/inactive hours (protects server & battery!)
        // - Exponential backoff on network failures
        scope.launch {
            var consecutiveErrors = 0
            while (isActive) {
                val myTokPrefs = try {
                    context.getSharedPreferences("sbkd_devotee_my_token_prefs", Context.MODE_PRIVATE)
                } catch (e: Exception) { null }
                val myToken = myTokPrefs?.getInt("my_token_number", 0) ?: 0
                val myTokenDate = myTokPrefs?.getString("my_token_date", "") ?: ""
                val todayStr = com.example.shribalajikripadham.data.local.DatabaseHelper.getTodayDateString()
                val hasActiveTokenToday = myToken > 0 && (myTokenDate == todayStr || myTokenDate.isBlank())

                // Dynamic interval calculation:
                val baseDelayMs = when {
                    consecutiveErrors > 0 -> (10_000L * consecutiveErrors.coerceAtMost(3))
                    hasActiveTokenToday || settings.isDarbarActive -> 5_000L
                    else -> 18_000L
                }

                delay(baseDelayMs)

                try {
                    val rawCfg = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchLiveConfig()
                    if (rawCfg != null && (rawCfg.optBoolean("success", false) || rawCfg.has("ashram_name") || rawCfg.has("config"))) {
                        consecutiveErrors = 0
                        val liveCfg = if (rawCfg.has("config")) rawCfg.getJSONObject("config") else rawCfg
                        val currentServing = liveCfg.optInt("running_token_number", liveCfg.optInt("current_serving_token", settings.runningTokenNumber))
                        val tokEnabled = if (liveCfg.has("is_token_service_enabled")) liveCfg.optBoolean("is_token_service_enabled") else settings.isTokenServiceEnabled
                        val busLive = if (liveCfg.has("is_bus_booking_live")) liveCfg.optBoolean("is_bus_booking_live") else settings.isBusBookingLive
                        val emNotice = liveCfg.optString("emergency_notice", settings.emergencyNoticeText)
                        val emVis = if (liveCfg.has("is_emergency_notice_visible")) liveCfg.optBoolean("is_emergency_notice_visible") else settings.isEmergencyNoticeVisible
                        val gurujiPhoto = liveCfg.optString("guruji_photo_url", settings.gurujiPhotoUri)
                        val bannerTitle = liveCfg.optString("banner_title", settings.bannerTitle)
                        val bannerSub = liveCfg.optString("banner_subtitle", settings.bannerSubtitle)
                        val timings = liveCfg.optString("darbar_timings", settings.darbarTimings)
                        val isDarbarActive = if (liveCfg.has("is_darbar_active")) liveCfg.optBoolean("is_darbar_active") else settings.isDarbarActive

                        // ⚡ Instant Devotee Token Calling Alert
                        if (currentServing != settings.runningTokenNumber && currentServing > 0) {
                            try {
                                if (myTokPrefs != null && hasActiveTokenToday) {
                                    val lastAlertServing = myTokPrefs.getInt("last_alerted_serving", 0)
                                    if (currentServing != lastAlertServing) {
                                        if (currentServing == myToken) {
                                            NotificationHelper.showSystemNotification(
                                                context = context,
                                                title = "🔔 आपका टोकन नंबर $myToken आ चुका है!",
                                                message = "आपका पावन दर्शन हेतु नंबर आ गया है। कृपया तुरंत पूज्य गुरुजी के समक्ष दरबार में पधारें!",
                                                notificationId = 10006
                                            )
                                            myTokPrefs.edit().putInt("last_alerted_serving", currentServing).apply()
                                        } else if (myToken > currentServing && (myToken - currentServing) <= 5) {
                                            val remaining = myToken - currentServing
                                            NotificationHelper.showSystemNotification(
                                                context = context,
                                                title = "🚨 आपका टोकन समीप है ($remaining टोकन शेष)",
                                                message = "वर्तमान में टोकन #$currentServing बुलाया जा रहा है। आपका टोकन #$myToken है। कृपया तुरंत आश्रम हॉल में उपस्थित रहें!",
                                                notificationId = 10007
                                            )
                                            myTokPrefs.edit().putInt("last_alerted_serving", currentServing).apply()
                                        }
                                    }
                                }
                            } catch (e: Exception) {}
                        }

                        if (currentServing != settings.runningTokenNumber ||
                            tokEnabled != settings.isTokenServiceEnabled ||
                            busLive != settings.isBusBookingLive ||
                            emNotice != settings.emergencyNoticeText ||
                            emVis != settings.isEmergencyNoticeVisible ||
                            (gurujiPhoto.isNotBlank() && gurujiPhoto != settings.gurujiPhotoUri) ||
                            (bannerTitle.isNotBlank() && bannerTitle != settings.bannerTitle) ||
                            (bannerSub.isNotBlank() && bannerSub != settings.bannerSubtitle) ||
                            (timings.isNotBlank() && timings != settings.darbarTimings) ||
                            isDarbarActive != settings.isDarbarActive
                        ) {
                            settings = settings.copy(
                                runningTokenNumber = currentServing,
                                isTokenServiceEnabled = tokEnabled,
                                isBusBookingLive = busLive,
                                emergencyNoticeText = emNotice,
                                isEmergencyNoticeVisible = emVis,
                                gurujiPhotoUri = if (gurujiPhoto.isNotBlank()) gurujiPhoto else settings.gurujiPhotoUri,
                                bannerTitle = if (bannerTitle.isNotBlank()) bannerTitle else settings.bannerTitle,
                                bannerSubtitle = if (bannerSub.isNotBlank()) bannerSub else settings.bannerSubtitle,
                                darbarTimings = if (timings.isNotBlank()) timings else settings.darbarTimings,
                                isDarbarActive = isDarbarActive
                            )
                            repository.updateSettings(settings)
                        }
                    } else {
                        consecutiveErrors++
                    }
                } catch (e: Exception) {
                    consecutiveErrors++
                }
            }
        }

        // 🔄 Continuous auto-check for new app releases in real-time (every 30 seconds)
        scope.launch {
            while (isActive) {
                delay(30_000)
                try {
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
                        settings = repository.getSettings()
                        // Keep settings updated in state; do not interrupt active user session with modal popup
                    }
                } catch (e: Exception) {}
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.width(320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Header with Sacred Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        currentTheme.primaryColor,
                                        currentTheme.headerGradientEnd
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, currentTheme.accentGold, CircleShape)
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
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isHindi) "डूँगरा जाट, बुलन्दशहर" else "Dungra Jaat, Bulandshahr",
                                        fontSize = 12.sp,
                                        color = currentTheme.accentGold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "॥ ॐ श्री हनुमते नमः ॥",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isHindi) "📑 मुख्य पृष्ठ अनुभाग (Page Toggles)" else "📑 Home Pages",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    HomeTab.values().forEach { tab ->
                        val isSelected = selectedHomeTab == tab
                        NavigationDrawerItem(
                            icon = { Text(tab.icon, fontSize = 20.sp) },
                            label = {
                                Text(
                                    text = if (isHindi) {
                                        when (tab) {
                                            HomeTab.DARSHAN_TOKEN -> "पेज 1: मुख्य दर्शन व टोकन"
                                            HomeTab.BHAKTI_AARTI -> "पेज 2: नित्य सेवा व भक्ति (15 पाठ)"
                                            HomeTab.DHARAMSHALA_YATRA -> "पेज 3: आश्रम आवास व यात्रा"
                                            HomeTab.ASHRAM_ABOUT -> "पेज 4: आश्रम परिचय व नियम"
                                        }
                                    } else {
                                        when (tab) {
                                            HomeTab.DARSHAN_TOKEN -> "Page 1: Darshan & Token"
                                            HomeTab.BHAKTI_AARTI -> "Page 2: Sacred Bhakti (15 Aartis)"
                                            HomeTab.DHARAMSHALA_YATRA -> "Page 3: Stay & Yatra"
                                            HomeTab.ASHRAM_ABOUT -> "Page 4: Ashram Info & Rules"
                                        }
                                    },
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) MaroonPrimary else TextPrimaryDark
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                selectedHomeTab = tab
                                scope.launch {
                                    drawerState.close()
                                    homeScrollState.scrollTo(0)
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = GoldSecondary.copy(alpha = 0.25f),
                                unselectedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = Color(0xFFEEEEEE))

                    Text(
                        text = if (isHindi) "🚩 मुख्य सेवाएं व स्क्रीन" else "🚩 Main Screens & Services",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    // Navigation Items
                    data class NavDrawerItem(val icon: String, val title: String, val action: () -> Unit)
                    val navItems = buildList {
                        add(NavDrawerItem("🏠", if (isHindi) "मुख्य पृष्ठ (Home)" else "Home", {
                            selectedHomeTab = HomeTab.DARSHAN_TOKEN
                            scope.launch {
                                drawerState.close()
                                homeScrollState.scrollTo(0)
                            }
                        }))
                        add(NavDrawerItem("🔴", if (isHindi) "🔴 लाइव दर्शन व आरती/भजन" else "🔴 Live Darbar & Bhajans", onNavigateToLiveDarbar))
                        add(NavDrawerItem("🎟️", if (isHindi) "दरबार टोकन जनरेट करें" else "Generate Darbar Token", onNavigateToToken))
                        add(NavDrawerItem("🤳", if (isHindi) "फेस वेरिफिकेशन टोकन" else "Face Token", onNavigateToFaceToken))
                        add(NavDrawerItem("📜", if (isHindi) "डिजिटल पर्चा देखें" else "Digital Parchas", onNavigateToParchas))
                        if (settings.isYatraServiceEnabled) {
                            add(NavDrawerItem("🚗", if (isHindi) "यात्रा व दूरी विवरण" else "Yatra & Distance Info", onNavigateToYatra))
                            if (settings.canDevoteeViewYatraDiary) {
                                add(NavDrawerItem("💰", if (isHindi) "यात्रा खर्च डायरी" else "Yatra Expense Diary", onNavigateToYatraExpenses))
                            }
                        }
                        add(NavDrawerItem("ℹ️", if (isHindi) "आश्रम परिचय व नियम" else "Ashram Info & Rules", onNavigateToInfo))
                        add(NavDrawerItem("🏨", if (isHindi) "धर्मशाला व कमरा आरक्षण" else "Dharamshala Room Booking", onNavigateToDharamshala))
                        add(NavDrawerItem("📖", if (isHindi) "ऐप संपूर्ण मार्गदर्शिका (PDF)" else "Devotee User Manual (PDF)", {
                            scope.launch { drawerState.close() }
                            val file = com.example.shribalajikripadham.util.AshramManualPdfGenerator.generateDevoteeGuidePdf(context)
                            if (file != null) {
                                com.example.shribalajikripadham.util.AshramManualPdfGenerator.openOrSharePdf(
                                    context,
                                    file,
                                    if (isHindi) "श्री बालाजी कृपा धाम - भक्त संपूर्ण मार्गदर्शिका" else "Shri Balaji Kripa Dham - Devotee User Manual"
                                )
                            } else {
                                Toast.makeText(context, if (isHindi) "PDF तैयार करने में असमर्थ" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                            }
                        }))
                        add(NavDrawerItem("🔄", if (isHindi) "ऐप अपडेट जांचें (Live)" else "Check App Update", {
                            scope.launch {
                                drawerState.close()
                                Toast.makeText(context, if (isHindi) "🔄 लाइव सर्वर से अपडेट जांच रहे हैं..." else "Checking server for updates...", Toast.LENGTH_SHORT).show()
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
                                    settings = repository.getSettings()
                                    showUpdatePopup = true
                                } else {
                                    val freshSettings = repository.getSettings()
                                    settings = freshSettings
                                    if (AppUpdateManager.isUpdateAvailable(currentCode, freshSettings.latestVersionCode)) {
                                        showUpdatePopup = true
                                    } else {
                                        if (onlineInfo != null) {
                                            onlineCheckedVerCode = onlineInfo.versionCode
                                            onlineCheckedVerName = onlineInfo.versionName
                                        }
                                        showUpToDateDialog = true
                                    }
                                }
                            }
                        }))
                        add(NavDrawerItem("📥", if (isHindi) "नवीनतम APK डाउनलोड करें" else "Download Latest APK", {
                            scope.launch {
                                drawerState.close()
                                val targetUrl = settings.apkDownloadUrl.ifBlank {
                                    AppUpdateManager.DEFAULT_APK_URL
                                }
                                AppUpdateManager.downloadAndInstallUpdate(context, targetUrl)
                            }
                        }))
                        add(NavDrawerItem("🔐", if (isHindi) "प्रबंधक / सेवादार लॉगिन" else "Sevadar & Admin Portal", onNavigateToAdmin))
                    }

                    navItems.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Text(item.icon, fontSize = 20.sp) },
                            label = {
                                Text(
                                    item.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                            },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                item.action()
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Color(0xFFEEEEEE))

                    Text(
                        text = if (isHindi) "⚙️ त्वरित सेटिंग्स" else "⚙️ Quick Actions",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    // 🌟 10 COMPLETE UI LOOKS SHOWCASE IN SIDEBAR DRAWER
                    Text(
                        text = if (isHindi) "🌟 ऐप का स्वरूप / 10 UI Looks (पूरा ढांचा बदलें)" else "🌟 App Architecture / 10 UI Looks",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    if (settings.isUiLayoutEnforced) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👑", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "सुपर एडमिन यूनिवर्सल कंट्रोल सक्रिय" else "Super Admin Universal Control Active",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaroonAccent
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isHindi) "वर्तमान सक्रिय ढांचा: ${activeLayout.titleHindi} (${activeLayout.icon})\n(यह रूप सभी भक्तों के फोन पर अनिवार्य रूप से लागू है)" else "Active Layout: ${activeLayout.titleEnglish} (${activeLayout.icon})\n(Enforced across all devotees)",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            for (layout in AppUiLayout.entries) {
                                val isSelected = layout == activeLayout
                                Surface(
                                    onClick = {
                                        activeLayout = layout
                                        LayoutPreferences.saveLayout(context, layout)
                                        scope.launch {
                                            drawerState.close()
                                            Toast.makeText(
                                                context,
                                                if (isHindi) "✅ ऐप का रूप बदलकर '${layout.titleHindi}' हो गया!" else "✅ Switched to ${layout.titleEnglish}!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) currentTheme.primaryColor.copy(alpha = 0.14f) else Color(0xFFFBFBFB),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.8.dp else 0.6.dp,
                                        color = if (isSelected) currentTheme.primaryColor else Color(0xFFE0E0E0)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) currentTheme.primaryColor else Color(0xFFEEEEEE)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(layout.icon, fontSize = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isHindi) layout.titleHindi else layout.titleEnglish,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isSelected) currentTheme.primaryColor else TextPrimaryDark
                                            )
                                            Text(
                                                text = if (isHindi) layout.subtitleHindi else layout.titleHindi,
                                                fontSize = 10.sp,
                                                color = TextSecondaryDark,
                                                maxLines = 1
                                            )
                                        }
                                        if (isSelected) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = currentTheme.primaryColor
                                            ) {
                                                Text(
                                                    text = if (isHindi) "सक्रिय" else "ACTIVE",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Compact Palette Row of 12 Sacred Colors
                    Text(
                        text = if (isHindi) "🎨 आध्यात्मिक रंग (12 Palette)" else "🎨 Sacred Palette",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(SacredTheme.entries.size) { idx ->
                            val theme = SacredTheme.entries[idx]
                            val isSel = theme == currentTheme
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(theme.primaryColor)
                                    .border(
                                        if (isSel) 2.dp else 0.8.dp,
                                        if (isSel) Color.White else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { onThemeChanged(theme) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live Cloud Sync Action Button
                    NavigationDrawerItem(
                        icon = { Text("🔄", fontSize = 20.sp) },
                        label = {
                            Text(
                                if (isHindi) "क्लाउड से लाइव सिंक करें" else "Sync with Cloud Now",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                        },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                Toast.makeText(context, if (isHindi) "🔄 क्लाउड से डेटा सिंक हो रहा है..." else "Syncing with cloud...", Toast.LENGTH_SHORT).show()
                                try {
                                    repository.syncLiveConfigFromGitHub()
                                    repository.syncAdminsFromGitHub()
                                    repository.syncLiveParchasFromGitHub()
                                    settings = repository.getSettings()
                                    val evs = repository.getAllEvents()
                                    if (evs.isNotEmpty()) dynamicEvents = evs
                                    activeSevadars = repository.getAllActiveSevadars()
                                    uiSectionConfigs = repository.getUiSectionConfigs()
                                    Toast.makeText(context, if (isHindi) "✅ ऐप का सारा डेटा लाइव अपडेट हो गया!" else "✅ App data updated live from cloud!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "सिंक त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color(0xFFF5F5F5)),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )

                    // Quick Language Toggle
                    NavigationDrawerItem(
                        icon = { Text("🌐", fontSize = 20.sp) },
                        label = {
                            Text(
                                if (isHindi) "भाषा बदलें (English)" else "Switch Language (हिंदी)",
                                fontSize = 14.sp,
                                color = TextPrimaryDark
                            )
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onToggleLanguage()
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )

                    // Ashram Call
                    NavigationDrawerItem(
                        icon = { Text("📞", fontSize = 20.sp) },
                        label = {
                            Text(
                                if (isHindi) "आश्रम संपर्क (कॉल करें)" else "Call Ashram Helpline",
                                fontSize = 14.sp,
                                color = TextPrimaryDark
                            )
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            val phone = settings.contactPhone.ifEmpty { "+91 98765 00000" }
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )

                    // Ashram WhatsApp
                    NavigationDrawerItem(
                        icon = { Text("💬", fontSize = 20.sp) },
                        label = {
                            Text(
                                if (isHindi) "व्हाट्सएप सेवा" else "WhatsApp Helpline",
                                fontSize = 14.sp,
                                color = TextPrimaryDark
                            )
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            val wa = settings.whatsappNumber.ifEmpty { "+919876543210" }
                            openSocialMediaLink(
                                context = context,
                                rawUrl = "https://wa.me/91$wa",
                                defaultUrl = "https://wa.me/91$wa",
                                isWhatsApp = true
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )

                    // Official Website Link (Live)
                    NavigationDrawerItem(
                        icon = { Text("🌐", fontSize = 20.sp) },
                        label = {
                            Text(
                                if (isHindi) "आधिकारिक वेबसाइट (Live)" else "Official Website (Live)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            val webUrl = settings.officialWebsiteUrl.ifEmpty { "https://shribalajikripadham.online" }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color(0xFFE3F2FD)),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )

                    // Devotee App Share (WhatsApp & Social Media)
                    NavigationDrawerItem(
                        icon = { Text("📲", fontSize = 20.sp) },
                        label = {
                            Text(
                                if (isHindi) "ऐप शेयर करें (भक्तों को भेजें)" else "Share App with Devotees",
                                fontSize = 14.sp,
                                color = TextPrimaryDark
                            )
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            val shareUrl = settings.appShareUrl.ifEmpty { "https://shribalajikripadham.org/app" }
                            val shareMsg = if (isHindi) {
                                "🚩 ॐ श्री हनुमते नमः 🚩\n\nश्री बालाजी कृपा धाम (ग्राम डूँगरा जाट, जिला बुलंदशहर, उ.प्र.)\nपरम पूज्य गुरुजी तेजवीर सिंह जी महाराज\n\nआश्रम का आधिकारिक मोबाइल ऐप डाउनलोड करें और रविवार टोकन, पर्चा, व लाइव जानकारी प्राप्त करें:\n$shareUrl"
                            } else {
                                "🚩 Om Shri Hanumate Namah 🚩\n\nShri Balaji Kripa Dham (Gram Dungra Jaat, Bulandshahr, UP)\nParam Pujya Guruji Tejveer Singh Ji\n\nDownload the Official Ashram App for Sunday Token, Parchas & Live Updates:\n$shareUrl"
                            }
                            shareAppContent(context, shareMsg, if (isHindi) "श्री बालाजी कृपा धाम ऐप" else "Shri Balaji Kripa Dham App")
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "v${AppUpdateManager.getCurrentVersionName(context)} • श्री बालाजी कृपा धाम",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } }
                            ) {
                                Text("☰", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, currentTheme.secondaryColor, CircleShape)
                                    .clickable { scope.launch { drawerState.open() } }
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
                    Column {
                        Text(
                            text = if (isHindi) "श्री बालाजी कृपा धाम" else "Shri Balaji Kripa Dham",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = if (isHindi) "डूँगरा जाट, बुलन्दशहर (उ.प्र.)" else "Dungra Jaat, Bulandshahr (U.P.)",
                            fontSize = 11.sp,
                            color = currentTheme.accentGold,
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    // Manual Check for Updates Button
                    IconButton(
                        onClick = {
                            scope.launch {
                                Toast.makeText(
                                    context,
                                    if (isHindi) "🔄 डेटा एवं नवीनतम अपडेट जांच रहे हैं..." else "🔄 Checking for live updates...",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // 1. Sync latest live UI layout & settings from GitHub
                                try {
                                    val (synced, liveConfig) = repository.syncLiveConfigFromGitHub()
                                    if (synced && liveConfig != null && liveConfig.sections.isNotEmpty()) {
                                        uiSectionConfigs = liveConfig.sections
                                    }
                                } catch (e: Exception) {}

                                // 2. Live Online Zero-Cache Update Check
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
                                    settings = repository.getSettings()
                                    showUpdatePopup = true
                                } else {
                                    val freshSettings = repository.getSettings()
                                    settings = freshSettings
                                    if (AppUpdateManager.isUpdateAvailable(currentCode, freshSettings.latestVersionCode)) {
                                        showUpdatePopup = true
                                    } else {
                                        if (onlineInfo != null) {
                                            onlineCheckedVerCode = onlineInfo.versionCode
                                            onlineCheckedVerName = onlineInfo.versionName
                                        }
                                        showUpToDateDialog = true
                                    }
                                }
                            }
                        }
                    ) {
                        Text("🔄", fontSize = 19.sp)
                    }

                    IconButton(
                        onClick = {
                            val file = com.example.shribalajikripadham.util.AshramManualPdfGenerator.generateDevoteeGuidePdf(context)
                            if (file != null) {
                                com.example.shribalajikripadham.util.AshramManualPdfGenerator.openOrSharePdf(
                                    context,
                                    file,
                                    if (isHindi) "श्री बालाजी कृपा धाम - भक्त संपूर्ण मार्गदर्शिका" else "Shri Balaji Kripa Dham - Devotee User Manual"
                                )
                            } else {
                                Toast.makeText(context, if (isHindi) "PDF तैयार करने में असमर्थ" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("📖", fontSize = 19.sp)
                    }

                    Button(
                        onClick = onToggleLanguage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.25f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isHindi) "English" else "हिंदी",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.topBarColor
                )
            )
        },
        containerColor = if (currentTheme.isDark) Color(0xFF121212) else SacredBackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 🌟 PINNED SACRED GOLDEN TAB BAR (Side-Toggle & Page Selector)
            ScrollableTabRow(
                selectedTabIndex = selectedHomeTab.ordinal,
                containerColor = Color.White,
                contentColor = MaroonPrimary,
                edgePadding = 8.dp,
                divider = {
                    HorizontalDivider(color = currentTheme.secondaryColor.copy(alpha = 0.3f), thickness = 1.dp)
                }
            ) {
                HomeTab.values().forEach { tab ->
                    Tab(
                        selected = selectedHomeTab == tab,
                        onClick = {
                            selectedHomeTab = tab
                            scope.launch { homeScrollState.scrollTo(0) }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.icon, fontSize = if (isCompact) 13.sp else 15.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHindi) tab.titleHindi else tab.titleEnglish,
                                    fontWeight = if (selectedHomeTab == tab) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = if (isCompact) 12.sp else 13.5.sp,
                                    color = if (selectedHomeTab == tab) MaroonPrimary else TextSecondaryDark
                                )
                            }
                        }
                    )
                }
            }

            // Scrollable Content for the Selected Tab
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(homeScrollState)
                    .padding(responsivePadding)
            ) {
                when (selectedHomeTab) {
                    HomeTab.DARSHAN_TOKEN -> {
                        // 📖 BHAKT APP MARGDARSHIKA (USER MANUAL PDF) BANNER
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
                            shape = RoundedCornerShape(if (isCompact) 10.dp else 14.dp),
                            border = BorderStroke(1.2.dp, Color(0xFFFFB300)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = sectionSpacing)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = if (isCompact) 10.dp else 12.dp, vertical = if (isCompact) 6.dp else 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isCompact) 32.dp else 42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFECB3)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📖", fontSize = if (isCompact) 17.sp else 22.sp)
                                }
                                Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHindi) "भक्त संपूर्ण ऐप मार्गदर्शिका (PDF)" else "Devotee User Manual (PDF)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompact) 12.sp else 13.5.sp,
                                        color = MaroonPrimary,
                                        maxLines = 1
                                    )
                                    if (!isUltraCompact) {
                                        Text(
                                            text = if (isHindi) "ऐप में क्या-क्या है और कैसे उपयोग करें - संपूर्ण विवरण पढ़ें" else "Learn everything you can do and see in the app",
                                            fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                                            color = TextSecondaryDark,
                                            maxLines = 1,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        val file = com.example.shribalajikripadham.util.AshramManualPdfGenerator.generateDevoteeGuidePdf(context)
                                        if (file != null) {
                                            com.example.shribalajikripadham.util.AshramManualPdfGenerator.openOrSharePdf(
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
                                    contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 10.dp, vertical = if (isCompact) 4.dp else 6.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "PDF देखें" else "Open PDF",
                                        fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        val currentCode = AppUpdateManager.getCurrentVersionCode(context)
                        val currentVerName = AppUpdateManager.getCurrentVersionName(context)
                        val isUpdateAvailable = AppUpdateManager.isUpdateAvailable(currentCode, settings.latestVersionCode)

                        // 📲 PERMANENT SACRED APP VERSION & DOWNLOAD / UPDATE PORTAL CARD
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUpdateAvailable) Color(0xFFFFF8E1) else Color(0xFFF1F8E9)
                            ),
                            shape = RoundedCornerShape(if (isCompact) 12.dp else 16.dp),
                            border = BorderStroke(1.5.dp, if (isUpdateAvailable) Color(0xFFFFB300) else Color(0xFF81C784)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = sectionSpacing)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(if (isCompact) 10.dp else 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isCompact) 36.dp else 44.dp)
                                            .clip(CircleShape)
                                            .background(if (isUpdateAvailable) Color(0xFFFFB300) else Color(0xFFC8E6C9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(if (isUpdateAvailable) "🔔" else "📲", fontSize = if (isCompact) 18.sp else 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (isUpdateAvailable) {
                                                    if (isHindi) "नया अपडेट v${settings.latestVersionName} उपलब्ध!" else "New Update v${settings.latestVersionName} Available!"
                                                } else {
                                                    if (isHindi) "ऐप संस्करण: v$currentVerName" else "App Version: v$currentVerName"
                                                },
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = if (isCompact) 13.sp else 14.5.sp,
                                                color = if (isUpdateAvailable) MaroonPrimary else Color(0xFF1B5E20)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = if (isUpdateAvailable) Color(0xFFFFE082) else Color(0xFFA5D6A7),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = if (isUpdateAvailable) "Build #${settings.latestVersionCode}" else "Build #$currentCode",
                                                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isUpdateAvailable) Color(0xFFB78103) else Color(0xFF1B5E20),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isUpdateAvailable) {
                                                if (isHindi) "नवीनतम फीचर्स व सुरक्षा पैच के लिए तुरंत अपडेट करें।" else "Update now for latest features & security patches."
                                            } else {
                                                if (isHindi) "✅ आपका ऐप नवीनतम संस्करण पर है (Up to Date)" else "✅ Your app is on the latest version (Up to Date)"
                                            },
                                            fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                                            fontWeight = if (isUpdateAvailable) FontWeight.Normal else FontWeight.SemiBold,
                                            color = if (isUpdateAvailable) TextSecondaryDark else Color(0xFF2E7D32)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                Toast.makeText(context, if (isHindi) "🔄 लाइव सर्वर से जांच रहे हैं..." else "Checking server...", Toast.LENGTH_SHORT).show()
                                                val cCode = AppUpdateManager.getCurrentVersionCode(context)
                                                val online = AppUpdateManager.fetchLatestUpdateFromOnline()
                                                if (online != null && online.versionCode > cCode) {
                                                    repository.updateAppUpdateConfig(
                                                        latestVersionCode = online.versionCode,
                                                        latestVersionName = online.versionName,
                                                        updateNotes = if (isHindi) online.updateNotesHindi else online.updateNotesEnglish,
                                                        apkDownloadUrl = online.apkUrl,
                                                        isForceUpdate = online.isForce
                                                    )
                                                    settings = repository.getSettings()
                                                    showUpdatePopup = true
                                                } else {
                                                    val fresh = repository.getSettings()
                                                    settings = fresh
                                                    if (AppUpdateManager.isUpdateAvailable(cCode, fresh.latestVersionCode)) {
                                                        showUpdatePopup = true
                                                    } else {
                                                        if (online != null) {
                                                            onlineCheckedVerCode = online.versionCode
                                                            onlineCheckedVerName = online.versionName
                                                        }
                                                        showUpToDateDialog = true
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (isUpdateAvailable) Color(0xFFFFB300) else Color(0xFF81C784)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text("🔄", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isHindi) "अपडेट जांचें" else "Check Update",
                                            fontSize = if (isCompact) 11.sp else 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUpdateAvailable) MaroonPrimary else Color(0xFF2E7D32)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (isUpdateAvailable) {
                                                showUpdatePopup = true
                                            } else {
                                                val targetUrl = settings.apkDownloadUrl.ifBlank {
                                                    AppUpdateManager.DEFAULT_APK_URL
                                                }
                                                AppUpdateManager.downloadAndInstallUpdate(context, targetUrl)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isUpdateAvailable) MaroonPrimary else Color(0xFF2E7D32)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(if (isUpdateAvailable) "⚡" else "📥", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isUpdateAvailable) {
                                                if (isHindi) "अभी अपडेट करें" else "Update Now"
                                            } else {
                                                if (isHindi) "APK डाउनलोड करें" else "Download APK"
                                            },
                                            fontSize = if (isCompact) 11.sp else 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // 🌟 GRAND LIVE TOKEN STATUS ANNOUNCEMENT BANNER
                        val scheduleState = remember(settings) {
                            SundayTokenScheduleHelper.evaluateSchedule(settings)
                        }
                        val isTokenOpen = scheduleState is SundayScheduleState.Open

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (scheduleState) {
                                    is SundayScheduleState.Open -> Color(0xFFE8F5E9)
                                    else -> Color(0xFFFFFBEA)
                                }
                            ),
                            shape = RoundedCornerShape(if (isCompact) 12.dp else 16.dp),
                            border = BorderStroke(
                                if (isCompact) 1.5.dp else 2.dp,
                                when (scheduleState) {
                                    is SundayScheduleState.Open -> Color(0xFF2E7D32)
                                    is SundayScheduleState.SundayBeforeStart -> Color(0xFFD84315)
                                    is SundayScheduleState.SundayClosedEvening -> Color(0xFF8B0000)
                                    is SundayScheduleState.NonSunday -> Color(0xFFD84315)
                                    else -> Color(0xFF8B0000)
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isCompact) 2.dp else 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = sectionSpacing)
                                .clickable { onNavigateToFaceToken() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = if (isCompact) 10.dp else 14.dp, vertical = if (isCompact) 8.dp else 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isCompact) 36.dp else 46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (scheduleState) {
                                                is SundayScheduleState.Open -> Color(0xFF2E7D32)
                                                is SundayScheduleState.SundayBeforeStart -> Color(0xFFD84315)
                                                is SundayScheduleState.SundayClosedEvening -> Color(0xFF8B0000)
                                                is SundayScheduleState.NonSunday -> Color(0xFFD84315)
                                                else -> Color(0xFF8B0000)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (scheduleState) {
                                            is SundayScheduleState.Open -> "🎟️"
                                            is SundayScheduleState.SundayBeforeStart -> "⏳"
                                            is SundayScheduleState.SundayClosedEvening -> "🔴"
                                            is SundayScheduleState.NonSunday -> "📅"
                                            else -> "🔒"
                                        },
                                        fontSize = if (isCompact) 18.sp else 24.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when (scheduleState) {
                                            is SundayScheduleState.Open -> if (isHindi) "🟢 रविवार टोकन वितरण चालू है!" else "🟢 Sunday Token Generation OPEN!"
                                            is SundayScheduleState.SundayBeforeStart -> if (isHindi) "⏳ टोकन आज सुबह 8:30 बजे खुलेंगे" else "⏳ Opens Today at 8:30 AM"
                                            is SundayScheduleState.SundayClosedEvening -> if (isHindi) "🔴 आज के टोकन पूरे हो गए हैं" else "🔴 Today's Tokens Closed"
                                            is SundayScheduleState.NonSunday -> if (isHindi) "📅 रविवार टोकन वितरण सूचना" else "📅 Sunday Token Schedule"
                                            is SundayScheduleState.CustomScheduled -> if (isHindi) "⏳ टोकन पूर्व-निर्धारित है" else "⏳ Token Scheduled"
                                            else -> if (isHindi) "🔴 रविवार टोकन वितरण बंद है" else "🔴 Token Service Closed"
                                        },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = if (isCompact) 13.sp else 15.sp,
                                        color = when (scheduleState) {
                                            is SundayScheduleState.Open -> Color(0xFF1B5E20)
                                            else -> Color(0xFF8B0000)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when (scheduleState) {
                                            is SundayScheduleState.Open -> if (isHindi) "👉 अभी टोकन प्राप्त करें (टैप करें ➔)" else "👉 Tap here to get token now ➔"
                                            is SundayScheduleState.SundayBeforeStart -> if (isHindi) "सुबह 8:30 बजे आश्रम लोकेशन पर टोकन प्राप्त करें" else "Available from 8:30 AM at Ashram"
                                            is SundayScheduleState.SundayClosedEvening -> if (isHindi) "अब टोकन आगामी रविवार, ${scheduleState.nextSundayDateStr} को 8:30 AM से मिलेंगे" else "Next tokens on Sunday, ${scheduleState.nextSundayDateStr} 8:30 AM"
                                            is SundayScheduleState.NonSunday -> if (isHindi) "आगामी रविवार, ${scheduleState.nextSundayDateStr} को 8:30 AM से मिलेंगे" else "Next tokens on Sunday, ${scheduleState.nextSundayDateStr} 8:30 AM"
                                            is SundayScheduleState.CustomScheduled -> if (isHindi) "खुलने का समय: ${scheduleState.formattedDate}" else "Opens at: ${scheduleState.formattedDate}"
                                            else -> if (isHindi) "आश्रम व्यवस्था अनुसार टोकन सेवा अभी बंद है" else "Token service paused by Ashram"
                                        },
                                        fontSize = if (isCompact) 11.sp else 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = if (isCompact) 14.sp else 18.sp,
                                        color = when (scheduleState) {
                                            is SundayScheduleState.Open -> Color(0xFF1B5E20)
                                            else -> Color(0xFF111111)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { onNavigateToFaceToken() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when (scheduleState) {
                                            is SundayScheduleState.Open -> Color(0xFF2E7D32)
                                            else -> Color(0xFF8B0000)
                                        }
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 10.dp, vertical = if (isCompact) 4.dp else 6.dp)
                                ) {
                                    Text(
                                        text = if (isTokenOpen) (if (isHindi) "टोकन लें ➔" else "Get Token ➔") else (if (isHindi) "विवरण ➔" else "Details ➔"),
                                        fontSize = if (isCompact) 10.5.sp else 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // 🚩 ASHRAM MAIN HERO BANNER
                        if (settings.isBannerVisible && settings.bannerPhotoUri.isNotBlank()) {
                            AshramHomeHeroBanner(
                                bannerPhotoUri = settings.bannerPhotoUri,
                                title = settings.bannerTitle,
                                subtitle = settings.bannerSubtitle,
                                actionUrl = settings.bannerActionUrl,
                                primaryColor = currentTheme.primaryColor,
                                secondaryColor = currentTheme.secondaryColor,
                                context = context
                            )
                            Spacer(modifier = Modifier.height(sectionSpacing))
                        }

                        // 🎯 AI SMART QUEUE & LIVE DARSHAN WAITING TIME (LIVE ETA)
                        val myTokPrefs = remember { context.getSharedPreferences("sbkd_devotee_my_token_prefs", Context.MODE_PRIVATE) }
                        val devoteeMyToken = remember(settings.runningTokenNumber) { myTokPrefs.getInt("my_token_number", 0) }
                        val devoteeMyTokenDate = remember(settings.runningTokenNumber) { myTokPrefs.getString("my_token_date", "") ?: "" }
                        val todayDateStr = remember { com.example.shribalajikripadham.data.local.DatabaseHelper.getTodayDateString() }
                        val isMyTokenToday = devoteeMyToken > 0 && (devoteeMyTokenDate == todayDateStr || devoteeMyTokenDate.isBlank())

                        if (settings.runningTokenNumber > 0 || isMyTokenToday) {
                            val queueEta = remember(devoteeMyToken, settings.runningTokenNumber, isMyTokenToday) {
                                com.example.shribalajikripadham.util.SundayTokenScheduleHelper.calculateQueueEta(
                                    myToken = if (isMyTokenToday) devoteeMyToken else 0,
                                    currentServing = settings.runningTokenNumber
                                )
                            }

                            DevoteeSmartQueueEtaCard(
                                queueEta = queueEta,
                                runningTokenNumber = settings.runningTokenNumber,
                                devoteeToken = if (isMyTokenToday) devoteeMyToken else 0,
                                isHindi = isHindi,
                                isCompact = isCompact,
                                onNavigateToToken = onNavigateToToken
                            )
                            Spacer(modifier = Modifier.height(sectionSpacing))
                        }

                        // 🌺 दैनिक अलौकिक श्रृंगार दर्शन
                        DailyDarshanQuickCard(
                            isHindi = isHindi,
                            ashramSettings = settings
                        )
                        Spacer(modifier = Modifier.height(sectionSpacing))

                        // IF CLASSIC_DARBAR: render core darbar sections
                        if (activeLayout == AppUiLayout.CLASSIC_DARBAR) {
                            val classicDarbarSectionIds = listOf(
                                UiSectionConfig.ID_EMERGENCY_NOTICE,
                                UiSectionConfig.ID_FREE_TREATMENT_BOX,
                                UiSectionConfig.ID_TOKEN_COUNTDOWN,
                                UiSectionConfig.ID_SMART_FACE_TOKEN,
                                UiSectionConfig.ID_QUICK_SERVICES
                            )
                            for (secId in classicDarbarSectionIds) {
                                RenderClassicSection(
                                    sectionId = secId,
                                    settings = settings,
                                    isHindi = isHindi,
                                    currentTheme = currentTheme,
                                    activeSevadars = activeSevadars,
                                    sevadarProfiles = sevadarProfiles,
                                    dynamicEvents = dynamicEvents,
                                    onNavigateToToken = onNavigateToToken,
                                    onNavigateToFaceToken = onNavigateToFaceToken,
                                    onNavigateToYatra = onNavigateToYatra,
                                    onNavigateToInfo = onNavigateToInfo,
                                    onNavigateToAdmin = onNavigateToAdmin,
                                    onNavigateToParchas = onNavigateToParchas,
                                    onNavigateToLiveDarbar = onNavigateToLiveDarbar,
                                    onNavigateToDharamshala = onNavigateToDharamshala,
                                    context = context,
                                    isCompact = isCompact
                                )
                                Spacer(modifier = Modifier.height(sectionSpacing))
                            }
                        } else {
                            // Render chosen alternative layout
                            when (activeLayout) {
                                AppUiLayout.MODERN_CARDS -> ModernCardsLayout(settings, isHindi, currentTheme, activeSevadars, dynamicEvents, onNavigateToToken, onNavigateToFaceToken, onNavigateToYatra, onNavigateToInfo, onNavigateToAdmin, onNavigateToParchas, onNavigateToYatraExpenses, onThemeChanged)
                                AppUiLayout.VEDIC_GRID -> VedicGridLayout(settings, isHindi, currentTheme, activeSevadars, dynamicEvents, onNavigateToToken, onNavigateToFaceToken, onNavigateToYatra, onNavigateToInfo, onNavigateToAdmin, onNavigateToParchas, onNavigateToYatraExpenses, onThemeChanged)
                                AppUiLayout.COMPACT_LIST -> CompactListLayout(settings, isHindi, currentTheme, activeSevadars, dynamicEvents, onNavigateToToken, onNavigateToFaceToken, onNavigateToYatra, onNavigateToInfo, onNavigateToAdmin, onNavigateToParchas, onNavigateToYatraExpenses, onThemeChanged)
                                AppUiLayout.DIVINE_FEED -> DivineFeedLayout(settings, isHindi, currentTheme, activeSevadars, dynamicEvents, onNavigateToToken, onNavigateToFaceToken, onNavigateToYatra, onNavigateToInfo, onNavigateToAdmin, onNavigateToParchas, onNavigateToYatraExpenses, onThemeChanged)
                                AppUiLayout.MAHABALI_HERO -> MahabaliHeroLayout(settings, isHindi, currentTheme, activeSevadars, dynamicEvents, onNavigateToToken, onNavigateToFaceToken, onNavigateToYatra, onNavigateToInfo, onNavigateToAdmin, onNavigateToParchas, onNavigateToYatraExpenses, onThemeChanged)
                                AppUiLayout.BHAKTI_ACCORDION -> BhaktiAccordionLayout(settings, isHindi, currentTheme, activeSevadars, dynamicEvents, onNavigateToToken, onNavigateToFaceToken, onNavigateToYatra, onNavigateToInfo, onNavigateToAdmin, onNavigateToParchas, onNavigateToYatraExpenses, onThemeChanged)
                                AppUiLayout.PARIKRAMA_FLOW -> MandirParikramaLayout(settings, isHindi, currentTheme, activeSevadars, dynamicEvents, onNavigateToToken, onNavigateToFaceToken, onNavigateToYatra, onNavigateToInfo, onNavigateToAdmin, onNavigateToParchas, onNavigateToYatraExpenses, onThemeChanged)
                                AppUiLayout.GOLDEN_LOTUS -> GoldenLotusLayout(settings, isHindi, currentTheme, activeSevadars, dynamicEvents, onNavigateToToken, onNavigateToFaceToken, onNavigateToYatra, onNavigateToInfo, onNavigateToAdmin, onNavigateToParchas, onNavigateToYatraExpenses, onThemeChanged)
                                AppUiLayout.SIDDHA_PEETH_PORTAL -> SiddhaPeethPortalLayout(settings, isHindi, currentTheme, activeSevadars, dynamicEvents, onNavigateToToken, onNavigateToFaceToken, onNavigateToYatra, onNavigateToInfo, onNavigateToAdmin, onNavigateToParchas, onNavigateToYatraExpenses, onThemeChanged)
                                else -> {}
                            }
                        }

                        // 📢 SPONSORED ADS / ASHRAM SEVA SAHYOG BANNER
                        if (settings.isAdsEnabled && (settings.adBannerPhotoUri.isNotBlank() || settings.adBannerTitle.isNotBlank())) {
                            DevoteeSponsorAdBanner(
                                photoUri = settings.adBannerPhotoUri,
                                title = settings.adBannerTitle,
                                description = settings.adBannerDescription,
                                targetUrl = settings.adTargetUrl,
                                primaryColor = currentTheme.primaryColor,
                                secondaryColor = currentTheme.secondaryColor,
                                context = context
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    HomeTab.BHAKTI_AARTI -> {
                        DevoteeSacredAartiBhaktiTab(
                            isHindi = isHindi,
                            currentTheme = currentTheme,
                            context = context,
                            isCompact = isCompact,
                            onNavigateToLiveDarbar = onNavigateToLiveDarbar,
                            onNavigateToPanchang = onNavigateToPanchang,
                            onNavigateToSacredGranth = onNavigateToSacredGranth,
                            onOpenLyrics = { track -> viewingLyricsTrack = track }
                        )
                    }

                    HomeTab.DHARAMSHALA_YATRA -> {
                        DevoteeDharamshalaYatraTab(
                            isHindi = isHindi,
                            currentTheme = currentTheme,
                            context = context,
                            isCompact = isCompact,
                            onNavigateToDharamshala = onNavigateToDharamshala,
                            onNavigateToYatra = onNavigateToYatra,
                            onNavigateToYatraExpenses = onNavigateToYatraExpenses
                        )
                    }

                    HomeTab.ASHRAM_ABOUT -> {
                        DevoteeAshramAboutTab(
                            isHindi = isHindi,
                            currentTheme = currentTheme,
                            settings = settings,
                            activeSevadars = activeSevadars,
                            sevadarProfiles = sevadarProfiles,
                            dynamicEvents = dynamicEvents,
                            context = context,
                            isCompact = isCompact,
                            onNavigateToParchas = onNavigateToParchas,
                            onNavigateToInfo = onNavigateToInfo,
                            onNavigateToToken = onNavigateToToken,
                            onNavigateToFaceToken = onNavigateToFaceToken,
                            onNavigateToYatra = onNavigateToYatra,
                            onNavigateToAdmin = onNavigateToAdmin
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Page Switcher / Quick Navigation Pills
                HomeTabBottomSwitcher(
                    currentTab = selectedHomeTab,
                    isHindi = isHindi,
                    onSelectTab = { tab ->
                        selectedHomeTab = tab
                        scope.launch { homeScrollState.scrollTo(0) }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Sacred Lyrics Full Viewer Modal Dialog
    viewingLyricsTrack?.let { track ->
        SacredLyricsViewerDialog(
            track = track,
            isHindi = isHindi,
            onDismiss = { viewingLyricsTrack = null }
        )
    }


            }
    // IN-APP UPDATE POPUP DIALOG (Pops up directly on Home Screen!)
    if (showUpdatePopup) {
        Dialog(
            onDismissRequest = {
                if (!settings.isForceUpdate && !isDownloadingUpdate) {
                    val updatePrefs = context.getSharedPreferences("sbkd_update_snooze", android.content.Context.MODE_PRIVATE)
                    updatePrefs.edit()
                        .putInt("snoozed_version_code", settings.latestVersionCode)
                        .putLong("snooze_until_timestamp", System.currentTimeMillis() + (24 * 60 * 60 * 1000L))
                        .apply()
                    showUpdatePopup = false
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = !settings.isForceUpdate && !isDownloadingUpdate,
                dismissOnClickOutside = !settings.isForceUpdate && !isDownloadingUpdate
            )
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(10.dp),
                border = BorderStroke(2.dp, currentTheme.secondaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Divine App Icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        currentTheme.primaryColor,
                                        currentTheme.headerGradientEnd
                                    )
                                )
                            )
                            .border(2.dp, currentTheme.secondaryColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚀", fontSize = 30.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isHindi) "नया संस्करण उपलब्ध है!" else "New Version Available!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = currentTheme.primaryColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Version Badges: Current v1.0 -> Latest v2.0
                    Surface(
                        color = currentTheme.primaryColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "v${AppUpdateManager.getCurrentVersionName(context)} ➔ v${settings.latestVersionName} (Build #${settings.latestVersionCode})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primaryColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isHindi) "नवीनतम बदलाव (What's New):" else "What's New in this Update:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF81C784)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛡️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "सुरक्षा पैच एवं सिस्टम स्थिरता सुधार" else "Security Patch & System Stability",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = if (isHindi) "टोकन व दर्शन सेवा की गति एवं सुरक्षा बढ़ाई गई है।" else "Enhanced token & darshan service performance and security.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (isDownloadingUpdate) {
                        // Real-time progress bar with live percentage and MB counters
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8F9FA), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { if (downloadTotalBytes > 0L) downloadProgress / 100f else 0.5f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = currentTheme.primaryColor,
                                trackColor = Color(0xFFE0E0E0)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHindi) "डाउनलोड प्रगति: $downloadProgress%" else "Downloading: $downloadProgress%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentTheme.primaryColor
                                )
                                Text(
                                    text = if (downloadTotalBytes > 0L) {
                                        "${AppUpdateManager.formatFileSize(downloadDownloadedBytes)} / ${AppUpdateManager.formatFileSize(downloadTotalBytes)}"
                                    } else if (downloadDownloadedBytes > 0L) {
                                        AppUpdateManager.formatFileSize(downloadDownloadedBytes)
                                    } else {
                                        "..."
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHindi) "🔒 सुरक्षित इन-ऐप डाउनलोड सक्रिय (सीधा बैकग्राउंड डाउनलोड)"
                                else "🔒 Secure in-app stream active (direct download)",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    } else if (downloadedApkFile != null && downloadedApkFile!!.exists()) {
                        // 100% Downloaded -> Direct In-App Install Button
                        Button(
                            onClick = {
                                AppUpdateManager.triggerApkInstall(context, downloadedApkFile!!)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📦", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi) "इन्स्टॉल करें (Install Update)" else "Install Update Now",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        if (downloadErrorMsg != null) {
                            Surface(
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ $downloadErrorMsg",
                                    color = Color(0xFFC62828),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = {
                                isDownloadingUpdate = true
                                downloadProgress = 0
                                downloadDownloadedBytes = 0L
                                downloadTotalBytes = 0L
                                downloadErrorMsg = null
                                scope.launch {
                                    AppUpdateManager.startInAppUpdateDetailed(
                                        context = context,
                                        downloadUrl = settings.apkDownloadUrl,
                                        onProgress = { progress, downloaded, total ->
                                            downloadProgress = progress
                                            downloadDownloadedBytes = downloaded
                                            downloadTotalBytes = total
                                        },
                                        onSuccess = { apkFile ->
                                            isDownloadingUpdate = false
                                            downloadedApkFile = apkFile
                                            AppUpdateManager.triggerApkInstall(context, apkFile)
                                        },
                                        onError = { err ->
                                            isDownloadingUpdate = false
                                            downloadErrorMsg = err
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚡", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (downloadErrorMsg != null) {
                                        if (isHindi) "पुनः प्रयास करें (Retry)" else "Retry Download"
                                    } else {
                                        if (isHindi) "तुरंत अपडेट करें (Update Now)" else "Update Now"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                AppUpdateManager.openInBrowser(context, settings.apkDownloadUrl)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌐", fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "ब्राउज़र से डाउनलोड करें" else "Download via Browser",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = currentTheme.primaryColor
                                )
                            }
                        }

                        if (!settings.isForceUpdate) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    val updatePrefs = context.getSharedPreferences("sbkd_update_snooze", android.content.Context.MODE_PRIVATE)
                                    updatePrefs.edit()
                                        .putInt("snoozed_version_code", settings.latestVersionCode)
                                        .putLong("snooze_until_timestamp", System.currentTimeMillis() + (24 * 60 * 60 * 1000L))
                                        .apply()
                                    showUpdatePopup = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isHindi) "बाद में (Remind Later)" else "Remind Later",
                                    fontSize = 13.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- APP IS UP-TO-DATE STATUS & DOWNLOAD MODAL ---
    if (showUpToDateDialog) {
        val currentCode = AppUpdateManager.getCurrentVersionCode(context)
        val currentName = AppUpdateManager.getCurrentVersionName(context)
        val displayServerCode = if (onlineCheckedVerCode > 0) onlineCheckedVerCode else settings.latestVersionCode.coerceAtLeast(currentCode)
        val displayServerName = if (onlineCheckedVerName.isNotBlank()) onlineCheckedVerName else settings.latestVersionName.ifBlank { currentName }

        AlertDialog(
            onDismissRequest = { showUpToDateDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✅", fontSize = 28.sp)
                }
            },
            title = {
                Text(
                    text = if (isHindi) "आपका ऐप नवीनतम है" else "App is Up to Date",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF1B5E20),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Color(0xFFF1F8E9),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isHindi) "📱 स्थापित संस्करण:" else "📱 Installed Version:",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark
                                )
                                Text(
                                    text = "v$currentName (#$currentCode)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isHindi) "🌐 सर्वर नवीनतम:" else "🌐 Server Latest:",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark
                                )
                                Text(
                                    text = "v$displayServerName (#$displayServerCode)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isHindi)
                            "बधाई! आपके फोन में श्री बालाजी कृपा धाम का सबसे नया, तेज़ और 100% सुरक्षित संस्करण सक्रिय है। सभी सेवाएं व सुरक्षा नियम सुचारू रूप से कार्य कर रहे हैं।"
                        else
                            "Great! You have the official latest and most secure version of Shri Balaji Kripa Dham app installed. All services are running optimally.",
                        fontSize = 12.5.sp,
                        color = TextPrimaryDark,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isHindi) "यदि आप किसी अन्य फोन हेतु APK फ़ाइल सुरक्षित रखना चाहते हैं, तो नीचे से सीधे डाउनलोड कर सकते हैं।"
                        else "You can also download the latest APK file directly below to share or archive.",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpToDateDialog = false
                        val targetUrl = settings.apkDownloadUrl.ifBlank {
                            AppUpdateManager.DEFAULT_APK_URL
                        }
                        AppUpdateManager.downloadAndInstallUpdate(context, targetUrl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isHindi) "📥 APK डाउनलोड करें" else "📥 Download APK", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUpToDateDialog = false }
                ) {
                    Text(if (isHindi) "ठीक है (OK)" else "OK", fontWeight = FontWeight.Bold, color = TextSecondaryDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- WHAT'S NEW IN V2.2.0 DIALOG ---
    if (showWhatsNewDialog) {
        Dialog(
            onDismissRequest = { showWhatsNewDialog = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(currentTheme.secondaryColor.copy(alpha = 0.4f), Color.Transparent)
                                ),
                                CircleShape
                            )
                            .border(2.dp, currentTheme.secondaryColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 30.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isHindi) "नया संस्करण v2.2.0 सक्रिय!" else "App Updated to v2.2.0!",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = currentTheme.primaryColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = currentTheme.primaryColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isHindi) "नवीनतम रिलीज • Build #3" else "Latest Release • Build #3",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primaryColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isHindi) "इस नए अपडेट में जोड़े गए प्रमुख फीचर्स:" else "What's New in this Update:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val features = listOf(
                        "🔑 सुपर एडमिन मास्टर पासवर्ड परिवर्तन सुविधा",
                        "🛑 टोकन रद्दीकरण एवं स्थायी विलोपन (सुरक्षा डायलॉग सहित)",
                        "📊 दैनिक टोकन कोटा सीमा एवं स्वतः पंजीकरण लॉक",
                        "📑 एक्सेल / CSV डेटा एक्सपोर्ट (UTF-8 समर्थित)",
                        "🎨 आश्रम द्वारा निर्धारित भक्त UI लेआउट प्रवर्तन",
                        "📍 कस्टम गाँव व शहर सड़क दूरी प्रबंधक",
                        "💾 1-क्लिक सम्पूर्ण डेटाबेस बैकअप एवं रिस्टोर (JSON)",
                        "🌐 क्लाउड डेटा सिंक एवं बैकअप व्यवस्था"
                    )

                    features.forEach { feat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("• ", color = currentTheme.primaryColor, fontWeight = FontWeight.Bold)
                            Text(feat, fontSize = 12.sp, color = TextPrimaryDark, lineHeight = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { showWhatsNewDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            if (isHindi) "🙏 जय श्री बालाजी (जारी रखें)" else "Proceed",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    iconBadge: String,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    isPopular: Boolean = false,
    isCompact: Boolean = false,
    onClick: () -> Unit
) {
    val cornerRadius = if (isCompact) 13.dp else 18.dp
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompact) 2.5.dp else 4.dp, pressedElevation = 1.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.22f)),
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isCompact) 10.dp else 14.dp, vertical = if (isCompact) 9.dp else 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(if (isCompact) 9.dp else 12.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f)),
                    modifier = Modifier.size(if (isCompact) 34.dp else 44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = iconBadge, fontSize = if (isCompact) 17.sp else 22.sp)
                    }
                }

                if (isPopular) {
                    Surface(
                        shape = RoundedCornerShape(if (isCompact) 6.dp else 8.dp),
                        color = GoldSecondary.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, GoldDark)
                    ) {
                        Text(
                            text = "★ मुख्य",
                            fontSize = if (isCompact) 8.5.sp else 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SaffronDark,
                            modifier = Modifier.padding(horizontal = if (isCompact) 4.dp else 6.dp, vertical = if (isCompact) 1.dp else 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 12.dp))

            Text(
                text = title,
                fontSize = if (isCompact) 13.sp else 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaroonAccent,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(if (isCompact) 1.5.dp else 3.dp))
            Text(
                text = subtitle,
                fontSize = if (isCompact) 10.sp else 11.5.sp,
                color = TextSecondaryDark,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SevadarCard(sevadar: SevadarProfile, isHindi: Boolean) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        border = BorderStroke(1.dp, Color(0xFFFFCC80)),
        modifier = Modifier.width(220.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SacredAvatar(
                    photoUri = sevadar.photoUri,
                    fallbackText = sevadar.name,
                    size = 50.dp,
                    primaryColor = SaffronPrimary,
                    borderColor = GoldDark
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = sevadar.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1
                    )
                    Text(
                        text = if (isHindi) sevadar.roleTitleHindi else sevadar.roleTitleEnglish,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaroonPrimary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (sevadar.dutyHindi.isNotBlank()) {
                Text(
                    text = if (isHindi) sevadar.dutyHindi else sevadar.dutyEnglish,
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    lineHeight = 14.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // 1-Click Direct Phone Dialer Button
            Surface(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${sevadar.phoneNumber.trim()}"))
                        context.startActivity(intent)
                    } catch (e: Exception) {}
                },
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF81C784)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("📞 ", fontSize = 12.sp)
                    Text(
                        text = sevadar.phoneNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(कॉल)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
fun EventCard(title: String, subtitle: String, icon: String, badge: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = icon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonAccent
                    )
                }
                Surface(
                    color = SaffronLight.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badge,
                        color = SaffronDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondaryDark,
                lineHeight = 17.sp
            )
        }
    }
}


@Composable
fun RenderClassicSection(
    sectionId: String,
    sectionConfig: UiSectionConfig? = null,
    settings: AshramSettings,
    isHindi: Boolean,
    currentTheme: SacredTheme,
    activeSevadars: List<Admin>,
    sevadarProfiles: List<SevadarProfile> = emptyList(),
    dynamicEvents: List<AshramEvent>,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToYatra: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToParchas: () -> Unit = {},
    onNavigateToLiveDarbar: () -> Unit = {},
    onNavigateToDharamshala: () -> Unit = {},
    context: Context,
    isCompact: Boolean = false
) {
    // Custom announcement / guideline banner customized by Super Admin
    if (sectionConfig != null && (sectionConfig.customContentHindi.isNotBlank() || sectionConfig.customSubtitleHindi.isNotBlank())) {
        val subtitleText = if (isHindi) sectionConfig.customSubtitleHindi.ifEmpty { sectionConfig.customSubtitleEnglish } else sectionConfig.customSubtitleEnglish.ifEmpty { sectionConfig.customSubtitleHindi }
        val contentText = if (isHindi) sectionConfig.customContentHindi.ifEmpty { sectionConfig.customContentEnglish } else sectionConfig.customContentEnglish.ifEmpty { sectionConfig.customContentHindi }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9EE)),
            shape = RoundedCornerShape(if (isCompact) 8.dp else 12.dp),
            border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = if (isCompact) 4.dp else 6.dp)
        ) {
            Column(modifier = Modifier.padding(if (isCompact) 8.dp else 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sectionConfig.icon, fontSize = if (isCompact) 14.sp else 16.sp)
                    Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 6.dp))
                    Text(
                        text = if (isHindi) sectionConfig.titleHindi else sectionConfig.titleEnglish,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompact) 11.5.sp else 13.sp,
                        color = MaroonPrimary
                    )
                    if (subtitleText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 6.dp))
                        Text(
                            text = "• $subtitleText",
                            fontSize = if (isCompact) 9.5.sp else 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }
                if (contentText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = contentText,
                        fontSize = if (isCompact) 10.5.sp else 12.sp,
                        color = Color(0xFF3E2723),
                        lineHeight = if (isCompact) 14.sp else 16.sp
                    )
                }
            }
        }
    }

    when (sectionId) {
        UiSectionConfig.ID_GURUJI_BANNER -> {
            // GRAND ROYAL ASHRAM HEADER CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = currentTheme.primaryColor),
                shape = currentTheme.cardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isCompact) 4.dp else 6.dp),
                border = BorderStroke(currentTheme.cardBorderWidth + 0.5.dp, currentTheme.secondaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    currentTheme.headerGradientStart,
                                    currentTheme.headerGradientEnd,
                                    Color(0xFF28000C)
                                )
                            )
                        )
                        .padding(if (isCompact) 11.dp else 18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCompact) 56.dp else 76.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                GoldLight,
                                                currentTheme.secondaryColor
                                            )
                                        )
                                    )
                                    .padding(if (isCompact) 2.dp else 3.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = "Divine Ashram Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(if (isCompact) 6.dp else 8.dp),
                                    color = Color.White.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "🚩 ॥ श्री हनुमते नमः ॥",
                                        fontSize = if (isCompact) 9.5.sp else 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldLight,
                                        modifier = Modifier.padding(horizontal = if (isCompact) 6.dp else 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))
                                Text(
                                    text = if (isHindi) "श्री बालाजी कृपा धाम" else "Shri Balaji Kripa Dham",
                                    fontSize = if (isCompact) 17.5.sp else 21.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = currentTheme.accentGold,
                                    letterSpacing = 0.3.sp
                                )
                                Text(
                                    text = if (isHindi) "डूँगरा जाट, बुलन्दशहर (उ.प्र.)" else "Dungra Jaat, Bulandshahr (U.P.)",
                                    fontSize = if (isCompact) 11.5.sp else 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = if (isHindi) "परम पूज्य गुरुजी तेजवीर सिंह जी" else "Param Pujya Guruji Tejveer Singh Ji",
                                    fontSize = if (isCompact) 10.5.sp else 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = currentTheme.secondaryColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

                        // Live Darbar status pill
                        Surface(
                            shape = RoundedCornerShape(if (isCompact) 8.dp else 12.dp),
                            color = Color.Black.copy(alpha = 0.30f),
                            border = BorderStroke(0.8.dp, currentTheme.secondaryColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = if (isCompact) 8.dp else 12.dp, vertical = if (isCompact) 4.dp else 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isCompact) 6.dp else 8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E676))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "आगामी दिव्य दरबार: प्रत्येक रविवार प्रातः 7:00 बजे" else "Next Holy Darbar: Sunday 7:00 AM",
                                        fontSize = if (isCompact) 10.sp else 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text("🪔", fontSize = if (isCompact) 12.sp else 14.sp)
                            }
                        }
                    }
                }
            }
        }

        UiSectionConfig.ID_EMERGENCY_NOTICE -> {
            // EMERGENCY BROADCAST TICKER
            if (settings.isEmergencyNoticeVisible && settings.emergencyNoticeText.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFD32F2F)),
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(if (isCompact) 8.dp else 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚨", fontSize = if (isCompact) 18.sp else 22.sp)
                        Spacer(modifier = Modifier.width(if (isCompact) 6.dp else 10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "आश्रम महत्वपूर्ण सूचना" else "Ashram Urgent Notice",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isCompact) 11.sp else 12.sp,
                                color = Color(0xFFC62828)
                            )
                            Text(
                                text = settings.emergencyNoticeText,
                                fontSize = if (isCompact) 11.sp else 12.5.sp,
                                color = TextPrimaryDark,
                                lineHeight = if (isCompact) 14.sp else 17.sp
                            )
                        }
                    }
                }
            }
        }

        UiSectionConfig.ID_FREE_TREATMENT_BOX -> {
            // 100% FREE TREATMENT CERTIFIED TRUST SEAL
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(if (isCompact) 14.dp else 20.dp),
                elevation = CardDefaults.cardElevation(if (isCompact) 3.dp else 5.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFFB300)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(if (isCompact) 10.dp else 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(if (isCompact) 10.dp else 14.dp),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.size(if (isCompact) 40.dp else 52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🕊️", fontSize = if (isCompact) 20.sp else 26.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = if (isHindi) "★ पूर्णतः निःशुल्क (100% FREE)" else "★ 100% FREE OF COST",
                                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isHindi) "आध्यात्मिक कष्ट निवारण सेवा" else "Spiritual Healing Service",
                                fontSize = if (isCompact) 14.sp else 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaroonAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 10.dp))

                    Text(
                        text = settings.freeDisclaimer.ifEmpty {
                            if (isHindi)
                                "यहाँ भूत-प्रेत व मानसिक समस्याओं का इलाज पूर्णतः निःशुल्क किया जाता है। कोई पैसा नहीं लिया जाता, केवल भगवान की पूजा-पाठ और नियम बताए जाते हैं।"
                            else
                                "Treatment for mental afflictions and spiritual disturbances is completely FREE. No money is charged; only divine prayers and spiritual disciplines are prescribed."
                        },
                        fontSize = if (isCompact) 11.sp else 12.5.sp,
                        color = TextPrimaryDark,
                        lineHeight = if (isCompact) 15.sp else 17.5.sp
                    )
                }
            }
        }

        UiSectionConfig.ID_TOKEN_COUNTDOWN -> {
            // Pre-Scheduled Token Opening Countdown Banner
            if (settings.isTokenServiceEnabled && settings.scheduledTokenOpenTimestamp > System.currentTimeMillis()) {
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                val scheduledTimeStr = sdf.format(java.util.Date(settings.scheduledTokenOpenTimestamp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    shape = RoundedCornerShape(if (isCompact) 12.dp else 18.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFFFB300)),
                    elevation = CardDefaults.cardElevation(if (isCompact) 2.dp else 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(if (isCompact) 10.dp else 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏳", fontSize = if (isCompact) 22.sp else 28.sp)
                        Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
                        Column {
                            Text(
                                text = if (isHindi) "रविवार टोकन पंजीकरण पूर्व-निर्धारित है" else "Token Registration Scheduled",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isCompact) 12.5.sp else 14.sp,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isHindi)
                                    "टोकन खुलने का समय: $scheduledTimeStr\n(उस समय यह स्वतः खुल जाएगा)"
                                else
                                    "Opens automatically on: $scheduledTimeStr",
                                fontSize = if (isCompact) 10.5.sp else 12.sp,
                                color = TextPrimaryDark
                            )
                        }
                    }
                }
            }
        }

        UiSectionConfig.ID_SMART_FACE_TOKEN -> {
            // Smart Face Recognition 1-Second Token Banner
            if (settings.isTokenServiceEnabled) {
                val isBeforeSchedule = settings.scheduledTokenOpenTimestamp > System.currentTimeMillis()
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBeforeSchedule) Color(0xFF422018) else Color(0xFF5C001E)
                    ),
                    shape = RoundedCornerShape(if (isCompact) 14.dp else 20.dp),
                    elevation = CardDefaults.cardElevation(if (isCompact) 4.dp else 6.dp),
                    border = BorderStroke(1.5.dp, GoldSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        if (isBeforeSchedule) Color(0xFF422018) else Color(0xFF5C001E),
                                        if (isBeforeSchedule) Color(0xFF2D1610) else Color(0xFF800028)
                                    )
                                )
                            )
                            .padding(if (isCompact) 10.dp else 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(if (isCompact) 10.dp else 14.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, GoldLight.copy(alpha = 0.5f)),
                                    modifier = Modifier.size(if (isCompact) 38.dp else 50.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(if (isBeforeSchedule) "⏳" else "⚡", fontSize = if (isCompact) 19.sp else 24.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isHindi) "स्मार्ट चेहरा टोकन" else "Smart Face Token",
                                            color = GoldLight,
                                            fontSize = if (isCompact) 13.5.sp else 15.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(5.dp),
                                            color = SaffronPrimary
                                        ) {
                                            Text(
                                                text = "< 1s",
                                                color = Color.White,
                                                fontSize = if (isCompact) 8.sp else 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = if (isBeforeSchedule)
                                            (if (isHindi) "पंजीकरण पूर्व-निर्धारित समय पर खुलेगा" else "Scheduled to open at set time")
                                        else
                                            (if (isHindi) "दाढ़ी/चश्मा अप्रभावित • 1-क्लिक पुष्टि" else "AI facial match • Instant confirmation"),
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = if (isCompact) 10.sp else 11.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = onNavigateToFaceToken,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBeforeSchedule) Color.White.copy(alpha = 0.2f) else GoldSecondary,
                                    contentColor = if (isBeforeSchedule) Color.White else Color(0xFF4A0017)
                                ),
                                shape = RoundedCornerShape(if (isCompact) 10.dp else 14.dp),
                                elevation = ButtonDefaults.buttonElevation(if (isCompact) 2.dp else 4.dp),
                                contentPadding = PaddingValues(horizontal = if (isCompact) 10.dp else 14.dp, vertical = if (isCompact) 5.dp else 8.dp)
                            ) {
                                Text(
                                    text = if (isBeforeSchedule) (if (isHindi) "देखें" else "View") else (if (isHindi) "स्कैन करें" else "Scan"),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if (isCompact) 11.5.sp else 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        UiSectionConfig.ID_QUICK_SERVICES -> {
            // Quick Access Action Tiles (Responsive 2-Column Grid)
            val tileSpacing = if (isCompact) 6.dp else 10.dp
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isHindi) "मुख्य सेवाएं व विकल्प" else "Quick Access Services",
                        fontSize = if (isCompact) 14.5.sp else 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaroonAccent
                    )
                    Text(
                        text = if (isHindi) "त्वरित सेवाएं" else "Quick Services",
                        fontSize = if (isCompact) 10.sp else 11.sp,
                        color = TextSecondaryDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(tileSpacing))

                if (settings.isYatraServiceEnabled) {
                    // Row 1: Sunday Token + Balaji Yatra
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ActionTile(
                            title = if (isHindi) "रविवार टोकन" else "Sunday Token",
                            subtitle = if (settings.isTokenServiceEnabled) (if (isHindi) "दरबार कतार नंबर" else "Live Queue & Pass") else (if (isHindi) "पंजीकरण स्थगित" else "Paused by Admin"),
                            iconBadge = if (settings.isTokenServiceEnabled) "🏷️" else "🔒",
                            badgeColor = if (settings.isTokenServiceEnabled) SaffronPrimary else Color.Gray,
                            isPopular = settings.isTokenServiceEnabled,
                            isCompact = isCompact,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToToken
                        )
                        Spacer(modifier = Modifier.width(tileSpacing))
                        ActionTile(
                            title = if (isHindi) "बालाजी यात्रा" else "Balaji Yatra",
                            subtitle = if (isHindi) "बस सीट बुकिंग" else "Bus Seat Booking",
                            iconBadge = "🚌",
                            badgeColor = GoldDark,
                            isCompact = isCompact,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToYatra
                        )
                    }
                    Spacer(modifier = Modifier.height(tileSpacing))
                    // Row 2: Sacred Parchas + Live Darbar (2 columns side-by-side)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ActionTile(
                            title = if (isHindi) "📜 आश्रम पर्चे" else "📜 Sacred Parchas",
                            subtitle = if (isHindi) "हवन, उतारा व PDF" else "Hawan, Utara & PDF",
                            iconBadge = "📜",
                            badgeColor = Color(0xFF6A1B9A),
                            isPopular = true,
                            isCompact = isCompact,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToParchas
                        )
                        Spacer(modifier = Modifier.width(tileSpacing))
                        ActionTile(
                            title = if (isHindi) "🔴 लाइव दर्शन" else "🔴 Live Darbar",
                            subtitle = if (isHindi) "लाइव स्ट्रीम व भजन" else "Live Stream & Audio",
                            iconBadge = "🔴",
                            badgeColor = Color(0xFFD32F2F),
                            isPopular = true,
                            isCompact = isCompact,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToLiveDarbar
                        )
                    }
                    Spacer(modifier = Modifier.height(tileSpacing))
                    // Row 3: Ashram Info + Sevadar Portal
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ActionTile(
                            title = if (isHindi) "आश्रम परिचय" else "Ashram Info",
                            subtitle = if (isHindi) "नियम व लोकेशन" else "Rules & GPS Route",
                            iconBadge = "ℹ️",
                            badgeColor = Color(0xFF3949AB),
                            isCompact = isCompact,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToInfo
                        )
                        Spacer(modifier = Modifier.width(tileSpacing))
                        ActionTile(
                            title = if (isHindi) "सेवादार पोर्टल" else "Admin Portal",
                            subtitle = if (isHindi) "व्यवस्थापक प्रवेश" else "Sevadar & Admin",
                            iconBadge = "🛡️",
                            badgeColor = MaroonAccent,
                            isCompact = isCompact,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAdmin
                        )
                    }
                    Spacer(modifier = Modifier.height(tileSpacing))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ActionTile(
                            title = if (isHindi) "धर्मशाला व आवास" else "Dharamshala",
                            subtitle = if (isHindi) "कमरा व बेड आरक्षण (AC / Non-AC)" else "Room & Bed Booking",
                            iconBadge = "🏨",
                            badgeColor = Color(0xFF00796B),
                            isPopular = true,
                            isCompact = isCompact,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onNavigateToDharamshala
                        )
                    }
                    Spacer(modifier = Modifier.height(tileSpacing))
                    Surface(
                        onClick = {
                            val webUrl = settings.officialWebsiteUrl.ifEmpty { "https://shribalajikripadham.online" }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(if (isCompact) 10.dp else 14.dp),
                        border = BorderStroke(1.dp, Color(0xFF90CAF9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = if (isCompact) 10.dp else 14.dp, vertical = if (isCompact) 6.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌐", fontSize = if (isCompact) 20.sp else 24.sp)
                                Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 10.dp))
                                Column {
                                    Text(
                                        text = if (isHindi) "आश्रम की आधिकारिक वेबसाइट" else "Official Ashram Website",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompact) 12.sp else 14.sp,
                                        color = Color(0xFF0D47A1)
                                    )
                                    Text(
                                        text = "shribalajikripadham.online • लाइव टोकन व दर्शन",
                                        fontSize = if (isCompact) 9.5.sp else 11.sp,
                                        color = Color(0xFF1976D2)
                                    )
                                }
                            }
                            Text("खोलें ➔", fontWeight = FontWeight.Bold, fontSize = if (isCompact) 11.sp else 12.sp, color = Color(0xFF0D47A1))
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ActionTile(
                            title = if (isHindi) "रविवार टोकन" else "Sunday Token",
                            subtitle = if (settings.isTokenServiceEnabled) (if (isHindi) "दरबार कतार नंबर" else "Live Queue & Pass") else (if (isHindi) "पंजीकरण स्थगित" else "Paused by Admin"),
                            iconBadge = if (settings.isTokenServiceEnabled) "🏷️" else "🔒",
                            badgeColor = if (settings.isTokenServiceEnabled) SaffronPrimary else Color.Gray,
                            isPopular = settings.isTokenServiceEnabled,
                            isCompact = isCompact,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToToken
                        )
                        Spacer(modifier = Modifier.width(tileSpacing))
                        ActionTile(
                            title = if (isHindi) "🔴 लाइव दर्शन" else "🔴 Live Darbar",
                            subtitle = if (isHindi) "लाइव स्ट्रीम व भजन" else "Live Stream & Audio",
                            iconBadge = "🔴",
                            badgeColor = Color(0xFFD32F2F),
                            isPopular = true,
                            isCompact = isCompact,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToLiveDarbar
                        )
                    }
                    Spacer(modifier = Modifier.height(tileSpacing))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ActionTile(
                            title = if (isHindi) "📜 आश्रम पर्चे" else "📜 Sacred Parchas",
                            subtitle = if (isHindi) "हवन, उतारा व PDF" else "Hawan, Utara & PDF",
                            iconBadge = "📜",
                            badgeColor = Color(0xFF6A1B9A),
                            isPopular = true,
                            isCompact = isCompact,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToParchas
                        )
                        Spacer(modifier = Modifier.width(tileSpacing))
                        ActionTile(
                            title = if (isHindi) "आश्रम परिचय" else "Ashram Info",
                            subtitle = if (isHindi) "नियम व लोकेशन" else "Rules & GPS Route",
                            iconBadge = "ℹ️",
                            badgeColor = Color(0xFF3949AB),
                            isCompact = isCompact,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToInfo
                        )
                    }
                    Spacer(modifier = Modifier.height(tileSpacing))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ActionTile(
                            title = if (isHindi) "सेवादार पोर्टल" else "Admin Portal",
                            subtitle = if (isHindi) "व्यवस्थापक प्रवेश" else "Sevadar & Admin",
                            iconBadge = "🛡️",
                            badgeColor = MaroonAccent,
                            isCompact = isCompact,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAdmin
                        )
                        Spacer(modifier = Modifier.width(tileSpacing))
                        ActionTile(
                            title = if (isHindi) "धर्मशाला व आवास" else "Dharamshala",
                            subtitle = if (isHindi) "कमरा व बेड आरक्षण" else "Room & Bed Booking",
                            iconBadge = "🏨",
                            badgeColor = Color(0xFF00796B),
                            isPopular = true,
                            isCompact = isCompact,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToDharamshala
                        )
                    }
                }
            }
        }

        UiSectionConfig.ID_AARTI_TIMINGS -> {
            // Dedicated Aarti & Darbar Timings Card
            if (settings.isAartiTimingsVisible) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    border = BorderStroke(1.dp, GoldSecondary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF8E1),
                            border = BorderStroke(1.dp, GoldSecondary.copy(alpha = 0.5f)),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("⏰", fontSize = 24.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (isHindi) "आरती व दरबार समय सारणी" else "Aarti & Darbar Timings",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaroonAccent
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = settings.darbarTimings.ifEmpty { "प्रत्येक रविवार प्रातः 7:00 बजे से (Every Sunday from 7:00 AM)" },
                                fontSize = 12.5.sp,
                                color = TextPrimaryDark,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }

        UiSectionConfig.ID_DARBAR_STATUS -> {
            // Guruji Profile & Darbar Details Card
            if (settings.isGurujiInfoVisible) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(5.dp),
                    border = BorderStroke(1.dp, currentTheme.secondaryColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SacredAvatar(
                                photoUri = settings.gurujiPhotoUri,
                                fallbackText = "गुरुजी",
                                size = 92.dp,
                                primaryColor = currentTheme.primaryColor,
                                borderColor = currentTheme.secondaryColor
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = settings.gurujiName.ifEmpty {
                                        if (isHindi) "परम पूज्य गुरुजी तेजवीर सिंह जी" else "Param Pujya Guruji Tejveer Singh Ji"
                                    },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaroonAccent
                                )
                                Text(
                                    text = if (isHindi) "आश्रम प्रमुख एवं मार्गदर्शक" else "Ashram Head & Spiritual Guide",
                                    fontSize = 13.sp,
                                    color = SaffronDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isHindi)
                                "गुरुजी के पावन सानिध्य में ${settings.darbarTimings} में दिव्य दरबार लगता है। गुरुजी का एकमात्र संकल्प है कि पीड़ित मानवता को बिना किसी शुल्क के ईश्वर भक्ति एवं पवित्र नियमों के माध्यम से मानसिक एवं आध्यात्मिक शांति प्रदान की जाए।"
                            else
                                "Under the holy guidance of ${settings.gurujiName}, Darbar is held (${settings.darbarTimings}). Guruji's mission is to relieve suffering souls without any charges through divine prayers and bhakti.",
                            fontSize = 13.sp,
                            color = TextSecondaryDark,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }

        UiSectionConfig.ID_SEVADAR_TEAM -> {
            // Ashram Sevadar Showcase & Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "🚩 आश्रम के समर्पित सेवादार" else "Dedicated Ashram Sevadars",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = currentTheme.primaryColor
                    )
                    Text(
                        text = if (isHindi) "📞 1-क्लिक कॉल" else "📞 1-Tap Call",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                val profilesToShow = if (sevadarProfiles.isNotEmpty()) sevadarProfiles else SevadarProfile.defaultProfiles()
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(profilesToShow) { sProfile ->
                        SevadarCard(sevadar = sProfile, isHindi = isHindi)
                    }
                }
            }
        }

        UiSectionConfig.ID_DYNAMIC_EVENTS -> {
            // Annual Events Section
            if (settings.isEventsVisible) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isHindi) "वार्षिक धार्मिक उत्सव व कार्यक्रम" else "Annual Sacred Programs & Festivals",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaroonAccent
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (dynamicEvents.isNotEmpty()) {
                        dynamicEvents.forEach { ev ->
                            EventCard(
                                title = if (isHindi) ev.titleHindi else ev.titleEnglish,
                                subtitle = if (isHindi) ev.detailsHindi else ev.detailsEnglish,
                                icon = "🚩",
                                badge = if (isHindi) ev.dateDescriptionHindi else ev.dateDescriptionEnglish
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    } else {
                        EventCard(
                            title = if (isHindi) "1. गुरु पूर्णिमा महोत्सव" else "1. Guru Purnima Mahotsav",
                            subtitle = if (isHindi)
                                "गुरु पूजा, अखंड संकीर्तन एवं विशाल महाप्रसाद भंडारा। जो श्रद्धालु स्वेच्छा से अपनी श्रद्धा अनुसार सहयोग करना चाहें कर सकते हैं, कोई दबाव नहीं होता।"
                            else
                                "Guru Pooja, Akhand Kirtan, and Grand Feast (Bhandara). Voluntary contribution welcomed.",
                            icon = "🪔",
                            badge = if (isHindi) "वार्षिक भंडारा" else "Annual Bhandara"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        EventCard(
                            title = if (isHindi) "2. हनुमान जयंती महोत्सव" else "2. Hanuman Jayanti Utsav",
                            subtitle = if (isHindi)
                                "श्री हनुमान चालीसा एवं सुंदरकांड पाठ, विशेष दरबार, महाआरती और जन-कल्याण हेतु महाभोज/भंडारा।"
                            else
                                "Sundarkand Path, Special Darbar, Maha Aarti, and Community Feast.",
                            icon = "🚩",
                            badge = if (isHindi) "महाउत्सव" else "Grand Celebration"
                        )
                    }
                }
            }
        }

        UiSectionConfig.ID_SOCIAL_MEDIA_HUB -> {
            // Social Media & App Share Hub
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(5.dp),
                border = BorderStroke(1.5.dp, currentTheme.secondaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = currentTheme.primaryColor.copy(alpha = 0.12f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🌐", fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isHindi) "आश्रम से सोशल मीडिया पर जुड़ें" else "Connect with Ashram",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = currentTheme.primaryColor
                            )
                            Text(
                                text = if (isHindi) "लाइव दर्शन, आरती, सूचनाएं व ऍप शेयर करें" else "Live Darshan, Aarti, Updates & App Share",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // YouTube
                        Button(
                            onClick = {
                                openSocialMediaLink(
                                    context = context,
                                    rawUrl = settings.youtubeChannelUrl,
                                    defaultUrl = "https://www.youtube.com/@ShriBalajiKripaDham",
                                    isWhatsApp = false,
                                    errorMessage = if (isHindi) "यूट्यूब चैनल खोलने में असमर्थ" else "Unable to open YouTube"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("▶ YouTube", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // WhatsApp
                        Button(
                            onClick = {
                                openSocialMediaLink(
                                    context = context,
                                    rawUrl = settings.whatsappGroupUrl,
                                    defaultUrl = "https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4",
                                    isWhatsApp = true,
                                    errorMessage = if (isHindi) "व्हाट्सएप्प लिंक या ऐप खोलने में असमर्थ" else "Unable to open WhatsApp"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("💬 WhatsApp", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Facebook
                        Button(
                            onClick = {
                                openSocialMediaLink(
                                    context = context,
                                    rawUrl = settings.facebookPageUrl,
                                    defaultUrl = "https://www.facebook.com/ShriBalajiKripaDham",
                                    isWhatsApp = false,
                                    errorMessage = if (isHindi) "फेसबुक पेज खोलने में असमर्थ" else "Unable to open Facebook"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("f Facebook", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Instagram
                        Button(
                            onClick = {
                                openSocialMediaLink(
                                    context = context,
                                    rawUrl = settings.instagramUrl,
                                    defaultUrl = "https://www.instagram.com/shribalajikripadham",
                                    isWhatsApp = false,
                                    errorMessage = if (isHindi) "इंस्टाग्राम पेज खोलने में असमर्थ" else "Unable to open Instagram"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE4405F)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("📸 Insta", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Share App Button
                    Button(
                        onClick = {
                            val shareUrl = settings.appShareUrl.ifBlank { "https://shribalajikripadham.org/app" }
                            val shareMessage = if (isHindi) {
                                """
                                🚩 श्री बालाजी कृपा धाम, डूँगरा जाट (बुलन्दशहर, उ.प्र.) 🚩
                                
                                परम पूज्य गुरुजी तेजवीर सिंह जी के पावन सानिध्य में:
                                ✨ भूत-प्रेत व मानसिक समस्याओं का 100% निःशुल्क (FREE) इलाज!
                                ✨ प्रत्येक रविवार दिव्य दरबार एवं ऑनलाइन टोकन सुविधा
                                ✨ आरती, दर्शन व आश्रम की सभी सेवाओं की अधिकृत जानकारी
                                
                                📲 अभी श्री बालाजी कृपा धाम ऍप डाउनलोड करें व परिजनों को शेयर करें:
                                $shareUrl
                                
                                ॥ जय श्री बालाजी महाराज ॥
                                """.trimIndent()
                            } else {
                                """
                                🚩 Shri Balaji Kripa Dham, Dungra Jaat (Bulandshahr, U.P.) 🚩
                                
                                Under the divine grace of Param Pujya Guruji Tejveer Singh Ji:
                                ✨ 100% FREE spiritual healing for afflictions & distress!
                                ✨ Online Sunday Darbar Token & Queue Management
                                ✨ Live Aarti, Darshan & Ashram services
                                
                                📲 Download Shri Balaji Kripa Dham Official App:
                                $shareUrl
                                
                                || Jai Shri Balaji Maharaj ||
                                """.trimIndent()
                            }

                            shareAppContent(
                                context = context,
                                shareMessage = shareMessage,
                                title = if (isHindi) "श्री बालाजी कृपा धाम ऍप शेयर करें" else "Share Ashram App"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📤", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "ऍप को व्हाट्सएप्प व अन्य सोशल मीडिया पर शेयर करें" else "Share App on WhatsApp & Social Media",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        UiSectionConfig.ID_CONTACT_FOOTER -> {
            // Official App Branding & Ashram Contact / Location Footer
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚩 श्री बालाजी कृपा धाम 🚩",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaroonAccent
                    )
                    Text(
                        text = "ग्राम डूँगरा जाट, जिला बुलन्दशहर (उ०प्र०)",
                        fontSize = 12.5.sp,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFEEEEEE))
                    Text(
                        text = "Developer: Ankit Chaudhary",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaffronDark
                    )
                    Text(
                        text = "Anti Gravity • High-Performance Native Android Engineering",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }
        }
    }
}

@Composable
fun AshramHomeHeroBanner(
    bannerPhotoUri: String,
    title: String,
    subtitle: String,
    actionUrl: String,
    primaryColor: Color,
    secondaryColor: Color,
    context: android.content.Context
) {
    var bmp by remember(bannerPhotoUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(bannerPhotoUri) {
        if (bannerPhotoUri.isNotBlank()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                bmp = com.example.shribalajikripadham.util.DevoteePhotoHelper.loadBitmap(context, bannerPhotoUri)
            }
        }
    }
    val safeBmp = remember(bmp) {
        bmp?.let { com.example.shribalajikripadham.util.DevoteePhotoHelper.toSoftwareBitmap(it) }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(5.dp),
        border = BorderStroke(1.2.dp, secondaryColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = actionUrl.isNotBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {}
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (safeBmp != null) {
                Image(
                    bitmap = safeBmp.asImageBitmap(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(0f, -0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = title.ifEmpty { "🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट" },
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryColor
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.5.sp,
                        color = Color.DarkGray,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DevoteeSponsorAdBanner(
    photoUri: String,
    title: String,
    description: String,
    targetUrl: String,
    primaryColor: Color,
    secondaryColor: Color,
    context: android.content.Context
) {
    var bmp by remember(photoUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(photoUri) {
        if (photoUri.isNotBlank()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                bmp = com.example.shribalajikripadham.util.DevoteePhotoHelper.loadBitmap(context, photoUri)
            }
        }
    }
    val safeBmp = remember(bmp) {
        bmp?.let { com.example.shribalajikripadham.util.DevoteePhotoHelper.toSoftwareBitmap(it) }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF0)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        border = BorderStroke(1.2.dp, secondaryColor.copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = targetUrl.isNotBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {}
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF3E0))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📢", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "धर्मार्थ सहयोग एवं प्रायोजक (Ashram Sponsorship)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
            if (safeBmp != null) {
                Image(
                    bitmap = safeBmp.asImageBitmap(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title.ifEmpty { "आश्रम सेवा व गौशाला सहयोग" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaroonAccent
                )
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
                if (targetUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🔗 अधिक जानकारी देखें ➔",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun DevoteeSmartQueueEtaCard(
    queueEta: com.example.shribalajikripadham.util.QueueEtaResult,
    runningTokenNumber: Int,
    devoteeToken: Int,
    isHindi: Boolean,
    isCompact: Boolean,
    onNavigateToToken: () -> Unit
) {
    val isServingMe = queueEta.isNowServing
    val isWaiting = queueEta.isWaiting
    val hasPassed = queueEta.hasPassed

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                isServingMe -> Color(0xFFE8F5E9)
                hasPassed -> Color(0xFFFFEBEE)
                isWaiting -> Color(0xFFFFF8E1)
                else -> Color(0xFFFFF9EE)
            }
        ),
        shape = RoundedCornerShape(if (isCompact) 14.dp else 18.dp),
        border = BorderStroke(
            1.5.dp,
            when {
                isServingMe -> Color(0xFF2E7D32)
                hasPassed -> Color(0xFFC62828)
                isWaiting -> Color(0xFFFFB300)
                else -> SaffronPrimary.copy(alpha = 0.7f)
            }
        ),
        elevation = CardDefaults.cardElevation(if (isCompact) 3.dp else 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToToken() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 10.dp else 14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            isServingMe -> "🔔"
                            hasPassed -> "⚠️"
                            isWaiting -> "⏳"
                            else -> "🚩"
                        },
                        fontSize = if (isCompact) 18.sp else 22.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isServingMe -> if (isHindi) "आपका दर्शन समय आ चुका है!" else "Your Turn Now!"
                            hasPassed -> if (isHindi) "टोकन निकल चुका है" else "Token Number Passed"
                            isWaiting -> if (isHindi) "स्मार्ट कतार व संभावित समय (Live ETA)" else "Smart Queue & Live ETA"
                            else -> if (isHindi) "दरबार लाइव टोकन स्थिति" else "Live Darbar Queue Status"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isCompact) 13.sp else 15.sp,
                        color = when {
                            isServingMe -> Color(0xFF1B5E20)
                            hasPassed -> Color(0xFFB71C1C)
                            isWaiting -> Color(0xFFE65100)
                            else -> MaroonPrimary
                        }
                    )
                }

                Surface(
                    color = when {
                        isServingMe -> Color(0xFF2E7D32)
                        hasPassed -> Color(0xFFC62828)
                        isWaiting -> Color(0xFFF57C00)
                        else -> MaroonPrimary
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isHindi) "लाइव अपडेट" else "LIVE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Token Numbers Comparison Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Currently Serving Box
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD54F))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isHindi) "वर्तमान में सेवारत" else "Now Calling",
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (runningTokenNumber > 0) "#$runningTokenNumber" else "--",
                            fontSize = if (isCompact) 20.sp else 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFB71C1C)
                        )
                    }
                }

                // Devotee's Personal Token Box
                Surface(
                    modifier = Modifier.weight(1f),
                    color = if (devoteeToken > 0) Color(0xFFFFFDE7) else Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (devoteeToken > 0) Color(0xFFFFA000) else Color.LightGray)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isHindi) "आपका टोकन" else "Your Token",
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (devoteeToken > 0) "#$devoteeToken" else if (isHindi) "उपलब्ध नहीं" else "None",
                            fontSize = if (isCompact) 18.sp else 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (devoteeToken > 0) Color(0xFF1B5E20) else Color.DarkGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ETA Status Details
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isHindi) queueEta.statusTextHindi else queueEta.statusTextEnglish,
                        fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF263238),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = " ➔",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun DevoteePanchangQuickCard(
    isHindi: Boolean,
    onNavigateToPanchang: () -> Unit
) {
    val panchangData = remember { com.example.shribalajikripadham.panchang.VedicPanchangEngine.calculatePanchang() }
    val currentChog = panchangData.currentChoghadiya

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToPanchang() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🕉️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "दैनिक पंचांग व शुभ चौघड़िया" else "Daily Panchang & Choghadiya",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaroonPrimary
                    )
                }

                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, Color(0xFFA5D6A7))
                ) {
                    Text(
                        text = if (isHindi) "१०८% स्वतः अद्यतन" else "Auto-Updated",
                        fontSize = 10.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Date & Tithi summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${panchangData.dayOfWeekHindi} • ${panchangData.tithiHindi}",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A148C)
                )
                Text(
                    text = "नक्षत्र: ${panchangData.nakshatraHindi}",
                    fontSize = 11.5.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Quick Muhurat line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "⚠️ राहुकाल: ${panchangData.rahuKaalTime}",
                    fontSize = 11.sp,
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.SemiBold
                )
                if (currentChog != null) {
                    Text(
                        text = "सक्रिय: ${currentChog.nature.labelHindi.split(" ")[0]} 🟢",
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // View full button line
            Surface(
                color = AmberGold.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "आज का सम्पूर्ण पंचांग, चौघड़िया व आरती समय देखें" else "View Full Panchang, Choghadiya & Aarti Times",
                        fontSize = 11.5.sp,
                        color = MaroonPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("➔", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                }
            }
        }
    }
}

@Composable
fun DevoteeSacredGranthQuickCard(
    isHindi: Boolean,
    onNavigateToSacredGranth: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFD7CCC8)),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToSacredGranth() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📖", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "पावन ग्रंथ (सुंदरकाण्ड, बाहुक, हनुमानाष्टक)" else "Sacred Texts (Sundarkand, Bahuk)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaroonPrimary
                    )
                }

                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, Color(0xFFFFB74D))
                ) {
                    Text(
                        text = if (isHindi) "१०८% ऑफ़लाइन" else "100% Offline",
                        fontSize = 10.sp,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = if (isHindi)
                    "• सम्पूर्ण सुन्दरकाण्ड (६० दोहे अर्थ सहित)\n• श्री हनुमान बाहुक (४४ पद - समस्त रोग निवारण)\n• संकटमोचन हनुमानाष्टक (८ छंद भावार्थ सहित)"
                else
                    "• Complete Sundarkand (60 Dohas with meanings)\n• Shri Hanuman Bahuk (44 Verses - Disease Relief)\n• Sankatmochan Hanumanashtak (8 Stanzas)",
                fontSize = 12.sp,
                color = Color(0xFF424242),
                lineHeight = 17.sp
            )

            Surface(
                color = MaroonPrimary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "बड़े अक्षरों में पावन पाठ पढ़ें (ऑटो-स्क्रॉल सहित)" else "Read Sacred Texts (Large Text & Auto-Scroll)",
                        fontSize = 11.5.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text("➔", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                }
            }
        }
    }
}

@Composable
fun DevoteeSacredAartiBhaktiTab(
    isHindi: Boolean,
    currentTheme: SacredTheme,
    context: Context,
    isCompact: Boolean,
    onNavigateToLiveDarbar: () -> Unit,
    onNavigateToPanchang: () -> Unit,
    onNavigateToSacredGranth: () -> Unit,
    onOpenLyrics: (SacredTrack) -> Unit
) {
    val scope = rememberCoroutineScope()
    val isPlaying by BhajanAudioService.isPlaying.collectAsState()
    val currentTitle by BhajanAudioService.currentTitle.collectAsState()
    val currentTrackIndex by BhajanAudioService.currentTrackIndex.collectAsState()

    var cachedCount by remember { mutableIntStateOf(DevotionalAudioCacheManager.getCachedTrackCount(context)) }
    var isSyncing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cachedCount = DevotionalAudioCacheManager.getCachedTrackCount(context)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Grand Header Card with Offline Status
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            border = BorderStroke(1.5.dp, GoldSecondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SaffronPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("📿", fontSize = 24.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "नित्य सेवा व आरती संग्रह" else "Daily Aarti & Chalisa",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "१५ संपूर्ण पावन पाठ • बफर-मुक्त ध्वनि" else "15 Complete Tracks • Zero Buffering",
                                fontSize = 11.5.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9),
                        border = BorderStroke(1.dp, Color(0xFF81C784))
                    ) {
                        Text(
                            text = "🟢 $cachedCount/15 " + (if (isHindi) "ऑफ़लाइन" else "Offline"),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isHindi)
                        "⚡ सभी १५ पावन आरती व चालीसा आपके फोन में गुप्त आंतरिक बैकअप (.nomedia) में स्वतः सुरक्षित हैं। फोन की गैलरी भरे बिना और इंटरनेट धीमा होने पर भी ० सेकंड में बजेंगी।"
                    else
                        "⚡ All 15 sacred tracks are silently cached in hidden internal storage for 0ms offline playback with zero buffering.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF333333),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isSyncing = true
                            scope.launch {
                                DevotionalAudioCacheManager.startSilentBackgroundSync(context)
                                delay(2000)
                                cachedCount = DevotionalAudioCacheManager.getCachedTrackCount(context)
                                isSyncing = false
                                Toast.makeText(context, if (isHindi) "✅ ऑफ़लाइन बैकअप अद्यतन किया जा रहा है" else "Offline cache syncing...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSyncing) "🔄 सिंक जारी..." else "🔄 बैकअप सिंक",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonPrimary
                        )
                    }

                    Button(
                        onClick = onNavigateToLiveDarbar,
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🔴 " + (if (isHindi) "बड़ा प्लेयर खोलें ➔" else "Open Player ➔"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 2. Active Now Playing Bar (if playing)
        if (isPlaying || currentTitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, SaffronPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🔊", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = currentTitle.ifEmpty { "श्री बालाजी भजन" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaroonPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = if (isPlaying) (if (isHindi) "बज रहा है • 0s ऑफ़लाइन" else "Playing Offline") else (if (isHindi) "रुका हुआ है" else "Paused"),
                                fontSize = 10.5.sp,
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { BhajanAudioService.togglePlayPause(context) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(if (isPlaying) "⏸" else "▶", fontSize = 18.sp, color = MaroonPrimary)
                        }
                        IconButton(
                            onClick = { BhajanAudioService.stopPlayback(context) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("⏹", fontSize = 16.sp, color = Color.Red)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. List of All 15 Sacred Tracks
        Text(
            text = if (isHindi) "📜 संपूर्ण आरती व चालीसा संग्रह (१५ पावन पाठ)" else "All 15 Sacred Tracks & Lyrics",
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaroonAccent,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        SACRED_TRACKS.forEachIndexed { index, track ->
            val isThisPlaying = (currentTrackIndex == index && isPlaying)
            val isCached = remember(track.trackKey, cachedCount) {
                DevotionalAudioCacheManager.isTrackCached(context, track.trackKey)
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isThisPlaying) Color(0xFFFFF8E1) else Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    if (isThisPlaying) 1.5.dp else 1.dp,
                    if (isThisPlaying) GoldDark else Color(0xFFEEEEEE)
                ),
                elevation = CardDefaults.cardElevation(if (isThisPlaying) 4.dp else 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isThisPlaying) SaffronPrimary else Color(0xFFF5F5F5),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isThisPlaying) Color.White else MaroonPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isHindi) track.titleHindi else track.titleEnglish,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaroonPrimary
                                )
                                Text(
                                    text = track.subtitleHindi,
                                    fontSize = 10.5.sp,
                                    color = TextSecondaryDark,
                                    maxLines = 1
                                )
                            }
                        }

                        if (isCached) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = "🟢 0s Play",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Play Button
                        Button(
                            onClick = {
                                if (isThisPlaying) {
                                    BhajanAudioService.togglePlayPause(context)
                                } else {
                                    BhajanAudioService.playTrack(
                                        context = context,
                                        trackIndex = index,
                                        title = track.titleHindi,
                                        artist = "श्री बालाजी कृपा धाम",
                                        audioUrl = track.audioUrl,
                                        trackKey = track.trackKey
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isThisPlaying) Color(0xFF2E7D32) else currentTheme.primaryColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isThisPlaying) "⏸ रोकें" else "▶ बजाएं (${track.durationText})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Lyrics Button
                        OutlinedButton(
                            onClick = { onOpenLyrics(track) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "📖 संपूर्ण पाठ",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaroonPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. DevoteePanchangQuickCard
        DevoteePanchangQuickCard(isHindi = isHindi, onNavigateToPanchang = onNavigateToPanchang)
        Spacer(modifier = Modifier.height(12.dp))

        // 5. DevoteeSacredGranthQuickCard
        DevoteeSacredGranthQuickCard(isHindi = isHindi, onNavigateToSacredGranth = onNavigateToSacredGranth)
    }
}

@Composable
fun DevoteeDharamshalaYatraTab(
    isHindi: Boolean,
    currentTheme: SacredTheme,
    context: Context,
    isCompact: Boolean,
    onNavigateToDharamshala: () -> Unit,
    onNavigateToYatra: () -> Unit,
    onNavigateToYatraExpenses: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Dharamshala Room Booking Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            border = BorderStroke(1.2.dp, Color(0xFF00796B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE0F2F1),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🏨", fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHindi) "धर्मशाला व कमरा आरक्षण" else "Dharamshala Room Booking",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF004D40)
                        )
                        Text(
                            text = if (isHindi) "आश्रम में रात्रि विश्राम एवं आवास व्यवस्था" else "Clean Rooms & Rest Facilities",
                            fontSize = 11.5.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isHindi)
                        "• वातानुकूलित (AC) एवं नॉन-एसी सुविधायुक्त कमरे\n• स्वच्छ बिस्तर, २४ घंटे बिजली एवं स्वच्छ जल व्यवस्था\n• आश्रम महाप्रसाद एवं भोजनालय की उत्तम सुविधा\n• पारिवारिक एवं व्यक्तिगत कक्ष अग्रिम बुकिंग उपलब्ध"
                    else
                        "• AC and Non-AC clean rooms available\n• 24-hr electricity, clean water & bedding\n• Ashram Bhandara & Bhojanalaya facilities\n• Advance booking available for families and individuals",
                    fontSize = 12.sp,
                    color = Color(0xFF333333),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onNavigateToDharamshala,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 11.dp)
                ) {
                    Text(
                        text = if (isHindi) "🏨 धर्मशाला कमरा / बेड आरक्षित करें ➔" else "Book Dharamshala Room ➔",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Balaji Yatra & Bus Booking Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            border = BorderStroke(1.2.dp, GoldDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF8E1),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🚌", fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHindi) "श्री बालाजी यात्रा व बस सेवा" else "Balaji Yatra & Bus Booking",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaroonAccent
                        )
                        Text(
                            text = if (isHindi) "आश्रम हेतु सीधी बस व सीट आरक्षण" else "Direct Bus Service to Ashram",
                            fontSize = 11.5.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isHindi)
                        "प्रत्येक रविवार एवं विशेष पर्वों पर दिल्ली, बुलन्दशहर एवं आस-पास के क्षेत्रों से सीधी बस सेवा उपलब्ध है। अपनी सीट पहले से बुक करें।"
                    else
                        "Direct buses from Delhi, Bulandshahr and nearby areas on Sundays and festive occasions. Reserve your seat early.",
                    fontSize = 12.sp,
                    color = Color(0xFF333333),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToYatra,
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isHindi) "🚌 यात्रा सीट बुक करें" else "Book Bus Seat",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateToYatraExpenses,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isHindi) "💰 खर्च डायरी" else "Expense Diary",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonAccent
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Distance & GPS Route Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            border = BorderStroke(1.2.dp, Color(0xFF3949AB).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8EAF6),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📍", fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHindi) "आश्रम दूरी व सड़क मार्ग (GPS Route)" else "Ashram Route & Distance",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A237E)
                        )
                        Text(
                            text = "ग्राम डूँगरा जाट, बुलन्दशहर (उ.प्र.)",
                            fontSize = 11.5.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Major City Distances
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val cities = listOf(
                        "दिल्ली" to "85 KM",
                        "बुलन्दशहर" to "32 KM",
                        "मेरठ" to "78 KM",
                        "नोएडा" to "72 KM"
                    )
                    cities.forEach { (city, dist) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF5F5F5),
                            border = BorderStroke(0.8.dp, Color(0xFFE0E0E0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(city, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryDark)
                                Text(dist, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = MaroonPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        try {
                            val mapUri = Uri.parse("geo:28.4070,77.8498?q=Shri+Balaji+Kripa+Dham+Dungra+Jaat")
                            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=28.4070,77.8498"))
                            context.startActivity(webMapIntent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949AB)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 11.dp)
                ) {
                    Text(
                        text = if (isHindi) "🗺️ गूगल मैप्स पर लाइव रास्ता देखें ➔" else "Open in Google Maps ➔",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun DevoteeAshramAboutTab(
    isHindi: Boolean,
    currentTheme: SacredTheme,
    settings: AshramSettings,
    activeSevadars: List<Admin>,
    sevadarProfiles: List<SevadarProfile>,
    dynamicEvents: List<AshramEvent>,
    context: Context,
    isCompact: Boolean,
    onNavigateToParchas: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToYatra: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Digital Sacred Parchas Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            border = BorderStroke(1.2.dp, Color(0xFF6A1B9A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF3E5F5),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📜", fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHindi) "आश्रम के पावन पर्चे व नियम" else "Sacred Parchas & Rules",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4A148C)
                        )
                        Text(
                            text = if (isHindi) "हवन सामग्री, उतारा विधि व विशेष नियम" else "Hawan Samagri, Utara & Ritual Rules",
                            fontSize = 11.5.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isHindi)
                        "आश्रम द्वारा जारी किए गए सभी अधिकृत पर्चे, हवन विधि, उतारा की सामग्री एवं प्रेत बाधा निवारण के नियमों को मोबाइल पर देखें एवं PDF में डाउनलोड करें।"
                    else
                        "View and download official Ashram parchas, hawan guides, utara items, and spiritual rules in PDF format.",
                    fontSize = 12.sp,
                    color = Color(0xFF333333),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onNavigateToParchas,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 11.dp)
                ) {
                    Text(
                        text = if (isHindi) "📜 संपूर्ण पर्चे देखें व PDF डाउनलोड करें ➔" else "View Parchas & Download PDF ➔",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Ashram Rules & Info Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            border = BorderStroke(1.2.dp, Color(0xFF3949AB)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8EAF6),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("ℹ️", fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHindi) "आश्रम परिचय एवं दरबार नियम" else "Ashram Info & Darbar Rules",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A237E)
                        )
                        Text(
                            text = if (isHindi) "इतिहास, मर्यादा एवं दर्शन दिशा-निर्देश" else "History, Discipline & Guidelines",
                            fontSize = 11.5.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isHindi)
                        "श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट का संपूर्ण इतिहास, पूज्य गुरुजी के नियम, दर्शन व्यवस्था, स्वच्छता व अनुशासन संबंधी सभी आवश्यक नियम पढ़ें।"
                    else
                        "Complete history of Shri Balaji Kripa Dham, Dungra Jaat, Guruji's guidelines, discipline, and darbar rules.",
                    fontSize = 12.sp,
                    color = Color(0xFF333333),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onNavigateToInfo,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949AB)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 11.dp)
                ) {
                    Text(
                        text = if (isHindi) "ℹ️ आश्रम परिचय व नियम पढ़ें ➔" else "Read Ashram Info & Rules ➔",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Guruji Profile & Darbar Mission
        RenderClassicSection(
            sectionId = UiSectionConfig.ID_DARBAR_STATUS,
            settings = settings,
            isHindi = isHindi,
            currentTheme = currentTheme,
            activeSevadars = activeSevadars,
            sevadarProfiles = sevadarProfiles,
            dynamicEvents = dynamicEvents,
            onNavigateToToken = onNavigateToToken,
            onNavigateToFaceToken = onNavigateToFaceToken,
            onNavigateToYatra = onNavigateToYatra,
            onNavigateToInfo = onNavigateToInfo,
            onNavigateToAdmin = onNavigateToAdmin,
            context = context,
            isCompact = isCompact
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Sevadar Team Showcase
        RenderClassicSection(
            sectionId = UiSectionConfig.ID_SEVADAR_TEAM,
            settings = settings,
            isHindi = isHindi,
            currentTheme = currentTheme,
            activeSevadars = activeSevadars,
            sevadarProfiles = sevadarProfiles,
            dynamicEvents = dynamicEvents,
            onNavigateToToken = onNavigateToToken,
            onNavigateToFaceToken = onNavigateToFaceToken,
            onNavigateToYatra = onNavigateToYatra,
            onNavigateToInfo = onNavigateToInfo,
            onNavigateToAdmin = onNavigateToAdmin,
            context = context,
            isCompact = isCompact
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Dynamic Events
        RenderClassicSection(
            sectionId = UiSectionConfig.ID_DYNAMIC_EVENTS,
            settings = settings,
            isHindi = isHindi,
            currentTheme = currentTheme,
            activeSevadars = activeSevadars,
            sevadarProfiles = sevadarProfiles,
            dynamicEvents = dynamicEvents,
            onNavigateToToken = onNavigateToToken,
            onNavigateToFaceToken = onNavigateToFaceToken,
            onNavigateToYatra = onNavigateToYatra,
            onNavigateToInfo = onNavigateToInfo,
            onNavigateToAdmin = onNavigateToAdmin,
            context = context,
            isCompact = isCompact
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Official Website Card
        Surface(
            onClick = {
                val webUrl = settings.officialWebsiteUrl.ifEmpty { "https://shribalajikripadham.online" }
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {}
            },
            color = Color(0xFFE3F2FD),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF90CAF9)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌐", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHindi) "आश्रम की आधिकारिक वेबसाइट" else "Official Ashram Website",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0D47A1)
                        )
                        Text(
                            text = "shribalajikripadham.online • लाइव टोकन व दर्शन",
                            fontSize = 11.sp,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
                Text("खोलें ➔", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0D47A1))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7. Social Media Hub
        RenderClassicSection(
            sectionId = UiSectionConfig.ID_SOCIAL_MEDIA_HUB,
            settings = settings,
            isHindi = isHindi,
            currentTheme = currentTheme,
            activeSevadars = activeSevadars,
            sevadarProfiles = sevadarProfiles,
            dynamicEvents = dynamicEvents,
            onNavigateToToken = onNavigateToToken,
            onNavigateToFaceToken = onNavigateToFaceToken,
            onNavigateToYatra = onNavigateToYatra,
            onNavigateToInfo = onNavigateToInfo,
            onNavigateToAdmin = onNavigateToAdmin,
            context = context,
            isCompact = isCompact
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 8. Contact & Developer Credit Footer
        RenderClassicSection(
            sectionId = UiSectionConfig.ID_CONTACT_FOOTER,
            settings = settings,
            isHindi = isHindi,
            currentTheme = currentTheme,
            activeSevadars = activeSevadars,
            sevadarProfiles = sevadarProfiles,
            dynamicEvents = dynamicEvents,
            onNavigateToToken = onNavigateToToken,
            onNavigateToFaceToken = onNavigateToFaceToken,
            onNavigateToYatra = onNavigateToYatra,
            onNavigateToInfo = onNavigateToInfo,
            onNavigateToAdmin = onNavigateToAdmin,
            context = context,
            isCompact = isCompact
        )
    }
}

@Composable
fun HomeTabBottomSwitcher(
    currentTab: HomeTab,
    isHindi: Boolean,
    onSelectTab: (HomeTab) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GoldDark.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = if (isHindi) "📑 अन्य मुख्य पृष्ठ देखें (Direct Page Switch):" else "📑 Direct Page Switch:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaroonPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeTab.values().filter { it != currentTab }.forEach { tab ->
                    Surface(
                        onClick = { onSelectTab(tab) },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MaroonPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(tab.icon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isHindi) tab.titleHindi else tab.titleEnglish,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaroonPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SacredLyricsViewerDialog(
    track: SacredTrack,
    isHindi: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var fontSizeSp by remember { mutableFloatStateOf(16f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5)),
            border = BorderStroke(2.dp, GoldSecondary),
            elevation = CardDefaults.cardElevation(10.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) track.titleHindi else track.titleEnglish,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaroonPrimary
                        )
                        Text(
                            text = track.subtitleHindi,
                            fontSize = 12.sp,
                            color = TextSecondaryDark,
                            maxLines = 1
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Font Size Controls
                        IconButton(
                            onClick = { if (fontSizeSp > 12f) fontSizeSp -= 2f },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("A-", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaroonPrimary)
                        }
                        IconButton(
                            onClick = { if (fontSizeSp < 26f) fontSizeSp += 2f },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("A+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaroonPrimary)
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("✕", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Gray)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = AmberGold.copy(alpha = 0.5f))

                // Scrollable Lyrics Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = track.lyricsHindi,
                        fontSize = fontSizeSp.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111111), // High-contrast jet black
                        lineHeight = (fontSizeSp * 1.5f).sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Sacred Track Lyrics", track.lyricsHindi)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, if (isHindi) "पाठ कॉपी किया गया!" else "Lyrics copied!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📋 " + (if (isHindi) "कॉपी करें" else "Copy"), fontWeight = FontWeight.Bold, color = MaroonPrimary)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHindi) "🙏 बन्द करें" else "Close", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
