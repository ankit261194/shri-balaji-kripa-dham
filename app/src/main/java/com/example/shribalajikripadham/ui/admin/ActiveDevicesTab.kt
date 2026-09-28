package com.example.shribalajikripadham.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.DevicePresence
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.hardware.DeviceFingerprintManager
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonAccent
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveDevicesTab(
    isHindi: Boolean,
    repository: AshramRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentDeviceId = remember { DeviceFingerprintManager.getDeviceId(context) }

    var totalDevices by remember { mutableIntStateOf(0) }
    var activeTodayCount by remember { mutableIntStateOf(0) }
    var devicesList by remember { mutableStateOf<List<DevicePresence>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") } // "ALL", "ONLINE", "USER", "ADMIN"

    val fetchTelemetry: () -> Unit = {
        scope.launch {
            isLoading = true
            try {
                // 1. Populate immediately from local SQLite cache
                val cached = repository.getLocalActiveDevices()
                if (cached.isNotEmpty()) {
                    devicesList = cached
                    totalDevices = cached.size
                }

                // 2. Fetch live data from Central Hostinger MySQL backend
                val result = repository.getActiveDevicesTelemetry()
                totalDevices = result.first
                activeTodayCount = result.second
                devicesList = result.third
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchTelemetry()
    }

    val now = remember { System.currentTimeMillis() }
    val onlineNowCount = remember(devicesList) {
        val currTime = System.currentTimeMillis()
        devicesList.count { it.isOnline || (it.lastSeenAt >= currTime - 15 * 60 * 1000L) }
    }
    val adminCount = remember(devicesList) {
        devicesList.count { it.role == "ADMIN" || it.role == "SUPER_ADMIN" || it.role == "SEVADAR" }
    }
    val devoteeCount = remember(devicesList) {
        devicesList.count { it.role != "ADMIN" && it.role != "SUPER_ADMIN" && it.role != "SEVADAR" }
    }

    val filteredList = remember(devicesList, searchQuery, selectedRoleFilter) {
        val currTime = System.currentTimeMillis()
        devicesList.filter { dev ->
            val isOnline = dev.isOnline || (dev.lastSeenAt >= currTime - 15 * 60 * 1000L)
            val matchesRole = when (selectedRoleFilter) {
                "ONLINE" -> isOnline
                "ADMIN" -> dev.role == "ADMIN" || dev.role == "SUPER_ADMIN" || dev.role == "SEVADAR"
                "USER" -> dev.role != "ADMIN" && dev.role != "SUPER_ADMIN" && dev.role != "SEVADAR"
                else -> true
            }
            val matchesQuery = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase(Locale.getDefault())
                dev.deviceModel.lowercase(Locale.getDefault()).contains(q) ||
                dev.userName.lowercase(Locale.getDefault()).contains(q) ||
                dev.phoneNumber.contains(q) ||
                dev.city.lowercase(Locale.getDefault()).contains(q) ||
                dev.androidVersion.lowercase(Locale.getDefault()).contains(q) ||
                dev.appVersion.lowercase(Locale.getDefault()).contains(q)
            }
            matchesRole && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Live Summary KPI Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9EE)),
                border = BorderStroke(1.5.dp, AmberGold),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "📱 सक्रिय फोन व लाइव दर्शक उपस्थिति" else "📱 Active Devices & Live Presence",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaroonPrimary
                            )
                            Text(
                                text = if (isHindi) "केंद्रीय सर्वर लाइव टेलीमेट्री: कितने फोन में ऐप खुला हुआ है" else "Central Hostinger live telemetry: real-time devices",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }

                        Button(
                            onClick = fetchTelemetry,
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaroonAccent),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (isHindi) "🔄 रीफ्रेश" else "🔄 Refresh",
                                    fontSize = 12.sp,
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 1: Total Phones, Online Now, Active Today
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Devices
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF1565C0))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${devicesList.size.coerceAtLeast(totalDevices)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF0D47A1)
                                )
                                Text(
                                    text = if (isHindi) "📱 कुल फोन" else "📱 Total",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        // Online Now
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            border = BorderStroke(1.2.dp, Color(0xFF2E7D32))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$onlineNowCount",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = if (isHindi) "🟢 ऑनलाइन" else "🟢 Online",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }

                        // Active Today
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF57F17))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$activeTodayCount",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    text = if (isHindi) "⚡ आज सक्रिय" else "⚡ Today",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Devotees Count vs Admin Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF43A047))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$devoteeCount",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = if (isHindi) "🙏 भक्त फोन" else "🙏 Devotees",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SaffronPrimary)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$adminCount",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaroonAccent
                                )
                                Text(
                                    text = if (isHindi) "👑 एडमिन फोन" else "👑 Admins",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(if (isHindi) "🔍 नाम, मोबाइल, फोन मॉडल या एंड्रॉइड से खोजें" else "🔍 Search by name, phone, model or OS") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaroonAccent,
                    unfocusedBorderColor = Color.LightGray
                )
            )
        }

        // Role & Status Filter Chips Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedRoleFilter == "ALL",
                    onClick = { selectedRoleFilter = "ALL" },
                    label = { Text(if (isHindi) "सभी (${devicesList.size})" else "All (${devicesList.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedRoleFilter == "ONLINE",
                    onClick = { selectedRoleFilter = "ONLINE" },
                    label = { Text(if (isHindi) "🟢 ऑनलाइन ($onlineNowCount)" else "🟢 Online ($onlineNowCount)", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedRoleFilter == "USER",
                    onClick = { selectedRoleFilter = "USER" },
                    label = { Text(if (isHindi) "🙏 भक्त ($devoteeCount)" else "Devotees ($devoteeCount)", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedRoleFilter == "ADMIN",
                    onClick = { selectedRoleFilter = "ADMIN" },
                    label = { Text(if (isHindi) "👑 एडमिन ($adminCount)" else "Admins ($adminCount)", fontSize = 11.sp) }
                )
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📱", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHindi) "कोई डिवाइस डेटा उपलब्ध नहीं है।" else "No device telemetry records found.",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isHindi) "जैसे ही भक्त या सेवादार ऐप खोलेंगे, उनके वास्तविक फोन यहाँ तुरंत लाइव दिखाई देंगे।" else "Handsets will appear here live as users open the app.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(filteredList) { dev ->
                DeviceCardItem(
                    dev = dev,
                    isHindi = isHindi,
                    currentDeviceId = currentDeviceId
                )
            }
        }
    }
}

@Composable
private fun DeviceCardItem(
    dev: DevicePresence,
    isHindi: Boolean,
    currentDeviceId: String
) {
    val context = LocalContext.current
    val isThisDevice = dev.deviceId == currentDeviceId
    val now = System.currentTimeMillis()
    val isOnlineNow = dev.isOnline || (dev.lastSeenAt >= now - 15 * 60 * 1000L)
    val isToday = dev.lastSeenAt >= now - 24 * 3600 * 1000L

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isThisDevice) Color(0xFFFFFDF5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isThisDevice) 4.dp else 2.dp),
        border = BorderStroke(
            if (isThisDevice) 2.dp else if (isOnlineNow) 1.5.dp else 1.dp,
            if (isThisDevice) AmberGold else if (isOnlineNow) Color(0xFF43A047) else Color(0xFFE0E0E0)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row with Device Model, "This Device" tag, Online tag, and Role
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isThisDevice) Color(0xFFFFF3E0)
                                else if (isOnlineNow) Color(0xFFE8F5E9)
                                else Color(0xFFE3F2FD)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isThisDevice) "⭐" else if (isOnlineNow) "🟢" else "📱",
                            fontSize = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = dev.deviceModel.ifBlank { "Android Handset" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isThisDevice) MaroonAccent else Color(0xFF0D47A1)
                            )
                        }
                        if (isThisDevice) {
                            Text(
                                text = if (isHindi) "⭐ यह आपका फोन है (This Device)" else "⭐ This is your phone",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaroonAccent
                            )
                        }
                        Text(
                            text = if (dev.userName.isNotBlank()) "👤 ${dev.userName}" else if (isHindi) "👤 श्रद्धालु भक्त" else "👤 Devotee",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaroonPrimary
                        )
                    }
                }

                // Badges column
                Column(horizontalAlignment = Alignment.End) {
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (dev.role) {
                            "SUPER_ADMIN" -> Color(0xFFFFF3E0)
                            "ADMIN" -> Color(0xFFFFF8E1)
                            "SEVADAR" -> Color(0xFFF3E5F5)
                            else -> Color(0xFFE8F5E9)
                        },
                        border = BorderStroke(
                            1.dp,
                            when (dev.role) {
                                "SUPER_ADMIN" -> Color(0xFFE65100)
                                "ADMIN" -> Color(0xFFFFA000)
                                "SEVADAR" -> Color(0xFFAB47BC)
                                else -> Color(0xFF81C784)
                            }
                        )
                    ) {
                        Text(
                            text = when (dev.role) {
                                "SUPER_ADMIN" -> if (isHindi) "👑 सुपर एडमिन" else "👑 Super Admin"
                                "ADMIN" -> if (isHindi) "🛡️ एडमिन" else "🛡️ Admin"
                                "SEVADAR" -> if (isHindi) "🚩 सेवादार" else "🚩 Sevadar"
                                else -> if (isHindi) "🙏 भक्त" else "🙏 Devotee"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (dev.role) {
                                "SUPER_ADMIN" -> Color(0xFFBF360C)
                                "ADMIN" -> Color(0xFFE65100)
                                "SEVADAR" -> Color(0xFF6A1B9A)
                                else -> Color(0xFF1B5E20)
                            },
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Online Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isOnlineNow) Color(0xFFE8F5E9) else if (isToday) Color(0xFFFFFDE7) else Color(0xFFF5F5F5),
                        border = BorderStroke(
                            0.8.dp,
                            if (isOnlineNow) Color(0xFF4CAF50) else if (isToday) Color(0xFFFBC02D) else Color(0xFFBDBDBD)
                        )
                    ) {
                        Text(
                            text = if (isOnlineNow) {
                                if (isHindi) "🟢 ऑनलाइन" else "🟢 Online"
                            } else if (isToday) {
                                if (isHindi) "🟡 आज सक्रिय" else "🟡 Active Today"
                            } else {
                                if (isHindi) "🕒 ऑफलाइन" else "🕒 Offline"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnlineNow) Color(0xFF1B5E20) else if (isToday) Color(0xFFF57F17) else Color.DarkGray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(8.dp))

            // Contact and City Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dev.phoneNumber.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${dev.phoneNumber}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, dev.phoneNumber, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text(
                            text = "📞 ${dev.phoneNumber}",
                            fontSize = 13.sp,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "📞 संपर्क: —",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                if (dev.city.isNotBlank()) {
                    Text(
                        text = "📍 ${dev.city}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Android & App Version Tags Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dev.androidVersion.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = "🤖 ${dev.androidVersion}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE3F2FD)
                ) {
                    Text(
                        text = "📲 App v${dev.appVersion}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0D47A1),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (dev.ipAddress.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF3E5F5)
                    ) {
                        Text(
                            text = "🌐 IP: ${dev.ipAddress}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6A1B9A),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Last Seen Timestamp & Open Count Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeStr = if (dev.lastSeenAt > 0) {
                    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                    }
                    sdf.format(Date(dev.lastSeenAt))
                } else {
                    "हाल ही में (Recently)"
                }

                Text(
                    text = "🕒 अंतिम देखा गया: $timeStr (IST)",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = "🔄 कुल ${dev.openCount} बार",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaroonAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
