package com.example.shribalajikripadham.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.shribalajikripadham.data.model.AppQuery
import com.example.shribalajikripadham.data.model.AshramSevadarContact
import com.example.shribalajikripadham.data.model.SevadarChatMessage
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.data.repository.SevadarChatRepository
import com.example.shribalajikripadham.data.repository.SevadarDirectoryManager
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHelpdeskTab(
    isHindi: Boolean,
    repository: AshramRepository,
    superAdminName: String = "सुपर एडमिन (अंकित चौधरी)",
    currentUserRole: String = "SUPER_ADMIN",
    currentAdminId: String = "",
    currentAdminPhone: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var queries by remember { mutableStateOf<List<AppQuery>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var activeSourceTab by remember { mutableIntStateOf(0) } // 0: सेवादार/एडमिन प्रश्न, 1: आम भक्त प्रश्न, 2: इन-ऐप चैट
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, REPLIED
    var replyingToQuery by remember { mutableStateOf<AppQuery?>(null) }
    var replyText by remember { mutableStateOf("") }
    var isSendingReply by remember { mutableStateOf(false) }

    fun refreshQueries() {
        isLoading = true
        scope.launch {
            queries = repository.getAllQueriesForSuperAdmin()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshQueries()
    }

    val adminQueries = remember(queries) { queries.filter { it.senderRole == "ADMIN" } }
    val devoteeQueries = remember(queries) { queries.filter { it.senderRole != "ADMIN" } }
    val adminPendingCount = remember(adminQueries) { adminQueries.count { !it.isReplied() } }
    val devoteePendingCount = remember(devoteeQueries) { devoteeQueries.count { !it.isReplied() } }

    val roleQueries = if (activeSourceTab == 0) adminQueries else devoteeQueries
    val filteredQueries = remember(roleQueries, statusFilter) {
        when (statusFilter) {
            "PENDING" -> roleQueries.filter { !it.isReplied() }
            "REPLIED" -> roleQueries.filter { it.isReplied() }
            else -> roleQueries
        }
    }

    val pendingCount = remember(queries) { queries.count { !it.isReplied() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Top Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
            border = BorderStroke(1.dp, Color(0xFFFFD54F)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isHindi) "📩 हेल्पडेस्क: सुझाव व समस्या समाधान" else "📩 Helpdesk: Queries & Feedback",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaroonPrimary
                    )
                    Text(
                        text = if (isHindi) "कुल संदेश: ${queries.size} | लंबित (Pending): $pendingCount" else "Total: ${queries.size} | Pending: $pendingCount",
                        fontSize = 11.5.sp,
                        color = Color.DarkGray
                    )
                }
                Button(
                    onClick = { refreshQueries() },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(if (isHindi) "🔄 रिफ्रेश" else "🔄 Refresh", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3 Primary Tabs: Admin Queries vs Devotee Queries vs In-App Chat
        TabRow(
            selectedTabIndex = activeSourceTab,
            containerColor = Color(0xFFF1F5F9),
            contentColor = MaroonPrimary
        ) {
            Tab(
                selected = activeSourceTab == 0,
                onClick = { activeSourceTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isHindi) "👥 सेवादार प्रश्न (${adminQueries.size})" else "👥 Staff (${adminQueries.size})",
                            fontWeight = if (activeSourceTab == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp
                        )
                        if (adminPendingCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = Color(0xFFC62828),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "$adminPendingCount",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            )
            Tab(
                selected = activeSourceTab == 1,
                onClick = { activeSourceTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isHindi) "🙏 भक्त प्रश्न (${devoteeQueries.size})" else "🙏 Devotees (${devoteeQueries.size})",
                            fontWeight = if (activeSourceTab == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp
                        )
                        if (devoteePendingCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = Color(0xFFE65100),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "$devoteePendingCount",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            )
            Tab(
                selected = activeSourceTab == 2,
                onClick = { activeSourceTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isHindi) "💬 इन-ऐप चैट" else "💬 In-App Chat",
                            fontWeight = if (activeSourceTab == 2) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (activeSourceTab == 2) {
            // Render In-App Chat View with Strict Role-Based Privacy
            AdminInAppChatSection(
                isHindi = isHindi,
                currentUserRole = currentUserRole,
                currentAdminId = currentAdminId,
                currentAdminPhone = currentAdminPhone,
                superAdminName = superAdminName
            )
        } else {
            // Status Sub-filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = statusFilter == "ALL",
                    onClick = { statusFilter = "ALL" },
                    label = { Text(if (isHindi) "सभी (${roleQueries.size})" else "All (${roleQueries.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = statusFilter == "PENDING",
                    onClick = { statusFilter = "PENDING" },
                    label = {
                        Text(
                            text = if (isHindi) "🔴 लंबित (${roleQueries.count { !it.isReplied() }})" else "Pending",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                FilterChip(
                    selected = statusFilter == "REPLIED",
                    onClick = { statusFilter = "REPLIED" },
                    label = {
                        Text(
                            text = if (isHindi) "🟢 समाधान प्राप्त (${roleQueries.count { it.isReplied() }})" else "Replied",
                            fontSize = 11.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaroonPrimary)
                }
            } else if (filteredQueries.isEmpty()) {
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
                            text = if (isHindi) "कोई समस्या या सुझाव उपलब्ध नहीं है।" else "No queries or suggestions found.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredQueries) { q ->
                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("hi", "IN"))
                        val dateStr = try { sdf.format(Date(q.createdAt)) } catch (e: Exception) { "" }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (q.isReplied()) Color(0xFFF9FBE7) else Color.White
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (q.isReplied()) Color(0xFFC5E1A5) else Color(0xFFFFCC80)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Top Row: Category & Status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            color = if (q.senderRole == "ADMIN") Color(0xFFE1BEE7) else Color(0xFFFFECB3),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (q.senderRole == "ADMIN") "👔 व्यवस्थापक/सेवादार" else "🙏 भक्त",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (q.senderRole == "ADMIN") Color(0xFF4A148C) else Color(0xFFE65100),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            color = Color(0xFFE0F2F1),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = q.getCategoryHindi(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF004D40),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        color = if (q.isReplied()) Color(0xFF2E7D32) else Color(0xFFD32F2F),
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

                                Spacer(modifier = Modifier.height(8.dp))

                                // Sender info
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = q.senderName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                    if (q.senderCity.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("(${q.senderCity})", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }

                                Text(
                                    text = "मोबाइल: ${q.senderPhone} • $dateStr",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Devotee Message
                                Surface(
                                    color = Color(0xFFF5F5F5),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = q.message,
                                        fontSize = 13.sp,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                // If replied: show admin response
                                if (q.isReplied()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF81C784)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "👑 सुपर एडमिन का उत्तर (${q.repliedBy}):",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF1B5E20)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = q.adminReply,
                                                fontSize = 13.sp,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Action buttons: Call, WhatsApp, Reply, Delete
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${q.senderPhone}"))
                                            context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("📞 कॉल", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val cleanPh = if (q.senderPhone.startsWith("+91")) q.senderPhone.removePrefix("+91") else q.senderPhone
                                            val waUrl = "https://api.whatsapp.com/send?phone=91$cleanPh"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                            context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("📲 WhatsApp", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            replyingToQuery = q
                                            replyText = q.adminReply
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.3f)
                                    ) {
                                        Text(if (q.isReplied()) "✏️ पुनः उत्तर" else "💬 उत्तर दें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                repository.deleteAppQuery(q.id)
                                                refreshQueries()
                                                Toast.makeText(context, "हटा दिया गया", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                    ) {
                                        Text("🗑️", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // REPLY DIALOG (FOR FEEDBACK/QUERIES)
    replyingToQuery?.let { target ->
        AlertDialog(
            onDismissRequest = { replyingToQuery = null },
            title = {
                Text(
                    text = "उत्तर दें: ${target.senderName} (${target.senderPhone})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaroonPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "प्रश्न: \"${target.message}\"",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("सुपर एडमिन का उत्तर संदेश") },
                        placeholder = { Text("जैसे: आपकी समस्या का समाधान कर दिया गया है / सुझाव स्वीकार किया गया...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (replyText.isBlank()) return@Button
                        isSendingReply = true
                        scope.launch {
                            val (ok, resMsg) = repository.replyToAppQuery(
                                queryId = target.id,
                                replyMessage = replyText.trim(),
                                repliedBy = superAdminName
                            )
                            isSendingReply = false
                            if (ok) {
                                replyingToQuery = null
                                refreshQueries()
                                Toast.makeText(context, resMsg, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, resMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isSendingReply,
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    if (isSendingReply) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("उत्तर भेजें")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { replyingToQuery = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

/**
 * Section for in-app chat management with strict role-based privacy:
 * - SUPER_ADMIN: Can view all chats across all sevadars and devotees, and reply to any thread.
 * - SEVADAR: Can ONLY view their own chats with devotees. Strictly blocked from viewing other sevadars' chats.
 * - Messages are permanently preserved like WhatsApp and cannot be deleted.
 */
@Composable
fun AdminInAppChatSection(
    isHindi: Boolean,
    currentUserRole: String,
    currentAdminId: String,
    currentAdminPhone: String,
    superAdminName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSuperAdmin = currentUserRole.equals("SUPER_ADMIN", ignoreCase = true)
    val allSevadars by remember { mutableStateOf(SevadarDirectoryManager.getAllSevadars(context)) }

    // Role-based visibility
    val visibleSevadars = remember(allSevadars, isSuperAdmin, currentAdminId, currentAdminPhone) {
        if (isSuperAdmin) {
            allSevadars
        } else {
            val matched = allSevadars.filter { sev ->
                (currentAdminId.isNotBlank() && sev.id.equals(currentAdminId, ignoreCase = true)) ||
                (currentAdminPhone.isNotBlank() && currentAdminPhone.replace(Regex("[^0-9]"), "").endsWith(sev.phoneNumber.replace(Regex("[^0-9]"), "").takeLast(10)))
            }
            if (matched.isNotEmpty()) matched else {
                listOf(
                    AshramSevadarContact(
                        id = currentAdminId.ifBlank { "sev_current" },
                        name = superAdminName,
                        department = "आश्रम सेवा",
                        roleTitleHindi = "सेवादार",
                        phoneNumber = currentAdminPhone,
                        whatsappNumber = currentAdminPhone,
                        isAvailable = true,
                        description = "अधिकृत सेवादार"
                    )
                )
            }
        }
    }

    var selectedSevadar by remember { mutableStateOf<AshramSevadarContact?>(null) }
    var chatRefreshTrigger by remember { mutableIntStateOf(0) }

    if (selectedSevadar != null) {
        val targetSev = selectedSevadar!!
        var liveMessages by remember(targetSev, chatRefreshTrigger) {
            mutableStateOf(
                SevadarDirectoryManager.getChatMessagesForRole(
                    context = context,
                    currentUserRole = currentUserRole,
                    currentAdminId = currentAdminId,
                    currentAdminPhone = currentAdminPhone,
                    targetSevadarId = targetSev.id,
                    targetSevadarPhone = targetSev.phoneNumber
                )
            )
        }

        LaunchedEffect(targetSev.id, chatRefreshTrigger) {
            while (isActive) {
                try {
                    val cloudMsgs = SevadarChatRepository.getMessages(
                        context = context,
                        conversationId = "",
                        sevadarId = targetSev.id,
                        devoteePhone = ""
                    )
                    if (cloudMsgs.isNotEmpty()) {
                        liveMessages = cloudMsgs
                    }
                } catch (e: Exception) {}
                delay(3500L)
            }
        }
        val messages = liveMessages
        var newReplyText by remember { mutableStateOf("") }
        val isAllowed = isSuperAdmin || (currentAdminId.isNotBlank() && targetSev.id.equals(currentAdminId, ignoreCase = true)) ||
                (currentAdminPhone.isNotBlank() && currentAdminPhone.replace(Regex("[^0-9]"), "").endsWith(targetSev.phoneNumber.replace(Regex("[^0-9]"), "").takeLast(10)))

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                border = BorderStroke(1.dp, Color(0xFFAED581)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedSevadar = null }, modifier = Modifier.size(32.dp)) {
                            Text("⬅️", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = targetSev.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaroonPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF2E7D32),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${targetSev.department} (${targetSev.phoneNumber})",
                                        fontSize = 9.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isSuperAdmin) "👑 सुपर एडमिन व्यू (सभी संदेश दृश्यमान)" else "🔒 आपकी निजी सेवादार चैट (सुरक्षित)",
                                fontSize = 10.sp,
                                color = Color.DarkGray
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${messages.size} संदेश",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (!isAllowed) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔒", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "गोपनीयता प्रतिबंध (Privacy Restriction)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFB71C1C)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "आप केवल अपने और भक्तों के बीच की चैट देख सकते हैं। अन्य सेवादारों के संवाद केवल सुपर एडमिन को दृश्यमान हैं।",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { selectedSevadar = null },
                            colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                        ) {
                            Text("सूची पर वापस जाएँ", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Messages List
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💬", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "इस चैट में अभी कोई संदेश नहीं है।",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "भक्तों द्वारा भेजे गए संदेश यहाँ हमेशा सुरक्षित रहेंगे।",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { msg ->
                            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale("hi", "IN"))
                            val timeStr = try { sdf.format(Date(msg.timestamp)) } catch (e: Exception) { "" }
                            val isMe = !msg.isFromDevotee

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) Color(0xFFE8F5E9) else Color.White
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isMe) Color(0xFFA5D6A7) else Color(0xFFE0E0E0)
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isMe) 12.dp else 2.dp,
                                        bottomEnd = if (isMe) 2.dp else 12.dp
                                    ),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isMe) "👔 ${msg.senderName}" else "🙏 ${msg.senderName}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isMe) Color(0xFF1B5E20) else MaroonPrimary
                                            )
                                            Text(
                                                text = timeStr,
                                                fontSize = 9.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = msg.message,
                                            fontSize = 13.sp,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Reply composer (NO DELETE BUTTON - permanent preservation)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(6.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newReplyText,
                            onValueChange = { newReplyText = it },
                            placeholder = { Text("उत्तर संदेश लिखें...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (newReplyText.isNotBlank()) {
                                    val textToSend = newReplyText.trim()
                                    newReplyText = ""
                                    val replyMsg = SevadarChatMessage(
                                        id = "reply_" + System.currentTimeMillis() + "_" + (100..999).random(),
                                        sevadarId = targetSev.id,
                                        sevadarName = targetSev.name,
                                        senderName = if (isSuperAdmin) "सुपर एडमिन ($superAdminName)" else superAdminName,
                                        senderRole = if (isSuperAdmin) "SUPER_ADMIN" else "SEVADAR",
                                        isFromDevotee = false,
                                        message = textToSend,
                                        attachmentUri = null,
                                        attachmentType = "NONE",
                                        status = "DELIVERED",
                                        timestamp = System.currentTimeMillis()
                                    )
                                    scope.launch {
                                        SevadarChatRepository.sendMessage(context, replyMsg)
                                        chatRefreshTrigger++
                                        Toast.makeText(context, "संदेश भेजा गया 🚀", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                            shape = RoundedCornerShape(8.dp),
                            enabled = newReplyText.isNotBlank()
                        ) {
                            Text("भेजें 🚀", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        // Sevadars List View
        Column(modifier = Modifier.fillMaxSize()) {
            // Privacy Banner
            Surface(
                color = if (isSuperAdmin) Color(0xFFFFF8E1) else Color(0xFFE8F5E9),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isSuperAdmin) Color(0xFFFFD54F) else Color(0xFFA5D6A7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isSuperAdmin) "👑" else "🔒", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSuperAdmin) {
                            "सुपर एडमिन: सभी सेवादारों व भक्तों के बीच के लाइव संदेश हमेशा संरक्षित रहेंगे। आप किसी भी सेवादार की चैट खोलकर उत्तर दे सकते हैं।"
                        } else {
                            "सेवादार गोपनीयता: आप केवल अपने और भक्तों के बीच के संदेश देख सकते हैं। अन्य सेवादारों के संदेश केवल सुपर एडमिन देख सकते हैं।"
                        },
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        lineHeight = 15.sp
                    )
                }
            }

            if (visibleSevadars.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("कोई सेवादार संपर्क उपलब्ध नहीं है।", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleSevadars) { sev ->
                        val chatList = SevadarDirectoryManager.getChatMessages(context, sev.id)
                        val lastMsg = chatList.lastOrNull()

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFFFF3E0),
                                        border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("🙏", fontSize = 18.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = sev.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = MaroonPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0xFFF1F5F9),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = sev.department,
                                                    fontSize = 9.5.sp,
                                                    color = Color.DarkGray,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "मो: ${sev.phoneNumber}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        if (lastMsg != null) {
                                            Text(
                                                text = "अंतिम: ${lastMsg.message}",
                                                fontSize = 10.5.sp,
                                                color = Color(0xFF475569),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        color = if (chatList.isNotEmpty()) Color(0xFF2E7D32) else Color(0xFF9E9E9E),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${chatList.size} संदेश",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = { selectedSevadar = sev },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("💬 चैट खोलें", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
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
