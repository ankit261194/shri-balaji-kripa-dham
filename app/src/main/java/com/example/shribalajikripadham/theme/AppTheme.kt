package com.example.shribalajikripadham.theme

import androidx.compose.foundation.shape.CornerBasedShape
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
    val cardShape: CornerBasedShape = RoundedCornerShape(18.dp),
    val buttonShape: CornerBasedShape = RoundedCornerShape(12.dp),
    val cardBorderWidth: Dp = 1.5.dp,
    val cardElevation: Dp = 4.dp,
    val styleNameHindi: String = "शाही राजसी धरोहर",
    val styleBadge: String = "👑 सेरिफ • राजसी 18dp"
) {
    // 1. Royal Mandir Maroon - Vedic Heritage & Temple Arch
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
        cardShape = RoundedCornerShape(18.dp),
        buttonShape = RoundedCornerShape(12.dp),
        cardBorderWidth = 1.5.dp,
        cardElevation = 4.dp,
        styleNameHindi = "शाही राजसी धरोहर",
        styleBadge = "👑 सेरिफ • राजसी 18dp"
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
        cardShape = RoundedCornerShape(22.dp),
        buttonShape = RoundedCornerShape(16.dp),
        cardBorderWidth = 1.5.dp,
        cardElevation = 4.dp,
        styleNameHindi = "मंदिर गोपुरम मेहराब",
        styleBadge = "🚩 सेरिफ • गोपुरम 22dp"
    ),

    // 3. Pitambar Gold - Ornate Regal Palace
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
        cardShape = RoundedCornerShape(14.dp),
        buttonShape = RoundedCornerShape(10.dp),
        cardBorderWidth = 2.dp,
        cardElevation = 4.dp,
        styleNameHindi = "स्वर्ण रत्न कट",
        styleBadge = "🪔 सेरिफ • क्लासिक 14dp"
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
        buttonShape = RoundedCornerShape(24.dp),
        cardBorderWidth = 1.dp,
        cardElevation = 3.dp,
        styleNameHindi = "आधुनिक मयूर स्क्वर्कर",
        styleBadge = "🦚 सांस-सेरिफ • 26dp"
    ),

    // 5. Vedic Tulsi Forest Green - Organic Smooth Leaf Curve
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
        cardShape = RoundedCornerShape(20.dp),
        buttonShape = RoundedCornerShape(14.dp),
        cardBorderWidth = 1.dp,
        cardElevation = 3.dp,
        styleNameHindi = "सात्विक तुलसी पर्ण",
        styleBadge = "🌿 सात्विक • स्मूथ 20dp"
    ),

    // 6. Devotional Midnight Dark - Cyber Neon Gold & Modern Glass
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
        cardShape = RoundedCornerShape(16.dp),
        buttonShape = RoundedCornerShape(14.dp),
        cardBorderWidth = 1.5.dp,
        cardElevation = 4.dp,
        styleNameHindi = "नियॉन गोल्ड ग्लास",
        styleBadge = "🌌 नियॉन • डार्क 16dp"
    ),

    // 7. Surya Tej Crimson / Sindoor
    SURYA_CRIMSON(
        id = "crimson",
        nameHindi = "सूर्य तेज सिंदूरी",
        nameEnglish = "Surya Crimson",
        icon = "☀️",
        primaryColor = Color(0xFFC2185B),
        secondaryColor = Color(0xFFFFD54F),
        topBarColor = Color(0xFF880E4F),
        headerGradientStart = Color(0xFFC2185B),
        headerGradientEnd = Color(0xFF880E4F),
        accentGold = Color(0xFFFFE082),
        cardBorderColor = Color(0xFFFF80AB),
        isDark = false,
        fontFamily = FontFamily.Serif,
        cardShape = RoundedCornerShape(20.dp),
        buttonShape = RoundedCornerShape(14.dp),
        cardBorderWidth = 1.5.dp,
        cardElevation = 4.dp,
        styleNameHindi = "सिंदूरी सूर्य तेज",
        styleBadge = "☀️ सिंदूरी • 20dp"
    ),

    // 8. Ayodhya Sandalwood
    AYODHYA_SANDALWOOD(
        id = "sandalwood",
        nameHindi = "अयोध्या चंदन",
        nameEnglish = "Ayodhya Sandalwood",
        icon = "🪵",
        primaryColor = Color(0xFF6D4C41),
        secondaryColor = Color(0xFFFFD54F),
        topBarColor = Color(0xFF4E342E),
        headerGradientStart = Color(0xFF6D4C41),
        headerGradientEnd = Color(0xFF3E2723),
        accentGold = Color(0xFFFFECB3),
        cardBorderColor = Color(0xFFFFD54F),
        isDark = false,
        fontFamily = FontFamily.Serif,
        cardShape = RoundedCornerShape(16.dp),
        buttonShape = RoundedCornerShape(12.dp),
        cardBorderWidth = 1.5.dp,
        cardElevation = 4.dp,
        styleNameHindi = "पावन चंदन काष्ठ",
        styleBadge = "🪵 चंदन • 16dp"
    ),

    // 9. Ganga Aarti Amber Flame
    GANGA_AARTI_AMBER(
        id = "amber",
        nameHindi = "गंगा आरती दीप",
        nameEnglish = "Ganga Aarti Amber",
        icon = "🔥",
        primaryColor = Color(0xFFE64A19),
        secondaryColor = Color(0xFFFFC107),
        topBarColor = Color(0xFFBF360C),
        headerGradientStart = Color(0xFFE64A19),
        headerGradientEnd = Color(0xFFD84315),
        accentGold = Color(0xFFFFD54F),
        cardBorderColor = Color(0xFFFFB300),
        isDark = false,
        fontFamily = FontFamily.SansSerif,
        cardShape = RoundedCornerShape(22.dp),
        buttonShape = RoundedCornerShape(16.dp),
        cardBorderWidth = 1.5.dp,
        cardElevation = 4.dp,
        styleNameHindi = "महा आरती ज्योति",
        styleBadge = "🔥 महा आरती • 22dp"
    ),

    // 10. Vrindavan Violet
    VRINDAVAN_VIOLET(
        id = "purple",
        nameHindi = "राधा माधव जामुनी",
        nameEnglish = "Vrindavan Violet",
        icon = "🪷",
        primaryColor = Color(0xFF512DA8),
        secondaryColor = Color(0xFFFFD54F),
        topBarColor = Color(0xFF311B92),
        headerGradientStart = Color(0xFF512DA8),
        headerGradientEnd = Color(0xFF311B92),
        accentGold = Color(0xFFFFD54F),
        cardBorderColor = Color(0xFFB39DDB),
        isDark = false,
        fontFamily = FontFamily.Serif,
        cardShape = RoundedCornerShape(18.dp),
        buttonShape = RoundedCornerShape(12.dp),
        cardBorderWidth = 1.5.dp,
        cardElevation = 4.dp,
        styleNameHindi = "ब्रज कमल जामुनी",
        styleBadge = "🪷 ब्रज कमल • 18dp"
    ),

    // 11. Kailash Teal
    KAILASH_TEAL(
        id = "teal",
        nameHindi = "कैलाश मानसरोवर",
        nameEnglish = "Kailash Teal",
        icon = "🌊",
        primaryColor = Color(0xFF00695C),
        secondaryColor = Color(0xFFFFD54F),
        topBarColor = Color(0xFF004D40),
        headerGradientStart = Color(0xFF00695C),
        headerGradientEnd = Color(0xFF004D40),
        accentGold = Color(0xFF80CBC4),
        cardBorderColor = Color(0xFF4DB6AC),
        isDark = false,
        fontFamily = FontFamily.SansSerif,
        cardShape = RoundedCornerShape(24.dp),
        buttonShape = RoundedCornerShape(20.dp),
        cardBorderWidth = 1.dp,
        cardElevation = 3.dp,
        styleNameHindi = "पवित्र मानसरोवर",
        styleBadge = "🌊 मानसरोवर • 24dp"
    ),

    // 12. Mahadev Rudraksha Copper
    MAHADEV_COPPER(
        id = "copper",
        nameHindi = "रुद्राक्ष ताम्र",
        nameEnglish = "Mahadev Copper",
        icon = "🔱",
        primaryColor = Color(0xFF4E342E),
        secondaryColor = Color(0xFFFFB300),
        topBarColor = Color(0xFF2E1C18),
        headerGradientStart = Color(0xFF4E342E),
        headerGradientEnd = Color(0xFF2E1C18),
        accentGold = Color(0xFFFFD54F),
        cardBorderColor = Color(0xFFFFB300),
        isDark = false,
        fontFamily = FontFamily.Serif,
        cardShape = RoundedCornerShape(14.dp),
        buttonShape = RoundedCornerShape(10.dp),
        cardBorderWidth = 1.5.dp,
        cardElevation = 4.dp,
        styleNameHindi = "महाकाल रुद्राक्ष",
        styleBadge = "🔱 रुद्राक्ष • 14dp"
    );

    companion object {
        fun fromId(id: String): SacredTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ROYAL_MAROON
        }
    }
}

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
