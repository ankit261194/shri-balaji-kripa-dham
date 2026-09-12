package com.example.shribalajikripadham.data.model

import org.json.JSONArray
import org.json.JSONObject

data class EmergencyNoticeDto(
    val isEnabled: Boolean = true,
    val noticeHindi: String = "",
    val noticeEnglish: String = ""
)

data class LocationConfigDto(
    val latitude: Double = 28.4089,
    val longitude: Double = 77.3178,
    val allowedRadiusMeters: Double = 500.0,
    val isGeofenceEnforced: Boolean = true,
    val locationName: String = "श्री बालाजी कृपा धाम",
    val updatedAt: Long = System.currentTimeMillis()
)

data class LiveUiConfigDto(
    val updatedAt: String = "",
    val updatedBy: String = "Super Admin",
    val version: Int = 1,
    val activeUiLayout: String = "CLASSIC_DARBAR",
    val emergencyNotice: EmergencyNoticeDto = EmergencyNoticeDto(),
    val locationConfig: LocationConfigDto = LocationConfigDto(),
    val sections: List<UiSectionConfig> = UiSectionConfig.defaultSections()
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("updated_at", updatedAt)
        root.put("updated_by", updatedBy)
        root.put("version", version)
        root.put("active_ui_layout", activeUiLayout)

        val emObj = JSONObject()
        emObj.put("is_enabled", emergencyNotice.isEnabled)
        emObj.put("notice_hindi", emergencyNotice.noticeHindi)
        emObj.put("notice_english", emergencyNotice.noticeEnglish)
        root.put("emergency_notice", emObj)

        val locObj = JSONObject()
        locObj.put("latitude", locationConfig.latitude)
        locObj.put("longitude", locationConfig.longitude)
        locObj.put("allowed_radius_meters", locationConfig.allowedRadiusMeters)
        locObj.put("is_geofence_enforced", locationConfig.isGeofenceEnforced)
        locObj.put("location_name", locationConfig.locationName)
        locObj.put("updated_at", locationConfig.updatedAt)
        root.put("location_config", locObj)

        val secArr = JSONArray()
        sections.forEach { s ->
            val sObj = JSONObject()
            sObj.put("section_id", s.sectionId)
            sObj.put("title_hindi", s.titleHindi)
            sObj.put("title_english", s.titleEnglish)
            sObj.put("icon", s.icon)
            sObj.put("is_visible", s.isVisible)
            sObj.put("order_index", s.orderIndex)
            secArr.put(sObj)
        }
        root.put("sections", secArr)
        return root.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): LiveUiConfigDto? {
            if (jsonStr.isBlank()) return null
            return try {
                val root = JSONObject(jsonStr)
                val updatedAt = root.optString("updated_at", "")
                val updatedBy = root.optString("updated_by", "Super Admin")
                val version = root.optInt("version", 1)
                val activeLayout = root.optString("active_ui_layout", "CLASSIC_DARBAR")

                val emObj = root.optJSONObject("emergency_notice")
                val emergencyNotice = if (emObj != null) {
                    EmergencyNoticeDto(
                        isEnabled = emObj.optBoolean("is_enabled", true),
                        noticeHindi = emObj.optString("notice_hindi", ""),
                        noticeEnglish = emObj.optString("notice_english", "")
                    )
                } else EmergencyNoticeDto()

                val locObj = root.optJSONObject("location_config")
                val locationConfig = if (locObj != null) {
                    LocationConfigDto(
                        latitude = locObj.optDouble("latitude", 28.4089),
                        longitude = locObj.optDouble("longitude", 77.3178),
                        allowedRadiusMeters = locObj.optDouble("allowed_radius_meters", 500.0),
                        isGeofenceEnforced = locObj.optBoolean("is_geofence_enforced", true),
                        locationName = locObj.optString("location_name", "श्री बालाजी कृपा धाम"),
                        updatedAt = locObj.optLong("updated_at", 0L)
                    )
                } else LocationConfigDto()

                val secArr = root.optJSONArray("sections")
                val sectionsList = mutableListOf<UiSectionConfig>()
                if (secArr != null) {
                    for (i in 0 until secArr.length()) {
                        val obj = secArr.getJSONObject(i)
                        sectionsList.add(
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
                }
                LiveUiConfigDto(
                    updatedAt = updatedAt,
                    updatedBy = updatedBy,
                    version = version,
                    activeUiLayout = activeLayout,
                    emergencyNotice = emergencyNotice,
                    locationConfig = locationConfig,
                    sections = if (sectionsList.isNotEmpty()) sectionsList else UiSectionConfig.defaultSections()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
