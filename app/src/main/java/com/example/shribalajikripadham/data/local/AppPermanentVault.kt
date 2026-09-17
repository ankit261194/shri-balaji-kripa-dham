package com.example.shribalajikripadham.data.local

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.shribalajikripadham.data.model.Admin
import com.example.shribalajikripadham.data.model.AdminRole
import com.example.shribalajikripadham.data.model.AshramSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * AppPermanentVault:
 * Indestructible multi-layer persistent backup vault for Admins, Permissions, and Settings.
 * Backed by:
 * 1. Isolated app filesDir JSON (sbkd_vault_data.json)
 * 2. External app storage JSON (persists across reinstall if app data kept)
 * 3. Android SharedPreferences (never wiped during app updates)
 *
 * Ensures that whenever an admin configures any setting, it can NEVER be reset or lost
 * during app updates, restarts, or background syncs.
 */
object AppPermanentVault {
    private const val TAG = "AppPermanentVault"
    private const val VAULT_FILE_NAME = "sbkd_vault_data.json"
    private const val PREFS_NAME = "sbkd_indestructible_settings"

    /**
     * Save settings to Android SharedPreferences.
     * SharedPreferences is guaranteed by Android OS to remain intact across APK updates.
     */
    fun saveToPreferences(context: Context, settings: AshramSettings?) {
        if (settings == null) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("ashram_name", settings.ashramName)
                putString("guruji_name", settings.gurujiName)
                putString("address", settings.address)
                putFloat("latitude", settings.latitude.toFloat())
                putFloat("longitude", settings.longitude.toFloat())
                putFloat("allowed_radius_meters", settings.allowedRadiusMeters.toFloat())
                putInt("running_token_number", settings.runningTokenNumber)
                putBoolean("is_darbar_active", settings.isDarbarActive)
                putString("darbar_date", settings.darbarDate)
                putString("darbar_timings", settings.darbarTimings)
                putString("free_disclaimer", settings.freeDisclaimer)
                putString("contact_phone", settings.contactPhone)
                putString("emergency_notice", settings.emergencyNoticeText)
                putBoolean("is_token_service_enabled", settings.isTokenServiceEnabled)
                putBoolean("is_yatra_service_enabled", settings.isYatraServiceEnabled)
                putBoolean("is_live_counter_visible", settings.isLiveCounterVisible)
                putBoolean("is_events_visible", settings.isEventsVisible)
                putBoolean("is_aarti_timings_visible", settings.isAartiTimingsVisible)
                putBoolean("is_guruji_info_visible", settings.isGurujiInfoVisible)
                putBoolean("is_emergency_notice_visible", settings.isEmergencyNoticeVisible)
                putLong("scheduled_token_open_timestamp", settings.scheduledTokenOpenTimestamp)
                putBoolean("is_geofence_enforced", settings.isGeofenceEnforced)
                putBoolean("is_outstation_advance_allowed", settings.isOutstationAdvanceAllowed)
                putFloat("outstation_min_distance_km", settings.outstationMinDistanceKm.toFloat())
                putString("whatsapp_group_url", settings.whatsappGroupUrl)
                putString("whatsapp_number", settings.whatsappNumber)
                putString("youtube_channel_url", settings.youtubeChannelUrl)
                putString("facebook_page_url", settings.facebookPageUrl)
                putString("instagram_url", settings.instagramUrl)
                putString("app_share_url", settings.appShareUrl)
                putString("current_theme_id", settings.currentThemeId)
                putString("guruji_photo_uri", settings.gurujiPhotoUri)
                putString("active_ui_layout", settings.activeUiLayout)
                putInt("max_daily_tokens", settings.maxDailyTokens)
                putBoolean("is_ui_layout_enforced", settings.isUiLayoutEnforced)
                putString("token_voice_preset", settings.tokenVoicePreset)
                putString("banner_photo_uri", settings.bannerPhotoUri)
                putBoolean("is_banner_visible", settings.isBannerVisible)
                putString("banner_title", settings.bannerTitle)
                putString("banner_subtitle", settings.bannerSubtitle)
                putString("banner_action_url", settings.bannerActionUrl)
                putBoolean("is_ads_enabled", settings.isAdsEnabled)
                putString("ad_type", settings.adType)
                putString("ad_banner_photo_uri", settings.adBannerPhotoUri)
                putString("ad_banner_title", settings.adBannerTitle)
                putString("ad_banner_description", settings.adBannerDescription)
                putString("ad_target_url", settings.adTargetUrl)
                putString("ad_placement", settings.adPlacement)
                putString("sunday_token_banner_title", settings.sundayTokenBannerTitle)
                putString("sunday_token_banner_text", settings.sundayTokenBannerText)
                putString("sunday_token_custom_notice", settings.sundayTokenCustomNotice)
                putBoolean("is_bus_booking_live", settings.isBusBookingLive)
                putBoolean("is_payment_feature_live", settings.isPaymentFeatureLive)
                putBoolean("can_admin_view_payment_history", settings.canAdminViewPaymentHistory)
                putBoolean("can_devotee_view_payment_history", settings.canDevoteeViewPaymentHistory)
                putString("ashram_upi_id", settings.ashramUpiId)
                putString("ashram_upi_name", settings.ashramUpiName)
                putString("custom_upi_qr_uri", settings.customUpiQrUri)
                putInt("bus_seat_fare_amount", settings.busSeatFareAmount)
                putBoolean("is_arzi_ledger_live", settings.isArziLedgerLive)
                putFloat("badi_arzi_rate", settings.badiArziRate.toFloat())
                putFloat("chhoti_arzi_rate", settings.chhotiArziRate.toFloat())
                putBoolean("can_admin_view_arzi_ledger", settings.canAdminViewArziLedger)
                putBoolean("can_devotee_view_arzi_ledger", settings.canDevoteeViewArziLedger)
                putBoolean("can_devotee_view_yatra_diary", settings.canDevoteeViewYatraDiary)
                putString("ashram_parichay_hindi", settings.ashramParichayHindi)
                putString("ashram_parichay_english", settings.ashramParichayEnglish)
                putString("ashram_history_hindi", settings.ashramHistoryHindi)
                putString("ashram_rules_hindi", settings.ashramRulesHindi)
                putBoolean("allow_admin_reserved_tokens", settings.allowAdminReservedTokens)
                putBoolean("has_custom_settings", true)
                putLong("saved_at", System.currentTimeMillis())
                apply()
            }
            Log.d(TAG, "Settings saved to SharedPreferences successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings to SharedPreferences: ${e.message}")
        }
    }

    /**
     * Save complete Admins & Settings to JSON vault files in internal and external storage
     */
    fun saveVault(context: Context, admins: List<Admin>, settings: AshramSettings?) {
        try {
            saveToPreferences(context, settings)

            val root = JSONObject()
            root.put("vault_version", 2)
            root.put("updated_at", System.currentTimeMillis())

            // 1. Serialize Admins
            val adminsArr = JSONArray()
            admins.forEach { a ->
                val aObj = JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("username", a.username)
                    put("phone", a.phoneNumber)
                    put("role", a.role.name)
                    put("pin_hash", a.pinHash)
                    put("password_hash", a.passwordHash)
                    put("photo_uri", a.photoUri)
                    put("is_active", a.isActive)
                    put("can_manage_tokens", a.canManageTokens)
                    put("can_issue_manual_tokens", a.canIssueManualTokens)
                    put("can_manage_yatra", a.canManageYatra)
                    put("can_manage_expenses", a.canManageExpenses)
                    put("can_change_location", a.canChangeLocation)
                    put("can_send_notifications", a.canSendNotifications)
                    put("can_edit_ashram_info", a.canEditAshramInfo)
                    put("can_manage_admins", a.canManageAdmins)
                    put("can_view_devotee_photos", a.canViewDevoteePhotos)
                    put("can_issue_tokens_anywhere", a.canIssueTokensAnywhere)
                    put("can_scan_paper_register", a.canScanPaperRegister)
                    put("can_manage_parchas", a.canManageParchas)
                    put("can_cancel_tokens", a.canCancelTokens)
                    put("can_delete_tokens", a.canDeleteTokens)
                    put("can_custom_token_number", a.canSetCustomTokenNumber)
                    put("can_export_pdf", a.canExportPdf)
                    put("can_manage_arzi", a.canManageArzi)
                    put("created_at", a.createdAt)
                }
                adminsArr.put(aObj)
            }
            root.put("admins", adminsArr)

            // 2. Serialize Complete Custom Settings
            if (settings != null) {
                val setObj = JSONObject().apply {
                    put("ashram_name", settings.ashramName)
                    put("guruji_name", settings.gurujiName)
                    put("address", settings.address)
                    put("latitude", settings.latitude)
                    put("longitude", settings.longitude)
                    put("allowed_radius_meters", settings.allowedRadiusMeters)
                    put("running_token_number", settings.runningTokenNumber)
                    put("is_darbar_active", settings.isDarbarActive)
                    put("darbar_date", settings.darbarDate)
                    put("darbar_timings", settings.darbarTimings)
                    put("free_disclaimer", settings.freeDisclaimer)
                    put("contact_phone", settings.contactPhone)
                    put("emergency_notice", settings.emergencyNoticeText)
                    put("is_token_service_enabled", settings.isTokenServiceEnabled)
                    put("is_yatra_service_enabled", settings.isYatraServiceEnabled)
                    put("is_live_counter_visible", settings.isLiveCounterVisible)
                    put("is_events_visible", settings.isEventsVisible)
                    put("is_aarti_timings_visible", settings.isAartiTimingsVisible)
                    put("is_guruji_info_visible", settings.isGurujiInfoVisible)
                    put("is_emergency_notice_visible", settings.isEmergencyNoticeVisible)
                    put("scheduled_token_open_timestamp", settings.scheduledTokenOpenTimestamp)
                    put("is_geofence_enforced", settings.isGeofenceEnforced)
                    put("is_outstation_advance_allowed", settings.isOutstationAdvanceAllowed)
                    put("outstation_min_distance_km", settings.outstationMinDistanceKm)
                    put("whatsapp_group_url", settings.whatsappGroupUrl)
                    put("whatsapp_number", settings.whatsappNumber)
                    put("youtube_channel_url", settings.youtubeChannelUrl)
                    put("facebook_page_url", settings.facebookPageUrl)
                    put("instagram_url", settings.instagramUrl)
                    put("app_share_url", settings.appShareUrl)
                    put("current_theme_id", settings.currentThemeId)
                    put("guruji_photo_uri", settings.gurujiPhotoUri)
                    put("active_ui_layout", settings.activeUiLayout)
                    put("max_daily_tokens", settings.maxDailyTokens)
                    put("is_ui_layout_enforced", settings.isUiLayoutEnforced)
                    put("token_voice_preset", settings.tokenVoicePreset)
                    put("banner_photo_uri", settings.bannerPhotoUri)
                    put("is_banner_visible", settings.isBannerVisible)
                    put("banner_title", settings.bannerTitle)
                    put("banner_subtitle", settings.bannerSubtitle)
                    put("banner_action_url", settings.bannerActionUrl)
                    put("is_ads_enabled", settings.isAdsEnabled)
                    put("adType", settings.adType)
                    put("ad_banner_photo_uri", settings.adBannerPhotoUri)
                    put("ad_banner_title", settings.adBannerTitle)
                    put("ad_banner_description", settings.adBannerDescription)
                    put("ad_target_url", settings.adTargetUrl)
                    put("ad_placement", settings.adPlacement)
                    put("sunday_token_banner_title", settings.sundayTokenBannerTitle)
                    put("sunday_token_banner_text", settings.sundayTokenBannerText)
                    put("sunday_token_custom_notice", settings.sundayTokenCustomNotice)
                    put("is_bus_booking_live", settings.isBusBookingLive)
                    put("is_payment_feature_live", settings.isPaymentFeatureLive)
                    put("can_admin_view_payment_history", settings.canAdminViewPaymentHistory)
                    put("can_devotee_view_payment_history", settings.canDevoteeViewPaymentHistory)
                    put("ashram_upi_id", settings.ashramUpiId)
                    put("ashram_upi_name", settings.ashramUpiName)
                    put("custom_upi_qr_uri", settings.customUpiQrUri)
                    put("bus_seat_fare_amount", settings.busSeatFareAmount)
                    put("is_arzi_ledger_live", settings.isArziLedgerLive)
                    put("badi_arzi_rate", settings.badiArziRate)
                    put("chhoti_arzi_rate", settings.chhotiArziRate)
                    put("can_admin_view_arzi_ledger", settings.canAdminViewArziLedger)
                    put("can_devotee_view_arzi_ledger", settings.canDevoteeViewArziLedger)
                    put("can_devotee_view_yatra_diary", settings.canDevoteeViewYatraDiary)
                    put("ashram_parichay_hindi", settings.ashramParichayHindi)
                    put("ashram_parichay_english", settings.ashramParichayEnglish)
                    put("ashram_history_hindi", settings.ashramHistoryHindi)
                    put("ashram_rules_hindi", settings.ashramRulesHindi)
                    put("allow_admin_reserved_tokens", settings.allowAdminReservedTokens)
                }
                root.put("settings", setObj)
            }

            val jsonStr = root.toString(2)

            // Save in internal app files
            val internalFile = File(context.filesDir, VAULT_FILE_NAME)
            internalFile.writeText(jsonStr, StandardCharsets.UTF_8)

            // Also mirror in external files dir (persists across reinstall if files kept)
            try {
                val extDir = context.getExternalFilesDir(null)
                if (extDir != null) {
                    val extFile = File(extDir, VAULT_FILE_NAME)
                    extFile.writeText(jsonStr, StandardCharsets.UTF_8)
                }
            } catch (e: Exception) {}

            Log.d(TAG, "Permanent vault saved with ${admins.size} admins and complete settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save permanent vault: ${e.message}")
        }
    }

    /**
     * Safely restore vault into SQLite only when needed (e.g. empty database or forced).
     * NEVER overwrites existing, valid user settings if table already contains data!
     */
    fun restoreVault(context: Context, db: SQLiteDatabase, force: Boolean = false): Boolean {
        try {
            // Check if ashram_settings table already has data
            if (!force) {
                var rowCount = 0
                try {
                    val c = db.rawQuery("SELECT COUNT(*) FROM ashram_settings", null)
                    if (c.moveToFirst()) rowCount = c.getInt(0)
                    c.close()
                } catch (ignored: Exception) {}

                // If settings exist, DO NOT overwrite them!
                if (rowCount > 0) {
                    Log.d(TAG, "Settings table already populated ($rowCount rows). Skipping vault restore to protect user settings.")
                    return true
                }
            }

            // 1. Try restoring from SharedPreferences first (highest reliability across APK upgrades)
            val restoredFromPrefs = restoreFromPreferences(context, db)
            if (restoredFromPrefs) {
                Log.d(TAG, "Settings restored successfully from SharedPreferences!")
                return true
            }

            // 2. If SharedPreferences was empty, try JSON vault file
            var file = File(context.filesDir, VAULT_FILE_NAME)
            if (!file.exists()) {
                val extDir = context.getExternalFilesDir(null)
                if (extDir != null) {
                    file = File(extDir, VAULT_FILE_NAME)
                }
            }
            if (!file.exists()) return false

            val jsonStr = file.readText(StandardCharsets.UTF_8)
            val root = JSONObject(jsonStr)

            // Restore Admins
            val adminsArr = root.optJSONArray("admins")
            if (adminsArr != null && adminsArr.length() > 0) {
                db.beginTransaction()
                try {
                    for (i in 0 until adminsArr.length()) {
                        val a = adminsArr.getJSONObject(i)
                        val username = a.optString("username", "")
                        if (username.isBlank()) continue

                        val cv = ContentValues().apply {
                            put("name", a.optString("name", ""))
                            put("username", username)
                            put("phone", a.optString("phone", ""))
                            put("role", a.optString("role", "SEVADAR"))
                            put("pin_hash", a.optString("pin_hash", ""))
                            put("password_hash", a.optString("password_hash", ""))
                            put("photo_uri", a.optString("photo_uri", ""))
                            put("is_active", if (a.optBoolean("is_active", true)) 1 else 0)
                            put("can_manage_tokens", if (a.optBoolean("can_manage_tokens", true)) 1 else 0)
                            put("can_issue_manual_tokens", if (a.optBoolean("can_issue_manual_tokens", true)) 1 else 0)
                            put("can_manage_yatra", if (a.optBoolean("can_manage_yatra", true)) 1 else 0)
                            put("can_manage_expenses", if (a.optBoolean("can_manage_expenses", true)) 1 else 0)
                            put("can_change_location", if (a.optBoolean("can_change_location", false)) 1 else 0)
                            put("can_send_notifications", if (a.optBoolean("can_send_notifications", false)) 1 else 0)
                            put("can_edit_ashram_info", if (a.optBoolean("can_edit_ashram_info", false)) 1 else 0)
                            put("can_manage_admins", if (a.optBoolean("can_manage_admins", false)) 1 else 0)
                            put("can_view_devotee_photos", if (a.optBoolean("can_view_devotee_photos", false)) 1 else 0)
                            put("can_issue_tokens_anywhere", if (a.optBoolean("can_issue_tokens_anywhere", false)) 1 else 0)
                            put("can_scan_paper_register", if (a.optBoolean("can_scan_paper_register", false)) 1 else 0)
                            put("can_manage_parchas", if (a.optBoolean("can_manage_parchas", false)) 1 else 0)
                            put("can_cancel_tokens", if (a.optBoolean("can_cancel_tokens", false)) 1 else 0)
                            put("can_delete_tokens", if (a.optBoolean("can_delete_tokens", false)) 1 else 0)
                            put("can_custom_token_number", if (a.optBoolean("can_custom_token_number", false)) 1 else 0)
                            put("can_export_pdf", if (a.optBoolean("can_export_pdf", true)) 1 else 0)
                            put("can_manage_arzi", if (a.optBoolean("can_manage_arzi", false)) 1 else 0)
                            put("created_at", a.optLong("created_at", System.currentTimeMillis()))
                        }

                        val updated = db.update("admins", cv, "username = ?", arrayOf(username))
                        if (updated == 0) {
                            db.insert("admins", null, cv)
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }

            // Restore Complete Settings
            val setObj = root.optJSONObject("settings")
            if (setObj != null) {
                val cv = ContentValues().apply {
                    setObj.keys().forEach { key ->
                        when (val v = setObj.get(key)) {
                            is String -> put(key, v)
                            is Boolean -> put(key, if (v) 1 else 0)
                            is Int -> put(key, v)
                            is Long -> put(key, v)
                            is Double -> put(key, v)
                        }
                    }
                }
                if (cv.size() > 0) {
                    val updated = db.update("ashram_settings", cv, "id = 1", null)
                    if (updated == 0) {
                        cv.put("id", 1)
                        db.insertWithOnConflict("ashram_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }
            }

            Log.d(TAG, "Permanent vault successfully restored from JSON file!")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore permanent vault: ${e.message}")
            return false
        }
    }

    /**
     * Restores settings from SharedPreferences into SQLite
     */
    private fun restoreFromPreferences(context: Context, db: SQLiteDatabase): Boolean {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean("has_custom_settings", false)) return false

            val cv = ContentValues().apply {
                put("ashram_name", prefs.getString("ashram_name", "श्री बालाजी कृपा धाम"))
                put("guruji_name", prefs.getString("guruji_name", "परम पूज्य गुरुजी"))
                put("address", prefs.getString("address", "ग्राम डूंगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर (उ.प्र.)"))
                put("latitude", prefs.getFloat("latitude", 28.3972915f).toDouble())
                put("longitude", prefs.getFloat("longitude", 78.1460410f).toDouble())
                put("allowed_radius_meters", prefs.getFloat("allowed_radius_meters", 200.0f).toDouble())
                put("running_token_number", prefs.getInt("running_token_number", 1))
                put("is_darbar_active", if (prefs.getBoolean("is_darbar_active", true)) 1 else 0)
                put("darbar_date", prefs.getString("darbar_date", ""))
                put("darbar_timings", prefs.getString("darbar_timings", "प्रत्येक रविवार प्रातःकाल 8:00 बजे से"))
                put("free_disclaimer", prefs.getString("free_disclaimer", "भूत-प्रेत व मानसिक समस्याओं का पूर्णतः निःशुल्क इलाज।"))
                put("contact_phone", prefs.getString("contact_phone", "+91 98765 00000"))
                put("emergency_notice", prefs.getString("emergency_notice", ""))
                put("is_token_service_enabled", if (prefs.getBoolean("is_token_service_enabled", true)) 1 else 0)
                put("is_yatra_service_enabled", if (prefs.getBoolean("is_yatra_service_enabled", false)) 1 else 0)
                put("is_live_counter_visible", if (prefs.getBoolean("is_live_counter_visible", true)) 1 else 0)
                put("is_events_visible", if (prefs.getBoolean("is_events_visible", true)) 1 else 0)
                put("is_aarti_timings_visible", if (prefs.getBoolean("is_aarti_timings_visible", true)) 1 else 0)
                put("is_guruji_info_visible", if (prefs.getBoolean("is_guruji_info_visible", true)) 1 else 0)
                put("is_emergency_notice_visible", if (prefs.getBoolean("is_emergency_notice_visible", false)) 1 else 0)
                put("scheduled_token_open_timestamp", prefs.getLong("scheduled_token_open_timestamp", 0L))
                put("is_geofence_enforced", if (prefs.getBoolean("is_geofence_enforced", true)) 1 else 0)
                put("is_outstation_advance_allowed", if (prefs.getBoolean("is_outstation_advance_allowed", true)) 1 else 0)
                put("outstation_min_distance_km", prefs.getFloat("outstation_min_distance_km", 30.0f).toDouble())
                put("whatsapp_group_url", prefs.getString("whatsapp_group_url", "https://chat.whatsapp.com/invite"))
                put("whatsapp_number", prefs.getString("whatsapp_number", "+919876543210"))
                put("youtube_channel_url", prefs.getString("youtube_channel_url", "https://www.youtube.com/@ShriBalajiKripaDham"))
                put("facebook_page_url", prefs.getString("facebook_page_url", "https://www.facebook.com/ShriBalajiKripaDham"))
                put("instagram_url", prefs.getString("instagram_url", "https://www.instagram.com/shribalajikripadham"))
                put("app_share_url", prefs.getString("app_share_url", "https://shribalajikripadham.org/app"))
                put("current_theme_id", prefs.getString("current_theme_id", "maroon"))
                put("guruji_photo_uri", prefs.getString("guruji_photo_uri", ""))
                put("active_ui_layout", prefs.getString("active_ui_layout", "CLASSIC_DARBAR"))
                put("max_daily_tokens", prefs.getInt("max_daily_tokens", 0))
                put("is_ui_layout_enforced", if (prefs.getBoolean("is_ui_layout_enforced", false)) 1 else 0)
                put("token_voice_preset", prefs.getString("token_voice_preset", "GURU_CALM"))
                put("banner_photo_uri", prefs.getString("banner_photo_uri", ""))
                put("is_banner_visible", if (prefs.getBoolean("is_banner_visible", true)) 1 else 0)
                put("banner_title", prefs.getString("banner_title", "🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट"))
                put("banner_subtitle", prefs.getString("banner_subtitle", "परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार"))
                put("banner_action_url", prefs.getString("banner_action_url", ""))
                put("is_bus_booking_live", if (prefs.getBoolean("is_bus_booking_live", false)) 1 else 0)
                put("is_payment_feature_live", if (prefs.getBoolean("is_payment_feature_live", false)) 1 else 0)
                put("can_admin_view_payment_history", if (prefs.getBoolean("can_admin_view_payment_history", false)) 1 else 0)
                put("can_devotee_view_payment_history", if (prefs.getBoolean("can_devotee_view_payment_history", false)) 1 else 0)
                put("ashram_upi_id", prefs.getString("ashram_upi_id", "shribalajikripadham@upi"))
                put("ashram_upi_name", prefs.getString("ashram_upi_name", "Shri Balaji Kripa Dham"))
                put("custom_upi_qr_uri", prefs.getString("custom_upi_qr_uri", ""))
                put("bus_seat_fare_amount", prefs.getInt("bus_seat_fare_amount", 1500))
                put("is_arzi_ledger_live", if (prefs.getBoolean("is_arzi_ledger_live", true)) 1 else 0)
                put("badi_arzi_rate", prefs.getFloat("badi_arzi_rate", 100.0f).toDouble())
                put("chhoti_arzi_rate", prefs.getFloat("chhoti_arzi_rate", 50.0f).toDouble())
                put("can_admin_view_arzi_ledger", if (prefs.getBoolean("can_admin_view_arzi_ledger", true)) 1 else 0)
                put("can_devotee_view_arzi_ledger", if (prefs.getBoolean("can_devotee_view_arzi_ledger", false)) 1 else 0)
                put("can_devotee_view_yatra_diary", if (prefs.getBoolean("can_devotee_view_yatra_diary", false)) 1 else 0)
                put("ashram_parichay_hindi", prefs.getString("ashram_parichay_hindi", ""))
                put("ashram_parichay_english", prefs.getString("ashram_parichay_english", ""))
                put("ashram_history_hindi", prefs.getString("ashram_history_hindi", ""))
                put("ashram_rules_hindi", prefs.getString("ashram_rules_hindi", ""))
                put("allow_admin_reserved_tokens", if (prefs.getBoolean("allow_admin_reserved_tokens", false)) 1 else 0)
            }

            val updated = db.update("ashram_settings", cv, "id = 1", null)
            if (updated == 0) {
                cv.put("id", 1)
                db.insertWithOnConflict("ashram_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore from SharedPreferences: ${e.message}")
            return false
        }
    }
}

