package com.example.shribalajikripadham.ui.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.ai.AntigravityDiagnosticReport
import com.example.shribalajikripadham.ai.AntigravityMobileEngine
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.hardware.GeofenceLocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class AntigravityChatMessage(
    val sender: String, // "USER" or "ANTIGRAVITY"
    val text: String,
    val timestamp: String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
    val isAction: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntigravityStudioTab(
    isHindi: Boolean,
    repository: AshramRepository,
    settings: AshramSettings,
    onQueueDateReset: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedStudioTabIndex by remember { mutableStateOf(0) }

    var isBypassActive by remember { mutableStateOf(GeofenceLocationManager.isGeofenceGloballyBypassed) }
    var isMockCheckActive by remember { mutableStateOf(GeofenceLocationManager.isMockCheckGloballyEnabled) }
    var currentTodayDate by remember { mutableStateOf(AntigravityMobileEngine.getTodayFreshDateString()) }

    var isExecutingAction by remember { mutableStateOf(false) }
    var statusBannerMessage by remember { mutableStateOf<String?>(null) }
    var diagnosticReport by remember { mutableStateOf<AntigravityDiagnosticReport?>(null) }
    var showDarshanDialog by remember { mutableStateOf(false) }

    // Chat state
    var chatInput by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            AntigravityChatMessage(
                sender = "ANTIGRAVITY",
                text = "⚡ **Antigravity Mobile Studio (PC God-Mode) सक्रिय है।**\n\nगुरुजी, आप यहाँ से पूरी ऐप का रिमोट कंट्रोल संभाल सकते हैं — SQL क्वेरी चलाएं, रिमोट फाइलें बदलें, होस्टिंगर सर्वर पर डिप्लॉय करें, टोकन बाईपास करें या डिवाइस अनब्लॉक करें। अब किसी भी काम के लिए PC खोलने की आवश्यकता नहीं है।"
            )
        )
    }
    val listState = rememberLazyListState()

    // SQL Console State
    var sqlQueryInput by remember { mutableStateOf("SELECT * FROM tokens WHERE darbar_date = date('now') ORDER BY token_number DESC LIMIT 10") }
    var sqlOutputResult by remember { mutableStateOf<String?>(null) }
    var isSqlExecuting by remember { mutableStateOf(false) }

    // Remote File Editor State
    var selectedRemoteFileName by remember { mutableStateOf("live_ui_config.json") }
    var remoteFileContent by remember { mutableStateOf("") }
    var isFetchingFile by remember { mutableStateOf(false) }
    var isPushingFile by remember { mutableStateOf(false) }
    var filePushStatus by remember { mutableStateOf<String?>(null) }

    // Cloud Deployer State
    var isDeployingCloud by remember { mutableStateOf(false) }
    var deployLogsOutput by remember { mutableStateOf<String?>(null) }

    // Device Unlocker State
    var unlockDeviceQuery by remember { mutableStateOf("") }
    var unlockDeviceStatus by remember { mutableStateOf<String?>(null) }
    var isUnlockingDevice by remember { mutableStateOf(false) }

    // Refresh telemetry on launch
    LaunchedEffect(Unit) {
        val report = AntigravityMobileEngine.runFullSystemDiagnostics(context, repository)
        diagnosticReport = report
        isBypassActive = GeofenceLocationManager.isGeofenceGloballyBypassed
        isMockCheckActive = GeofenceLocationManager.isMockCheckGloballyEnabled
    }

    val runCommand: (String) -> Unit = { cmdText ->
        if (cmdText.isNotBlank()) {
            messages.add(AntigravityChatMessage(sender = "USER", text = cmdText))
            chatInput = ""
            isExecutingAction = true
            scope.launch {
                val res = AntigravityMobileEngine.executeCommand(cmdText, context, repository)
                isBypassActive = GeofenceLocationManager.isGeofenceGloballyBypassed
                isMockCheckActive = GeofenceLocationManager.isMockCheckGloballyEnabled
                if (res.actionExecuted == "DATE_RESET_TODAY") {
                    currentTodayDate = AntigravityMobileEngine.getTodayFreshDateString()
                    onQueueDateReset(currentTodayDate)
                }
                if (res.diagnosticReport != null) {
                    diagnosticReport = res.diagnosticReport
                }
                messages.add(
                    AntigravityChatMessage(
                        sender = "ANTIGRAVITY",
                        text = res.messageHindi,
                        isAction = res.actionExecuted != null
                    )
                )
                isExecutingAction = false
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Developer Studio Deep Slate
    ) {
        // --- 1. Studio Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ANTIGRAVITY STUDIO",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "PC God-Mode v2.56.57 • सुपर एडमिन पावर्ड",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isBypassActive) Color(0xFFDC2626) else Color(0xFF059669))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isBypassActive) "🚨 बाईपास चालू" else "🛡️ सामान्य नियम",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // --- 2. Studio Mode Navigation Tabs ---
        val studioTabs = listOf(
            "💬 AI कंसोल",
            "⚡ पैनिक सेंटर",
            "💻 SQL कंसोल",
            "📝 रिमोट फाइल",
            "☁️ डिप्लॉयर",
            "🔓 अनब्लॉकर"
        )
        ScrollableTabRow(
            selectedTabIndex = selectedStudioTabIndex,
            containerColor = Color(0xFF1E293B),
            contentColor = Color(0xFF60A5FA),
            edgePadding = 8.dp,
            indicator = {},
            divider = {}
        ) {
            studioTabs.forEachIndexed { index, tabTitle ->
                val isSelected = selectedStudioTabIndex == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedStudioTabIndex = index },
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF3B82F6) else Color(0xFF334155))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tabTitle,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // Status banner if present
        statusBannerMessage?.let { msg ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = Color(0xFF334155),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = msg, fontSize = 12.sp, color = Color(0xFFF8FAFC), modifier = Modifier.weight(1f))
                    Text(
                        text = "✕",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .clickable { statusBannerMessage = null }
                            .padding(4.dp)
                    )
                }
            }
        }

        // --- 3. TAB CONTENT VIEWER ---
        when (selectedStudioTabIndex) {
            0 -> {
                // ==========================================
                // TAB 0: CONVERSATIONAL AI CONSOLE
                // ==========================================
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(messages) { msg ->
                                val isMe = msg.sender == "USER"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        color = if (isMe) Color(0xFF4338CA) else Color(0xFF1E293B),
                                        shape = RoundedCornerShape(12.dp),
                                        border = if (!isMe) BorderStroke(1.dp, Color(0xFF334155)) else null,
                                        modifier = Modifier.widthIn(max = 330.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = if (isMe) "सुपर एडमिन" else "⚡ Antigravity AI",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isMe) Color(0xFFC7D2FE) else Color(0xFF818CF8)
                                                )
                                                Text(text = msg.timestamp, fontSize = 9.sp, color = Color(0xFF94A3B8))
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = msg.text,
                                                fontSize = 13.sp,
                                                color = Color(0xFFF8FAFC),
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick Action Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionChip("🚨 बाईपास ऑन") { runCommand("बाईपास चालू करो") }
                        QuickActionChip("🔒 बाईपास ऑफ") { runCommand("बाईपास बंद करो") }
                        QuickActionChip("📍 फ़ेक GPS बंद") { runCommand("फेक जीपीएस बंद करो") }
                        QuickActionChip("🔄 कतार आज की तारीख") { runCommand("कतार तारीख आज पर लाओ") }
                        QuickActionChip("🩺 डीप जांच") { runCommand("पूरी ऐप की जाँच करो") }
                        QuickActionChip("🚀 डिप्लॉय") { runCommand("डिप्लॉय") }
                    }

                    // Input Box
                    Surface(
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = {
                                    Text("हिंदी या अंग्रेजी में निर्देश दें या SQL लिखें...", fontSize = 12.sp, color = Color(0xFF64748B))
                                },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (chatInput.isNotBlank()) Color(0xFF4F46E5) else Color(0xFF334155))
                                    .clickable(enabled = chatInput.isNotBlank() && !isExecutingAction) {
                                        runCommand(chatInput)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isExecutingAction) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("➤", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // ==========================================
                // TAB 1: PANIC CONTROLS
                // ==========================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "🚨 आपातकालीन मास्टर नियंत्रण (Panic Overrides)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                    Text(
                        text = "यहाँ से बदला गया कोई भी नियम 0 सेकंड में GitHub व सेंट्रल सर्वर पर लाइव हो जाता है। भक्तों को ऐप अपडेट करने की ज़रूरत नहीं है।",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    // Control 1: Master Token Bypass
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isBypassActive) Color(0xFFEF4444) else Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("🚨 मास्टर टोकन बाईपास (सभी स्थानों हेतु)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text(
                                        text = if (isBypassActive) "सक्रिय: सभी भक्त कहीं से भी टोकन बना सकते हैं।" else "निष्क्रिय: सामान्य 30 किमी आश्रम दायरा लागू है।",
                                        fontSize = 11.sp,
                                        color = if (isBypassActive) Color(0xFFFCA5A5) else Color(0xFF94A3B8)
                                    )
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isExecutingAction = true
                                            val (ok, msg) = AntigravityMobileEngine.setMasterTokenBypass(context, !isBypassActive, repository)
                                            isBypassActive = GeofenceLocationManager.isGeofenceGloballyBypassed
                                            statusBannerMessage = msg
                                            isExecutingAction = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isBypassActive) Color(0xFFDC2626) else Color(0xFF2563EB)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isBypassActive) "बाईपास बंद करें" else "तुरंत बाईपास चालू करें", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Control 2: Mock GPS Check Killswitch
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("📍 फ़ेक जीपीएस सुरक्षा जांच (Kill-Switch)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text(
                                        text = if (isMockCheckActive) "सक्रिय: फ़ेक लोकेशन पकड़ने पर टोकन ब्लॉक होगा।" else "निष्क्रिय (किल-स्विच ऑन): किसी भी भक्त को फ़ेक जीपीएस एरर नहीं आएगा।",
                                        fontSize = 11.sp,
                                        color = if (isMockCheckActive) Color(0xFF86EFAC) else Color(0xFFFDE047)
                                    )
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isExecutingAction = true
                                            val (ok, msg) = AntigravityMobileEngine.setMockCheckEnabled(context, !isMockCheckActive, repository)
                                            isMockCheckActive = GeofenceLocationManager.isMockCheckGloballyEnabled
                                            statusBannerMessage = msg
                                            isExecutingAction = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isMockCheckActive) Color(0xFFD97706) else Color(0xFF059669)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isMockCheckActive) "जांच बंद करें" else "जांच चालू करें", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Control 3: Queue Date Force Reset
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("🔄 कतार तिथि आज पर रीसेट (Queue Date Reset)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text("वर्तमान तिथि: $currentTodayDate (कल के पुराने टोकन कतार से हट जाएंगे)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                Button(
                                    onClick = {
                                        currentTodayDate = AntigravityMobileEngine.getTodayFreshDateString()
                                        onQueueDateReset(currentTodayDate)
                                        statusBannerMessage = "🔄 कतार तिथि आज ($currentTodayDate) पर रीसेट हो गई!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("आज पर सेट करें", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Control 4: Daily Darshan Override
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("🌺 दैनिक दर्शन लाइव ओवरराइड", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text("आज का पावन अलौकिक फोटो, शीर्षक व सुविचार तुरंत बदलें", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                Button(
                                    onClick = { showDarshanDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE185D)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("दर्शन बदलें", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // ==========================================
                // TAB 2: RAW SQL CONSOLE
                // ==========================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "💻 SQLite डेटाबेस कमांड कंसोल (Raw SQL Terminal)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "सीधे मोबाइल से किसी भी टेबल को देखें, टोकन चेक करें या डेटा अपडेट करें:",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickActionChip("आज के टोकन") {
                            sqlQueryInput = "SELECT * FROM tokens WHERE darbar_date = date('now') ORDER BY token_number DESC LIMIT 20"
                        }
                        QuickActionChip("टोकन स्थिति गणना") {
                            sqlQueryInput = "SELECT status, count(*) AS total FROM tokens GROUP BY status"
                        }
                        QuickActionChip("डिवाइस प्रतिबंध") {
                            sqlQueryInput = "SELECT * FROM device_registrations ORDER BY created_at DESC LIMIT 10"
                        }
                        QuickActionChip("सभी टेबल्स") {
                            sqlQueryInput = "SELECT name FROM sqlite_master WHERE type='table'"
                        }
                        QuickActionChip("आश्रम सेटिंग्स") {
                            sqlQueryInput = "SELECT allowed_radius_meters, is_token_generation_active, darbar_date FROM ashram_settings LIMIT 1"
                        }
                        QuickActionChip("ऑडिट लॉग") {
                            sqlQueryInput = "SELECT * FROM audit_logs ORDER BY id DESC LIMIT 10"
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = sqlQueryInput,
                        onValueChange = { sqlQueryInput = it },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White),
                        placeholder = { Text("Enter SQL query here...", color = Color.Gray, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSqlExecuting = true
                                    val (ok, res) = AntigravityMobileEngine.executeRawSql(sqlQueryInput, context)
                                    sqlOutputResult = res
                                    isSqlExecuting = false
                                }
                            },
                            enabled = sqlQueryInput.isNotBlank() && !isSqlExecuting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSqlExecuting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("⚡ क्वेरी निष्पादित करें (Execute)", fontSize = 12.sp)
                        }

                        if (sqlOutputResult != null) {
                            TextButton(onClick = { sqlOutputResult = null }) {
                                Text("साफ़ करें", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Output Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF020617))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        if (sqlOutputResult != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = sqlOutputResult ?: "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8),
                                    lineHeight = 15.sp
                                )
                            }
                        } else {
                            Text(
                                text = "क्वेरी परिणाम यहाँ दिखाई देंगे...",
                                color = Color(0xFF475569),
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }

            3 -> {
                // ==========================================
                // TAB 3: REMOTE FILE & LIVE CONFIG EDITOR
                // ==========================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "📝 रिमोट फाइल एवं लाइव कॉन्फ़िग एडिटर (Zero-Update Engine)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA855F7)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "सीधे GitHub व सर्वर पर फाइलें बदलें। ऐप अपडेट किए बिना सभी भक्तों के फोन पर लाइव!",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // File selector row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = selectedRemoteFileName,
                            onValueChange = { selectedRemoteFileName = it },
                            label = { Text("फाइल नाम", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFA855F7),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    isFetchingFile = true
                                    filePushStatus = null
                                    val (ok, content) = AntigravityMobileEngine.fetchRemoteFile(selectedRemoteFileName)
                                    if (ok) {
                                        remoteFileContent = content
                                        filePushStatus = "✅ फ़ाइल सफलतापूर्वक लोड हुई (${content.length} अक्षर)"
                                    } else {
                                        filePushStatus = "❌ फ़ाइल लोड विफल: $content"
                                    }
                                    isFetchingFile = false
                                }
                            },
                            enabled = !isFetchingFile,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isFetchingFile) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("डाउनलोड", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = remoteFileContent,
                        onValueChange = { remoteFileContent = it },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFFE2E8F0)),
                        placeholder = { Text("फ़ाइल सामग्री यहाँ प्रदर्शित होगी या लिखें...", color = Color.Gray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA855F7),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF020617),
                            unfocusedContainerColor = Color(0xFF020617)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        filePushStatus?.let { status ->
                            Text(text = status, fontSize = 11.sp, color = Color(0xFFD8B4FE), modifier = Modifier.weight(1f))
                        } ?: Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                scope.launch {
                                    isPushingFile = true
                                    val (ok, res) = AntigravityMobileEngine.pushRemoteFile(
                                        fileName = selectedRemoteFileName,
                                        content = remoteFileContent,
                                        commitMessage = "Live update via Antigravity Mobile Studio"
                                    )
                                    filePushStatus = if (ok) "🚀 लाइव पुश सफल: $res" else "❌ पुश त्रुटि: $res"
                                    isPushingFile = false
                                }
                            },
                            enabled = remoteFileContent.isNotBlank() && !isPushingFile,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isPushingFile) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("🚀 सर्वर पर लाइव पुश करें", fontSize = 12.sp)
                        }
                    }
                }
            }

            4 -> {
                // ==========================================
                // TAB 4: CLOUD DEPLOYER & SERVER HEALTH
                // ==========================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "☁️ 1-क्लिक होस्टिंगर क्लाउड डिप्लॉयर (Autonomous Server Deployer)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "Hostinger सर्वर पर सीधे GitHub main ब्रांच से सारा बैकएंड कोड एक क्लिक में सिंक्रोनाइज़ करें:",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("🔗 टार्गेट सर्वर: https://shribalajikripadham.online", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("⚡ डिप्लॉय ट्रिगर: deploy.php?sha=main", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        isDeployingCloud = true
                                        val (ok, log) = AntigravityMobileEngine.triggerCloudDeploy()
                                        deployLogsOutput = log
                                        isDeployingCloud = false
                                    }
                                },
                                enabled = !isDeployingCloud,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isDeployingCloud) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("🚀 होस्टिंगर पर तुरंत नया कोड डिप्लॉय करें", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("📋 डिप्लॉयमेंट लाइव लॉग आउटपुट:", fontSize = 12.sp, color = Color(0xFFE2E8F0), fontWeight = FontWeight.SemiBold)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF020617))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        if (deployLogsOutput != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = deployLogsOutput ?: "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF4ADE80),
                                    lineHeight = 16.sp
                                )
                            }
                        } else {
                            Text(
                                text = "डिप्लॉय बटन दबाने पर सर्वर का लाइव लॉग यहाँ दिखेगा...",
                                color = Color(0xFF475569),
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }

            5 -> {
                // ==========================================
                // TAB 5: DEVICE & PHONE UNBLOCKER
                // ==========================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "🔓 डिवाइस एवं फोन टोकन अनब्लॉकर (Hardware Unban Hub)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF97316)
                    )
                    Text(
                        text = "1-फोन-1-टोकन नियम के कारण अगर किसी भक्त का फोन या नंबर अटक गया है, तो उसे यहाँ से 1-क्लिक में रीसेट करें:",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    OutlinedTextField(
                        value = unlockDeviceQuery,
                        onValueChange = { unlockDeviceQuery = it },
                        label = { Text("भक्त का 10-अंकीय फोन नंबर या Device ID") },
                        placeholder = { Text("उदा. 9876543210 या c8f1-...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF97316),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isUnlockingDevice = true
                                val (ok, msg) = AntigravityMobileEngine.clearDeviceLock(unlockDeviceQuery, context)
                                unlockDeviceStatus = msg
                                isUnlockingDevice = false
                            }
                        },
                        enabled = unlockDeviceQuery.isNotBlank() && !isUnlockingDevice,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isUnlockingDevice) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("🔓 डिवाइस प्रतिबंध हटाएं (Unblock Device Now)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    unlockDeviceStatus?.let { status ->
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFEA580C)),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text(
                                text = status,
                                fontSize = 12.sp,
                                color = Color(0xFFFED7AA),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Daily Darshan Override Dialog ---
    if (showDarshanDialog) {
        var darshanTitleInput by remember { mutableStateOf("श्री बालाजी महाराज दैनिक दिव्य अलौकिक श्रृंगार दर्शन") }
        var darshanPhotoUrlInput by remember { mutableStateOf("https://shribalajikripadham.online/media/balaji_darshan_today.jpg") }
        var darshanQuoteInput by remember { mutableStateOf("संकट कटे मिटे सब पीरा, जो सुमिरै हनुमत बलबीरा।") }
        var isSavingDarshan by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSavingDarshan) showDarshanDialog = false },
            title = {
                Text("🌺 दैनिक दर्शन लाइव ओवरराइड", fontWeight = FontWeight.Bold, color = Color(0xFFF472B6))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("यह फोटो व संदेश बिना ऐप अपडेट किए सीधे सभी भक्तों के फोन पर तुरंत दिखने लगेगा:", fontSize = 12.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = darshanTitleInput,
                        onValueChange = { darshanTitleInput = it },
                        label = { Text("शीर्षक (Title)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = darshanPhotoUrlInput,
                        onValueChange = { darshanPhotoUrlInput = it },
                        label = { Text("फोटो वेब लिंक (Image URL)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = darshanQuoteInput,
                        onValueChange = { darshanQuoteInput = it },
                        label = { Text("दैनिक सुविचार / दोहा (Quote)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isSavingDarshan = true
                            val (ok, msg) = AntigravityMobileEngine.updateDailyDarshanLive(
                                title = darshanTitleInput.trim(),
                                photoUrl = darshanPhotoUrlInput.trim(),
                                quote = darshanQuoteInput.trim()
                            )
                            isSavingDarshan = false
                            showDarshanDialog = false
                            statusBannerMessage = if (ok) "🌺 $msg" else "❌ $msg"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE185D)),
                    enabled = !isSavingDarshan
                ) {
                    if (isSavingDarshan) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("लाइव सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDarshanDialog = false }, enabled = !isSavingDarshan) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
private fun QuickActionChip(label: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF334155),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF475569)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFFE2E8F0),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
