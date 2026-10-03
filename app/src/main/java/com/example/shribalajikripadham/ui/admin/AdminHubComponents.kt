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
import com.example.shribalajikripadham.theme.MaroonPrimary
import com.example.shribalajikripadham.theme.SaffronPrimary

/**
 * Model representing any Admin module or task in the Unified Control Hub.
 * Can scale from 29 items to 28,000 items with instant search, category filtering,
 * and dual Grid / List views.
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
        // 1. Token Queue
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
                if (isHindi) "आश्रम पर्चे" else "Sacred Parchas"
            )
        ),
        // 2. Manual Token
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
        // 3. Voice & 5-API
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
        // 4. Paper Register Scan
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
        // 5. Sacred Parchas
        AdminHubModuleItem(
            tabTitle = if (isHindi) "आश्रम पर्चे" else "Sacred Parchas",
            icon = "📜",
            titleHindi = "आश्रम पर्चे",
            titleEnglish = "Sacred Parchas",
            categoryHindi = "🎫 टोकन व दर्शन",
            descriptionHindi = "भक्तों के दिव्य पर्चे, समाधान व आध्यात्मिक इतिहास",
            descriptionEnglish = "Divine parchas, spiritual remedies & historical records",
            relatedTabs = listOf(
                if (isHindi) "टोकन कतार" else "Tokens",
                if (isHindi) "अर्जी लेजर 📦" else "Arzi Ledger 📦",
                if (isHindi) "🔥 हवन आवेदन" else "🔥 Havan Requests"
            )
        ),
        // 6. Tuesday Darbar
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🚩 मंगलवार दरबार" else "Tuesday Darbar",
            icon = "🚩",
            titleHindi = "मंगलवार दरबार",
            titleEnglish = "Tuesday Darbar",
            categoryHindi = "🎫 टोकन व दर्शन",
            descriptionHindi = "मंगलवार विशेष दरबार सेटिंग्स, समय व व्यवस्था",
            descriptionEnglish = "Tuesday special darbar schedule, limits and settings",
            relatedTabs = listOf(
                if (isHindi) "टोकन कतार" else "Tokens",
                if (isHindi) "सेवाएं ऑन/ऑफ" else "Services",
                if (isHindi) "सुपर कंट्रोल" else "Super Control"
            )
        ),
        // 7. Payment Ledger
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
                if (isHindi) "बस बुकिंग लेजर" else "Bus Ledger"
            )
        ),
        // 8. Arzi Ledger
        AdminHubModuleItem(
            tabTitle = if (isHindi) "अर्जी लेजर 📦" else "Arzi Ledger 📦",
            icon = "📦",
            titleHindi = "अर्जी लेजर 📦",
            titleEnglish = "Arzi Ledger",
            categoryHindi = "📊 लेजर व खाते",
            descriptionHindi = "नारियल व ध्वजा अर्जी आवेदन, डाक व ट्रैकिंग",
            descriptionEnglish = "Online coconut & flag arzi applications, dispatch and postal tracking",
            relatedTabs = listOf(
                if (isHindi) "पेमेंट लेजर" else "Payment Ledger",
                if (isHindi) "महा-लेजर 📊" else "Master Ledger 📊",
                if (isHindi) "आश्रम पर्चे" else "Sacred Parchas"
            )
        ),
        // 9. Bus Ledger
        AdminHubModuleItem(
            tabTitle = if (isHindi) "बस बुकिंग लेजर" else "Bus Ledger",
            icon = "🚌",
            titleHindi = "बस बुकिंग लेजर",
            titleEnglish = "Bus Booking Ledger",
            categoryHindi = "📊 लेजर व खाते",
            descriptionHindi = "आश्रम यात्रा बस सीटें, यात्री सूची व किराया प्रबंधन",
            descriptionEnglish = "Yatra bus passenger manifest, seat allocations & tickets",
            relatedTabs = listOf(
                if (isHindi) "पेमेंट लेजर" else "Payment Ledger",
                if (isHindi) "महा-लेजर 📊" else "Master Ledger 📊",
                if (isHindi) "सेवाएं ऑन/ऑफ" else "Services"
            )
        ),
        // 10. Master Ledger
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
                if (isHindi) "बस बुकिंग लेजर" else "Bus Ledger"
            )
        ),
        // 11. Havan Requests
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🔥 हवन आवेदन" else "🔥 Havan Requests",
            icon = "🔥",
            titleHindi = "हवन आवेदन",
            titleEnglish = "Havan Requests",
            categoryHindi = "⚡ आश्रम सेवाएं",
            descriptionHindi = "महा-हवन बुकिंग, आहुति संकल्प व यजमान सूची",
            descriptionEnglish = "Sacred havan bookings, devotee sankalp and priest assignments",
            relatedTabs = listOf(
                if (isHindi) "पेमेंट लेजर" else "Payment Ledger",
                if (isHindi) "आश्रम पर्चे" else "Sacred Parchas",
                if (isHindi) "सेवाएं ऑन/ऑफ" else "Services"
            )
        ),
        // 12. Daily Darshan Studio
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🌺 दैनिक दर्शन" else "Daily Darshan Studio",
            icon = "🌺",
            titleHindi = "दैनिक दर्शन स्टूडियो",
            titleEnglish = "Daily Darshan Studio",
            categoryHindi = "🌺 मीडिया व दर्शन",
            descriptionHindi = "आज के दिव्य श्रृंगार फोटो, स्टेटस व दर्शन गैलरी अपलोड",
            descriptionEnglish = "Daily sacred deity shringar photos, statuses & gallery",
            relatedTabs = listOf(
                if (isHindi) "🔴 लाइव स्टूडियो" else "🔴 Live Studio",
                if (isHindi) "🎵 आरती व भजन प्रबंधन" else "Audio & Aarti Manager",
                if (isHindi) "UI बॉक्स कंट्रोल" else "UI Control"
            )
        ),
        // 13. Live Studio
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🔴 लाइव स्टूडियो" else "🔴 Live Studio",
            icon = "🔴",
            titleHindi = "लाइव स्टूडियो",
            titleEnglish = "Live Broadcast Studio",
            categoryHindi = "🌺 मीडिया व दर्शन",
            descriptionHindi = "YouTube / Facebook लाइव आरती व दरबार प्रसारण लिंक",
            descriptionEnglish = "YouTube & Facebook live darbar broadcasting streams",
            relatedTabs = listOf(
                if (isHindi) "🌺 दैनिक दर्शन" else "Daily Darshan Studio",
                if (isHindi) "🎵 आरती व भजन प्रबंधन" else "Audio & Aarti Manager",
                if (isHindi) "सूचना भेजें" else "Broadcast"
            )
        ),
        // 14. Audio & Aarti Manager
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🎵 आरती व भजन प्रबंधन" else "Audio & Aarti Manager",
            icon = "🎵",
            titleHindi = "आरती व भजन प्रबंधन",
            titleEnglish = "Audio & Aarti Manager",
            categoryHindi = "🌺 मीडिया व दर्शन",
            descriptionHindi = "हनुमान चालीसा, दैनिक आरती व भजनों की ऑडियो लाइब्रेरी",
            descriptionEnglish = "Sacred aartis, Hanuman Chalisa, and bhajan audio library",
            relatedTabs = listOf(
                if (isHindi) "🌺 दैनिक दर्शन" else "Daily Darshan Studio",
                if (isHindi) "🔴 लाइव स्टूडियो" else "🔴 Live Studio",
                if (isHindi) "UI बॉक्स कंट्रोल" else "UI Control"
            )
        ),
        // 15. UI Control
        AdminHubModuleItem(
            tabTitle = if (isHindi) "UI बॉक्स कंट्रोल" else "UI Control",
            icon = "🎛️",
            titleHindi = "UI बॉक्स कंट्रोल",
            titleEnglish = "UI Layout Control",
            categoryHindi = "🌺 मीडिया व दर्शन",
            descriptionHindi = "होम स्क्रीन के बैनर, कार्ड व विज़ुअल टाइल्स ऑन/ऑफ",
            descriptionEnglish = "Home screen card visibility, banners and visual layout",
            relatedTabs = listOf(
                if (isHindi) "ऐप कस्टमाइजर" else "Customizer",
                if (isHindi) "सेवाएं ऑन/ऑफ" else "Services",
                if (isHindi) "🌐 वेबसाइट व CMS" else "Website & CMS"
            )
        ),
        // 16. Broadcast Notifications
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
        // 17. Active Devices
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
        // 18. GPS Location
        AdminHubModuleItem(
            tabTitle = if (isHindi) "GPS लोकेशन" else "Location",
            icon = "📍",
            titleHindi = "GPS लोकेशन",
            titleEnglish = "Ashram GPS Location",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "आश्रम का सही अक्षांश-देशांतर व जियो-फेंसिंग दायरा",
            descriptionEnglish = "Ashram coordinates, geofence radius and proximity check",
            relatedTabs = listOf(
                if (isHindi) "कस्टम दूरियाँ" else "Distances",
                if (isHindi) "सुपर कंट्रोल" else "Super Control",
                if (isHindi) "🛡️ सुरक्षा व ऑडिट" else "Security & Audit"
            )
        ),
        // 19. Custom Distances
        AdminHubModuleItem(
            tabTitle = if (isHindi) "कस्टम दूरियाँ" else "Distances",
            icon = "📏",
            titleHindi = "कस्टम दूरियाँ",
            titleEnglish = "City Distances",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "प्रमुख शहरों से आश्रम की दूरी व यात्रा मार्ग निर्देश",
            descriptionEnglish = "Distance rules, travel directions and kilometer thresholds",
            relatedTabs = listOf(
                if (isHindi) "GPS लोकेशन" else "Location",
                if (isHindi) "बस बुकिंग लेजर" else "Bus Ledger",
                if (isHindi) "🌐 वेबसाइट व CMS" else "Website & CMS"
            )
        ),
        // 20. Services On/Off
        AdminHubModuleItem(
            tabTitle = if (isHindi) "सेवाएं ऑन/ऑफ" else "Services",
            icon = "⚡",
            titleHindi = "सेवाएं ऑन/ऑफ",
            titleEnglish = "Services Matrix",
            categoryHindi = "⚡ आश्रम सेवाएं",
            descriptionHindi = "टोकन, अर्जी, बस व हवन सेवाओं को एक क्लिक में चालू/बंद करें",
            descriptionEnglish = "Master switches for public booking services & features",
            relatedTabs = listOf(
                if (isHindi) "सुपर कंट्रोल" else "Super Control",
                if (isHindi) "🚩 मंगलवार दरबार" else "Tuesday Darbar",
                if (isHindi) "टोकन कतार" else "Tokens"
            )
        ),
        // 21. Sevadar Accounts
        AdminHubModuleItem(
            tabTitle = if (isHindi) "सेवादार खाते" else "Sevadars",
            icon = "👥",
            titleHindi = "सेवादार खाते",
            titleEnglish = "Sevadar Management",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "व्यवस्थापक व सेवादारों के अधिकार, फोटो व पिन प्रबंधन",
            descriptionEnglish = "Sevadar staff permissions, profile photos and security PINs",
            relatedTabs = listOf(
                if (isHindi) "🛡️ सुरक्षा व ऑडिट" else "Security & Audit",
                if (isHindi) "🪪 ID कार्ड स्टूडियो" else "🪪 ID Card Studio",
                if (isHindi) "सुपर कंट्रोल" else "Super Control"
            )
        ),
        // 22. ID Card Studio
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🪪 ID कार्ड स्टूडियो" else "🪪 ID Card Studio",
            icon = "🪪",
            titleHindi = "ID कार्ड स्टूडियो",
            titleEnglish = "ID Card Studio",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "सेवादारों के अधिकृत डिजिटल पहचान पत्र बनाएं व प्रिंट करें",
            descriptionEnglish = "Generate, preview and export official Sevadar ID badges",
            relatedTabs = listOf(
                if (isHindi) "सेवादार खाते" else "Sevadars",
                if (isHindi) "🛡️ सुरक्षा व ऑडिट" else "Security & Audit",
                if (isHindi) "सुपर कंट्रोल" else "Super Control"
            )
        ),
        // 23. Super Control
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
        // 24. Triple Cloud Sync
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
        // 25. Security & Audit
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
        // 26. Website Live Editor
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🌐 वेबसाइट लाइव एडिटर" else "Website Live Editor",
            icon = "🌐",
            titleHindi = "वेबसाइट लाइव एडिटर",
            titleEnglish = "Website Live Editor",
            categoryHindi = "🌺 मीडिया व दर्शन",
            descriptionHindi = "ऑनलाइन वेबसाइट के टेक्स्ट, फोटो व सूचनाएं तुरंत अपडेट करें",
            descriptionEnglish = "Instantly edit live website content, headlines and notices",
            relatedTabs = listOf(
                if (isHindi) "🌐 वेबसाइट व CMS" else "Website & CMS",
                if (isHindi) "UI बॉक्स कंट्रोल" else "UI Control",
                if (isHindi) "🌺 दैनिक दर्शन" else "Daily Darshan Studio"
            )
        ),
        // 27. Website & CMS
        AdminHubModuleItem(
            tabTitle = if (isHindi) "🌐 वेबसाइट व CMS" else "Website & CMS",
            icon = "💻",
            titleHindi = "वेबसाइट व CMS",
            titleEnglish = "Website & CMS",
            categoryHindi = "🌺 मीडिया व दर्शन",
            descriptionHindi = "आश्रम वेब पोर्टल का व्यापक सामग्री व पेज प्रबंधन",
            descriptionEnglish = "Full CMS article and portal configuration",
            relatedTabs = listOf(
                if (isHindi) "🌐 वेबसाइट लाइव एडिटर" else "Website Live Editor",
                if (isHindi) "UI बॉक्स कंट्रोल" else "UI Control",
                if (isHindi) "🌺 दैनिक दर्शन" else "Daily Darshan Studio"
            )
        ),
        // 28. App Customizer
        AdminHubModuleItem(
            tabTitle = if (isHindi) "ऐप कस्टमाइजर" else "Customizer",
            icon = "🎨",
            titleHindi = "ऐप कस्टमाइजर",
            titleEnglish = "App Customizer",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "रंग, थीम, फोंट व धार्मिक विज़ुअल स्टाइलिंग",
            descriptionEnglish = "Color palette, sacred themes, and visual aesthetics",
            relatedTabs = listOf(
                if (isHindi) "UI बॉक्स कंट्रोल" else "UI Control",
                if (isHindi) "सुपर कंट्रोल" else "Super Control",
                if (isHindi) "🌐 वेबसाइट व CMS" else "Website & CMS"
            )
        ),
        // 29. Auto Updates
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
        ),
        // 30. Helpdesk & Queries
        AdminHubModuleItem(
            tabTitle = if (isHindi) "📩 सहायता व सुझाव" else "Helpdesk & Queries",
            icon = "📩",
            titleHindi = "सहायता व सुझाव",
            titleEnglish = "Helpdesk & Queries",
            categoryHindi = "⚙️ सिस्टम व कंट्रोल",
            descriptionHindi = "भक्तों व सेवादारों की समस्याएं, कमियां, सुझाव व सुपर एडमिन समाधान",
            descriptionEnglish = "Devotee and admin issues, complaints, suggestions and replies",
            relatedTabs = listOf(
                if (isHindi) "सेवादार खाते" else "Sevadars",
                if (isHindi) "सुपर कंट्रोल" else "Super Control"
            )
        )
    )
}

/**
 * Dedicated Sub-Header shown at the top of each individual screen/window.
 * Includes a "← मुख्य मेनू (Back to Menu)" button, title, and a horizontal row of
 * related useful window buttons so the user's work is never interrupted!
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onBackToMenu() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("←", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaroonPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentMod != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFF3E0),
                                    border = BorderStroke(0.8.dp, Color(0xFFFFB74D))
                                ) {
                                    Text(
                                        text = currentMod.categoryHindi,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isHindi) "अलग स्वतंत्र स्क्रीन • बैक से मुख्य मेनू पर लौटें" else "Dedicated Window • Back returns to Main Menu",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Button(
                    onClick = onBackToMenu,
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(if (isHindi) "← मुख्य मेनू" else "← Menu", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Quick Related Shortcut Buttons
            if (shortcuts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isHindi) "⚡ उपयोगी शॉर्टकट:" else "⚡ Shortcuts:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    shortcuts.forEach { shortcutTabTitle ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable { onNavigateToModule(shortcutTabTitle) }
                        ) {
                            Text(
                                text = shortcutTabTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaroonPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Main Admin Control Hub showing ALL options in responsive List & Grid View,
 * with search bar and category filters.
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
        "सभी",
        "🎫 टोकन व दर्शन",
        "📊 लेजर व खाते",
        "🌺 मीडिया व दर्शन",
        "⚡ आश्रम सेवाएं",
        "⚙️ सिस्टम व कंट्रोल"
    )

    val filtered = modules.filter { m ->
        (selectedCategory == "सभी" || m.categoryHindi == selectedCategory) &&
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
            .padding(horizontal = 10.dp),
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
                                "काम खोजें (उदा: टोकन, लेजर, आरती, पर्चे, हवन)..."
                            else
                                "Search any task (tokens, ledger, audio, havan)...",
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
                        .height(50.dp),
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
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(14.dp),
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.Black,
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
                            "कुल ${filtered.size} कार्य उपलब्ध (क्लिक करें और अलग विंडो खोलें)"
                        else
                            "${filtered.size} tasks available (tap to open dedicated window)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
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
                                        text = item.categoryHindi.takeLast(6),
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
