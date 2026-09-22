package com.example.shribalajikripadham.ui.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.network.HostingerCentralSyncManager
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteLiveEditorTab(
    isHindi: Boolean,
    settings: AshramSettings,
    onSettingsUpdated: (AshramSettings) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    val scope = rememberCoroutineScope()

    // Form fields mapped directly to live website MySQL ashram_settings
    var ashramName by remember { mutableStateOf(settings.ashramName.ifBlank { "श्री बालाजी कृपा धाम" }) }
    var bannerTitle by remember { mutableStateOf(settings.bannerTitle.ifBlank { "🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट" }) }
    var bannerSubtitle by remember { mutableStateOf(settings.bannerSubtitle.ifBlank { "परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार" }) }
    var isEmergencyNoticeVisible by remember { mutableStateOf(settings.isEmergencyNoticeVisible) }
    var emergencyNoticeText by remember { mutableStateOf(settings.emergencyNoticeText) }

    var isDarbarActive by remember { mutableStateOf(settings.isDarbarActive) }
    var runningTokenNumber by remember { mutableIntStateOf(settings.runningTokenNumber) }
    var darbarTimings by remember { mutableStateOf(settings.darbarTimings.ifBlank { "प्रत्येक रविवार प्रातःकाल 8:00 बजे से" }) }
    var darbarDate by remember { mutableStateOf(settings.darbarDate) }

    var badiArziRate by remember { mutableStateOf(settings.badiArziRate.toString()) }
    var chhotiArziRate by remember { mutableStateOf(settings.chhotiArziRate.toString()) }

    var contactPhone by remember { mutableStateOf(settings.contactPhone.ifBlank { "+91 97206 91090" }) }
    var whatsappNumber by remember { mutableStateOf(settings.whatsappNumber.ifBlank { "+91 97206 91090" }) }
    var upiId by remember { mutableStateOf(settings.ashramUpiId.ifBlank { "shribalajikripadham@upi" }) }
    var upiName by remember { mutableStateOf(settings.ashramUpiName.ifBlank { "श्री बालाजी कृपा धाम" }) }

    var isSaving by remember { mutableStateOf(false) }
    var saveStatusMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Website Status Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaroonPrimary)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "🌐 आधिकारिक वेबसाइट लाइव एडिटर" else "🌐 Official Website Live Editor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "shribalajikripadham.online",
                            fontSize = 12.sp,
                            color = AmberGold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF2E7D32)
                    ) {
                        Text(
                            text = "🟢 LIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isHindi)
                        "यहाँ से आप वेबसाइट के मुख्य शीर्षक, नोटिस पट्टी, दरबार समय, लाइव टोकन एवं सभी नियम मोबाइल ऐप से ही सीधे 1-क्लिक में बदल सकते हैं।"
                    else
                        "Edit website headlines, marquee notices, darbar timings, live serving token and rules directly from this screen.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: Header Titles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "🚩 1. वेबसाइट मुख्य हेडर व शीर्षक" else "🚩 1. Website Header & Titles",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaroonPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ashramName,
                    onValueChange = { ashramName = it },
                    label = { Text("मंदिर का नाम (Ashram Name)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bannerTitle,
                    onValueChange = { bannerTitle = it },
                    label = { Text("मुख्य बैनर हेडिंग (Website Hero Title)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bannerSubtitle,
                    onValueChange = { bannerSubtitle = it },
                    label = { Text("उप-शीर्षक / स्थान (Website Subtitle)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 2: Scrolling Marquee Notice
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "📢 2. लाइव स्क्रॉलिंग विशेष सूचना पट्टी" else "📢 2. Live Marquee Notice Banner",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaroonPrimary
                        )
                        Text(
                            text = if (isEmergencyNoticeVisible) "पट्टी चालू है (Visible)" else "पट्टी छिपी हुई है (Hidden)",
                            fontSize = 12.sp,
                            color = if (isEmergencyNoticeVisible) Color(0xFF2E7D32) else Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Switch(
                        checked = isEmergencyNoticeVisible,
                        onCheckedChange = { isEmergencyNoticeVisible = it }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = emergencyNoticeText,
                    onValueChange = { emergencyNoticeText = it },
                    label = { Text("स्क्रॉलिंग सूचना का टेक्स्ट", fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("उदाहरण: आगामी रविवार को भव्य महारती एवं भंडारे का आयोजन...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 3: Live Darbar & Token Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "⏱️ 3. दरबार दिवस, समय व टोकन स्थिति" else "⏱️ 3. Darbar Timings & Token",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaroonPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDarbarActive) "🟢 दरबार चालू है (Open)" else "🔴 दरबार विश्राम (Closed)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDarbarActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Switch(
                        checked = isDarbarActive,
                        onCheckedChange = { isDarbarActive = it }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = if (runningTokenNumber > 0) runningTokenNumber.toString() else "",
                        onValueChange = { runningTokenNumber = it.toIntOrNull() ?: 0 },
                        label = { Text("लाइव टोकन #", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("उदा. 45") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = darbarDate,
                        onValueChange = { darbarDate = it },
                        label = { Text("आगामी तिथि", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("रविवार, 21 सितम्बर") },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = darbarTimings,
                    onValueChange = { darbarTimings = it },
                    label = { Text("दरबार समय विवरण", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 4: Arzi Rates & Contact Numbers
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "📦 4. अर्जी सेवा दर व संपर्क सूत्र" else "📦 4. Arzi Rates & Contact",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaroonPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = badiArziRate,
                        onValueChange = { badiArziRate = it },
                        label = { Text("बड़ी अर्जी दर (₹)", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = chhotiArziRate,
                        onValueChange = { chhotiArziRate = it },
                        label = { Text("छोटी अर्जी दर (₹)", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("हेल्पलाइन फोन", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = whatsappNumber,
                        onValueChange = { whatsappNumber = it },
                        label = { Text("WhatsApp नंबर", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Status Feedback Message
        saveStatusMsg?.let { msg ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8F5E9)
            ) {
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // SAVE BUTTON (1-Click Push Live to Hostinger)
        Button(
            onClick = {
                isSaving = true
                saveStatusMsg = null
                val updatedSettings = settings.copy(
                    ashramName = ashramName.trim(),
                    bannerTitle = bannerTitle.trim(),
                    bannerSubtitle = bannerSubtitle.trim(),
                    isEmergencyNoticeVisible = isEmergencyNoticeVisible,
                    emergencyNoticeText = emergencyNoticeText.trim(),
                    isDarbarActive = isDarbarActive,
                    runningTokenNumber = runningTokenNumber,
                    darbarTimings = darbarTimings.trim(),
                    darbarDate = darbarDate.trim(),
                    badiArziRate = badiArziRate.toDoubleOrNull() ?: 100.0,
                    chhotiArziRate = chhotiArziRate.toDoubleOrNull() ?: 50.0,
                    contactPhone = contactPhone.trim(),
                    whatsappNumber = whatsappNumber.trim(),
                    ashramUpiId = upiId.trim(),
                    ashramUpiName = upiName.trim()
                )

                scope.launch {
                    try {
                        // 1. Save to local SQLite
                        repository.updateSettings(updatedSettings)
                        onSettingsUpdated(updatedSettings)

                        // 2. Push directly to Hostinger live_config.php MySQL table
                        val (ok, serverMsg) = HostingerCentralSyncManager.updateFullLiveConfig(updatedSettings)
                        isSaving = false
                        if (ok) {
                            saveStatusMsg = if (isHindi)
                                "✅ परिवर्तन वेबसाइट पर तुरंत लाइव हो गए हैं! (shribalajikripadham.online)"
                            else
                                "✅ Changes pushed LIVE to website successfully!"
                            Toast.makeText(context, saveStatusMsg, Toast.LENGTH_LONG).show()
                        } else {
                            saveStatusMsg = "⚠️ लोकल सेव हुआ, सर्वर सिंक संदेश: $serverMsg"
                        }
                    } catch (e: Exception) {
                        isSaving = false
                        saveStatusMsg = "त्रुटि: ${e.message}"
                    }
                }
            },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("वेबसाइट पर लाइव अपडेट हो रहा है...", fontWeight = FontWeight.Bold)
            } else {
                Text(
                    text = if (isHindi) "🚀 वेबसाइट पर तुरंत लाइव सेव करें" else "🚀 Push Live to Website",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Open live website button
        OutlinedButton(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shribalajikripadham.online"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "वेबसाइट खोलने में असमर्थ", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isHindi) "🌐 वेबसाइट खोलकर देखें (shribalajikripadham.online)" else "🌐 Open Live Website",
                fontWeight = FontWeight.SemiBold,
                color = MaroonPrimary
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
