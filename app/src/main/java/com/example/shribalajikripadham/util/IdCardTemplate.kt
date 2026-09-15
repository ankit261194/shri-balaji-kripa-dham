package com.example.shribalajikripadham.util

import androidx.compose.ui.graphics.Color

data class IdCardTemplate(
    val id: String,
    val titleHindi: String,
    val titleEnglish: String,
    val category: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val backgroundColor: Color,
    val textColor: Color,
    val ribbonColor: Color,
    val borderStyle: String, // "DOUBLE_GOLD", "SINGLE_GOLD", "ROYAL_FRAME", "MODERN_LINE", "MINIMAL"
    val emblem: String,      // "🚩", "🔱", "🕉️", "🪷", "🛡️", "⭐", "👑", "📜"
    val isVertical: Boolean = true
)

object IdCardTemplateLibrary {

    val TEMPLATES: List<IdCardTemplate> = buildList {
        // ====================================================================
        // 1. राजसी स्वर्णिम व महरून (ROYAL GOLD & MAROON) - 10 Templates
        // ====================================================================
        add(IdCardTemplate("RG_01", "राजसी स्वर्ण महरून", "Royal Gold Maroon", "राजसी स्वर्णिम",
            Color(0xFF800000), Color(0xFFFFD700), Color(0xFFB8860B), Color(0xFFFFF8E7), Color(0xFF330000), Color(0xFF800000), "DOUBLE_GOLD", "👑"))
        add(IdCardTemplate("RG_02", "मुख्य व्यवस्थापक राजसी", "Chief Admin Royal", "राजसी स्वर्णिम",
            Color(0xFF4A0E17), Color(0xFFFFDF73), Color(0xFFC5A059), Color(0xFFFFFFFF), Color(0xFF2B0000), Color(0xFF5B141E), "ROYAL_FRAME", "🚩"))
        add(IdCardTemplate("RG_03", "स्वर्ण मंदिर भव्य", "Golden Temple Glory", "राजसी स्वर्णिम",
            Color(0xFF6B1D2F), Color(0xFFF7C844), Color(0xFFE5A93C), Color(0xFFFFFBF0), Color(0xFF38000E), Color(0xFF6B1D2F), "DOUBLE_GOLD", "🕉️"))
        add(IdCardTemplate("RG_04", "शाही दरबार लाल", "Imperial Darbar Red", "राजसी स्वर्णिम",
            Color(0xFF900C3F), Color(0xFFFFD700), Color(0xFFDAA520), Color(0xFFFFF9F5), Color(0xFF4A0019), Color(0xFF900C3F), "SINGLE_GOLD", "👑"))
        add(IdCardTemplate("RG_05", "दिव्य रुद्राक्ष महरून", "Divine Rudraksha Maroon", "राजसी स्वर्णिम",
            Color(0xFF5C0616), Color(0xFFE6AF2E), Color(0xFFC47B2B), Color(0xFFFCF7F0), Color(0xFF280008), Color(0xFF5C0616), "ROYAL_FRAME", "🪷"))
        add(IdCardTemplate("RG_06", "अशोक चक्र स्वर्णिम", "Ashoka Gold Royal", "राजसी स्वर्णिम",
            Color(0xFF7B1113), Color(0xFFFFE082), Color(0xFFFFA000), Color(0xFFFFFFFF), Color(0xFF3E0506), Color(0xFF7B1113), "DOUBLE_GOLD", "⭐"))
        add(IdCardTemplate("RG_07", "प्राचीन गरुड़ स्तंभ", "Ancient Garuda Pillar", "राजसी स्वर्णिम",
            Color(0xFF581845), Color(0xFFFFC300), Color(0xFFFF5733), Color(0xFFFFF5F5), Color(0xFF330026), Color(0xFF581845), "ROYAL_FRAME", "🔱"))
        add(IdCardTemplate("RG_08", "राज पुरोहित सम्मान", "Raj Purohit Gold", "राजसी स्वर्णिम",
            Color(0xFF8B1E0F), Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFFFDE7), Color(0xFF450D05), Color(0xFF8B1E0F), "DOUBLE_GOLD", "📜"))
        add(IdCardTemplate("RG_09", "स्वर्ण कंगन महरून", "Golden Bangle Maroon", "राजसी स्वर्णिम",
            Color(0xFF641E16), Color(0xFFF9E79F), Color(0xFFD4AC0D), Color(0xFFFDFEFE), Color(0xFF340B06), Color(0xFF641E16), "SINGLE_GOLD", "👑"))
        add(IdCardTemplate("RG_10", "महारानी पद्मावती गोल्ड", "Padmavati Velvet Gold", "राजसी स्वर्णिम",
            Color(0xFF78281F), Color(0xFFEDBB99), Color(0xFFBA4A00), Color(0xFFFBEEE6), Color(0xFF42100A), Color(0xFF78281F), "ROYAL_FRAME", "🪷"))

