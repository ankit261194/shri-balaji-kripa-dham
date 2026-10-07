package com.example.shribalajikripadham.ui.feedback

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.shribalajikripadham.data.model.AshramSevadarContact
import com.example.shribalajikripadham.ui.common.SacredAvatar
import com.example.shribalajikripadham.util.InAppVoiceCallManager

@Composable
fun InAppVoiceCallDialog(
    sevadar: AshramSevadarContact,
    callerName: String = "भक्त",
    callerPhone: String = "9100100251",
    callerRole: String = "DEVOTEE",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var callState by remember { mutableStateOf(InAppVoiceCallManager.CallState.DIALING) }
    var statusText by remember { mutableStateOf("कॉल स्थापित हो रहा है...") }
    var durationSec by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(InAppVoiceCallManager.isMuted) }
    var isSpeakerOn by remember { mutableStateOf(InAppVoiceCallManager.isSpeakerOn) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            InAppVoiceCallManager.startCall(
                context = context,
                sevadar = sevadar,
                callerName = callerName,
                callerPhone = callerPhone,
                callerRole = callerRole
            )
        } else {
            statusText = "माइक्रोफोन अनुमति आवश्यक है"
        }
    }

    // Connect Call Manager callbacks
    DisposableEffect(sevadar.id) {
        InAppVoiceCallManager.onStateChanged = { state, msg ->
            callState = state
            statusText = msg
            if (state == InAppVoiceCallManager.CallState.ENDED) {
                // Auto close dialog after brief display
            }
        }
        InAppVoiceCallManager.onDurationTick = { sec ->
            durationSec = sec
        }

        if (hasAudioPermission) {
            InAppVoiceCallManager.startCall(
                context = context,
                sevadar = sevadar,
                callerName = callerName,
                callerPhone = callerPhone,
                callerRole = callerRole
            )
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        onDispose {
            if (InAppVoiceCallManager.currentState != InAppVoiceCallManager.CallState.ENDED) {
                InAppVoiceCallManager.endCall(context, reason = "उपयोगकर्ता द्वारा समाप्त")
            }
        }
    }

    // Auto dismiss after call ends
    LaunchedEffect(callState) {
        if (callState == InAppVoiceCallManager.CallState.ENDED) {
            kotlinx.coroutines.delay(1600)
            onDismiss()
        }
    }

    // Pulsing Wave Animation for Calling / Live audio
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha"
    )

    Dialog(
        onDismissRequest = {
            InAppVoiceCallManager.endCall(context, reason = "उपयोगकर्ता द्वारा बंद")
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF240404),
                            Color(0xFF4A0A0A),
                            Color(0xFF2B0000),
                            Color(0xFF120000)
                        )
                    )
                )
        ) {
            // Background Spiritual Glow Circles
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.Center)
                    .scale(if (callState == InAppVoiceCallManager.CallState.RINGING || callState == InAppVoiceCallManager.CallState.CONNECTED) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9900).copy(alpha = if (callState == InAppVoiceCallManager.CallState.CONNECTED) 0.15f else waveAlpha))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Surface(
                        color = Color(0x33FFD700),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFD700))
                    ) {
                        Text(
                            text = "🚩 श्री बालाजी कृपा धाम • सेवादार लाइव वॉइस कॉल",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = sevadar.name,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${sevadar.department} • आधिकारिक सहायता",
                        color = Color(0xFFFFCC80),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Call Status Pill
                    Surface(
                        color = when (callState) {
                            InAppVoiceCallManager.CallState.CONNECTED -> Color(0xFF1B5E20).copy(alpha = 0.8f)
                            InAppVoiceCallManager.CallState.ENDED -> Color(0xFFB71C1C).copy(alpha = 0.8f)
                            else -> Color(0xFFE65100).copy(alpha = 0.8f)
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when (callState) {
                                    InAppVoiceCallManager.CallState.CONNECTED -> "🟢 "
                                    InAppVoiceCallManager.CallState.ENDED -> "🔴 "
                                    else -> "🔔 "
                                },
                                fontSize = 12.sp
                            )
                            Text(
                                text = statusText,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Live Call Duration Counter
                    if (callState == InAppVoiceCallManager.CallState.CONNECTED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val mins = durationSec / 60
                        val secs = durationSec % 60
                        val timeStr = String.format("%02d:%02d", mins, secs)
                        Text(
                            text = "⏱️ $timeStr",
                            color = Color(0xFFFFE082),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Center Portrait with Glowing Aura
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    // Outer Ring
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            3.dp,
                            if (callState == InAppVoiceCallManager.CallState.CONNECTED) Color(0xFF4CAF50) else Color(0xFFFFB300)
                        ),
                        modifier = Modifier
                            .size(190.dp)
                            .scale(if (callState == InAppVoiceCallManager.CallState.CONNECTED) pulseScale else 1f)
                    ) {}

                    // Avatar Image with SacredAvatar
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF3E0A0A),
                        border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFFFD54F)),
                        modifier = Modifier.size(160.dp)
                    ) {
                        SacredAvatar(
                            photoUri = sevadar.photoUri,
                            fallbackText = sevadar.name.ifBlank { "सेवादार" },
                            size = 160.dp
                        )
                    }
                }

                // Live Audio Waveform Bars (During Connected Call)
                if (callState == InAppVoiceCallManager.CallState.CONNECTED) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(30.dp)
                    ) {
                        val heights = listOf(14.dp, 26.dp, 18.dp, 28.dp, 12.dp, 24.dp, 16.dp)
                        heights.forEachIndexed { idx, h ->
                            val barScale by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(400 + (idx * 90), easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bar$idx"
                            )
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(h * if (isMuted) 0.3f else barScale)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isMuted) Color.Gray else Color(0xFFFFD54F))
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(30.dp))
                }

                // Bottom Action Controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Microphone Mute Toggle
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = if (isMuted) Color(0xFFC62828) else Color(0x33FFFFFF),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .size(58.dp)
                                    .clickable {
                                        isMuted = InAppVoiceCallManager.toggleMute(context)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (isMuted) "🔇" else "🎤", fontSize = 24.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isMuted) "अनम्यूट" else "माइक म्यूट",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // 2. Big Red Hangup Button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFD32F2F),
                                border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
                                shadowElevation = 10.dp,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clickable {
                                        InAppVoiceCallManager.endCall(context, reason = "कॉल समाप्त")
                                        onDismiss()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("📞", fontSize = 32.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "कॉल काटें",
                                color = Color(0xFFFF8A80),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 3. Speaker Toggle
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSpeakerOn) Color(0xFF2E7D32) else Color(0x33FFFFFF),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .size(58.dp)
                                    .clickable {
                                        isSpeakerOn = InAppVoiceCallManager.toggleSpeaker(context)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (isSpeakerOn) "🔊" else "🔈", fontSize = 24.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isSpeakerOn) "स्पीकर ऑन" else "ईयरपीस",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
