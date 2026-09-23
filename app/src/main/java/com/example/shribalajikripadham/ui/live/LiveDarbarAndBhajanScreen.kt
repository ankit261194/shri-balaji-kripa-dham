package com.example.shribalajikripadham.ui.live

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import com.example.shribalajikripadham.service.BhajanAudioService
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.example.shribalajikripadham.data.sacred.SacredTrack
import com.example.shribalajikripadham.util.DevotionalAudioCacheManager
import com.example.shribalajikripadham.util.SacredOfflineVaniEngine
import android.widget.Toast


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDarbarAndBhajanScreen(
    isHindi: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AshramRepository(context) }
    var ashramSettings by remember { mutableStateOf(AshramSettings()) }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Live Video, 1: Sacred Audio

    // Foreground Media Service State Binding
    val svcTrackIndex by BhajanAudioService.currentTrackIndex.collectAsState()
    val isPlaying by BhajanAudioService.isPlaying.collectAsState()
    val isBuffering by BhajanAudioService.isBuffering.collectAsState()
    val currentPositionMs by BhajanAudioService.currentPositionMs.collectAsState()
    val totalDurationMs by BhajanAudioService.durationMs.collectAsState()

    var currentTrackIndex by remember { mutableIntStateOf(0) }
    var isLooping by remember { mutableStateOf(false) }
    var playbackErrorMessage by remember { mutableStateOf<String?>(null) }
    var showLyricsDialog by remember { mutableStateOf<SacredTrack?>(null) }
    var cacheRefreshCounter by remember { mutableIntStateOf(0) }
    var tracksList by remember { mutableStateOf<List<SacredTrack>>(emptyList()) }
    var isTracksLoading by remember { mutableStateOf(false) }

    val isVaniReciting by SacredOfflineVaniEngine.isReciting.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            SacredOfflineVaniEngine.stop()
        }
    }

    LaunchedEffect(svcTrackIndex, tracksList) {
        if (tracksList.isNotEmpty() && svcTrackIndex in tracksList.indices) {
            currentTrackIndex = svcTrackIndex
        }
    }

    LaunchedEffect(Unit) {
        ashramSettings = repository.getSettings()
        tracksList = repository.getSacredTracks(publishedOnly = true)
        scope.launch {
            isTracksLoading = true
            val (ok, list) = repository.syncSacredTracksFromHostinger(admin = false)
            if (ok && list.isNotEmpty()) {
                tracksList = list
            }
            isTracksLoading = false
        }
    }

    fun playTrack(index: Int) {
        try {
            playbackErrorMessage = null
            if (index !in tracksList.indices) return
            currentTrackIndex = index
            val track = tracksList[index]
            val playableSource = DevotionalAudioCacheManager.getPlayableSource(context, track.trackKey, track.audioUrl)
            BhajanAudioService.playTrack(
                context = context,
                trackIndex = index,
                title = if (isHindi) track.titleHindi else track.titleEnglish,
                artist = track.subtitleHindi.ifBlank { "श्री बालाजी कृपा धाम (डूँगरा जाट)" },
                audioUrl = playableSource,
                trackKey = track.trackKey
            )
        } catch (e: Exception) {
            playbackErrorMessage = e.localizedMessage
        }
    }

    fun togglePlayPause() {
        if (svcTrackIndex != currentTrackIndex) {
            playTrack(currentTrackIndex)
        } else {
            BhajanAudioService.togglePlayPause(context)
        }
    }

    fun formatTime(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }

    fun openTrackInYouTube(track: SacredTrack) {
        val query = track.youtubeSearchQuery
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
            setPackage("com.google.android.youtube")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "श्री बालाजी कृपा धाम (डूँगरा जाट)" else "Shri Balaji Kripa Dham",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
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
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaroonPrimary.copy(alpha = 0.95f),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(pulseScale)
                                    .background(Color.Red, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "🔴 लाइव दर्शन" else "Live Darbar",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTabIndex == 0) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎵", fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "आरती व भजन" else "Aarti & Bhajans",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTabIndex == 1) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                )
            }

            if (selectedTabIndex == 0) {
                // ============================================================
                // TAB 0: LIVE STREAM / DARBAR VIDEO IN-APP
                // ============================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val rawChannelUrl = ashramSettings.youtubeChannelUrl.trim().ifEmpty {
                        "https://www.youtube.com/@ShriBalajiKripaDham"
                    }
                    val liveVideoUrl = if (rawChannelUrl.contains("/@") && !rawChannelUrl.endsWith("/live")) "$rawChannelUrl/live" else rawChannelUrl
                    val isDarbarLive = ashramSettings.isDarbarActive && ashramSettings.isDarbarLiveNow
                    var showInAppPlayer by remember { mutableStateOf(false) }

                    // Clean YouTube Embed URL Generator (Zero-cookie, no redirects)
                    fun getYouTubeEmbedUrl(url: String): String {
                        val clean = url.trim()
                        if (clean.contains("youtube.com/embed/")) {
                            return clean.substringBefore("?") + "?autoplay=1&modestbranding=1&rel=0&playsinline=1&controls=1"
                        }
                        val shortMatch = Regex("""youtu\.be/([a-zA-Z0-9_\-]+)""").find(clean)
                        if (shortMatch != null) {
                            val vid = shortMatch.groupValues[1]
                            return "https://www.youtube-nocookie.com/embed/$vid?autoplay=1&modestbranding=1&rel=0&playsinline=1&controls=1"
                        }
                        val watchMatch = Regex("""[?&]v=([a-zA-Z0-9_\-]+)""").find(clean)
                        if (watchMatch != null) {
                            val vid = watchMatch.groupValues[1]
                            return "https://www.youtube-nocookie.com/embed/$vid?autoplay=1&modestbranding=1&rel=0&playsinline=1&controls=1"
                        }
                        val liveMatch = Regex("""youtube\.com/live/([a-zA-Z0-9_\-]+)""").find(clean)
                        if (liveMatch != null) {
                            val vid = liveMatch.groupValues[1]
                            return "https://www.youtube-nocookie.com/embed/$vid?autoplay=1&modestbranding=1&rel=0&playsinline=1&controls=1"
                        }
                        if (clean.contains("/channel/")) {
                            val chId = clean.substringAfter("/channel/").substringBefore("/").substringBefore("?")
                            return "https://www.youtube-nocookie.com/embed/live_stream?channel=$chId&autoplay=1&modestbranding=1&rel=0&playsinline=1"
                        }
                        val handle = if (clean.contains("/@")) clean.substringAfter("/@").substringBefore("/").substringBefore("?") else ""
                        return if (handle.isNotBlank()) {
                            "https://www.youtube-nocookie.com/embed/live_stream?channel=$handle&autoplay=1&modestbranding=1&rel=0&playsinline=1"
                        } else {
                            "https://www.youtube-nocookie.com/embed/live_stream?autoplay=1&modestbranding=1&rel=0&playsinline=1"
                        }
                    }

                    if (isDarbarLive || showInAppPlayer) {
                        // 🟢 IN-APP HIGH-PERFORMANCE VIDEO PLAYER (CLEAN EMBED)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            @SuppressLint("SetJavaScriptEnabled")
                                            val webConfig = this.settings
                                            webConfig.javaScriptEnabled = true
                                            webConfig.domStorageEnabled = true
                                            webConfig.mediaPlaybackRequiresUserGesture = false
                                            webConfig.loadWithOverviewMode = true
                                            webConfig.useWideViewPort = true
                                            webChromeClient = WebChromeClient()
                                            webViewClient = object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                    val reqUrl = request?.url?.toString() ?: ""
                                                    if (reqUrl.contains("youtube.com") || reqUrl.contains("googlevideo.com") || reqUrl.contains("youtube-nocookie.com")) {
                                                        return false
                                                    }
                                                    try {
                                                        val extIntent = Intent(Intent.ACTION_VIEW, Uri.parse(reqUrl))
                                                        ctx.startActivity(extIntent)
                                                    } catch (e: Exception) {}
                                                    return true
                                                }
                                            }

                                            val embedUrl = getYouTubeEmbedUrl(liveVideoUrl)
                                            val html = """
                                                <!DOCTYPE html>
                                                <html>
                                                <head>
                                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                                    <style>
                                                        * { margin:0; padding:0; box-sizing:border-box; }
                                                        html, body { width:100%; height:100%; background-color:#000000; overflow:hidden; display:flex; align-items:center; justify-content:center; }
                                                        iframe { width:100%; height:100%; border:none; }
                                                    </style>
                                                </head>
                                                <body>
                                                    <iframe 
                                                        src="$embedUrl" 
                                                        title="Shri Balaji Live Darbar"
                                                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                                                        allowfullscreen>
                                                    </iframe>
                                                </body>
                                                </html>
                                            """.trimIndent()

                                            loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // In-App player controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showInAppPlayer = false },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isHindi) "वीडियो बंद करें" else "Close Video", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(liveVideoUrl)).apply {
                                        setPackage("com.google.android.youtube")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(liveVideoUrl)))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC0000)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text(if (isHindi) "▶ YouTube में खोलें" else "▶ Open in YouTube", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    } else {
                        // 🪔 DIVINE DARBAR CARD: With Instant In-App Video Play Button!
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            border = BorderStroke(1.5.dp, SaffronPrimary)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = { showInAppPlayer = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("📺 ऐप के अंदर लाइव व पावन दर्शन वीडियो चलाएं", fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Spacer(Modifier.height(14.dp))

                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(Brush.radialGradient(listOf(Color(0xFFFFECB3), SaffronPrimary.copy(alpha = 0.3f))))
                                        .border(2.dp, SaffronPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🚩", fontSize = 36.sp)
                                }

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    text = if (isHindi) "॥ श्री बालाजी महाराज पावन दिव्य दरबार ॥" else "॥ Shri Balaji Maharaj Darbar ॥",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaroonPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (isHindi) "डूँगरा जाट, बुलन्दशहर • परम पूज्य गुरुजी तेजवीर सिंह जी" else "Dungra Jaat, Bulandshahr • Pujya Guruji",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(12.dp))

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFFFF3E0),
                                    border = BorderStroke(1.dp, SaffronPrimary)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⏳", fontSize = 14.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "वर्तमान में लाइव दर्शन विश्राम पर हैं" else "Live broadcast currently on recess",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                                    border = BorderStroke(1.dp, Color(0xFFFFF176))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text(
                                            text = if (isHindi) "🪔 दैनिक पावन आरती एवं दरबार समय-सारणी" else "🪔 Daily Aarti & Darbar Timings",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaroonPrimary
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        val timings = listOf(
                                            Pair("🌅 प्रातः मंगला आरती", "प्रातः 05:30 बजे"),
                                            Pair("☀️ दोपहर राजभोग आरती", "दोपहर 12:00 बजे"),
                                            Pair("🌆 सायं संध्या महाआरती", "सायं 07:00 बजे"),
                                            Pair("🚩 रविवार दिव्य दरबार व झाड़ा", "रविवार प्रातः 08:00 बजे से")
                                        )
                                        timings.forEach { (title, time) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(title, fontSize = 12.sp, color = Color(0xFF333333), fontWeight = FontWeight.Medium)
                                                Text(time, fontSize = 12.sp, color = MaroonPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    text = if (isHindi) "दरबार अथवा आरती लाइव शुरू होते ही यह स्क्रीन स्वतः लाइव वीडियो में बदल जाएगी। तब तक आप पावन चालीसा व भजन सुन सकते हैं।"
                                    else "When Live Darbar or Aarti begins, this screen automatically switches to Live video.",
                                    fontSize = 11.5.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )

                                Spacer(Modifier.height(16.dp))

                                Button(
                                    onClick = { selectedTabIndex = 1 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "🎵 पावन चालीसा व भजन सुनें (बैकग्राउंड प्लेयर)" else "🎵 Listen to Sacred Chalisas",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ashramSettings.youtubeChannelUrl.ifEmpty { "https://www.youtube.com/@ShriBalajiKripaDham" })).apply {
                                            setPackage("com.google.android.youtube")
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ashramSettings.youtubeChannelUrl.ifEmpty { "https://www.youtube.com/@ShriBalajiKripaDham" })))
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.2.dp, Color(0xFFCC0000))
                                ) {
                                    Text(
                                        text = if (isHindi) "▶ यूट्यूब चैनल पर पिछले पावन वीडियो देखें" else "▶ Watch Past Videos on YouTube",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFCC0000)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Darbar Status Badge
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ashramSettings.isDarbarActive) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (ashramSettings.isDarbarActive) Color(0xFF4CAF50) else SaffronPrimary
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (ashramSettings.isDarbarActive) "🟢" else "⏳",
                                fontSize = 20.sp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (ashramSettings.isDarbarActive)
                                        (if (isHindi) "पावन दरबार लाइव सक्रिय है" else "Live Darbar is Currently Active")
                                    else
                                        (if (isHindi) "आगामी दरबार की प्रतीक्षा" else "Awaiting Next Darbar"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (ashramSettings.isDarbarActive) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                                Text(
                                    text = ashramSettings.darbarTimings.ifBlank { "प्रत्येक रविवार प्रातः 8:00 बजे से" },
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Divine Ashram Notice
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isHindi) "॥ पावन दर्शन नियम एवं सूचना ॥" else "Darshan Information",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaroonPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (isHindi)
                                    "• रविवार प्रातःकाल 8:00 बजे से पूज्य गुरुजी द्वारा दिव्य दरबार प्रारंभ होता है।\n• घर बैठे भक्तगण लाइव दर्शन व महाआरती का पावन लाभ प्राप्त कर सकते हैं।\n• दरबार में टोकन वाले भक्त अपनी बारी पर ही गुरुजी के समीप पधारें।"
                                else
                                    "• Sunday Darbar starts at 8:00 AM by Pujya Guruji.\n• Devotees from home can watch Live Darbar and Aarti.\n• Token holders please proceed when your number is called.",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                // ============================================================
                // TAB 1: SACRED AUDIO, CHALISA & AARTI PLAYER
                // ============================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    if (tracksList.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaroonPrimary)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFD54F)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🚩", fontSize = 36.sp)
                                }
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = if (isHindi) "श्री बालाजी कृपा धाम (डूँगरा जाट)" else "Shri Balaji Kripa Dham",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (isHindi)
                                        "🚩 आश्रम प्रबंधन द्वारा प्रामाणिक आरती व भजन शीघ्र प्रकाशित किए जाएंगे। कृपया प्रतीक्षा करें।"
                                    else
                                        "🚩 Sacred Aartis & Bhajans will be published soon by Ashram Administration. Please wait.",
                                    fontSize = 13.sp,
                                    color = Color(0xFFFFD54F),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(18.dp))
                                if (isTracksLoading) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFFFD54F),
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isTracksLoading = true
                                                val (ok, list) = repository.syncSacredTracksFromHostinger(admin = false)
                                                if (ok && list.isNotEmpty()) {
                                                    tracksList = list
                                                    Toast.makeText(context, "✅ आरतियाँ लोड हो गईं!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "अभी कोई नई आरती प्रकाशित नहीं है।", Toast.LENGTH_SHORT).show()
                                                }
                                                isTracksLoading = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("🔄 ताज़ा करें (Refresh)", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        val safeTrackIndex = currentTrackIndex.coerceIn(0, tracksList.size - 1)
                        val activeTrack = tracksList[safeTrackIndex]

                        // Player Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaroonPrimary)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.sweepGradient(
                                                listOf(
                                                    SaffronPrimary,
                                                    Color(0xFFFFD54F),
                                                    MaroonPrimary,
                                                    SaffronPrimary
                                                )
                                            )
                                        )
                                        .border(3.dp, Color(0xFFFFD54F), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isPlaying) "🕉️" else "🚩",
                                        fontSize = 36.sp,
                                        modifier = if (isPlaying) Modifier.scale(pulseScale) else Modifier
                                    )
                                }

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = if (isHindi) activeTrack.titleHindi else activeTrack.titleEnglish,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = activeTrack.subtitleHindi,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFD54F),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(10.dp))

                                // Action buttons: Read Lyrics & Watch on YouTube
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    OutlinedButton(
                                        onClick = { showLyricsDialog = activeTrack },
                                        border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("📖 सम्पूर्ण पाठ पढ़ें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Button(
                                        onClick = { openTrackInYouTube(activeTrack) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC0000)),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("▶ यूट्यूब पर सुनें", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Seek Bar
                                Slider(
                                    value = currentPositionMs.toFloat().coerceIn(0f, totalDurationMs.coerceAtLeast(1).toFloat()),
                                    onValueChange = { newPos ->
                                        BhajanAudioService.seekTo(context, newPos.toInt())
                                    },
                                    valueRange = 0f..totalDurationMs.coerceAtLeast(1).toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFD54F),
                                        activeTrackColor = Color(0xFFFFD54F),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatTime(currentPositionMs),
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = formatTime(totalDurationMs),
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                // Controls Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { isLooping = !isLooping }) {
                                        Text(
                                            text = "🔁",
                                            fontSize = 20.sp,
                                            color = if (isLooping) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.5f)
                                        )
                                    }

                                    IconButton(onClick = {
                                        val prevIdx = if (safeTrackIndex - 1 < 0) tracksList.size - 1 else safeTrackIndex - 1
                                        playTrack(prevIdx)
                                    }) {
                                        Text(text = "⏮", fontSize = 24.sp, color = Color.White)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFD54F))
                                            .clickable { togglePlayPause() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isBuffering) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = MaroonPrimary,
                                                strokeWidth = 3.dp
                                            )
                                        } else {
                                            Text(
                                                text = if (isPlaying) "⏸" else "▶",
                                                fontSize = 24.sp,
                                                color = MaroonPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    IconButton(onClick = {
                                        val nextIdx = (safeTrackIndex + 1) % tracksList.size
                                        playTrack(nextIdx)
                                    }) {
                                        Text(text = "⏭", fontSize = 24.sp, color = Color.White)
                                    }

                                    IconButton(onClick = {
                                        BhajanAudioService.stopPlayback(context)
                                    }) {
                                        Text(text = "⏹", fontSize = 20.sp, color = Color.White.copy(alpha = 0.7f))
                                    }
                                }

                                if (playbackErrorMessage != null) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = playbackErrorMessage ?: "",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFFCC80),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Offline Fast Cache Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                            border = BorderStroke(1.dp, Color(0xFF81C784))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "⚡ 0.0s बफरिंग ऑफ़लाइन ऑडियो",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF1B5E20)
                                    )
                                    Text(
                                        text = "डिवाइस में सुरक्षित: ${DevotionalAudioCacheManager.getTotalCacheSizeFormatted(context)} (बिना इंटरनेट 100% चलेगा)",
                                        fontSize = 10.5.sp,
                                        color = Color.DarkGray
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            Toast.makeText(context, "📥 सभी आरतियाँ ऑफ़लाइन डाउनलोड हो रही हैं...", Toast.LENGTH_SHORT).show()
                                            for (t in tracksList) {
                                                if (t.audioUrl.isNotBlank() && !DevotionalAudioCacheManager.isTrackCached(context, t.trackKey)) {
                                                    DevotionalAudioCacheManager.downloadTrackForOffline(context, t.trackKey, t.audioUrl)
                                                }
                                            }
                                            cacheRefreshCounter++
                                            Toast.makeText(context, "✅ सभी पावन आरतियाँ ऑफ़लाइन सुरक्षित हो गईं!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                ) {
                                    Text("📥 सभी सेव करें", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHindi) "पावन आरतियाँ, चालीसा एवं स्तुतियाँ (${tracksList.size})" else "Sacred Aartis & Chalisas (${tracksList.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaroonPrimary
                            )

                            if (isVaniReciting) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SaffronPrimary,
                                    modifier = Modifier.clickable {
                                        SacredOfflineVaniEngine.stop()
                                    }
                                ) {
                                    Text(
                                        text = "⏹ वाणी पाठ रोकें",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Track List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(tracksList) { index, track ->
                                val isCurrent = safeTrackIndex == index
                                val isOfflineCached = remember(track.trackKey, cacheRefreshCounter) {
                                    DevotionalAudioCacheManager.isTrackCached(context, track.trackKey)
                                }
                                var isDownloadingThis by remember { mutableStateOf(false) }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { playTrack(index) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrent) Color(0xFFFFF3E0) else Color.White
                                    ),
                                    border = if (isCurrent) BorderStroke(1.5.dp, SaffronPrimary) else null,
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrent) SaffronPrimary else Color(0xFFF5F5F5)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isCurrent && isPlaying) "▶" else "${index + 1}",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) Color.White else Color.DarkGray,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (isHindi) track.titleHindi else track.titleEnglish,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = if (isCurrent) MaroonPrimary else Color.Black
                                                )
                                                if (isOfflineCached) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFE8F5E9)
                                                    ) {
                                                        Text(
                                                            text = "⚡ 0s ऑफ़लाइन",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF2E7D32),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = track.subtitleHindi,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF333333),
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (!isOfflineCached) {
                                            if (isDownloadingThis) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaroonPrimary
                                                )
                                            } else {
                                                IconButton(onClick = {
                                                    if (track.audioUrl.isBlank()) {
                                                        Toast.makeText(context, "इस ट्रैक का ऑडियो उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
                                                        return@IconButton
                                                    }
                                                    isDownloadingThis = true
                                                    scope.launch {
                                                        val ok = DevotionalAudioCacheManager.downloadTrackForOffline(
                                                            context, track.trackKey, track.audioUrl
                                                        )
                                                        isDownloadingThis = false
                                                        if (ok) {
                                                            cacheRefreshCounter++
                                                            Toast.makeText(context, "✅ '${track.titleHindi}' ऑफ़लाइन सुरक्षित हो गई (0s बफरिंग)!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "डाउनलोड विफल। कृपया इंटरनेट जांचें।", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }) {
                                                    Text("📥", fontSize = 16.sp)
                                                }
                                            }
                                        }

                                        IconButton(onClick = { showLyricsDialog = track }) {
                                            Text("📖", fontSize = 18.sp)
                                        }

                                        IconButton(onClick = { openTrackInYouTube(track) }) {
                                            Text("▶", fontSize = 16.sp, color = Color(0xFFCC0000))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sacred Text / Lyrics Reader Dialog
    if (showLyricsDialog != null) {
        val track = showLyricsDialog!!
        AlertDialog(
            onDismissRequest = { showLyricsDialog = null },
            title = {
                Text(
                    text = track.titleHindi,
                    fontWeight = FontWeight.Bold,
                    color = MaroonPrimary,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = track.lyricsHindi,
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        color = Color(0xFF212121),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLyricsDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonAccent)
                ) {
                    Text("जय श्री बालाजी", color = Color.White)
                }
            },
            dismissButton = {
                Row {
                    Button(
                        onClick = {
                            SacredOfflineVaniEngine.startRecitation(context, track.lyricsHindi)
                            Toast.makeText(context, "🪔 100% ऑफ़लाइन पाठ वाचन प्रारंभ हो गया...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                    ) {
                        Text("🪔 पाठ वाचन", color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = {
                            openTrackInYouTube(track)
                        }
                    ) {
                        Text("▶", color = Color(0xFFCC0000))
                    }
                }
            }
        )
    }
}