        // ====================================================================
        // 2. केसरिया व संन्यासी वैदिक विरासत (SAFFRON & VEDIC) - 10 Templates
        // ====================================================================
        add(IdCardTemplate("SF_01", "केसरिया तेज प्रभा", "Saffron Radiant Glow", "केसरिया वैदिक",
            Color(0xFFE65100), Color(0xFFFFB74D), Color(0xFFFF9800), Color(0xFFFFF3E0), Color(0xFF4E1D00), Color(0xFFE65100), "DOUBLE_GOLD", "🚩"))
        add(IdCardTemplate("SF_02", "वैदिक महायज्ञ केसरी", "Vedic Mahayagya Saffron", "केसरिया वैदिक",
            Color(0xFFD84315), Color(0xFFFFCC80), Color(0xFFFF6F00), Color(0xFFFFF8E1), Color(0xFF3E1100), Color(0xFFD84315), "ROYAL_FRAME", "🕉️"))
        add(IdCardTemplate("SF_03", "सूर्य देव प्रभात", "Surya Prabhat Saffron", "केसरिया वैदिक",
            Color(0xFFEF6C00), Color(0xFFFFE082), Color(0xFFFF8F00), Color(0xFFFFFFFF), Color(0xFF4A2000), Color(0xFFEF6C00), "SINGLE_GOLD", "☀️"))
        add(IdCardTemplate("SF_04", "त्रिशूल महाकाल केसरी", "Trishul Mahakal Orange", "केसरिया वैदिक",
            Color(0xFFBF360C), Color(0xFFFFAB40), Color(0xFFFF3D00), Color(0xFFFBE9E7), Color(0xFF3B0B00), Color(0xFFBF360C), "ROYAL_FRAME", "🔱"))
        add(IdCardTemplate("SF_05", "भक्ति रस चंदन", "Bhakti Ras Chandan", "केसरिया वैदिक",
            Color(0xFFF57C00), Color(0xFFFFF176), Color(0xFFFBC02D), Color(0xFFFFFDE7), Color(0xFF552A00), Color(0xFFF57C00), "DOUBLE_GOLD", "🪷"))
        add(IdCardTemplate("SF_06", "हनुमान सिंदूरी तेज", "Hanuman Sindoor Vermilion", "केसरिया वैदिक",
            Color(0xFFC2185B), Color(0xFFFF80AB), Color(0xFFFF4081), Color(0xFFFCE4EC), Color(0xFF4A001E), Color(0xFFC2185B), "SINGLE_GOLD", "🚩"))
        add(IdCardTemplate("SF_07", "संन्यासी भगवा वस्त्र", "Sannyasi Bhagwa Heritage", "केसरिया वैदिक",
            Color(0xFFE64A19), Color(0xFFFFAB91), Color(0xFFFF5722), Color(0xFFFFFFFF), Color(0xFF4E1200), Color(0xFFE64A19), "DOUBLE_GOLD", "🕉️"))
        add(IdCardTemplate("SF_08", "पवित्र गंगा आरती केसरी", "Ganga Aarti Saffron", "केसरिया वैदिक",
            Color(0xFFFF6D00), Color(0xFFFFD180), Color(0xFFFF9100), Color(0xFFFFF9C4), Color(0xFF522200), Color(0xFFFF6D00), "ROYAL_FRAME", "🪷"))
        add(IdCardTemplate("SF_09", "श्री बालाजी ध्वज केसरी", "Balaji Flag Saffron", "केसरिया वैदिक",
            Color(0xFFDD2C00), Color(0xFFFF9E80), Color(0xFFFF3D00), Color(0xFFFFF5F2), Color(0xFF450A00), Color(0xFFDD2C00), "DOUBLE_GOLD", "🚩"))
        add(IdCardTemplate("SF_10", "तपोवन ऋषिकेश भगवा", "Tapovan Vedic Orange", "केसरिया वैदिक",
            Color(0xFFF4511E), Color(0xFFFFCCBC), Color(0xFFFF7043), Color(0xFFFFFFFF), Color(0xFF431303), Color(0xFFF4511E), "SINGLE_GOLD", "🕉️"))

