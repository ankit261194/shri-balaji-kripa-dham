package com.example.shribalajikripadham.ui.admin

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shribalajikripadham.data.model.AshramSevadarContact
import com.example.shribalajikripadham.data.repository.SevadarDirectoryManager
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSevadarDirectoryDialog(
    isHindi: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var sevadars by remember { mutableStateOf(SevadarDirectoryManager.getAllSevadars(context)) }
    var editingContact by remember { mutableStateOf<AshramSevadarContact?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC)
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
                            text = if (isHindi) "सेवादार डायरेक्टरी प्रबंधन" else "Manage Sevadar Directory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaroonPrimary
                        )
                        Text(
                            text = if (isHindi) "व्हाट्सएप सेवा व इन-ऐप चैट हेतु सेवादार सूची" else "Sevadars for WhatsApp helpdesk & chat",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Add New Sevadar Button
                Button(
                    onClick = {
                        editingContact = AshramSevadarContact(
                            id = "sev_" + System.currentTimeMillis(),
                            name = "",
                            department = "टोकन व दर्शन",
                            roleTitleHindi = "सेवादार",
                            phoneNumber = "",
                            whatsappNumber = "",
                            isAvailable = true,
                            description = ""
                        )
                        showEditDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isHindi) "➕ नया सेवादार जोड़ें" else "➕ Add New Sevadar",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Sevadars
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sevadars, key = { it.id }) { sev ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sev.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaroonPrimary
                                    )
                                    Text(
                                        text = "${sev.department} • ${sev.phoneNumber}",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                    if (sev.description.isNotBlank()) {
                                        Text(
                                            text = sev.description,
                                            fontSize = 10.5.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            editingContact = sev
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("✏️", fontSize = 14.sp)
                                    }

                                    IconButton(
                                        onClick = {
                                            SevadarDirectoryManager.deleteSevadar(context, sev.id)
                                            sevadars = SevadarDirectoryManager.getAllSevadars(context)
                                            Toast.makeText(context, if (isHindi) "हटा दिया गया" else "Deleted", Toast.LENGTH_SHORT).show()
                                        },
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
        }
    }

    if (showEditDialog && editingContact != null) {
        val target = editingContact!!
        var name by remember { mutableStateOf(target.name) }
        var dept by remember { mutableStateOf(target.department) }
        var role by remember { mutableStateOf(target.roleTitleHindi) }
        var phone by remember { mutableStateOf(target.phoneNumber) }
        var wa by remember { mutableStateOf(target.whatsappNumber) }
        var desc by remember { mutableStateOf(target.description) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (isHindi) "सेवादार विवरण दर्ज करें" else "Sevadar Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isHindi) "सेवादार का नाम *" else "Sevadar Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dept,
                        onValueChange = { dept = it },
                        label = { Text(if (isHindi) "विभाग (उदा. टोकन, अर्जी, बस, हवन...) *" else "Department *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text(if (isHindi) "पद / दायित्व (उदा. मुख्य सेवादार)" else "Role Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isHindi) "फोन नंबर *" else "Phone Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = wa,
                        onValueChange = { wa = it },
                        label = { Text(if (isHindi) "व्हाट्सएप नंबर (खाली हो तो फोन नंबर)" else "WhatsApp Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(if (isHindi) "कार्य विवरण / समय" else "Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank() || phone.isBlank()) {
                            Toast.makeText(context, if (isHindi) "कृपया नाम और फोन नंबर दर्ज करें" else "Please enter name and phone", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val updated = target.copy(
                            name = name.trim(),
                            department = dept.trim().ifBlank { "सामान्य सहायता" },
                            roleTitleHindi = role.trim().ifBlank { "सेवादार" },
                            phoneNumber = phone.trim(),
                            whatsappNumber = wa.trim().ifBlank { phone.trim() },
                            description = desc.trim()
                        )
                        val exists = sevadars.any { it.id == updated.id }
                        if (exists) {
                            SevadarDirectoryManager.updateSevadar(context, updated)
                        } else {
                            SevadarDirectoryManager.addSevadar(context, updated)
                        }
                        sevadars = SevadarDirectoryManager.getAllSevadars(context)
                        showEditDialog = false
                        Toast.makeText(context, if (isHindi) "✅ सेवादार डायरेक्टरी अपडेट सफल!" else "Sevadar updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isHindi) "सहेजें" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}
