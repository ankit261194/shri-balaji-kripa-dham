package com.example.shribalajikripadham.ui.admin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.local.DatabaseHelper
import com.example.shribalajikripadham.data.model.Admin
import com.example.shribalajikripadham.data.model.AdminRole
import com.example.shribalajikripadham.data.model.ArziDistributionRecord
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonAccent
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import com.example.shribalajikripadham.util.ArziVoiceParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArziLedgerTab(
    isHindi: Boolean,
    repository: AshramRepository,
    settings: AshramSettings,
    isSuperAdmin: Boolean,
    scope: CoroutineScope,
    context: Context
) {
    val isSuper = isSuperAdmin

    var records by remember { mutableStateOf<List<ArziDistributionRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("ALL") } // ALL, PAID, PENDING

    // Dialogs state
    var showConfirmDialog by remember { mutableStateOf(false) }
    var dialogRecordId by remember { mutableStateOf(0L) }
    var dialogDevoteeName by remember { mutableStateOf("") }
    var dialogPhoneNumber by remember { mutableStateOf("") }
    var dialogBadiQty by remember { mutableStateOf(1) }
    var dialogChhotiQty by remember { mutableStateOf(0) }
    var dialogIsPaid by remember { mutableStateOf(false) }
    var dialogPaymentMode by remember { mutableStateOf("CASH") }
    var dialogNotes by remember { mutableStateOf("") }

    // Rates config dialog
    var showRateConfigDialog by remember { mutableStateOf(false) }
    var inputBadiRate by remember { mutableStateOf(settings.badiArziRate.toInt().toString()) }
    var inputChhotiRate by remember { mutableStateOf(settings.chhotiArziRate.toInt().toString()) }

    fun loadData() {
        scope.launch {
            isLoading = true
            records = repository.getAllArziRecords()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    // Speech Recognizer Launcher for Hindi Voice Input
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                val parsed = ArziVoiceParser.parseSpeech(spokenText)
                dialogRecordId = 0L
                dialogDevoteeName = parsed.devoteeName
                dialogPhoneNumber = parsed.phoneNumber
                dialogBadiQty = if (parsed.bigArziQty > 0) parsed.bigArziQty else 1
                dialogChhotiQty = parsed.smallArziQty
                dialogIsPaid = false
                dialogPaymentMode = "CASH"
                dialogNotes = "🎙️ बोलकर दर्ज: \"$spokenText\""
                showConfirmDialog = true
            }
        }
    }

    fun launchVoiceRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, if (isHindi) "बोलें: जैसे 'रोहित पर पांच बड़ी अर्जी 4 छोटी अर्जी'..." else "Speak devotee name and arzi quantities...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, if (isHindi) "वॉयस इनपुट समर्थित नहीं है" else "Voice input not supported", Toast.LENGTH_SHORT).show()
        }
    }

    // Filter records
    val filteredRecords = remember(records, searchQuery, filterStatus) {
        records.filter { r ->
            val matchesQuery = searchQuery.isBlank() ||
                    r.devoteeName.contains(searchQuery, ignoreCase = true) ||
                    r.phoneNumber.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterStatus) {
                "PAID" -> r.isPaid
                "PENDING" -> !r.isPaid
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    val totalRecords = records.size
    val totalBadi = records.sumOf { it.bigArziQty }
    val totalChhoti = records.sumOf { it.smallArziQty }
    val totalAmount = records.sumOf { it.totalAmount }
    val totalPaidAmount = records.filter { it.isPaid }.sumOf { it.totalAmount }
    val totalPendingAmount = totalAmount - totalPaidAmount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        // 1. Header Banner with Voice Mic & Actions
        Card(
            colors = CardDefaults.cardColors(containerColor = MaroonPrimary),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "पवित्र अर्जी डिब्बा वितरण लेजर 📦" else "Sacred Arzi Distribution Ledger 📦",
                            color = AmberGold,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHindi) "दर: बड़ी ₹${settings.badiArziRate.toInt()} | छोटी ₹${settings.chhotiArziRate.toInt()}" else "Rates: Big ₹${settings.badiArziRate.toInt()} | Small ₹${settings.chhotiArziRate.toInt()}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isSuper) {
                            IconButton(
                                onClick = {
                                    inputBadiRate = settings.badiArziRate.toInt().toString()
                                    inputChhotiRate = settings.chhotiArziRate.toInt().toString()
                                    showRateConfigDialog = true
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Text("⚙️", fontSize = 18.sp)
                            }
                        }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    val res = repository.syncLiveArziFromCloud()
                                    loadData()
                                    Toast.makeText(context, if (res.first) "✅ क्लाउड से ${res.second} रिकॉर्ड सिंक हुए" else "सिंक पूर्ण (नवीनतम)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Text("🔄", fontSize = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Voice Mic Button (Big & Prominent)
                Button(
                    onClick = { launchVoiceRecognition() },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = if (isHindi) "🎙️ बोलकर अर्जी दर्ज करें (वॉयस इनपुट)" else "🎙️ Record Arzi by Spoken Voice",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. KPI Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isHindi) "कुल अर्जी डिब्बे" else "Total Boxes", fontSize = 11.sp, color = Color.Gray)
                    Text("${totalBadi + totalChhoti}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                    Text("बड़ी: $totalBadi | छोटी: $totalChhoti", fontSize = 10.sp, color = Color.DarkGray)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                border = BorderStroke(1.dp, Color(0xFF81C784))
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isHindi) "चुकता राशि ✅" else "Paid Amount", fontSize = 11.sp, color = Color(0xFF2E7D32))
                    Text("₹${totalPaidAmount.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    Text("${records.count { it.isPaid }} भक्त", fontSize = 10.sp, color = Color(0xFF2E7D32))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                border = BorderStroke(1.dp, Color(0xFFFFB74D))
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isHindi) "बकाया राशि ⏳" else "Pending Due", fontSize = 11.sp, color = Color(0xFFE65100))
                    Text("₹${totalPendingAmount.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBF360C))
                    Text("${records.count { !it.isPaid }} शेष", fontSize = 10.sp, color = Color(0xFFE65100))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Search and Add Manual Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (isHindi) "नाम / मोबाइल खोजें..." else "Search name/phone...", fontSize = 13.sp) },
                leadingIcon = { Text("🔍", fontSize = 16.sp) },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Button(
                onClick = {
                    dialogRecordId = 0L
                    dialogDevoteeName = ""
                    dialogPhoneNumber = ""
                    dialogBadiQty = 1
                    dialogChhotiQty = 0
                    dialogIsPaid = false
                    dialogPaymentMode = "CASH"
                    dialogNotes = ""
                    showConfirmDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Text("➕", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isHindi) "नया" else "Add", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips (All / Paid / Pending)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filterStatus == "ALL",
                onClick = { filterStatus = "ALL" },
                label = { Text("सभी ($totalRecords)") }
            )
            FilterChip(
                selected = filterStatus == "PAID",
                onClick = { filterStatus = "PAID" },
                label = { Text("✓ चुकता (${records.count { it.isPaid }})") }
            )
            FilterChip(
                selected = filterStatus == "PENDING",
                onClick = { filterStatus = "PENDING" },
                label = { Text("⏳ बकाया (${records.count { !it.isPaid }})") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Records List
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SaffronPrimary)
            }
        } else if (filteredRecords.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📦", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isHindi) "कोई अर्जी वितरण रिकॉर्ड नहीं मिला" else "No Arzi Records Found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi) "माइक बटन दबाकर बोलें: 'रोहित पर 5 बड़ी अर्जी 2 छोटी अर्जी'" else "Tap mic to record via Hindi voice command",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRecords, key = { it.id }) { record ->
                    ArziRecordRowItem(
                        record = record,
                        isHindi = isHindi,
                        onTogglePaid = { isPaid ->
                            scope.launch {
                                repository.toggleArziPaymentStatus(record.id, isPaid)
                                loadData()
                            }
                        },
                        onEdit = {
                            dialogRecordId = record.id
                            dialogDevoteeName = record.devoteeName
                            dialogPhoneNumber = record.phoneNumber
                            dialogBadiQty = record.bigArziQty
                            dialogChhotiQty = record.smallArziQty
                            dialogIsPaid = record.isPaid
                            dialogPaymentMode = record.paymentMode
                            dialogNotes = record.notes
                            showConfirmDialog = true
                        },
                        onDelete = {
                            scope.launch {
                                repository.deleteArziRecord(record.id)
                                loadData()
                                Toast.makeText(context, if (isHindi) "रिकॉर्ड हटाया गया" else "Record deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // ========================================================================
    // CONFIRMATION & VERIFICATION DIALOG (PREVENTS ANY MISRECORDING)
    // ========================================================================
    if (showConfirmDialog) {
        val calculatedTotal = (dialogBadiQty * settings.badiArziRate) + (dialogChhotiQty * settings.chhotiArziRate)

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (dialogRecordId == 0L) "🎙️ अर्जी वितरण पुष्टि" else "✏️ अर्जी वितरण संशोधन", fontWeight = FontWeight.Bold, color = MaroonPrimary)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isHindi) "कृपया सुनिश्चित करें कि नाम व डिब्बों की संख्या सही है:" else "Please confirm details before saving:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dialogDevoteeName,
                        onValueChange = { dialogDevoteeName = it },
                        label = { Text(if (isHindi) "भक्त / मरीज का नाम *" else "Devotee Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dialogPhoneNumber,
                        onValueChange = { dialogPhoneNumber = it },
                        label = { Text(if (isHindi) "मोबाइल नंबर" else "Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Badi Arzi Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(if (isHindi) "बड़ी अर्जी डिब्बा" else "Big Arzi Box", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("दर: ₹${settings.badiArziRate.toInt()}/डिब्बा", fontSize = 11.sp, color = Color.Gray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (dialogBadiQty > 0) dialogBadiQty-- },
                                modifier = Modifier.size(36.dp).background(Color(0xFFEEEEEE), CircleShape)
                            ) {
                                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "$dialogBadiQty",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(
                                onClick = { dialogBadiQty++ },
                                modifier = Modifier.size(36.dp).background(SaffronPrimary.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Chhoti Arzi Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(if (isHindi) "छोटी अर्जी डिब्बा" else "Small Arzi Box", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("दर: ₹${settings.chhotiArziRate.toInt()}/डिब्बा", fontSize = 11.sp, color = Color.Gray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (dialogChhotiQty > 0) dialogChhotiQty-- },
                                modifier = Modifier.size(36.dp).background(Color(0xFFEEEEEE), CircleShape)
                            ) {
                                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "$dialogChhotiQty",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(
                                onClick = { dialogChhotiQty++ },
                                modifier = Modifier.size(36.dp).background(SaffronPrimary.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Total Calculation Banner
                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AmberGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isHindi) "कुल देय राशि:" else "Total Amount:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("₹${calculatedTotal.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaroonPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1-Tap Paid / Unpaid Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (dialogIsPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
                            .clickable { dialogIsPaid = !dialogIsPaid }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (dialogIsPaid) (if (isHindi) "✅ राशि प्राप्त हो गई (चुकता)" else "✅ Paid In Full") else (if (isHindi) "⏳ भुगतान बाकी है (बकाया)" else "⏳ Payment Pending"),
                                fontWeight = FontWeight.Bold,
                                color = if (dialogIsPaid) Color(0xFF2E7D32) else Color(0xFFE65100),
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (dialogIsPaid) "पैसे मिल चुके हैं" else "टैप करके चुकता चिह्नित करें",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = dialogIsPaid,
                            onCheckedChange = { dialogIsPaid = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2E7D32), checkedTrackColor = Color(0xFFA5D6A7))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dialogDevoteeName.isBlank()) {
                            Toast.makeText(context, if (isHindi) "कृपया भक्त का नाम दर्ज करें" else "Please enter devotee name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (dialogBadiQty == 0 && dialogChhotiQty == 0) {
                            Toast.makeText(context, if (isHindi) "कम से कम 1 अर्जी डिब्बा चुनें" else "Select at least 1 box", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val record = ArziDistributionRecord(
                            id = dialogRecordId,
                            devoteeName = dialogDevoteeName.trim(),
                            phoneNumber = dialogPhoneNumber.trim(),
                            bigArziQty = dialogBadiQty,
                            smallArziQty = dialogChhotiQty,
                            bigArziRate = settings.badiArziRate,
                            smallArziRate = settings.chhotiArziRate,
                            totalAmount = calculatedTotal,
                            isPaid = dialogIsPaid,
                            paymentMode = dialogPaymentMode,
                            recordedBy = if (isSuper) "SUPER_ADMIN" else "SEVADAR",
                            darbarDate = settings.darbarDate.ifBlank { DatabaseHelper.getTodayDateString() },
                            timestamp = System.currentTimeMillis(),
                            notes = dialogNotes
                        )

                        scope.launch {
                            repository.upsertArziRecord(record)
                            loadData()
                            showConfirmDialog = false
                            Toast.makeText(context, if (isHindi) "अर्जी वितरण सुरक्षित हो गया!" else "Record Saved!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isHindi) "सुरक्षित करें (Save)" else "Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // ========================================================================
    // RATE CONFIGURATION DIALOG (SUPER ADMIN ONLY)
    // ========================================================================
    if (showRateConfigDialog && isSuper) {
        AlertDialog(
            onDismissRequest = { showRateConfigDialog = false },
            title = { Text(if (isHindi) "⚙️ अर्जी दर व सेटिंग्स (Super Admin)" else "⚙️ Arzi Rates Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = inputBadiRate,
                        onValueChange = { inputBadiRate = it },
                        label = { Text(if (isHindi) "बड़ी अर्जी की दर (₹)" else "Big Arzi Rate (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputChhotiRate,
                        onValueChange = { inputChhotiRate = it },
                        label = { Text(if (isHindi) "छोटी अर्जी की दर (₹)" else "Small Arzi Rate (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bRate = inputBadiRate.toDoubleOrNull() ?: 100.0
                        val cRate = inputChhotiRate.toDoubleOrNull() ?: 50.0
                        scope.launch {
                            repository.updateArziSettings(
                                isArziLedgerLive = settings.isArziLedgerLive,
                                badiArziRate = bRate.toInt(),
                                chhotiArziRate = cRate.toInt(),
                                canAdminViewArziLedger = settings.canAdminViewArziLedger,
                                canDevoteeViewArziLedger = settings.canDevoteeViewArziLedger
                            )
                            showRateConfigDialog = false
                            Toast.makeText(context, if (isHindi) "दरें अपडेट हो गईं" else "Rates updated", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isHindi) "सुरक्षित करें" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRateConfigDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun ArziRecordRowItem(
    record: ArziDistributionRecord,
    isHindi: Boolean,
    onTogglePaid: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val timeStr = sdf.format(Date(record.timestamp))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (record.isPaid) Color(0xFFF9FFF9) else Color(0xFFFFFDF8)
        ),
        border = BorderStroke(
            1.dp,
            if (record.isPaid) Color(0xFFC8E6C9) else Color(0xFFFFE082)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.devoteeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaroonPrimary
                    )
                    if (record.phoneNumber.isNotBlank()) {
                        Text(
                            text = "📞 ${record.phoneNumber}",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                // Total Amount Tag
                Surface(
                    color = if (record.isPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (record.isPaid) Color(0xFF81C784) else Color(0xFFFFB74D))
                ) {
                    Text(
                        text = "₹${record.totalAmount.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (record.isPaid) Color(0xFF1B5E20) else Color(0xFFBF360C),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantities details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (record.bigArziQty > 0) {
                        Surface(
                            color = MaroonPrimary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "बड़ी अर्जी: ${record.bigArziQty}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaroonPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (record.smallArziQty > 0) {
                        Surface(
                            color = SaffronPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "छोटी अर्जी: ${record.smallArziQty}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFBF360C),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(timeStr, fontSize = 10.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Row: 1-Tap Toggle Checkmark, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1-Tap Paid Status Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTogglePaid(!record.isPaid) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = record.isPaid,
                        onCheckedChange = { onTogglePaid(it) },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (record.isPaid) "✅ चुकता (Paid)" else "⏳ बकाया (Pending)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (record.isPaid) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Text("✏️", fontSize = 16.sp)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Text("🗑️", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
