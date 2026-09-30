package com.example.shribalajikripadham.ui.live

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 🔴 NATIVE BROADCAST-GRADE LIVE STREAMING PLAYER
 * 
 * Supports:
 * 1. Direct HLS (.m3u8), MP4, RTSP streaming using 100% Native AndroidX Media3 (ExoPlayer)
 * 2. Hardware-Accelerated YouTube Live Streams with zero cookies, zero external redirects,
 *    ambient glow, native controls overlay, fullscreen toggle, and floating devotional reactions.
 */
@OptIn(UnstableApi::class)
@Composable
fun NativeDarbarLivePlayer(
    modifier: Modifier = Modifier,
    streamUrl: String,
    title: String = "श्री बालाजी कृपा धाम • पावन दिव्य दरबार सजीव दर्शन",
    isDarbarLiveNow: Boolean = true,
    onClosePlayer: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity

    val cleanUrl = streamUrl.trim()
    val isDirectMediaStream = remember(cleanUrl) {
        cleanUrl.endsWith(".m3u8", ignoreCase = true) ||
        cleanUrl.contains(".m3u8?", ignoreCase = true) ||
        cleanUrl.endsWith(".mp4", ignoreCase = true) ||
        cleanUrl.contains(".mp4?", ignoreCase = true) ||
        cleanUrl.startsWith("rtmp://", ignoreCase = true) ||
        cleanUrl.startsWith("rtsp://", ignoreCase = true)
    }

    var isFullscreen by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }

    // Floating devotional blessings state
    val blessingItems = remember { mutableStateListOf<FloatingBlessing>() }

    // Pulsing live indicator animation
    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Handle Hardware Back button in Fullscreen
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
        activity?.let { act ->
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Fullscreen system bars toggling
    LaunchedEffect(isFullscreen) {
        activity?.let { act ->
            val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
            if (isFullscreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    fun spawnBlessing(emoji: String, text: String) {
        val id = System.currentTimeMillis() + (0..1000).random()
        blessingItems.add(FloatingBlessing(id = id, emoji = emoji, text = text))
        scope.launch {
            delay(2500)
            blessingItems.removeAll { it.id == id }
        }
    }

    val playerModifier = if (isFullscreen) {
        Modifier.fillMaxSize()
    } else {
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    }

    Card(
        modifier = playerModifier,
        shape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isDirectMediaStream) {
                // ========================================================
                // 1. NATIVE EXOPLAYER (MEDIA3) FOR HLS / DIRECT VIDEO
                // ========================================================
                key(retryTrigger) {
                    NativeExoPlayerContainer(
                        streamUrl = cleanUrl,
                        isMuted = isMuted,
                        onBuffering = { isBuffering = it },
                        onError = { playbackError = it }
                    )
                }
            } else {
                // ========================================================
                // 2. HARDWARE-ACCELERATED ZERO-COOKIE BROADCAST PLAYER
                // ========================================================
                key(retryTrigger) {
                    NativeHardwareLiveContainer(
                        streamUrl = cleanUrl,
                        onBuffering = { isBuffering = it },
                        onError = { playbackError = it }
                    )
                }
            }

            // ========================================================
            // 3. AMBIENT GLOW & GRADIENT TOP BAR
            // ========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Live Pulsing Dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(pulseScale)
                                .background(Color.Red, CircleShape)
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Red
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Refresh / Reconnect Stream Button
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    playbackError = null
                                    isBuffering = true
                                    retryTrigger++
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔄", fontSize = 14.sp)
                            }
                        }

                        // Fullscreen Toggle
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { isFullscreen = !isFullscreen }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isFullscreen) "↙" else "⛶",
                                    fontSize = 16.sp,
                                    color = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Close Button (if non-fullscreen)
                        if (!isFullscreen && onClosePlayer != null) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { onClosePlayer() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✕", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ========================================================
            // 4. BUFFERING SPINNER OVERLAY
            // ========================================================
            if (isBuffering && playbackError == null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = SaffronPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "पावन प्रसारण जुड़ रहा है...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ========================================================
            // 5. ERROR & RETRY OVERLAY
            // ========================================================
            if (playbackError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.88f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚩", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "दरबार वर्तमान में विश्राम पर है",
                            color = Color(0xFFFFD54F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "लाइव प्रसारण उपलब्ध नहीं है अथवा नेटवर्क धीमा है।",
                            color = Color.LightGray,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    playbackError = null
                                    isBuffering = true
                                    retryTrigger++
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("पुनः प्रयास करें", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                                            setPackage("com.google.android.youtube")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)))
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.White),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("YouTube में खोलें", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // ========================================================
            // 6. FLOATING DEVOTIONAL BLESSINGS ANIMATION
            // ========================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 44.dp, start = 12.dp, end = 12.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    blessingItems.takeLast(3).forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.emoji, fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    item.text,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ========================================================
            // 7. DEVOTIONAL QUICK REACTION BAR (BOTTOM OVERLAY)
            // ========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Quick Blessings Taps
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { spawnBlessing("🚩", "जय श्री बालाजी महाराज!") }
                        ) {
                            Text(
                                text = "🚩 जय बालाजी",
                                color = Color(0xFFFFD54F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { spawnBlessing("🪔", "पावन आरती दीप अर्पण!") }
                        ) {
                            Text(
                                text = "🪔 दीप अर्पण",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { spawnBlessing("🌸", "चरणों में पुष्प वंदना!") }
                        ) {
                            Text(
                                text = "🌸 पुष्प",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // YouTube Direct Launch Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFCC0000).copy(alpha = 0.85f),
                        modifier = Modifier.clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                                    setPackage("com.google.android.youtube")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)))
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("▶ YouTube", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 100% NATIVE EXOPLAYER CONTAINER (FOR HLS .m3u8 & DIRECT VIDEO)
 */
@OptIn(UnstableApi::class)
@Composable
private fun NativeExoPlayerContainer(
    streamUrl: String,
    isMuted: Boolean,
    onBuffering: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(streamUrl) {
        val player = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(streamUrl)
            setMediaItem(mediaItem)
            volume = if (isMuted) 0f else 1f
            playWhenReady = true
            prepare()

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> onBuffering(true)
                        Player.STATE_READY -> onBuffering(false)
                        Player.STATE_ENDED -> onBuffering(false)
                        Player.STATE_IDLE -> {}
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    onBuffering(false)
                    onError(error.message ?: "प्रसारण त्रुटि")
                }
            })
        }
        exoPlayer = player

        onDispose {
            player.release()
            exoPlayer = null
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                useController = false // We provide custom devotional UI controls
                this.player = exoPlayer
            }
        },
        update = { view ->
            view.player = exoPlayer
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * HARDWARE-ACCELERATED ZERO-COOKIE BROADCAST EMBED CONTAINER
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun NativeHardwareLiveContainer(
    streamUrl: String,
    onBuffering: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    val embedUrl = remember(streamUrl) {
        extractEmbedUrl(streamUrl)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(0xFF000000.toInt())
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    allowFileAccess = false
                    allowContentAccess = false
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onBuffering(newProgress < 85)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onBuffering(false)
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val req = request?.url?.toString() ?: ""
                        if (req.contains("youtube-nocookie.com") || req.contains("googlevideo.com") || req.contains("youtube.com/embed")) {
                            return false
                        }
                        return true // Block external ad redirects
                    }
                }

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <style>
                            * { margin:0; padding:0; box-sizing:border-box; }
                            html, body { width:100%; height:100%; background:#000; overflow:hidden; display:flex; align-items:center; justify-content:center; }
                            iframe { width:100%; height:100%; border:none; outline:none; }
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

/**
 * Clean YouTube Embed URL Extractor
 */
private fun extractEmbedUrl(url: String): String {
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

private data class FloatingBlessing(
    val id: Long,
    val emoji: String,
    val text: String
)
