package com.example.shribalajikripadham.ui.feedback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shribalajikripadham.data.model.AshramSevadarContact
import com.example.shribalajikripadham.data.model.SevadarChatMessage
import com.example.shribalajikripadham.data.repository.SevadarDirectoryManager
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SevadarHelpdeskDialog(
    isHindi: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var sevadars by remember { mutableStateOf(SevadarDirectoryManager.getAllSevadars(context)) }
    var selectedDepartment by remember { mutableStateOf("सभी") }
    var searchQuery by remember { mutableStateOf("") }
    var activeChatSevadar by remember { mutableStateOf<AshramSevadarContact?>(null) }

    val departments = listOf(
        "सभी",
        "टोकन व दर्शन",
        "अर्जी व डाक विभाग",
        "बस व यात्रा व्यवस्था",
        "हवन व पूजा सेवा",
        "भंडारा व आवास",
        "सामान्य आश्रम सहायता"
    )

    val filteredSevadars = remember(sevadars, selectedDepartment, searchQuery) {
        sevadars.filter { sev ->
            val matchesDept = selectedDepartment == "सभी" || sev.department.contains(selectedDepartment, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    sev.name.contains(searchQuery, ignoreCase = true) ||
                    sev.department.contains(searchQuery, ignoreCase = true) ||
                    sev.description.contains(searchQuery, ignoreCase = true) ||
                    sev.phoneNumber.contains(searchQuery)
            matchesDept && matchesQuery
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFDFBF7),
            border = BorderStroke(1.dp, Color(0xFFFFD54F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaroonPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👥", fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "धाम सेवादार सहायता केंद्र" else "Ashram Sevadar Helpdesk",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "WhatsApp या इन-ऐप चैट से सीधे जुड़ें" else "Connect via WhatsApp or In-App Chat",
                                fontSize = 11.5.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isHindi) "सेवादार नाम, विभाग या फोन खोजें..." else "Search by name, department or phone...", fontSize = 12.5.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaroonPrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Department Filter Horizontal Scroll
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    departments.forEach { dept ->
                        val isSelected = selectedDepartment == dept
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaroonPrimary else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) MaroonPrimary else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable { selectedDepartment = dept }
                        ) {
                            Text(
                                text = dept,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.DarkGray,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sevadars List
                if (filteredSevadars.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isHindi) "कोई सेवादार नहीं मिला" else "No sevadars found",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredSevadars, key = { it.id }) { sevadar ->
                            SevadarContactCard(
                                sevadar = sevadar,
                                isHindi = isHindi,
                                onConnectWhatsApp = {
                                    val cleanNum = sevadar.whatsappNumber.replace(Regex("[^0-9]"), "").let {
                                        if (it.length == 10) "91$it" else it
                                    }
                                    val waUrl = "https://wa.me/$cleanNum?text=" + Uri.encode("जय श्री राम! मैं श्री बालाजी कृपा धाम ऐप से ${sevadar.department} के संबंध में संपर्क कर रहा हूँ।")
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, if (isHindi) "व्हाट्सएप ऐप खोलने में असमर्थ" else "Unable to open WhatsApp", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onOpenChat = {
                                    activeChatSevadar = sevadar
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // In-App Chat Dialog
    if (activeChatSevadar != null) {
        SevadarInAppChatDialog(
            sevadar = activeChatSevadar!!,
            isHindi = isHindi,
            onDismiss = { activeChatSevadar = null }
        )
    }
}

@Composable
fun SevadarContactCard(
    sevadar: AshramSevadarContact,
    isHindi: Boolean,
    onConnectWhatsApp: () -> Unit,
    onOpenChat: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🙏", fontSize = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sevadar.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaroonPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (sevadar.isAvailable) Color(0xFF2E7D32) else Color.Gray, CircleShape)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "${sevadar.department} • ${sevadar.roleTitleHindi}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (sevadar.isAvailable) Color(0xFFE8F5E9) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = if (sevadar.isAvailable) "🟢 उपलब्ध" else "⚪ व्यस्त",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sevadar.isAvailable) Color(0xFF2E7D32) else Color.Gray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (sevadar.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = sevadar.description,
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two Action Buttons: Direct WhatsApp & In-App Chat/Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // WhatsApp Button
                Button(
                    onClick = onConnectWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHindi) "WhatsApp पर जुड़ें" else "Connect WhatsApp",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // In-App Chat & Call Button
                Button(
                    onClick = onOpenChat,
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📱", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHindi) "इन-ऐप चैट व कॉल" else "In-App Chat & Call",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive In-App Chat Dialog with Sevadar:
 * Supports photo attachments, file attachments, phone dialer calling, video calling and instant messaging!
 */
@Composable
fun SevadarInAppChatDialog(
    sevadar: AshramSevadarContact,
    isHindi: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var messages by remember { mutableStateOf(SevadarDirectoryManager.getChatMessages(context, sevadar.id)) }
    var inputText by remember { mutableStateOf("") }
    var attachedFileUri by remember { mutableStateOf<String?>(null) }
    var attachedType by remember { mutableStateOf("NONE") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachedFileUri = uri.toString()
            attachedType = "PHOTO"
            Toast.makeText(context, if (isHindi) "📷 फोटो चुनी गई" else "Photo attached", Toast.LENGTH_SHORT).show()
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachedFileUri = uri.toString()
            attachedType = "FILE"
            Toast.makeText(context, if (isHindi) "📎 फाइल चुनी गई" else "File attached", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendMessage() {
        if (inputText.isBlank() && attachedFileUri == null) return
        val newMsg = SevadarChatMessage(
            id = "msg_" + System.currentTimeMillis(),
            sevadarId = sevadar.id,
            senderName = "भक्त",
            isFromDevotee = true,
            message = inputText.trim(),
            attachmentUri = attachedFileUri,
            attachmentType = attachedType,
            timestamp = System.currentTimeMillis()
        )
        SevadarDirectoryManager.sendChatMessage(context, newMsg)
        messages = SevadarDirectoryManager.getChatMessages(context, sevadar.id)
        inputText = ""
        attachedFileUri = null
        attachedType = "NONE"

        // Automated polite Ashram ACK if 1st message
        if (messages.size <= 2) {
            val autoReply = SevadarChatMessage(
                id = "reply_" + System.currentTimeMillis(),
                sevadarId = sevadar.id,
                senderName = sevadar.name,
                isFromDevotee = false,
                message = "जय श्री राम! आपका संदेश प्राप्त हो गया है। ${sevadar.roleTitleHindi} शीघ्र आपसे संपर्क करेंगे। आप चाहें तो ऊपर दिए कॉल बटन से सीधे बात भी कर सकते हैं।",
                attachmentUri = null,
                attachmentType = "NONE",
                timestamp = System.currentTimeMillis() + 500
            )
            SevadarDirectoryManager.sendChatMessage(context, autoReply)
            messages = SevadarDirectoryManager.getChatMessages(context, sevadar.id)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 20.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Chat Header Bar with Calling Controls
                Surface(
                    color = MaroonPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { onDismiss() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("←", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = sevadar.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${sevadar.department} • 🟢 ऑनलाइन",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        // Call & Video Calling Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Direct Phone Call
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        try {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${sevadar.phoneNumber}"))
                                            context.startActivity(dialIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "कॉल शुरू करने में असमर्थ", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("📞", fontSize = 16.sp)
                                }
                            }

                            // Video Calling (Direct WhatsApp Video connect)
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0288D1),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        val cleanNum = sevadar.whatsappNumber.replace(Regex("[^0-9]"), "").let {
                                            if (it.length == 10) "91$it" else it
                                        }
                                        val waUrl = "https://wa.me/$cleanNum?text=" + Uri.encode("जय श्री राम! मैं वीडियो कॉल / लाइव सहायता हेतु संपर्क कर रहा हूँ।")
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "वीडियो कॉल लिंक खोलने में असमर्थ", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("📹", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }

                // Chat Messages Feed
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("💬", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isHindi) "सेवादार जी से चैट शुरू करें" else "Start chatting with Sevadar",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = if (isHindi) "फोटो, पर्ची या समस्या लिखकर भेजें" else "Send photo, receipt or message",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    } else {
                        items(messages, key = { it.id }) { msg ->
                            val isMe = msg.isFromDevotee
                            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isMe) 12.dp else 2.dp,
                                        bottomEnd = if (isMe) 2.dp else 12.dp
                                    ),
                                    color = if (isMe) MaroonPrimary else Color.White,
                                    border = if (isMe) null else BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (!isMe) {
                                            Text(
                                                text = msg.senderName,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SaffronPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }

                                        if (msg.attachmentUri != null) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isMe) Color.White.copy(alpha = 0.2f) else Color(0xFFF1F5F9),
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(if (msg.attachmentType == "PHOTO") "📷 फोटो संलग्न" else "📎 फाइल संलग्न", fontSize = 11.sp, color = if (isMe) Color.White else Color.DarkGray)
                                                }
                                            }
                                        }

                                        if (msg.message.isNotBlank()) {
                                            Text(
                                                text = msg.message,
                                                fontSize = 13.sp,
                                                color = if (isMe) Color.White else Color(0xFF1E293B)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = timeStr,
                                            fontSize = 9.sp,
                                            color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray,
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Active attachment preview if any
                if (attachedFileUri != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF3E0),
                        border = BorderStroke(1.dp, Color(0xFFFFB74D))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (attachedType == "PHOTO") "📷 फोटो तैयार है" else "📎 फाइल तैयार है",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaroonPrimary
                            )
                            IconButton(onClick = {
                                attachedFileUri = null
                                attachedType = "NONE"
                            }, modifier = Modifier.size(24.dp)) {
                                Text("✕", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Input Bar with Attachment, Camera, Text & Send
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Camera / Photo Picker
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("📷", fontSize = 18.sp)
                        }

                        // File / Doc Picker
                        IconButton(
                            onClick = { docPickerLauncher.launch("*/*") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("📎", fontSize = 18.sp)
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text(if (isHindi) "संदेश लिखें..." else "Type message...", fontSize = 12.5.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 80.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaroonPrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Send Button
                        Surface(
                            shape = CircleShape,
                            color = MaroonPrimary,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { sendMessage() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("➤", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
