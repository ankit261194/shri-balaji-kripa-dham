package com.example.shribalajikripadham.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager
import com.example.shribalajikripadham.data.network.HostingerCentralSyncManager
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleSheetLedgerTab(
    isHindi: Boolean,
    repository: AshramRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var webhookUrl by remember { mutableStateOf(GoogleSheetTokenSyncManager.getWebhookUrl(context)) }
    var isTestingSheet by remember { mutableStateOf(false) }
    var isSheetConnected by remember { mutableStateOf(GoogleSheetTokenSyncManager.isConfigured(context)) }
    var sheetStatusMsg by remember { mutableStateOf<String?>(null) }

    // Cloud Mode & Shift state
    var activeCloudMode by remember { mutableStateOf(HostingerCentralSyncManager.getActiveCloudMode(context)) }
    var isShiftingToGitHub by remember { mutableStateOf(false) }
    var isShiftingToHosting by remember { mutableStateOf(false) }
    var shiftReportMsg by remember { mutableStateOf<String?>(null) }

    // Sync state flags
    var isSyncingSheet by remember { mutableStateOf(false) }
    var isSyncingHostinger by remember { mutableStateOf(false) }
    var isSyncingGitHub by remember { mutableStateOf(false) }
    var isSyncingTriple by remember { mutableStateOf(false) }

    var tripleSyncReport by remember { mutableStateOf<String?>(null) }
    var hostingerOnline by remember { mutableStateOf(true) }

    // Test Hostinger on launch
    LaunchedEffect(Unit) {
        val (ok, _) = HostingerCentralSyncManager.testConnection()
        hostingerOnline = ok
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 🚩 1. MASTER HEADER: TRIPLE CLOUD REDUNDANCY
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
            border = BorderStroke(2.dp, AmberGold),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "🚩 त्रिमूर्ति क्लाउड बैकअप व सिंक हब" else "🚩 Triple Cloud Sync Hub",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonPrimary
                        )
                        Text(
                            text = if (isHindi) "3 स्वतंत्र क्लाउड्स पर डेटा 100% सुरक्षित रहता है" else "Data secured across 3 independent clouds",
                            fontSize = 12.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                    Text(text = "☁️", fontSize = 28.sp)
                }

                Divider(color = AmberGold.copy(alpha = 0.5f), thickness = 1.dp)

                // 3 Cloud indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cloud 1: Hosting
                    Surface(
                        color = if (hostingerOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (hostingerOnline) Color(0xFF4CAF50) else Color(0xFFEF5350))
                    ) {
                        Text(
                            text = if (hostingerOnline) "🌐 Hosting: लाइव" else "🌐 Hosting: जाँचें",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hostingerOnline) Color(0xFF1B5E20) else Color(0xFFC62828),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Cloud 2: GitHub
                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF2196F3))
                    ) {
                        Text(
                            text = "🚀 GitHub: लाइव",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Cloud 3: Google Sheet
                    Surface(
                        color = if (isSheetConnected) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSheetConnected) Color(0xFF4CAF50) else AmberGold)
                    ) {
                        Text(
                            text = if (isSheetConnected) "📊 Sheet: लाइव" else "📊 Sheet: सेटअप करें",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSheetConnected) Color(0xFF1B5E20) else Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 🔄 2. DYNAMIC SHIFT & CLOUD MODE SWITCH (Super Admin Control)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
            border = BorderStroke(1.5.dp, Color(0xFF7B1FA2)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "🔄 क्लाउड मोड व 1-क्लिक शिफ्ट" else "🔄 Cloud Mode & 1-Click Shift",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A148C)
                        )
                        Text(
                            text = if (isHindi)
                                "जब चाहें Hosting से GitHub पर शिफ्ट हों, या GitHub से Hosting पर। दोनों में A to Z 100% एकसमान डेटा रहता है।"
                            else
                                "Shift between Hosting and GitHub anytime. All A-Z data remains 100% mirrored.",
                            fontSize = 11.sp,
                            color = Color(0xFF311B92)
                        )
                    }
                    Text(text = "🔁", fontSize = 26.sp)
                }

                // Active Mode Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: Hostinger Mode
                    FilterChip(
                        selected = (activeCloudMode == "HOSTING"),
                        onClick = {
                            activeCloudMode = "HOSTING"
                            HostingerCentralSyncManager.setActiveCloudMode(context, "HOSTING")
                            Toast.makeText(context, if (isHindi) "🌐 Hostinger मोड सक्रिय हुआ!" else "Hosting Mode Activated", Toast.LENGTH_SHORT).show()
                        },
                        label = {
                            Text(
                                text = if (isHindi) "🌐 Hosting मोड" else "🌐 Hosting Mode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1A237E),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Option 2: GitHub Mode
                    FilterChip(
                        selected = (activeCloudMode == "GITHUB"),
                        onClick = {
                            activeCloudMode = "GITHUB"
                            HostingerCentralSyncManager.setActiveCloudMode(context, "GITHUB")
                            Toast.makeText(context, if (isHindi) "🚀 GitHub मोड सक्रिय (Zero-Cost / Offline Safe)!" else "GitHub Mode Activated", Toast.LENGTH_SHORT).show()
                        },
                        label = {
                            Text(
                                text = if (isHindi) "🚀 GitHub मोड" else "🚀 GitHub Mode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF24292E),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Shift Migration Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button: Shift Hosting -> GitHub
                    Button(
                        onClick = {
                            scope.launch {
                                isShiftingToGitHub = true
                                shiftReportMsg = null
                                val (ok, msg) = HostingerCentralSyncManager.shiftFromHostingToGitHub(repository, context)
                                isShiftingToGitHub = false
                                if (ok) activeCloudMode = "GITHUB"
                                shiftReportMsg = msg
                                Toast.makeText(context, if (ok) "Hosting ➔ GitHub शिफ्ट सफल!" else "त्रुटि", Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isShiftingToGitHub && !isShiftingToHosting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        if (isShiftingToGitHub) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isHindi) "Hosting ➔ GitHub\nशिफ्ट करें" else "Hosting ➔ GitHub\nShift",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Button: Shift GitHub -> Hosting
                    Button(
                        onClick = {
                            scope.launch {
                                isShiftingToHosting = true
                                shiftReportMsg = null
                                val (ok, msg) = HostingerCentralSyncManager.shiftFromGitHubToHosting(repository, context)
                                isShiftingToHosting = false
                                if (ok) activeCloudMode = "HOSTING"
                                shiftReportMsg = msg
                                Toast.makeText(context, if (ok) "GitHub ➔ Hosting शिफ्ट सफल!" else "त्रुटि", Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isShiftingToGitHub && !isShiftingToHosting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        if (isShiftingToHosting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isHindi) "GitHub ➔ Hosting\nशिफ्ट करें" else "GitHub ➔ Hosting\nShift",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                if (shiftReportMsg != null) {
                    Surface(
                        color = Color(0xFFEDE7F6),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFBA68C8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = shiftReportMsg!!,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4A148C),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // ⚡ 3. MASTER 1-CLICK TRIPLE SYNC BUTTON
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, MaroonPrimary),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isHindi) "⚡ मास्टर 1-क्लिक ऑल-सिंक (Super Admin Action)" else "⚡ Master 1-Click All-Sync",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaroonPrimary
                )
                Text(
                    text = if (isHindi)
                        "एक क्लिक में सारा डेटा तीनों जगह (Hosting + GitHub + Sheet) सुरक्षित करें:"
                    else
                        "Sync everything to all 3 clouds in a single tap:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Button(
                    onClick = {
                        scope.launch {
                            isSyncingTriple = true
                            tripleSyncReport = null
                            val (ok, report) = HostingerCentralSyncManager.bidirectionalTripleSync(repository, context)
                            isSyncingTriple = false
                            tripleSyncReport = report
                            Toast.makeText(context, if (ok) "त्रिमूर्ति 100% एकसमान सिंक पूर्ण!" else "सिंक त्रुटि", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSyncingTriple,
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSyncingTriple) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isHindi) "तीनों क्लाउड सिंक हो रहे हैं..." else "Syncing all 3 clouds...", color = Color.White)
                    } else {
                        Text(text = "⚡", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "तीनों जगह 100% एकसमान सिंक (Bidirectional Triple Sync)" else "100% Full Mirror All 3 Clouds",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                if (tripleSyncReport != null) {
                    Surface(
                        color = Color(0xFFF1F8E9),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF81C784)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tripleSyncReport!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1B5E20),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // 🌐 3. CARD 1: HOSTINGER SERVER PUSH BUTTON
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🌐 1. Hostinger सेंट्रल सर्वर (MySQL)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A237E)
                        )
                        Text(
                            text = "shribalajikripadham.online/api/",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Text(text = "🌐", fontSize = 24.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                isSyncingHostinger = true
                                try {
                                    val tokens = repository.getAllTokens()
                                    val (ok, msg) = HostingerCentralSyncManager.syncAllTokensToHostinger(tokens)
                                    val s = repository.getSettings()
                                    HostingerCentralSyncManager.updateLiveConfig(
                                        radiusMeters = s.allowedRadiusMeters,
                                        isGeofenceEnforced = s.isGeofenceEnforced,
                                        isOutstationAllowed = s.isOutstationAdvanceAllowed,
                                        outstationKm = s.outstationMinDistanceKm,
                                        lat = s.latitude,
                                        long = s.longitude
                                    )
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSyncingHostinger = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSyncingHostinger,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSyncingHostinger) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(if (isHindi) "Hosting पर पुश करें" else "Push to Hosting", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shribalajikripadham.online/"))
                                context.startActivity(browserIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "ब्राउज़र नहीं खुला", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isHindi) "वेबसाइट खोलें" else "Open Website", fontSize = 12.sp)
                    }
                }
            }
        }

        // 🚀 4. CARD 2: GITHUB PUSH BUTTON
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🚀 2. GitHub लाइव क्लाउड रिपॉजिटरी",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF24292E)
                        )
                        Text(
                            text = "github.com/ankit261194/shri-balaji-kripa-dham",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Text(text = "🚀", fontSize = 24.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                isSyncingGitHub = true
                                try {
                                    val (ok, msg) = repository.publishCurrentSettingsToGitHub("Super Admin")
                                    Toast.makeText(context, if (ok) "GitHub पर सेटिंग्स व डेटा पुश हो गया!" else msg, Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSyncingGitHub = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSyncingGitHub,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292E)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSyncingGitHub) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(if (isHindi) "GitHub पर पुश करें" else "Push to GitHub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ankit261194/shri-balaji-kripa-dham"))
                                context.startActivity(browserIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "ब्राउज़र नहीं खुला", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isHindi) "GitHub खोलें" else "Open GitHub", fontSize = 12.sp)
                    }
                }
            }
        }

        // 📊 5. CARD 3: GOOGLE SHEET PUSH & CONFIG
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📊 3. निजी Google Sheet बही-खाता",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F9D58)
                        )
                        Text(
                            text = if (isSheetConnected) "कनेक्टेड (Live Sync Active)" else "सेटअप लंबित",
                            fontSize = 12.sp,
                            color = if (isSheetConnected) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }

                    Button(
                        onClick = {
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/spreadsheets/"))
                                context.startActivity(browserIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "ब्राउज़र नहीं खुला", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(if (isHindi) "शीट खोलें" else "Open Sheet", fontSize = 11.sp, color = Color.White)
                    }
                }

                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val trimmed = webhookUrl.trim()
                            GoogleSheetTokenSyncManager.saveWebhookUrl(context, trimmed)
                            isSheetConnected = GoogleSheetTokenSyncManager.isConfigured(context)
                            Toast.makeText(context, if (isHindi) "वेबहुक URL सुरक्षित हुआ!" else "Saved!", Toast.LENGTH_SHORT).show()

                            scope.launch {
                                isTestingSheet = true
                                val (ok, msg) = GoogleSheetTokenSyncManager.testConnection(context)
                                isTestingSheet = false
                                isSheetConnected = ok
                                sheetStatusMsg = msg
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHindi) "URL सेव करें" else "Save URL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isSyncingSheet = true
                                try {
                                    val tokens = repository.getAllTokens()
                                    if (tokens.isNotEmpty()) {
                                        GoogleSheetTokenSyncManager.postBatchTokensToSheet(context, tokens)
                                    }
                                    Toast.makeText(context, if (isHindi) "Google Sheet में डेटा सुरक्षित हुआ!" else "Synced to Sheet!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSyncingSheet = false
                                }
                            }
                        },
                        enabled = !isSyncingSheet && isSheetConnected,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSyncingSheet) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(if (isHindi) "Sheet पर पुश करें" else "Push to Sheet", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                if (sheetStatusMsg != null) {
                    Text(
                        text = sheetStatusMsg!!,
                        fontSize = 11.sp,
                        color = if (isSheetConnected) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    )
                }
            }
        }

        // 📝 6. Instructions Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
            border = BorderStroke(1.2.dp, AmberGold)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isHindi) "💡 Google Sheet बही-खाता सेटअप निर्देश" else "💡 Setup Guide",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFBF360C)
                )
                Text(
                    text = if (isHindi)
                        "1. अपने Google Drive में एक नई Google Sheet बनाएं।\n" +
                        "2. ऊपर मेन्यू में 'Extensions' -> 'Apps Script' खोलें।\n" +
                        "3. google_sheets_apps_script.js का कोड वहाँ पेस्ट करें।\n" +
                        "4. 'Deploy' -> 'New deployment' -> Type में 'Web app' चुनें (Access: Anyone)।\n" +
                        "5. मिला हुआ URL ऊपर डालकर 'URL सेव करें' दबाएं।"
                    else
                        "1. Create a Google Sheet in Google Drive.\n" +
                        "2. Open Extensions -> Apps Script.\n" +
                        "3. Paste google_sheets_apps_script.js code.\n" +
                        "4. Deploy as Web app (Access: Anyone).\n" +
                        "5. Paste URL above and click Save URL.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF3E2723)
                )
            }
        }
    }
}
