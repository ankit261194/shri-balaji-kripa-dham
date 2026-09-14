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
        titleHindi = "आधुनिक फ्लोटिंग",
        titleEnglish = "Modern Floating Cards",
        subtitleHindi = "उभरे हुए 3D कार्ड्स व आधुनिक स्टाइल",
        icon = "🎴"
    ),
    VEDIC_GRID(
        id = "VEDIC_GRID",
        titleHindi = "वैदिक मंदिर ग्रिड",
        titleEnglish = "Vedic Mandir Grid",
        subtitleHindi = "2-कॉलम सममित पावन मंदिर शिखर ग्रिड",
        icon = "🏛️"
    ),
    COMPACT_LIST(
        id = "COMPACT_LIST",
        titleHindi = "त्वरित दर्शन सूची",
        titleEnglish = "Speed Compact List",
        subtitleHindi = "सुपरफास्ट 1-क्लिक न्यूनतम सूची",
        icon = "⚡"
    ),
    DIVINE_FEED(
        id = "DIVINE_FEED",
        titleHindi = "दिव्य दर्शन फीड",
        titleEnglish = "Divine Devotional Feed",
        subtitleHindi = "भक्तिमय पत्रिका, दर्शन व विचार टाइमलाइन",
        icon = "📜"
    ),
    MAHABALI_HERO(
        id = "MAHABALI_HERO",
        titleHindi = "महाबली डैशबोर्ड",
        titleEnglish = "Mahabali Hero Hub",
        subtitleHindi = "विशाल बजरंगबली कार्ड व सर्कुलर एक्शन डॉक",
        icon = "🚩"
    ),
    BHAKTI_ACCORDION(
        id = "BHAKTI_ACCORDION",
        titleHindi = "भक्ति संगम (ड्रॉपडाउन)",
        titleEnglish = "Bhakti Sangam (Accordions)",
        subtitleHindi = "सभी सुविधाएं स्मूथ स्क्रॉल-डाउन ड्रॉपडाउन में",
        icon = "🔽"
    ),
    PARIKRAMA_FLOW(
        id = "PARIKRAMA_FLOW",
        titleHindi = "मंदिर परिक्रमा (स्वाइप)",
        titleEnglish = "Mandir Parikrama Flow",
        subtitleHindi = "4 मुख्य धाम पड़ाव एवं क्षैतिज परिक्रमा",
        icon = "🔄"
    ),
    GOLDEN_LOTUS(
        id = "GOLDEN_LOTUS",
        titleHindi = "गोल्डन लोटस (श्वेत पदम)",
        titleEnglish = "Golden Lotus Spiritual",
        subtitleHindi = "शांत पावन श्वेत-स्वर्ण आध्यात्मिक भव्यता",
        icon = "🪷"
    ),
    SIDDHA_PEETH_PORTAL(
        id = "SIDDHA_PEETH_PORTAL",
        titleHindi = "सिद्ध पीठ नेविगेटर",
        titleEnglish = "Siddha Peeth Portal",
        subtitleHindi = "शीर्ष टैब बार से त्वरित श्रेणी फ़िल्टर",
        icon = "⛩️"
    );

    companion object {
        fun fromId(id: String): AppUiLayout {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CLASSIC_DARBAR
        }
    }
}
