package com.example.shribalajikripadham.ui.admin

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.Token
import com.example.shribalajikripadham.data.model.TokenStatus
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.util.AshramVoiceAnnouncementManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 👑 पूज्य गुरुजी दरबार स्क्रीन (Elderly-Friendly Giant Screen Darbar Mode)
 *
 * विशेष रूप से बुजुर्ग गुरुजी के लिए तैयार किया गया अल्ट्रा-सिंपल बिग स्क्रीन मोड:
 * 1. स्क्रीन पर कोई जटिल मेन्यू या छोटे बटन नहीं हैं।
 * 2. वर्तमान टोकन नंबर और भक्त का नाम विशालकाय अक्षरों में दिखता है।
 * 3. गुरुजी बस स्क्रीन के बड़े हिस्से पर कहीं भी हाथ मारेंगे (टैप करेंगे),
 *    तुरंत अगला टोकन माइक पर बोल उठेगा और कतार आगे बढ़ जाएगी।
 * 4. स्क्रीन कभी बंद नहीं होगी (Keep Screen Awake)।
 */
@Composable
fun GurujiDarbarScreen(
    repository: AshramRepository,
    settings: AshramSettings,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Keep screen awake while Guruji is conducting Darbar
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var allTokens by remember { mutableStateOf<List<Token>>(emptyList()) }
    var runningTokenNum by remember { mutableStateOf(settings.runningTokenNumber) }
    var isAdvancing by remember { mutableStateOf(false) }

    // Load today's tokens
    fun reloadTokens() {
        scope.launch {
            val list = repository.getAllTokensToday()
            allTokens = list.sortedBy { it.tokenNumber }
        }
    }

    LaunchedEffect(Unit) {
        reloadTokens()
        // Boost volume for Darbar speaker
        AshramVoiceAnnouncementManager.boostAudioVolumeForLoudspeaker(context)
    }

    // Determine current token and next token
    val currentToken = remember(allTokens, runningTokenNum) {
        allTokens.find { it.tokenNumber == runningTokenNum }
            ?: allTokens.firstOrNull { it.status == TokenStatus.WAITING || it.status == TokenStatus.SERVING || it.status == TokenStatus.CALLED }
    }

    val nextTokenInQueue = remember(allTokens, currentToken) {
        if (currentToken != null) {
            allTokens.firstOrNull { it.tokenNumber > currentToken.tokenNumber && it.status != TokenStatus.CANCELLED }
        } else {
            allTokens.firstOrNull { it.status == TokenStatus.WAITING }
        }
    }

    val previousTokenInQueue = remember(allTokens, currentToken) {
        if (currentToken != null) {
            allTokens.filter { it.tokenNumber < currentToken.tokenNumber && it.status != TokenStatus.CANCELLED }.maxByOrNull { it.tokenNumber }
        } else null
    }

    fun triggerVibration() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (ignored: Exception) {}
    }

    // Advance to Next Token
    fun callNext() {
        if (isAdvancing) return
        triggerVibration()
        val target = nextTokenInQueue ?: return
        isAdvancing = true

        scope.launch {
            // 1. Mark current completed if exists
            currentToken?.let { cur ->
                repository.updateTokenStatus(cur.id, TokenStatus.COMPLETED)
                repository.toggleDarshanCompleted(cur.id, true)
            }

            // 2. Advance running number
            runningTokenNum = target.tokenNumber
            repository.updateRunningTokenNumber(target.tokenNumber)
            repository.updateTokenStatus(target.id, TokenStatus.SERVING)

            // 3. Make loud audio announcement
            AshramVoiceAnnouncementManager.announceNextToken(
                context = context,
                tokenNumber = target.tokenNumber,
                devoteeName = target.patientName,
                city = target.city,
                repeatCount = 1
            )

            reloadTokens()
            delay(500)
            isAdvancing = false
        }
    }

    // Go to Previous Token
    fun callPrevious() {
        if (isAdvancing) return
        triggerVibration()
        val target = previousTokenInQueue ?: return
        isAdvancing = true

        scope.launch {
            runningTokenNum = target.tokenNumber
            repository.updateRunningTokenNumber(target.tokenNumber)
            repository.updateTokenStatus(target.id, TokenStatus.SERVING)

            AshramVoiceAnnouncementManager.announceNextToken(
                context = context,
                tokenNumber = target.tokenNumber,
                devoteeName = target.patientName,
                city = target.city,
                repeatCount = 1
            )

            reloadTokens()
            delay(500)
            isAdvancing = false
        }
    }

    // Re-announce current
    fun repeatCurrent() {
        triggerVibration()
        currentToken?.let { cur ->
            AshramVoiceAnnouncementManager.announceNextToken(
                context = context,
                tokenNumber = cur.tokenNumber,
                devoteeName = cur.patientName,
                city = cur.city,
                repeatCount = 1
            )
        }
    }

    // Full screen layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0B06)) // Deep Sacred Dark Background
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🚩 पूज्य गुरुजी दरबार स्क्रीन",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24)
                )
            }

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("✕ बाहर निकलें", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // --- 2. Current Running Devotee Card (Giant Text) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1308)),
            border = BorderStroke(2.dp, Color(0xFFF59E0B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "वर्तमान चल रहा टोकन",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD97706),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (currentToken != null) {
                    Text(
                        text = "टोकन #${currentToken.tokenNumber}",
                        fontSize = 62.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFBBF24),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentToken.patientName.ifBlank { "भक्त जन" },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 38.sp
                    )

                    if (currentToken.city.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "📍 ${currentToken.city}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (nextTokenInQueue != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = Color(0xFF291E0A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF78350F))
                        ) {
                            Text(
                                text = "अगला तैयार: #${nextTokenInQueue.tokenNumber} ${nextTokenInQueue.patientName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFCD34D),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "सभी टोकन पूर्ण",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "दरबार में इस समय कोई प्रतीक्षारत टोकन नहीं है।",
                        fontSize = 18.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 3. THE GIANT TAP ZONE: "NEXT TOKEN" ---
        // Big pad designed so Guruji can tap anywhere on screen with his hand!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (nextTokenInQueue != null) {
                        Brush.verticalGradient(
                            listOf(Color(0xFF059669), Color(0xFF047857)) // Big Vibrant Emerald Green
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0xFF374151), Color(0xFF1F2937))
                        )
                    }
                )
                .clickable(enabled = nextTokenInQueue != null && !isAdvancing) {
                    callNext()
                }
                .border(
                    width = 3.dp,
                    color = if (nextTokenInQueue != null) Color(0xFF34D399) else Color(0xFF4B5563),
                    shape = RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                if (isAdvancing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp), strokeWidth = 5.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("टोकन बुलाया जा रहा है...", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        text = "⏭️",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (nextTokenInQueue != null) "अगला टोकन बुलाएं" else "दरबार समाप्त",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (nextTokenInQueue != null) "👉 यहाँ स्क्रीन पर कहीं भी हाथ मारें" else "कोई अन्य टोकन शेष नहीं है",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (nextTokenInQueue != null) Color(0xFFA7F3D0) else Color(0xFF9CA3AF)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 4. Bottom Large Controls: Previous & Repeat ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Previous Token Button
            Button(
                onClick = { callPrevious() },
                enabled = previousTokenInQueue != null && !isAdvancing,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.5.dp, Color(0xFF475569))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏮️ पिछला टोकन", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    previousTokenInQueue?.let {
                        Text("#${it.tokenNumber} ${it.patientName}", fontSize = 11.sp, color = Color(0xFF94A3B8), maxLines = 1)
                    }
                }
            }

            // Repeat Announcement Button
            Button(
                onClick = { repeatCurrent() },
                enabled = currentToken != null,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF78350F) // Sacred Amber
                ),
                border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔊 दोबारा बोलें", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFEF3C7))
                    Text("माइक पर पुनः आवाज लगाएं", fontSize = 11.sp, color = Color(0xFFFDE68A))
                }
            }
        }
    }
}
