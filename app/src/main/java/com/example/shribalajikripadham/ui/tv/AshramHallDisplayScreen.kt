package com.example.shribalajikripadham.ui.tv

import android.app.Activity
import android.content.Context
import android.media.ToneGenerator
import android.media.AudioManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.Token
import com.example.shribalajikripadham.data.model.TokenStatus
import com.example.shribalajikripadham.data.network.HostingerCentralSyncManager
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.util.AshramVoiceAnnouncementManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AshramHallDisplayScreen(
    isHindi: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AshramRepository(context) }

    var settings by remember { mutableStateOf(AshramSettings()) }
    var todayTokens by remember { mutableStateOf<List<Token>>(emptyList()) }
    var runningToken by remember { mutableIntStateOf(1) }
    var lastAnnouncedToken by remember { mutableIntStateOf(0) }
    var currentTimeString by remember { mutableStateOf("") }
    var isAutoAnnounceEnabled by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    var isOnlineConnected by remember { mutableStateOf(true) }

    // Keep screen on for TV display
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Initialize voice engine and local cache
    LaunchedEffect(Unit) {
        AshramVoiceAnnouncementManager.initIfNeeded(context)
        settings = repository.getSettings()
        val cachedTokens = repository.getAllTokensToday()
        if (cachedTokens.isNotEmpty()) {
            todayTokens = cachedTokens
        }
        runningToken = if (settings.runningTokenNumber > 0) settings.runningTokenNumber else (cachedTokens.firstOrNull()?.tokenNumber ?: 1)
    }

    // Clock ticker (every second)
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("hh:mm:ss a • EEEE, d MMMM yyyy", Locale("hi", "IN"))
        while (isActive) {
            currentTimeString = sdf.format(Date())
            delay(1000)
        }
    }

    fun playSacredChime() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
        } catch (e: Exception) {}
    }

    fun announceCurrentToken(tokenNum: Int, devoteeName: String, devoteeCity: String) {
        if (!isAutoAnnounceEnabled) return
        scope.launch {
            playSacredChime()
            delay(300)
            AshramVoiceAnnouncementManager.announceNextToken(
                context = context,
                tokenNumber = tokenNum,
                devoteeName = devoteeName,
                city = devoteeCity
            )
        }
    }

    suspend fun refreshLiveQueue() {
        try {
            isSyncing = true
            val queueJson = HostingerCentralSyncManager.fetchLiveQueue()
            val configJson = HostingerCentralSyncManager.fetchLiveConfig()

            if (configJson != null && (configJson.optBoolean("success", false) || configJson.has("running_token_number") || configJson.has("config"))) {
                val liveCfg = if (configJson.has("config")) configJson.getJSONObject("config") else configJson
                val currentServing = liveCfg.optInt("running_token_number", liveCfg.optInt("current_serving_token", runningToken))
                if (currentServing > 0 && currentServing != runningToken) {
                    runningToken = currentServing
                }
            }

            // Parse live online queue from central Hostinger server
            val serverTokens = mutableListOf<Token>()
            if (queueJson != null && queueJson.optBoolean("success", false) && queueJson.has("tokens")) {
                val tArr = queueJson.optJSONArray("tokens")
                if (tArr != null) {
                    for (i in 0 until tArr.length()) {
                        val obj = tArr.getJSONObject(i)
                        val tNum = obj.optInt("token_number", 0)
                        if (tNum > 0) {
                            val pName = obj.optString("patient_name", "")
                            val pPhone = obj.optString("phone_number", "")
                            val pCity = obj.optString("city", "डूँगरा जाट")
                            val pDate = obj.optString("darbar_date", "")
                            val stStr = obj.optString("status", "WAITING")
                            val pStatus = try { TokenStatus.valueOf(stStr) } catch (e: Exception) { TokenStatus.WAITING }
                            serverTokens.add(
                                Token(
                                    tokenNumber = tNum,
                                    patientName = pName,
                                    phoneNumber = pPhone,
                                    city = pCity,
                                    darbarDate = pDate,
                                    status = pStatus,
                                    deviceId = obj.optString("device_id", "ONLINE_SERVER"),
                                    latitude = obj.optDouble("latitude", 0.0),
                                    longitude = obj.optDouble("longitude", 0.0),
                                    registeredBy = obj.optString("registered_by", "ONLINE_DEVOTEE")
                                )
                            )
                        }
                    }
                }
                val qServing = queueJson.optInt("current_serving_token", 0)
                if (qServing > 0 && qServing != runningToken) {
                    runningToken = qServing
                }
            }

            val localTokens = repository.getAllTokensToday()
            val mergedMap = mutableMapOf<Int, Token>()
            localTokens.forEach { mergedMap[it.tokenNumber] = it }
            serverTokens.forEach { st ->
                val ex = mergedMap[st.tokenNumber]
                if (ex == null) {
                    mergedMap[st.tokenNumber] = st
                } else if (ex.patientName.isBlank() && st.patientName.isNotBlank()) {
                    mergedMap[st.tokenNumber] = st
                }
            }
            val computed = mergedMap.values.sortedBy { it.tokenNumber }
            if (computed.isNotEmpty()) {
                todayTokens = computed
            }
            isOnlineConnected = (queueJson != null || configJson != null)
        } catch (e: Exception) {
            isOnlineConnected = false
            // Failsafe: Preserve existing tokens or load local sqlite tokens without blanking
            if (todayTokens.isEmpty()) {
                todayTokens = repository.getAllTokensToday()
            }
        } finally {
            isSyncing = false
        }
    }

    // Real-Time 2.5s Auto-Poller for Ashram TV Board
    LaunchedEffect(Unit) {
        while (isActive) {
            refreshLiveQueue()
            delay(2500)
        }
    }

    val currentDevotee = remember(runningToken, todayTokens) {
        todayTokens.find { it.tokenNumber == runningToken }
    }

    // Trigger Hindi voice announcement whenever running token changes
    LaunchedEffect(runningToken) {
        if (runningToken > 0 && runningToken != lastAnnouncedToken) {
            lastAnnouncedToken = runningToken
            val dev = currentDevotee
            announceCurrentToken(
                tokenNum = runningToken,
                devoteeName = dev?.patientName ?: "",
                devoteeCity = dev?.city ?: "डूँगरा जाट"
            )
        }
    }

    val pastTokens = remember(runningToken, todayTokens) {
        todayTokens.filter { it.tokenNumber < runningToken && it.status != TokenStatus.CANCELLED }
            .sortedByDescending { it.tokenNumber }
            .take(5)
    }

    val upcomingTokens = remember(runningToken, todayTokens) {
        todayTokens.filter { it.tokenNumber > runningToken && it.status != TokenStatus.CANCELLED }
            .sortedBy { it.tokenNumber }
            .take(5)
    }

    // Pulse animation for Current Serving Token Box
    val infiniteTransition = rememberInfiniteTransition(label = "TvPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )
    val borderGlow = SaffronPrimary.copy(alpha = glowAlpha)

    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0202))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ============================================================
            // TOP BAR: HEADER & ASHRAM BANNER
            // ============================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0606)),
                border = BorderStroke(1.5.dp, borderGlow)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🚩", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "श्री बालाजी कृपा धाम, डूँगरा जाट",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD54F)
                            )
                            Text(
                                text = "प्रत्येक रविवार पावन दिव्य दरबार • लाइव टोकन सूचना पटल (TV Board)",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = currentTimeString,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFCC80)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        when {
                                            isSyncing -> Color.Yellow
                                            isOnlineConnected -> Color.Green
                                            else -> Color(0xFFFF9800)
                                        },
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isSyncing -> "सिंक हो रहा है..."
                                    isOnlineConnected -> "लाइव कनेक्टेड (2.5s)"
                                    else -> "ऑफ़लाइन सुरक्षित मोड (लोकल कैश)"
                                },
                                fontSize = 11.sp,
                                color = if (isOnlineConnected) Color.LightGray else Color(0xFFFFCC80)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ============================================================
            // MAIN CONTENT: GIANT TOKEN DISPLAY + UPCOMING/PAST COLUMNS
            // ============================================================
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT COLUMN: RECENTLY SERVED TOKENS
                Card(
                    modifier = Modifier
                        .weight(0.28f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0606)),
                    border = BorderStroke(1.dp, Color(0xFF3E1010))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "पूर्व पूर्ण टोकन (Recent)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFAB91)
                        )
                        Spacer(Modifier.height(8.dp))
                        if (pastTokens.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("कोई पूर्व टोकन नहीं", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(pastTokens) { t ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF260A0A))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${t.tokenNumber}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFFFFCC80)
                                        )
                                        Text(
                                            text = t.patientName,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "✅ पूर्ण",
                                            fontSize = 11.sp,
                                            color = Color(0xFF81C784)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // CENTER: ENORMOUS NOW SERVING TOKEN DISPLAY (FOCAL POINT FOR 50 FEET)
                Card(
                    modifier = Modifier
                        .weight(0.44f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF280808)),
                    border = BorderStroke(3.dp, borderGlow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(50.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C))
                        ) {
                            Text(
                                text = "🔴 वर्तमान में सेवारत टोकन (NOW SERVING)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Giant Token Number
                        Text(
                            text = "#$runningToken",
                            fontSize = 92.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFFFFD54F),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        // Devotee Name
                        val devName = currentDevotee?.patientName?.ifBlank { "श्रद्धालु उपस्थित" } ?: "टोकन नंबर $runningToken"
                        Text(
                            text = devName,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Devotee City
                        val devCity = currentDevotee?.city?.ifBlank { "डूँगरा जाट" } ?: "डूँगरा जाट (स्थानीय)"
                        Text(
                            text = "📍 $devCity",
                            fontSize = 18.sp,
                            color = Color(0xFFFFAB91),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF388E3C))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🚩", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "कृपया गुरुजी के समीप दरबार में पधारें",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // RIGHT COLUMN: UPCOMING IN LINE TOKENS
                Card(
                    modifier = Modifier
                        .weight(0.28f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0606)),
                    border = BorderStroke(1.dp, Color(0xFF3E1010))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "आगामी टोकन (Next in Line)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                        Spacer(Modifier.height(8.dp))
                        if (upcomingTokens.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("कोई आगामी टोकन नहीं", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(upcomingTokens) { t ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF260A0A))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${t.tokenNumber}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFFFFD54F)
                                        )
                                        Text(
                                            text = t.patientName,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "⏳ तैयार रहें",
                                            fontSize = 11.sp,
                                            color = Color(0xFFFFCC80)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ============================================================
            // BOTTOM CONTROLS BAR: ASHRAM SEVADAR ACTIONS
            // ============================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0606)),
                border = BorderStroke(1.dp, Color(0xFF4E1616))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Token Button
                    Button(
                        onClick = {
                            val newNum = (runningToken - 1).coerceAtLeast(1)
                            runningToken = newNum
                            scope.launch {
                                repository.updateRunningTokenNumber(newNum)
                                HostingerCentralSyncManager.updateLiveConfig(
                                    radiusMeters = settings.allowedRadiusMeters,
                                    isGeofenceEnforced = settings.isGeofenceEnforced,
                                    isOutstationAllowed = settings.isOutstationAdvanceAllowed,
                                    outstationKm = settings.outstationMinDistanceKm,
                                    currentServingToken = newNum
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("⏪ पिछला (#${(runningToken - 1).coerceAtLeast(1)})", fontWeight = FontWeight.Bold)
                    }

                    // Re-announce Button
                    Button(
                        onClick = {
                            val dev = currentDevotee
                            announceCurrentToken(
                                tokenNum = runningToken,
                                devoteeName = dev?.patientName ?: "",
                                devoteeCity = dev?.city ?: "डूँगरा जाट"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🔊 पुनः घोषणा करें (Speak)", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Chime Button
                    OutlinedButton(
                        onClick = { playSacredChime() },
                        border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD54F)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🔔 घंटी बजाएं", fontWeight = FontWeight.Bold)
                    }

                    // Next Token Button
                    Button(
                        onClick = {
                            val nextNum = runningToken + 1
                            runningToken = nextNum
                            scope.launch {
                                repository.updateRunningTokenNumber(nextNum)
                                HostingerCentralSyncManager.updateLiveConfig(
                                    radiusMeters = settings.allowedRadiusMeters,
                                    isGeofenceEnforced = settings.isGeofenceEnforced,
                                    isOutstationAllowed = settings.isOutstationAdvanceAllowed,
                                    outstationKm = settings.outstationMinDistanceKm,
                                    currentServingToken = nextNum
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("⏩ अगला टोकन (#${runningToken + 1})", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Exit TV Mode Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFB71C1C))
                    ) {
                        Text("✕", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