        // ====================================================================
        // 3. सुरक्षा, व्यवस्था व अनुशासन (SECURITY & DISCIPLINE) - 8 Templates
        // ====================================================================
        add(IdCardTemplate("SEC_01", "सुरक्षा प्रहरी नेवी ब्लू", "Security Force Navy Blue", "सुरक्षा व व्यवस्था",
            Color(0xFF0D47A1), Color(0xFF90CAF9), Color(0xFF2196F3), Color(0xFFE3F2FD), Color(0xFF001E4D), Color(0xFF0D47A1), "DOUBLE_GOLD", "🛡️"))
        add(IdCardTemplate("SEC_02", "द्वारपाल मुख्य रक्षक", "Gatekeeper Elite Guard", "सुरक्षा व व्यवस्था",
            Color(0xFF1A237E), Color(0xFFFFD54F), Color(0xFF3949AB), Color(0xFFFFFFFF), Color(0xFF00083D), Color(0xFF1A237E), "ROYAL_FRAME", "🛡️"))
        add(IdCardTemplate("SEC_03", "कतार व अनुशासन प्रमुख", "Queue & Discipline Incharge", "सुरक्षा व व्यवस्था",
            Color(0xFF263238), Color(0xFFCFD8DC), Color(0xFF607D8B), Color(0xFFECEFF1), Color(0xFF0F161A), Color(0xFF263238), "SINGLE_GOLD", "⭐"))
        add(IdCardTemplate("SEC_04", "ब्लैक कमांडो सुरक्षा", "Black Command Elite", "सुरक्षा व व्यवस्था",
            Color(0xFF212121), Color(0xFFFFD700), Color(0xFF757575), Color(0xFFF5F5F5), Color(0xFF000000), Color(0xFF212121), "DOUBLE_GOLD", "🛡️"))
        add(IdCardTemplate("SEC_05", "आश्रम पेट्रोलिंग गार्ड", "Ashram Patrol Guardian", "सुरक्षा व व्यवस्था",
            Color(0xFF004D40), Color(0xFF80CBC4), Color(0xFF00796B), Color(0xFFE0F2F1), Color(0xFF00241E), Color(0xFF004D40), "SINGLE_GOLD", "🛡️"))
        add(IdCardTemplate("SEC_06", "रात्रि पहरेदार स्पेशल", "Night Patrol Special", "सुरक्षा व व्यवस्था",
            Color(0xFF311B92), Color(0xFFB39DDB), Color(0xFF673AB7), Color(0xFFEDE7F6), Color(0xFF130449), Color(0xFF311B92), "ROYAL_FRAME", "⭐"))
        add(IdCardTemplate("SEC_07", "यातायात व पार्किंग प्रभारी", "Parking & Traffic Warden", "सुरक्षा व व्यवस्था",
            Color(0xFF006064), Color(0xFF80DEEA), Color(0xFF0097A7), Color(0xFFE0F7FA), Color(0xFF002B2D), Color(0xFF006064), "SINGLE_GOLD", "🛡️"))
        add(IdCardTemplate("SEC_08", "इमरजेंसी क्विक रिस्पॉन्स", "Emergency Response Team", "सुरक्षा व व्यवस्था",
            Color(0xFFB71C1C), Color(0xFFFFCDD2), Color(0xFFE53935), Color(0xFFFFEBEE), Color(0xFF4A0000), Color(0xFFB71C1C), "DOUBLE_GOLD", "🚨"))

