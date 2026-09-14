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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
    onToggleLanguage: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(AshramSettings()) }
    var dynamicEvents by remember { mutableStateOf<List<AshramEvent>>(emptyList()) }
    var activeSevadars by remember { mutableStateOf<List<Admin>>(emptyList()) }
    var activeLayout by remember { mutableStateOf(AppUiLayout.CLASSIC_DARBAR) }
    var uiSectionConfigs by remember { mutableStateOf<List<UiSectionConfig>>(UiSectionConfig.defaultSections()) }

    var showUpdatePopup by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadDownloadedBytes by remember { mutableLongStateOf(0L) }
    var downloadTotalBytes by remember { mutableLongStateOf(0L) }
    var downloadedApkFile by remember { mutableStateOf<java.io.File?>(null) }
    var downloadErrorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        var s = settings
        try {
            s = repository.getSettings()
            settings = s
            activeLayout = if (s.isUiLayoutEnforced) {
                AppUiLayout.fromId(s.activeUiLayout)
            } else {
                LayoutPreferences.getSavedLayout(context, AppUiLayout.fromId(s.activeUiLayout))
            }
            val customDists = repository.getAllCustomCityDistances()
            DistanceCalculatorService.loadCustomDistances(customDists.map { Pair(it.cityName, it.distanceKm) })
            val evs = repository.getAllEvents()
            if (evs.isNotEmpty()) {
                dynamicEvents = evs
            }
            activeSevadars = repository.getAllActiveSevadars()
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
                        showUpdatePopup = true
                    }
                } else {
                    val fresh = repository.getSettings()
                    if (AppUpdateManager.isUpdateAvailable(currentCode, fresh.latestVersionCode)) {
                        settings = fresh
                        showUpdatePopup = true
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeScreenInit", "Update check error on startup", e)
            }
        }

        // 🔄 Continuous live sync loop (every 20 seconds) while screen is open
        scope.launch {
            while (isActive) {
                delay(20_000)
                try {
                    val (synced, liveConfig) = repository.syncLiveConfigFromGitHub()
                    try { repository.syncAdminsFromGitHub() } catch (e: Exception) {}
                    try { repository.syncLiveParchasFromGitHub() } catch (e: Exception) {}
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
                    }

                    // 🔄 Continuous auto-check for new app releases in real-time
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
                        if (!showUpdatePopup && !isDownloadingUpdate) {
                            showUpdatePopup = true
                        }
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
                        text = if (isHindi) "🚩 मुख्य सेवाएं व स्क्रीन" else "🚩 Main Screens & Services",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    // Navigation Items
                    data class NavDrawerItem(val icon: String, val title: String, val action: () -> Unit)
                    val navItems = buildList {
                        add(NavDrawerItem("🏠", if (isHindi) "मुख्य पृष्ठ (Home)" else "Home", { /* Stay on home */ }))
                        add(NavDrawerItem("🎟️", if (isHindi) "दरबार टोकन जनरेट करें" else "Generate Darbar Token", onNavigateToToken))
                        add(NavDrawerItem("🤳", if (isHindi) "फेस वेरिफिकेशन टोकन" else "Face Token", onNavigateToFaceToken))
                        add(NavDrawerItem("📜", if (isHindi) "डिजिटल पर्चा देखें" else "Digital Parchas", onNavigateToParchas))
                        add(NavDrawerItem("🚗", if (isHindi) "यात्रा व दूरी विवरण" else "Yatra & Distance Info", onNavigateToYatra))
                        if (settings.canDevoteeViewYatraDiary) {
                            add(NavDrawerItem("💰", if (isHindi) "यात्रा खर्च डायरी" else "Yatra Expense Diary", onNavigateToYatraExpenses))
                        }
                        add(NavDrawerItem("ℹ️", if (isHindi) "आश्रम परिचय व नियम" else "Ashram Info & Rules", onNavigateToInfo))
                        add(NavDrawerItem("🔄", if (isHindi) "ऐप अपडेट जांचें (Live)" else "Check App Update", {
                            scope.launch {
                                drawerState.close()
                                Toast.makeText(context, if (isHindi) "🔄 लाइव अपडेट जांच रहे हैं..." else "Checking for updates...", Toast.LENGTH_SHORT).show()
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
                                        Toast.makeText(
                                            context,
                                            if (isHindi) "✅ आपका ऐप नवीनतम संस्करण (v${AppUpdateManager.getCurrentVersionName(context)} Build #$currentCode) पर है!"
                                            else "✅ App is on the latest version (v${AppUpdateManager.getCurrentVersionName(context)} Build #$currentCode)!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
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
                                            .background(if (isSelected) currentTheme.primaryColor else Color(0xFFEEEEEE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(layout.icon, fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isHindi) layout.titleHindi else layout.titleEnglish,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) currentTheme.primaryColor else TextPrimaryDark
                                        )
                                        Text(
                                            text = layout.subtitleHindi,
                                            fontSize = 9.5.sp,
                                            color = Color.Gray,
                                            maxLines = 1
                                        )
                                    }
                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = currentTheme.primaryColor
                                        ) {
                                            Text(
                                                text = if (isHindi) "✓ सक्रिय" else "✓ Active",
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
                                        Toast.makeText(
                                            context,
                                            if (isHindi) "✅ आपका ऐप पहले से नवीनतम संस्करण (v${AppUpdateManager.getCurrentVersionName(context)} Build #$currentCode) पर है!"
                                            else "✅ App is already on the latest version (v${AppUpdateManager.getCurrentVersionName(context)} Build #$currentCode)!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showWhatsNewDialog = true
                                    }
                                }
                            }
                        }
                    ) {
                        Text("🔄", fontSize = 19.sp)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val currentCode = AppUpdateManager.getCurrentVersionCode(context)
            val isUpdateAvailable = AppUpdateManager.isUpdateAvailable(currentCode, settings.latestVersionCode)

            // UPDATE ALERT BANNER (Active whenever an update is available)
            if (isUpdateAvailable) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFFFB300)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB300)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔔", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "नया अपडेट v${settings.latestVersionName} उपलब्ध है!" else "New Update v${settings.latestVersionName} Available!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "नए फीचर्स व सुधारों के लिए अभी तुरंत अपडेट करें।" else "Tap to update app immediately.",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                        Button(
                            onClick = { showUpdatePopup = true },
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isHindi) "अपडेट करें" else "Update",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 🌟 GRAND LIVE TOKEN STATUS ANNOUNCEMENT BANNER (Sunday 8:30 AM to 5:00 PM Schedule)
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
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    2.dp,
                    when (scheduleState) {
                        is SundayScheduleState.Open -> Color(0xFF2E7D32)
                        is SundayScheduleState.SundayBeforeStart -> Color(0xFFD84315)
                        is SundayScheduleState.SundayClosedEvening -> Color(0xFF8B0000)
                        is SundayScheduleState.NonSunday -> Color(0xFFD84315)
                        else -> Color(0xFF8B0000)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clickable {
                        onNavigateToFaceToken()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
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
                            fontSize = 24.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
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
                            fontSize = 15.sp,
                            color = when (scheduleState) {
                                is SundayScheduleState.Open -> Color(0xFF1B5E20)
                                else -> Color(0xFF8B0000)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (scheduleState) {
                                is SundayScheduleState.Open -> if (isHindi) "👉 अभी टोकन प्राप्त करें (टैप करें ➔)" else "👉 Tap here to get token now ➔"
                                is SundayScheduleState.SundayBeforeStart -> if (isHindi) "सुबह 8:30 बजे आश्रम लोकेशन पर टोकन प्राप्त करें" else "Available from 8:30 AM at Ashram"
                                is SundayScheduleState.SundayClosedEvening -> if (isHindi) "अब टोकन आगामी रविवार, ${scheduleState.nextSundayDateStr} को 8:30 AM से मिलेंगे" else "Next tokens on Sunday, ${scheduleState.nextSundayDateStr} 8:30 AM"
                                is SundayScheduleState.NonSunday -> if (isHindi) "आगामी रविवार, ${scheduleState.nextSundayDateStr} को 8:30 AM से मिलेंगे" else "Next tokens on Sunday, ${scheduleState.nextSundayDateStr} 8:30 AM"
                                is SundayScheduleState.CustomScheduled -> if (isHindi) "खुलने का समय: ${scheduleState.formattedDate}" else "Opens at: ${scheduleState.formattedDate}"
                                else -> if (isHindi) "आश्रम व्यवस्था अनुसार टोकन सेवा अभी बंद है" else "Token service paused by Ashram"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp,
                            color = when (scheduleState) {
                                is SundayScheduleState.Open -> Color(0xFF1B5E20)
                                else -> Color(0xFF111111)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onNavigateToFaceToken() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (scheduleState) {
                                is SundayScheduleState.Open -> Color(0xFF2E7D32)
                                else -> Color(0xFF8B0000)
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isTokenOpen) (if (isHindi) "टोकन लें ➔" else "Get Token ➔") else (if (isHindi) "विवरण ➔" else "Details ➔"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }



            // RENDERING BASED ON ACTIVE UI LAYOUT (10 COMPLETE UI LOOKS)
            when (activeLayout) {
                AppUiLayout.CLASSIC_DARBAR -> {
                    val sortedVisibleSections = uiSectionConfigs.filter { it.isVisible }.sortedBy { it.orderIndex }
                    for (section in sortedVisibleSections) {
                        RenderClassicSection(
                            sectionId = section.sectionId,
                            sectionConfig = section,
                            settings = settings,
                            isHindi = isHindi,
                            currentTheme = currentTheme,
                            activeSevadars = activeSevadars,
                            dynamicEvents = dynamicEvents,
                            onNavigateToToken = onNavigateToToken,
                            onNavigateToFaceToken = onNavigateToFaceToken,
                            onNavigateToYatra = onNavigateToYatra,
                            onNavigateToInfo = onNavigateToInfo,
                            onNavigateToAdmin = onNavigateToAdmin,
                            onNavigateToParchas = onNavigateToParchas,
                            context = context
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
                AppUiLayout.MODERN_CARDS -> {
                    ModernCardsLayout(
                        settings = settings,
                        isHindi = isHindi,
                        currentTheme = currentTheme,
                        activeSevadars = activeSevadars,
                        dynamicEvents = dynamicEvents,
                        onNavigateToToken = onNavigateToToken,
                        onNavigateToFaceToken = onNavigateToFaceToken,
                        onNavigateToYatra = onNavigateToYatra,
                        onNavigateToInfo = onNavigateToInfo,
                        onNavigateToAdmin = onNavigateToAdmin,
                        onNavigateToParchas = onNavigateToParchas,
                        onNavigateToYatraExpenses = onNavigateToYatraExpenses,
                        onThemeChanged = onThemeChanged
                    )
                }
                AppUiLayout.VEDIC_GRID -> {
                    VedicGridLayout(
                        settings = settings,
                        isHindi = isHindi,
                        currentTheme = currentTheme,
                        activeSevadars = activeSevadars,
                        dynamicEvents = dynamicEvents,
                        onNavigateToToken = onNavigateToToken,
                        onNavigateToFaceToken = onNavigateToFaceToken,
                        onNavigateToYatra = onNavigateToYatra,
                        onNavigateToInfo = onNavigateToInfo,
                        onNavigateToAdmin = onNavigateToAdmin,
                        onNavigateToParchas = onNavigateToParchas,
                        onNavigateToYatraExpenses = onNavigateToYatraExpenses,
                        onThemeChanged = onThemeChanged
                    )
                }
                AppUiLayout.COMPACT_LIST -> {
                    CompactListLayout(
                        settings = settings,
                        isHindi = isHindi,
                        currentTheme = currentTheme,
                        activeSevadars = activeSevadars,
                        dynamicEvents = dynamicEvents,
                        onNavigateToToken = onNavigateToToken,
                        onNavigateToFaceToken = onNavigateToFaceToken,
                        onNavigateToYatra = onNavigateToYatra,
                        onNavigateToInfo = onNavigateToInfo,
                        onNavigateToAdmin = onNavigateToAdmin,
                        onNavigateToParchas = onNavigateToParchas,
                        onNavigateToYatraExpenses = onNavigateToYatraExpenses,
                        onThemeChanged = onThemeChanged
                    )
                }
                AppUiLayout.DIVINE_FEED -> {
                    DivineFeedLayout(
                        settings = settings,
                        isHindi = isHindi,
                        currentTheme = currentTheme,
                        activeSevadars = activeSevadars,
                        dynamicEvents = dynamicEvents,
                        onNavigateToToken = onNavigateToToken,
                        onNavigateToFaceToken = onNavigateToFaceToken,
                        onNavigateToYatra = onNavigateToYatra,
                        onNavigateToInfo = onNavigateToInfo,
                        onNavigateToAdmin = onNavigateToAdmin,
                        onNavigateToParchas = onNavigateToParchas,
                        onNavigateToYatraExpenses = onNavigateToYatraExpenses,
                        onThemeChanged = onThemeChanged
                    )
                }
                AppUiLayout.MAHABALI_HERO -> {
                    MahabaliHeroLayout(
                        settings = settings,
                        isHindi = isHindi,
                        currentTheme = currentTheme,
                        activeSevadars = activeSevadars,
                        dynamicEvents = dynamicEvents,
                        onNavigateToToken = onNavigateToToken,
                        onNavigateToFaceToken = onNavigateToFaceToken,
                        onNavigateToYatra = onNavigateToYatra,
                        onNavigateToInfo = onNavigateToInfo,
                        onNavigateToAdmin = onNavigateToAdmin,
                        onNavigateToParchas = onNavigateToParchas,
                        onNavigateToYatraExpenses = onNavigateToYatraExpenses,
                        onThemeChanged = onThemeChanged
                    )
                }
                AppUiLayout.BHAKTI_ACCORDION -> {
                    BhaktiAccordionLayout(
                        settings = settings,
                        isHindi = isHindi,
                        currentTheme = currentTheme,
                        activeSevadars = activeSevadars,
                        dynamicEvents = dynamicEvents,
                        onNavigateToToken = onNavigateToToken,
                        onNavigateToFaceToken = onNavigateToFaceToken,
                        onNavigateToYatra = onNavigateToYatra,
                        onNavigateToInfo = onNavigateToInfo,
                        onNavigateToAdmin = onNavigateToAdmin,
                        onNavigateToParchas = onNavigateToParchas,
                        onNavigateToYatraExpenses = onNavigateToYatraExpenses,
                        onThemeChanged = onThemeChanged
                    )
                }
                AppUiLayout.PARIKRAMA_FLOW -> {
                    MandirParikramaLayout(
                        settings = settings,
                        isHindi = isHindi,
                        currentTheme = currentTheme,
                        activeSevadars = activeSevadars,
                        dynamicEvents = dynamicEvents,
                        onNavigateToToken = onNavigateToToken,
                        onNavigateToFaceToken = onNavigateToFaceToken,
                        onNavigateToYatra = onNavigateToYatra,
                        onNavigateToInfo = onNavigateToInfo,
                        onNavigateToAdmin = onNavigateToAdmin,
                        onNavigateToParchas = onNavigateToParchas,
                        onNavigateToYatraExpenses = onNavigateToYatraExpenses,
                        onThemeChanged = onThemeChanged
                    )
                }
                AppUiLayout.GOLDEN_LOTUS -> {
                    GoldenLotusLayout(
                        settings = settings,
                        isHindi = isHindi,
                        currentTheme = currentTheme,
                        activeSevadars = activeSevadars,
                        dynamicEvents = dynamicEvents,
                        onNavigateToToken = onNavigateToToken,
                        onNavigateToFaceToken = onNavigateToFaceToken,
                        onNavigateToYatra = onNavigateToYatra,
                        onNavigateToInfo = onNavigateToInfo,
                        onNavigateToAdmin = onNavigateToAdmin,
                        onNavigateToParchas = onNavigateToParchas,
                        onNavigateToYatraExpenses = onNavigateToYatraExpenses,
                        onThemeChanged = onThemeChanged
                    )
                }
                AppUiLayout.SIDDHA_PEETH_PORTAL -> {
                    SiddhaPeethPortalLayout(
                        settings = settings,
                        isHindi = isHindi,
                        currentTheme = currentTheme,
                        activeSevadars = activeSevadars,
                        dynamicEvents = dynamicEvents,
                        onNavigateToToken = onNavigateToToken,
                        onNavigateToFaceToken = onNavigateToFaceToken,
                        onNavigateToYatra = onNavigateToYatra,
                        onNavigateToInfo = onNavigateToInfo,
                        onNavigateToAdmin = onNavigateToAdmin,
                        onNavigateToParchas = onNavigateToParchas,
                        onNavigateToYatraExpenses = onNavigateToYatraExpenses,
                        onThemeChanged = onThemeChanged
                    )
                }
            }



            // 7. SOCIAL MEDIA & APP SHARE HUB (Only for alternative layouts, since Classic Darbar renders it dynamically)
            if (activeLayout != AppUiLayout.CLASSIC_DARBAR) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = currentTheme.cardShape,
                elevation = CardDefaults.cardElevation(currentTheme.cardElevation),
                border = BorderStroke(currentTheme.cardBorderWidth, currentTheme.secondaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌐", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "आश्रम से सोशल मीडिया पर जुड़ें" else "Connect with Ashram",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.primaryColor
                            )
                            Text(
                                text = if (isHindi) "लाइव दर्शन, आरती, सूचनाएं व ऍप शेयर करें" else "Live Darshan, Aarti, Updates & Share App",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // WhatsApp Group
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
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isHindi) "💬 व्हाट्सएप्प" else "💬 WhatsApp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // YouTube Channel
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
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isHindi) "▶️ यूट्यूब" else "▶️ YouTube",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Facebook Page
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
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isHindi) "📘 फेसबुक" else "📘 Facebook",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isHindi) "📸 इंस्टाग्राम" else "📸 Instagram",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Master Share App Button with Pre-filled Devotional Message
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

            Spacer(modifier = Modifier.height(24.dp))

            // 8. OFFICIAL APP BRANDING & DEVELOPER CREDIT FOOTER (PRD Requirement)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚩 श्री बालाजी कृपा धाम 🚩",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonAccent
                    )
                    Text(
                        text = "ग्राम डूँगरा जाट, जिला बुलन्दशहर (उ०प्र०)",
                        fontSize = 12.sp,
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

            }
    // IN-APP UPDATE POPUP DIALOG (Pops up directly on Home Screen!)
    if (showUpdatePopup) {
        Dialog(
            onDismissRequest = {
                if (!settings.isForceUpdate && !isDownloadingUpdate) {
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
                    modifier = Modifier.padding(22.dp),
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = settings.updateNotes.ifEmpty {
                            if (isHindi) "नवीनतम सुधार, तीव्र गति, 6 दिव्य थीम्स व सोशल मीडिया हब जोड़ा गया है।"
                            else "Latest fixes, faster performance and new features added."
                        },
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        lineHeight = 17.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

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

                        if (!settings.isForceUpdate) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { showUpdatePopup = false },
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
}

@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    iconBadge: String,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    isPopular: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.22f)),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = iconBadge, fontSize = 22.sp)
                    }
                }

                if (isPopular) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoldSecondary.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, GoldDark)
                    ) {
                        Text(
                            text = "★ मुख्य",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SaffronDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaroonAccent,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = TextSecondaryDark,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SevadarCard(sevadar: SevadarProfile, isHindi: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.width(220.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SaffronPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sevadar.initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = sevadar.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = sevadar.phoneNumber,
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isHindi) sevadar.roleTitleHindi else sevadar.roleTitleEnglish,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SaffronDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isHindi) sevadar.dutyHindi else sevadar.dutyEnglish,
                fontSize = 11.sp,
                color = TextSecondaryDark,
                lineHeight = 15.sp
            )
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
    dynamicEvents: List<AshramEvent>,
    onNavigateToToken: () -> Unit,
    onNavigateToFaceToken: () -> Unit,
    onNavigateToYatra: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToParchas: () -> Unit = {},
    context: Context
) {
    // Custom announcement / guideline banner customized by Super Admin
    if (sectionConfig != null && (sectionConfig.customContentHindi.isNotBlank() || sectionConfig.customSubtitleHindi.isNotBlank())) {
        val subtitleText = if (isHindi) sectionConfig.customSubtitleHindi.ifEmpty { sectionConfig.customSubtitleEnglish } else sectionConfig.customSubtitleEnglish.ifEmpty { sectionConfig.customSubtitleHindi }
        val contentText = if (isHindi) sectionConfig.customContentHindi.ifEmpty { sectionConfig.customContentEnglish } else sectionConfig.customContentEnglish.ifEmpty { sectionConfig.customContentHindi }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9EE)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sectionConfig.icon, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) sectionConfig.titleHindi else sectionConfig.titleEnglish,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaroonPrimary
                    )
                    if (subtitleText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• $subtitleText",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }
                if (contentText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = contentText,
                        fontSize = 12.sp,
                        color = Color(0xFF3E2723),
                        lineHeight = 16.sp
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
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                GoldLight,
                                                currentTheme.secondaryColor
                                            )
                                        )
                                    )
                                    .padding(3.dp)
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

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "🚩 ॥ श्री हनुमते नमः ॥",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldLight,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isHindi) "श्री बालाजी कृपा धाम" else "Shri Balaji Kripa Dham",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = currentTheme.accentGold,
                                    letterSpacing = 0.3.sp
                                )
                                Text(
                                    text = if (isHindi) "डूँगरा जाट, बुलन्दशहर (उ.प्र.)" else "Dungra Jaat, Bulandshahr (U.P.)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isHindi) "परम पूज्य गुरुजी तेजवीर सिंह जी" else "Param Pujya Guruji Tejveer Singh Ji",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = currentTheme.secondaryColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live Darbar status pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.30f),
                            border = BorderStroke(0.8.dp, currentTheme.secondaryColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E676))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "आगामी दिव्य दरबार: प्रत्येक रविवार प्रातः 7:00 बजे" else "Next Holy Darbar: Sunday 7:00 AM",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text("🪔", fontSize = 14.sp)
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
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(3.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📢", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = settings.emergencyNoticeText,
                            color = Color(0xFFB71C1C),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        UiSectionConfig.ID_FREE_TREATMENT_BOX -> {
            // 100% FREE TREATMENT CERTIFIED TRUST SEAL
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(5.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFFB300)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🕊️", fontSize = 26.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = if (isHindi) "★ पूर्णतः निःशुल्क (100% FREE)" else "★ 100% FREE OF COST",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isHindi) "आध्यात्मिक कष्ट निवारण सेवा" else "Spiritual Healing Service",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaroonAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = settings.freeDisclaimer.ifEmpty {
                            if (isHindi)
                                "यहाँ भूत-प्रेत व मानसिक समस्याओं का इलाज पूर्णतः निःशुल्क किया जाता है। कोई पैसा नहीं लिया जाता, केवल भगवान की पूजा-पाठ और नियम बताए जाते हैं।"
                            else
                                "Treatment for mental afflictions and spiritual disturbances is completely FREE. No money is charged; only divine prayers and spiritual disciplines are prescribed."
                        },
                        fontSize = 12.5.sp,
                        color = TextPrimaryDark,
                        lineHeight = 17.5.sp
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
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFFFB300)),
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏳", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isHindi) "रविवार टोकन पंजीकरण पूर्व-निर्धारित है" else "Token Registration Scheduled",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isHindi)
                                    "टोकन खुलने का समय: $scheduledTimeStr\n(उस समय यह स्वतः खुल जाएगा)"
                                else
                                    "Opens automatically on: $scheduledTimeStr",
                                fontSize = 12.sp,
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
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
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
                            .padding(16.dp)
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
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, GoldLight.copy(alpha = 0.5f)),
                                    modifier = Modifier.size(50.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(if (isBeforeSchedule) "⏳" else "⚡", fontSize = 24.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isHindi) "स्मार्ट चेहरा टोकन" else "Smart Face Token",
                                            color = GoldLight,
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SaffronPrimary
                                        ) {
                                            Text(
                                                text = "< 1s",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isBeforeSchedule)
                                            (if (isHindi) "पंजीकरण पूर्व-निर्धारित समय पर खुलेगा" else "Scheduled to open at set time")
                                        else
                                            (if (isHindi) "दाढ़ी/चश्मा अप्रभावित • 1-क्लिक पुष्टि" else "AI facial match • Instant confirmation"),
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = onNavigateToFaceToken,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBeforeSchedule) Color.White.copy(alpha = 0.2f) else GoldSecondary,
                                    contentColor = if (isBeforeSchedule) Color.White else Color(0xFF4A0017)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(4.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (isBeforeSchedule) (if (isHindi) "देखें" else "View") else (if (isHindi) "स्कैन करें" else "Scan"),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        UiSectionConfig.ID_QUICK_SERVICES -> {
            // Quick Access Action Tiles
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isHindi) "मुख्य सेवाएं व विकल्प" else "Quick Access Services",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaroonAccent
                    )
                    Text(
                        text = if (isHindi) "4 मुख्य सेवाएं" else "4 Core Services",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (settings.isYatraServiceEnabled) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ActionTile(
                            title = if (isHindi) "रविवार टोकन" else "Sunday Token",
                            subtitle = if (settings.isTokenServiceEnabled) (if (isHindi) "दरबार कतार नंबर" else "Live Queue & Pass") else (if (isHindi) "पंजीकरण स्थगित" else "Paused by Admin"),
                            iconBadge = if (settings.isTokenServiceEnabled) "🏷️" else "🔒",
                            badgeColor = if (settings.isTokenServiceEnabled) SaffronPrimary else Color.Gray,
                            isPopular = settings.isTokenServiceEnabled,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToToken
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        ActionTile(
                            title = if (isHindi) "बालाजी यात्रा" else "Balaji Yatra",
                            subtitle = if (isHindi) "बस सीट बुकिंग" else "Bus Seat Booking",
                            iconBadge = "🚌",
                            badgeColor = GoldDark,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToYatra
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ActionTile(
                            title = if (isHindi) "आश्रम परिचय" else "Ashram Info",
                            subtitle = if (isHindi) "नियम व लोकेशन" else "Rules & GPS Route",
                            iconBadge = "ℹ️",
                            badgeColor = Color(0xFF3949AB),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToInfo
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        ActionTile(
                            title = if (isHindi) "सेवादार पोर्टल" else "Admin Portal",
                            subtitle = if (isHindi) "व्यवस्थापक प्रवेश" else "Sevadar & Admin",
                            iconBadge = "🛡️",
                            badgeColor = MaroonAccent,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAdmin
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ActionTile(
                            title = if (isHindi) "📜 आश्रम पर्चे व दस्तावेज" else "📜 Sacred Documents & Slips",
                            subtitle = if (isHindi) "हवन पर्चा, मैया उतारा, अर्जी व A4 PDF डाउनलोड" else "Hawan, Maiya Utara, Arji & A4 PDF",
                            iconBadge = "📜",
                            badgeColor = Color(0xFF6A1B9A),
                            isPopular = true,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onNavigateToParchas
                        )
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ActionTile(
                            title = if (isHindi) "रविवार टोकन" else "Sunday Token",
                            subtitle = if (settings.isTokenServiceEnabled) (if (isHindi) "दरबार कतार नंबर" else "Live Queue & Pass") else (if (isHindi) "पंजीकरण स्थगित" else "Paused by Admin"),
                            iconBadge = if (settings.isTokenServiceEnabled) "🏷️" else "🔒",
                            badgeColor = if (settings.isTokenServiceEnabled) SaffronPrimary else Color.Gray,
                            isPopular = settings.isTokenServiceEnabled,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToToken
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        ActionTile(
                            title = if (isHindi) "आश्रम परिचय" else "Ashram Info",
                            subtitle = if (isHindi) "नियम व लोकेशन" else "Rules & GPS Route",
                            iconBadge = "ℹ️",
                            badgeColor = Color(0xFF3949AB),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToInfo
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ActionTile(
                            title = if (isHindi) "सेवादार पोर्टल" else "Admin Portal",
                            subtitle = if (isHindi) "व्यवस्थापक प्रवेश" else "Sevadar & Admin",
                            iconBadge = "🛡️",
                            badgeColor = MaroonAccent,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAdmin
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        ActionTile(
                            title = if (isHindi) "📜 आश्रम पर्चे" else "📜 Sacred Parchas",
                            subtitle = if (isHindi) "हवन, उतारा व A4 PDF" else "Hawan, Utara & PDF",
                            iconBadge = "📜",
                            badgeColor = Color(0xFF6A1B9A),
                            isPopular = true,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToParchas
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
                                size = 58.dp,
                                primaryColor = currentTheme.primaryColor,
                                borderColor = currentTheme.secondaryColor
                            )
                            Spacer(modifier = Modifier.width(14.dp))
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
            // Ashram Sevadar Showcase
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isHindi) "आश्रम के समर्पित सेवादार" else "Dedicated Ashram Sevadars",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = currentTheme.primaryColor
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (activeSevadars.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(activeSevadars) { sevadar ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(18.dp),
                                elevation = CardDefaults.cardElevation(4.dp),
                                border = BorderStroke(1.dp, currentTheme.secondaryColor.copy(alpha = 0.5f)),
                                modifier = Modifier.width(225.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SacredAvatar(
                                            photoUri = sevadar.photoUri,
                                            fallbackText = sevadar.name,
                                            size = 46.dp,
                                            primaryColor = currentTheme.primaryColor,
                                            borderColor = currentTheme.secondaryColor
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = sevadar.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp,
                                                color = TextPrimaryDark,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = if (isHindi) "अधिकृत सेवादार" else "Authorized Sevadar",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = currentTheme.primaryColor
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF5F5F5),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isHindi) "📞 ${sevadar.phoneNumber}" else "📞 ${sevadar.phoneNumber}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextSecondaryDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🙏", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isHindi)
                                    "आश्रम सेवादारों की अधिकृत सूची मुख्य व्यवस्थापक (Super Admin) द्वारा जल्द ही जोड़ी जाएगी।"
                                else
                                    "Authorized Sevadar list will be added by the Super Admin.",
                                fontSize = 13.sp,
                                color = TextSecondaryDark,
                                lineHeight = 18.sp
                            )
                        }
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
