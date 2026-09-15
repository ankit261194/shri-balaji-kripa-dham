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

    val TEMPLATES: List<IdCardTemplate> = listOf(
        // 1. राजसी दरबार स्वर्ण (Royal Darbar Gold)
        IdCardTemplate(
            id = "RG_01",
            titleHindi = "राजसी दरबार स्वर्ण",
            titleEnglish = "Royal Darbar Gold",
            category = "राजसी स्वर्णिम",
            primaryColor = Color(0xFF800000),
            secondaryColor = Color(0xFFFFD700),
            accentColor = Color(0xFFB8860B),
            backgroundColor = Color(0xFFFFF8E7),
            textColor = Color(0xFF330000),
            ribbonColor = Color(0xFF800000),
            borderStyle = "DOUBLE_GOLD",
            emblem = "👑",
            isVertical = true
        ),

        // 2. मुख्य सेवादार परिचय पत्र (Chief Sevadar Horizontal Badge)
        IdCardTemplate(
            id = "SEV_01",
            titleHindi = "मुख्य सेवादार परिचय पत्र (हॉरिजॉन्टल)",
            titleEnglish = "Chief Sevadar Horizontal Badge",
            category = "सेवादार बैज",
            primaryColor = Color(0xFF1B365D),
            secondaryColor = Color(0xFF4A90E2),
            accentColor = Color(0xFFFFB300),
            backgroundColor = Color(0xFFF4F7FC),
            textColor = Color(0xFF0D1B2A),
            ribbonColor = Color(0xFF1B365D),
            borderStyle = "MODERN_LINE",
            emblem = "🚩",
            isVertical = false
        ),

        // 3. सुरक्षा व अनुशासन पास (Security & Gate Pass)
        IdCardTemplate(
            id = "SEC_01",
            titleHindi = "सुरक्षा व अनुशासन पास",
            titleEnglish = "Security & Gate Pass",
            category = "सुरक्षा व व्यवस्था",
            primaryColor = Color(0xFF212121),
            secondaryColor = Color(0xFFFFC107),
            accentColor = Color(0xFFD32F2F),
            backgroundColor = Color(0xFFFAFAFA),
            textColor = Color(0xFF111111),
            ribbonColor = Color(0xFFD32F2F),
            borderStyle = "MINIMAL",
            emblem = "🛡️",
            isVertical = true
        ),

        // 4. ट्रस्टी व विशिष्ट VIP पत्र (Trustee & Executive VIP)
        IdCardTemplate(
            id = "TRU_01",
            titleHindi = "ट्रस्टी व विशिष्ट VIP परिषद",
            titleEnglish = "Trustee & VIP Executive",
            category = "ट्रस्टी व VIP",
            primaryColor = Color(0xFF1C1C1C),
            secondaryColor = Color(0xFFFFD700),
            accentColor = Color(0xFFC5A059),
            backgroundColor = Color(0xFFFFFDF8),
            textColor = Color(0xFF1A1A1A),
            ribbonColor = Color(0xFF800000),
            borderStyle = "ROYAL_FRAME",
            emblem = "👑",
            isVertical = true
        ),

        // 5. मातृशक्ति सेविका पत्र (Mahila Sevika Sacred)
        IdCardTemplate(
            id = "MAH_01",
            titleHindi = "मातृशक्ति सेविका पत्र",
            titleEnglish = "Mahila Sevika Sacred",
            category = "मातृशक्ति सेवा",
            primaryColor = Color(0xFFE65100),
            secondaryColor = Color(0xFFFFCC80),
            accentColor = Color(0xFFC2185B),
            backgroundColor = Color(0xFFFFF9F5),
            textColor = Color(0xFF4A1A00),
            ribbonColor = Color(0xFFE65100),
            borderStyle = "DOUBLE_GOLD",
            emblem = "🪷",
            isVertical = true
        ),

        // 6. यात्रा व बस सेवादार (Yatra Transport Seva)
        IdCardTemplate(
            id = "YAT_01",
            titleHindi = "यात्रा व बस सेवादार",
            titleEnglish = "Yatra Transport Volunteer",
            category = "यात्रा प्रबंधन",
            primaryColor = Color(0xFF1B5E20),
            secondaryColor = Color(0xFF81C784),
            accentColor = Color(0xFFFFB300),
            backgroundColor = Color(0xFFF1F8E9),
            textColor = Color(0xFF0E3813),
            ribbonColor = Color(0xFF1B5E20),
            borderStyle = "SINGLE_GOLD",
            emblem = "⭐",
            isVertical = true
        ),

        // 7. अन्नक्षेत्र भण्डारा सेवा (Bhandara Prasad Seva)
        IdCardTemplate(
            id = "BHA_01",
            titleHindi = "अन्नक्षेत्र भण्डारा सेवा",
            titleEnglish = "Bhandara Prasad Seva",
            category = "प्रसाद व भण्डारा",
            primaryColor = Color(0xFFD84315),
            secondaryColor = Color(0xFFFFB74D),
            accentColor = Color(0xFFF57C00),
            backgroundColor = Color(0xFFFFF8E1),
            textColor = Color(0xFF3E1100),
            ribbonColor = Color(0xFFD84315),
            borderStyle = "DOUBLE_GOLD",
            emblem = "🪔",
            isVertical = true
        ),

        // 8. प्राथमिक चिकित्सा सेवा (Medical Camp Seva)
        IdCardTemplate(
            id = "MED_01",
            titleHindi = "प्राथमिक चिकित्सा सेवा (हॉरिजॉन्टल)",
            titleEnglish = "Medical Health Camp (Horizontal)",
            category = "चिकित्सा सेवा",
            primaryColor = Color(0xFFC62828),
            secondaryColor = Color(0xFFEF9A9A),
            accentColor = Color(0xFF1565C0),
            backgroundColor = Color(0xFFFFFFFF),
            textColor = Color(0xFF212121),
            ribbonColor = Color(0xFFC62828),
            borderStyle = "MODERN_LINE",
            emblem = "⭐",
            isVertical = false
        ),

        // 9. मुख्य पुजारी वैदिक पत्र (Ashram Chief Pujari Vedic)
        IdCardTemplate(
            id = "PUJ_01",
            titleHindi = "मुख्य पुजारी वैदिक पत्र",
            titleEnglish = "Ashram Chief Pujari Vedic",
            category = "वैदिक पुरोहित",
            primaryColor = Color(0xFFBF360C),
            secondaryColor = Color(0xFFFFD54F),
            accentColor = Color(0xFFE65100),
            backgroundColor = Color(0xFFFFFDE7),
            textColor = Color(0xFF3B0B00),
            ribbonColor = Color(0xFFBF360C),
            borderStyle = "ROYAL_FRAME",
            emblem = "🕉️",
            isVertical = true
        ),

        // 10. आश्रम प्रेस व मीडिया पास (Media & Press Pass)
        IdCardTemplate(
            id = "MED_02",
            titleHindi = "आश्रम प्रेस व मीडिया पास (हॉरिजॉन्टल)",
            titleEnglish = "Media & Press Pass (Horizontal)",
            category = "प्रेस व मीडिया",
            primaryColor = Color(0xFF0D47A1),
            secondaryColor = Color(0xFF64B5F6),
            accentColor = Color(0xFFFF5722),
            backgroundColor = Color(0xFFF5F9FF),
            textColor = Color(0xFF002171),
            ribbonColor = Color(0xFF0D47A1),
            borderStyle = "MODERN_LINE",
            emblem = "🎙️",
            isVertical = false
        ),

        // 11. वाहन व पार्किंग व्यवस्था (Parking Traffic Control)
        IdCardTemplate(
            id = "PAR_01",
            titleHindi = "वाहन व पार्किंग व्यवस्था",
            titleEnglish = "Parking & Traffic Control",
            category = "वाहन प्रबंधन",
            primaryColor = Color(0xFF263238),
            secondaryColor = Color(0xFFFF9800),
            accentColor = Color(0xFF00ACC1),
            backgroundColor = Color(0xFFECEFF1),
            textColor = Color(0xFF102027),
            ribbonColor = Color(0xFFFF9800),
            borderStyle = "MINIMAL",
            emblem = "🛡️",
            isVertical = true
        ),

        // 12. युवा सेवा दल (Youth Volunteer Wing)
        IdCardTemplate(
            id = "YOU_01",
            titleHindi = "युवा सेवा दल ऊर्जा",
            titleEnglish = "Youth Volunteer Dynamic",
            category = "युवा सेवा दल",
            primaryColor = Color(0xFF00695C),
            secondaryColor = Color(0xFF4DB6AC),
            accentColor = Color(0xFFFFC107),
            backgroundColor = Color(0xFFE0F2F1),
            textColor = Color(0xFF00363A),
            ribbonColor = Color(0xFF00695C),
            borderStyle = "MODERN_LINE",
            emblem = "⚡",
            isVertical = true
        ),

        // 13. स्वच्छता व जल सेवा (Cleanliness & Water Seva)
        IdCardTemplate(
            id = "SWA_01",
            titleHindi = "स्वच्छता व जल सेवा",
            titleEnglish = "Cleanliness & Water Seva",
            category = "स्वच्छता सेवा",
            primaryColor = Color(0xFF00838F),
            secondaryColor = Color(0xFF80DEEA),
            accentColor = Color(0xFF4CAF50),
            backgroundColor = Color(0xFFE0F7FA),
            textColor = Color(0xFF004D40),
            ribbonColor = Color(0xFF00838F),
            borderStyle = "SINGLE_GOLD",
            emblem = "🪷",
            isVertical = true
        ),

        // 14. आश्रम कार्यकारिणी परिषद (Governing Council)
        IdCardTemplate(
            id = "KAR_01",
            titleHindi = "आश्रम कार्यकारिणी परिषद",
            titleEnglish = "Ashram Governing Council",
            category = "कार्यकारिणी",
            primaryColor = Color(0xFF4A148C),
            secondaryColor = Color(0xFFFFD54F),
            accentColor = Color(0xFFAB47BC),
            backgroundColor = Color(0xFFF3E5F5),
            textColor = Color(0xFF311B92),
            ribbonColor = Color(0xFF4A148C),
            borderStyle = "ROYAL_FRAME",
            emblem = "📜",
            isVertical = true
        )
    )

    fun findTemplate(id: String): IdCardTemplate {
        return TEMPLATES.find { it.id == id } ?: TEMPLATES[0]
    }

    fun getByCategory(category: String): List<IdCardTemplate> {
        return TEMPLATES.filter { it.category == category }
    }

    fun getAllCategories(): List<String> {
        return TEMPLATES.map { it.category }.distinct()
    }
}