        // ====================================================================
        // 4. अन्नपूर्णा लंगर व रसोई सेवा (LANGAR & SEVA) - 6 Templates
        // ====================================================================
        add(IdCardTemplate("LNG_01", "अन्नपूर्णा कृपा हरित", "Annapurna Green Seva", "लंगर व रसोई",
            Color(0xFF1B5E20), Color(0xFFA5D6A7), Color(0xFF4CAF50), Color(0xFFE8F5E9), Color(0xFF052B08), Color(0xFF1B5E20), "DOUBLE_GOLD", "🪷"))
        add(IdCardTemplate("LNG_02", "प्रसाद वितरण सेवादार", "Prasad Distribution Seva", "लंगर व रसोई",
            Color(0xFF2E7D32), Color(0xFFFFE082), Color(0xFFFF8F00), Color(0xFFFFFDE7), Color(0xFF0B3A0F), Color(0xFF2E7D32), "SINGLE_GOLD", "🍲"))
        add(IdCardTemplate("LNG_03", "पाकशाला मुख्य प्रबंधक", "Kitchen Master Manager", "लंगर व रसोई",
            Color(0xFF33691E), Color(0xFFDCEDC8), Color(0xFF8BC34A), Color(0xFFF1F8E9), Color(0xFF122C05), Color(0xFF33691E), "ROYAL_FRAME", "🍲"))
        add(IdCardTemplate("LNG_04", "जल सेवा व प्याऊ प्रमुख", "Water & Jal Seva Incharge", "लंगर व रसोई",
            Color(0xFF0277BD), Color(0xFFB3E5FC), Color(0xFF03A9F4), Color(0xFFE1F5FE), Color(0xFF00304D), Color(0xFF0277BD), "DOUBLE_GOLD", "💧"))
        add(IdCardTemplate("LNG_05", "स्वच्छता व पर्यावरण सेवा", "Sanitation & Swachh Seva", "लंगर व रसोई",
            Color(0xFF004D40), Color(0xFFA7FFEB), Color(0xFF00BFA5), Color(0xFFE0F2F1), Color(0xFF002B24), Color(0xFF004D40), "SINGLE_GOLD", "🌱"))
        add(IdCardTemplate("LNG_06", "भंडारा महाप्रसाद सेवक", "Bhandara Mahaprasad", "लंगर व रसोई",
            Color(0xFF558B2F), Color(0xFFFFF59D), Color(0xFFFBC02D), Color(0xFFFFFFFF), Color(0xFF25420E), Color(0xFF558B2F), "DOUBLE_GOLD", "🍲"))

