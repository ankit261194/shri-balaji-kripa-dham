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
    var isTesting by remember { mutableStateOf(false) }
    var isSyncingAll by remember { mutableStateOf(false) }
    var connectionStatusMsg by remember { mutableStateOf<String?>(null) }
    var isConnected by remember { mutableStateOf(GoogleSheetTokenSyncManager.isConfigured(context)) }

    val copyScriptToClipboard = {
        try {
            val scriptStream = context.assets.open("google_sheets_apps_script.js")
            val fullCode = scriptStream.bufferedReader().use { it.readText() }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("AppsScriptCode", fullCode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, if (isHindi) "Apps Script कोड कॉपी हो गया!" else "Master script copied to clipboard!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("AppsScriptCode", webhookUrl)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, if (isHindi) "लिंक कॉपी हुआ!" else "URL copied!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Card with Live Connection Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, if (isConnected) Color(0xFF2E7D32) else AmberGold),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "निजी Google Sheet बही-खाता" else "Private Google Sheet Ledger",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonPrimary
                        )
                        Text(
                            text = if (isHindi) "रियल-टाइम ऑटोमैटिक डेटा बैकअप" else "Real-time automatic cloud ledger",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Surface(
                        color = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isConnected) Color(0xFF4CAF50) else Color(0xFFEF5350))
                    ) {
                        Text(
                            text = if (isConnected) (if (isHindi) "सक्रिय (Connected)" else "Connected") else (if (isHindi) "लिंक नहीं है" else "Disconnected"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) Color(0xFF1B5E20) else Color(0xFFC62828),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                if (connectionStatusMsg != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFF1F8E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = connectionStatusMsg!!,
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        // 2. Action Buttons (Open Sheet + Test Connection)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/spreadsheets/"))
                        context.startActivity(browserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "ब्राउज़र नहीं खुला", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isHindi) "Google Sheet खोलें" else "Open Sheet", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = {
                    scope.launch {
                        isTesting = true
                        val (success, msg) = GoogleSheetTokenSyncManager.testConnection(context)
                        isTesting = false
                        isConnected = success
                        connectionStatusMsg = msg
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isTesting && webhookUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (isHindi) "कनेक्शन टेस्ट" else "Test Connection", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // 3. Webhook URL Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isHindi) "Webhook URL (Google Apps Script)" else "Webhook URL (Google Apps Script)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val trimmed = webhookUrl.trim()
                            GoogleSheetTokenSyncManager.saveWebhookUrl(context, trimmed)
                            isConnected = GoogleSheetTokenSyncManager.isConfigured(context)
                            Toast.makeText(context, if (isHindi) "वेबहुक URL सुरक्षित हो गया!" else "Webhook URL saved!", Toast.LENGTH_SHORT).show()

                            scope.launch {
                                isTesting = true
                                val (ok, msg) = GoogleSheetTokenSyncManager.testConnection(context)
                                isTesting = false
                                isConnected = ok
                                connectionStatusMsg = msg
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isHindi) "लिंक सेव करें" else "Save URL", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isSyncingAll = true
                                try {
                                    val tokens = repository.getAllTokensToday()
                                    if (tokens.isNotEmpty()) {
                                        GoogleSheetTokenSyncManager.postBatchTokensToSheet(context, tokens)
                                    }
                                    Toast.makeText(context, if (isHindi) "आज का डेटा शीट में सिंक हो गया!" else "Synced to sheet!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSyncingAll = false
                                }
                            }
                        },
                        enabled = !isSyncingAll && isConnected,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSyncingAll) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (isHindi) "सब कुछ अभी सिंक करें" else "Sync All Now", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 4. Instructions & Code Copy Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
            border = BorderStroke(1.2.dp, AmberGold)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isHindi) "2 मिनट का सेटअप निर्देश" else "2-Minute Setup Guide",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFBF360C)
                )

                Text(
                    text = if (isHindi)
                        "1. अपने Google Drive में एक नई Google Sheet बनाएं।\n" +
                        "2. ऊपर मेन्यू में 'Extensions' -> 'Apps Script' खोलें।\n" +
                        "3. प्रोजेक्ट रूट में मौजूद google_sheets_apps_script.js का कोड वहाँ पेस्ट करें।\n" +
                        "4. 'Deploy' -> 'New deployment' -> Type में 'Web app' चुनें (Access: Anyone)।\n" +
                        "5. मिला हुआ Web App URL ऊपर डालकर 'लिंक सेव करें' दबाएं।"
                    else
                        "1. Open your Google Drive and create a new Google Sheet.\n" +
                        "2. Open 'Extensions' -> 'Apps Script'.\n" +
                        "3. Paste google_sheets_apps_script.js code there.\n" +
                        "4. Click Deploy -> New deployment -> Web app (Access: Anyone).\n" +
                        "5. Paste the generated Web App URL above and click Save.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF3E2723)
                )
            }
        }
    }
}