package com.example.shribalajikripadham.ui.home

enum class AppUiLayout(
    val id: String,
    val titleHindi: String,
    val titleEnglish: String,
    val subtitleHindi: String,
    val icon: String
) {
    CLASSIC_DARBAR(
        id = "CLASSIC_DARBAR",
        titleHindi = "राजसी दरबार",
        titleEnglish = "Royal Darbar",
        subtitleHindi = "पारंपरिक भव्य एवं पावन स्वरूप",
        icon = "👑"
    ),
    MODERN_CARDS(
        id = "MODERN_CARDS",
        titleHindi = "आधुनिक कार्ड्स",
        titleEnglish = "Modern Cards",
        subtitleHindi = "सरल, सुंदर एवं आधुनिक दृश्य",
        icon = "🎴"
    ),
    VEDIC_GRID(
        id = "VEDIC_GRID",
        titleHindi = "वैदिक ग्रिड",
        titleEnglish = "Vedic Grid",
        subtitleHindi = "दोहरी ग्रिड में मंदिर दर्शन",
        icon = "🏛️"
    ),
    COMPACT_LIST(
        id = "COMPACT_LIST",
        titleHindi = "त्वरित दर्शन सूची",
        titleEnglish = "Quick Compact",
        subtitleHindi = "तेज़ व सुलभ 1-क्लिक एक्सेस",
        icon = "⚡"
    ),
    DIVINE_FEED(
        id = "DIVINE_FEED",
        titleHindi = "दिव्य दर्शन फीड",
        titleEnglish = "Divine Feed",
        subtitleHindi = "भक्तिमय पत्रिका एवं दर्शन शैली",
        icon = "📜"
    );

    companion object {
        fun fromId(id: String): AppUiLayout {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CLASSIC_DARBAR
        }
    }
}
