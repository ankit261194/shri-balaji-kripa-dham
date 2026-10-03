package com.example.shribalajikripadham.ui.admin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.util.AshramVoiceAnnouncementManager
import com.example.shribalajikripadham.util.ElevenLabsKeyInfo
import com.example.shribalajikripadham.util.ElevenLabsTtsEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAndApiSettingsScreen(
    isHindi: Boolean,
    settings: AshramSettings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isVoiceMasterOn by remember { mutableStateOf(AshramVoiceAnnouncementManager.isVoiceMasterEnabled(context)) }
    var selectedPreset by remember { mutableStateOf(AshramVoiceAnnouncementManager.getSelectedVoicePreset(context)) }
    var isAutoNextOn by remember { mutableStateOf(AshramVoiceAnnouncementManager.isAutoNextEnabled(context)) }
    var autoNextDelay by remember { mutableIntStateOf(AshramVoiceAnnouncementManager.getAutoNextDelaySeconds(context)) }

    // Live Key balances
    val keyBalances by AshramVoiceAnnouncementManager.elevenLabsKeyBalances.collectAsState()
    val isRefreshingBalances by AshramVoiceAnnouncementManager.isRefreshingBalances.collectAsState()

    // Editing Key Dialog
    var editingSlot by remember { mutableStateOf<Int?>(null) }
    var editingKeyValue by remember { mutableStateOf("") }

    // Recording state
    var isRecording by remember { mutableStateOf(false) }
    var hasCustomRec by remember { mutableStateOf(AshramVoiceAnnouncementManager.hasCustomRecording(context)) }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val recordSuccess = AshramVoiceAnnouncementManager.startCustomRecording(context)
            isRecording = recordSuccess
            if (!recordSuccess) {
                Toast.makeText(context, if (isHindi) "रिकॉर्डिंग शुरू करने में असमर्थ" else "Failed to start recording", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, if (isHindi) "माइक की अनुमति आवश्यक है" else "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        // Fetch balances on screen load
        AshramVoiceAnnouncementManager.refreshAllKeyBalances(context)
    }

    // Key editing dialog
    if (editingSlot != null) {
        val slotNum = editingSlot!!
        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = {
                Text(
                    text = if (isHindi) "ElevenLabs API कुंजी #$slotNum बदलें" else "Edit ElevenLabs API Key #$slotNum",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHindi)
                            "ElevenLabs डैशबोर्ड से 'sk_...' कुंजी पेस्ट करें:"
                        else
                            "Paste the ElevenLabs API Key starting with 'sk_...':",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    OutlinedTextField(
                        value = editingKeyValue,
                        onValueChange = { editingKeyValue = it.trim() },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AshramVoiceAnnouncementManager.setElevenLabsApiKey(context, editingKeyValue, slotNum)
                        editingSlot = null
                        Toast.makeText(context, if (isHindi) "कुंजी #$slotNum सुरक्षित कर दी गई!" else "Key #$slotNum saved!", Toast.LENGTH_SHORT).show()
                        AshramVoiceAnnouncementManager.refreshAllKeyBalances(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text(if (isHindi) "सुरक्षित करें" else "Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSlot = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. MASTER VOICE ON/OFF SWITCH CARD ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isVoiceMasterOn) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (isVoiceMasterOn) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isVoiceMasterOn) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (isVoiceMasterOn) "🔊" else "🔇", fontSize = 24.sp)
                            }
                        }
                        Column {
                            Text(
                                text = if (isVoiceMasterOn)
                                    (if (isHindi) "आवाज़: चालू (Loudspeaker ON)" else "Voice Announcement: ON")
                                else
                                    (if (isHindi) "आवाज़: बंद (Muted)" else "Voice Announcement: OFF"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isVoiceMasterOn) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                            )
                            Text(
                                text = if (isVoiceMasterOn)
                                    (if (isHindi) "टोकन बुलाने पर मंदिर लाउडस्पीकर पर आवाज़ गूंजेगी" else "Speaker will announce next tokens")
                                else
                                    (if (isHindi) "सभी आवाज़ें बंद हैं (म्यूट मोड)" else "All voice announcements are disabled"),
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }

                    Switch(
                        checked = isVoiceMasterOn,
                        onCheckedChange = { newState ->
                            isVoiceMasterOn = newState
                            AshramVoiceAnnouncementManager.setVoiceMasterEnabled(context, newState)
                            Toast.makeText(
                                context,
                                if (newState)
                                    (if (isHindi) "🔊 आवाज़ उद्घोषणा चालू कर दी गई" else "Voice enabled")
                                else
                                    (if (isHindi) "🔇 आवाज़ बंद कर दी गई" else "Voice disabled"),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2E7D32),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFC62828)
                        )
                    )
                }
            }
        }

        // --- 2. AUTO-TIMER & CALL INTERVAL CONTROLS ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⏱️", fontSize = 20.sp)
                        Column {
                            Text(
                                text = if (isHindi) "टोकन उद्घोषणा अंतराल व ऑटो-टाइमर" else "Announcement Interval & Auto-Timer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi)
                                    "टोकन बदलने के कितने सेकंड बाद अगला टोकन व भीड़ नियंत्रण आवाज़ बोले:"
                                else
                                    "Delay before announcing next standby devotee:",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            10 to "10s",
                            15 to "15s",
                            20 to "20s",
                            30 to "30s",
                            0 to (if (isHindi) "👆 मैनुअल" else "Manual")
                        ).forEach { (seconds, label) ->
                            val isSelected = if (seconds == 0) !isAutoNextOn else (isAutoNextOn && autoNextDelay == seconds)
                            OutlinedButton(
                                onClick = {
                                    if (seconds == 0) {
                                        isAutoNextOn = false
                                        AshramVoiceAnnouncementManager.setAutoNextEnabled(context, false)
                                    } else {
                                        isAutoNextOn = true
                                        autoNextDelay = seconds
                                        AshramVoiceAnnouncementManager.setAutoNextEnabled(context, true)
                                        AshramVoiceAnnouncementManager.setAutoNextDelaySeconds(context, seconds)
                                    }
                                    Toast.makeText(
                                        context,
                                        if (seconds == 0)
                                            (if (isHindi) "👆 केवल मैनुअल बटन दबाने पर बोलेगा" else "Manual announcement mode")
                                        else
                                            (if (isHindi) "⏱️ $seconds सेकंड का ऑटो-टाइमर सेट किया गया" else "Set to $seconds seconds"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaroonPrimary else Color(0xFFF5F5F5),
                                    contentColor = if (isSelected) Color.White else Color.Black
                                ),
                                border = BorderStroke(1.dp, if (isSelected) MaroonPrimary else Color(0xFFD0D0D0)),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. VOICE PRESET SELECTOR (ONLY 3 ALLOWED VOICES) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎙️", fontSize = 20.sp)
                        Column {
                            Text(
                                text = if (isHindi) "सक्रिय आवाज़ का चयन (3 शुद्ध विकल्प)" else "Active Spoken Voice (3 Pure Options)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi)
                                    "रोबोटिक आवाज़ें पूर्णतः हटाई गई हैं। केवल 100% स्पष्ट HD आवाज़ें उपलब्ध हैं।"
                                else
                                    "Robotic TTS removed. Only pure studio voices available.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    AshramVoiceAnnouncementManager.AVAILABLE_VOICE_PRESETS.forEach { preset ->
                        val isSelected = selectedPreset == preset.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPreset = preset.id
                                    AshramVoiceAnnouncementManager.setVoicePreset(context, preset.id)
                                    Toast.makeText(
                                        context,
                                        if (isHindi) "✅ ${preset.nameHindi} चुनी गई" else "✅ ${preset.nameEnglish} selected",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                if (isSelected) 1.8.dp else 1.dp,
                                if (isSelected) SaffronPrimary else Color(0xFFE0E0E0)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFFFF8E1) else Color(0xFFFAFAFA)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedPreset = preset.id
                                            AshramVoiceAnnouncementManager.setVoicePreset(context, preset.id)
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = SaffronPrimary)
                                    )
                                    Column {
                                        Text(
                                            text = if (isHindi) preset.nameHindi else preset.nameEnglish,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = if (isSelected) MaroonPrimary else Color.Black
                                        )
                                        Text(
                                            text = preset.description,
                                            fontSize = 11.5.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        AshramVoiceAnnouncementManager.testVoice(context, preset.id)
                                        Toast.makeText(
                                            context,
                                            if (isHindi) "📢 आवाज़ का परीक्षण शुरू हुआ..." else "Testing voice...",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(if (isHindi) "टेस्ट 🔊" else "Test 🔊", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. ELEVENLABS 5-API KEY POOL WITH PROACTIVE 50-CREDIT FAILOVER ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.2.dp, SaffronPrimary)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔑", fontSize = 22.sp)
                            Column {
                                Text(
                                    text = if (isHindi) "ElevenLabs 5-Key पूल प्रबंधन" else "ElevenLabs 5-Key Pool Manager",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaroonPrimary
                                )
                                Text(
                                    text = if (isHindi) "कुल 50,000 क्रेडिट/माह (स्वतः बैकअप स्विच)" else "50,000 Free Credits/Month Pool",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Button(
                            onClick = {
                                AshramVoiceAnnouncementManager.refreshAllKeyBalances(context)
                                Toast.makeText(context, if (isHindi) "सभी 5 कुंजियों के क्रेडिट चेक किए जा रहे हैं..." else "Checking credit balances...", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !isRefreshingBalances,
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (isRefreshingBalances) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(if (isHindi) "🔄 रीफ्रेश" else "🔄 Refresh", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Failover rule highlight
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        border = BorderStroke(1.dp, Color(0xFFFFB74D))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚡", fontSize = 18.sp)
                            Text(
                                text = if (isHindi)
                                    "ऑटो-स्विच नियम: जब किसी भी की (Key) में 50 या उससे कम क्रेडिट शेष रह जाते हैं, तो सिस्टम तुरंत अगली उपलब्ध की पर अपने आप स्विच कर जाता है। उद्घोषणा कभी नहीं रुकती!"
                                else
                                    "Auto-Switch: When any key drops below 50 credits, the engine seamlessly fails over to the next key without interruption!",
                                fontSize = 11.5.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Display all 11 slots
                    for (slot in 1..11) {
                        val keyInfo = keyBalances.find { it.slotNumber == slot }
                        val currentKey = AshramVoiceAnnouncementManager.getElevenLabsApiKey(context, slot)
                        val maskedKey = if (currentKey.length > 12) {
                            "${currentKey.take(7)}...${currentKey.takeLast(4)}"
                        } else currentKey

                        val isCritical = (keyInfo != null && keyInfo.remainingCharacters <= 50 && keyInfo.isValid)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isCritical -> Color(0xFFE53935)
                                    slot == 1 -> SaffronPrimary
                                    else -> Color(0xFFE0E0E0)
                                }
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isCritical -> Color(0xFFFFEBEE)
                                    slot == 1 -> Color(0xFFFFFBF2)
                                    else -> Color(0xFFFAFAFA)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (slot == 1) SaffronPrimary else MaroonPrimary
                                        ) {
                                            Text(
                                                text = if (slot == 1) "प्राथमिक (Slot 1)" else "बैकअप $slot",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = maskedKey.ifBlank { if (isHindi) "कुंजी खाली है" else "Not set" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            editingSlot = slot
                                            editingKeyValue = currentKey
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("✏️", fontSize = 14.sp)
                                    }
                                }

                                if (keyInfo != null) {
                                    if (keyInfo.isValid) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isHindi)
                                                    "क्रेडिट: ${keyInfo.remainingCharacters} / ${keyInfo.characterLimit} शेष"
                                                else
                                                    "Credits: ${keyInfo.remainingCharacters} / ${keyInfo.characterLimit} left",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCritical) Color(0xFFC62828) else Color(0xFF2E7D32)
                                            )

                                            if (isCritical) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFC62828)
                                                ) {
                                                    Text(
                                                        text = "⚠️ ≤ 50 (स्वतः स्विच होगा)",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        LinearProgressIndicator(
                                            progress = { keyInfo.remainingPercent },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = if (isCritical) Color(0xFFC62828) else Color(0xFF2E7D32),
                                            trackColor = Color(0xFFE0E0E0),
                                        )
                                    } else {
                                        Text(
                                            text = "❌ ${keyInfo.errorMsg ?: "त्रुटि"}",
                                            fontSize = 11.sp,
                                            color = Color(0xFFC62828)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. ASHRAM MICROPHONE RECORDER (CUSTOM REAL VOICE) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎤", fontSize = 20.sp)
                        Column {
                            Text(
                                text = if (isHindi) "आश्रम लाइव माइक रिकॉर्डर" else "Ashram Live Mic Recorder",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi)
                                    "गुरुजी अथवा आश्रम सेवक की वास्तविक आवाज़ फोन माइक से रिकॉर्ड करें:"
                                else
                                    "Record Guruji or Sevak's real voice from microphone:",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isRecording) {
                                    val saved = AshramVoiceAnnouncementManager.stopCustomRecording(context)
                                    isRecording = false
                                    hasCustomRec = saved
                                    Toast.makeText(
                                        context,
                                        if (saved)
                                            (if (isHindi) "✅ आवाज़ सुरक्षित हो गई!" else "Voice saved!")
                                        else
                                            (if (isHindi) "रिकॉर्डिंग विफल रही" else "Recording failed"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPerm) {
                                        val recordSuccess = AshramVoiceAnnouncementManager.startCustomRecording(context)
                                        isRecording = recordSuccess
                                    } else {
                                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) Color(0xFFC62828) else MaroonPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isRecording)
                                    (if (isHindi) "⏹️ रिकॉर्डिंग रोकें" else "Stop Recording")
                                else
                                    (if (isHindi) "🔴 नई आवाज़ रिकॉर्ड करें" else "Record Voice"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (hasCustomRec) {
                            OutlinedButton(
                                onClick = {
                                    AshramVoiceAnnouncementManager.playCustomRecording(context)
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.2.dp, SaffronPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SaffronPrimary)
                            ) {
                                Text(if (isHindi) "▶️ सुनें" else "Play", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 6. FOOTER BACK BUTTON ---
        item {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(
                    text = if (isHindi) "← मुख्य मेनू पर लौटें" else "← Back to Main Menu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}
