package com.example.shribalajikripadham.ui.admin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LiveCameraFilter(val titleHindi: String, val icon: String) {
    NATURAL("प्राकृतिक HD", "🌿"),
    GOLDEN_AURA("स्वर्णिम आभा", "🌟"),
    DIYA_GLOW("आरती दीप प्रकाश", "🪔"),
    DEVOTIONAL_BLOOM("भक्ति कांति", "✨"),
    TEMPLE_FRAME("मंदिर तोरण फ्रेम", "🌺"),
    ROYAL_RADIANCE("रॉयल प्रसादम ग्लो", "👑")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLiveStudioTab(
    isHindi: Boolean,
    repository: AshramRepository,
    settings: AshramSettings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLiveActive by remember { mutableStateOf(settings.isDarbarLiveNow) }
    var liveTitle by remember { mutableStateOf(if (settings.isDarbarActive) "🔴 आज का पावन दिव्य दरबार व महाआरती लाइव दर्शन - धाम डूँगरा जाट" else "🔴 श्री बालाजी कृपा धाम लाइव दर्शन") }
    var streamUrlInput by remember { mutableStateOf(settings.youtubeChannelUrl) }
    var ytLiveKeyInput by remember { mutableStateOf(settings.youtubeChannelUrl) }
    var fbLiveKeyInput by remember { mutableStateOf(settings.facebookPageUrl) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var isMicMuted by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(LiveCameraFilter.GOLDEN_AURA) }
    var showWatermark by remember { mutableStateOf(true) }
    var showSettingsTray by remember { mutableStateOf(false) }

    var liveDurationSeconds by remember { mutableIntStateOf(0) }
    var showSocialModal by remember { mutableStateOf(false) }
    var showEndLiveConfirm by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    // Live broadcast timer
    LaunchedEffect(isLiveActive) {
        if (isLiveActive) {
            liveDurationSeconds = 0
            while (isLiveActive) {
                delay(1000)
                liveDurationSeconds++
            }
        }
    }

    fun formatDuration(totalSecs: Int): String {
        val hrs = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hrs > 0) "%02d:%02d:%02d".format(hrs, mins, secs) else "%02d:%02d".format(mins, secs)
    }

    // Pulse animation for live badge
    val infiniteTransition = rememberInfiniteTransition(label = "AdminLivePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AdminLivePulseAnim"
    )

    // PRO FULL-SCREEN EDGE-TO-EDGE STUDIO
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. FULL SCREEN CAMERA PREVIEW
        if (hasCameraPermission) {
            Camera2PreviewView(
                isFrontCamera = isFrontCamera,
                isTorchOn = isTorchOn,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 48.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (isHindi) "लाइव प्रसारण हेतु कैमरा अनुमति आवश्यक है" else "Camera permission required for live broadcast",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                    ) {
                        Text(if (isHindi) "कैमरा अनुमति दें" else "Grant Permission")
                    }
                }
            }
        }

        // 2. FULL-SCREEN PRO DEVOTIONAL & BEAUTY FILTERS
        when (selectedFilter) {
            LiveCameraFilter.GOLDEN_AURA -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0x33FFD54F),
                                    Color(0x55FFA000),
                                    Color(0x66FF6F00)
                                )
                            )
                        )
                )
            }
            LiveCameraFilter.DIYA_GLOW -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0x15FFF9C4),
                                    Color(0x35FFE082),
                                    Color(0x70E65100)
                                )
                            )
                        )
                )
            }
            LiveCameraFilter.DEVOTIONAL_BLOOM -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x30FFF8E1),
                                    Color(0x10FFFFFF),
                                    Color(0x40FFD54F)
                                )
                            )
                        )
                )
            }
            LiveCameraFilter.ROYAL_RADIANCE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0x20FFE082),
                                    Color(0x30FFB300),
                                    Color(0x45E65100)
                                )
                            )
                        )
                )
            }
            LiveCameraFilter.TEMPLE_FRAME -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .background(
                                Brush.verticalGradient(listOf(MaroonAccent, MaroonPrimary.copy(alpha = 0.85f)))
                            )
                            .border(1.5.dp, Color(0xFFFFD54F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🚩 ॥ श्री बालाजी कृपा धाम (डूँगरा जाट) ॥ 🚩",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .background(
                                Brush.verticalGradient(listOf(MaroonPrimary.copy(alpha = 0.85f), MaroonAccent))
                            )
                            .border(1.5.dp, Color(0xFFFFD54F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🪔 परम पूज्य गुरुजी तेजवीर सिंह जी • दिव्य दरबार 🪔",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            LiveCameraFilter.NATURAL -> {}
        }

        // 3. WATERMARK OVERLAY
        if (showWatermark) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 54.dp, end = 12.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🕉️", fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "श्री बालाजी कृपा धाम",
                        color = Color(0xFFFFD54F),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. FLOATING TOP HUD (Status + Quick Action Icons)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.55f),
            border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Live Status Pill + Duration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isLiveActive) Color(0xFFD32F2F) else Color(0xFF455A64)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(if (isLiveActive) pulseScale else 1f)
                                    .background(Color.White, CircleShape)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (isLiveActive) "LIVE ${formatDuration(liveDurationSeconds)}" else "STANDBY",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "👥 148 भक्त जुड़े हैं",
                        color = Color(0xFFFFD54F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Quick Camera / Flash / Mic / Social Toggles
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Switch Camera
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { isFrontCamera = !isFrontCamera }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🔄", fontSize = 15.sp)
                        }
                    }

                    // Mic Mute Toggle
                    Surface(
                        shape = CircleShape,
                        color = if (isMicMuted) Color.Red.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { isMicMuted = !isMicMuted }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (isMicMuted) "🔇" else "🎙️", fontSize = 15.sp)
                        }
                    }

                    // Torch (only back cam)
                    if (!isFrontCamera) {
                        Surface(
                            shape = CircleShape,
                            color = if (isTorchOn) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { isTorchOn = !isTorchOn }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (isTorchOn) "🔦" else "💡", fontSize = 15.sp)
                            }
                        }
                    }

                    // Social Multi-Stream Dialog
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF673AB7).copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { showSocialModal = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🌐", fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // 5. FLOATING BOTTOM STUDIO CONTROLS HUD
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // PRO FILTER CHIPS ROW (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiveCameraFilter.values().forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) SaffronPrimary else Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(filter.icon, fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = filter.titleHindi,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Expandable Title & Direct External Stream Link Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Title Bar (Compact)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = liveTitle,
                            onValueChange = { liveTitle = it },
                            placeholder = { Text("प्रसारण शीर्षक (Live Title)...", fontSize = 11.5.sp, color = Color.LightGray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SaffronPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f).height(46.dp)
                        )

                        Spacer(Modifier.width(6.dp))

                        // Toggle settings tray (for Direct External URL)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (showSettingsTray) SaffronPrimary else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.clickable { showSettingsTray = !showSettingsTray }
                        ) {
                            Text(
                                text = "🔗 लिंक",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Direct Stream / RTMP / YouTube Live URL Input
                    if (showSettingsTray) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = streamUrlInput,
                            onValueChange = { streamUrlInput = it },
                            placeholder = { Text("सीधा YouTube/Facebook/RTMP लाइव लिंक डालें...", fontSize = 11.sp, color = Color.LightGray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD54F),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        )
                        Text(
                            text = "💡 लिंक डालने पर सभी भक्तों के फोन पर एक क्लिक में यह लाइव प्रसारण शुरू होगा!",
                            fontSize = 10.sp,
                            color = Color(0xFFFFD54F),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // BIG BROADCAST ACTION BUTTON
            if (!isLiveActive) {
                Button(
                    onClick = {
                        if (liveTitle.isBlank()) {
                            Toast.makeText(context, "कृपया लाइव शीर्षक दर्ज करें!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            val effectiveUrl = streamUrlInput.trim().ifBlank {
                                settings.youtubeChannelUrl.trim().ifEmpty { "https://www.youtube.com/@ShriBalajiKripaDham" }
                            }
                            val (ok, msg) = repository.updateLiveStreamingStatus(
                                isLive = true,
                                title = liveTitle.trim(),
                                liveUrl = effectiveUrl,
                                ytUrl = ytLiveKeyInput.trim(),
                                fbUrl = fbLiveKeyInput.trim()
                            )
                            if (ok) {
                                isLiveActive = true
                                Toast.makeText(context, "🔴 लाइव शुरू हो गया! सभी भक्तों को सूचना भेज दी गई है।", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(
                        text = "🔴 लाइव प्रसारण शुरू करें (GO LIVE)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            } else {
                Button(
                    onClick = { showEndLiveConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
                ) {
                    Text(
                        text = "⏹️ लाइव समाप्त करें (END LIVE STREAM)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    // --- END LIVE CONFIRMATION DIALOG ---
    if (showEndLiveConfirm) {
        AlertDialog(
            onDismissRequest = { showEndLiveConfirm = false },
            title = { Text("⏹️ लाइव समाप्त करने की पुष्टि", fontWeight = FontWeight.Bold) },
            text = {
                Text("क्या आप निश्चित रूप से लाइव प्रसारण समाप्त करना चाहते हैं? प्रसारण समाप्त होते ही भक्तों के ऐप पर लाइव स्टेटस विश्राम पर आ जाएगा।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val (ok, msg) = repository.updateLiveStreamingStatus(
                                isLive = false,
                                title = liveTitle.trim()
                            )
                            isLiveActive = false
                            showEndLiveConfirm = false
                            Toast.makeText(context, "⏹️ लाइव प्रसारण समाप्त कर दिया गया।", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("हाँ, समाप्त करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEndLiveConfirm = false }) {
                    Text("चालू रखें")
                }
            }
        )
    }

    // --- SOCIAL MEDIA SETTINGS DIALOG ---
    if (showSocialModal) {
        AlertDialog(
            onDismissRequest = { showSocialModal = false },
            title = {
                Text("🌐 सोशल मीडिया चैनल्स व RTMP सेटअप", fontWeight = FontWeight.Bold, color = MaroonPrimary)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ytLiveKeyInput,
                        onValueChange = { ytLiveKeyInput = it },
                        label = { Text("यूट्यूब चैनल या लाइव URL") },
                        singleLine = true,
                        colors = sacredOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = fbLiveKeyInput,
                        onValueChange = { fbLiveKeyInput = it },
                        label = { Text("फेसबुक पेज या लाइव URL") },
                        singleLine = true,
                        colors = sacredOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7))
                    ) {
                        Text(
                            text = "ℹ️ सूचना: जब आप '🔴 लाइव शुरू करें' दबाएंगे तो यह लिंक स्वतः सभी भक्तों के ऐप और होम स्क्रीन पर लाइव दिखेगा।",
                            fontSize = 11.5.sp,
                            color = Color(0xFFF57F17),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSocialModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text("ठीक है")
                }
            }
        )
    }
}

/**
 * Camera2 Preview Surface using standard Android SDK TextureView
 */
@Composable
fun Camera2PreviewView(
    isFrontCamera: Boolean,
    isTorchOn: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    var cameraDevice by remember { mutableStateOf<CameraDevice?>(null) }
    var captureSession by remember { mutableStateOf<CameraCaptureSession?>(null) }

    DisposableEffect(isFrontCamera, isTorchOn) {
        onDispose {
            try {
                captureSession?.close()
                cameraDevice?.close()
            } catch (ignored: Exception) {}
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        try {
                            val targetLensFacing = if (isFrontCamera) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
                            var selectedCamId: String? = null

                            for (id in cameraManager.cameraIdList) {
                                val chars = cameraManager.getCameraCharacteristics(id)
                                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                                if (facing == targetLensFacing) {
                                    selectedCamId = id
                                    break
                                }
                            }
                            if (selectedCamId == null && cameraManager.cameraIdList.isNotEmpty()) {
                                selectedCamId = cameraManager.cameraIdList[0]
                            }

                            if (selectedCamId != null && ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                cameraManager.openCamera(selectedCamId, object : CameraDevice.StateCallback() {
                                    override fun onOpened(camera: CameraDevice) {
                                        cameraDevice = camera
                                        try {
                                            val surface = Surface(surfaceTexture)
                                            val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                            previewRequestBuilder.addTarget(surface)

                                            // Torch control if supported and back camera
                                            if (!isFrontCamera && isTorchOn) {
                                                previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                                            } else {
                                                previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                                            }

                                            camera.createCaptureSession(
                                                listOf(surface),
                                                object : CameraCaptureSession.StateCallback() {
                                                    override fun onConfigured(session: CameraCaptureSession) {
                                                        captureSession = session
                                                        try {
                                                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                                            session.setRepeatingRequest(previewRequestBuilder.build(), null, Handler(Looper.getMainLooper()))
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }

                                                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                                                },
                                                Handler(Looper.getMainLooper())
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }

                                    override fun onDisconnected(camera: CameraDevice) {
                                        camera.close()
                                    }

                                    override fun onError(camera: CameraDevice, error: Int) {
                                        camera.close()
                                    }
                                }, Handler(Looper.getMainLooper()))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        try {
                            captureSession?.close()
                            cameraDevice?.close()
                        } catch (ignored: Exception) {}
                        return true
                    }
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        }
    )
}
