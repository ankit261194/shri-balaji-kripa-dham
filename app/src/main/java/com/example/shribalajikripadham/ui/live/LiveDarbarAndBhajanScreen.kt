package com.example.shribalajikripadham.ui.live

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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

data class SacredTrack(
    val titleHindi: String,
    val titleEnglish: String,
    val subtitleHindi: String,
    val durationText: String,
    val audioUrl: String
)

val SACRED_TRACKS = listOf(
    SacredTrack(
        titleHindi = "श्री हनुमान चालीसा",
        titleEnglish = "Shri Hanuman Chalisa",
        subtitleHindi = "जय हनुमान ज्ञान गुन सागर • संकट कटे मिटे सब पीरा",
        durationText = "09:42",
        audioUrl = "https://shribalajikripadham.online/media/hanuman_chalisa.mp3"
    ),
    SacredTrack(
        titleHindi = "श्री बालाजी महाआरती (डूँगरा जाट)",
        titleEnglish = "Shri Balaji Maha Aarti",
        subtitleHindi = "आरती कीजै श्री बालाजी की • कलिकाल में मंगलकारी",
        durationText = "06:15",
        audioUrl = "https://shribalajikripadham.online/media/balaji_aarti.mp3"
    ),
    SacredTrack(
        titleHindi = "बजरंग बाण",
        titleEnglish = "Bajrang Baan",
        subtitleHindi = "निश्चय प्रेम प्रतीति ते बिनय करै सनमान",
        durationText = "07:30",
        audioUrl = "https://shribalajikripadham.online/media/bajrang_baan.mp3"
    ),
    SacredTrack(
        titleHindi = "संकट मोचन हनुमानाष्टक",
        titleEnglish = "Sankat Mochan Hanumanashtak",
        subtitleHindi = "बाल समय रवि भक्ष लियो तब तीनहुं लोक भयो अंधियारों",
        durationText = "05:48",
        audioUrl = "https://shribalajikripadham.online/media/sankatmochan.mp3"
    ),
    SacredTrack(
        titleHindi = "आरती कीजै हनुमान लला की",
        titleEnglish = "Aarti Kije Hanuman Lala Ki",
        subtitleHindi = "दुष्ट दलन रघुनाथ कला की • जाके बल से गिरिवर कांपै",
        durationText = "05:12",
        audioUrl = "https://shribalajikripadham.online/media/aarti_kije.mp3"
    ),
    SacredTrack(
        titleHindi = "श्री रामचन्द्र कृपालु भजु मन",
        titleEnglish = "Shri Ramachandra Kripalu",
        subtitleHindi = "हरन भवभय दारुणं • नवकंज लोचन कंज मुख",
        durationText = "06:35",
        audioUrl = "https://shribalajikripadham.online/media/ram_stuti.mp3"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDarbarAndBhajanScreen(
    isHindi: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    var ashramSettings by remember { mutableStateOf(AshramSettings()) }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Live Video, 1: Sacred Audio

    // Audio Player State
    var currentTrackIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(1) }
    var isLooping by remember { mutableStateOf(false) }
    var playbackErrorMessage by remember { mutableStateOf<String?>(null) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(Unit) {
        ashramSettings = repository.getSettings()
    }

    // Release player on exit
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Playback Progress Timer
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            try {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        currentPositionMs = mp.currentPosition
                        totalDurationMs = mp.duration.coerceAtLeast(1)
                    }
                }
            } catch (e: Exception) {}
            delay(500)
        }
    }

    fun playTrack(index: Int) {
        try {
            playbackErrorMessage = null
            currentTrackIndex = index
            isBuffering = true

            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            val track = SACRED_TRACKS[index]
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(track.audioUrl)
                setOnPreparedListener { player ->
                    isBuffering = false
                    totalDurationMs = player.duration
                    player.start()
                    isPlaying = true
                }
                setOnCompletionListener {
                    if (isLooping) {
                        it.seekTo(0)
                        it.start()
                    } else {
                        // Play next
                        val nextIdx = (currentTrackIndex + 1) % SACRED_TRACKS.size
                        playTrack(nextIdx)
                    }
                }
                setOnErrorListener { _, _, _ ->
                    isBuffering = false
                    isPlaying = false
                    playbackErrorMessage = if (isHindi) 
                        "ऑडियो लोड करने में असमर्थ। कृपया इंटरनेट कनेक्शन जांचें।" 
                    else 
                        "Unable to stream audio. Please check internet connection."
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            isBuffering = false
            isPlaying = false
            playbackErrorMessage = e.localizedMessage
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.pause()
                    isPlaying = false
                } else {
                    mp.start()
                    isPlaying = true
                }
            } catch (e: Exception) {
                playTrack(currentTrackIndex)
            }
        } ?: run {
            playTrack(currentTrackIndex)
        }
    }

    fun formatTime(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }

    // Pulse animation for live badge
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
                    Column {
                        Text(
                            text = if (isHindi) "🔴 लाइव दर्शन व पावन भजन" else "Live Darbar & Bhajans",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isHindi) "श्री बालाजी कृपा धाम, डूँगरा जाट" else "Shri Balaji Kripa Dham",
                            fontSize = 12.sp,
                            color = Color(0xFFFFD54F)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "🚩 श्री बालाजी कृपा धाम, डूँगरा जाट लाइव दर्शन एवं पावन आरती/भजन ऐप में सुनें: https://shribalajikripadham.online"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "शेयर करें"))
                    }) {
                        Text("📤", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaroonPrimary
                )
            )
        },
        containerColor = Color(0xFFFFF8E7)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                            Text(
                                text = "🎵",
                                fontSize = 14.sp
                            )
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
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val liveVideoUrl = remember(ashramSettings.youtubeChannelUrl) {
                                val base = ashramSettings.youtubeChannelUrl.trim()
                                if (base.contains("youtube.com") || base.contains("youtu.be")) {
                                    // If channel URL, point to live stream directly
                                    if (base.contains("/@")) {
                                        "$base/live"
                                    } else {
                                        base
                                    }
                                } else {
                                    "https://www.youtube.com/@ShriBalajiKripaDham/live"
                                }
                            }

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
                                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                                return false // Stay inside WebView!
                                            }
                                        }
                                        loadUrl(liveVideoUrl)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

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

                    Spacer(Modifier.height(16.dp))

                    // External YouTube App Shortcut
                    OutlinedButton(
                        onClick = {
                            try {
                                val ytUrl = ashramSettings.youtubeChannelUrl.ifBlank { "https://www.youtube.com/@ShriBalajiKripaDham" }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonPrimary),
                        border = BorderStroke(1.5.dp, MaroonPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("▶️ YouTube ऐप में चैनल खोलें (Open YouTube App)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Notice
                    Text(
                        text = if (isHindi)
                            "नोट: जब दरबार लाइव नहीं होता, तो पूर्व दरबार के पावन सत्संग व दर्शन वीडियो ऊपर स्वतः प्रदर्शित होते हैं।"
                        else
                            "Note: When live stream is offline, sacred past Darbar recordings and bhajans are displayed.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

            } else {
                // ============================================================
                // TAB 1: SACRED AARTI & BHAJAN PLAYER
                // ============================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Active Player Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaroonPrimary)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val activeTrack = SACRED_TRACKS[currentTrackIndex]

                            // Animated Ring & Disc
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
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
                                    fontSize = 40.sp,
                                    modifier = if (isPlaying) Modifier.scale(pulseScale) else Modifier
                                )
                            }

                            Spacer(Modifier.height(12.dp))

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

                            // Seek Bar
                            Slider(
                                value = currentPositionMs.toFloat().coerceIn(0f, totalDurationMs.toFloat()),
                                onValueChange = { newPos ->
                                    currentPositionMs = newPos.toInt()
                                    mediaPlayer?.seekTo(newPos.toInt())
                                },
                                valueRange = 0f..totalDurationMs.toFloat(),
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

                            Spacer(Modifier.height(10.dp))

                            // Controls Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Loop button
                                IconButton(onClick = { isLooping = !isLooping }) {
                                    Text(
                                        text = "🔁",
                                        fontSize = 20.sp,
                                        color = if (isLooping) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.5f)
                                    )
                                }

                                // Previous
                                IconButton(onClick = {
                                    val prevIdx = if (currentTrackIndex - 1 < 0) SACRED_TRACKS.size - 1 else currentTrackIndex - 1
                                    playTrack(prevIdx)
                                }) {
                                    Text(
                                        text = "⏮",
                                        fontSize = 24.sp,
                                        color = Color.White
                                    )
                                }

                                // Play / Pause Button
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFD54F))
                                        .clickable { togglePlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isBuffering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = MaroonPrimary,
                                            strokeWidth = 3.dp
                                        )
                                    } else {
                                        Text(
                                            text = if (isPlaying) "⏸" else "▶",
                                            fontSize = 26.sp,
                                            color = MaroonPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Next
                                IconButton(onClick = {
                                    val nextIdx = (currentTrackIndex + 1) % SACRED_TRACKS.size
                                    playTrack(nextIdx)
                                }) {
                                    Text(
                                        text = "⏭",
                                        fontSize = 24.sp,
                                        color = Color.White
                                    )
                                }

                                // Stop
                                IconButton(onClick = {
                                    mediaPlayer?.stop()
                                    isPlaying = false
                                    currentPositionMs = 0
                                }) {
                                    Text(
                                        text = "⏹",
                                        fontSize = 22.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            if (playbackErrorMessage != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = playbackErrorMessage ?: "",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF8A80),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = if (isHindi) "पावन आरतियाँ एवं स्तुतियाँ (Playlists)" else "Sacred Aartis & Chalisas",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary
                    )

                    Spacer(Modifier.height(8.dp))

                    // Track List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(SACRED_TRACKS) { index, track ->
                            val isCurrent = currentTrackIndex == index
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
                                        Text(
                                            text = if (isHindi) track.titleHindi else track.titleEnglish,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isCurrent) MaroonPrimary else Color.Black
                                        )
                                        Text(
                                            text = track.subtitleHindi,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = track.durationText,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
