package com.example.shribalajikripadham.data.model

import org.json.JSONArray
import org.json.JSONObject

data class UiSectionConfig(
    val sectionId: String,
    val titleHindi: String,
    val titleEnglish: String,
    val icon: String,
    val isVisible: Boolean = true,
    val orderIndex: Int = 0
) {
    companion object {
        const val ID_GURUJI_BANNER = "GURUJI_BANNER"
        const val ID_EMERGENCY_NOTICE = "EMERGENCY_NOTICE"
        const val ID_FREE_TREATMENT_BOX = "FREE_TREATMENT_BOX"
        const val ID_TOKEN_COUNTDOWN = "TOKEN_COUNTDOWN"
        const val ID_SMART_FACE_TOKEN = "SMART_FACE_TOKEN"
        const val ID_QUICK_SERVICES = "QUICK_SERVICES"
        const val ID_DARBAR_STATUS = "DARBAR_STATUS"
        const val ID_SEVADAR_TEAM = "SEVADAR_TEAM"
        const val ID_DYNAMIC_EVENTS = "DYNAMIC_EVENTS"
        const val ID_AARTI_TIMINGS = "AARTI_TIMINGS"
        const val ID_SOCIAL_MEDIA_HUB = "SOCIAL_MEDIA_HUB"
        const val ID_CONTACT_FOOTER = "CONTACT_FOOTER"

        fun defaultSections(): List<UiSectionConfig> = listOf(
            UiSectionConfig(
                sectionId = ID_GURUJI_BANNER,
                titleHindi = "गुरुजी दर्शन बैनर व आश्रम हेडर",
                titleEnglish = "Guruji Banner & Ashram Header",
                icon = "👑",
                isVisible = true,
                orderIndex = 0
            ),
            UiSectionConfig(
                sectionId = ID_EMERGENCY_NOTICE,
                titleHindi = "आपातकालीन घोषणा टिकर",
                titleEnglish = "Emergency Broadcast Notice",
                icon = "📢",
                isVisible = true,
                orderIndex = 1
            ),
            UiSectionConfig(
                sectionId = ID_FREE_TREATMENT_BOX,
                titleHindi = "100% निःशुल्क सेवा सूचना बॉक्स",
                titleEnglish = "100% Free Treatment Notice",
                icon = "🕊️",
                isVisible = true,
                orderIndex = 2
            ),
            UiSectionConfig(
                sectionId = ID_TOKEN_COUNTDOWN,
                titleHindi = "टोकन समय-सीमा / उलटी गिनती",
                titleEnglish = "Token Schedule Countdown",
                icon = "⏳",
                isVisible = true,
                orderIndex = 3
            ),
            UiSectionConfig(
                sectionId = ID_SMART_FACE_TOKEN,
                titleHindi = "स्मार्ट फेस टोकन कार्ड (< 1s)",
                titleEnglish = "Smart Face Token Card",
                icon = "⚡",
                isVisible = true,
                orderIndex = 4
            ),
            UiSectionConfig(
                sectionId = ID_QUICK_SERVICES,
                titleHindi = "मुख्य सेवाएँ व विकल्प ग्रिड",
                titleEnglish = "Quick Services Grid",
                icon = "📱",
                isVisible = true,
                orderIndex = 5
            ),
            UiSectionConfig(
                sectionId = ID_DARBAR_STATUS,
                titleHindi = "रविवार दरबार व लाइव टोकन काउंटर",
                titleEnglish = "Darbar Status & Token Counter",
                icon = "🔢",
                isVisible = true,
                orderIndex = 6
            ),
            UiSectionConfig(
                sectionId = ID_SEVADAR_TEAM,
                titleHindi = "आश्रम सेवादार दल",
                titleEnglish = "Sevadar Team Showcase",
                icon = "🤝",
                isVisible = true,
                orderIndex = 7
            ),
            UiSectionConfig(
                sectionId = ID_DYNAMIC_EVENTS,
                titleHindi = "आगामी धार्मिक उत्सव व कार्यक्रम",
                titleEnglish = "Upcoming Ashram Events",
                icon = "🚩",
                isVisible = true,
                orderIndex = 8
            ),
            UiSectionConfig(
                sectionId = ID_AARTI_TIMINGS,
                titleHindi = "आरती एवं दर्शन समय-सारणी",
                titleEnglish = "Aarti & Darbar Timings",
                icon = "🪔",
                isVisible = true,
                orderIndex = 9
            ),
            UiSectionConfig(
                sectionId = ID_SOCIAL_MEDIA_HUB,
                titleHindi = "सोशल मीडिया हब (YT, FB, Insta, WA)",
                titleEnglish = "Social Media Hub",
                icon = "🌐",
                isVisible = true,
                orderIndex = 10
            ),
            UiSectionConfig(
                sectionId = ID_CONTACT_FOOTER,
                titleHindi = "धाम संपर्क सूत्र व आधिकारिक पता",
                titleEnglish = "Ashram Contact & Location",
                icon = "📍",
                isVisible = true,
                orderIndex = 11
            )
        )

        fun toJson(configs: List<UiSectionConfig>): String {
            val arr = JSONArray()
            configs.forEach { item ->
                val obj = JSONObject().apply {
                    put("section_id", item.sectionId)
                    put("title_hindi", item.titleHindi)
                    put("title_english", item.titleEnglish)
                    put("icon", item.icon)
                    put("is_visible", item.isVisible)
                    put("order_index", item.orderIndex)
                }
                arr.put(obj)
            }
            return arr.toString()
        }

        fun fromJson(jsonStr: String): List<UiSectionConfig> {
            if (jsonStr.isBlank()) return defaultSections()
            return try {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<UiSectionConfig>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        UiSectionConfig(
                            sectionId = obj.optString("section_id", ""),
                            titleHindi = obj.optString("title_hindi", ""),
                            titleEnglish = obj.optString("title_english", ""),
                            icon = obj.optString("icon", "📌"),
                            isVisible = obj.optBoolean("is_visible", true),
                            orderIndex = obj.optInt("order_index", i)
                        )
                    )
                }
                if (list.isEmpty()) defaultSections() else list
            } catch (e: Exception) {
                defaultSections()
            }
        }
    }
}
