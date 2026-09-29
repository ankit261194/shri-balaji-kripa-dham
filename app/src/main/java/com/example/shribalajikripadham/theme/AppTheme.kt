package com.example.shribalajikripadham.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern, Peaceful, Clean Themes for Shri Balaji Kripa Dham.
 * Inspired by WhatsApp, Telegram, Apple Minimal, and calm, soothing natural palettes.
 */
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
    val backgroundLight: Color = Color(0xFFF0F2F5),
    val surfaceLight: Color = Color(0xFFFFFFFF),
    val isDark: Boolean = false,
    val fontFamily: FontFamily = FontFamily.Default,
    val cardShape: CornerBasedShape = RoundedCornerShape(12.dp),
    val buttonShape: CornerBasedShape = RoundedCornerShape(10.dp),
    val cardBorderWidth: Dp = 0.8.dp,
    val cardElevation: Dp = 1.dp,
    val styleNameHindi: String = "वाट्सएप क्लीन",
    val styleBadge: String = "💬 वाट्सएप • 12dp"
) {
    // 1. WhatsApp Emerald - Soothing, iconic clean green (Default)
    WHATSAPP_EMERALD(
        id = "whatsapp",
        nameHindi = "वाट्सएप हरा",
        nameEnglish = "WhatsApp Emerald",
        icon = "💬",
        primaryColor = Color(0xFF008069),
        secondaryColor = Color(0xFF00A884),
        topBarColor = Color(0xFF008069),
        headerGradientStart = Color(0xFF008069),
        headerGradientEnd = Color(0xFF075E54),
        accentGold = Color(0xFF25D366),
        cardBorderColor = Color(0xFFE9EDEF),
        backgroundLight = Color(0xFFF0F2F5),
        surfaceLight = Color(0xFFFFFFFF),
        isDark = false,
        fontFamily = FontFamily.Default,
        cardShape = RoundedCornerShape(12.dp),
        buttonShape = RoundedCornerShape(10.dp),
        cardBorderWidth = 0.8.dp,
        cardElevation = 1.dp,
        styleNameHindi = "वाट्सएप क्लीन एमराल्ड",
        styleBadge = "💬 वाट्सएप • 12dp"
    ),

    // 2. Telegram Sky Blue - Modern, crisp, fresh
    TELEGRAM_BLUE(
        id = "telegram",
        nameHindi = "टेलीग्राम नीला",
        nameEnglish = "Telegram Sky Blue",
        icon = "✈️",
        primaryColor = Color(0xFF2481CC),
        secondaryColor = Color(0xFF54A9EB),
        topBarColor = Color(0xFF1E6EA8),
        headerGradientStart = Color(0xFF2481CC),
        headerGradientEnd = Color(0xFF176097),
        accentGold = Color(0xFF40A7E3),
        cardBorderColor = Color(0xFFE3E8EC),
        backgroundLight = Color(0xFFF4F6F8),
        surfaceLight = Color(0xFFFFFFFF),
        isDark = false,
        fontFamily = FontFamily.Default,
        cardShape = RoundedCornerShape(14.dp),
        buttonShape = RoundedCornerShape(12.dp),
        cardBorderWidth = 0.8.dp,
        cardElevation = 1.dp,
        styleNameHindi = "टेलीग्राम स्काई ब्लू",
        styleBadge = "✈️ टेलीग्राम • 14dp"
    ),

    // 3. Calm Sage Mint - Peaceful, Ayurvedic, serene forest
    CALM_SAGE_MINT(
        id = "sage",
        nameHindi = "शांत पुदीना (Sage Mint)",
        nameEnglish = "Peaceful Sage Mint",
        icon = "🍃",
        primaryColor = Color(0xFF2D6A4F),
        secondaryColor = Color(0xFF52B788),
        topBarColor = Color(0xFF1B4332),
        headerGradientStart = Color(0xFF2D6A4F),
        headerGradientEnd = Color(0xFF1B4332),
        accentGold = Color(0xFF74C69D),
        cardBorderColor = Color(0xFFD8E2DC),
        backgroundLight = Color(0xFFF3F7F5),
        surfaceLight = Color(0xFFFFFFFF),
        isDark = false,
        fontFamily = FontFamily.Default,
        cardShape = RoundedCornerShape(14.dp),
        buttonShape = RoundedCornerShape(10.dp),
        cardBorderWidth = 0.8.dp,
        cardElevation = 1.dp,
        styleNameHindi = "शांत सात्विक पुदीना",
        styleBadge = "🍃 सात्विक • 14dp"
    ),

    // 4. Ocean Indigo - Deep, calm, executive
    OCEAN_INDIGO(
        id = "ocean",
        nameHindi = "शांत महासागर (Navy Blue)",
        nameEnglish = "Calm Ocean Navy",
        icon = "🌊",
        primaryColor = Color(0xFF1E3A8A),
        secondaryColor = Color(0xFF38BDF8),
        topBarColor = Color(0xFF0F172A),
        headerGradientStart = Color(0xFF1E3A8A),
        headerGradientEnd = Color(0xFF0F172A),
        accentGold = Color(0xFF0284C7),
        cardBorderColor = Color(0xFFE2E8F0),
        backgroundLight = Color(0xFFF8FAFC),
        surfaceLight = Color(0xFFFFFFFF),
        isDark = false,
        fontFamily = FontFamily.Default,
        cardShape = RoundedCornerShape(12.dp),
        buttonShape = RoundedCornerShape(10.dp),
        cardBorderWidth = 0.8.dp,
        cardElevation = 1.dp,
        styleNameHindi = "शांत गंभीर नेवी",
        styleBadge = "🌊 नेवी • 12dp"
    ),

    // 5. Peaceful Lavender - Gentle, soothing spiritual iris
    PEACEFUL_LAVENDER(
        id = "lavender",
        nameHindi = "सौम्य लैवेंडर (Soft Iris)",
        nameEnglish = "Peaceful Lavender",
        icon = "🪻",
        primaryColor = Color(0xFF6D28D9),
        secondaryColor = Color(0xFFA78BFA),
        topBarColor = Color(0xFF4C1D95),
        headerGradientStart = Color(0xFF6D28D9),
        headerGradientEnd = Color(0xFF4C1D95),
        accentGold = Color(0xFFC4B5FD),
        cardBorderColor = Color(0xFFEDE9FE),
        backgroundLight = Color(0xFFFAF8FF),
        surfaceLight = Color(0xFFFFFFFF),
        isDark = false,
        fontFamily = FontFamily.Default,
        cardShape = RoundedCornerShape(14.dp),
        buttonShape = RoundedCornerShape(12.dp),
        cardBorderWidth = 0.8.dp,
        cardElevation = 1.dp,
        styleNameHindi = "सौम्य शांति लैवेंडर",
        styleBadge = "🪻 लैवेंडर • 14dp"
    ),

    // 6. Minimal Slate - Apple-inspired clean dark slate & cyan
    MINIMAL_SLATE(
        id = "slate",
        nameHindi = "एप्पल मिनिमल स्लेट",
        nameEnglish = "Minimal Slate Gray",
        icon = "🩶",
        primaryColor = Color(0xFF334155),
        secondaryColor = Color(0xFF64748B),
        topBarColor = Color(0xFF1E293B),
        headerGradientStart = Color(0xFF334155),
        headerGradientEnd = Color(0xFF1E293B),
        accentGold = Color(0xFF0EA5E9),
        cardBorderColor = Color(0xFFE2E8F0),
        backgroundLight = Color(0xFFF8FAFC),
        surfaceLight = Color(0xFFFFFFFF),
        isDark = false,
        fontFamily = FontFamily.Default,
        cardShape = RoundedCornerShape(16.dp),
        buttonShape = RoundedCornerShape(12.dp),
        cardBorderWidth = 0.8.dp,
        cardElevation = 1.dp,
        styleNameHindi = "एप्पल मिनिमल स्लेट",
        styleBadge = "🩶 स्लेट • 16dp"
    ),

    // 7. Soothing Amber - Earthy sandalwood, calm warm amber
    SOOTHING_AMBER(
        id = "amber",
        nameHindi = "सात्विक चंदन (Soft Amber)",
        nameEnglish = "Soothing Sandalwood Amber",
        icon = "🪵",
        primaryColor = Color(0xFFB45309),
        secondaryColor = Color(0xFFF59E0B),
        topBarColor = Color(0xFF78350F),
        headerGradientStart = Color(0xFFB45309),
        headerGradientEnd = Color(0xFF78350F),
        accentGold = Color(0xFFFBBF24),
        cardBorderColor = Color(0xFFFDE68A),
        backgroundLight = Color(0xFFFFFBEB),
        surfaceLight = Color(0xFFFFFFFF),
        isDark = false,
        fontFamily = FontFamily.Default,
        cardShape = RoundedCornerShape(12.dp),
        buttonShape = RoundedCornerShape(10.dp),
        cardBorderWidth = 0.8.dp,
        cardElevation = 1.dp,
        styleNameHindi = "सात्विक सौम्य चंदन",
        styleBadge = "🪵 चंदन • 12dp"
    ),

    // 8. WhatsApp Dark Night - Official WhatsApp Dark Mode
    WHATSAPP_DARK(
        id = "dark",
        nameHindi = "वाट्सएप डार्क नाइट",
        nameEnglish = "WhatsApp Dark Night",
        icon = "🌙",
        primaryColor = Color(0xFF00A884),
        secondaryColor = Color(0xFF25D366),
        topBarColor = Color(0xFF1F2C34),
        headerGradientStart = Color(0xFF1F2C34),
        headerGradientEnd = Color(0xFF121B22),
        accentGold = Color(0xFF00A884),
        cardBorderColor = Color(0xFF2A3942),
        backgroundLight = Color(0xFF121B22),
        surfaceLight = Color(0xFF1F2C34),
        isDark = true,
        fontFamily = FontFamily.Default,
        cardShape = RoundedCornerShape(12.dp),
        buttonShape = RoundedCornerShape(10.dp),
        cardBorderWidth = 0.8.dp,
        cardElevation = 1.dp,
        styleNameHindi = "वाट्सएप नाइट डार्क",
        styleBadge = "🌙 वाट्सएप • डार्क 12dp"
    );

    companion object {
        val DEFAULT = WHATSAPP_EMERALD

        // Backward-compatibility alias for legacy code
        val ROYAL_MAROON: SacredTheme get() = WHATSAPP_EMERALD

        fun fromId(id: String): SacredTheme {
            return when (id.lowercase().trim()) {
                "whatsapp", "green", "emerald", "maroon" -> WHATSAPP_EMERALD
                "telegram", "blue", "sky" -> TELEGRAM_BLUE
                "sage", "mint" -> CALM_SAGE_MINT
                "ocean", "navy", "indigo" -> OCEAN_INDIGO
                "lavender", "purple", "iris", "violet" -> PEACEFUL_LAVENDER
                "slate", "gray", "grey", "minimal" -> MINIMAL_SLATE
                "amber", "sandalwood", "gold", "saffron", "copper" -> SOOTHING_AMBER
                "dark", "night", "black" -> WHATSAPP_DARK
                else -> WHATSAPP_EMERALD
            }
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
        theme = SacredTheme.WHATSAPP_EMERALD,
        fontFamily = FontFamily.Default,
        cardShape = SacredTheme.WHATSAPP_EMERALD.cardShape,
        buttonShape = SacredTheme.WHATSAPP_EMERALD.buttonShape,
        cardBorderWidth = SacredTheme.WHATSAPP_EMERALD.cardBorderWidth,
        cardElevation = SacredTheme.WHATSAPP_EMERALD.cardElevation,
        cardBorderColor = SacredTheme.WHATSAPP_EMERALD.cardBorderColor,
        isDark = false
    )
}
