package com.example.shribalajikripadham.data.model

import org.json.JSONArray
import org.json.JSONObject

data class EmergencyNoticeDto(
    val isEnabled: Boolean = true,
    val noticeHindi: String = "",
    val noticeEnglish: String = ""
)

data class LocationConfigDto(
    val latitude: Double = 28.3972915,
    val longitude: Double = 78.1460410,
    val allowedRadiusMeters: Double = 200.0,
    val isGeofenceEnforced: Boolean = true,
    val isOutstationAdvanceAllowed: Boolean = true,
    val outstationMinDistanceKm: Double = 30.0,
    val locationName: String = "श्री बालाजी कृपा धाम",
    val updatedAt: Long = System.currentTimeMillis()
)

data class AshramDetailsConfigDto(
    val ashramName: String = "श्री बालाजी कृपा धाम",
    val gurujiName: String = "परम पूज्य गुरुजी",
    val address: String = "ग्राम डूंगरा जाट, तहसील शिकारपुर, जिला बुलन्दशहर (उ.प्र.)",
    val contactPhone: String = "7417531776",
    val contactPhoneSecondary: String = "9456488344",
    val whatsappNumber: String = "7417531776",
    val darbarTimings: String = "प्रत्येक रविवार प्रातः 7:00 बजे से प्रभु इच्छा तक",
    val freeDisclaimer: String = "भूत-प्रेत व मानसिक समस्याओं का पूर्णतः निःशुल्क (FREE) इलाज। कोई शुल्क अथवा दक्षिणा नहीं ली जाती।",
    val whatsappGroupUrl: String = "https://chat.whatsapp.com/invite",
    val youtubeChannelUrl: String = "https://www.youtube.com/@ShriBalajiKripaDham",
    val facebookPageUrl: String = "https://www.facebook.com/ShriBalajiKripaDham",
    val instagramUrl: String = "https://www.instagram.com/shribalajikripadham",
    val appShareUrl: String = "https://shribalajikripadham.org/app",
    val gurujiPhotoUrl: String = ""
)

data class ServicesConfigDto(
    val isTokenServiceEnabled: Boolean = true,
    val isYatraServiceEnabled: Boolean = false,
    val isLiveCounterVisible: Boolean = true,
    val isEventsVisible: Boolean = true,
    val isAartiTimingsVisible: Boolean = true,
    val isGurujiInfoVisible: Boolean = true,
    val isEmergencyNoticeVisible: Boolean = true,
    val scheduledTokenOpenTimestamp: Long = 0L,
    val maxDailyTokens: Int = 0,
    val sundayTokenBannerTitle: String = "",
    val sundayTokenBannerText: String = "",
    val sundayTokenCustomNotice: String = "",
    val isBusBookingLive: Boolean = false,
    val isPaymentFeatureLive: Boolean = false,
    val canAdminViewPaymentHistory: Boolean = false,
    val canDevoteeViewPaymentHistory: Boolean = false,
    val ashramUpiId: String = "shribalajikripadham@upi",
    val ashramUpiName: String = "Shri Balaji Kripa Dham",
    val customUpiQrUri: String = "",
    val busSeatFareAmount: Int = 1500,
    val isArziLedgerLive: Boolean = true,
    val badiArziRate: Double = 100.0,
    val chhotiArziRate: Double = 50.0,
    val canAdminViewArziLedger: Boolean = true,
    val canDevoteeViewArziLedger: Boolean = false,
    val canDevoteeViewYatraDiary: Boolean = false,
    val bannerTitle: String = "🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट",
    val bannerSubtitle: String = "परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार",
    val bannerPhotoUri: String = "",
    val isBannerVisible: Boolean = true,
    val bannerActionUrl: String = ""
)

data class AshramEventConfigDto(
    val id: Long = 0,
    val titleHindi: String = "",
    val titleEnglish: String = "",
    val dateDescriptionHindi: String = "",
    val dateDescriptionEnglish: String = "",
    val detailsHindi: String = "",
    val detailsEnglish: String = "",
    val isActive: Boolean = true
)