        // ====================================================================
        // 5. डिजिटल सत्यापन व स्मार्ट क्यूआर कोड (DIGITAL QR ELITE) - 8 Templates
        // ====================================================================
        add(IdCardTemplate("DGT_01", "स्मार्ट क्यूआर कार्ड गोल्ड", "Smart QR Badge Gold", "डिजिटल सत्यापन",
            Color(0xFF1A1A2E), Color(0xFFE94560), Color(0xFFFFD700), Color(0xFFF8F9FA), Color(0xFF0F3460), Color(0xFF1A1A2E), "DOUBLE_GOLD", "📱"))
        add(IdCardTemplate("DGT_02", "साइबर सेक्यूरिटी आईडी", "Cyber Security ID Badge", "डिजिटल सत्यापन",
            Color(0xFF0F2027), Color(0xFF2C5364), Color(0xFF00E676), Color(0xFFFFFFFF), Color(0xFF0A1418), Color(0xFF0F2027), "ROYAL_FRAME", "🔒"))
        add(IdCardTemplate("DGT_03", "डिजिटल टोकन स्कैनर", "Digital Token Scanner", "डिजिटल सत्यापन",
            Color(0xFF11998E), Color(0xFF38EF7D), Color(0xFFFFD700), Color(0xFFF0FFF4), Color(0xFF084540), Color(0xFF11998E), "SINGLE_GOLD", "📲"))
        add(IdCardTemplate("DGT_04", "क्लाउड डेटा एडमिन", "Cloud Data Admin Badge", "डिजिटल सत्यापन",
            Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B), Color(0xFFFAF5FF), Color(0xFF1D0B3D), Color(0xFF3A1C71), "DOUBLE_GOLD", "☁️"))
        add(IdCardTemplate("DGT_05", "इलेक्ट्रॉनिक पास वीआईपी", "Electronic VIP Pass", "डिजिटल सत्यापन",
            Color(0xFF232526), Color(0xFF414345), Color(0xFFFFD700), Color(0xFFFFFFFF), Color(0xFF0D0E0E), Color(0xFF232526), "ROYAL_FRAME", "⭐"))
        add(IdCardTemplate("DGT_06", "होलोग्राफिक स्मार्ट बैज", "Holographic Smart Badge", "डिजिटल सत्यापन",
            Color(0xFF4A148C), Color(0xFFEA80FC), Color(0xFF00E5FF), Color(0xFFF3E5F5), Color(0xFF230545), Color(0xFF4A148C), "DOUBLE_GOLD", "💠"))
        add(IdCardTemplate("DGT_07", "बायोमेट्रिक सेवादार कार्ड", "Biometric Sevadar Card", "डिजिटल सत्यापन",
            Color(0xFF00695C), Color(0xFF64FFDA), Color(0xFFFFC107), Color(0xFFE0F2F1), Color(0xFF00332D), Color(0xFF00695C), "SINGLE_GOLD", "👆"))
        add(IdCardTemplate("DGT_08", "एनएफसी स्मार्ट पास", "NFC Smart Pass Gold", "डिजिटल सत्यापन",
            Color(0xFF283593), Color(0xFF5C6BC0), Color(0xFFFFD700), Color(0xFFE8EAF6), Color(0xFF101642), Color(0xFF283593), "DOUBLE_GOLD", "📶"))

        // ====================================================================
        // 6. आधुनिक कॉर्पोरेट व स्वच्छ बैज (MODERN MINIMALIST) - 8 Templates
        // ====================================================================
        add(IdCardTemplate("MDN_01", "प्रीमियम व्हाइट एंड गोल्ड", "Premium White & Gold", "आधुनिक स्वच्छ",
            Color(0xFF212121), Color(0xFFFFD700), Color(0xFFB8860B), Color(0xFFFFFFFF), Color(0xFF1A1A1A), Color(0xFF212121), "DOUBLE_GOLD", "⭐"))
        add(IdCardTemplate("MDN_02", "एमरल्ड ग्रीन लक्ज़री", "Emerald Green Luxury", "आधुनिक स्वच्छ",
            Color(0xFF004D40), Color(0xFFA7FFEB), Color(0xFFFFD700), Color(0xFFFFFFFF), Color(0xFF00241E), Color(0xFF004D40), "ROYAL_FRAME", "🪷"))
        add(IdCardTemplate("MDN_03", "नेवी कॉर्पोरेट एलिगेंट", "Navy Corporate Elegant", "आधुनिक स्वच्छ",
            Color(0xFF1A237E), Color(0xFF9FA8DA), Color(0xFFFFB300), Color(0xFFF5F6FA), Color(0xFF0B1040), Color(0xFF1A237E), "SINGLE_GOLD", "👑"))
        add(IdCardTemplate("MDN_04", "रूबी रेड एलिगेंट", "Ruby Red Elegant", "आधुनिक स्वच्छ",
            Color(0xFF880E4F), Color(0xFFF48FB1), Color(0xFFFFD54F), Color(0xFFFFF0F5), Color(0xFF3E0323), Color(0xFF880E4F), "DOUBLE_GOLD", "🪷"))
        add(IdCardTemplate("MDN_05", "सिल्वर प्लैटिनम प्रेस्टीज", "Silver Platinum Prestige", "आधुनिक स्वच्छ",
            Color(0xFF37474F), Color(0xFFCFD8DC), Color(0xFF90A4AE), Color(0xFFFFFFFF), Color(0xFF1C2529), Color(0xFF37474F), "ROYAL_FRAME", "⭐"))
        add(IdCardTemplate("MDN_06", "अंबर सूर्यास्त मॉडर्न", "Sunset Amber Modern", "आधुनिक स्वच्छ",
            Color(0xFFFF6F00), Color(0xFFFFE082), Color(0xFFE65100), Color(0xFFFFFBF0), Color(0xFF4E2000), Color(0xFFFF6F00), "SINGLE_GOLD", "☀️"))
        add(IdCardTemplate("MDN_07", "टील लक्स मिनिमल", "Teal Luxe Minimal", "आधुनिक स्वच्छ",
            Color(0xFF00695C), Color(0xFF80CBC4), Color(0xFFFFD700), Color(0xFFF0FDF4), Color(0xFF00332D), Color(0xFF00695C), "DOUBLE_GOLD", "💠"))
        add(IdCardTemplate("MDN_08", "पवित्र चारधाम क्लासिक", "Sacred Chardham Classic", "आधुनिक स्वच्छ",
            Color(0xFF4E342E), Color(0xFFD7CCC8), Color(0xFFFFB74D), Color(0xFFEFEBE9), Color(0xFF271916), Color(0xFF4E342E), "ROYAL_FRAME", "🕉️"))
    }

    fun getByCategory(category: String): List<IdCardTemplate> {
        return TEMPLATES.filter { it.category == category }
    }

    fun getAllCategories(): List<String> {
        return TEMPLATES.map { it.category }.distinct()
    }
}
