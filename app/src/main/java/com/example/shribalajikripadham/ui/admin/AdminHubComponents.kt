package com.example.shribalajikripadham.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.theme.AmberGold
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary

/**
 * Pro-Tier Model representing authentic Admin modules in Shri Balaji Kripa Dham.
 * Zero dead features. Instant search, category filtering, and dual Grid / List views.
 */
data class AdminHubModuleItem(
    val tabTitle: String,
    val icon: String,
    val titleHindi: String,
    val titleEnglish: String,
    val categoryHindi: String,
    val descriptionHindi: String,
    val descriptionEnglish: String,
    val relatedTabs: List<String>
)

fun getAshramAdminModules(isHindi: Boolean): List<AdminHubModuleItem> {
    return listOf(
        // ==========================================
        // 1. 🎫 टोकन व दर्शन (Tokens & Darbar)
        // ==========================================
        AdminHubModuleItem(
            tabTitle = if (isHindi) "टोकन कतार" else "Tokens",
            icon = "🎫",
            titleHindi = "टोकन कतार",
            titleEnglish = "Token Queue",
            categoryHindi = "🎫 टोकन व दर्शन",
            descriptionHindi = "लाइव टोकन नंबर बुलाना, कतार व दर्शन नियंत्रण",
            descriptionEnglish = "Live token calling, queue & darshan management",
            relatedTabs = listOf(
                if (isHindi) "मैनुअल टोकन" else "Manual",
                if (isHindi) "🎙️ टोकन वॉइस व 5-API" else "Voice & 5-API",
                if (isHindi) "रजिस्टर स्कैन" else "Register Scan",
                if (isHindi) "🚩 मंगलवार दरबार" else "Tuesday Darbar"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "मैनुअल टोकन" else "Manual",
            icon = "✍️",
            titleHindi = "मैनुअल टोकन",
            titleEnglish = "Manual Tokens",
            categoryHindi = "🎫 टोकन व दर्शन",
            descriptionHindi = "बिना स्मार्टफोन वाले वृद्ध व दूरस्थ भक्तों को हाथ से टोकन दें",
            descriptionEnglish = "Issue offline/manual tokens for elderly & outstation devotees",
            relatedTabs = listOf(
                if (isHindi) "टोकन कतार" else "Tokens",
                if (isHindi) "रजिस्टर स्कैन" else "Register Scan",
                if (isHindi) "🎙️ टोकन वॉइस व 5-API" else "Voice & 5-API"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🎙️ टोकन वॉइस व 5-API" else "Voice & 5-API",
            icon = "🎙️",
            titleHindi = "टोकन वॉइस व 5-API",
            titleEnglish = "Voice & 5-API Pool",
            categoryHindi = "🎫 टोकन व दर्शन",
            descriptionHindi = "ElevenLabs 5-Key पूल, ऑटो-टाइमर (10-30s) व माइक घोषणा",
            descriptionEnglish = "ElevenLabs 5-Key Pool, auto-timer, and microphone announcements",
            relatedTabs = listOf(
                if (isHindi) "टोकन कतार" else "Tokens",
                if (isHindi) "मैनुअल टोकन" else "Manual",
                if (isHindi) "सूचना भेजें" else "Broadcast"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "रजिस्टर स्कैन" else "Register Scan",
            icon = "📷",
            titleHindi = "रजिस्टर स्कैन",
            titleEnglish = "Register Scan",
            categoryHindi = "🎫 टोकन व दर्शन",
            descriptionHindi = "कागजी टोकन रजिस्टर की फोटो खींचकर OCR से डिजिटल करें",
            descriptionEnglish = "Scan paper token register photos and auto-digitize into database",
            relatedTabs = listOf(
                if (isHindi) "टोकन कतार" else "Tokens",
                if (isHindi) "मैनुअल टोकन" else "Manual",
                if (isHindi) "आश्रम पर्चे" else "Sacred Parchas"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🚩 मंगलवार दरबार" else "Tuesday Darbar",
            icon = "🚩",
            titleHindi = "मंगलवार दरबार",
            titleEnglish = "Tuesday Darbar",
            categoryHindi = "🎫 टोकन व दर्शन",
            descriptionHindi = "मंगलवार विशेष दरबार 1-क्लिक मास्टर चालू/बंद व समय व्यवस्था",
            descriptionEnglish = "Tuesday special darbar schedule, master toggle and settings",
            relatedTabs = listOf(
                if (isHindi) "टोकन कतार" else "Tokens",
                if (isHindi) "सेवाएं ऑन/ऑफ" else "Services",
                if (isHindi) "सुपर कंट्रोल" else "Super Control"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "👑 गुरुजी स्क्रीन" else "👑 Guruji Screen",
            icon = "👑",
            titleHindi = "पूज्य गुरुजी दरबार स्क्रीन",
            titleEnglish = "Guruji Big Screen Mode",
            categoryHindi = "🎫 टोकन व दर्शन",
            descriptionHindi = "बुजुर्ग गुरुजी हेतु विशालकाय स्क्रीन — एक टच पर अगला टोकन उद्घोषणा",
            descriptionEnglish = "Elderly-friendly giant screen mode: tap anywhere to announce next token",
            relatedTabs = listOf(
                if (isHindi) "टोकन कतार" else "Tokens Queue",
                if (isHindi) "⚡ एंटीग्रेविटी स्टूडियो" else "⚡ Antigravity Studio",
                if (isHindi) "सुपर कंट्रोल" else "Super Control"
            )
        ),

        // ==========================================
        // 2. 🔥 हवन, अर्जी व पर्चे (Havan, Arzi & Parchas)
        // ==========================================
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🔥 हवन आवेदन" else "🔥 Havan Requests",
            icon = "🔥",
            titleHindi = "हवन आवेदन व सेटिंग्स",
            titleEnglish = "Havan Requests & Settings",
            categoryHindi = "🔥 हवन, अर्जी व पर्चे",
            descriptionHindi = "महा-हवन बुकिंग, ₹14,000 सेवा राशि, यजमान संकल्प व नियम",
            descriptionEnglish = "Sacred havan bookings, devotee sankalp and priest assignments",
            relatedTabs = listOf(
                if (isHindi) "पेमेंट लेजर" else "Payment Ledger",
                if (isHindi) "आश्रम पर्चे" else "Sacred Parchas",
                if (isHindi) "सेवाएं ऑन/ऑफ" else "Services"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "आश्रम पर्चे" else "Sacred Parchas",
            icon = "📜",
            titleHindi = "आश्रम पर्चे",
            titleEnglish = "Sacred Parchas",
            categoryHindi = "🔥 हवन, अर्जी व पर्चे",
            descriptionHindi = "भक्तों के दिव्य पर्चे, समाधान व आध्यात्मिक इतिहास",
            descriptionEnglish = "Divine parchas, spiritual remedies & historical records",
            relatedTabs = listOf(
                if (isHindi) "टोकन कतार" else "Tokens",
                if (isHindi) "अर्जी लेजर 📦" else "Arzi Ledger 📦",
                if (isHindi) "🔥 हवन आवेदन" else "🔥 Havan Requests"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "अर्जी लेजर 📦" else "Arzi Ledger 📦",
            icon = "📦",
            titleHindi = "अर्जी लेजर 📦",
            titleEnglish = "Arzi Ledger",
            categoryHindi = "🔥 हवन, अर्जी व पर्चे",
            descriptionHindi = "नारियल व ध्वजा अर्जी आवेदन, डाक व ट्रैकिंग",
            descriptionEnglish = "Online coconut & flag arzi applications, dispatch and postal tracking",
            relatedTabs = listOf(
                if (isHindi) "पेमेंट लेजर" else "Payment Ledger",
                if (isHindi) "महा-लेजर 📊" else "Master Ledger 📊",
                if (isHindi) "आश्रम पर्चे" else "Sacred Parchas"
            )
        ),

        // ==========================================
        // 3. 👥 सेवादार व स्टाफ (Staff & Sevadars)
        // ==========================================
        AdminHubModuleItem(
            tabTitle = if (isHindi) "💬 धाम सेवादार सहायता केंद्र" else "💬 Sevadar Helpdesk",
            icon = "💬",
            titleHindi = "धाम सेवादार सहायता केंद्र",
            titleEnglish = "Sevadar Helpdesk & Chat",
            categoryHindi = "👥 सेवादार व स्टाफ",
            descriptionHindi = "भक्तों के लाइव प्रश्न, सेवादार चैट व वॉइस कॉल प्रबंधन",
            descriptionEnglish = "Live devotee chat inquiries, sevadar direct messaging and calling",
            relatedTabs = listOf(
                if (isHindi) "सेवादार खाते" else "Sevadars",
                if (isHindi) "सूचना भेजें" else "Broadcast",
                if (isHindi) "सुपर कंट्रोल" else "Super Control"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "सेवादार खाते" else "Sevadars",
            icon = "👥",
            titleHindi = "सेवादार खाते",
            titleEnglish = "Sevadar Management",
            categoryHindi = "👥 सेवादार व स्टाफ",
            descriptionHindi = "व्यवस्थापक व सेवादारों के अधिकार, फोटो व पिन प्रबंधन",
            descriptionEnglish = "Sevadar staff permissions, profile photos and security PINs",
            relatedTabs = listOf(
                if (isHindi) "🛡️ सुरक्षा व ऑडिट" else "Security & Audit",
                if (isHindi) "🪪 ID कार्ड स्टूडियो" else "🪪 ID Card Studio",
                if (isHindi) "सुपर कंट्रोल" else "Super Control"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🪪 ID कार्ड स्टूडियो" else "🪪 ID Card Studio",
            icon = "🪪",
            titleHindi = "ID कार्ड स्टूडियो",
            titleEnglish = "ID Card Studio",
            categoryHindi = "👥 सेवादार व स्टाफ",
            descriptionHindi = "सेवादारों के अधिकृत डिजिटल पहचान पत्र बनाएं व प्रिंट करें",
            descriptionEnglish = "Generate, preview and export official Sevadar ID badges",
            relatedTabs = listOf(
                if (isHindi) "सेवादार खाते" else "Sevadars",
                if (isHindi) "🛡️ सुरक्षा व ऑडिट" else "Security & Audit",
                if (isHindi) "सुपर कंट्रोल" else "Super Control"
            )
        ),

        // ==========================================
        // 4. 🌺 दर्शन व मीडिया (Darshan & Media)
        // ==========================================
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🌺 दैनिक दर्शन" else "Daily Darshan Studio",
            icon = "🌺",
            titleHindi = "दैनिक दर्शन स्टूडियो",
            titleEnglish = "Daily Darshan Studio",
            categoryHindi = "🌺 दर्शन व मीडिया",
            descriptionHindi = "आज के दिव्य श्रृंगार फोटो अपलोड करें (Hostinger लाइव सिंक)",
            descriptionEnglish = "Daily sacred deity shringar photos, statuses & gallery",
            relatedTabs = listOf(
                if (isHindi) "🔴 लाइव स्टूडियो" else "🔴 Live Studio",
                if (isHindi) "🎵 आरती व भजन प्रबंधन" else "Audio & Aarti Manager",
                if (isHindi) "सूचना भेजें" else "Broadcast"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🎵 आरती व भजन प्रबंधन" else "Audio & Aarti Manager",
            icon = "🎵",
            titleHindi = "आरती व भजन प्रबंधन",
            titleEnglish = "Audio & Aarti Manager",
            categoryHindi = "🌺 दर्शन व मीडिया",
            descriptionHindi = "14 पावन आरतियां, चालीसा पाठ व MP3 ऑडियो लाइब्रेरी प्रबंधन",
            descriptionEnglish = "Sacred aartis, Hanuman Chalisa, and bhajan audio library",
            relatedTabs = listOf(
                if (isHindi) "🌺 दैनिक दर्शन" else "Daily Darshan Studio",
                if (isHindi) "🔴 लाइव स्टूडियो" else "🔴 Live Studio",
                if (isHindi) "सुपर कंट्रोल" else "Super Control"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🔴 लाइव स्टूडियो" else "🔴 Live Studio",
            icon = "🔴",
            titleHindi = "लाइव स्टूडियो",
            titleEnglish = "Live Broadcast Studio",
            categoryHindi = "🌺 दर्शन व मीडिया",
            descriptionHindi = "YouTube / Facebook लाइव आरती व दरबार प्रसारण लिंक",
            descriptionEnglish = "YouTube & Facebook live darbar broadcasting streams",
            relatedTabs = listOf(
                if (isHindi) "🌺 दैनिक दर्शन" else "Daily Darshan Studio",
                if (isHindi) "🎵 आरती व भजन प्रबंधन" else "Audio & Aarti Manager",
                if (isHindi) "सूचना भेजें" else "Broadcast"
            )
        ),

        // ==========================================
        // 5. 📊 लेजर व खाते (Ledgers & Finance)
        // ==========================================
        AdminHubModuleItem(
            tabTitle = if (isHindi) "पेमेंट लेजर" else "Payment Ledger",
            icon = "💳",
            titleHindi = "पेमेंट लेजर",
            titleEnglish = "Payment Ledger",
            categoryHindi = "📊 लेजर व खाते",
            descriptionHindi = "दान, रसीदें, UPI व ऑनलाइन भुगतान का सत्यापित रिकॉर्ड",
            descriptionEnglish = "Donations, receipts, UPI transactions & payment audit",
            relatedTabs = listOf(
                if (isHindi) "महा-लेजर 📊" else "Master Ledger 📊",
                if (isHindi) "अर्जी लेजर 📦" else "Arzi Ledger 📦",
                if (isHindi) "सेवाएं ऑन/ऑफ" else "Services"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "महा-लेजर 📊" else "Master Ledger 📊",
            icon = "📊",
            titleHindi = "महा-लेजर 📊",
            titleEnglish = "Master Unified Ledger",
            categoryHindi = "📊 लेजर व खाते",
            descriptionHindi = "सम्पूर्ण आय-व्यय, विस्तृत ऑडिट रिपोर्ट व वित्तीय बैलेंस शीट",
            descriptionEnglish = "Unified financial audit, overall income/expense & balance sheet",
            relatedTabs = listOf(
                if (isHindi) "पेमेंट लेजर" else "Payment Ledger",
                if (isHindi) "अर्जी लेजर 📦" else "Arzi Ledger 📦",
                if (isHindi) "सुपर कंट्रोल" else "Super Control"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "बस बुकिंग लेजर" else "Bus Ledger",
            icon = "🚌",
            titleHindi = "बस बुकिंग लेजर",
            titleEnglish = "Bus Ledger",
            categoryHindi = "📊 लेजर व खाते",
            descriptionHindi = "आश्रम बस सेवा सीटें, यात्री विवरण व भाड़ा हिसाब-किताब",
            descriptionEnglish = "Ashram bus booking, seats & passenger ledger",
            relatedTabs = listOf(
                if (isHindi) "पेमेंट लेजर" else "Payment Ledger",
                if (isHindi) "सेवाएं ऑन/ऑफ" else "Services"
            )
        ),

        // ==========================================
        // 6. ⚙️ सिस्टम व कंट्रोल (System & Root Controls)
        // ==========================================
        AdminHubModuleItem(
            tabTitle = if (isHindi) "सेवाएं ऑन/ऑफ" else "Services",
            icon = "⚡",
            titleHindi = "सेवाएं ऑन/ऑफ",
            titleEnglish = "Services Matrix",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "टोकन मोड, मंगलवार दरबार, बस सेवा, जियो-फेंसिंग व अर्जी दरों का मास्टर ऑन/ऑफ",
            descriptionEnglish = "Master switches for public booking services & features",
            relatedTabs = listOf(
                if (isHindi) "सुपर कंट्रोल" else "Super Control",
                if (isHindi) "🚩 मंगलवार दरबार" else "Tuesday Darbar",
                if (isHindi) "टोकन कतार" else "Tokens"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🌐 वेबसाइट लाइव एडिटर" else "Website Live Editor",
            icon = "🌐",
            titleHindi = "वेबसाइट लाइव एडिटर",
            titleEnglish = "Website Live Editor",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "आश्रम वेबसाइट बैनर, हेडलाइंस, लाइव दर्शन लिंक व परिचय बदलें",
            descriptionEnglish = "Update website banner, headlines, live links & details directly from app",
            relatedTabs = listOf(
                if (isHindi) "सेवाएं ऑन/ऑफ" else "Services",
                if (isHindi) "सूचना भेजें" else "Broadcast"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "सूचना भेजें" else "Broadcast",
            icon = "📢",
            titleHindi = "सूचना भेजें",
            titleEnglish = "Push Broadcast",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "सभी भक्तों के फोन पर तत्काल पुश नोटिफिकेशन भेजें",
            descriptionEnglish = "Broadcast instant alerts & messages to all devotee phones",
            relatedTabs = listOf(
                if (isHindi) "टोकन कतार" else "Tokens",
                if (isHindi) "सक्रिय फोन" else "Active Devices",
                if (isHindi) "सुपर कंट्रोल" else "Super Control"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "सक्रिय फोन" else "Active Devices",
            icon = "📱",
            titleHindi = "सक्रिय फोन",
            titleEnglish = "Active Devices",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "वर्तमान में ऐप इस्तेमाल कर रहे फोन व लाइव भक्त सूची",
            descriptionEnglish = "Live connected smartphones and active devotee metrics",
            relatedTabs = listOf(
                if (isHindi) "सूचना भेजें" else "Broadcast",
                if (isHindi) "🛡️ सुरक्षा व ऑडिट" else "Security & Audit",
                if (isHindi) "GPS लोकेशन" else "Location"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "GPS लोकेशन" else "Location",
            icon = "📍",
            titleHindi = "GPS लोकेशन",
            titleEnglish = "Ashram GPS Location",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "आश्रम का सही अक्षांश-देशांतर व 200m जियो-फेंसिंग दायरा",
            descriptionEnglish = "Ashram coordinates, geofence radius and proximity check",
            relatedTabs = listOf(
                if (isHindi) "सुपर कंट्रोल" else "Super Control",
                if (isHindi) "🛡️ सुरक्षा व ऑडिट" else "Security & Audit",
                if (isHindi) "सेवाएं ऑन/ऑफ" else "Services"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "सुपर कंट्रोल" else "Super Control",
            icon = "👑",
            titleHindi = "सुपर कंट्रोल",
            titleEnglish = "Super Admin Control",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "संपूर्ण ऐप की मास्टर सेटिंग्स, आपातकालीन शटडाउन व रीसेट",
            descriptionEnglish = "Master system switches, factory reset & emergency control",
            relatedTabs = listOf(
                if (isHindi) "सेवाएं ऑन/ऑफ" else "Services",
                if (isHindi) "त्रिमूर्ति क्लाउड सिंक ☁️" else "Triple Cloud Sync ☁️",
                if (isHindi) "🛡️ सुरक्षा व ऑडिट" else "Security & Audit"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "⚡ एंटीग्रेविटी स्टूडियो" else "⚡ Antigravity Studio",
            icon = "⚡",
            titleHindi = "⚡ एंटीग्रेविटी मोबाइल स्टूडियो",
            titleEnglish = "Antigravity Mobile Studio",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "रूट मास्टर AI कंट्रोल, 1-टैप टोकन बाईपास, फ़ेक GPS किल-स्विच व लाइव सिंक",
            descriptionEnglish = "Root master AI console, 1-tap token bypass & zero-update live remote sync",
            relatedTabs = listOf(
                if (isHindi) "सुपर कंट्रोल" else "Super Control",
                if (isHindi) "GPS लोकेशन" else "Location",
                if (isHindi) "🛡️ सुरक्षा व ऑडिट" else "Security & Audit"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "त्रिमूर्ति क्लाउड सिंक ☁️" else "Triple Cloud Sync ☁️",
            icon = "☁️",
            titleHindi = "त्रिमूर्ति क्लाउड सिंक",
            titleEnglish = "Triple Cloud Sync",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "Hostinger, Supabase व Firebase तीनों का लाइव बैकअप व सिंक",
            descriptionEnglish = "Hostinger, Supabase, and Firebase real-time triple backup sync",
            relatedTabs = listOf(
                if (isHindi) "सुपर कंट्रोल" else "Super Control",
                if (isHindi) "🛡️ सुरक्षा व ऑडिट" else "Security & Audit",
                if (isHindi) "ऑटो-अपडेट" else "Updates"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🛡️ सुरक्षा व ऑडिट" else "Security & Audit",
            icon = "🛡️",
            titleHindi = "सुरक्षा व ऑडिट",
            titleEnglish = "Security & Audit Trail",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "एडमिन लॉगिन इतिहास, सुरक्षा अलर्ट व विस्तृत ऑडिट ट्रेल",
            descriptionEnglish = "Login audit trail, device fingerprints and security alerts",
            relatedTabs = listOf(
                if (isHindi) "सेवादार खाते" else "Sevadars",
                if (isHindi) "सुपर कंट्रोल" else "Super Control",
                if (isHindi) "त्रिमूर्ति क्लाउड सिंक ☁️" else "Triple Cloud Sync ☁️"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "ऐप कस्टमाइजर" else "Customizer",
            icon = "🎨",
            titleHindi = "पावन थीम व कस्टमाइजर",
            titleEnglish = "Sacred Themes & Customizer",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "4 दिव्य उत्सव थीम्स (तिरंगा, दीपावली, शेरावाली, हनुमान जी), रंग व आश्रम विवरण",
            descriptionEnglish = "4 Divine sacred themes, cloud festival broadcast, and ashram details",
            relatedTabs = listOf(
                if (isHindi) "सुपर कंट्रोल" else "Super Control",
                if (isHindi) "सेवाएं ऑन/ऑफ" else "Services",
                if (isHindi) "ऑटो-अपडेट" else "Updates"
            )
        ),
        AdminHubModuleItem(
            tabTitle = if (isHindi) "ऑटो-अपडेट" else "Updates",
            icon = "🚀",
            titleHindi = "ऑटो-अपडेट",
            titleEnglish = "Auto Updates & Releases",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "APK रिलीज, सर्वर सिंक व नया वर्जन पब्लिश",
            descriptionEnglish = "Deploy APKs, check version metadata & publish updates",
            relatedTabs = listOf(
                if (isHindi) "त्रिमूर्ति क्लाउड सिंक ☁️" else "Triple Cloud Sync ☁️",
                if (isHindi) "सुपर कंट्रोल" else "Super Control",
                if (isHindi) "🛡️ सुरक्षा व ऑडिट" else "Security & Audit"
            )
        )
    )
}

/**
 * Dedicated Sub-Header shown at the top of each individual screen/window.
 * Ultra-compact 34dp sleek strip: preserves back button, title, quick shortcuts & menu button
 * without wasting vertical screen space!
 */
@Composable
fun AdminDedicatedModuleHeader(
    isHindi: Boolean,
    title: String,
    allModules: List<AdminHubModuleItem>,
    allowedTabs: List<String>,
    onBackToMenu: () -> Unit,
    onNavigateToModule: (String) -> Unit
) {
    val currentMod = allModules.find { it.tabTitle == title }
    val shortcuts = currentMod?.relatedTabs?.filter { allowedTabs.contains(it) } ?: emptyList()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small circular back button
            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onBackToMenu() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("←", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Title + Compact Category Tag
            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp,
                color = MaroonPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (currentMod != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = currentMod.categoryHindi.split(" ").lastOrNull() ?: currentMod.categoryHindi,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Inline Compact Shortcuts
            if (shortcuts.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    shortcuts.take(3).forEach { shortcutTabTitle ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(0.6.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable { onNavigateToModule(shortcutTabTitle) }
                        ) {
                            Text(
                                text = shortcutTabTitle,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaroonPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Quick Menu Return Chip
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaroonPrimary,
                modifier = Modifier.clickable { onBackToMenu() }
            ) {
                Text(
                    text = if (isHindi) "मेनू" else "Menu",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * Main Admin Control Hub showing ALL authentic options in responsive List & Grid View,
 * with search bar and 6 focused operational category filters.
 */
@Composable
fun AdminHubDashboardView(
    isHindi: Boolean,
    modules: List<AdminHubModuleItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String,
    onSelectedCategoryChange: (String) -> Unit,
    isGridView: Boolean,
    onToggleView: (Boolean) -> Unit,
    onSelectModule: (AdminHubModuleItem) -> Unit
) {
    val categories = listOf(
        if (isHindi) "सभी" else "All",
        if (isHindi) "🎫 टोकन व दर्शन" else "🎫 Tokens & Darbar",
        if (isHindi) "🔥 हवन, अर्जी व पर्चे" else "🔥 Havan & Parchas",
        if (isHindi) "👥 सेवादार व स्टाफ" else "👥 Sevadars & Staff",
        if (isHindi) "🌺 दर्शन व मीडिया" else "🌺 Darshan & Media",
        if (isHindi) "📊 लेजर व खाते" else "📊 Ledgers & Accounts",
        if (isHindi) "⚙️ सिस्टम व कंट्रोल" else "⚙️ System & Control"
    )

    val allCatLabel = if (isHindi) "सभी" else "All"

    val filtered = modules.filter { m ->
        (selectedCategory == allCatLabel || selectedCategory == "सभी" || m.categoryHindi == selectedCategory) &&
        (searchQuery.isBlank() ||
         m.titleHindi.contains(searchQuery, ignoreCase = true) ||
         m.titleEnglish.contains(searchQuery, ignoreCase = true) ||
         m.descriptionHindi.contains(searchQuery, ignoreCase = true) ||
         m.descriptionEnglish.contains(searchQuery, ignoreCase = true) ||
         m.categoryHindi.contains(searchQuery, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- SEARCH BAR & VIEW SWITCHER ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = if (isHindi)
                                "कार्य खोजें (उदा: टोकन, हवन, दर्शन, सेवादार, लेजर)..."
                            else
                                "Search any task (tokens, havan, darshan, sevadars)...",
                            fontSize = 12.5.sp
                        )
                    },
                    leadingIcon = {
                        Text("🔍", fontSize = 16.sp)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Text("✕", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaroonPrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                // Category Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat || (selectedCategory == "सभी" && cat == allCatLabel)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaroonPrimary else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaroonPrimary else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.clickable { onSelectedCategoryChange(cat) }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Status banner & List/Grid switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isHindi)
                            "कुल ${filtered.size} सक्रिय कार्य उपलब्ध"
                        else
                            "${filtered.size} active tasks available",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isGridView) MaroonPrimary else Color(0xFFE2E8F0),
                            modifier = Modifier
                                .clickable { onToggleView(true) }
                                .padding(0.dp)
                        ) {
                            Text(
                                text = "▦ ग्रिड",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGridView) Color.White else Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isGridView) MaroonPrimary else Color(0xFFE2E8F0),
                            modifier = Modifier
                                .clickable { onToggleView(false) }
                                .padding(0.dp)
                        ) {
                            Text(
                                text = "☰ लिस्ट",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isGridView) Color.White else Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- MODULE ITEMS DISPLAY ---
        if (filtered.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔍", fontSize = 32.sp)
                    Text(
                        text = if (isHindi) "कोई कार्य नहीं मिला" else "No matching tasks found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = if (isHindi) "कृपया अन्य शब्द खोजें या फ़िल्टर साफ़ करें" else "Try a different search term or clear filters",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else if (isGridView) {
            // --- 2-COLUMN RESPONSIVE GRID VIEW ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.tabTitle }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectModule(item) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(item.icon, fontSize = 20.sp)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFF8E1),
                                    border = BorderStroke(0.6.dp, Color(0xFFFFE082))
                                ) {
                                    Text(
                                        text = item.categoryHindi.split(" ").lastOrNull() ?: item.categoryHindi,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB78103),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = if (isHindi) item.titleHindi else item.titleEnglish,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaroonPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = if (isHindi) item.descriptionHindi else item.descriptionEnglish,
                                fontSize = 10.5.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 14.sp
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFF9EE),
                                border = BorderStroke(0.8.dp, SaffronPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectModule(item) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "खोलें ➜" else "Open ➜",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaroonPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- DETAILED FULL-WIDTH LIST VIEW ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.tabTitle }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectModule(item) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(item.icon, fontSize = 22.sp)
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) item.titleHindi else item.titleEnglish,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaroonPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFF8E1),
                                        border = BorderStroke(0.6.dp, Color(0xFFFFE082))
                                    ) {
                                        Text(
                                            text = item.categoryHindi,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB78103),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = if (isHindi) item.descriptionHindi else item.descriptionEnglish,
                                    fontSize = 11.5.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFF9EE),
                                border = BorderStroke(0.8.dp, SaffronPrimary),
                                modifier = Modifier.clickable { onSelectModule(item) }
                            ) {
                                Text(
                                    text = if (isHindi) "खोलें ➜" else "Open ➜",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaroonPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
