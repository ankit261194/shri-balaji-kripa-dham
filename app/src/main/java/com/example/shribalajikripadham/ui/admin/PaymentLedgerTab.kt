package com.example.shribalajikripadham.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.PaymentRecord
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonAccent
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentLedgerTab(
    isHindi: Boolean,
    repository: AshramRepository,
    settings: AshramSettings,
    isSuperAdmin: Boolean,
    scope: CoroutineScope,
    context: Context
) {
    // 1. Super Admin Direct Control Guard
    if (!isSuperAdmin && !settings.canAdminViewPaymentHistory) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFCC80)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔒", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isHindi) "केवल सुपर एडमिन के अधीन" else "Super Admin Access Required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaroonPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi)
                            "आश्रम UPI व बस पेमेंट का संपूर्ण वित्तीय इतिहास केवल मुख्य व्यवस्थापक (Super Admin) के अधीन है।\n\nजब सुपर एडमिन 'सेवाएं ऑन/ऑफ' सेटिंग्स में जाकर 'एडमिन को पेमेंट लेजर देखने की अनुमति' चालू करेंगे, तब यह डेटा आपको दिखाई देगा।"
                        else
                            "Payment records are restricted to Super Admin. When Super Admin grants access under Services Settings, this ledger will be visible here.",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        return
    }

    // Accessible: Load records
    var payments by remember { mutableStateOf<List<PaymentRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppFilter by remember { mutableStateOf("सभी") }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<PaymentRecord?>(null) }

    fun refreshPayments() {
        scope.launch {
            isLoading = true
            try {
                payments = repository.getAllPayments()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshPayments()
    }

    val totalAmount = payments.sumOf { it.amount }
    val verifiedCount = payments.count { it.paymentStatus == "VERIFIED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // TOP SUMMARY CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💳", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "आश्रम UPI व पेमेंट ऑडिट लेजर" else "Ashram Payment Audit Ledger",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaroonPrimary
                            )
                        }
                        Text(
                            text = if (isHindi) "PhonePe, GPay, Paytm, BHIM, नकद ऑडिट ट्रेल" else "Complete Payment History & Verification",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { refreshPayments() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("🔄", fontSize = 16.sp)
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    Toast.makeText(context, if (isHindi) "क्लाउड से पेमेंट सिंक हो रहा है..." else "Syncing payments...", Toast.LENGTH_SHORT).show()
                                    val (success, _) = repository.syncLivePaymentsFromGitHub()
                                    refreshPayments()
                                    if (success) {
                                        Toast.makeText(context, if (isHindi) "पेमेंट लेजर सिंक सफल!" else "Payments synced!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, if (isHindi) "स्थानीय डेटा लोड है" else "Local data active", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("☁️", fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "₹${String.format("%.0f", totalAmount)}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = if (isHindi) "कुल प्राप्त राशि" else "Total Received",
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${payments.size}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Text(
                                text = if (isHindi) "कुल लेन-देन" else "Transactions",
                                fontSize = 10.sp,
                                color = Color(0xFF1565C0)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$verifiedCount",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = if (isHindi) "सत्यापित" else "Verified",
                                fontSize = 10.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHindi) "➕ नया पेमेंट दर्ज करें" else "➕ Record Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val (ok, msg) = repository.publishPaymentsToGitHub()
                                Toast.makeText(context, if (ok) (if (isHindi) "पेमेंट लेजर क्लाउड पर सुरक्षित हुआ!" else "Saved to cloud!") else msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isHindi) "☁️ बैकअप" else "☁️ Backup", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SEARCH & FILTER CHIPS
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(if (isHindi) "भक्त का नाम / फोन / UTR नंबर खोजें" else "Search Name / Phone / UTR") },
            leadingIcon = { Text("🔍", fontSize = 14.sp) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Text("✕", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filterOptions = listOf("सभी", "PhonePe", "Google Pay", "Paytm", "BHIM", "Cash", "बस टिकट")
            filterOptions.forEach { filter ->
                FilterChip(
                    selected = selectedAppFilter == filter,
                    onClick = { selectedAppFilter = filter },
                    label = { Text(filter, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // TRANSACTION LIST
        val filteredPayments = payments.filter { p ->
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    p.devoteeName.lowercase().contains(q) ||
                    p.devoteePhone.contains(q) ||
                    p.transactionId.lowercase().contains(q) ||
                    p.seatNumbers.lowercase().contains(q) ||
                    p.purpose.lowercase().contains(q)

            val matchesFilter = when (selectedAppFilter) {
                "सभी" -> true
                "बस टिकट" -> p.purpose.contains("BUS", ignoreCase = true)
                else -> p.paymentApp.contains(selectedAppFilter, ignoreCase = true) || p.paymentMode.contains(selectedAppFilter, ignoreCase = true)
            }

            matchesQuery && matchesFilter
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaroonPrimary)
            }
        } else if (filteredPayments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank() || selectedAppFilter != "सभी")
                        (if (isHindi) "कोई मेल खाता पेमेंट रिकॉर्ड नहीं मिला।" else "No matching payment record.")
                    else
                        (if (isHindi) "अभी तक कोई पेमेंट रिकॉर्ड दर्ज नहीं हुआ है।" else "No payment records found."),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPayments) { pay ->
                    PaymentCard(
                        isHindi = isHindi,
                        payment = pay,
                        isSuperAdmin = isSuperAdmin,
                        onCopyUtr = { utr ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("UTR", utr)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, if (isHindi) "UTR कॉपी हुआ: $utr" else "Copied: $utr", Toast.LENGTH_SHORT).show()
                        },
                        onToggleVerify = {
                            val newStatus = if (pay.paymentStatus == "VERIFIED") "SUCCESS" else "VERIFIED"
                            scope.launch {
                                repository.updatePaymentStatus(pay.paymentId, newStatus, if (newStatus == "VERIFIED") "SUPER_ADMIN" else "")
                                refreshPayments()
                            }
                        },
                        onDelete = {
                            deleteCandidate = pay
                        },
                        onShareWhatsApp = {
                            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            val dt = sdf.format(Date(pay.timestamp))
                            val msg = "🚩 *श्री बालाजी कृपा धाम, डूँगरा जाट*\n" +
                                    "━━━━━━━━━━━━━━━━━━\n" +
                                    "💳 *भुगतान रसीद / ऑडिट विवरण*\n\n" +
                                    "👤 *भक्त का नाम:* ${pay.devoteeName}\n" +
                                    "📱 *मोबाइल नंबर:* ${pay.devoteePhone}\n" +
                                    "💰 *राशि:* ₹${pay.amount}\n" +
                                    "📲 *माध्यम:* ${pay.paymentApp} (${pay.paymentMode})\n" +
                                    "🔢 *UTR / Ref No:* ${pay.transactionId}\n" +
                                    "🏷️ *उद्देश्य:* ${pay.purpose} ${if (pay.seatNumbers.isNotBlank()) "(सीटें: ${pay.seatNumbers})" else ""}\n" +
                                    "⏱️ *समय:* $dt\n" +
                                    "✅ *स्थिति:* ${pay.paymentStatus}\n" +
                                    "━━━━━━━━━━━━━━━━━━\n" +
                                    "जय श्री बालाजी महाराज!"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            }
                            context.startActivity(Intent.createChooser(intent, "रसीद शेयर करें"))
                        }
                    )
                }
            }
        }
    }

    // DELETE CONFIRMATION DIALOG
    if (deleteCandidate != null) {
        val p = deleteCandidate!!
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(if (isHindi) "पेमेंट रिकॉर्ड हटाएं?" else "Delete Payment Record?") },
            text = {
                Text(
                    if (isHindi)
                        "क्या आप निश्चित रूप से ${p.devoteeName} (₹${p.amount}, UTR: ${p.transactionId}) का पेमेंट रिकॉर्ड हटाना चाहते हैं? यह क्रिया स्थायी है।"
                    else
                        "Are you sure you want to permanently delete payment record of ${p.devoteeName} (₹${p.amount})?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val ok = repository.deletePayment(p.paymentId)
                            if (ok) {
                                Toast.makeText(context, if (isHindi) "रिकॉर्ड हटाया गया!" else "Deleted!", Toast.LENGTH_SHORT).show()
                                refreshPayments()
                            }
                            deleteCandidate = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(if (isHindi) "हटाएं" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteCandidate = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // MANUAL / CASH PAYMENT ENTRY DIALOG
    if (showAddDialog) {
        var devName by remember { mutableStateOf("") }
        var devPhone by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var paymentApp by remember { mutableStateOf("PhonePe") }
        var transactionId by remember { mutableStateOf("") }
        var purpose by remember { mutableStateOf("BUS_TICKET") }
        var seatNumbers by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isHindi) "नया पेमेंट रिकॉर्ड दर्ज करें" else "Record Payment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaroonPrimary
                        )
                        IconButton(onClick = { showAddDialog = false }) {
                            Text("✕", fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = devName,
                        onValueChange = { devName = it },
                        label = { Text(if (isHindi) "भक्त का नाम *" else "Devotee Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = devPhone,
                        onValueChange = { devPhone = it },
                        label = { Text(if (isHindi) "मोबाइल नंबर *" else "Phone Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text(if (isHindi) "राशि (₹) *" else "Amount (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isHindi) "भुगतान माध्यम / ऐप:" else "Payment App / Mode:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("PhonePe", "Google Pay", "Paytm", "BHIM", "Cash", "Cred", "अन्य").forEach { app ->
                            FilterChip(
                                selected = paymentApp == app,
                                onClick = { paymentApp = app },
                                label = { Text(app, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = transactionId,
                        onValueChange = { transactionId = it },
                        label = { Text(if (isHindi) "UTR / ट्रांजेक्शन ID (नकद होने पर छोड़ सकते हैं)" else "UTR / Reference ID") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isHindi) "भुगतान का उद्देश्य:" else "Purpose:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("BUS_TICKET" to "बस टिकट", "DONATION" to "दान/सहयोग", "HAWAN_SEVA" to "हवन सेवा", "DHARAMSHALA" to "धर्मशाला").forEach { (key, label) ->
                            FilterChip(
                                selected = purpose == key,
                                onClick = { purpose = key },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (purpose == "BUS_TICKET") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = seatNumbers,
                            onValueChange = { seatNumbers = it },
                            label = { Text(if (isHindi) "सीट नंबर (उदा. 1A, 1B)" else "Seat Numbers (e.g. 1A)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (isHindi) "टिप्पणी / नोट्स" else "Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (devName.isBlank() || devPhone.isBlank() || amountStr.isBlank()) {
                                Toast.makeText(context, if (isHindi) "कृपया नाम, फोन व राशि भरें" else "Please fill all required fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            scope.launch {
                                val rec = PaymentRecord(
                                    devoteeName = devName.trim(),
                                    devoteePhone = devPhone.trim(),
                                    paymentApp = paymentApp,
                                    transactionId = transactionId.trim().ifBlank { "MANUAL-${System.currentTimeMillis() % 10000}" },
                                    amount = amt,
                                    purpose = purpose,
                                    seatNumbers = seatNumbers.trim(),
                                    paymentStatus = "VERIFIED",
                                    paymentMode = if (paymentApp.equals("Cash", ignoreCase = true)) "OFFLINE_CASH" else "UPI_QR",
                                    verifiedBy = "ADMIN",
                                    notes = notes.trim()
                                )
                                repository.recordPayment(rec)
                                Toast.makeText(context, if (isHindi) "पेमेंट रिकॉर्ड सुरक्षित हुआ!" else "Payment recorded!", Toast.LENGTH_SHORT).show()
                                refreshPayments()
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(if (isHindi) "💾 रिकॉर्ड सुरक्षित करें" else "Save Record", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentCard(
    isHindi: Boolean,
    payment: PaymentRecord,
    isSuperAdmin: Boolean,
    onCopyUtr: (String) -> Unit,
    onToggleVerify: () -> Unit,
    onDelete: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(payment.timestamp))

    val appBadgeColor = when {
        payment.paymentApp.contains("PhonePe", ignoreCase = true) -> Color(0xFF5F259F)
        payment.paymentApp.contains("Google", ignoreCase = true) || payment.paymentApp.contains("GPay", ignoreCase = true) -> Color(0xFF1A73E8)
        payment.paymentApp.contains("Paytm", ignoreCase = true) -> Color(0xFF00BAF2)
        payment.paymentApp.contains("BHIM", ignoreCase = true) -> Color(0xFF00796B)
        payment.paymentApp.contains("Cash", ignoreCase = true) -> Color(0xFF388E3C)
        else -> MaroonPrimary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row: Devotee & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = appBadgeColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = payment.paymentApp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = payment.devoteeName.ifBlank { if (isHindi) "भक्त" else "Devotee" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "📞 ${payment.devoteePhone}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format("%.0f", payment.amount)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Surface(
                        color = if (payment.paymentStatus == "VERIFIED") Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (payment.paymentStatus == "VERIFIED") "✓ सत्यापित" else "सफल",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (payment.paymentStatus == "VERIFIED") Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // UTR Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "UTR: ${payment.transactionId}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
                IconButton(
                    onClick = { onCopyUtr(payment.transactionId) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("📋", fontSize = 14.sp)
                }
            }

            // Purpose & Seats
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (payment.purpose == "BUS_TICKET") "🚌 बस यात्रा टिकट ${if (payment.seatNumbers.isNotBlank()) "(सीट: ${payment.seatNumbers})" else ""}" else "🙏 ${payment.purpose}",
                    fontSize = 11.sp,
                    color = Color(0xFF1565C0),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onShareWhatsApp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("📤", fontSize = 14.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isSuperAdmin) {
                        TextButton(
                            onClick = onToggleVerify,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (payment.paymentStatus == "VERIFIED") "असत्यापित करें" else "✓ सत्यापित करें",
                                fontSize = 11.sp,
                                color = if (payment.paymentStatus == "VERIFIED") Color.Gray else Color(0xFF2E7D32)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("🗑️", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
