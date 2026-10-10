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
    val styleBadge: String = "💬 वाट्सएप • 12dp",
    val watermarkText: String = "",
    val watermarkIcon: String = "",
    val glowColor: Color = Color.Transparent,
    val isTricolor: Boolean = false,
    val festivalCategoryHindi: String = "दैनिक",
    val divineMotif: String = "",
    val divineChantHindi: String = "",
    val divineChantEnglish: String = "",
    val festiveBannerTitle: String = ""
) {
    // =========================================================================
    // 🌟 4 DIVINE SACRED THEMES (दिव्य उत्सव व राष्ट्र गौरव थीम्स)
    // =========================================================================

    // 1. 🇮🇳 Tiranga Theme (National Flag / Deshbhakti / अखंड भारत)
    TIRANGA(
        id = "tiranga",
        nameHindi = "राष्ट्र ध्वज तिरंगा",
        nameEnglish = "Tiranga National Pride",
        icon = "🇮🇳",
        primaryColor = Color(0xFFFF9933), // Deep Kesariya / Saffron
        secondaryColor = Color(0xFF138808), // Sacred Emerald Green
        topBarColor = Color(0xFFE65100), // Vibrant Saffron / Kesariya
        headerGradientStart = Color(0xFFFF6F00),
        headerGradientEnd = Color(0xFFE65100),
        accentGold = Color(0xFF1A237E), // Navy Blue Ashoka Chakra
        cardBorderColor = Color(0xFFD1E7DD),
        backgroundLight = Color(0xFFF8FAF9),
        surfaceLight = Color(0xFFFFFFFF),
        isDark = false,
        fontFamily = FontFamily.Default,
        cardShape = RoundedCornerShape(14.dp),
        buttonShape = RoundedCornerShape(10.dp),
        cardBorderWidth = 1.dp,
        cardElevation = 2.dp,
        styleNameHindi = "राष्ट्र ध्वज तिरंगा (अखंड भारत)",
        styleBadge = "🇮🇳 तिरंगा • अखंड भारत",
        watermarkText = "वन्दे मातरम्",
        watermarkIcon = "🇮🇳",
        glowColor = Color(0xFFFF9933),
        isTricolor = true,
        festivalCategoryHindi = "राष्ट्रीय पर्व",
        divineMotif = "🇮🇳 ☸️ 🇮🇳",
        divineChantHindi = "वन्दे मातरम् • अखंड भारत राष्ट्र गौरव",
        divineChantEnglish = "Vande Mataram • Akhand Bharat Pride",
        festiveBannerTitle = "राष्ट्र गौरव तिरंगा उत्सव"
    ),

    // 2. 🪔 Divya Deepawali Theme (Festival of Lights / रोशनी का पावन पर्व)
    DIVYA_DEEPAWALI(
        id = "deepawali",
        nameHindi = "दिव्य दीपावली",
        nameEnglish = "Divya Deepawali (Lights)",
        icon = "🪔",
        primaryColor = Color(0xFFF59E0B), // Radiant Amber Gold
        secondaryColor = Color(0xFFFBBF24), // Golden Ray Glow
        topBarColor = Color(0xFF0F172A), // Royal Midnight Velvet
        headerGradientStart = Color(0xFF1E293B),
        headerGradientEnd = Color(0xFF0B0F19),
        accentGold = Color(0xFFFDE68A), // Deepak Jyoti Gold
        cardBorderColor = Color(0xFF374151),
        backgroundLight = Color(0xFF0B0F19), // Deep Velvet Midnight Night
        surfaceLight = Color(0xFF161F30), // Deep Slate Card Surface
        isDark = true,
        fontFamily = FontFamily.Default,
        cardShape = RoundedCornerShape(16.dp),
        buttonShape = RoundedCornerShape(12.dp),
        cardBorderWidth = 1.2.dp,
        cardElevation = 4.dp,
        styleNameHindi = "दिव्य दीपावली (महालक्ष्मी प्रकाश)",
        styleBadge = "🪔 दीपावली • दिव्य प्रकाश",
        watermarkText = "शुभ दीपावली",
        watermarkIcon = "🪔",
        glowColor = Color(0xFFFBBF24),
        isTricolor = false,
        festivalCategoryHindi = "दीपावली महोत्सव",
        divineMotif = "🪔 ✨ 🪔",
        divineChantHindi = "शुभ दीपावली • महालक्ष्मी प्रकाश एवं सुख-समृद्धि",
        divineChantEnglish = "Shubh Deepawali • Divine Light of Mahalakshmi",
        festiveBannerTitle = "दिव्य दीपोत्सव एवं महालक्ष्मी कृपा"
    ),

    // 3. 🦁 Sherawali Maiya Theme (Maa Durga / Navratri Shakti / शेरावाली)
    SHERAWALI_MAIYA(
        id = "sherawali",
        nameHindi = "शेरावाली मैया",
        nameEnglish = "Sherawali Durga Shakti",
        icon = "🦁",
        primaryColor = Color(0xFF991B1B), // Deep Crimson Sindoor Red
        secondaryColor = Color(0xFFDC2626), // Radiant Crimson Vermilion
        topBarColor = Color(0xFF880808), // Sacred Sindoor Maroon
        headerGradientStart = Color(0xFF991B1B),
        headerGradientEnd = Color(0xFF5A0000),
        accentGold = Color(0xFFD4AF37), // Golden Gota-Kinari Lace
        cardBorderColor = Color(0xFFFECDD3),
        backgroundLight = Color(0xFFFFF7F7), // Vermilion Sacred Warmth
        surfaceLight = Color(0xFFFFFFFF),
        isDark = false,
        fontFamily = FontFamily.Default,
        cardShape = RoundedCornerShape(14.dp),
        buttonShape = RoundedCornerShape(10.dp),
        cardBorderWidth = 1.dp,
        cardElevation = 2.dp,
        styleNameHindi = "शेरावाली मैया (नवरात्रि शक्ति)",
        styleBadge = "🦁 शेरावाली • शक्ति स्वरूपा",
        watermarkText = "जय माता दी",
        watermarkIcon = "🦁",
        glowColor = Color(0xFFEF4444),
        isTricolor = false,
        festivalCategoryHindi = "नवरात्रि उत्सव",
        divineMotif = "🔱 🦁 🔱",
        divineChantHindi = "जय माता दी • शक्ति स्वरूपा माँ शेरावाली की असीम कृपा",
        divineChantEnglish = "Jai Mata Di • Divine Grace of Maa Sherawali",
        festiveBannerTitle = "नवरात्रि शक्ति स्वरूपा शेरावाली मैया"
    ),

    // 4. 🚩 Veer Bajrangi Hanuman Ji Theme (Sindoori Balaji / संकट मोचन)
    VEER_HANUMAN(
        id = "hanuman",
        nameHindi = "वीर बजरंगी हनुमान",
        nameEnglish = "Veer Bajrangi Balaji",
        icon = "🚩",
        primaryColor = Color(0xFFC2410C), // Chameli Oil Sindoori Orange
        secondaryColor = Color(0xFFEA580C), // Radiant Sindoor
        topBarColor = Color(0xFF9A3412), // Deep Sindoori Bronze
        headerGradientStart = Color(0xFFC2410C),
        headerGradientEnd = Color(0xFF7C2D12),
        accentGold = Color(0xFFF59E0B), // Ashtadhatu Antique Gold
        cardBorderColor = Color(0xFFFED7AA), // Vajra Saffron Border
        backgroundLight = Color(0xFFFFF8F1), // Sindoori Sandalwood Warmth
        surfaceLight = Color(0xFFFFFFFF),
        isDark = false,
        fontFamily = FontFamily.Default,
        cardShape = RoundedCornerShape(12.dp),
        buttonShape = RoundedCornerShape(10.dp),
        cardBorderWidth = 1.dp,
        cardElevation = 2.dp,
        styleNameHindi = "वीर बजरंगी हनुमान (संकट मोचन)",
        styleBadge = "🚩 वीर बजरंगी • संकट मोचन",
        watermarkText = "जय श्री राम",
        watermarkIcon = "🚩",
        glowColor = Color(0xFFF97316),
        isTricolor = false,
        festivalCategoryHindi = "हनुमान दरबार",
        divineMotif = "🚩 ⚡ 🚩",
        divineChantHindi = "जय श्री राम • संकट मोचन श्री बालाजी महाराज",
        divineChantEnglish = "Jai Shri Ram • Sankat Mochan Balaji Maharaj",
        festiveBannerTitle = "वीर बजरंगी संकट मोचन दरबार"
    ),

    // =========================================================================
    // 🍃 8 MODERN PEACEFUL THEMES (शांत व सात्विक थीम्स)
    // =========================================================================

    // 5. WhatsApp Emerald - Soothing, iconic clean green (Default)
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
        styleBadge = "💬 वाट्सएप • 12dp",
        watermarkText = "श्री बालाजी धाम",
        watermarkIcon = "💬",
        glowColor = Color(0xFF25D366),
        isTricolor = false,
        festivalCategoryHindi = "दैनिक",
        divineMotif = "💬 🍃 💬",
        divineChantHindi = "श्री बालाजी कृपा धाम • दैनिक दर्शन व सेवा",
        divineChantEnglish = "Shri Balaji Kripa Dham • Daily Darshan & Seva",
        festiveBannerTitle = "दैनिक पावन दर्शन"
    ),

    // 6. Telegram Sky Blue - Modern, crisp, fresh
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
        styleBadge = "✈️ टेलीग्राम • 14dp",
        divineMotif = "✈️ 🌊 ✈️",
        divineChantHindi = "निर्मल आकाश जैसी शांति • भक्ति व समर्पण",
        divineChantEnglish = "Sky Calm • Pure Devotion & Serenity",
        festiveBannerTitle = "टेलीग्राम शांत स्काई"
    ),

    // 7. Calm Sage Mint - Peaceful, Ayurvedic, serene forest
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
        styleBadge = "🍃 सात्विक • 14dp",
        divineMotif = "🍃 🌿 🍃",
        divineChantHindi = "सात्विक शांति • तुलसी व सेज मिंट शीतलता",
        divineChantEnglish = "Peaceful Sage Mint • Ayurvedic Serenity",
        festiveBannerTitle = "सात्विक प्राकृतिक शांति"
    ),

    // 8. Ocean Indigo - Deep, calm, executive
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
        styleBadge = "🌊 नेवी • 12dp",
        divineMotif = "🌊 ⚓ 🌊",
        divineChantHindi = "गंभीर भक्ति महासागर • स्थिर मन व ध्यान",
        divineChantEnglish = "Deep Ocean Calm • Steady Devotion & Peace",
        festiveBannerTitle = "शांत महासागर नेवी"
    ),

    // 9. Peaceful Lavender - Gentle, soothing spiritual iris
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
        styleBadge = "🪻 लैवेंडर • 14dp",
        divineMotif = "🪻 🌸 🪻",
        divineChantHindi = "सौम्य पारिजात शांति • कोमल आध्यात्मिक आभा",
        divineChantEnglish = "Gentle Lavender • Spiritual Harmony",
        festiveBannerTitle = "सौम्य लैवेंडर शांति"
    ),

    // 10. Minimal Slate - Apple-inspired clean dark slate & cyan
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
        styleBadge = "🩶 स्लेट • 16dp",
        divineMotif = "🩶 🏛️ 🩶",
        divineChantHindi = "सादगी व स्वच्छता • एकाग्र चित्त प्रार्थना",
        divineChantEnglish = "Minimal Slate • Pure Simplicity & Focus",
        festiveBannerTitle = "एप्पल मिनिमल स्लेट"
    ),

    // 11. Soothing Amber - Earthy sandalwood, calm warm amber
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
        styleBadge = "🪵 चंदन • 12dp",
        divineMotif = "🪵 🪔 🪵",
        divineChantHindi = "ॐ नमो भगवते वासुदेवाय • सात्विक चंदन सुगंध",
        divineChantEnglish = "Sacred Sandalwood • Calming Amber Fragrance",
        festiveBannerTitle = "सात्विक चंदन पीतांबर"
    ),

    // 12. WhatsApp Dark Night - Official WhatsApp Dark Mode
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
        styleBadge = "🌙 वाट्सएप • डार्क 12dp",
        divineMotif = "🌙 ✨ 🌙",
        divineChantHindi = "रात्रि ध्यान व विश्राम • नयनाभिराम शांत दर्शन",
        divineChantEnglish = "Night Meditation • Soothing Dark Mode",
        festiveBannerTitle = "वाट्सएप नाइट डार्क"
    );

    companion object {
        val DEFAULT = WHATSAPP_EMERALD

        // Backward-compatibility alias for legacy code
        val ROYAL_MAROON: SacredTheme get() = SHERAWALI_MAIYA

        fun fromId(id: String): SacredTheme {
            return when (id.lowercase().trim()) {
                "tiranga", "flag", "india", "bharat", "deshbhakti" -> TIRANGA
                "deepawali", "diwali", "roshni", "lights", "deep" -> DIVYA_DEEPAWALI
                "sherawali", "durga", "maiya", "navratri", "shakti", "maroon" -> SHERAWALI_MAIYA
                "hanuman", "bajrangi", "balaji", "sindoori", "sindoor" -> VEER_HANUMAN
                "whatsapp", "green", "emerald" -> WHATSAPP_EMERALD
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
    val isDark: Boolean,
    val watermarkText: String = "",
    val watermarkIcon: String = "",
    val glowColor: Color = Color.Transparent,
    val isTricolor: Boolean = false,
    val styleBadge: String = "",
    val divineMotif: String = "",
    val divineChantHindi: String = "",
    val divineChantEnglish: String = "",
    val festiveBannerTitle: String = ""
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
        isDark = false,
        watermarkText = SacredTheme.WHATSAPP_EMERALD.watermarkText,
        watermarkIcon = SacredTheme.WHATSAPP_EMERALD.watermarkIcon,
        glowColor = SacredTheme.WHATSAPP_EMERALD.glowColor,
        isTricolor = SacredTheme.WHATSAPP_EMERALD.isTricolor,
        styleBadge = SacredTheme.WHATSAPP_EMERALD.styleBadge,
        divineMotif = SacredTheme.WHATSAPP_EMERALD.divineMotif,
        divineChantHindi = SacredTheme.WHATSAPP_EMERALD.divineChantHindi,
        divineChantEnglish = SacredTheme.WHATSAPP_EMERALD.divineChantEnglish,
        festiveBannerTitle = SacredTheme.WHATSAPP_EMERALD.festiveBannerTitle
    )
}