data class AppUpdateLiveDto(
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val apkUrl: String = "",
    val updateNotesHindi: String = "",
    val updateNotesEnglish: String = "",
    val isForceUpdate: Boolean = false
)

data class LiveUiConfigDto(
    val updatedAt: String = "",
    val updatedBy: String = "Super Admin",
    val version: Int = 1,
    val activeUiLayout: String = "CLASSIC_DARBAR",
    val ashramDetails: AshramDetailsConfigDto = AshramDetailsConfigDto(),
    val emergencyNotice: EmergencyNoticeDto = EmergencyNoticeDto(),
    val locationConfig: LocationConfigDto = LocationConfigDto(),
    val servicesConfig: ServicesConfigDto = ServicesConfigDto(),
    val sections: List<UiSectionConfig> = UiSectionConfig.defaultSections(),
    val events: List<AshramEventConfigDto> = emptyList(),
    val appUpdate: AppUpdateLiveDto? = null
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("updated_at", updatedAt)
        root.put("updated_by", updatedBy)
        root.put("version", version)
        root.put("active_ui_layout", activeUiLayout)

        if (appUpdate != null && appUpdate.latestVersionCode > 0) {
            val updObj = JSONObject()
            updObj.put("latest_version_code", appUpdate.latestVersionCode)
            updObj.put("latest_version_name", appUpdate.latestVersionName)
            updObj.put("apk_url", appUpdate.apkUrl)
            updObj.put("update_notes_hindi", appUpdate.updateNotesHindi)
            updObj.put("update_notes_english", appUpdate.updateNotesEnglish)
            updObj.put("is_force_update", appUpdate.isForceUpdate)
            root.put("app_update", updObj)
        }

        val detObj = JSONObject()
        detObj.put("ashram_name", ashramDetails.ashramName)
        detObj.put("guruji_name", ashramDetails.gurujiName)
        detObj.put("address", ashramDetails.address)
        detObj.put("contact_phone", ashramDetails.contactPhone)
        detObj.put("contact_phone_secondary", ashramDetails.contactPhoneSecondary)
        detObj.put("whatsapp_number", ashramDetails.whatsappNumber)
        detObj.put("darbar_timings", ashbarTimings(ashramDetails))
        detObj.put("free_disclaimer", ashramDetails.freeDisclaimer)
        detObj.put("whatsapp_group_url", ashramDetails.whatsappGroupUrl)
        detObj.put("youtube_channel_url", ashramDetails.youtubeChannelUrl)
        detObj.put("facebook_page_url", ashramDetails.facebookPageUrl)
        detObj.put("instagram_url", ashramDetails.instagramUrl)
        detObj.put("app_share_url", ashramDetails.appShareUrl)
        detObj.put("guruji_photo_url", ashramDetails.gurujiPhotoUrl)
        root.put("ashram_details", detObj)

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
        locObj.put("is_outstation_advance_allowed", locationConfig.isOutstationAdvanceAllowed)
        locObj.put("outstation_min_distance_km", locationConfig.outstationMinDistanceKm)
        locObj.put("location_name", locationConfig.locationName)
        locObj.put("updated_at", locationConfig.updatedAt)
        root.put("location_config", locObj)

        val srvObj = JSONObject()
        srvObj.put("is_token_service_enabled", servicesConfig.isTokenServiceEnabled)
        srvObj.put("is_yatra_service_enabled", servicesConfig.isYatraServiceEnabled)
        srvObj.put("is_live_counter_visible", servicesConfig.isLiveCounterVisible)
        srvObj.put("is_events_visible", servicesConfig.isEventsVisible)
        srvObj.put("is_aarti_timings_visible", servicesConfig.isAartiTimingsVisible)
        srvObj.put("is_guruji_info_visible", servicesConfig.isGurujiInfoVisible)
        srvObj.put("is_emergency_notice_visible", servicesConfig.isEmergencyNoticeVisible)
        srvObj.put("scheduled_token_open_timestamp", servicesConfig.scheduledTokenOpenTimestamp)
        srvObj.put("max_daily_tokens", servicesConfig.maxDailyTokens)
        srvObj.put("sunday_token_banner_title", servicesConfig.sundayTokenBannerTitle)
        srvObj.put("sunday_token_banner_text", servicesConfig.sundayTokenBannerText)
        srvObj.put("sunday_token_custom_notice", servicesConfig.sundayTokenCustomNotice)
        srvObj.put("is_bus_booking_live", servicesConfig.isBusBookingLive)
        srvObj.put("is_payment_feature_live", servicesConfig.isPaymentFeatureLive)
        srvObj.put("can_admin_view_payment_history", servicesConfig.canAdminViewPaymentHistory)
        srvObj.put("can_devotee_view_payment_history", servicesConfig.canDevoteeViewPaymentHistory)
        srvObj.put("ashram_upi_id", servicesConfig.ashramUpiId)
        srvObj.put("ashram_upi_name", servicesConfig.ashramUpiName)
        srvObj.put("custom_upi_qr_uri", servicesConfig.customUpiQrUri)
        srvObj.put("bus_seat_fare_amount", servicesConfig.busSeatFareAmount)
        srvObj.put("is_arzi_ledger_live", servicesConfig.isArziLedgerLive)
        srvObj.put("badi_arzi_rate", servicesConfig.badiArziRate)
        srvObj.put("chhoti_arzi_rate", servicesConfig.chhotiArziRate)
        srvObj.put("can_admin_view_arzi_ledger", servicesConfig.canAdminViewArziLedger)
        srvObj.put("can_devotee_view_arzi_ledger", servicesConfig.canDevoteeViewArziLedger)
        srvObj.put("can_devotee_view_yatra_diary", servicesConfig.canDevoteeViewYatraDiary)
        srvObj.put("banner_title", servicesConfig.bannerTitle)
        srvObj.put("banner_subtitle", servicesConfig.bannerSubtitle)
        srvObj.put("banner_photo_uri", servicesConfig.bannerPhotoUri)
        srvObj.put("is_banner_visible", servicesConfig.isBannerVisible)
        srvObj.put("banner_action_url", servicesConfig.bannerActionUrl)
        root.put("services_config", srvObj)

        val secArr = JSONArray()
        sections.forEach { s ->
            val sObj = JSONObject()
            sObj.put("section_id", s.sectionId)
            sObj.put("title_hindi", s.titleHindi)
            sObj.put("title_english", s.titleEnglish)
            sObj.put("icon", s.icon)
            sObj.put("is_visible", s.isVisible)
            sObj.put("order_index", s.orderIndex)
            sObj.put("custom_subtitle_hindi", s.customSubtitleHindi)
            sObj.put("custom_subtitle_english", s.customSubtitleEnglish)
            sObj.put("custom_content_hindi", s.customContentHindi)
            sObj.put("custom_content_english", s.customContentEnglish)
            secArr.put(sObj)
        }
        root.put("sections", secArr)

        val evtArr = JSONArray()
        events.forEach { e ->
            val eObj = JSONObject()
            eObj.put("id", e.id)
            eObj.put("title_hindi", e.titleHindi)
            eObj.put("title_english", e.titleEnglish)
            eObj.put("date_desc_hindi", e.dateDescriptionHindi)
            eObj.put("date_desc_english", e.dateDescriptionEnglish)
            eObj.put("details_hindi", e.detailsHindi)
            eObj.put("details_english", e.detailsEnglish)
            eObj.put("is_active", e.isActive)
            evtArr.put(eObj)
        }
        root.put("events", evtArr)

        return root.toString(2)
    }

    private fun ashbarTimings(details: AshramDetailsConfigDto): String = details.darbarTimings

    companion object {
        fun fromJson(jsonStr: String): LiveUiConfigDto? {
            if (jsonStr.isBlank()) return null
            return try {
                val root = JSONObject(jsonStr)
                val updatedAt = root.optString("updated_at", "")
                val updatedBy = root.optString("updated_by", "Super Admin")
                val version = root.optInt("version", 1)
                val activeLayout = root.optString("active_ui_layout", "CLASSIC_DARBAR")

                val detObj = root.optJSONObject("ashram_details")
                val ashramDetails = if (detObj != null) {
                    AshramDetailsConfigDto(
                        ashramName = detObj.optString("ashram_name", "श्री बालाजी कृपा धाम"),
                        gurujiName = detObj.optString("guruji_name", "परम पूज्य गुरुजी"),
                        address = detObj.optString("address", "ग्राम डूंगरा जाट, तहसील शिकारपुर, जिला बुलन्दशहर (उ.प्र.)"),
                        contactPhone = detObj.optString("contact_phone", "7417531776"),
                        contactPhoneSecondary = detObj.optString("contact_phone_secondary", "9456488344"),
                        whatsappNumber = detObj.optString("whatsapp_number", "7417531776"),
                        darbarTimings = detObj.optString("darbar_timings", "प्रत्येक रविवार प्रातः 7:00 बजे से प्रभु इच्छा तक"),
                        freeDisclaimer = detObj.optString("free_disclaimer", "भूत-प्रेत व मानसिक समस्याओं का पूर्णतः निःशुल्क (FREE) इलाज। कोई शुल्क अथवा दक्षिणा नहीं ली जाती।"),
                        whatsappGroupUrl = detObj.optString("whatsapp_group_url", "https://chat.whatsapp.com/invite"),
                        youtubeChannelUrl = detObj.optString("youtube_channel_url", "https://www.youtube.com/@ShriBalajiKripaDham"),
                        facebookPageUrl = detObj.optString("facebook_page_url", "https://www.facebook.com/ShriBalajiKripaDham"),
                        instagramUrl = detObj.optString("instagram_url", "https://www.instagram.com/shribalajikripadham"),
                        appShareUrl = detObj.optString("app_share_url", "https://shribalajikripadham.org/app"),
                        gurujiPhotoUrl = detObj.optString("guruji_photo_url", "")
                    )
                } else AshramDetailsConfigDto()

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
                        latitude = locObj.optDouble("latitude", 28.3972915),
                        longitude = locObj.optDouble("longitude", 78.1460410),
                        allowedRadiusMeters = locObj.optDouble("allowed_radius_meters", 200.0).coerceIn(10.0, 50000.0),
                        isGeofenceEnforced = locObj.optBoolean("is_geofence_enforced", true),
                        isOutstationAdvanceAllowed = locObj.optBoolean("is_outstation_advance_allowed", true),
                        outstationMinDistanceKm = locObj.optDouble("outstation_min_distance_km", 30.0),
                        locationName = locObj.optString("location_name", "श्री बालाजी कृपा धाम"),
                        updatedAt = locObj.optLong("updated_at", 0L)
                    )
                } else LocationConfigDto()

                val srvObj = root.optJSONObject("services_config")
                val servicesConfig = if (srvObj != null) {
                    ServicesConfigDto(
                        isTokenServiceEnabled = srvObj.optBoolean("is_token_service_enabled", true),
                        isYatraServiceEnabled = srvObj.optBoolean("is_yatra_service_enabled", false),
                        isLiveCounterVisible = srvObj.optBoolean("is_live_counter_visible", true),
                        isEventsVisible = srvObj.optBoolean("is_events_visible", true),
                        isAartiTimingsVisible = srvObj.optBoolean("is_aarti_timings_visible", true),
                        isGurujiInfoVisible = srvObj.optBoolean("is_guruji_info_visible", true),
                        isEmergencyNoticeVisible = srvObj.optBoolean("is_emergency_notice_visible", true),
                        scheduledTokenOpenTimestamp = srvObj.optLong("scheduled_token_open_timestamp", 0L),
                        maxDailyTokens = srvObj.optInt("max_daily_tokens", 0),
                        sundayTokenBannerTitle = srvObj.optString("sunday_token_banner_title", ""),
                        sundayTokenBannerText = srvObj.optString("sunday_token_banner_text", ""),
                        sundayTokenCustomNotice = srvObj.optString("sunday_token_custom_notice", ""),
                        isBusBookingLive = srvObj.optBoolean("is_bus_booking_live", false),
                        isPaymentFeatureLive = srvObj.optBoolean("is_payment_feature_live", false),
                        canAdminViewPaymentHistory = srvObj.optBoolean("can_admin_view_payment_history", false),
                        canDevoteeViewPaymentHistory = srvObj.optBoolean("can_devotee_view_payment_history", false),
                        ashramUpiId = srvObj.optString("ashram_upi_id", "shribalajikripadham@upi"),
                        ashramUpiName = srvObj.optString("ashram_upi_name", "Shri Balaji Kripa Dham"),
                        customUpiQrUri = srvObj.optString("custom_upi_qr_uri", ""),
                        busSeatFareAmount = srvObj.optInt("bus_seat_fare_amount", 1500),
                        isArziLedgerLive = srvObj.optBoolean("is_arzi_ledger_live", true),
                        badiArziRate = srvObj.optDouble("badi_arzi_rate", 100.0),
                        chhotiArziRate = srvObj.optDouble("chhoti_arzi_rate", 50.0),
                        canAdminViewArziLedger = srvObj.optBoolean("can_admin_view_arzi_ledger", true),
                        canDevoteeViewArziLedger = srvObj.optBoolean("can_devotee_view_arzi_ledger", false),
                        canDevoteeViewYatraDiary = srvObj.optBoolean("can_devotee_view_yatra_diary", false),
                        bannerTitle = srvObj.optString("banner_title", "🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट"),
                        bannerSubtitle = srvObj.optString("banner_subtitle", "परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार"),
                        bannerPhotoUri = srvObj.optString("banner_photo_uri", ""),
                        isBannerVisible = srvObj.optBoolean("is_banner_visible", true),
                        bannerActionUrl = srvObj.optString("banner_action_url", "")
                    )
                } else ServicesConfigDto()

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
                                orderIndex = obj.optInt("order_index", i),
                                customSubtitleHindi = obj.optString("custom_subtitle_hindi", ""),
                                customSubtitleEnglish = obj.optString("custom_subtitle_english", ""),
                                customContentHindi = obj.optString("custom_content_hindi", ""),
                                customContentEnglish = obj.optString("custom_content_english", "")
                            )
                        )
                    }
                }

                val eventsList = mutableListOf<AshramEventConfigDto>()
                val evtArr = root.optJSONArray("events")
                if (evtArr != null) {
                    for (i in 0 until evtArr.length()) {
                        val obj = evtArr.getJSONObject(i)
                        eventsList.add(
                            AshramEventConfigDto(
                                id = obj.optLong("id", 0L),
                                titleHindi = obj.optString("title_hindi", ""),
                                titleEnglish = obj.optString("title_english", ""),
                                dateDescriptionHindi = obj.optString("date_desc_hindi", ""),
                                dateDescriptionEnglish = obj.optString("date_desc_english", ""),
                                detailsHindi = obj.optString("details_hindi", ""),
                                detailsEnglish = obj.optString("details_english", ""),
                                isActive = obj.optBoolean("is_active", true)
                            )
                        )
                    }
                }

                val appUpdate = if (root.has("app_update")) {
                    val updObj = root.optJSONObject("app_update")
                    if (updObj != null) {
                        AppUpdateLiveDto(
                            latestVersionCode = updObj.optInt("latest_version_code", 0),
                            latestVersionName = updObj.optString("latest_version_name", ""),
                            apkUrl = updObj.optString("apk_url", ""),
                            updateNotesHindi = updObj.optString("update_notes_hindi", ""),
                            updateNotesEnglish = updObj.optString("update_notes_english", ""),
                            isForceUpdate = updObj.optBoolean("is_force_update", false)
                        )
                    } else null
                } else null

                LiveUiConfigDto(
                    updatedAt = updatedAt,
                    updatedBy = updatedBy,
                    version = version,
                    activeUiLayout = activeLayout,
                    ashramDetails = ashramDetails,
                    emergencyNotice = emergencyNotice,
                    locationConfig = locationConfig,
                    servicesConfig = servicesConfig,
                    sections = if (sectionsList.isNotEmpty()) sectionsList else UiSectionConfig.defaultSections(),
                    events = eventsList,
                    appUpdate = appUpdate
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
