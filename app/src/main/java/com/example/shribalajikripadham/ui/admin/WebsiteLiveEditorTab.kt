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

    // ZERO DUMMY DATA: No fake +91 97206 91090
    var contactPhone by remember { mutableStateOf(settings.contactPhone.replace("+91 97206 91090", "").trim()) }
    var whatsappNumber by remember { mutableStateOf(settings.whatsappNumber.replace("+91 97206 91090", "").trim()) }
    var contactEmail by remember { mutableStateOf(if (settings.websiteContactEmail == "shribalajikripadham@gmail.com") "" else settings.websiteContactEmail.trim()) }

    // Bank & UPI
    var upiId by remember { mutableStateOf(if (settings.ashramUpiId == "shribalajikripadham@upi") "" else settings.ashramUpiId.trim()) }
    var upiName by remember { mutableStateOf(settings.ashramUpiName.ifBlank { "श्री बालाजी कृपा धाम" }) }
    var bankName by remember { mutableStateOf(settings.websiteBankName) }
    var bankAccountHolder by remember { mutableStateOf(settings.websiteAccountHolder) }
    var bankAccountNumber by remember { mutableStateOf(settings.websiteAccountNumber) }
    var bankIfsc by remember { mutableStateOf(settings.websiteBankIfsc) }
    var bankBranch by remember { mutableStateOf(settings.websiteBankBranch) }

    // Website CMS Sections
    var topBarText by remember { mutableStateOf(settings.websiteTopBarText) }
    var gurujiTitle by remember { mutableStateOf(settings.websiteGurujiTitle) }
    var gurujiBio by remember { mutableStateOf(settings.websiteGurujiBio) }
    var ashramHistoryHindi by remember { mutableStateOf(settings.ashramHistoryHindi) }
    var tokenRulesNotice by remember { mutableStateOf(settings.websiteTokenRuleNotice) }
    var youtubeLiveUrl by remember { mutableStateOf(settings.youtubeLiveUrl) }
    var instagramUrl by remember { mutableStateOf(settings.instagramUrl) }

    var isSaving by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var saveStatusMsg by remember { mutableStateOf<String?>(null) }

    // Helper to refresh live config from server
    val refreshFromLiveServer: suspend () -> Unit = {
        try {
            isRefreshing = true
            val liveJson = HostingerCentralSyncManager.fetchLiveConfig()
            if (liveJson != null && liveJson.optBoolean("success", true)) {
                val cfg = if (liveJson.has("data")) liveJson.getJSONObject("data") else liveJson

                val sContact = cfg.optString("contact_phone", "").replace("+91 97206 91090", "").trim()
                contactPhone = sContact
                val sWa = cfg.optString("whatsapp_number", "").replace("+91 97206 91090", "").trim()
                whatsappNumber = sWa

                val sEmail = cfg.optString("contact_email", "").replace("shribalajikripadham@gmail.com", "").trim()
                contactEmail = sEmail

                val sAshramName = cfg.optString("ashram_name", "")
                if (sAshramName.isNotBlank()) ashramName = sAshramName

                val sBannerTitle = cfg.optString("banner_title", "")
                if (sBannerTitle.isNotBlank()) bannerTitle = sBannerTitle

                val sBannerSub = cfg.optString("banner_subtitle", "")
                if (sBannerSub.isNotBlank()) bannerSubtitle = sBannerSub

                if (cfg.has("is_emergency_notice_visible")) {
                    isEmergencyNoticeVisible = cfg.optInt("is_emergency_notice_visible", 0) == 1
                }
                emergencyNoticeText = cfg.optString("emergency_notice", emergencyNoticeText)

                if (cfg.has("is_darbar_active")) {
                    isDarbarActive = cfg.optInt("is_darbar_active", 1) == 1
                }
                if (cfg.has("current_serving_token")) {
                    runningTokenNumber = cfg.optInt("current_serving_token", runningTokenNumber)
                }
                val sDate = cfg.optString("darbar_date", "")
                if (sDate.isNotBlank()) darbarDate = sDate

                val sTimings = cfg.optString("darbar_timings", "")
                if (sTimings.isNotBlank()) darbarTimings = sTimings

                if (cfg.has("badi_arzi_rate")) badiArziRate = cfg.optDouble("badi_arzi_rate", 0.0).toString()
                if (cfg.has("chhoti_arzi_rate")) chhotiArziRate = cfg.optDouble("chhoti_arzi_rate", 0.0).toString()

                val sUpi = cfg.optString("upi_id", "")
                upiId = if (sUpi == "shribalajikripadham@upi") "" else sUpi

                val sUpiName = cfg.optString("upi_name", "")
                if (sUpiName.isNotBlank()) upiName = sUpiName

                val sBankName = cfg.optString("bank_name", "")
                if (sBankName.isNotBlank()) bankName = sBankName

                val sAccHolder = cfg.optString("bank_account_holder", "")
                if (sAccHolder.isNotBlank()) bankAccountHolder = sAccHolder

                val sAccNum = cfg.optString("bank_account_number", "")
                if (sAccNum.isNotBlank()) bankAccountNumber = sAccNum

                val sIfsc = cfg.optString("bank_ifsc", "")
                if (sIfsc.isNotBlank()) bankIfsc = sIfsc

                val sBranch = cfg.optString("bank_branch", "")
                if (sBranch.isNotBlank()) bankBranch = sBranch

                val sTopBar = cfg.optString("top_bar_text", "")
                if (sTopBar.isNotBlank()) topBarText = sTopBar

                val sGurujiTitle = cfg.optString("guruji_title", "")
                if (sGurujiTitle.isNotBlank()) gurujiTitle = sGurujiTitle

                val sGurujiBio = cfg.optString("guruji_bio", "")
                if (sGurujiBio.isNotBlank()) gurujiBio = sGurujiBio

                val sHistory = cfg.optString("ashram_history", cfg.optString("ashram_history_hindi", ""))
                if (sHistory.isNotBlank()) ashramHistoryHindi = sHistory

                val sTokenRules = cfg.optString("token_rules_notice", cfg.optString("token_rules_summary", ""))
                if (sTokenRules.isNotBlank()) tokenRulesNotice = sTokenRules

                val sYt = cfg.optString("youtube_live_url", "")
                if (sYt.isNotBlank()) youtubeLiveUrl = sYt

                val sInsta = cfg.optString("instagram_url", "")
                if (sInsta.isNotBlank()) instagramUrl = sInsta

                saveStatusMsg = if (isHindi) "✅ वेबसाइट से ताज़ा डेटा सफलतापूर्वक लोड हो गया है।" else "✅ Live website data loaded successfully."
            }
        } catch (e: Exception) {
            saveStatusMsg = "वेबसाइट से लोड नहीं हो सका: ${e.message}"
        } finally {
            isRefreshing = false
        }
    }

    // Auto-fetch fresh server data on open
    LaunchedEffect(Unit) {
        refreshFromLiveServer()
    }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isHindi)
                        "यहाँ से आप वेबसाइट के मुख्य शीर्षक, नोटिस पट्टी, दरबार समय, लाइव टोकन, संपर्क सूत्र एवं नियम मोबाइल ऐप से ही सीधे 1-क्लिक में बदल सकते हैं।"
                    else
                        "Edit website headlines, marquee notices, darbar timings, live serving token and rules directly from this screen.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Refresh Button
                OutlinedButton(
                    onClick = {
                        scope.launch { refreshFromLiveServer() }
                    },
                    enabled = !isRefreshing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("सर्वर से डेटा लोड हो रहा है...", fontSize = 12.sp)
                    } else {
                        Text(
                            if (isHindi) "🔄 लाइव वेबसाइट से ताज़ा डेटा लोड करें" else "🔄 Refresh Latest Data from Server",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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

        // SECTION 4: Arzi Rates & Contact Numbers (Zero Dummy Data Guarantee)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "📞 4. संपर्क सूत्र व अर्जी सेवा दर" else "📞 4. Contact & Arzi Rates",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaroonPrimary
                )
                Text(
                    text = if (isHindi)
                        "⚠️ ध्यान दें: यहाँ केवल वास्तविक मोबाइल नंबर ही दर्ज करें। यदि खाली छोड़ेंगे तो वेबसाइट पर कोई भी फर्जी या डमी नंबर नहीं दिखेगा।"
                    else
                        "Note: Only real contact details will appear. If left blank, contact buttons will remain hidden on the website.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("हेल्पलाइन फोन (Call)", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("खाली छोड़ें या असली नंबर") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = whatsappNumber,
                        onValueChange = { whatsappNumber = it },
                        label = { Text("WhatsApp नंबर", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("उदा. 98xxxxxxxx") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    label = { Text("आधिकारिक ईमेल (Email)", fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("खाली छोड़ें या असली ईमेल") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

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
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 5: Top Bar & Guruji Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "🚩 5. वेबसाइट टॉप पट्टी व गुरुजी विवरण" else "🚩 5. Top Bar & Guruji Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaroonPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = topBarText,
                    onValueChange = { topBarText = it },
                    label = { Text("वेबसाइट की सबसे ऊपर वाली पट्टी (Top Bar)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = gurujiTitle,
                    onValueChange = { gurujiTitle = it },
                    label = { Text("गुरुजी का नाम / पदवी", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = gurujiBio,
                    onValueChange = { gurujiBio = it },
                    label = { Text("गुरुजी का संक्षिप्त परिचय", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 6: Ashram History & Rules
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "📜 6. आश्रम इतिहास व टोकन नियम" else "📜 6. Ashram History & Token Rules",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaroonPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ashramHistoryHindi,
                    onValueChange = { ashramHistoryHindi = it },
                    label = { Text("आश्रम का पावन इतिहास (वेबसाइट पर प्रदर्शित)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = tokenRulesNotice,
                    onValueChange = { tokenRulesNotice = it },
                    label = { Text("टोकन नियम व ऐप डाउनलोड निर्देश", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 7: Bank & Donation Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "🏦 7. बैंक खाता व दान विवरण" else "🏦 7. Bank & Donation Details",
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
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("UPI ID", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("उदा. name@upi") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = upiName,
                        onValueChange = { upiName = it },
                        label = { Text("खाताधारक नाम", fontWeight = FontWeight.SemiBold) },
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
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("बैंक का नाम", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = bankAccountHolder,
                        onValueChange = { bankAccountHolder = it },
                        label = { Text("ट्रस्ट/खाता नाम", fontWeight = FontWeight.SemiBold) },
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
                        value = bankAccountNumber,
                        onValueChange = { bankAccountNumber = it },
                        label = { Text("खाता संख्या (A/C No)", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = bankIfsc,
                        onValueChange = { bankIfsc = it },
                        label = { Text("IFSC कोड", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 8: Social Media & Live Links
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "📺 8. सोशल मीडिया व यूट्यूब लाइव लिंक" else "📺 8. Social Media & Live Links",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaroonPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = youtubeLiveUrl,
                    onValueChange = { youtubeLiveUrl = it },
                    label = { Text("यूट्यूब लाइव स्ट्रीम / वीडियो लिंक", fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("https://www.youtube.com/watch?v=...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = instagramUrl,
                    onValueChange = { instagramUrl = it },
                    label = { Text("इंस्टाग्राम प्रोफ़ाइल लिंक", fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("https://www.instagram.com/...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Status Feedback Message
        saveStatusMsg?.let { msg ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (msg.startsWith("✅")) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
            ) {
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (msg.startsWith("✅")) Color(0xFF1B5E20) else Color(0xFFE65100),
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
                    badiArziRate = badiArziRate.toDoubleOrNull() ?: 0.0,
                    chhotiArziRate = chhotiArziRate.toDoubleOrNull() ?: 0.0,
                    contactPhone = contactPhone.trim(),
                    whatsappNumber = whatsappNumber.trim(),
                    websiteContactEmail = contactEmail.trim(),
                    ashramUpiId = upiId.trim(),
                    ashramUpiName = upiName.trim(),
                    websiteBankName = bankName.trim(),
                    websiteAccountHolder = bankAccountHolder.trim(),
                    websiteAccountNumber = bankAccountNumber.trim(),
                    websiteBankIfsc = bankIfsc.trim(),
                    websiteBankBranch = bankBranch.trim(),
                    websiteTopBarText = topBarText.trim(),
                    websiteGurujiTitle = gurujiTitle.trim(),
                    websiteGurujiBio = gurujiBio.trim(),
                    ashramHistoryHindi = ashramHistoryHindi.trim(),
                    websiteTokenRuleNotice = tokenRulesNotice.trim(),
                    youtubeLiveUrl = youtubeLiveUrl.trim(),
                    instagramUrl = instagramUrl.trim()
                )

                scope.launch {
                    try {
                        isSaving = true
                        // 1. Push directly to Hostinger live_config.php MySQL table
                        val (ok, serverMsg) = HostingerCentralSyncManager.updateFullLiveConfig(updatedSettings)

                        // 2. Save to local SQLite
                        repository.updateSettings(updatedSettings)
                        onSettingsUpdated(updatedSettings)

                        isSaving = false
                        if (ok) {
                            saveStatusMsg = if (isHindi)
                                "✅ परिवर्तन वेबसाइट पर तुरंत 100% लाइव हो गए हैं! (shribalajikripadham.online)"
                            else
                                "✅ Changes pushed LIVE to website successfully!"
                            Toast.makeText(context, saveStatusMsg, Toast.LENGTH_LONG).show()
                        } else {
                            saveStatusMsg = "⚠️ लोकल सेव हुआ, सर्वर संदेश: $serverMsg"
                            Toast.makeText(context, saveStatusMsg, Toast.LENGTH_LONG).show()
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
