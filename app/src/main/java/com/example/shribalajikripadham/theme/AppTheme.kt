package com.example.shribalajikripadham.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    val isDark: Boolean = false,
    val fontFamily: FontFamily = FontFamily.Serif,
    val cardShape: CornerBasedShape = RoundedCornerShape(16.dp),
    val buttonShape: CornerBasedShape = RoundedCornerShape(12.dp),
    val cardBorderWidth: Dp = 1.5.dp,
    val cardElevation: Dp = 4.dp,
    val styleNameHindi: String = "शाही राजसी धरोहर",
    val styleBadge: String = "👑 सेरिफ • मेहराब"
) {
    // 1. Royal Mandir Maroon - Vedic Heritage & Temple Jharokha
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
        cardBorderColor = Color(0xFFFFB300),
        isDark = false,
        fontFamily = FontFamily.Serif,
        cardShape = RoundedCornerShape(topStart = 22.dp, topEnd = 6.dp, bottomEnd = 22.dp, bottomStart = 6.dp),
        buttonShape = RoundedCornerShape(12.dp),
        cardBorderWidth = 1.5.dp,
        cardElevation = 4.dp,
        styleNameHindi = "शाही राजसी धरोहर",
        styleBadge = "👑 सेरिफ • मेहराब"
    ),

    // 2. Sacred Mandir Saffron / Bhagwa - Devotional Temple Gopuram
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
        cardBorderColor = Color(0xFFFFB300),
        isDark = false,
        fontFamily = FontFamily.Serif,
        cardShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 10.dp, bottomEnd = 10.dp),
        buttonShape = RoundedCornerShape(16.dp),
        cardBorderWidth = 1.5.dp,
        cardElevation = 5.dp,
        styleNameHindi = "मंदिर गोपुरम मेहराब",
        styleBadge = "🚩 सेरिफ • गोपुरम"
    ),

    // 3. Pitambar Gold - Ornate Diamond Cut Regal Palace
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
        cardBorderColor = Color(0xFFFFD54F),
        isDark = false,
        fontFamily = FontFamily.Serif,
        cardShape = CutCornerShape(12.dp),
        buttonShape = CutCornerShape(8.dp),
        cardBorderWidth = 2.dp,
        cardElevation = 6.dp,
        styleNameHindi = "स्वर्ण रत्न कट",
        styleBadge = "🪔 सेरिफ • डायमंड कट"
    ),

    // 4. Shyamal / Divine Peacock Blue - Ultra Modern Squircle
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
        cardBorderColor = Color(0xFFFFC107),
        isDark = false,
        fontFamily = FontFamily.SansSerif,
        cardShape = RoundedCornerShape(26.dp),
        buttonShape = RoundedCornerShape(50),
        cardBorderWidth = 1.dp,
        cardElevation = 3.dp,
        styleNameHindi = "आधुनिक मयूर स्क्वर्कर",
        styleBadge = "🦚 सांस-सेरिफ • स्क्वर्कर"
    ),

    // 5. Vedic Tulsi Forest Green - Organic Tulsi Leaf Curve
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
        cardBorderColor = Color(0xFFFFB300),
        isDark = false,
        fontFamily = FontFamily.Default,
        cardShape = RoundedCornerShape(topStart = 26.dp, bottomEnd = 26.dp, topEnd = 8.dp, bottomStart = 8.dp),
        buttonShape = RoundedCornerShape(14.dp),
        cardBorderWidth = 1.dp,
        cardElevation = 3.dp,
        styleNameHindi = "सात्विक तुलसी पर्ण",
        styleBadge = "🌿 सात्विक • तुलसी पर्ण"
    ),

    // 6. Devotional Midnight Dark - Cyber Neon Gold & Floating Glass
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
        isDark = true,
        fontFamily = FontFamily.SansSerif,
        cardShape = RoundedCornerShape(20.dp),
        buttonShape = RoundedCornerShape(16.dp),
        cardBorderWidth = 1.5.dp,
        cardElevation = 8.dp,
        styleNameHindi = "नियॉन गोल्ड ग्लास",
        styleBadge = "🌌 नियॉन • ग्लास लुक"
    );

    companion object {
        fun fromId(id: String): SacredTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ROYAL_MAROON
        }
    }
}

/**
 * Direct access to the active style parameters anywhere in the Compose tree
 */
data class SacredStyle(
    val theme: SacredTheme,
    val fontFamily: FontFamily,
    val cardShape: CornerBasedShape,
    val buttonShape: CornerBasedShape,
    val cardBorderWidth: Dp,
    val cardElevation: Dp,
    val cardBorderColor: Color,
    val isDark: Boolean
)

val LocalSacredStyle = staticCompositionLocalOf {
    SacredStyle(
        theme = SacredTheme.ROYAL_MAROON,
        fontFamily = FontFamily.Serif,
        cardShape = SacredTheme.ROYAL_MAROON.cardShape,
        buttonShape = SacredTheme.ROYAL_MAROON.buttonShape,
        cardBorderWidth = SacredTheme.ROYAL_MAROON.cardBorderWidth,
        cardElevation = SacredTheme.ROYAL_MAROON.cardElevation,
        cardBorderColor = SacredTheme.ROYAL_MAROON.cardBorderColor,
        isDark = false
    )
}
