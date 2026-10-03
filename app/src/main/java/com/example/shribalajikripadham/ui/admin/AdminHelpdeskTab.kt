package com.example.shribalajikripadham.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHelpdeskTab(
    isHindi: Boolean,
    repository: AshramRepository,
    superAdminName: String = "सुपर एडमिन (अंकित चौधरी)"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var queries by remember { mutableStateOf<List<AppQuery>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, REPLIED, DEVOTEE, ADMIN
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

    val filteredQueries = remember(queries, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> queries.filter { !it.isReplied() }
            "REPLIED" -> queries.filter { it.isReplied() }
            "DEVOTEE" -> queries.filter { it.senderRole == "DEVOTEE" }
            "ADMIN" -> queries.filter { it.senderRole == "ADMIN" }
            else -> queries
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
                        text = if (isHindi) "📩 भक्त व एडमिन समस्या / सुझाव (Helpdesk)" else "📩 Queries & Suggestions (Helpdesk)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaroonPrimary
                    )
                    Text(
                        text = if (isHindi) "कुल: ${queries.size} | लंबित: $pendingCount" else "Total: ${queries.size} | Pending: $pendingCount",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
                Button(
                    onClick = { refreshQueries() },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isHindi) "🔄 रिफ्रेश" else "🔄 Refresh", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text(if (isHindi) "सभी (${queries.size})" else "All", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "PENDING",
                onClick = { selectedFilter = "PENDING" },
                label = { Text(if (isHindi) "🔴 लंबित ($pendingCount)" else "Pending", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "REPLIED",
                onClick = { selectedFilter = "REPLIED" },
                label = { Text(if (isHindi) "🟢 उत्तर दिया गया" else "Replied", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "DEVOTEE",
                onClick = { selectedFilter = "DEVOTEE" },
                label = { Text(if (isHindi) "भक्त" else "Devotees", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "ADMIN",
                onClick = { selectedFilter = "ADMIN" },
                label = { Text(if (isHindi) "सेवादार/एडमिन" else "Admins", fontSize = 11.sp) }
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
                                    Text("📲 व्हाट्सएप", fontSize = 11.sp)
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

    // REPLY DIALOG
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
