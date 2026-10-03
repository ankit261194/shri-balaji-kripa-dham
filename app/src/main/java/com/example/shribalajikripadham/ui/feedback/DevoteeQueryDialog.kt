package com.example.shribalajikripadham.ui.feedback

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shribalajikripadham.data.model.AppQuery
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.hardware.DeviceFingerprintManager
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevoteeQueryDialog(
    isHindi: Boolean,
    repository: AshramRepository,
    initialPhone: String = "",
    initialName: String = "",
    userRole: String = "DEVOTEE", // "DEVOTEE" or "ADMIN"
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: नया प्रश्न/सुझाव, 1: मेरे प्रश्न व उत्तर

    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var city by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("SUGGESTION") } // BUG, SUGGESTION, TOKEN_ISSUE, HAVAN_PARCHA, OTHER
    var customSubject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var submitResult by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    var myQueries by remember { mutableStateOf<List<AppQuery>>(emptyList()) }
    var isLoadingQueries by remember { mutableStateOf(false) }

    val deviceId = remember { DeviceFingerprintManager.getDeviceId(context) }

    fun loadMyQueries() {
        if (phone.isNotBlank()) {
            isLoadingQueries = true
            scope.launch {
                myQueries = repository.getMyQueries(phone.trim(), deviceId)
                isLoadingQueries = false
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            loadMyQueries()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "🚩 सुपर एडमिन सहायता व सुझाव" else "🚩 Super Admin Helpdesk",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaroonPrimary
                        )
                        Text(
                            text = if (isHindi) "समस्या, कमी अथवा सुझाव सीधे सुपर एडमिन को भेजें" else "Send issue or feedback directly to Super Admin",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: New Query vs My Queries
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF5F5F5),
                    contentColor = MaroonPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                if (isHindi) "📝 नया सुझाव / समस्या" else "📝 New Query",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                if (isHindi) "📬 मेरे प्रश्न व उत्तर" else "📬 My Queries & Replies",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // TAB 0: NEW QUERY FORM
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Category Selection
                        Text(
                            text = if (isHindi) "विषय / श्रेणी चुनें:" else "Select Category:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaroonPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategory == "BUG",
                                onClick = { selectedCategory = "BUG" },
                                label = { Text(if (isHindi) "🐛 ऐप में कमी/बग" else "🐛 Bug", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCategory == "SUGGESTION",
                                onClick = { selectedCategory = "SUGGESTION" },
                                label = { Text(if (isHindi) "💡 नया सुझाव" else "💡 Suggestion", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategory == "TOKEN_ISSUE",
                                onClick = { selectedCategory = "TOKEN_ISSUE" },
                                label = { Text(if (isHindi) "🎫 टोकन समस्या" else "🎫 Token", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCategory == "HAVAN_PARCHA",
                                onClick = { selectedCategory = "HAVAN_PARCHA" },
                                label = { Text(if (isHindi) "🔥 हवन/पर्चा प्रश्न" else "🔥 Havan", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCategory == "OTHER",
                                onClick = { selectedCategory = "OTHER" },
                                label = { Text(if (isHindi) "❓ अन्य" else "❓ Other", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(if (isHindi) "आपका पूरा नाम *" else "Full Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) phone = it },
                            label = { Text(if (isHindi) "10-अंकीय मोबाइल नंबर *" else "Mobile Number *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text(if (isHindi) "शहर / जिला (वैकल्पिक)" else "City / District (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customSubject,
                            onValueChange = { customSubject = it },
                            label = { Text(if (isHindi) "विषय / समस्या का शीर्षक (Subject) *" else "Subject / Title *") },
                            placeholder = { Text(if (isHindi) "उदा. टोकन कतार, माइक आवाज, नया सुझाव..." else "e.g. Token queue, mic sound, new suggestion...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text(if (isHindi) "समस्या, कमी अथवा सुझाव का विस्तृत विवरण *" else "Detailed Description *") },
                            placeholder = { Text(if (isHindi) "विस्तार से लिखें कि ऐप में क्या कमी है, क्या सुधार चाहते हैं अथवा क्या परेशानी आ रही है..." else "Describe your issue or suggestion in detail...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 6
                        )

                        submitResult?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, if (isError) Color(0xFFEF5350) else Color(0xFF66BB6A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    color = if (isError) Color(0xFFC62828) else Color(0xFF2E7D32),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (name.isBlank() || phone.length < 10 || message.isBlank()) {
                                    isError = true
                                    submitResult = if (isHindi) "कृपया नाम, सही 10-अंकीय मोबाइल नंबर और विवरण भरें।" else "Please fill all required fields correctly."
                                    return@Button
                                }
                                isSubmitting = true
                                submitResult = null
                                scope.launch {
                                    val (ok, resMsg) = repository.submitAppQuery(
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        city = city.trim(),
                                        role = userRole,
                                        category = selectedCategory,
                                        subject = customSubject.trim().ifBlank { selectedCategory },
                                        message = message.trim(),
                                        deviceId = deviceId
                                    )
                                    isSubmitting = false
                                    isError = !ok
                                    submitResult = resMsg
                                    if (ok) {
                                        customSubject = ""
                                        message = ""
                                        Toast.makeText(context, resMsg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (isHindi) "📤 सुपर एडमिन को भेजें" else "📤 Submit to Super Admin",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // TAB 1: MY QUERIES & SUPER ADMIN REPLIES
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) phone = it },
                                label = { Text(if (isHindi) "अपना मोबाइल नंबर" else "Mobile Number") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { loadMyQueries() },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isHindi) "खोजें" else "Find")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isLoadingQueries) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaroonPrimary)
                            }
                        } else if (myQueries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📭", fontSize = 42.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (isHindi) "इस नंबर से कोई पिछला प्रश्न या सुझाव नहीं मिला।" else "No queries found for this mobile number.",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isHindi) "नया सुझाव या समस्या भेजने के लिए पहले टैब पर जाएं।" else "Switch to the first tab to submit a new query.",
                                        fontSize = 11.sp,
                                        color = MaroonPrimary
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(myQueries) { q ->
                                    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("hi", "IN"))
                                    val dateStr = try { sdf.format(Date(q.createdAt)) } catch (e: Exception) { "" }

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (q.isReplied()) Color(0xFFF1F8E9) else Color(0xFFFAFAFA)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (q.isReplied()) Color(0xFF81C784) else Color(0xFFE0E0E0)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    color = Color(0xFFFFECB3),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = q.getCategoryHindi(),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFE65100),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                Surface(
                                                    color = if (q.isReplied()) Color(0xFF2E7D32) else Color(0xFF757575),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = q.getStatusHindi(),
                                                        fontSize = 10.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = q.message,
                                                fontSize = 13.sp,
                                                color = Color.DarkGray
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "दिनांक: $dateStr",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )

                                            // SUPER ADMIN REPLY SECTION
                                            if (q.isReplied()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Surface(
                                                    color = Color.White,
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("👑", fontSize = 14.sp)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = if (isHindi) "सुपर एडमिन का आधिकारिक उत्तर:" else "Super Admin's Official Reply:",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp,
                                                                color = Color(0xFF1B5E20)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = q.adminReply,
                                                            fontSize = 13.sp,
                                                            color = Color(0xFF2E7D32),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        if (q.repliedBy.isNotBlank()) {
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = "द्वारा: ${q.repliedBy}",
                                                                fontSize = 10.sp,
                                                                color = Color.Gray
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
