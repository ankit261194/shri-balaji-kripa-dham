package com.example.shribalajikripadham.theme

import androidx.compose.ui.graphics.Color

enum class SacredTheme(
    val id: String,
    val nameHindi: String,
    val nameEnglish: String,
    val icon: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val topBarColor: Color,
    val headerGradientStart: Color,
    val headerGradientEnd: Color,
    val accentGold: Color,
    val cardBorderColor: Color,
    val isDark: Boolean = false
) {
    // 1. Royal Mandir Maroon
    ROYAL_MAROON(
        id = "maroon",
        nameHindi = "शाही महरून",
        nameEnglish = "Royal Maroon",
        icon = "👑",
        primaryColor = Color(0xFF5C001E),
        secondaryColor = Color(0xFFFFB300),
        topBarColor = Color(0xFF5C001E),
        headerGradientStart = Color(0xFF5C001E),
        headerGradientEnd = Color(0xFF3B0010),
        accentGold = Color(0xFFFFD54F),
        cardBorderColor = Color(0xFFFFB300)
    ),

    // 2. Sacred Mandir Saffron / Bhagwa
    SACRED_SAFFRON(
        id = "saffron",
        nameHindi = "दिव्य केसरिया",
        nameEnglish = "Sacred Saffron",
        icon = "🚩",
        primaryColor = Color(0xFFE65100),
        secondaryColor = Color(0xFFFFB300),
        topBarColor = Color(0xFFBF360C),
        headerGradientStart = Color(0xFFE65100),
        headerGradientEnd = Color(0xFFBF360C),
        accentGold = Color(0xFFFFE082),
        cardBorderColor = Color(0xFFFFB300)
    ),

    // 3. Pitambar Gold
    PITAMBAR_GOLD(
        id = "gold",
        nameHindi = "पीताम्बर स्वर्ण",
        nameEnglish = "Pitambar Gold",
        icon = "🪔",
        primaryColor = Color(0xFFC68400),
        secondaryColor = Color(0xFFE65100),
        topBarColor = Color(0xFFA06700),
        headerGradientStart = Color(0xFFC68400),
        headerGradientEnd = Color(0xFF7A4E00),
        accentGold = Color(0xFFFFF176),
        cardBorderColor = Color(0xFFFFD54F)
    ),

    // 4. Shyamal / Divine Peacock Blue
    DIVINE_BLUE(
        id = "blue",
        nameHindi = "श्याम वर्ण (मयूर नीला)",
        nameEnglish = "Divine Blue",
        icon = "🦚",
        primaryColor = Color(0xFF0D47A1),
        secondaryColor = Color(0xFFFFB300),
        topBarColor = Color(0xFF072C66),
        headerGradientStart = Color(0xFF0D47A1),
        headerGradientEnd = Color(0xFF051C42),
        accentGold = Color(0xFFFFD54F),
        cardBorderColor = Color(0xFFFFC107)
    ),

    // 5. Vedic Tulsi Forest Green
    TULSI_GREEN(
        id = "green",
        nameHindi = "तुलसी हरित",
        nameEnglish = "Tulsi Green",
        icon = "🌿",
        primaryColor = Color(0xFF1B5E20),
        secondaryColor = Color(0xFFFFB300),
        topBarColor = Color(0xFF103D14),
        headerGradientStart = Color(0xFF1B5E20),
        headerGradientEnd = Color(0xFF0B290E),
        accentGold = Color(0xFFFFD54F),
        cardBorderColor = Color(0xFFFFB300)
    ),

    // 6. Devotional Midnight Dark
    MIDNIGHT_DARK(
        id = "dark",
        nameHindi = "दिव्य रात्रि (Dark)",
        nameEnglish = "Midnight Dark",
        icon = "🌌",
        primaryColor = Color(0xFFFFB300),
        secondaryColor = Color(0xFFFF8F00),
        topBarColor = Color(0xFF1E1E1E),
        headerGradientStart = Color(0xFF262626),
        headerGradientEnd = Color(0xFF121212),
        accentGold = Color(0xFFFFE082),
        cardBorderColor = Color(0xFFFFB300),
        isDark = true
    );

    companion object {
        fun fromId(id: String): SacredTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ROYAL_MAROON
        }
    }
}
