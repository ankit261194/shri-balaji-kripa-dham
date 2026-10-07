package com.example.shribalajikripadham.ui.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class HavanApplicationItem(
    val id: Long,
    val applicationNo: String,
    val devoteeName: String,
    val phoneNumber: String,
    val whatsappNumber: String,
    val preferredDate: String,
    val address: String,
    val gotra: String,
    val familyMembersCount: Int,
    val havanPurpose: String,
    val problemDetails: String,
    val status: String,
    val adminNotes: String,
    val createdAtFormatted: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HavanLedgerTab(
    isHindi: Boolean,
    context: Context,
    adminPin: String = "1234"
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var applications by remember { mutableStateOf<List<HavanApplicationItem>>(emptyList()) }
    var filterStatus by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var totalCount by remember { mutableIntStateOf(0) }
    var pendingCount by remember { mutableIntStateOf(0) }
    var contactedCount by remember { mutableIntStateOf(0) }
    var approvedCount by remember { mutableIntStateOf(0) }

    val repository = remember { com.example.shribalajikripadham.data.repository.AshramRepository(context) }
    var settings by remember { mutableStateOf(com.example.shribalajikripadham.data.model.AshramSettings()) }
    var editableCost by remember { mutableStateOf("14000") }
    var editableRules by remember { mutableStateOf("") }
    var isSavingHavanConfig by remember { mutableStateOf(false) }
    var showHavanConfigEditor by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val s = repository.getSettings()
        settings = s
        editableCost = s.havanEstimatedCost.toString()
        editableRules = s.havanRulesNotice
    }

    // Dialog state for updating status
    var selectedAppForEdit by remember { mutableStateOf<HavanApplicationItem?>(null) }
    var editStatus by remember { mutableStateOf("PENDING") }
    var editNotes by remember { mutableStateOf("") }

    fun fetchHavanApplications() {
        scope.launch {
            isLoading = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val encodedPin = URLEncoder.encode(adminPin, "UTF-8")
                    val encodedStatus = URLEncoder.encode(filterStatus, "UTF-8")
                    val encodedSearch = URLEncoder.encode(searchQuery, "UTF-8")
                    val urlStr = "https://shribalajikripadham.online/api/havan_service.php?action=GET_APPLICATIONS&admin_pin=$encodedPin&status=$encodedStatus&search=$encodedSearch"
                    
                    val conn = URL(urlStr).openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000

                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        text
                    } else {
                        null
                    }
                }

                if (result != null) {
                    val json = JSONObject(result)
                    if (json.optBoolean("success", false)) {
                        val countsObj = json.optJSONObject("counts")
                        if (countsObj != null) {
                            totalCount = countsObj.optInt("total", 0)
                            pendingCount = countsObj.optInt("pending", 0)
                            contactedCount = countsObj.optInt("contacted", 0)
                            approvedCount = countsObj.optInt("approved", 0)
                        }

                        val arr = json.optJSONArray("applications")
                        val list = mutableListOf<HavanApplicationItem>()
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                list.add(
                                    HavanApplicationItem(
                                        id = obj.optLong("id", 0L),
                                        applicationNo = obj.optString("application_no", ""),
                                        devoteeName = obj.optString("devotee_name", ""),
                                        phoneNumber = obj.optString("phone_number", ""),
                                        whatsappNumber = obj.optString("whatsapp_number", ""),
                                        preferredDate = obj.optString("preferred_date", ""),
                                        address = obj.optString("address", ""),
                                        gotra = obj.optString("gotra", ""),
                                        familyMembersCount = obj.optInt("family_members_count", 1),
                                        havanPurpose = obj.optString("havan_purpose", ""),
                                        problemDetails = obj.optString("problem_details", ""),
                                        status = obj.optString("status", "PENDING"),
                                        adminNotes = obj.optString("admin_notes", ""),
                                        createdAtFormatted = obj.optString("formatted_created_at", "")
                                    )
                                )
                            }
                        }
                        applications = list
                    }
                }
            } catch (e: Exception) {
                // Ignore network error gracefully
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(filterStatus) {
        fetchHavanApplications()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFBF8F5))
            .padding(8.dp)
    ) {
        // Top Header with Refresh Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isHindi) "🔥 हवन व अनुष्ठान आवेदन लेजर" else "🔥 Havan Applications Ledger",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaroonPrimary
                )
                Text(
                    text = if (isHindi) "खर्च: ~₹14,000 | वाहन किराया: भगत द्वारा देय" else "Estimated ₹14,000 | Travel: By Devotee",
                    fontSize = 11.sp,
                    color = Color(0xFFBF360C),
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = { fetchHavanApplications() }) {
                Text("🔄", fontSize = 18.sp)
            }
        }

        // Super Admin CMS Control Card for Havan Estimated Cost & Rules
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
            border = BorderStroke(1.dp, Color(0xFFFFB300))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHavanConfigEditor = !showHavanConfigEditor },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚙️ ", fontSize = 16.sp)
                        Column {
                            Text(
                                text = if (isHindi) "हवन सेटिंग्स व खर्च नियंत्रक (Super Admin CMS)" else "Havan Cost & Rules CMS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFBF360C)
                            )
                            Text(
                                text = if (isHindi) "वर्तमान अनुमानित खर्च: ₹${editableCost} | टैप करके बदलें" else "Current: ₹$editableCost",
                                fontSize = 11.sp,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }
                    Text(if (showHavanConfigEditor) "▲ बंद करें" else "▼ बदलें", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                }

                if (showHavanConfigEditor) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editableCost,
                        onValueChange = { editableCost = it.filter { ch -> ch.isDigit() } },
                        label = { Text("अनुमानित खर्च (₹)", fontSize = 12.sp) },
                        placeholder = { Text("उदा: 14000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editableRules,
                        onValueChange = { editableRules = it },
                        label = { Text("हवन नियम व वाहन किराया सूचना", fontSize = 12.sp) },
                        placeholder = { Text("हवन सामग्री खर्च व गाड़ी किराया नियम...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val cVal = editableCost.toIntOrNull() ?: 14000
                            scope.launch {
                                isSavingHavanConfig = true
                                val ok = repository.updateHavanSettings(cVal, editableRules.trim())
                                isSavingHavanConfig = false
                                if (ok) {
                                    settings = repository.getSettings()
                                    Toast.makeText(context, if (isHindi) "✓ हवन सेटिंग्स (खर्च ₹$cVal व नियम) लाइव अपडेट हो गईं!" else "Havan settings updated live!", Toast.LENGTH_SHORT).show()
                                    showHavanConfigEditor = false
                                } else {
                                    Toast.makeText(context, if (isHindi) "अपडेट करने में समस्या हुई" else "Update failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSavingHavanConfig,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                    ) {
                        Text(
                            text = if (isSavingHavanConfig) "अपडेट हो रहा है..." else "💾 सेटिंग्स सुरक्षित करें व लाइव प्रसारित करें",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Stats Summary Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = filterStatus == "ALL",
                onClick = { filterStatus = "ALL" },
                label = { Text("सभी ($totalCount)", fontSize = 11.5.sp) }
            )
            FilterChip(
                selected = filterStatus == "PENDING",
                onClick = { filterStatus = "PENDING" },
                label = { Text("लंबित ($pendingCount)", fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFF9C4)
                )
            )
            FilterChip(
                selected = filterStatus == "CONTACTED",
                onClick = { filterStatus = "CONTACTED" },
                label = { Text("संपर्क ($contactedCount)", fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFE1F5FE)
                )
            )
            FilterChip(
                selected = filterStatus == "APPROVED",
                onClick = { filterStatus = "APPROVED" },
                label = { Text("स्वीकृत ($approvedCount)", fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFE8F5E9)
                )
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                fetchHavanApplications()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            placeholder = { Text(if (isHindi) "नाम, फोन नंबर या क्रमांक खोजें..." else "Search name or phone...", fontSize = 13.sp) },
            leadingIcon = { Text("🔍", fontSize = 14.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaroonPrimary)
            }
        } else if (applications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isHindi) "कोई हवन आवेदन नहीं मिला।" else "No Havan applications found.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(applications, key = { it.id }) { app ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAppForEdit = app
                                editStatus = app.status
                                editNotes = app.adminNotes
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFE0B2))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🔥 ${app.devoteeName} ${if (app.gotra.isNotEmpty()) "(${app.gotra})" else ""}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaroonPrimary
                                    )
                                    Text(
                                        text = "क्रमांक: ${app.applicationNo}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                val (statusText, statusBg, statusColor) = when (app.status) {
                                    "PENDING" -> Triple("लंबित", Color(0xFFFFF9C4), Color(0xFFF57F17))
                                    "CONTACTED" -> Triple("संपर्क किया", Color(0xFFE1F5FE), Color(0xFF0277BD))
                                    "APPROVED" -> Triple("स्वीकृत", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                                    "COMPLETED" -> Triple("पूर्ण", Color(0xFFEDE7F6), Color(0xFF512DA8))
                                    else -> Triple("निरस्त", Color(0xFFFFEBEE), Color(0xFFC62828))
                                }

                                Surface(
                                    color = statusBg,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "🎯 प्रयोजन: ${app.havanPurpose}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD84315)
                            )

                            Text(
                                text = "📅 प्रस्तावित तिथि: ${app.preferredDate}",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )

                            Text(
                                text = "📍 स्थान: ${app.address}",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (app.problemDetails.isNotEmpty()) {
                                Text(
                                    text = "📝 विवरण: \"${app.problemDetails}\"",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF5D4037)
                                )
                            }

                            // Notice tag
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = Color(0xFFF1F8E9),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "✅ सामग्री खर्च (~₹14,000) व गाड़ी किराया भगत द्वारा स्वीकृत",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }

                            if (app.adminNotes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "✍️ नोट: ${app.adminNotes}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6A1B9A)
                                )
                            }

                            // Quick Action Buttons
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${app.phoneNumber}")
                                        }
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("📞", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("कॉल करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val cleanWa = (if (app.whatsappNumber.isNotEmpty()) app.whatsappNumber else app.phoneNumber).replace(Regex("[^0-9]"), "")
                                        val msg = "जय श्री बालाजी महाराज! 🙏\nश्री बालाजी कृपा धाम (डूँगरा जाट) से आपके हवन आवेदन (क्रमांक: ${app.applicationNo}) के संबंध में...\nयजमान: ${app.devoteeName}\nतिथि: ${app.preferredDate}"
                                        val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://wa.me/91$cleanWa?text=${URLEncoder.encode(msg, "UTF-8")}")
                                        }
                                        try { context.startActivity(waIntent) } catch (e: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("WhatsApp 💬", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedAppForEdit = app
                                        editStatus = app.status
                                        editNotes = app.adminNotes
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("स्थिति बदलें", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Status & Notes Edit Dialog
    selectedAppForEdit?.let { target ->
        AlertDialog(
            onDismissRequest = { selectedAppForEdit = null },
            title = {
                Text(
                    text = "हवन आवेदन स्थिति अपडेट",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaroonPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "यजमान: ${target.devoteeName} (${target.applicationNo})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text("स्थिति का चयन करें:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    val statusOptions = listOf(
                        "PENDING" to "⏳ लंबित (Pending)",
                        "CONTACTED" to "📞 संपर्क किया गया (Contacted)",
                        "APPROVED" to "✅ स्वीकृत / तय (Approved)",
                        "COMPLETED" to "🚩 पूर्ण अनुष्ठान (Completed)",
                        "CANCELLED" to "❌ निरस्त (Cancelled)"
                    )

                    statusOptions.forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editStatus = code }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = editStatus == code,
                                onClick = { editStatus = code }
                            )
                            Text(label, fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("व्यवस्थापक टिप्पणी (Notes)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val ok = withContext(Dispatchers.IO) {
                                    val url = URL("https://shribalajikripadham.online/api/havan_service.php")
                                    val conn = url.openConnection() as HttpURLConnection
                                    conn.requestMethod = "POST"
                                    conn.setRequestProperty("Content-Type", "application/json")
                                    conn.doOutput = true

                                    val payload = JSONObject().apply {
                                        put("action", "UPDATE_STATUS")
                                        put("admin_pin", adminPin)
                                        put("id", target.id)
                                        put("application_no", target.applicationNo)
                                        put("status", editStatus)
                                        put("admin_notes", editNotes)
                                    }

                                    conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                                    conn.responseCode == 200
                                }

                                if (ok) {
                                    Toast.makeText(context, "स्थिति अपडेट हो गई!", Toast.LENGTH_SHORT).show()
                                    selectedAppForEdit = null
                                    fetchHavanApplications()
                                } else {
                                    Toast.makeText(context, "अपडेट विफल रहा।", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text("सहेजें (Save)")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForEdit = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
