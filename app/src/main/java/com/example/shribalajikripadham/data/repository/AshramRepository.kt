package com.example.shribalajikripadham.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.shribalajikripadham.ai.FaceEmbeddingEngine
import com.example.shribalajikripadham.data.local.DatabaseHelper
import com.example.shribalajikripadham.data.model.*
import com.example.shribalajikripadham.hardware.GeofenceLocationManager
import com.example.shribalajikripadham.util.DistanceCalculatorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class CredentialCheckResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class AdminPermissionsUpdate(
    val canManageTokens: Boolean = true,
    val canIssueManualTokens: Boolean = true,
    val canManageYatra: Boolean = true,
    val canManageExpenses: Boolean = true,
    val canChangeLocation: Boolean = false,
    val canSendNotifications: Boolean = false,
    val canEditAshramInfo: Boolean = false,
    val canViewDevoteePhotos: Boolean = false,
    val canIssueTokensAnywhere: Boolean = false,
    val canScanPaperRegister: Boolean = false,
    val canManageParchas: Boolean = false,
    val canManageArzi: Boolean = false,
    val canManageHavan: Boolean = false,
    val canCancelTokens: Boolean = false,
    val canDeleteTokens: Boolean = false,
    val canSetCustomTokenNumber: Boolean = false,
    val canExportPdf: Boolean = true,
    // Granular decoupled service permissions:
    val canManageVoiceSettings: Boolean = false,
    val canManageDailyDarshan: Boolean = false,
    val canManageLiveStudio: Boolean = false,
    val canManageAartiBhajan: Boolean = false,
    val canManageUiControl: Boolean = false,
    val canManageTuesdayDarbar: Boolean = false,
    val canManageIdCards: Boolean = false,
    val canManageHelpdesk: Boolean = false,
    val canViewPaymentLedger: Boolean = false,
    val canManageWebsite: Boolean = false,
    val canManageServicesToggles: Boolean = false,
    val canManageDistances: Boolean = false,
    val isActive: Boolean = true
)

class AshramRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dbHelper = DatabaseHelper(appContext)

    companion object {
        private val tokenGenerationLock = Any()
    }

    // --- Ashram Settings & Customization ---
    private fun parseSettingsCursor(cursor: Cursor): AshramSettings {
        return AshramSettings(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            ashramName = cursor.getString(cursor.getColumnIndexOrThrow("ashram_name")),
            gurujiName = cursor.getString(cursor.getColumnIndexOrThrow("guruji_name")),
            address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
            allowedRadiusMeters = cursor.getDouble(cursor.getColumnIndexOrThrow("allowed_radius_meters")),
            runningTokenNumber = cursor.getInt(cursor.getColumnIndexOrThrow("running_token_number")).coerceAtLeast(1),
            isDarbarActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_darbar_active")) == 1,
            darbarDate = cursor.getString(cursor.getColumnIndexOrThrow("darbar_date")),
            darbarTimings = cursor.getString(cursor.getColumnIndexOrThrow("darbar_timings")),
            freeDisclaimer = cursor.getString(cursor.getColumnIndexOrThrow("free_disclaimer")),
            contactPhone = try { cursor.getString(cursor.getColumnIndexOrThrow("contact_phone"))?.trim() ?: "" } catch (e: Exception) { "" },
            emergencyNoticeText = cursor.getString(cursor.getColumnIndexOrThrow("emergency_notice")),
            isTokenServiceEnabled = cursor.getInt(cursor.getColumnIndexOrThrow("is_token_service_enabled")) == 1,
            tokenServiceMode = try { cursor.getString(cursor.getColumnIndexOrThrow("token_service_mode")) ?: "AUTO_SUNDAY" } catch (e: Exception) { "AUTO_SUNDAY" },
            isYatraServiceEnabled = cursor.getInt(cursor.getColumnIndexOrThrow("is_yatra_service_enabled")) == 1,
            isLiveCounterVisible = cursor.getInt(cursor.getColumnIndexOrThrow("is_live_counter_visible")) == 1,
            isEventsVisible = cursor.getInt(cursor.getColumnIndexOrThrow("is_events_visible")) == 1,
            isAartiTimingsVisible = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_aarti_timings_visible")) == 1 } catch (e: Exception) { true },
            isGurujiInfoVisible = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_guruji_info_visible")) == 1 } catch (e: Exception) { true },
            isEmergencyNoticeVisible = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_emergency_notice_visible")) == 1 } catch (e: Exception) { true },
            scheduledTokenOpenTimestamp = try { cursor.getLong(cursor.getColumnIndexOrThrow("scheduled_token_open_timestamp")) } catch (e: Exception) { 0L },
            isGeofenceEnforced = cursor.getInt(cursor.getColumnIndexOrThrow("is_geofence_enforced")) == 1,
            isOutstationAdvanceAllowed = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_outstation_advance_allowed")) == 1 } catch (e: Exception) { true },
            outstationMinDistanceKm = try { cursor.getDouble(cursor.getColumnIndexOrThrow("outstation_min_distance_km")) } catch (e: Exception) { 30.0 },
            latestVersionCode = cursor.getInt(cursor.getColumnIndexOrThrow("latest_version_code")),
            latestVersionName = cursor.getString(cursor.getColumnIndexOrThrow("latest_version_name")),
            updateNotes = cursor.getString(cursor.getColumnIndexOrThrow("update_notes")),
            apkDownloadUrl = cursor.getString(cursor.getColumnIndexOrThrow("apk_download_url")),
            isForceUpdate = cursor.getInt(cursor.getColumnIndexOrThrow("is_force_update")) == 1,
            whatsappGroupUrl = try { val v = cursor.getString(cursor.getColumnIndexOrThrow("whatsapp_group_url")); if (v.isNullOrBlank() || v.contains("/invite")) "https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0" else v } catch (e: Exception) { "https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0" },
            whatsappNumber = try { cursor.getString(cursor.getColumnIndexOrThrow("whatsapp_number"))?.trim() ?: "" } catch (e: Exception) { "" },
            youtubeChannelUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("youtube_channel_url")) } catch (e: Exception) { "https://www.youtube.com/@ShriBalajiKripaDham" },
            facebookPageUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("facebook_page_url")) } catch (e: Exception) { "https://www.facebook.com/ShriBalajiKripaDham" },
            instagramUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("instagram_url")) } catch (e: Exception) { "https://www.instagram.com/shribalajikripadham" },
            appShareUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("app_share_url")) } catch (e: Exception) { "https://shribalajikripadham.online/app" },
            currentThemeId = try { cursor.getString(cursor.getColumnIndexOrThrow("current_theme_id")) } catch (e: Exception) { "maroon" },
            gurujiPhotoUri = try { cursor.getString(cursor.getColumnIndexOrThrow("guruji_photo_uri")) } catch (e: Exception) { "" } ?: "",
            activeUiLayout = try { cursor.getString(cursor.getColumnIndexOrThrow("active_ui_layout")) } catch (e: Exception) { "CLASSIC_DARBAR" } ?: "CLASSIC_DARBAR",
            maxDailyTokens = try { cursor.getInt(cursor.getColumnIndexOrThrow("max_daily_tokens")) } catch (e: Exception) { 0 },
            allowAdminReservedTokens = try { cursor.getInt(cursor.getColumnIndexOrThrow("allow_admin_reserved_tokens")) == 1 } catch (e: Exception) { false },
            isUiLayoutEnforced = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_ui_layout_enforced")) == 1 } catch (e: Exception) { false },
            cloudSyncUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("cloud_sync_url")) } catch (e: Exception) { "" } ?: "",
            isCloudSyncEnabled = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_cloud_sync_enabled")) == 1 } catch (e: Exception) { false },
            sundayTokenBannerTitle = try { cursor.getString(cursor.getColumnIndexOrThrow("sunday_token_banner_title")) } catch (e: Exception) { "हार्डवेयर फिंगरप्रिंट नियम: 1 फोन = 1 टोकन" } ?: "हार्डवेयर फिंगरप्रिंट नियम: 1 फोन = 1 टोकन",
            sundayTokenBannerText = try { cursor.getString(cursor.getColumnIndexOrThrow("sunday_token_banner_text")) } catch (e: Exception) { "एक मोबाइल डिवाइस से प्रत्येक रविवार को केवल 1 मरीज का टोकन लिया जा सकता है।" } ?: "एक मोबाइल डिवाइस से प्रत्येक रविवार को केवल 1 मरीज का टोकन लिया जा सकता है।",
            sundayTokenCustomNotice = try { cursor.getString(cursor.getColumnIndexOrThrow("sunday_token_custom_notice")) } catch (e: Exception) { "" } ?: "",
            isBusBookingLive = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_bus_booking_live")) == 1 } catch (e: Exception) { false },
            isDharamshalaLive = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_dharamshala_live")) == 1 } catch (e: Exception) { false },
            isPaymentFeatureLive = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_payment_feature_live")) == 1 } catch (e: Exception) { false },
            canAdminViewPaymentHistory = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_admin_view_payment_history")) == 1 } catch (e: Exception) { false },
            canDevoteeViewPaymentHistory = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_devotee_view_payment_history")) == 1 } catch (e: Exception) { false },
            ashramUpiId = try { cursor.getString(cursor.getColumnIndexOrThrow("ashram_upi_id")) ?: "" } catch (e: Exception) { "" },
            ashramUpiName = try { cursor.getString(cursor.getColumnIndexOrThrow("ashram_upi_name")) } catch (e: Exception) { "Shri Balaji Kripa Dham" } ?: "Shri Balaji Kripa Dham",
            customUpiQrUri = try { cursor.getString(cursor.getColumnIndexOrThrow("custom_upi_qr_uri")) } catch (e: Exception) { "" } ?: "",
            busSeatFareAmount = try { cursor.getInt(cursor.getColumnIndexOrThrow("bus_seat_fare_amount")) } catch (e: Exception) { 0 },
            isArziLedgerLive = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_arzi_ledger_live")) == 1 } catch (e: Exception) { true },
            badiArziRate = try { cursor.getDouble(cursor.getColumnIndexOrThrow("badi_arzi_rate")) } catch (e: Exception) { 100.0 },
            chhotiArziRate = try { cursor.getDouble(cursor.getColumnIndexOrThrow("chhoti_arzi_rate")) } catch (e: Exception) { 50.0 },
            canAdminViewArziLedger = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_admin_view_arzi_ledger")) == 1 } catch (e: Exception) { true },
            canDevoteeViewArziLedger = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_devotee_view_arzi_ledger")) == 1 } catch (e: Exception) { false },
            canDevoteeViewYatraDiary = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_devotee_view_yatra_diary")) == 1 } catch (e: Exception) { false },
            ashramParichayHindi = try { cursor.getString(cursor.getColumnIndexOrThrow("ashram_parichay_hindi")) ?: "श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट, तहसील अनूपशहर, ज़िला बुलन्दशहर, उ.प्र.) में परम पूज्य गुरुजी तेजवीर सिंह जी के मार्गदर्शन में भूत-प्रेत, ऊपरी बाधा व मानसिक कष्टों का इलाज 100% निःशुल्क किया जाता है।" } catch (e: Exception) { "श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट, तहसील अनूपशहर, ज़िला बुलन्दशहर, उ.प्र.) में परम पूज्य गुरुजी तेजवीर सिंह जी के मार्गदर्शन में भूत-प्रेत, ऊपरी बाधा व मानसिक कष्टों का इलाज 100% निःशुल्क किया जाता है।" },
            ashramParichayEnglish = try { cursor.getString(cursor.getColumnIndexOrThrow("ashram_parichay_english")) ?: "At Shri Balaji Kripa Dham (Gram Dungra Jaat, Tehsil Anupshahr, Bulandshahr, UP), healing is 100% free under Guruji Tejveer Singh Ji." } catch (e: Exception) { "At Shri Balaji Kripa Dham (Gram Dungra Jaat, Tehsil Anupshahr, Bulandshahr, UP), healing is 100% free under Guruji Tejveer Singh Ji." },
            ashramHistoryHindi = try { cursor.getString(cursor.getColumnIndexOrThrow("ashram_history_hindi")) ?: "परम पूज्य गुरुजी को श्री बालाजी महाराज व भैरव बाबा का साक्षात आशीर्वाद प्राप्त है।" } catch (e: Exception) { "परम पूज्य गुरुजी को श्री बालाजी महाराज व भैरव बाबा का साक्षात आशीर्वाद प्राप्त है।" },
            ashramRulesHindi = try { cursor.getString(cursor.getColumnIndexOrThrow("ashram_rules_hindi")) ?: "1. प्रत्येक रविवार प्रातःकाल से दरबार प्रारंभ होता है।\n2. टोकन केवल आश्रम परिसर (200m परिधि) में भौतिक रूप से उपस्थित होने पर ही मिलेगा।\n3. एक मोबाइल से 1 ही टोकन बनेगा।" } catch (e: Exception) { "1. प्रत्येक रविवार प्रातःकाल से दरबार प्रारंभ होता है।\n2. टोकन केवल आश्रम परिसर (200m परिधि) में भौतिक रूप से उपस्थित होने पर ही मिलेगा।\n3. एक मोबाइल से 1 ही टोकन बनेगा।" },
            tokenVoicePreset = try { cursor.getString(cursor.getColumnIndexOrThrow("token_voice_preset")) ?: "GURU_CALM" } catch (e: Exception) { "GURU_CALM" },
            bannerPhotoUri = try { cursor.getString(cursor.getColumnIndexOrThrow("banner_photo_uri")) ?: "" } catch (e: Exception) { "" },
            isBannerVisible = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_banner_visible")) == 1 } catch (e: Exception) { true },
            bannerTitle = try { cursor.getString(cursor.getColumnIndexOrThrow("banner_title")) ?: "🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट" } catch (e: Exception) { "🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट" },
            bannerSubtitle = try { cursor.getString(cursor.getColumnIndexOrThrow("banner_subtitle")) ?: "परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार" } catch (e: Exception) { "परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार" },
            bannerActionUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("banner_action_url")) ?: "" } catch (e: Exception) { "" },
            isAdsEnabled = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_ads_enabled")) == 1 } catch (e: Exception) { false },
            adType = try { cursor.getString(cursor.getColumnIndexOrThrow("ad_type")) ?: "CUSTOM" } catch (e: Exception) { "CUSTOM" },
            adBannerPhotoUri = try { cursor.getString(cursor.getColumnIndexOrThrow("ad_banner_photo_uri")) ?: "" } catch (e: Exception) { "" },
            adBannerTitle = try { cursor.getString(cursor.getColumnIndexOrThrow("ad_banner_title")) ?: "आश्रम सेवा व गौशाला सहयोग" } catch (e: Exception) { "आश्रम सेवा व गौशाला सहयोग" },
            adBannerDescription = try { cursor.getString(cursor.getColumnIndexOrThrow("ad_banner_description")) ?: "धर्मार्थ सेवा, लंगर व गौशाला में सहयोग करें।" } catch (e: Exception) { "धर्मार्थ सेवा, लंगर व गौशाला में सहयोग करें।" },
            adTargetUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("ad_target_url")) ?: "" } catch (e: Exception) { "" },
            adPlacement = try { cursor.getString(cursor.getColumnIndexOrThrow("ad_placement")) ?: "HOME_BOTTOM" } catch (e: Exception) { "HOME_BOTTOM" },
            isDarbarLiveNow = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_darbar_live_now")) == 1 } catch (e: Exception) { false },
            liveStreamTitle = try { cursor.getString(cursor.getColumnIndexOrThrow("live_stream_title")) ?: "श्री बालाजी कृपा धाम दिव्य दरबार लाइव" } catch (e: Exception) { "श्री बालाजी कृपा धाम दिव्य दरबार लाइव" },
            liveStreamUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("live_stream_url")) ?: "" } catch (e: Exception) { "" },
            youtubeLiveUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("youtube_live_url")) ?: "" } catch (e: Exception) { "" },
            facebookLiveUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("facebook_live_url")) ?: "" } catch (e: Exception) { "" },
            // Website CMS Dynamic Fields
            websiteTopBarText = try { cursor.getString(cursor.getColumnIndexOrThrow("top_bar_text")) ?: "🚩 ॐ श्री हनुमते नमः | परम पूज्य गुरुजी तेजवीर सिंह जी | श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, बुलन्दशहर 🚩" } catch (e: Exception) { "🚩 ॐ श्री हनुमते नमः | परम पूज्य गुरुजी तेजवीर सिंह जी | श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, बुलन्दशहर 🚩" },
            websiteGurujiTitle = try { cursor.getString(cursor.getColumnIndexOrThrow("guruji_title")) ?: "परम पूज्य गुरुजी तेजवीर सिंह जी" } catch (e: Exception) { "परम पूज्य गुरुजी तेजवीर सिंह जी" },
            websiteGurujiBio = try { cursor.getString(cursor.getColumnIndexOrThrow("guruji_bio")) ?: "अध्यात्म, मानव सेवा एवं बालाजी महाराज की असीम कृपा के संवाहक" } catch (e: Exception) { "अध्यात्म, मानव सेवा एवं बालाजी महाराज की असीम कृपा के संवाहक" },
            websiteTokenRuleNotice = try { cursor.getString(cursor.getColumnIndexOrThrow("token_rules_notice")) ?: "आश्रम की निष्पक्षता, पारदर्शी कतार, GPS लोकेशन एवं AI बायोमेट्रिक सुरक्षा नियमों के अनुसार टोकन पंजीकरण केवल और केवल आधिकारिक मोबाइल ऐप से ही संभव है। वेबसाइट पर कोई टोकन जनरेशन फॉर्म नहीं है। टोकन प्राप्त करने के लिए कृपया ऊपर दिए गए बटन से मोबाइल ऐप इंस्टॉल करें।" } catch (e: Exception) { "आश्रम की निष्पक्षता, पारदर्शी कतार, GPS लोकेशन एवं AI बायोमेट्रिक सुरक्षा नियमों के अनुसार टोकन पंजीकरण केवल और केवल आधिकारिक मोबाइल ऐप से ही संभव है। वेबसाइट पर कोई टोकन जनरेशन फॉर्म नहीं है। टोकन प्राप्त करने के लिए कृपया ऊपर दिए गए बटन से मोबाइल ऐप इंस्टॉल करें।" },
            websiteAartiMangala = try { cursor.getString(cursor.getColumnIndexOrThrow("aarti_mangala_time")) ?: "प्रातः 05:30 बजे" } catch (e: Exception) { "प्रातः 05:30 बजे" },
            websiteAartiBalbhog = try { cursor.getString(cursor.getColumnIndexOrThrow("aarti_balbhog_time")) ?: "प्रातः 08:00 बजे" } catch (e: Exception) { "प्रातः 08:00 बजे" },
            websiteAartiSandhya = try { cursor.getString(cursor.getColumnIndexOrThrow("aarti_sandhya_time")) ?: "सायं 07:00 बजे" } catch (e: Exception) { "सायं 07:00 बजे" },
            websiteAartiShayan = try { cursor.getString(cursor.getColumnIndexOrThrow("aarti_shayan_time")) ?: "रात्रि 09:00 बजे" } catch (e: Exception) { "रात्रि 09:00 बजे" },
            websiteBankName = try { cursor.getString(cursor.getColumnIndexOrThrow("bank_name")) ?: "पंजाब नेशनल बैंक (PNB)" } catch (e: Exception) { "पंजाब नेशनल बैंक (PNB)" },
            websiteAccountHolder = try { cursor.getString(cursor.getColumnIndexOrThrow("bank_account_holder")) ?: "श्री बालाजी कृपा धाम सेवा ट्रस्ट" } catch (e: Exception) { "श्री बालाजी कृपा धाम सेवा ट्रस्ट" },
            websiteAccountNumber = try { cursor.getString(cursor.getColumnIndexOrThrow("bank_account_number")) ?: "" } catch (e: Exception) { "" },
            websiteBankIfsc = try { cursor.getString(cursor.getColumnIndexOrThrow("bank_ifsc")) ?: "" } catch (e: Exception) { "" },
            websiteBankBranch = try { cursor.getString(cursor.getColumnIndexOrThrow("bank_branch")) ?: "अनूपशहर, बुलन्दशहर" } catch (e: Exception) { "अनूपशहर, बुलन्दशहर" },
            websiteAshramAddress = try { cursor.getString(cursor.getColumnIndexOrThrow("ashram_address")) ?: "श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर, उत्तर प्रदेश - 202394" } catch (e: Exception) { "श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर, उत्तर प्रदेश - 202394" },
            websiteAshramDirections = try { cursor.getString(cursor.getColumnIndexOrThrow("ashram_directions")) ?: "" } catch (e: Exception) { "" },
            websiteContactEmail = try { cursor.getString(cursor.getColumnIndexOrThrow("contact_email")) ?: "shribalajikripadham@gmail.com" } catch (e: Exception) { "shribalajikripadham@gmail.com" },
            websiteInstagramUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("instagram_url")) ?: "https://www.instagram.com/shribalajikripadham" } catch (e: Exception) { "https://www.instagram.com/shribalajikripadham" },
            websiteWhatsappChannelUrl = try { val v = cursor.getString(cursor.getColumnIndexOrThrow("whatsapp_channel_url")); if (v.isNullOrBlank() || v.contains("/invite")) "https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0" else v } catch (e: Exception) { "https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0" },
            websiteFooterTitle = try { cursor.getString(cursor.getColumnIndexOrThrow("footer_title")) ?: "श्री बालाजी कृपा धाम" } catch (e: Exception) { "श्री बालाजी कृपा धाम" },
            websiteFooterDedication = try { cursor.getString(cursor.getColumnIndexOrThrow("footer_dedication")) ?: "सर्वस्व श्री रामभक्त वीर हनुमान जी महाराज के पावन चरणों में समर्पित।" } catch (e: Exception) { "सर्वस्व श्री रामभक्त वीर हनुमान जी महाराज के पावन चरणों में समर्पित।" },
            websiteFooterCopyright = try { cursor.getString(cursor.getColumnIndexOrThrow("footer_copyright")) ?: "© 2026 श्री बालाजी कृपा धाम सेवा ट्रस्ट। सर्वाधिकार सुरक्षित।" } catch (e: Exception) { "© 2026 श्री बालाजी कृपा धाम सेवा ट्रस्ट। सर्वाधिकार सुरक्षित।" },
            isTuesdayDarbarEnabled = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_tuesday_darbar_enabled")) == 1 } catch (e: Exception) { false },
            tuesdayDarbarName = try { cursor.getString(cursor.getColumnIndexOrThrow("tuesday_darbar_name")) ?: "श्री बालाजी कृपा धाम (बुलन्दशहर दरबार)" } catch (e: Exception) { "श्री बालाजी कृपा धाम (बुलन्दशहर दरबार)" },
            tuesdayDarbarAddress = try { cursor.getString(cursor.getColumnIndexOrThrow("tuesday_darbar_address")) ?: "बुलन्दशहर, उत्तर प्रदेश" } catch (e: Exception) { "बुलन्दशहर, उत्तर प्रदेश" },
            tuesdayLatitude = try { cursor.getDouble(cursor.getColumnIndexOrThrow("tuesday_latitude")) } catch (e: Exception) { 28.4069 },
            tuesdayLongitude = try { cursor.getDouble(cursor.getColumnIndexOrThrow("tuesday_longitude")) } catch (e: Exception) { 77.8498 },
            tuesdayAllowedRadiusMeters = try { cursor.getDouble(cursor.getColumnIndexOrThrow("tuesday_allowed_radius_meters")) } catch (e: Exception) { 200.0 },
            tuesdayOutstationMinDistanceKm = try { cursor.getDouble(cursor.getColumnIndexOrThrow("tuesday_outstation_min_distance_km")) } catch (e: Exception) { 30.0 },
            tuesdayDarbarTimings = try { cursor.getString(cursor.getColumnIndexOrThrow("tuesday_darbar_timings")) ?: "प्रत्येक मंगलवार प्रातः 8:00 बजे से (Every Tuesday from 8:00 AM)" } catch (e: Exception) { "प्रत्येक मंगलवार प्रातः 8:00 बजे से (Every Tuesday from 8:00 AM)" },
            tuesdayTokenServiceMode = try { cursor.getString(cursor.getColumnIndexOrThrow("tuesday_token_service_mode")) ?: "AUTO_TUESDAY" } catch (e: Exception) { "AUTO_TUESDAY" },
            tuesdayScheduledOpenTimestamp = try { cursor.getLong(cursor.getColumnIndexOrThrow("tuesday_scheduled_open_timestamp")) } catch (e: Exception) { 0L },
            tuesdayDarbarDate = try { cursor.getString(cursor.getColumnIndexOrThrow("tuesday_darbar_date")) ?: "" } catch (e: Exception) { "" },
            tuesdayCurrentServingToken = try { cursor.getInt(cursor.getColumnIndexOrThrow("tuesday_current_serving_token")) } catch (e: Exception) { 0 },
            tuesdayRunningTokenNumber = try { cursor.getInt(cursor.getColumnIndexOrThrow("tuesday_running_token_number")).coerceAtLeast(1) } catch (e: Exception) { 1 },
            tuesdayTokenNotice = try { cursor.getString(cursor.getColumnIndexOrThrow("tuesday_token_notice")) ?: "बुलन्दशहर मंगलवार दरबार: केवल टोकन प्रणाली मान्य।" } catch (e: Exception) { "बुलन्दशहर मंगलवार दरबार: केवल टोकन प्रणाली मान्य।" },
            havanEstimatedCost = try { cursor.getInt(cursor.getColumnIndexOrThrow("havan_estimated_cost")).let { if (it > 0) it else 14000 } } catch (e: Exception) { 14000 },
            havanRulesNotice = try { cursor.getString(cursor.getColumnIndexOrThrow("havan_rules_notice")) ?: "हवन अनुष्ठान का अनुमानित खर्च लगभग ₹14,000 होता है। गाड़ी का आने-जाने का सम्पूर्ण किराया यजमान (भगत) को स्वयं वहन करना होगा।" } catch (e: Exception) { "हवन अनुष्ठान का अनुमानित खर्च लगभग ₹14,000 होता है। गाड़ी का आने-जाने का सम्पूर्ण किराया यजमान (भगत) को स्वयं वहन करना होगा।" }
        )
    }

    suspend fun getSettings(): AshramSettings = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ashram_settings WHERE id = 1", null)
        var settings = AshramSettings()
        val found = cursor.moveToFirst()
        if (found) {
            settings = parseSettingsCursor(cursor)
        }
        cursor.close()

        if (!found) {
            try {
                val wdb = dbHelper.writableDatabase
                val restored = com.example.shribalajikripadham.data.local.AppPermanentVault.restoreVault(appContext, wdb, force = true)
                if (restored) {
                    val c2 = wdb.rawQuery("SELECT * FROM ashram_settings WHERE id = 1", null)
                    if (c2.moveToFirst()) {
                        settings = parseSettingsCursor(c2)
                    }
                    c2.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        settings
    }

    /**
     * INDESTRUCTIBLE MULTI-LAYER SETTINGS PERSISTENCE:
     * Guarantees settings survive APK updates, app clearing, crashes, and sync operations.
     * Writes synchronously to:
     * 1. Android SharedPreferences ('sbkd_indestructible_settings')
     * 2. Internal JSON Vault ('sbkd_vault_data.json')
     * 3. External Download backup JSON
     * 4. Central Hostinger MySQL Server (live_config.php)
     * 5. GitHub repository (if PAT configured)
     */
    suspend fun persistCurrentSettingsToAllLayers() = withContext(Dispatchers.IO) {
        try {
            val fresh = getSettings()
            com.example.shribalajikripadham.data.local.AppPermanentVault.saveVault(appContext, getAllAdmins(), fresh)
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.updateFullLiveConfig(fresh, getAllSevadars(), getUiSectionConfigs())
                } catch (e: Exception) {}
                try {
                    publishCurrentSettingsToGitHub()
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateCanDevoteeViewYatraDiary(canView: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        try {
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN can_devotee_view_yatra_diary INTEGER DEFAULT 0")
        } catch (ignored: Exception) {}
        val cv = android.content.ContentValues().apply {
            put("can_devotee_view_yatra_diary", if (canView) 1 else 0)
        }
        val res = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (res) {
            persistCurrentSettingsToAllLayers()
        }
        res
    }

    suspend fun updateAshramParichayAndRules(
        parichayHindi: String,
        parichayEnglish: String,
        historyHindi: String,
        rulesHindi: String
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        try {
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN ashram_parichay_hindi TEXT")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN ashram_parichay_english TEXT")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN ashram_history_hindi TEXT")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN ashram_rules_hindi TEXT")
        } catch (ignored: Exception) {}
        val cv = android.content.ContentValues().apply {
            put("ashram_parichay_hindi", parichayHindi)
            put("ashram_parichay_english", parichayEnglish)
            put("ashram_history_hindi", historyHindi)
            put("ashram_rules_hindi", rulesHindi)
        }
        val res = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (res) {
            persistCurrentSettingsToAllLayers()
        }
        res
    }

    suspend fun updateBannerSettings(
        photoUri: String,
        isVisible: Boolean,
        title: String,
        subtitle: String,
        actionUrl: String
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        try {
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN banner_photo_uri TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN is_banner_visible INTEGER DEFAULT 1")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN banner_title TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN banner_subtitle TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN banner_action_url TEXT DEFAULT ''")
        } catch (ignored: Exception) {}
        val cv = android.content.ContentValues().apply {
            put("banner_photo_uri", photoUri)
            put("is_banner_visible", if (isVisible) 1 else 0)
            put("banner_title", title)
            put("banner_subtitle", subtitle)
            put("banner_action_url", actionUrl)
        }
        val res = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (res) {
            persistCurrentSettingsToAllLayers()
        }
        res
    }

    suspend fun updateAdsSettings(
        isAdsEnabled: Boolean,
        adType: String,
        bannerPhotoUri: String,
        title: String,
        description: String,
        targetUrl: String,
        placement: String
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        try {
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN is_ads_enabled INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN ad_type TEXT DEFAULT 'CUSTOM'")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN ad_banner_photo_uri TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN ad_banner_title TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN ad_banner_description TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN ad_target_url TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN ad_placement TEXT DEFAULT 'HOME_BOTTOM'")
        } catch (ignored: Exception) {}
        val cv = android.content.ContentValues().apply {
            put("is_ads_enabled", if (isAdsEnabled) 1 else 0)
            put("ad_type", adType)
            put("ad_banner_photo_uri", bannerPhotoUri)
            put("ad_banner_title", title)
            put("ad_banner_description", description)
            put("ad_target_url", targetUrl)
            put("ad_placement", placement)
        }
        val res = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (res) {
            persistCurrentSettingsToAllLayers()
        }
        res
    }

    suspend fun updateTokenVoicePreset(voicePreset: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        try {
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN token_voice_preset TEXT DEFAULT 'GURU_CALM'")
        } catch (ignored: Exception) {}
        val cv = android.content.ContentValues().apply {
            put("token_voice_preset", voicePreset)
        }
        val res = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (res) {
            persistCurrentSettingsToAllLayers()
        }
        res
    }

    suspend fun updateTokenServiceMode(mode: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        try {
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN token_service_mode TEXT DEFAULT 'AUTO_SUNDAY'")
        } catch (ignored: Exception) {}
        val cv = android.content.ContentValues().apply {
            put("token_service_mode", mode)
            if (mode.equals("FORCE_OPEN", ignoreCase = true)) {
                put("is_token_service_enabled", 1)
                put("is_darbar_active", 1)
            } else if (mode.equals("FORCE_CLOSED", ignoreCase = true)) {
                put("is_token_service_enabled", 0)
            } else {
                // AUTO_SUNDAY
                put("is_token_service_enabled", 1)
            }
        }
        val res = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (res) {
            persistCurrentSettingsToAllLayers()
        }
        res
    }

    suspend fun updateAshramDetails(
        ashramName: String,
        gurujiName: String,
        address: String,
        contactPhone: String,
        darbarTimings: String,
        freeDisclaimer: String,
        emergencyNotice: String
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("ashram_name", ashramName)
            put("guruji_name", gurujiName)
            put("address", address)
            put("contact_phone", contactPhone)
            put("darbar_timings", darbarTimings)
            put("free_disclaimer", freeDisclaimer)
            put("emergency_notice", emergencyNotice)
        }
        val res = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (res) {
            persistCurrentSettingsToAllLayers()
        }
        res
    }

    suspend fun updateContactPhone(contactPhone: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("contact_phone", contactPhone.trim())
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) persistCurrentSettingsToAllLayers()
        ok
    }

    suspend fun updateGurujiPhoto(photoUri: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("guruji_photo_uri", photoUri.trim())
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) persistCurrentSettingsToAllLayers()
        ok
    }

    suspend fun updateActiveUiLayout(layoutKey: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("active_ui_layout", layoutKey.trim())
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) persistCurrentSettingsToAllLayers()
        ok
    }

    private fun safeUpdateAshramSettings(db: SQLiteDatabase, cv: ContentValues): Boolean {
        com.example.shribalajikripadham.data.local.DatabaseHelper.autoMigrateSettingsColumns(db)
        return try {
            db.update("ashram_settings", cv, "id = 1", null) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            // Force schema self-healing on any missing column and retry
            com.example.shribalajikripadham.data.local.DatabaseHelper.autoMigrateSettingsColumns(db)
            try {
                db.update("ashram_settings", cv, "id = 1", null) > 0
            } catch (e2: Exception) {
                e2.printStackTrace()
                false
            }
        }
    }

    suspend fun updateSettings(s: AshramSettings): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("running_token_number", s.runningTokenNumber)
            put("is_token_service_enabled", if (s.isTokenServiceEnabled) 1 else 0)
            put("token_service_mode", s.tokenServiceMode)
            put("is_bus_booking_live", if (s.isBusBookingLive) 1 else 0)
            put("is_dharamshala_live", if (s.isDharamshalaLive) 1 else 0)
            put("emergency_notice", s.emergencyNoticeText)
            put("is_emergency_notice_visible", if (s.isEmergencyNoticeVisible) 1 else 0)
            if (s.gurujiPhotoUri.isNotBlank()) put("guruji_photo_uri", s.gurujiPhotoUri)
            if (s.bannerTitle.isNotBlank()) put("banner_title", s.bannerTitle)
            if (s.bannerSubtitle.isNotBlank()) put("banner_subtitle", s.bannerSubtitle)
            if (s.darbarTimings.isNotBlank()) put("darbar_timings", s.darbarTimings)
            put("is_darbar_active", if (s.isDarbarActive) 1 else 0)
            put("is_darbar_live_now", if (s.isDarbarLiveNow) 1 else 0)
            put("badi_arzi_rate", s.badiArziRate)
            put("chhoti_arzi_rate", s.chhotiArziRate)
            put("bus_seat_fare_amount", s.busSeatFareAmount)
            put("contact_phone", s.contactPhone)
            put("whatsapp_number", s.whatsappNumber)
            put("ashram_upi_id", s.ashramUpiId)
            put("ashram_upi_name", s.ashramUpiName)
            put("top_bar_text", s.websiteTopBarText)
            put("guruji_title", s.websiteGurujiTitle)
            put("guruji_bio", s.websiteGurujiBio)
            put("token_rules_notice", s.websiteTokenRuleNotice)
            put("aarti_mangala_time", s.websiteAartiMangala)
            put("aarti_balbhog_time", s.websiteAartiBalbhog)
            put("aarti_sandhya_time", s.websiteAartiSandhya)
            put("aarti_shayan_time", s.websiteAartiShayan)
            put("bank_name", s.websiteBankName)
            put("bank_account_holder", s.websiteAccountHolder)
            put("bank_account_number", s.websiteAccountNumber)
            put("bank_ifsc", s.websiteBankIfsc)
            put("bank_branch", s.websiteBankBranch)
            put("ashram_address", s.websiteAshramAddress)
            put("ashram_directions", s.websiteAshramDirections)
            put("contact_email", s.websiteContactEmail)
            put("instagram_url", s.websiteInstagramUrl)
            put("whatsapp_channel_url", s.websiteWhatsappChannelUrl)
            put("footer_title", s.websiteFooterTitle)
            put("footer_dedication", s.websiteFooterDedication)
            put("footer_copyright", s.websiteFooterCopyright)
            put("is_tuesday_darbar_enabled", if (s.isTuesdayDarbarEnabled) 1 else 0)
            put("tuesday_darbar_name", s.tuesdayDarbarName)
            put("tuesday_darbar_address", s.tuesdayDarbarAddress)
            put("tuesday_latitude", s.tuesdayLatitude)
            put("tuesday_longitude", s.tuesdayLongitude)
            put("tuesday_allowed_radius_meters", s.tuesdayAllowedRadiusMeters)
            put("tuesday_outstation_min_distance_km", s.tuesdayOutstationMinDistanceKm)
            put("tuesday_darbar_timings", s.tuesdayDarbarTimings)
            put("tuesday_token_service_mode", s.tuesdayTokenServiceMode)
            put("tuesday_scheduled_open_timestamp", s.tuesdayScheduledOpenTimestamp)
            put("tuesday_darbar_date", s.tuesdayDarbarDate)
            put("tuesday_current_serving_token", s.tuesdayCurrentServingToken)
            put("tuesday_running_token_number", s.tuesdayRunningTokenNumber)
            put("tuesday_token_notice", s.tuesdayTokenNotice)
            put("havan_estimated_cost", s.havanEstimatedCost)
            put("havan_rules_notice", s.havanRulesNotice)
        }
        val ok = safeUpdateAshramSettings(db, cv)
        if (ok) persistCurrentSettingsToAllLayers()
        ok
    }

    suspend fun updateTuesdayDarbarSettings(
        isEnabled: Boolean,
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
        allowedRadiusMeters: Double,
        outstationMinDistanceKm: Double,
        timings: String,
        tokenServiceMode: String,
        tokenNotice: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_tuesday_darbar_enabled", if (isEnabled) 1 else 0)
            put("tuesday_darbar_name", name.trim())
            put("tuesday_darbar_address", address.trim())
            put("tuesday_latitude", latitude)
            put("tuesday_longitude", longitude)
            put("tuesday_allowed_radius_meters", allowedRadiusMeters.coerceIn(10.0, 50000.0))
            put("tuesday_outstation_min_distance_km", outstationMinDistanceKm.coerceIn(1.0, 500.0))
            put("tuesday_darbar_timings", timings.trim())
            put("tuesday_token_service_mode", tokenServiceMode.trim())
            put("tuesday_token_notice", tokenNotice.trim())
        }
        val ok = safeUpdateAshramSettings(db, cv)
        if (ok) {
            persistCurrentSettingsToAllLayers()
        }
        ok
    }

    suspend fun updateTuesdayCurrentServingToken(tokenNumber: Int): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("tuesday_current_serving_token", tokenNumber.coerceAtLeast(0))
            put("tuesday_running_token_number", tokenNumber.coerceAtLeast(0))
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) {
            persistCurrentSettingsToAllLayers()
        }
        ok
    }


    suspend fun updateActiveUiLayoutEnforced(layoutKey: String, isEnforced: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("active_ui_layout", layoutKey.trim())
            put("is_ui_layout_enforced", if (isEnforced) 1 else 0)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) persistCurrentSettingsToAllLayers()
        ok
    }

    suspend fun updateMaxDailyTokens(maxTokens: Int): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("max_daily_tokens", maxTokens)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) persistCurrentSettingsToAllLayers()
        ok
    }

    suspend fun updateCloudSyncSettings(url: String, isEnabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("cloud_sync_url", url.trim())
            put("is_cloud_sync_enabled", if (isEnabled) 1 else 0)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) persistCurrentSettingsToAllLayers()
        ok
    }

    suspend fun updateEmergencyNotice(emergencyNotice: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("emergency_notice", emergencyNotice)
        }
        val res = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (res) {
            persistCurrentSettingsToAllLayers()
        }
        res
    }

    suspend fun updateSundayTokenBanner(
        title: String,
        text: String,
        notice: String
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("sunday_token_banner_title", title.trim())
            put("sunday_token_banner_text", text.trim())
            put("sunday_token_custom_notice", notice.trim())
        }
        val res = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (res) {
            persistCurrentSettingsToAllLayers()
        }
        res
    }

    suspend fun updateAshramLocation(
        lat: Double,
        long: Double,
        radiusMeters: Double,
        isGeofenceEnforced: Boolean,
        isOutstationAdvanceAllowed: Boolean = true,
        outstationMinDistanceKm: Double = 30.0
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val clampedRadius = radiusMeters.coerceIn(10.0, 50000.0)
        val clampedOutstationKm = outstationMinDistanceKm.coerceIn(1.0, 500.0)
        val cv = ContentValues().apply {
            put("latitude", lat)
            put("longitude", long)
            put("allowed_radius_meters", clampedRadius)
            put("is_geofence_enforced", if (isGeofenceEnforced) 1 else 0)
            put("is_outstation_advance_allowed", if (isOutstationAdvanceAllowed) 1 else 0)
            put("outstation_min_distance_km", clampedOutstationKm)
        }
        val res = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (res) {
            persistCurrentSettingsToAllLayers()
        }
        res
    }

    suspend fun syncCurrentLiveSettingsFromGitHub(): Boolean = withContext(Dispatchers.IO) {
        val (cOk, _) = syncLiveConfigFromGitHub()
        syncLiveTokensFromCloud()
        syncLiveParchasFromGitHub()
        syncLivePaymentsFromGitHub()
        syncLiveBusSeatsFromGitHub()
        cOk
    }

    suspend fun updateAshramLocation(
        requestingAdmin: Admin,
        newLat: Double,
        newLong: Double,
        newRadius: Double,
        isGeofenceEnforced: Boolean,
        isOutstationAdvanceAllowed: Boolean = true,
        outstationMinDistanceKm: Double = 30.0
    ): Boolean = withContext(Dispatchers.IO) {
        if (!requestingAdmin.canChangeLocation && requestingAdmin.role != AdminRole.SUPER_ADMIN) {
            throw SecurityException("Unauthorized: Admin lacks 'can_change_location' permission.")
        }
        val db = dbHelper.writableDatabase
        val clampedRadius = newRadius.coerceIn(10.0, 50000.0)
        val clampedOutstationKm = outstationMinDistanceKm.coerceIn(1.0, 500.0)
        val cv = ContentValues().apply {
            put("latitude", newLat)
            put("longitude", newLong)
            put("allowed_radius_meters", clampedRadius)
            put("is_geofence_enforced", if (isGeofenceEnforced) 1 else 0)
            put("is_outstation_advance_allowed", if (isOutstationAdvanceAllowed) 1 else 0)
            put("outstation_min_distance_km", clampedOutstationKm)
        }
        val updated = db.update("ashram_settings", cv, "id = 1", null) > 0

        // Broadcast to cloud and indestructible local layers
        if (updated) {
            persistCurrentSettingsToAllLayers()
        }
        updated
    }

    suspend fun updateScheduledTokenOpenTime(timestamp: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("scheduled_token_open_timestamp", timestamp)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) persistCurrentSettingsToAllLayers()
        ok
    }

    suspend fun updateMasterVisibilityToggles(
        isTokenEnabled: Boolean,
        isYatraEnabled: Boolean,
        isLiveCounterVisible: Boolean,
        isEventsVisible: Boolean,
        isAartiTimingsVisible: Boolean,
        isGurujiInfoVisible: Boolean,
        isEmergencyNoticeVisible: Boolean,
        tokenServiceMode: String = "AUTO_SUNDAY"
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_token_service_enabled", if (isTokenEnabled) 1 else 0)
            put("token_service_mode", tokenServiceMode)
            put("is_yatra_service_enabled", if (isYatraEnabled) 1 else 0)
            put("is_live_counter_visible", if (isLiveCounterVisible) 1 else 0)
            put("is_events_visible", if (isEventsVisible) 1 else 0)
            put("is_aarti_timings_visible", if (isAartiTimingsVisible) 1 else 0)
            put("is_guruji_info_visible", if (isGurujiInfoVisible) 1 else 0)
            put("is_emergency_notice_visible", if (isEmergencyNoticeVisible) 1 else 0)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) persistCurrentSettingsToAllLayers()
        ok
    }

    suspend fun updateSocialLinks(
        whatsappGroupUrl: String,
        whatsappNumber: String,
        youtubeUrl: String,
        facebookUrl: String,
        instagramUrl: String,
        appShareUrl: String
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("whatsapp_group_url", whatsappGroupUrl.trim())
            put("whatsapp_number", whatsappNumber.trim())
            put("youtube_channel_url", youtubeUrl.trim())
            put("facebook_page_url", facebookUrl.trim())
            put("instagram_url", instagramUrl.trim())
            put("app_share_url", appShareUrl.trim())
        }
        val res = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (res) {
            persistCurrentSettingsToAllLayers()
        }
        res
    }

    suspend fun updateCurrentTheme(themeId: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("current_theme_id", themeId)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) persistCurrentSettingsToAllLayers()
        ok
    }

    suspend fun getAllActiveSevadars(): List<Admin> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM admins WHERE role = 'SEVADAR' AND is_active = 1 ORDER BY id ASC", null)
        val list = mutableListOf<Admin>()
        while (cursor.moveToNext()) {
            list.add(parseAdminCursor(cursor))
        }
        cursor.close()
        list
    }

    suspend fun updateServiceToggles(
        isTokenEnabled: Boolean,
        isYatraEnabled: Boolean,
        isLiveCounterVisible: Boolean,
        isEventsVisible: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        updateMasterVisibilityToggles(
            isTokenEnabled = isTokenEnabled,
            isYatraEnabled = isYatraEnabled,
            isLiveCounterVisible = isLiveCounterVisible,
            isEventsVisible = isEventsVisible,
            isAartiTimingsVisible = true,
            isGurujiInfoVisible = true,
            isEmergencyNoticeVisible = true
        )
    }

    suspend fun updateAppUpdateConfig(
        latestVersionCode: Int,
        latestVersionName: String,
        updateNotes: String,
        apkDownloadUrl: String,
        isForceUpdate: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("latest_version_code", latestVersionCode)
            put("latest_version_name", latestVersionName)
            put("update_notes", updateNotes)
            put("apk_download_url", apkDownloadUrl)
            put("is_force_update", if (isForceUpdate) 1 else 0)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) persistCurrentSettingsToAllLayers()
        ok
    }

    suspend fun updateRunningTokenNumber(tokenNum: Int): Boolean = withContext(Dispatchers.IO) {
        val safeNum = tokenNum.coerceAtLeast(1)
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("running_token_number", safeNum)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    val s = getSettings()
                    com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.updateLiveConfig(
                        radiusMeters = s.allowedRadiusMeters,
                        isGeofenceEnforced = s.isGeofenceEnforced,
                        isOutstationAllowed = s.isOutstationAdvanceAllowed,
                        outstationKm = s.outstationMinDistanceKm,
                        currentServingToken = tokenNum,
                        lat = s.latitude,
                        long = s.longitude
                    )
                } catch (e: Exception) {}
                persistCurrentSettingsToAllLayers()
            }
        }
        ok
    }

    // --- Tokens & Devices ---
    suspend fun checkDeviceRegisteredToday(deviceId: String, targetDarbarDate: String = ""): Token? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val today = DatabaseHelper.getTodayDateString()
        val checkDate = if (targetDarbarDate.isNotBlank()) targetDarbarDate else today
        val cursor = db.rawQuery(
            "SELECT * FROM tokens WHERE device_id = ? AND (darbar_date = ? OR darbar_date = ? OR darbar_date >= ?) ORDER BY id DESC LIMIT 1",
            arrayOf(deviceId, checkDate, today, today)
        )
        var token: Token? = null
        if (cursor.moveToFirst()) {
            token = Token(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                tokenNumber = cursor.getInt(cursor.getColumnIndexOrThrow("token_number")),
                darbarDate = cursor.getString(cursor.getColumnIndexOrThrow("darbar_date")),
                patientName = cursor.getString(cursor.getColumnIndexOrThrow("patient_name")),
                phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone_number")),
                city = try { cursor.getString(cursor.getColumnIndexOrThrow("city")) } catch (e: Exception) { "डूँगरा जाट (स्थानीय)" },
                deviceId = cursor.getString(cursor.getColumnIndexOrThrow("device_id")),
                latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                status = TokenStatus.fromRaw(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
                registeredBy = cursor.getString(cursor.getColumnIndexOrThrow("registered_by")),
                photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri")) ?: "",
                isDarshanCompleted = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_darshan_completed")) == 1 } catch (e: Exception) { false },
                darshanCompletedAt = try { cursor.getLong(cursor.getColumnIndexOrThrow("darshan_completed_at")) } catch (e: Exception) { 0L },
                originAddress = try { cursor.getString(cursor.getColumnIndexOrThrow("origin_address")) } catch (e: Exception) { "" }.ifEmpty { cursor.getString(cursor.getColumnIndexOrThrow("city")) },
                destinationAddress = try { cursor.getString(cursor.getColumnIndexOrThrow("destination_address")) } catch (e: Exception) { "श्री बालाजी कृपा धाम, डुंगरा जाट" },
                distanceKm = try { cursor.getFloat(cursor.getColumnIndexOrThrow("distance_km")) } catch (e: Exception) { -1f },
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
            )
        }
        cursor.close()

        // 🛡️ ANTI-BYPASS: If app was uninstalled & reinstalled or Clear Data was performed, local SQLite is empty!
        // 1. Check central server (Highest Authority):
        if (token == null) {
            try {
                var serverToken = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.checkDeviceRegisteredOnServer(deviceId, checkDate)
                if (serverToken == null && checkDate != today) {
                    serverToken = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.checkDeviceRegisteredOnServer(deviceId, today)
                }
                if (serverToken != null) {
                    val cv = ContentValues().apply {
                        put("token_number", serverToken.tokenNumber)
                        put("darbar_date", serverToken.darbarDate)
                        put("patient_name", serverToken.patientName)
                        put("phone_number", serverToken.phoneNumber)
                        put("city", serverToken.city)
                        put("device_id", serverToken.deviceId)
                        put("latitude", serverToken.latitude)
                        put("longitude", serverToken.longitude)
                        put("status", serverToken.status.name)
                        put("registered_by", serverToken.registeredBy)
                        put("photo_uri", serverToken.photoUri)
                        put("is_darshan_completed", if (serverToken.isDarshanCompleted) 1 else 0)
                        put("darshan_completed_at", serverToken.darshanCompletedAt)
                        put("origin_address", serverToken.originAddress)
                        put("destination_address", serverToken.destinationAddress)
                        put("distance_km", serverToken.distanceKm)
                        put("created_at", serverToken.createdAt)
                    }
                    val wDb = dbHelper.writableDatabase
                    wDb.insertWithOnConflict("tokens", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                    val devCv = ContentValues().apply {
                        put("device_id", serverToken.deviceId)
                        put("darbar_date", serverToken.darbarDate)
                        put("token_number", serverToken.tokenNumber)
                        put("registered_at", serverToken.createdAt)
                    }
                    wDb.insertWithOnConflict("device_registrations", null, devCv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                    token = serverToken
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Check persistent hardware receipt stored in public device storage:
        if (token == null) {
            val persistent = com.example.shribalajikripadham.hardware.PersistentTokenReceiptHelper.readPersistentReceipt(deviceId, checkDate)
                ?: com.example.shribalajikripadham.hardware.PersistentTokenReceiptHelper.readPersistentReceipt(deviceId, today)
            if (persistent != null) {
                val cv = ContentValues().apply {
                    put("token_number", persistent.tokenNumber)
                    put("darbar_date", persistent.darbarDate)
                    put("patient_name", persistent.patientName)
                    put("phone_number", persistent.phoneNumber)
                    put("city", persistent.city)
                    put("device_id", persistent.deviceId)
                    put("latitude", persistent.latitude)
                    put("longitude", persistent.longitude)
                    put("status", persistent.status.name)
                    put("registered_by", persistent.registeredBy)
                    put("photo_uri", persistent.photoUri)
                    put("is_darshan_completed", if (persistent.isDarshanCompleted) 1 else 0)
                    put("darshan_completed_at", persistent.darshanCompletedAt)
                    put("origin_address", persistent.originAddress)
                    put("destination_address", persistent.destinationAddress)
                    put("distance_km", persistent.distanceKm)
                    put("created_at", persistent.createdAt)
                }
                val wDb = dbHelper.writableDatabase
                wDb.insertWithOnConflict("tokens", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                token = persistent
            }
        }

        token
    }

    suspend fun registerToken(
        patientName: String,
        phoneNumber: String,
        deviceId: String,
        latitude: Double,
        longitude: Double,
        city: String = "डूँगरा जाट (स्थानीय)",
        registeredBy: String = "SELF",
        photoUri: String = "",
        isMockLocation: Boolean = false,
        locationAccuracy: Float = 10.0f,
        originAddress: String = city,
        destinationAddress: String = "श्री बालाजी कृपा धाम, डुंगरा जाट",
        distanceKm: Float = -1f,
        bypassGeofence: Boolean = false,
        customTokenNumber: Int? = null,
        darbarVenue: String = "DUNGRA_JAAT",
        darbarDate: String = ""
    ): Token = withContext(Dispatchers.IO) {
        val today = DatabaseHelper.getTodayDateString()
        val db = dbHelper.writableDatabase

        val isSuperAdmin = registeredBy.startsWith("SUPER_ADMIN")
        val isAdminDesk = registeredBy.startsWith("ADMIN") || registeredBy == "SEVADAR_DESK"
        val isDevoteeRequest = !isSuperAdmin && !isAdminDesk

        // Geofence & Anti-Spoof bypass: Super Admin ALWAYS bypasses; Admins bypass IF bypassGeofence is granted; Emergency master bypass from Antigravity/Server
        val shouldBypassGeofence = isSuperAdmin || (isAdminDesk && bypassGeofence) || com.example.shribalajikripadham.hardware.GeofenceLocationManager.isGeofenceGloballyBypassed

        // 0. DARBAR SCHEDULE & TOKEN OPENING CHECK (Sunday Dungra Jaat vs Tuesday Bulandshahr)
        val settings = getSettings()
        val isTuesdayDarbar = darbarVenue.equals("BULANDSHAHR", ignoreCase = true)
        val targetVenueName = if (isTuesdayDarbar) settings.tuesdayDarbarName else "श्री बालाजी कृपा धाम, डुंगरा जाट"
        val targetVenueLabel = if (isTuesdayDarbar) "बुलन्दशहर दरबार" else "आश्रम"
        val targetLat = if (isTuesdayDarbar) {
            if (settings.tuesdayLatitude != 0.0) settings.tuesdayLatitude else 28.4069
        } else {
            if (settings.latitude != 0.0) settings.latitude else 28.3972915
        }
        val targetLng = if (isTuesdayDarbar) {
            if (settings.tuesdayLongitude != 0.0) settings.tuesdayLongitude else 77.8498
        } else {
            if (settings.longitude != 0.0) settings.longitude else 78.1460410
        }
        val targetRadius = if (isTuesdayDarbar) settings.tuesdayAllowedRadiusMeters else settings.allowedRadiusMeters
        val targetOutstationKm = if (isTuesdayDarbar) settings.tuesdayOutstationMinDistanceKm else settings.outstationMinDistanceKm

        val targetDarbarDate = if (darbarDate.isNotBlank()) {
            darbarDate
        } else {
            if (isTuesdayDarbar) {
                if (settings.tuesdayDarbarDate.isNotBlank()) settings.tuesdayDarbarDate else today
            } else {
                if (settings.darbarDate.isNotBlank()) settings.darbarDate else today
            }
        }

        val isScheduleOpen = if (isTuesdayDarbar) {
            val tSched = com.example.shribalajikripadham.util.TuesdayTokenScheduleHelper.evaluateSchedule(settings)
            val open = (tSched is com.example.shribalajikripadham.util.TuesdayScheduleState.Open) && settings.isTuesdayDarbarEnabled
            if (isDevoteeRequest && !open) {
                when (tSched) {
                    is com.example.shribalajikripadham.util.TuesdayScheduleState.CountdownActive -> throw java.lang.IllegalStateException(tSched.messageHindi)
                    is com.example.shribalajikripadham.util.TuesdayScheduleState.TuesdayBeforeStart -> throw java.lang.IllegalStateException(tSched.messageHindi)
                    is com.example.shribalajikripadham.util.TuesdayScheduleState.TuesdayClosedEvening -> throw java.lang.IllegalStateException(tSched.messageHindi)
                    is com.example.shribalajikripadham.util.TuesdayScheduleState.NonTuesday -> throw java.lang.IllegalStateException(tSched.messageHindi)
                    is com.example.shribalajikripadham.util.TuesdayScheduleState.ServiceDisabled -> throw java.lang.IllegalStateException(tSched.messageHindi)
                    is com.example.shribalajikripadham.util.TuesdayScheduleState.CustomScheduled -> throw java.lang.IllegalStateException(tSched.messageHindi)
                    else -> throw java.lang.IllegalStateException("मंगलवार बुलन्दशहर दरबार वर्तमान में विश्राम पर है।")
                }
            }
            open
        } else {
            val sched = com.example.shribalajikripadham.util.SundayTokenScheduleHelper.evaluateSchedule(settings)
            val isForceOpen = settings.tokenServiceMode.equals("FORCE_OPEN", ignoreCase = true)
            val open = (sched is com.example.shribalajikripadham.util.SundayScheduleState.Open) && (settings.isDarbarActive || settings.tokenServiceMode.equals("AUTO_SUNDAY", ignoreCase = true) || isForceOpen)
            if (isDevoteeRequest && !open) {
                when (sched) {
                    is com.example.shribalajikripadham.util.SundayScheduleState.CountdownActive -> throw java.lang.IllegalStateException(sched.messageHindi)
                    is com.example.shribalajikripadham.util.SundayScheduleState.SundayBeforeStart -> throw java.lang.IllegalStateException(sched.messageHindi)
                    is com.example.shribalajikripadham.util.SundayScheduleState.SundayClosedEvening -> throw java.lang.IllegalStateException(sched.messageHindi)
                    is com.example.shribalajikripadham.util.SundayScheduleState.NonSunday -> throw java.lang.IllegalStateException(sched.messageHindi)
                    is com.example.shribalajikripadham.util.SundayScheduleState.ServiceDisabled -> throw java.lang.IllegalStateException(sched.messageHindi)
                    is com.example.shribalajikripadham.util.SundayScheduleState.CustomScheduled -> throw java.lang.IllegalStateException(sched.messageHindi)
                    else -> {
                        if (!settings.isDarbarActive && !settings.tokenServiceMode.equals("AUTO_SUNDAY", ignoreCase = true) && !isForceOpen) {
                            throw java.lang.IllegalStateException("दरबार वर्तमान में विश्राम पर है। टोकन पंजीकरण बंद है।")
                        }
                    }
                }
            }
            open
        }

        if (!isSuperAdmin && !isScheduleOpen) {
            val venueMsg = if (isTuesdayDarbar) "मंगलवार बुलन्दशहर दरबार" else "रविवार दरबार"
            val timeMsg = if (isTuesdayDarbar) "मंगलवार प्रातः 8:00 बजे से सायं 5:00 बजे तक" else "रविवार प्रातः 8:30 बजे से सायं 5:00 बजे तक"
            throw IllegalStateException("⚠️ $venueMsg टोकन सेवा वर्तमान में बंद है।\n\nटोकन केवल दरबार के दिन ($timeMsg) ही बनाए जा सकते हैं। किसी भी सामान्य एडमिन अथवा भक्त द्वारा पहले से (शनिवार या समय से पहले) टोकन बनाना पूर्णतः प्रतिबंधित है। केवल सुपर एडमिन ही विशेष परिस्थिति में टोकन बना सकते हैं।")
        }

        // 1. LOCATION & GEOFENCE CHECKS (Enforced for devotees and non-exempt admins)
        if (!shouldBypassGeofence) {
            if (isMockLocation && com.example.shribalajikripadham.hardware.GeofenceLocationManager.isMockCheckGloballyEnabled) {
                throw SecurityException("Security Exception: Spoofed Location or Duplicate Device Request Denied.")
            }

            if (locationAccuracy > GeofenceLocationManager.MAX_ALLOWED_ACCURACY_METERS) {
                throw SecurityException("Security Exception: Inaccurate GPS signal (${String.format("%.1f", locationAccuracy)}m). Please stand in open area.")
            }

            if (settings.isGeofenceEnforced) {
                if (latitude == 0.0 && longitude == 0.0) {
                    throw SecurityException("कृपया GPS चालू करें और $targetVenueLabel परिसर में उपस्थित रहें।")
                }
                if (kotlin.math.abs(latitude - targetLat) < 0.000005 && kotlin.math.abs(longitude - targetLng) < 0.000005) {
                    throw SecurityException("⚠️ सुरक्षा चेतावनी: नकली लोकेशन / मैप पिन इंजेक्शन पकड़ा गया है। कृपया वास्तविक फोन जीपीएस चालू करें।")
                }
                val distance = GeofenceLocationManager.calculateDistanceMeters(
                    latitude, longitude,
                    targetLat, targetLng
                )
                val isPermitted = GeofenceLocationManager.isTokenDistancePermitted(
                    distanceMeters = distance,
                    isGeofenceEnforced = true,
                    allowedRadiusMeters = targetRadius.coerceAtLeast(10.0),
                    isOutstationAdvanceAllowed = settings.isOutstationAdvanceAllowed,
                    outstationMinDistanceKm = targetOutstationKm
                )
                if (!isPermitted) {
                    val km = String.format(java.util.Locale.US, "%.1f", distance / 1000.0)
                    val allowedM = targetRadius.toInt()
                    val radiusDesc = if (allowedM >= 1000) "${String.format(java.util.Locale.US, "%.1f", allowedM / 1000.0)} किमी" else "$allowedM मीटर"
                    val outstationKm = targetOutstationKm.toInt()
                    val msg = if (settings.isOutstationAdvanceAllowed) {
                        "⚠️ $targetVenueLabel दूरी नियम: ${outstationKm} किमी के दायरे में रहने वाले स्थानीय भक्तों हेतु टोकन पंजीकरण केवल $targetVenueLabel परिसर ($radiusDesc के भीतर) में ही मान्य है। आप अभी $targetVenueLabel से $km किमी दूर हैं। कृपया परिसर पहुँचकर ही टोकन जनरेट करें ताकि दूर से आने वाले भक्तों का अवसर न छूटे।"
                    } else {
                        "⚠️ $targetVenueLabel दूरी नियम: टोकन पंजीकरण केवल $targetVenueLabel परिसर ($radiusDesc के भीतर) में ही मान्य है। आप अभी $targetVenueLabel से $km किमी दूर हैं। कृपया परिसर में आकर टोकन जनरेट करें।"
                    }
                    throw SecurityException(msg)
                }
            }
        }

        // 2. HARDWARE-LEVEL DEVICE LOCKING (Strict 1 Device = 1 Token per Sunday for devotees)
        if (isDevoteeRequest) {
            // Check local device_registrations table
            val checkCursor = db.rawQuery(
                "SELECT token_number FROM device_registrations WHERE device_id = ? AND (darbar_date = ? OR darbar_date = ? OR darbar_date >= ?)",
                arrayOf(deviceId, today, targetDarbarDate, today)
            )
            if (checkCursor.moveToFirst()) {
                checkCursor.close()
                throw SecurityException("Security Exception: Spoofed Location or Duplicate Device Request Denied.")
            }
            checkCursor.close()

            // Check local tokens table for device or phone number duplicate
            val cleanPhone10 = phoneNumber.trim().filter { it.isDigit() }.takeLast(10)
            val tokCursor = db.rawQuery(
                "SELECT token_number FROM tokens WHERE (device_id = ? OR (length(?) = 10 AND phone_number LIKE ?)) AND (darbar_date = ? OR darbar_date = ? OR darbar_date >= ?)",
                arrayOf(deviceId, cleanPhone10, "%$cleanPhone10", today, targetDarbarDate, today)
            )
            if (tokCursor.moveToFirst()) {
                tokCursor.close()
                throw SecurityException("Security Exception: Spoofed Location or Duplicate Device Request Denied.")
            }
            tokCursor.close()

            // Anti-Clear-Data check: Query Central Server to ensure this device hasn't already registered
            try {
                var serverTok = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.checkDeviceRegisteredOnServer(deviceId, targetDarbarDate, cleanPhone10)
                if (serverTok == null && targetDarbarDate != today) {
                    serverTok = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.checkDeviceRegisteredOnServer(deviceId, today, cleanPhone10)
                }
                if (serverTok != null) {
                    throw SecurityException("Security Exception: Spoofed Location or Duplicate Device Request Denied.")
                }
            } catch (se: SecurityException) {
                throw se
            } catch (e: Exception) {
                // If offline, check persistent hardware receipt in external storage
                val persistent = com.example.shribalajikripadham.hardware.PersistentTokenReceiptHelper.readPersistentReceipt(deviceId, targetDarbarDate)
                    ?: com.example.shribalajikripadham.hardware.PersistentTokenReceiptHelper.readPersistentReceipt(deviceId, today)
                if (persistent != null) {
                    throw SecurityException("Security Exception: Spoofed Location or Duplicate Device Request Denied.")
                }
            }
        }

        val distFromDarbar = if (latitude > 0.0 && longitude > 0.0 && targetLat > 0.0) {
            GeofenceLocationManager.calculateDistanceMeters(
                latitude, longitude,
                targetLat, targetLng
            )
        } else -1.0

        val isOutstationDevotee = isDevoteeRequest && (distFromDarbar > targetOutstationKm * 1000.0)
        val defaultLocalCity = if (isTuesdayDarbar) "बुलन्दशहर (स्थानीय)" else "डूँगरा जाट (स्थानीय)"
        val safeCity = if (isOutstationDevotee) {
            val autoResolved = GeofenceLocationManager.resolveVillageAndCity(appContext, latitude, longitude)
            if (autoResolved.isNotBlank()) autoResolved else (if (city.isNotBlank()) city.trim() else "आउटस्टेशन भक्त (GPS)")
        } else {
            if (city.isBlank()) {
                if (originAddress.isNotBlank()) originAddress.trim() else defaultLocalCity
            } else city.trim()
        }
        val safeOrigin = if (originAddress.isNotBlank() && !isOutstationDevotee) originAddress.trim() else safeCity
        val safeDest = if (destinationAddress.isNotBlank() && destinationAddress != "श्री बालाजी कृपा धाम, डुंगरा जाट") destinationAddress else targetVenueName

        // Priority 1: Real GPS distance (Supremacy over text queries)
        // Priority 2: Fallback to text query routing if GPS is unavailable
        val calculatedDistance = if (distFromDarbar >= 0.0) {
            if (distFromDarbar <= targetRadius) {
                (kotlin.math.round((distFromDarbar / 1000.0) * 10) / 10).toFloat()
            } else {
                (kotlin.math.round((distFromDarbar / 1000.0) * 1.28 * 10) / 10).toFloat()
            }
        } else if (distanceKm >= 0f) {
            distanceKm
        } else {
            DistanceCalculatorService.resolveDrivingDistance(
                origin = safeOrigin,
                deviceLat = latitude,
                deviceLng = longitude
            ).distanceKm
        }

        if (settings.isGeofenceEnforced && !shouldBypassGeofence) {
            val isPhysicallyAtVenue = distFromDarbar in 0.0..targetRadius
            val isOutstationAdvance = distFromDarbar > (targetOutstationKm * 1000.0) && settings.isOutstationAdvanceAllowed
            if (!isPhysicallyAtVenue && !isOutstationAdvance) {
                val outstationKm = targetOutstationKm.toInt()
                val allowedM = targetRadius.toInt()
                val radiusDesc = if (allowedM >= 1000) "${String.format(java.util.Locale.US, "%.1f", allowedM / 1000.0)} किमी" else "$allowedM मीटर"
                val realKm = if (distFromDarbar >= 0.0) String.format(java.util.Locale.US, "%.1f", distFromDarbar / 1000.0) else String.format(java.util.Locale.US, "%.1f", calculatedDistance)
                throw SecurityException("⚠️ $targetVenueLabel दूरी नियम: आपकी वास्तविक दूरी ($realKm किमी) ${outstationKm} किमी के दायरे में है।\n\nस्थानीय भक्तों के लिए टोकन पंजीकरण केवल $targetVenueLabel परिसर ($radiusDesc के भीतर) में उपस्थित होकर ही मान्य है। कृपया परिसर में आकर टोकन प्राप्त करें।")
            }
        }

        var nextTokenNum = 1
        var insertedId: Long = -1

        val finalPhotoUri = if (photoUri.startsWith("http://") || photoUri.startsWith("https://")) photoUri else ""

        var centralTokenNumber: Int? = null
        var centralOk = false
        var centralNum = -1
        try {
            val isRootedDevice = com.example.shribalajikripadham.hardware.GeofenceLocationManager.isDeviceRooted(appContext)
            val result = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.issueCentralToken(
                patientName = patientName,
                phoneNumber = phoneNumber,
                city = safeCity,
                deviceId = deviceId,
                latitude = latitude,
                longitude = longitude,
                distanceKm = calculatedDistance.toDouble(),
                photoUrl = finalPhotoUri,
                registeredBy = registeredBy,
                originAddress = safeOrigin,
                destinationAddress = safeDest,
                darbarDate = targetDarbarDate,
                customTokenNumber = customTokenNumber,
                canIssueAnytime = isSuperAdmin,
                darbarVenue = darbarVenue,
                isMockLocation = isMockLocation,
                locationAccuracy = locationAccuracy,
                isRooted = isRootedDevice
            )
            centralOk = result.first
            centralNum = result.second
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (customTokenNumber == null || customTokenNumber <= 0) {
            if (!centralOk || centralNum <= 0) {
                val srvErr = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.lastIssueErrorMessage
                if (!srvErr.isNullOrBlank()) {
                    throw SecurityException(srvErr)
                }
                throw IllegalStateException("⚠️ इंटरनेट कनेक्शन अनिवार्य है!\n\nटोकन नंबर में किसी भी टकराव (Duplicate Token) को रोकने के लिए सेंट्रल सर्वर से सीधा संपर्क अनिवार्य है। कृपया इंटरनेट चालू करें और पुनः प्रयास करें।")
            } else {
                centralTokenNumber = centralNum
            }
        } else {
            if (!centralOk || centralNum <= 0) {
                val srvErr = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.lastIssueErrorMessage
                if (!srvErr.isNullOrBlank()) {
                    throw SecurityException(srvErr)
                }
                throw IllegalStateException("⚠️ इंटरनेट कनेक्शन अनिवार्य है!\n\nटोकन नंबर में किसी भी टकराव को रोकने के लिए इंटरनेट चालू होना आवश्यक है।")
            }
            centralTokenNumber = centralNum
        }

        // Strict Thread & Atomic SQLite Lock to eliminate Token Race Conditions
        synchronized(tokenGenerationLock) {
            if (settings.maxDailyTokens > 0) {
                val countCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM tokens WHERE (darbar_date = ? OR darbar_date = ?) AND status != 'CANCELLED'",
                    arrayOf(today, targetDarbarDate)
                )
                var todayCount = 0
                if (countCursor.moveToFirst()) {
                    todayCount = countCursor.getInt(0)
                }
                countCursor.close()
                if (todayCount >= settings.maxDailyTokens) {
                    throw IllegalStateException("आज की अधिकतम टोकन सीमा (${settings.maxDailyTokens}) पूरी हो चुकी है। कृपया अगले दरबार में प्रयास करें।")
                }
            }

            db.beginTransaction()
            try {
                nextTokenNum = if (customTokenNumber != null && customTokenNumber > 0) {
                    // If replacing a previously cancelled token with this number, remove old entry
                    db.delete("tokens", "(darbar_date = ? OR darbar_date = ?) AND token_number = ? AND status = 'CANCELLED'", arrayOf(today, targetDarbarDate, customTokenNumber.toString()))
                    customTokenNumber
                } else if (centralTokenNumber != null && centralTokenNumber > 0) {
                    centralTokenNumber
                } else {
                    val maxTokenCursor = db.rawQuery(
                        "SELECT MAX(token_number) FROM tokens WHERE darbar_date = ? OR darbar_date = ?",
                        arrayOf(today, targetDarbarDate)
                    )
                    var num = 1
                    if (maxTokenCursor.moveToFirst() && !maxTokenCursor.isNull(0)) {
                        num = maxTokenCursor.getInt(0) + 1
                    }
                    maxTokenCursor.close()
                    // Public tokens must skip VIP slots [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
                    while (num <= 20 && num % 2 == 0) {
                        num++
                    }
                    num
                }

                val tokenValues = ContentValues().apply {
                    put("token_number", nextTokenNum)
                    put("darbar_date", targetDarbarDate)
                    put("patient_name", patientName)
                    put("phone_number", phoneNumber)
                    put("city", safeCity)
                    put("device_id", deviceId)
                    put("latitude", latitude)
                    put("longitude", longitude)
                    put("status", TokenStatus.WAITING.name)
                    put("registered_by", registeredBy)
                    put("photo_uri", if (finalPhotoUri.isNotBlank()) finalPhotoUri else photoUri)
                    put("is_darshan_completed", 0)
                    put("darshan_completed_at", 0L)
                    put("origin_address", safeOrigin)
                    put("destination_address", safeDest)
                    put("distance_km", calculatedDistance)
                    put("darbar_venue", darbarVenue)
                    put("created_at", System.currentTimeMillis())
                }

                insertedId = db.insertWithOnConflict("tokens", null, tokenValues, SQLiteDatabase.CONFLICT_REPLACE)

                if (isDevoteeRequest) {
                    val devValues = ContentValues().apply {
                        put("device_id", deviceId)
                        put("darbar_date", targetDarbarDate)
                        put("token_number", nextTokenNum)
                        put("patient_name", patientName)
                        put("created_at", System.currentTimeMillis())
                    }
                    db.insertWithOnConflict("device_registrations", null, devValues, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                throw SecurityException("Security Exception: Spoofed Location or Duplicate Device Request Denied.")
            } finally {
                db.endTransaction()
            }
        }

        // Asynchronous non-blocking photo upload to central CDN
        if (insertedId > 0 && photoUri.isNotBlank() && !photoUri.startsWith("http")) {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    val rawPath = photoUri.removePrefix("file://")
                    val f = java.io.File(rawPath)
                    if (f.exists() && f.length() > 0) {
                        val upUrl = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.uploadPhoto(f)
                        if (!upUrl.isNullOrBlank()) {
                            val wDb = dbHelper.writableDatabase
                            val cvUp = ContentValues().apply { put("photo_uri", upUrl) }
                            wDb.update("tokens", cvUp, "id = ?", arrayOf(insertedId.toString()))
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        // Auto-index into Devotee Master Directory
        upsertDevoteeDirectoryInternal(
            name = patientName,
            phone = phoneNumber,
            city = safeCity,
            photoUri = finalPhotoUri,
            sourceModule = "TOKEN",
            lastVisitDate = today
        )

        val createdToken = Token(
            id = insertedId,
            tokenNumber = nextTokenNum,
            darbarDate = targetDarbarDate,
            patientName = patientName,
            phoneNumber = phoneNumber,
            city = safeCity,
            deviceId = deviceId,
            latitude = latitude,
            longitude = longitude,
            status = TokenStatus.WAITING,
            registeredBy = registeredBy,
            photoUri = finalPhotoUri,
            isDarshanCompleted = false,
            darshanCompletedAt = 0L,
            originAddress = safeOrigin,
            destinationAddress = safeDest,
            distanceKm = calculatedDistance,
            darbarVenue = darbarVenue,
            createdAt = System.currentTimeMillis()
        )

        // ☁️ Smart GitHub Sync Policy: Individual real-time tokens are handled instantly by Hostinger Central MySQL & Google Sheets.
        // Consolidated token backup is pushed when Super Admin triggers "Push All to GitHub" to prevent GitHub 409 rate-limiting.

        // 🛡️ ANTI-BYPASS: Save hardware-bound persistent receipt into public device storage & SharedPreferences
        try {
            com.example.shribalajikripadham.hardware.PersistentTokenReceiptHelper.savePersistentReceipt(createdToken)
            appContext.getSharedPreferences("sbkd_devotee_my_token_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("my_phone_number", phoneNumber)
                .apply()
        } catch (e: Exception) {}

        // ⚡ NANO-SECOND RESPONSE: Offload all cloud syncs (Google Sheets, Hostinger, Directory, Drive Backup)
        // to a background CoroutineScope so that token generation returns instantly to devotee!
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            // 📊 Universal Real-Time Google Sheets Sync for ALL tokens (Devotees + Admin + Sevadar)
            try {
                com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager.postTokenToSheet(appContext, createdToken)
            } catch (e: Exception) {}

            // 🌐 Real-Time Hostinger Sync (Ensures MySQL has this token even if generated offline or via custom token)
            if (centralTokenNumber == null) {
                try {
                    com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.issueCentralToken(
                        patientName = createdToken.patientName,
                        phoneNumber = createdToken.phoneNumber,
                        city = createdToken.city,
                        deviceId = createdToken.deviceId,
                        latitude = createdToken.latitude,
                        longitude = createdToken.longitude,
                        distanceKm = createdToken.distanceKm.toDouble(),
                        photoUrl = createdToken.photoUri,
                        registeredBy = createdToken.registeredBy,
                        originAddress = createdToken.originAddress,
                        destinationAddress = createdToken.destinationAddress,
                        darbarDate = createdToken.darbarDate,
                        canIssueAnytime = shouldBypassGeofence || (isAdminDesk && (bypassGeofence || settings.allowAdminReservedTokens))
                    )
                } catch (e: Exception) {}
            }

            // 🌐 Central Devotee Profile Sync (Saves contact to registry for cross-device lookup)
            try {
                if (phoneNumber.isNotBlank() && patientName.isNotBlank()) {
                    upsertDevoteeProfile(
                        name = patientName,
                        phone = phoneNumber,
                        city = safeCity,
                        faceVector = null,
                        photoUri = photoUri,
                        registeredBy = registeredBy
                    )
                }
            } catch (e: Exception) {}

            // Auto-save local snapshot and trigger cloud vault backup
            try {
                com.example.shribalajikripadham.util.GoogleDriveSyncHelper.autoPushTrigger(appContext)
            } catch (e: Exception) {}
        }

        createdToken
    }

    /**
     * Sequential Batch Token Issuance for Paper Register OCR / Scanned Notebook.
     * Strict Sequencing: Starts immediately after today's existing MAX(token_number).
     * e.g., If app already generated #1, #2, #3, register tokens get #4, #5, #6... in exact serial order!
     * Subsequent tokens generated anywhere will continue sequentially after the register tokens (#7...).
     */
    suspend fun registerBatchTokens(
        entries: List<com.example.shribalajikripadham.data.model.RegisterEntry>,
        registeredBy: String
    ): List<Token> = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) return@withContext emptyList()

        val db = dbHelper.writableDatabase
        val today = DatabaseHelper.getTodayDateString()

        // 1. Determine the current MAX(token_number) for today
        val maxCursor = db.rawQuery(
            "SELECT MAX(token_number) FROM tokens WHERE darbar_date = ?",
            arrayOf(today)
        )
        var currentMax = 0
        if (maxCursor.moveToFirst() && !maxCursor.isNull(0)) {
            currentMax = maxCursor.getInt(0)
        }
        maxCursor.close()

        val createdTokens = mutableListOf<Token>()
        val now = System.currentTimeMillis()

        db.beginTransaction()
        try {
            for ((index, entry) in entries.withIndex()) {
                val nextTokenNum = currentMax + 1 + index
                val safeCity = if (entry.city.isBlank()) "डूँगरा जाट (स्थानीय)" else entry.city.trim()
                val safeName = entry.patientName.trim()
                val safePhone = entry.phoneNumber.trim()

                val cv = ContentValues().apply {
                    put("token_number", nextTokenNum)
                    put("darbar_date", today)
                    put("patient_name", safeName)
                    put("phone_number", safePhone)
                    put("city", safeCity)
                    put("device_id", "REGISTER_SCAN_${now}_$index")
                    put("latitude", 28.3972915)
                    put("longitude", 78.1460410)
                    put("status", TokenStatus.WAITING.name)
                    put("registered_by", registeredBy)
                    put("photo_uri", "")
                    put("is_darshan_completed", 0)
                    put("darshan_completed_at", 0L)
                    put("origin_address", safeCity)
                    put("destination_address", "श्री बालाजी कृपा धाम, डुंगरा जाट")
                    put("distance_km", 0.0)
                    put("created_at", now + index)
                }

                val insertedId = db.insertWithOnConflict("tokens", null, cv, SQLiteDatabase.CONFLICT_REPLACE)

                val token = Token(
                    id = insertedId,
                    tokenNumber = nextTokenNum,
                    darbarDate = today,
                    patientName = safeName,
                    phoneNumber = safePhone,
                    city = safeCity,
                    deviceId = "REGISTER_SCAN",
                    latitude = 28.3972915,
                    longitude = 78.1460410,
                    status = TokenStatus.WAITING,
                    registeredBy = registeredBy,
                    photoUri = "",
                    isDarshanCompleted = false,
                    darshanCompletedAt = 0L,
                    originAddress = safeCity,
                    destinationAddress = "श्री बालाजी कृपा धाम, डुंगरा जाट",
                    distanceKm = 0.0f,
                    createdAt = now + index
                )
                createdTokens.add(token)

                // Central Devotee Profile Sync if phone and name are present
                if (safePhone.isNotBlank() && safeName.isNotBlank()) {
                    try {
                        upsertDevoteeProfile(
                            name = safeName,
                            phone = safePhone,
                            city = safeCity,
                            faceVector = null,
                            photoUri = "",
                            registeredBy = registeredBy
                        )
                    } catch (e: Exception) {}
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        // 2. Batch Google Sheets Sync in background
        try {
            com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager.postBatchTokensToSheet(appContext, createdTokens)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Batch GitHub & Hostinger Triple Sync
        // 3. Batch Hostinger MySQL Central Sync (Guaranteed 0-collision online persistence)
        try {
            com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.syncAllTokensToHostinger(createdTokens)
        } catch (e: Exception) {}

        createdTokens
    }

    suspend fun getAllTokensToday(): List<Token> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val today = DatabaseHelper.getTodayDateString()
        val list = mutableListOf<Token>()
        val cursor = db.rawQuery("SELECT * FROM tokens WHERE darbar_date = ? ORDER BY token_number ASC", arrayOf(today))
        while (cursor.moveToNext()) {
            list.add(
                Token(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    tokenNumber = cursor.getInt(cursor.getColumnIndexOrThrow("token_number")),
                    darbarDate = cursor.getString(cursor.getColumnIndexOrThrow("darbar_date")),
                    patientName = cursor.getString(cursor.getColumnIndexOrThrow("patient_name")),
                    phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone_number")),
                    city = try { cursor.getString(cursor.getColumnIndexOrThrow("city")) } catch (e: Exception) { "डूँगरा जाट (स्थानीय)" },
                    deviceId = cursor.getString(cursor.getColumnIndexOrThrow("device_id")),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    status = TokenStatus.fromRaw(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
                    registeredBy = cursor.getString(cursor.getColumnIndexOrThrow("registered_by")),
                    photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri")) ?: "",
                    isDarshanCompleted = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_darshan_completed")) == 1 } catch (e: Exception) { false },
                    darshanCompletedAt = try { cursor.getLong(cursor.getColumnIndexOrThrow("darshan_completed_at")) } catch (e: Exception) { 0L },
                    originAddress = try { cursor.getString(cursor.getColumnIndexOrThrow("origin_address")) } catch (e: Exception) { "" }.ifEmpty { cursor.getString(cursor.getColumnIndexOrThrow("city")) },
                    destinationAddress = try { cursor.getString(cursor.getColumnIndexOrThrow("destination_address")) } catch (e: Exception) { "श्री बालाजी कृपा धाम, डुंगरा जाट" },
                    distanceKm = try { cursor.getFloat(cursor.getColumnIndexOrThrow("distance_km")) } catch (e: Exception) { -1f },
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                )
            )
        }
        cursor.close()
        val existingNumbers = list.map { it.tokenNumber }.toSet()
        val maxToken = list.maxOfOrNull { it.tokenNumber } ?: 0
        if (maxToken > 1) {
            val reservedSlots = listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
            for (num in reservedSlots) {
                if (num <= maxToken && !existingNumbers.contains(num)) {
                    list.add(
                        Token(
                            id = -num.toLong(),
                            tokenNumber = num,
                            darbarDate = today,
                            patientName = "व्यवस्थापक आरक्षित - प्रतीक्षारत / मरीज अभी उपस्थित नहीं है",
                            phoneNumber = "",
                            city = "आरक्षित स्लॉट",
                            deviceId = "RESERVED",
                            latitude = 28.3972915,
                            longitude = 78.1460410,
                            status = TokenStatus.WAITING,
                            registeredBy = "ADMIN (आरक्षित)",
                            originAddress = "व्यवस्थापक आरक्षित",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            list.sortBy { it.tokenNumber }
        }
        list
    }

    suspend fun getAdminReservedTokensCountToday(adminIdentifier: String): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val today = DatabaseHelper.getTodayDateString()
        val cleanName = adminIdentifier.replace("ADMIN", "").replace("SUPER_ADMIN", "").replace("(", "").replace(")", "").trim()
        val matchPattern = if (cleanName.isNotBlank()) "%$cleanName%" else adminIdentifier
        val cursor = db.rawQuery(
            """SELECT COUNT(*) FROM tokens 
               WHERE darbar_date = ? 
               AND (registered_by LIKE ? OR registered_by = ?) 
               AND token_number IN (2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
               AND status != 'CANCELLED'""",
            arrayOf(today, matchPattern, adminIdentifier)
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        count
    }

    suspend fun getAllTokens(): List<Token> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Token>()
        val cursor = db.rawQuery("SELECT * FROM tokens ORDER BY id ASC", null)
        while (cursor.moveToNext()) {
            list.add(
                Token(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    tokenNumber = cursor.getInt(cursor.getColumnIndexOrThrow("token_number")),
                    darbarDate = cursor.getString(cursor.getColumnIndexOrThrow("darbar_date")),
                    patientName = cursor.getString(cursor.getColumnIndexOrThrow("patient_name")),
                    phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone_number")),
                    city = try { cursor.getString(cursor.getColumnIndexOrThrow("city")) } catch (e: Exception) { "डूँगरा जाट (स्थानीय)" },
                    deviceId = cursor.getString(cursor.getColumnIndexOrThrow("device_id")),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    status = TokenStatus.fromRaw(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
                    registeredBy = cursor.getString(cursor.getColumnIndexOrThrow("registered_by")),
                    photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri")) ?: "",
                    isDarshanCompleted = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_darshan_completed")) == 1 } catch (e: Exception) { false },
                    darshanCompletedAt = try { cursor.getLong(cursor.getColumnIndexOrThrow("darshan_completed_at")) } catch (e: Exception) { 0L },
                    originAddress = try { cursor.getString(cursor.getColumnIndexOrThrow("origin_address")) } catch (e: Exception) { "" }.ifEmpty { cursor.getString(cursor.getColumnIndexOrThrow("city")) },
                    destinationAddress = try { cursor.getString(cursor.getColumnIndexOrThrow("destination_address")) } catch (e: Exception) { "श्री बालाजी कृपा धाम, डुंगरा जाट" },
                    distanceKm = try { cursor.getFloat(cursor.getColumnIndexOrThrow("distance_km")) } catch (e: Exception) { -1f },
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun insertOrUpdateCentralToken(
        tokenNumber: Int,
        darbarDate: String,
        patientName: String,
        phoneNumber: String,
        city: String = "डूँगरा जाट (स्थानीय)",
        deviceId: String = "HOSTINGER",
        latitude: Double = 28.3972915,
        longitude: Double = 78.1460410,
        distanceKm: Float = 0f,
        photoUri: String = "",
        registeredBy: String = "HOSTINGER",
        status: String = "WAITING",
        isDarshanCompleted: Boolean = false,
        createdAt: Long = System.currentTimeMillis()
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        try {
            val cv = ContentValues().apply {
                put("token_number", tokenNumber)
                put("darbar_date", darbarDate)
                put("patient_name", patientName)
                put("phone_number", phoneNumber)
                put("city", city)
                put("device_id", deviceId)
                put("latitude", latitude)
                put("longitude", longitude)
                put("distance_km", distanceKm)
                put("photo_uri", photoUri)
                put("registered_by", registeredBy)
                put("status", status)
                put("is_darshan_completed", if (isDarshanCompleted) 1 else 0)
                put("created_at", createdAt)
            }
            val existing = db.rawQuery(
                "SELECT id FROM tokens WHERE darbar_date = ? AND token_number = ?",
                arrayOf(darbarDate, tokenNumber.toString())
            )
            val exists = existing.moveToFirst()
            existing.close()
            if (exists) {
                db.update("tokens", cv, "darbar_date = ? AND token_number = ?", arrayOf(darbarDate, tokenNumber.toString()))
            } else {
                db.insertWithOnConflict("tokens", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleDarshanCompleted(tokenId: Long, completed: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        var tokenNum = 0
        var darbarDate = ""
        try {
            val cur = db.rawQuery("SELECT token_number, darbar_date FROM tokens WHERE id = ?", arrayOf(tokenId.toString()))
            if (cur.moveToFirst()) {
                tokenNum = cur.getInt(0)
                darbarDate = cur.getString(1)
            }
            cur.close()
        } catch (e: Exception) {}

        val cv = ContentValues().apply {
            put("is_darshan_completed", if (completed) 1 else 0)
            put("darshan_completed_at", if (completed) System.currentTimeMillis() else 0L)
            if (completed) {
                put("status", TokenStatus.COMPLETED.name)
            } else {
                put("status", TokenStatus.WAITING.name)
            }
        }
        val ok = db.update("tokens", cv, "id = ?", arrayOf(tokenId.toString())) > 0
        if (ok && tokenNum > 0) {
            if (completed) {
                try {
                    db.execSQL("UPDATE ashram_settings SET running_token_number = ? WHERE id = 1 AND running_token_number < ?", arrayOf(tokenNum, tokenNum))
                } catch (e: Exception) {}
            }
            val newStatus = if (completed) TokenStatus.COMPLETED else TokenStatus.WAITING
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.updateCentralTokenStatus(
                        tokenNum, darbarDate, newStatus.name, completed
                    )
                } catch (e: Exception) {}
                try {
                    com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager.updateTokenStatusInSheet(
                        appContext, darbarDate, tokenNum, newStatus.name
                    )
                } catch (e: Exception) {}
                try {
                    com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.updateLiveTokenStatusInGitHub(
                        appContext, tokenNum, darbarDate, newStatus
                    )
                } catch (e: Exception) {}
            }
        }
        ok
    }

    suspend fun updateTokenStatus(tokenId: Long, status: TokenStatus): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        var tokenNum = 0
        var darbarDate = ""
        try {
            val cur = db.rawQuery("SELECT token_number, darbar_date FROM tokens WHERE id = ?", arrayOf(tokenId.toString()))
            if (cur.moveToFirst()) {
                tokenNum = cur.getInt(0)
                darbarDate = cur.getString(1)
            }
            cur.close()
        } catch (e: Exception) {}

        val cv = ContentValues().apply {
            put("status", status.name)
            if (status == TokenStatus.COMPLETED) {
                put("is_darshan_completed", 1)
                put("darshan_completed_at", System.currentTimeMillis())
            } else {
                put("is_darshan_completed", 0)
                put("darshan_completed_at", 0L)
            }
        }
        val ok = db.update("tokens", cv, "id = ?", arrayOf(tokenId.toString())) > 0
        if (ok && tokenNum > 0) {
            if (status == TokenStatus.COMPLETED) {
                try {
                    db.execSQL("UPDATE ashram_settings SET running_token_number = ? WHERE id = 1 AND running_token_number < ?", arrayOf(tokenNum, tokenNum))
                } catch (e: Exception) {}
            }
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.updateCentralTokenStatus(
                        tokenNum, darbarDate, status.name, status == TokenStatus.COMPLETED
                    )
                } catch (e: Exception) {}
                try {
                    com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager.updateTokenStatusInSheet(
                        appContext, darbarDate, tokenNum, status.name
                    )
                } catch (e: Exception) {}
                try {
                    com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.updateLiveTokenStatusInGitHub(
                        appContext, tokenNum, darbarDate, status
                    )
                } catch (e: Exception) {}
            }
        }
        ok
    }

    // --- Balaji Yatra Bus Seats ---
    suspend fun getAllBusSeats(): List<BusSeat> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val seats = mutableListOf<BusSeat>()
        val cursor = db.rawQuery("SELECT * FROM bus_seats ORDER BY seat_number ASC", null)
        while (cursor.moveToNext()) {
            seats.add(
                BusSeat(
                    seatNumber = cursor.getInt(cursor.getColumnIndexOrThrow("seat_number")),
                    seatLabel = cursor.getString(cursor.getColumnIndexOrThrow("seat_label")),
                    row = cursor.getInt(cursor.getColumnIndexOrThrow("row_idx")),
                    column = cursor.getInt(cursor.getColumnIndexOrThrow("col_idx")),
                    isBooked = cursor.getInt(cursor.getColumnIndexOrThrow("is_booked")) == 1,
                    passengerName = cursor.getString(cursor.getColumnIndexOrThrow("passenger_name")) ?: "",
                    passengerAge = try { cursor.getInt(cursor.getColumnIndexOrThrow("passenger_age")) } catch (e: Exception) { 0 },
                    passengerGender = try { cursor.getString(cursor.getColumnIndexOrThrow("passenger_gender")) ?: "" } catch (e: Exception) { "" },
                    phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone_number")) ?: "",
                    boardingPoint = cursor.getString(cursor.getColumnIndexOrThrow("boarding_point")) ?: "Gram Dungra Jaat Ashram",
                    paymentStatus = try { PaymentStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("payment_status"))) } catch (e: Exception) { PaymentStatus.UNPAID },
                    paymentMode = cursor.getString(cursor.getColumnIndexOrThrow("payment_mode")) ?: "UPI_QR",
                    transactionId = try { cursor.getString(cursor.getColumnIndexOrThrow("transaction_id")) ?: "" } catch (e: Exception) { "" },
                    fareAmount = cursor.getInt(cursor.getColumnIndexOrThrow("fare_amount")),
                    yatraDate = cursor.getString(cursor.getColumnIndexOrThrow("yatra_date")) ?: "",
                    bookedAt = try { cursor.getLong(cursor.getColumnIndexOrThrow("booked_at")) } catch (e: Exception) { 0L },
                    bookedBy = try { cursor.getString(cursor.getColumnIndexOrThrow("booked_by")) ?: "DEVOTEE" } catch (e: Exception) { "DEVOTEE" },
                    notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")) ?: "",
                    holdExpiresAt = try { cursor.getLong(cursor.getColumnIndexOrThrow("hold_expires_at")) } catch (e: Exception) { 0L },
                    heldBy = try { cursor.getString(cursor.getColumnIndexOrThrow("held_by")) ?: "" } catch (e: Exception) { "" }
                )
            )
        }
        cursor.close()
        seats
    }

    suspend fun updateSeatBooking(
        seatNumber: Int,
        isBooked: Boolean,
        passengerName: String,
        phoneNumber: String,
        paymentStatus: PaymentStatus,
        paymentMode: String,
        boardingPoint: String = "Gram Dungra Jaat Ashram",
        fareAmount: Int = 0,
        notes: String = "",
        passengerAge: Int = 0,
        passengerGender: String = "",
        transactionId: String = "",
        bookedAt: Long = System.currentTimeMillis(),
        bookedBy: String = "ADMIN"
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_booked", if (isBooked) 1 else 0)
            put("passenger_name", if (isBooked) passengerName else "")
            put("passenger_age", if (isBooked) passengerAge else 0)
            put("passenger_gender", if (isBooked) passengerGender else "")
            put("phone_number", if (isBooked) phoneNumber else "")
            put("boarding_point", if (isBooked) boardingPoint else "")
            put("payment_status", paymentStatus.name)
            put("payment_mode", paymentMode)
            put("transaction_id", if (isBooked) transactionId else "")
            put("fare_amount", fareAmount)
            put("booked_at", if (isBooked) bookedAt else 0L)
            put("booked_by", if (isBooked) bookedBy else "DEVOTEE")
            put("notes", notes)
        }
        val res = db.update("bus_seats", cv, "seat_number = ?", arrayOf(seatNumber.toString())) > 0
        if (res) {
            try {
                publishBusSeatsToGitHub()
            } catch (e: Exception) {}
        }
        res
    }

    suspend fun bookBusSeat(
        seatNumber: Int,
        passengerName: String,
        phoneNumber: String,
        boardingPoint: String,
        paymentStatus: PaymentStatus,
        paymentMode: String,
        fareAmount: Int,
        notes: String,
        passengerAge: Int = 0,
        passengerGender: String = "",
        transactionId: String = "",
        bookedAt: Long = System.currentTimeMillis(),
        bookedBy: String = "DEVOTEE"
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_booked", 1)
            put("passenger_name", passengerName)
            put("passenger_age", passengerAge)
            put("passenger_gender", passengerGender)
            put("phone_number", phoneNumber)
            put("boarding_point", boardingPoint)
            put("payment_status", paymentStatus.name)
            put("payment_mode", paymentMode)
            put("transaction_id", transactionId)
            put("fare_amount", fareAmount)
            put("booked_at", bookedAt)
            put("booked_by", bookedBy)
            put("notes", notes)
        }
        val res = db.update("bus_seats", cv, "seat_number = ?", arrayOf(seatNumber.toString())) > 0
        if (res) {
            try {
                publishBusSeatsToGitHub()
            } catch (e: Exception) {}
        }
        res
    }

    suspend fun bookMultipleBusSeats(
        seatsToBook: List<BusSeat>,
        paymentRecord: PaymentRecord? = null
    ): Boolean = withContext(Dispatchers.IO) {
        // 0. Atomic Remote Lock & Collision Check via Central Server
        for (seat in seatsToBook) {
            try {
                val remoteBook = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.bookBusSeatRemote(
                    seat = seat,
                    deviceId = if (seat.heldBy.isNotBlank()) seat.heldBy else seat.phoneNumber
                )
                if (!remoteBook.first) {
                    throw IllegalStateException(remoteBook.second)
                }
            } catch (e: IllegalStateException) {
                throw e
            } catch (e: Exception) {
                // Offline fallback: allow local check
            }
        }

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // 1. Atomic Collision Check: Ensure no seat is already booked or held by another devotee
            val nowMs = System.currentTimeMillis()
            for (seat in seatsToBook) {
                val checkCursor = db.rawQuery("SELECT is_booked, passenger_name, hold_expires_at, held_by FROM bus_seats WHERE seat_number = ?", arrayOf(seat.seatNumber.toString()))
                var alreadyBooked = false
                var existingPassenger = ""
                var isHeldByOther = false
                if (checkCursor.moveToFirst()) {
                    if (checkCursor.getInt(0) == 1) {
                        alreadyBooked = true
                        existingPassenger = checkCursor.getString(1) ?: ""
                    } else {
                        val holdExp = try { checkCursor.getLong(2) } catch (e: Exception) { 0L }
                        val holder = try { checkCursor.getString(3) ?: "" } catch (e: Exception) { "" }
                        if (holdExp > nowMs && holder.isNotBlank() && holder != seat.heldBy && holder != seat.phoneNumber) {
                            isHeldByOther = true
                        }
                    }
                }
                checkCursor.close()
                if (alreadyBooked) {
                    throw IllegalStateException("सीट संख्या #${seat.seatNumber} पहले से आरक्षित है (${existingPassenger})!")
                }
                if (isHeldByOther) {
                    throw IllegalStateException("सीट संख्या #${seat.seatNumber} वर्तमान में अन्य भक्त द्वारा 5 मिनट के होल्ड पर है।")
                }
            }

            for (seat in seatsToBook) {
                val cv = ContentValues().apply {
                    put("is_booked", 1)
                    put("passenger_name", seat.passengerName)
                    put("passenger_age", seat.passengerAge)
                    put("passenger_gender", seat.passengerGender)
                    put("phone_number", seat.phoneNumber)
                    put("boarding_point", seat.boardingPoint)
                    put("payment_status", seat.paymentStatus.name)
                    put("payment_mode", seat.paymentMode)
                    put("transaction_id", seat.transactionId)
                    put("fare_amount", seat.fareAmount)
                    put("yatra_date", seat.yatraDate)
                    put("booked_at", if (seat.bookedAt > 0) seat.bookedAt else System.currentTimeMillis())
                    put("booked_by", seat.bookedBy)
                    put("notes", seat.notes)
                    put("hold_expires_at", 0L)
                    put("held_by", "")
                }
                db.update("bus_seats", cv, "seat_number = ?", arrayOf(seat.seatNumber.toString()))
            }
            if (paymentRecord != null) {
                val pCv = ContentValues().apply {
                    put("payment_id", paymentRecord.paymentId)
                    put("devotee_name", paymentRecord.devoteeName)
                    put("devotee_phone", paymentRecord.devoteePhone)
                    put("payment_app", paymentRecord.paymentApp)
                    put("transaction_id", paymentRecord.transactionId)
                    put("amount", paymentRecord.amount)
                    put("purpose", paymentRecord.purpose)
                    put("seat_numbers", paymentRecord.seatNumbers)
                    put("timestamp", paymentRecord.timestamp)
                    put("payment_status", paymentRecord.paymentStatus)
                    put("payment_mode", paymentRecord.paymentMode)
                    put("verified_by", paymentRecord.verifiedBy)
                    put("notes", paymentRecord.notes)
                }
                db.insertWithOnConflict("payment_records", null, pCv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.endTransaction()
            try {
                publishBusSeatsToGitHub()
                if (paymentRecord != null) {
                    publishPaymentsToGitHub()
                }
            } catch (e: Exception) {}
        }
    }

    suspend fun holdBusSeats(
        seatNumbers: List<Int>,
        heldBy: String,
        holdDurationMs: Long = 300000L
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        // 1. Try remote atomic 5-minute hold on Central Hostinger Server
        try {
            val remoteRes = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.holdBusSeatsRemote(
                yatraDate = "2026-04-15",
                seatNumbers = seatNumbers,
                deviceId = heldBy
            )
            if (!remoteRes.first) {
                return@withContext remoteRes
            }
        } catch (e: Exception) {
            // If offline, proceed with local check
        }

        val db = dbHelper.writableDatabase
        val nowMs = System.currentTimeMillis()
        val expireMs = nowMs + holdDurationMs
        db.beginTransaction()
        try {
            for (sNum in seatNumbers) {
                val checkCursor = db.rawQuery("SELECT is_booked, hold_expires_at, held_by FROM bus_seats WHERE seat_number = ?", arrayOf(sNum.toString()))
                if (checkCursor.moveToFirst()) {
                    val isBooked = checkCursor.getInt(0) == 1
                    val holdExp = try { checkCursor.getLong(1) } catch (e: Exception) { 0L }
                    val currentHolder = try { checkCursor.getString(2) ?: "" } catch (e: Exception) { "" }
                    checkCursor.close()

                    if (isBooked) {
                        return@withContext Pair(false, "सीट संख्या #$sNum पहले से आरक्षित है।")
                    }
                    if (holdExp > nowMs && currentHolder.isNotBlank() && currentHolder != heldBy) {
                        val remainingSec = ((holdExp - nowMs) / 1000).coerceAtLeast(1)
                        return@withContext Pair(false, "सीट संख्या #$sNum अन्य भक्त द्वारा होल्ड पर है ($remainingSec सेकंड शेष)।")
                    }
                } else {
                    checkCursor.close()
                }
            }

            for (sNum in seatNumbers) {
                val cv = ContentValues().apply {
                    put("hold_expires_at", expireMs)
                    put("held_by", heldBy)
                }
                db.update("bus_seats", cv, "seat_number = ?", arrayOf(sNum.toString()))
            }
            db.setTransactionSuccessful()
            Pair(true, "सीटें 5 मिनट के लिए आपके लिए होल्ड (लॉक) कर दी गई हैं।")
        } catch (e: Exception) {
            Pair(false, e.message ?: "होल्ड करने में त्रुटि")
        } finally {
            db.endTransaction()
        }
    }

    suspend fun releaseBusSeatsHold(
        seatNumbers: List<Int>,
        heldBy: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.releaseBusSeatsHoldRemote(
                yatraDate = "2026-04-15",
                seatNumbers = seatNumbers,
                deviceId = heldBy
            )
        } catch (e: Exception) {}

        val db = dbHelper.writableDatabase
        try {
            for (sNum in seatNumbers) {
                val cv = ContentValues().apply {
                    put("hold_expires_at", 0L)
                    put("held_by", "")
                }
                db.update("bus_seats", cv, "seat_number = ? AND (held_by = ? OR ? = '')", arrayOf(sNum.toString(), heldBy, heldBy))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun approveBusSeatBooking(seatNumber: Int, adminName: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("payment_status", PaymentStatus.PAID.name)
            put("notes", "सत्यापित द्वारा: $adminName")
        }
        val res = db.update("bus_seats", cv, "seat_number = ?", arrayOf(seatNumber.toString())) > 0
        if (res) {
            try {
                val cursor = db.query("bus_seats", arrayOf("transaction_id"), "seat_number = ?", arrayOf(seatNumber.toString()), null, null, null)
                var utr = ""
                if (cursor.moveToFirst()) {
                    utr = cursor.getString(0) ?: ""
                }
                cursor.close()
                if (utr.isNotBlank()) {
                    val pCv = ContentValues().apply {
                        put("payment_status", "CONFIRMED")
                        put("verified_by", adminName)
                    }
                    db.update("payment_records", pCv, "transaction_id = ?", arrayOf(utr))
                }
                publishBusSeatsToGitHub()
                publishPaymentsToGitHub()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        res
    }

    suspend fun rejectBusSeatBooking(seatNumber: Int, adminName: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        try {
            val cursor = db.query("bus_seats", arrayOf("transaction_id"), "seat_number = ?", arrayOf(seatNumber.toString()), null, null, null)
            var utr = ""
            if (cursor.moveToFirst()) {
                utr = cursor.getString(0) ?: ""
            }
            cursor.close()
            if (utr.isNotBlank()) {
                val pCv = ContentValues().apply {
                    put("payment_status", "REJECTED")
                    put("verified_by", adminName)
                }
                db.update("payment_records", pCv, "transaction_id = ?", arrayOf(utr))
                publishPaymentsToGitHub()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cancelBusSeatBooking(seatNumber)
    }

    suspend fun cancelBusSeatBooking(seatNumber: Int): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_booked", 0)
            put("passenger_name", "")
            put("passenger_age", 0)
            put("passenger_gender", "")
            put("phone_number", "")
            put("boarding_point", "")
            put("payment_status", PaymentStatus.UNPAID.name)
            put("payment_mode", "CASH")
            put("transaction_id", "")
            put("booked_at", 0L)
            put("booked_by", "DEVOTEE")
            put("notes", "")
        }
        val res = db.update("bus_seats", cv, "seat_number = ?", arrayOf(seatNumber.toString())) > 0
        if (res) {
            try {
                publishBusSeatsToGitHub()
            } catch (e: Exception) {}
        }
        res
    }

    // --- Payment Records Audit Ledger ---
    suspend fun recordPayment(payment: PaymentRecord): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("payment_id", payment.paymentId)
            put("devotee_name", payment.devoteeName)
            put("devotee_phone", payment.devoteePhone)
            put("payment_app", payment.paymentApp)
            put("transaction_id", payment.transactionId)
            put("amount", payment.amount)
            put("purpose", payment.purpose)
            put("seat_numbers", payment.seatNumbers)
            put("timestamp", payment.timestamp)
            put("payment_status", payment.paymentStatus)
            put("payment_mode", payment.paymentMode)
            put("verified_by", payment.verifiedBy)
            put("notes", payment.notes)
        }
        val id = db.insertWithOnConflict("payment_records", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        if (id > 0) {
            try {
                com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.savePayment(
                    receiptNumber = payment.paymentId,
                    devoteeName = payment.devoteeName,
                    phoneNumber = payment.devoteePhone,
                    amount = payment.amount,
                    purpose = payment.purpose,
                    paymentMode = payment.paymentMode,
                    transactionId = payment.transactionId,
                    status = payment.paymentStatus,
                    collectedBy = payment.verifiedBy,
                    notes = payment.notes
                )
            } catch (e: Exception) {}
            try {
                publishPaymentsToGitHub()
            } catch (e: Exception) {}
            try {
                com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager.postPaymentToSheet(appContext, payment)
            } catch (e: Exception) {}
        }
        id
    }

    suspend fun getAllPayments(): List<PaymentRecord> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<PaymentRecord>()
        val cursor = db.rawQuery("SELECT * FROM payment_records ORDER BY timestamp DESC", null)
        while (cursor.moveToNext()) {
            list.add(
                PaymentRecord(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    paymentId = cursor.getString(cursor.getColumnIndexOrThrow("payment_id")),
                    devoteeName = cursor.getString(cursor.getColumnIndexOrThrow("devotee_name")),
                    devoteePhone = cursor.getString(cursor.getColumnIndexOrThrow("devotee_phone")),
                    paymentApp = cursor.getString(cursor.getColumnIndexOrThrow("payment_app")),
                    transactionId = cursor.getString(cursor.getColumnIndexOrThrow("transaction_id")),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    purpose = cursor.getString(cursor.getColumnIndexOrThrow("purpose")),
                    seatNumbers = try { cursor.getString(cursor.getColumnIndexOrThrow("seat_numbers")) ?: "" } catch (e: Exception) { "" },
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    paymentStatus = try { cursor.getString(cursor.getColumnIndexOrThrow("payment_status")) ?: "SUCCESS" } catch (e: Exception) { "SUCCESS" },
                    paymentMode = try { cursor.getString(cursor.getColumnIndexOrThrow("payment_mode")) ?: "UPI_QR" } catch (e: Exception) { "UPI_QR" },
                    verifiedBy = try { cursor.getString(cursor.getColumnIndexOrThrow("verified_by")) ?: "" } catch (e: Exception) { "" },
                    notes = try { cursor.getString(cursor.getColumnIndexOrThrow("notes")) ?: "" } catch (e: Exception) { "" }
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun isTransactionIdAlreadyUsed(txId: String): Boolean = withContext(Dispatchers.IO) {
        val clean = txId.trim()
        if (clean.isBlank()) return@withContext false
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM payment_records WHERE LOWER(TRIM(transaction_id)) = LOWER(?)",
            arrayOf(clean)
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        count > 0
    }

    suspend fun updatePaymentStatus(paymentId: String, status: String, verifiedBy: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("payment_status", status)
                put("verified_by", verifiedBy)
            }
            val res = db.update("payment_records", cv, "payment_id = ?", arrayOf(paymentId)) > 0

            // Sync linked bus seats if any
            val cursor = db.rawQuery("SELECT seat_numbers FROM payment_records WHERE payment_id = ?", arrayOf(paymentId))
            var seatNumsStr = ""
            if (cursor.moveToFirst()) {
                seatNumsStr = cursor.getString(0) ?: ""
            }
            cursor.close()

            if (seatNumsStr.isNotBlank()) {
                val seatParts = seatNumsStr.split(",").map { it.trim().removePrefix("#") }.mapNotNull { it.toIntOrNull() }
                for (sNum in seatParts) {
                    if (status.equals("REJECTED", ignoreCase = true)) {
                        // Release seat
                        val sCv = ContentValues().apply {
                            put("is_booked", 0)
                            put("payment_status", PaymentStatus.UNPAID.name)
                            put("passenger_name", "")
                            put("phone_number", "")
                            put("transaction_id", "")
                        }
                        db.update("bus_seats", sCv, "seat_number = ?", arrayOf(sNum.toString()))
                    } else if (status.equals("VERIFIED", ignoreCase = true) || status.equals("SUCCESS", ignoreCase = true)) {
                        val sCv = ContentValues().apply {
                            put("payment_status", PaymentStatus.PAID.name)
                        }
                        db.update("bus_seats", sCv, "seat_number = ?", arrayOf(sNum.toString()))
                    }
                }
            }

            db.setTransactionSuccessful()
            res
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.endTransaction()
            try {
                publishPaymentsToGitHub()
                publishBusSeatsToGitHub()
            } catch (e: Exception) {}
        }
    }

    suspend fun deletePayment(paymentId: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val res = db.delete("payment_records", "payment_id = ?", arrayOf(paymentId)) > 0
        if (res) {
            try {
                publishPaymentsToGitHub()
            } catch (e: Exception) {}
        }
        res
    }

    suspend fun publishBusSeatsToGitHub(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val allSeats = getAllBusSeats()
        com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishLiveBusSeats(appContext, allSeats)
    }

    suspend fun syncLiveBusSeatsFromGitHub(): Pair<Boolean, List<BusSeat>> = withContext(Dispatchers.IO) {
        val remoteSeats = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.fetchLiveBusSeats(appContext)
        if (remoteSeats != null && remoteSeats.isNotEmpty()) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                for (s in remoteSeats) {
                    val cv = ContentValues().apply {
                        put("seat_number", s.seatNumber)
                        put("seat_label", s.seatLabel)
                        put("row_idx", s.row)
                        put("col_idx", s.column)
                        put("is_booked", if (s.isBooked) 1 else 0)
                        put("passenger_name", s.passengerName)
                        put("passenger_age", s.passengerAge)
                        put("passenger_gender", s.passengerGender)
                        put("phone_number", s.phoneNumber)
                        put("boarding_point", s.boardingPoint)
                        put("payment_status", s.paymentStatus.name)
                        put("payment_mode", s.paymentMode)
                        put("transaction_id", s.transactionId)
                        put("fare_amount", s.fareAmount)
                        put("yatra_date", s.yatraDate)
                        put("booked_at", s.bookedAt)
                        put("booked_by", s.bookedBy)
                        put("notes", s.notes)
                    }
                    db.insertWithOnConflict("bus_seats", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            Pair(true, remoteSeats)
        } else {
            Pair(false, emptyList())
        }
    }

    suspend fun publishPaymentsToGitHub(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val allPayments = getAllPayments()
        com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishLivePayments(appContext, allPayments)
    }

    suspend fun syncLivePaymentsFromGitHub(): Pair<Boolean, List<PaymentRecord>> = withContext(Dispatchers.IO) {
        val remotePayments = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.fetchLivePayments(appContext)
        if (remotePayments != null && remotePayments.isNotEmpty()) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                for (p in remotePayments) {
                    val cv = ContentValues().apply {
                        put("payment_id", p.paymentId)
                        put("devotee_name", p.devoteeName)
                        put("devotee_phone", p.devoteePhone)
                        put("payment_app", p.paymentApp)
                        put("transaction_id", p.transactionId)
                        put("amount", p.amount)
                        put("purpose", p.purpose)
                        put("seat_numbers", p.seatNumbers)
                        put("timestamp", p.timestamp)
                        put("payment_status", p.paymentStatus)
                        put("payment_mode", p.paymentMode)
                        put("verified_by", p.verifiedBy)
                        put("notes", p.notes)
                    }
                    db.insertWithOnConflict("payment_records", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            Pair(true, remotePayments)
        } else {
            Pair(false, emptyList())
        }
    }

    suspend fun updateBusAndPaymentSettings(
        isBusBookingLive: Boolean,
        isPaymentFeatureLive: Boolean,
        canAdminViewPaymentHistory: Boolean,
        canDevoteeViewPaymentHistory: Boolean,
        ashramUpiId: String,
        ashramUpiName: String,
        busSeatFareAmount: Int,
        customUpiQrUri: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_bus_booking_live", if (isBusBookingLive) 1 else 0)
            put("is_payment_feature_live", if (isPaymentFeatureLive) 1 else 0)
            put("can_admin_view_payment_history", if (canAdminViewPaymentHistory) 1 else 0)
            put("can_devotee_view_payment_history", if (canDevoteeViewPaymentHistory) 1 else 0)
            put("ashram_upi_id", ashramUpiId)
            put("ashram_upi_name", ashramUpiName)
            put("bus_seat_fare_amount", busSeatFareAmount)
            if (customUpiQrUri.isNotBlank()) put("custom_upi_qr_uri", customUpiQrUri)
        }
        val res = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (res) {
            persistCurrentSettingsToAllLayers()
        }
        res
    }

    suspend fun updateDharamshalaLiveStatus(isLive: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_dharamshala_live", if (isLive) 1 else 0)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) persistCurrentSettingsToAllLayers()
        ok
    }

    // --- Yatra Expenses ---
    suspend fun getAllYatraExpenses(): List<YatraExpense> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<YatraExpense>()
        val cursor = db.rawQuery("SELECT * FROM yatra_expenses ORDER BY created_at DESC", null)
        while (cursor.moveToNext()) {
            list.add(
                YatraExpense(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    category = ExpenseCategory.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("category"))),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    receiptUri = cursor.getString(cursor.getColumnIndexOrThrow("receipt_uri")) ?: "",
                    addedByAdminName = cursor.getString(cursor.getColumnIndexOrThrow("added_by")),
                    expenseDate = cursor.getString(cursor.getColumnIndexOrThrow("expense_date")),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun addYatraExpense(
        title: String,
        category: ExpenseCategory,
        amount: Double,
        receiptUri: String = "",
        addedBy: String
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("title", title)
            put("category", category.name)
            put("amount", amount)
            put("receipt_uri", receiptUri)
            put("added_by", addedBy)
            put("expense_date", DatabaseHelper.getTodayDateString())
            put("created_at", System.currentTimeMillis())
        }
        val ok = db.insert("yatra_expenses", null, cv) > 0
        if (ok) {
            try {
                com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.saveExpense(
                    title = title,
                    amount = amount,
                    category = category.name,
                    expenseDate = DatabaseHelper.getTodayDateString(),
                    spentBy = addedBy,
                    receiptPhotoUrl = receiptUri
                )
            } catch (e: Exception) {}
            try {
                val expObj = YatraExpense(
                    title = title,
                    category = category,
                    amount = amount,
                    receiptUri = receiptUri,
                    addedByAdminName = addedBy,
                    expenseDate = DatabaseHelper.getTodayDateString(),
                    createdAt = System.currentTimeMillis()
                )
                com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager.postExpenseToSheet(appContext, expObj)
            } catch (e: Exception) {}
            try {
                publishCurrentSettingsToGitHub("Auto Sync - Expense Added: $title")
            } catch (e: Exception) {}
        }
        ok
    }

    suspend fun getYatraFinancialSummary(): Triple<Double, Double, Double> = withContext(Dispatchers.IO) {
        val seats = getAllBusSeats()
        val expenses = getAllYatraExpenses()
        val totalCollection = seats.filter { it.isBooked && it.paymentStatus == PaymentStatus.PAID }
            .sumOf { it.fareAmount.toDouble() }
        val totalExpenses = expenses.sumOf { it.amount }
        val netBalance = totalCollection - totalExpenses
        Triple(totalCollection, totalExpenses, netBalance)
    }

    // --- Admin Authentication & Sevadar Management ---
    suspend fun authenticateAdmin(pin: String): Admin? = withContext(Dispatchers.IO) {
        val trimmedPin = pin.trim()
        val isMaster = DatabaseHelper.isMasterPin(trimmedPin)
        if (isMaster) {
            try {
                val wDb = dbHelper.writableDatabase
                wDb.execSQL("UPDATE admins SET pin_hash = ? WHERE role = 'SUPER_ADMIN' OR username = 'admin'", arrayOf(DatabaseHelper.MASTER_PIN_RAW_HASH))
            } catch (e: Exception) {}
        }
        val hashed = DatabaseHelper.hashPin(trimmedPin)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM admins WHERE pin_hash = ? AND is_active = 1 LIMIT 1", arrayOf(hashed))
        var admin: Admin? = null
        if (cursor.moveToFirst()) {
            admin = parseAdminCursor(cursor)
        } else if (isMaster) {
            // Direct fallback: Retrieve Super Admin
            val saCursor = db.rawQuery("SELECT * FROM admins WHERE role = 'SUPER_ADMIN' LIMIT 1", null)
            if (saCursor.moveToFirst()) {
                admin = parseAdminCursor(saCursor)
            }
            saCursor.close()
        }
        cursor.close()

        // STRICT SECURITY REQUIREMENT: SuperAdmin role can ONLY be opened by Master PIN
        if (admin?.role == AdminRole.SUPER_ADMIN && !isMaster) {
            admin = null
        }
        admin
    }

    suspend fun authenticateAdminByCredentials(username: String, password: String): Admin? = withContext(Dispatchers.IO) {
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()
        val isMasterPwd = DatabaseHelper.isMasterPassword(trimmedPass)
        if (trimmedUser.equals("admin", ignoreCase = true) && isMasterPwd) {
            try {
                val db = dbHelper.writableDatabase
                db.execSQL("UPDATE admins SET password_hash = ? WHERE role = 'SUPER_ADMIN'", arrayOf(DatabaseHelper.MASTER_PWD_SALTED_HASH))
            } catch (e: Exception) {}
        }
        val db = dbHelper.readableDatabase

        val passHash = DatabaseHelper.hashPassword(trimmedPass)
        val cursor = db.rawQuery(
            "SELECT * FROM admins WHERE LOWER(username) = LOWER(?) AND password_hash = ? AND is_active = 1 LIMIT 1",
            arrayOf(trimmedUser, passHash)
        )
        var admin: Admin? = null
        if (cursor.moveToFirst()) {
            admin = parseAdminCursor(cursor)
        } else if (trimmedUser.equals("admin", ignoreCase = true) && isMasterPwd) {
            val saCursor = db.rawQuery("SELECT * FROM admins WHERE role = 'SUPER_ADMIN' LIMIT 1", null)
            if (saCursor.moveToFirst()) {
                admin = parseAdminCursor(saCursor)
            }
            saCursor.close()
        }
        cursor.close()

        // STRICT SECURITY REQUIREMENT: SuperAdmin role can ONLY be opened by valid Master credentials
        if (admin?.role == AdminRole.SUPER_ADMIN && !isMasterPwd) {
            admin = null
        }
        admin
    }

    suspend fun authenticateSuperAdminByPasswordOnly(password: String): Admin? = withContext(Dispatchers.IO) {
        val trimmedPass = password.trim()
        val passHash = DatabaseHelper.hashPassword(trimmedPass)
        val isMaster = DatabaseHelper.isMasterPassword(trimmedPass)

        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM admins WHERE role = 'SUPER_ADMIN' AND (password_hash = ? OR ? = 1) AND is_active = 1 LIMIT 1",
            arrayOf(passHash, if (isMaster) "1" else "0")
        )
        var admin: Admin? = null
        if (cursor.moveToFirst()) {
            admin = parseAdminCursor(cursor)
        }
        cursor.close()

        // If local check failed, check Hostinger central server API directly
        if (admin == null) {
            try {
                val (serverOk, _) = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.loginAdminOnServer(appContext, "admin", trimmedPass)
                if (serverOk) {
                    val wDb = dbHelper.writableDatabase
                    val cv = android.content.ContentValues().apply {
                        put("name", "Ankit Chaudhary (Super Admin)")
                        put("username", "admin")
                        put("role", AdminRole.SUPER_ADMIN.name)
                        put("password_hash", passHash)
                        put("is_active", 1)
                        put("can_manage_tokens", 1)
                        put("can_issue_manual_tokens", 1)
                        put("can_manage_yatra", 1)
                        put("can_manage_expenses", 1)
                        put("can_change_location", 1)
                        put("can_send_notifications", 1)
                        put("can_edit_ashram_info", 1)
                        put("can_manage_admins", 1)
                    }
                    val updatedRows = wDb.update("admins", cv, "role = 'SUPER_ADMIN' OR username = 'admin'", null)
                    if (updatedRows == 0) {
                        wDb.insert("admins", null, cv)
                    }
                    admin = getSuperAdmin()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        admin
    }

    private fun parseAdminCursor(cursor: android.database.Cursor): Admin {
        return Admin(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
            phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
            role = AdminRole.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("role"))),
            pinHash = cursor.getString(cursor.getColumnIndexOrThrow("pin_hash")),
            passwordHash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash")),
            rawPin = try { cursor.getString(cursor.getColumnIndexOrThrow("raw_pin")) } catch (e: Exception) { "" } ?: "",
            rawPassword = try { cursor.getString(cursor.getColumnIndexOrThrow("raw_password")) } catch (e: Exception) { "" } ?: "",
            canManageTokens = cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_tokens")) == 1,
            canIssueManualTokens = cursor.getInt(cursor.getColumnIndexOrThrow("can_issue_manual_tokens")) == 1,
            canManageYatra = cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_yatra")) == 1,
            canManageExpenses = cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_expenses")) == 1,
            canChangeLocation = cursor.getInt(cursor.getColumnIndexOrThrow("can_change_location")) == 1,
            canSendNotifications = cursor.getInt(cursor.getColumnIndexOrThrow("can_send_notifications")) == 1,
            canEditAshramInfo = cursor.getInt(cursor.getColumnIndexOrThrow("can_edit_ashram_info")) == 1,
            canManageAdmins = cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_admins")) == 1,
            canViewDevoteePhotos = cursor.getInt(cursor.getColumnIndexOrThrow("can_view_devotee_photos")) == 1,
            canIssueTokensAnywhere = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_issue_tokens_anywhere")) == 1 } catch (e: Exception) { false },
            canScanPaperRegister = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_scan_paper_register")) == 1 } catch (e: Exception) { false },
            canManageParchas = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_parchas")) == 1 } catch (e: Exception) { false },
            canManageArzi = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_arzi")) == 1 } catch (e: Exception) { false },
            canManageHavan = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_havan")) == 1 } catch (e: Exception) { false },
            canCancelTokens = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_cancel_tokens")) == 1 } catch (e: Exception) { false },
            canDeleteTokens = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_delete_tokens")) == 1 } catch (e: Exception) { false },
            canSetCustomTokenNumber = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_custom_token_number")) == 1 } catch (e: Exception) { false },
            canExportPdf = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_export_pdf")) == 1 } catch (e: Exception) { true },
            canManageVoiceSettings = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_voice_settings")) == 1 } catch (e: Exception) { false },
            canManageDailyDarshan = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_daily_darshan")) == 1 } catch (e: Exception) { false },
            canManageLiveStudio = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_live_studio")) == 1 } catch (e: Exception) { false },
            canManageAartiBhajan = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_aarti_bhajan")) == 1 } catch (e: Exception) { false },
            canManageUiControl = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_ui_control")) == 1 } catch (e: Exception) { false },
            canManageTuesdayDarbar = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_tuesday_darbar")) == 1 } catch (e: Exception) { false },
            canManageIdCards = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_id_cards")) == 1 } catch (e: Exception) { false },
            canManageHelpdesk = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_helpdesk")) == 1 } catch (e: Exception) { false },
            canViewPaymentLedger = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_view_payment_ledger")) == 1 } catch (e: Exception) { false },
            canManageWebsite = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_website")) == 1 } catch (e: Exception) { false },
            canManageServicesToggles = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_services_toggles")) == 1 } catch (e: Exception) { false },
            canManageDistances = try { cursor.getInt(cursor.getColumnIndexOrThrow("can_manage_distances")) == 1 } catch (e: Exception) { false },
            photoUri = try { cursor.getString(cursor.getColumnIndexOrThrow("photo_uri")) } catch (e: Exception) { "" } ?: "",
            isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        )
    }

    suspend fun getAllAdmins(): List<Admin> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Admin>()
        val cursor = db.rawQuery("SELECT * FROM admins ORDER BY id ASC", null)
        while (cursor.moveToNext()) {
            list.add(parseAdminCursor(cursor))
        }
        cursor.close()
        list
    }

    suspend fun getSuperAdmin(): Admin? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM admins WHERE role = 'SUPER_ADMIN' LIMIT 1", null)
        var admin: Admin? = null
        if (cursor.moveToFirst()) {
            admin = parseAdminCursor(cursor)
        }
        cursor.close()
        admin
    }


    /**
     * STRICT CREDENTIAL UNIQUENESS VALIDATOR:
     * Guarantees that no two admins or sevadars can ever have identical
     * usernames, passwords, or PINs.
     */
    suspend fun validateUniqueCredentials(
        username: String,
        password: String?,
        pin: String?,
        excludeAdminId: Long? = null
    ): CredentialCheckResult = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cleanUsername = username.trim()

        // 1. Check Username uniqueness (case-insensitive)
        if (cleanUsername.isNotBlank()) {
            val userQuery = if (excludeAdminId != null) {
                "SELECT id, name FROM admins WHERE LOWER(username) = LOWER(?) AND id != ?"
            } else {
                "SELECT id, name FROM admins WHERE LOWER(username) = LOWER(?)"
            }
            val userArgs = if (excludeAdminId != null) arrayOf(cleanUsername, excludeAdminId.toString()) else arrayOf(cleanUsername)
            val userCursor = db.rawQuery(userQuery, userArgs)
            val exists = userCursor.moveToFirst()
            val existingName = if (exists) userCursor.getString(1) else null
            userCursor.close()
            if (exists) {
                return@withContext CredentialCheckResult(
                    isValid = false,
                    errorMessage = "⚠️ यूजर आईडी (Username) '$cleanUsername' पहले से $existingName द्वारा उपयोग में है! कृपया अलग यूजर आईडी दर्ज करें।"
                )
            }
        }

        // 2. Check Password uniqueness (no two accounts can share identical password)
        if (!password.isNullOrBlank()) {
            val hashedPass = DatabaseHelper.hashPassword(password.trim())
            val passQuery = if (excludeAdminId != null) {
                "SELECT id, name, username FROM admins WHERE password_hash = ? AND id != ?"
            } else {
                "SELECT id, name, username FROM admins WHERE password_hash = ?"
            }
            val passArgs = if (excludeAdminId != null) arrayOf(hashedPass, excludeAdminId.toString()) else arrayOf(hashedPass)
            val passCursor = db.rawQuery(passQuery, passArgs)
            val passExists = passCursor.moveToFirst()
            val existingName = if (passExists) passCursor.getString(1) else null
            passCursor.close()
            if (passExists) {
                return@withContext CredentialCheckResult(
                    isValid = false,
                    errorMessage = "⚠️ यह पासवर्ड पहले से $existingName के खाते में दर्ज है! नियमों के अनुसार प्रत्येक व्यवस्थापक/सेवादार का पासवर्ड पूर्णतः भिन्न (Unique) होना अनिवार्य है।"
                )
            }
        }

        // 3. Check PIN uniqueness (no two accounts can share identical PIN)
        if (!pin.isNullOrBlank()) {
            val hashedPin = DatabaseHelper.hashPin(pin.trim())
            val pinQuery = if (excludeAdminId != null) {
                "SELECT id, name, username FROM admins WHERE pin_hash = ? AND id != ?"
            } else {
                "SELECT id, name, username FROM admins WHERE pin_hash = ?"
            }
            val pinArgs = if (excludeAdminId != null) arrayOf(hashedPin, excludeAdminId.toString()) else arrayOf(hashedPin)
            val pinCursor = db.rawQuery(pinQuery, pinArgs)
            val pinExists = pinCursor.moveToFirst()
            val existingName = if (pinExists) pinCursor.getString(1) else null
            pinCursor.close()
            if (pinExists) {
                return@withContext CredentialCheckResult(
                    isValid = false,
                    errorMessage = "⚠️ यह सुरक्षा पिन (PIN) पहले से $existingName के खाते में दर्ज है! सभी व्यवस्थापकों व सेवादारों का पिन अलग होना चाहिए।"
                )
            }
        }

        CredentialCheckResult(isValid = true)
    }

    suspend fun createSevadarAdmin(
        name: String,
        username: String,
        phone: String,
        role: AdminRole,
        password: String,
        pin: String,
        canManageTokens: Boolean,
        canIssueManualTokens: Boolean,
        canManageYatra: Boolean,
        canManageExpenses: Boolean,
        canChangeLocation: Boolean,
        canSendNotifications: Boolean,
        canEditAshramInfo: Boolean,
        canViewDevoteePhotos: Boolean = false,
        canIssueTokensAnywhere: Boolean = false,
        canScanPaperRegister: Boolean = false,
        canManageParchas: Boolean = false,
        canManageArzi: Boolean = false,
        canManageHavan: Boolean = false,
        canCancelTokens: Boolean = false,
        canDeleteTokens: Boolean = false,
        canSetCustomTokenNumber: Boolean = false,
        canExportPdf: Boolean = true,
        canManageVoiceSettings: Boolean = false,
        canManageDailyDarshan: Boolean = false,
        canManageLiveStudio: Boolean = false,
        canManageAartiBhajan: Boolean = false,
        canManageUiControl: Boolean = false,
        canManageTuesdayDarbar: Boolean = false,
        canManageIdCards: Boolean = false,
        canManageHelpdesk: Boolean = false,
        canViewPaymentLedger: Boolean = false,
        canManageWebsite: Boolean = false,
        canManageServicesToggles: Boolean = false,
        canManageDistances: Boolean = false,
        photoUri: String = ""
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val check = validateUniqueCredentials(username, password, pin)
        if (!check.isValid) {
            return@withContext Pair(false, check.errorMessage ?: "क्रेडेंशियल्स अमान्य हैं")
        }

        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", name.trim())
            put("username", username.trim())
            put("phone", phone.trim())
            put("role", role.name)
            put("pin_hash", DatabaseHelper.hashPin(if (pin.isNotEmpty()) pin.trim() else "1234"))
            put("password_hash", DatabaseHelper.hashPassword(password.trim()))
            put("raw_pin", if (pin.isNotEmpty()) pin.trim() else "1234")
            put("raw_password", password.trim())
            put("can_manage_tokens", if (canManageTokens) 1 else 0)
            put("can_issue_manual_tokens", if (canIssueManualTokens) 1 else 0)
            put("can_manage_yatra", if (canManageYatra) 1 else 0)
            put("can_manage_expenses", if (canManageExpenses) 1 else 0)
            put("can_change_location", if (canChangeLocation) 1 else 0)
            put("can_send_notifications", if (canSendNotifications) 1 else 0)
            put("can_edit_ashram_info", if (canEditAshramInfo) 1 else 0)
            put("can_manage_admins", if (role == AdminRole.SUPER_ADMIN || role == AdminRole.SUB_ADMIN) 1 else 0)
            put("can_view_devotee_photos", if (canViewDevoteePhotos || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_issue_tokens_anywhere", if (canIssueTokensAnywhere || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_scan_paper_register", if (canScanPaperRegister || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_parchas", if (canManageParchas || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_arzi", if (canManageArzi || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_havan", if (canManageHavan || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_cancel_tokens", if (canCancelTokens || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_delete_tokens", if (canDeleteTokens || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_custom_token_number", if (canSetCustomTokenNumber || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_export_pdf", if (canExportPdf || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_voice_settings", if (canManageVoiceSettings || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_daily_darshan", if (canManageDailyDarshan || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_live_studio", if (canManageLiveStudio || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_aarti_bhajan", if (canManageAartiBhajan || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_ui_control", if (canManageUiControl || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_tuesday_darbar", if (canManageTuesdayDarbar || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_id_cards", if (canManageIdCards || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_helpdesk", if (role == AdminRole.SUB_ADMIN) 0 else if (canManageHelpdesk || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_view_payment_ledger", if (canViewPaymentLedger || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_website", if (canManageWebsite || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_services_toggles", if (canManageServicesToggles || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_distances", if (canManageDistances || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("photo_uri", photoUri.trim())
            put("is_active", 1)
            put("created_at", System.currentTimeMillis())
        }
        val inserted = db.insert("admins", null, cv) > 0
        if (inserted) {
            try { com.example.shribalajikripadham.data.local.AppPermanentVault.saveVault(appContext, getAllAdmins(), getSettings()) } catch (e: Exception) {}
            try { publishAdminsToGitHub() } catch (e: Exception) {}
            // Instant Push to Hostinger MySQL Central Server
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.saveAdminToCentralServer(
                        name = name.trim(),
                        username = username.trim(),
                        phone = phone.trim(),
                        role = role.name,
                        password = password.trim(),
                        pin = pin.trim(),
                        isActive = true
                    )
                } catch (_: Exception) {}
            }
        }
        Pair(inserted, if (inserted) "खाता सफलतापूर्वक बन गया!" else "डेटाबेस में सुरक्षित नहीं हो सका")
    }

    suspend fun updateAdminFullDetails(
        adminId: Long,
        name: String,
        username: String,
        phone: String,
        password: String? = null,
        pin: String? = null,
        photoUri: String? = null,
        permissions: AdminPermissionsUpdate? = null,
        role: AdminRole? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val check = validateUniqueCredentials(
            username = username,
            password = if (!password.isNullOrBlank()) password else null,
            pin = if (!pin.isNullOrBlank()) pin else null,
            excludeAdminId = adminId
        )
        if (!check.isValid) {
            return@withContext Pair(false, check.errorMessage ?: "क्रेडेंशियल्स अमान्य हैं")
        }

        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", name.trim())
            put("username", username.trim())
            put("phone", phone.trim())
            if (role != null) {
                put("role", role.name)
                put("can_manage_admins", if (role == AdminRole.SUPER_ADMIN || role == AdminRole.SUB_ADMIN) 1 else 0)
                if (role == AdminRole.SUB_ADMIN) {
                    put("can_manage_helpdesk", 0)
                }
            }
            if (!password.isNullOrBlank()) {
                put("password_hash", DatabaseHelper.hashPassword(password.trim()))
                put("raw_password", password.trim())
            }
            if (!pin.isNullOrBlank()) {
                put("pin_hash", DatabaseHelper.hashPin(pin.trim()))
                put("raw_pin", pin.trim())
            }
            if (photoUri != null) {
                put("photo_uri", photoUri.trim())
            }
            permissions?.let { p ->
                put("can_manage_tokens", if (p.canManageTokens) 1 else 0)
                put("can_issue_manual_tokens", if (p.canIssueManualTokens) 1 else 0)
                put("can_manage_yatra", if (p.canManageYatra) 1 else 0)
                put("can_manage_expenses", if (p.canManageExpenses) 1 else 0)
                put("can_change_location", if (p.canChangeLocation) 1 else 0)
                put("can_send_notifications", if (p.canSendNotifications) 1 else 0)
                put("can_edit_ashram_info", if (p.canEditAshramInfo) 1 else 0)
                put("can_view_devotee_photos", if (p.canViewDevoteePhotos) 1 else 0)
                put("can_issue_tokens_anywhere", if (p.canIssueTokensAnywhere) 1 else 0)
                put("can_scan_paper_register", if (p.canScanPaperRegister) 1 else 0)
                put("can_manage_parchas", if (p.canManageParchas) 1 else 0)
                put("can_manage_arzi", if (p.canManageArzi) 1 else 0)
                put("can_manage_havan", if (p.canManageHavan) 1 else 0)
                put("can_cancel_tokens", if (p.canCancelTokens) 1 else 0)
                put("can_delete_tokens", if (p.canDeleteTokens) 1 else 0)
                put("can_custom_token_number", if (p.canSetCustomTokenNumber) 1 else 0)
                put("can_export_pdf", if (p.canExportPdf) 1 else 0)
                put("can_manage_voice_settings", if (p.canManageVoiceSettings) 1 else 0)
                put("can_manage_daily_darshan", if (p.canManageDailyDarshan) 1 else 0)
                put("can_manage_live_studio", if (p.canManageLiveStudio) 1 else 0)
                put("can_manage_aarti_bhajan", if (p.canManageAartiBhajan) 1 else 0)
                put("can_manage_ui_control", if (p.canManageUiControl) 1 else 0)
                put("can_manage_tuesday_darbar", if (p.canManageTuesdayDarbar) 1 else 0)
                put("can_manage_id_cards", if (p.canManageIdCards) 1 else 0)
                if (role == AdminRole.SUB_ADMIN) {
                    put("can_manage_helpdesk", 0)
                } else {
                    put("can_manage_helpdesk", if (p.canManageHelpdesk) 1 else 0)
                }
                put("can_view_payment_ledger", if (p.canViewPaymentLedger) 1 else 0)
                put("can_manage_website", if (p.canManageWebsite) 1 else 0)
                put("can_manage_services_toggles", if (p.canManageServicesToggles) 1 else 0)
                put("can_manage_distances", if (p.canManageDistances) 1 else 0)
                put("is_active", if (p.isActive) 1 else 0)
            }
        }
        val count = db.update("admins", cv, "id = ?", arrayOf(adminId.toString()))
        val ok = count > 0
        if (ok) {
            try { com.example.shribalajikripadham.data.local.AppPermanentVault.saveVault(appContext, getAllAdmins(), getSettings()) } catch (e: Exception) {}
            try { publishAdminsToGitHub() } catch (e: Exception) {}
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.saveAdminToCentralServer(
                        name = name.trim(),
                        username = username.trim(),
                        phone = phone.trim(),
                        role = role?.name ?: "SEVADAR",
                        password = password?.trim() ?: "",
                        pin = pin?.trim() ?: "",
                        isActive = permissions?.isActive ?: true
                    )
                } catch (_: Exception) {}
            }
        }
        Pair(ok, if (ok) "विवरण सफलतापूर्वक सुरक्षित हुआ!" else "डेटाबेस में अपडेट नहीं हो सका")
    }

    suspend fun updateSuperAdminProfile(
        name: String,
        phone: String,
        username: String,
        password: String?,
        pin: String?,
        photoUri: String?
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val superAdmin = getSuperAdmin()
        if (superAdmin == null) {
            return@withContext Pair(false, "सुपर एडमिन खाता नहीं मिला")
        }
        updateAdminFullDetails(
            adminId = superAdmin.id,
            name = name,
            username = username,
            phone = phone,
            password = password,
            pin = pin,
            photoUri = photoUri
        )
    }

    suspend fun updateAdminPhoto(adminId: Long, photoUri: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("photo_uri", photoUri.trim())
        }
        db.update("admins", cv, "id = ?", arrayOf(adminId.toString())) > 0
    }

    suspend fun updateAdminPermissions(
        adminId: Long,
        canManageTokens: Boolean,
        canIssueManualTokens: Boolean,
        canManageYatra: Boolean,
        canManageExpenses: Boolean,
        canChangeLocation: Boolean,
        canSendNotifications: Boolean,
        canEditAshramInfo: Boolean,
        canViewDevoteePhotos: Boolean,
        canIssueTokensAnywhere: Boolean = false,
        canScanPaperRegister: Boolean = false,
        canManageParchas: Boolean = false,
        canManageArzi: Boolean = false,
        canManageHavan: Boolean = false,
        canCancelTokens: Boolean = false,
        canDeleteTokens: Boolean = false,
        canSetCustomTokenNumber: Boolean = false,
        canExportPdf: Boolean = true,
        canManageVoiceSettings: Boolean = false,
        canManageDailyDarshan: Boolean = false,
        canManageLiveStudio: Boolean = false,
        canManageAartiBhajan: Boolean = false,
        canManageUiControl: Boolean = false,
        canManageTuesdayDarbar: Boolean = false,
        canManageIdCards: Boolean = false,
        canManageHelpdesk: Boolean = false,
        canViewPaymentLedger: Boolean = false,
        canManageWebsite: Boolean = false,
        canManageServicesToggles: Boolean = false,
        canManageDistances: Boolean = false,
        isActive: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("can_manage_tokens", if (canManageTokens) 1 else 0)
            put("can_issue_manual_tokens", if (canIssueManualTokens) 1 else 0)
            put("can_manage_yatra", if (canManageYatra) 1 else 0)
            put("can_manage_expenses", if (canManageExpenses) 1 else 0)
            put("can_change_location", if (canChangeLocation) 1 else 0)
            put("can_send_notifications", if (canSendNotifications) 1 else 0)
            put("can_edit_ashram_info", if (canEditAshramInfo) 1 else 0)
            put("can_view_devotee_photos", if (canViewDevoteePhotos) 1 else 0)
            put("can_issue_tokens_anywhere", if (canIssueTokensAnywhere) 1 else 0)
            put("can_scan_paper_register", if (canScanPaperRegister) 1 else 0)
            put("can_manage_parchas", if (canManageParchas) 1 else 0)
            put("can_manage_arzi", if (canManageArzi) 1 else 0)
            put("can_manage_havan", if (canManageHavan) 1 else 0)
            put("can_cancel_tokens", if (canCancelTokens) 1 else 0)
            put("can_delete_tokens", if (canDeleteTokens) 1 else 0)
            put("can_custom_token_number", if (canSetCustomTokenNumber) 1 else 0)
            put("can_export_pdf", if (canExportPdf) 1 else 0)
            put("can_manage_voice_settings", if (canManageVoiceSettings) 1 else 0)
            put("can_manage_daily_darshan", if (canManageDailyDarshan) 1 else 0)
            put("can_manage_live_studio", if (canManageLiveStudio) 1 else 0)
            put("can_manage_aarti_bhajan", if (canManageAartiBhajan) 1 else 0)
            put("can_manage_ui_control", if (canManageUiControl) 1 else 0)
            put("can_manage_tuesday_darbar", if (canManageTuesdayDarbar) 1 else 0)
            put("can_manage_id_cards", if (canManageIdCards) 1 else 0)
            put("can_manage_helpdesk", if (canManageHelpdesk) 1 else 0)
            put("can_view_payment_ledger", if (canViewPaymentLedger) 1 else 0)
            put("can_manage_website", if (canManageWebsite) 1 else 0)
            put("can_manage_services_toggles", if (canManageServicesToggles) 1 else 0)
            put("can_manage_distances", if (canManageDistances) 1 else 0)
            put("is_active", if (isActive) 1 else 0)
        }
        val updated = db.update("admins", cv, "id = ?", arrayOf(adminId.toString())) > 0
        if (updated) {
            try { publishAdminsToGitHub() } catch (e: Exception) {}
        }
        updated
    }

    suspend fun updateAdminParchaPermission(adminId: Long, canManageParchas: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("can_manage_parchas", if (canManageParchas) 1 else 0)
        }
        db.update("admins", cv, "id = ?", arrayOf(adminId.toString())) > 0
    }

    suspend fun updateAdminHavanPermission(adminId: Long, canManageHavan: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("can_manage_havan", if (canManageHavan) 1 else 0)
        }
        db.update("admins", cv, "id = ?", arrayOf(adminId.toString())) > 0
    }

    suspend fun updateAdminAnywhereTokenPermission(adminId: Long, canIssueAnywhere: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("can_issue_tokens_anywhere", if (canIssueAnywhere) 1 else 0)
        }
        db.update("admins", cv, "id = ?", arrayOf(adminId.toString())) > 0
    }

    suspend fun updateAdminScanRegisterPermission(adminId: Long, canScan: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("can_scan_paper_register", if (canScan) 1 else 0)
        }
        db.update("admins", cv, "id = ?", arrayOf(adminId.toString())) > 0
    }

    suspend fun deleteAdmin(adminId: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val res = db.delete("admins", "id = ?", arrayOf(adminId.toString())) > 0
        if (res) {
            try { com.example.shribalajikripadham.data.local.AppPermanentVault.saveVault(appContext, getAllAdmins(), getSettings()) } catch (e: Exception) {}
            try { publishAdminsToGitHub() } catch (e: Exception) {}
        }
        res
    }

    // --- Dynamic Ashram Events ---
    suspend fun getAllEvents(): List<AshramEvent> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<AshramEvent>()
        val cursor = db.rawQuery("SELECT * FROM ashram_events ORDER BY id ASC", null)
        while (cursor.moveToNext()) {
            list.add(
                AshramEvent(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    titleHindi = cursor.getString(cursor.getColumnIndexOrThrow("title_hindi")),
                    titleEnglish = cursor.getString(cursor.getColumnIndexOrThrow("title_english")),
                    dateDescriptionHindi = cursor.getString(cursor.getColumnIndexOrThrow("date_desc_hindi")),
                    dateDescriptionEnglish = cursor.getString(cursor.getColumnIndexOrThrow("date_desc_english")),
                    detailsHindi = cursor.getString(cursor.getColumnIndexOrThrow("details_hindi")),
                    detailsEnglish = cursor.getString(cursor.getColumnIndexOrThrow("details_english")),
                    isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1,
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun addEvent(
        titleHindi: String,
        titleEnglish: String,
        dateDescHindi: String,
        dateDescEnglish: String,
        detailsHindi: String,
        detailsEnglish: String
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("title_hindi", titleHindi)
            put("title_english", titleEnglish)
            put("date_desc_hindi", dateDescHindi)
            put("date_desc_english", dateDescEnglish)
            put("details_hindi", detailsHindi)
            put("details_english", detailsEnglish)
            put("is_active", 1)
            put("created_at", System.currentTimeMillis())
        }
        db.insert("ashram_events", null, cv) > 0
    }

    suspend fun deleteEvent(eventId: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("ashram_events", "id = ?", arrayOf(eventId.toString())) > 0
    }

    // --- Broadcast Notifications ---
    suspend fun getAllNotifications(): List<AppNotification> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<AppNotification>()
        val cursor = db.rawQuery("SELECT * FROM app_notifications ORDER BY timestamp DESC", null)
        while (cursor.moveToNext()) {
            list.add(
                AppNotification(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    message = cursor.getString(cursor.getColumnIndexOrThrow("message")),
                    priority = cursor.getString(cursor.getColumnIndexOrThrow("priority")),
                    sentBy = cursor.getString(cursor.getColumnIndexOrThrow("sent_by")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    isRead = cursor.getInt(cursor.getColumnIndexOrThrow("is_read")) == 1
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun saveBroadcastNotification(
        title: String,
        message: String,
        priority: String = "HIGH",
        sentBy: String = "Super Admin"
    ): AppNotification = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("title", title)
            put("message", message)
            put("priority", priority)
            put("sent_by", sentBy)
            put("timestamp", System.currentTimeMillis())
            put("is_read", 0)
        }
        val id = db.insert("app_notifications", null, cv)
        AppNotification(
            id = id,
            title = title,
            message = message,
            priority = priority,
            sentBy = sentBy,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
    }

    // --- Facial Recognition & Devotee Face Profiles ---

    suspend fun getAllFaceProfiles(): List<DevoteeFaceProfile> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DevoteeFaceProfile>()
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM devotee_face_profiles ORDER BY last_verified_at DESC", null)
            while (cursor.moveToNext()) {
                try {
                    val blobIdx = cursor.getColumnIndex("face_vector")
                    val blob = if (blobIdx >= 0) cursor.getBlob(blobIdx) else null
                    val vector = FaceEmbeddingEngine.blobToVector(blob)

                    val idIdx = cursor.getColumnIndex("id")
                    val nameIdx = cursor.getColumnIndex("patient_name")
                    val phoneIdx = cursor.getColumnIndex("phone_number")
                    val cityIdx = cursor.getColumnIndex("city")
                    val photoIdx = cursor.getColumnIndex("photo_uri")
                    val visitIdx = cursor.getColumnIndex("visit_count")
                    val confIdx = cursor.getColumnIndex("last_confidence")
                    val verIdx = cursor.getColumnIndex("last_verified_at")
                    val createdIdx = cursor.getColumnIndex("created_at")

                    list.add(
                        DevoteeFaceProfile(
                            id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L,
                            patientName = if (nameIdx >= 0) (cursor.getString(nameIdx) ?: "") else "",
                            phoneNumber = if (phoneIdx >= 0) (cursor.getString(phoneIdx) ?: "") else "",
                            city = if (cityIdx >= 0) (cursor.getString(cityIdx) ?: "डूँगरा जाट (स्थानीय)") else "डूँगरा जाट (स्थानीय)",
                            faceVector = vector,
                            photoUri = if (photoIdx >= 0) (cursor.getString(photoIdx) ?: "") else "",
                            visitCount = if (visitIdx >= 0) cursor.getInt(visitIdx) else 1,
                            lastConfidence = if (confIdx >= 0) cursor.getFloat(confIdx) else 1.0f,
                            lastVerifiedAt = if (verIdx >= 0) cursor.getLong(verIdx) else System.currentTimeMillis(),
                            createdAt = if (createdIdx >= 0) cursor.getLong(createdIdx) else System.currentTimeMillis()
                        )
                    )
                } catch (rowEx: Exception) {
                    rowEx.printStackTrace()
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun matchFaceVector(
        candidateVector: FloatArray,
        threshold: Float = FaceEmbeddingEngine.MINIMUM_CONFIDENCE_THRESHOLD
    ): FaceMatchResult? = withContext(Dispatchers.Default) {
        try {
            val profiles = getAllFaceProfiles()
            FaceEmbeddingEngine.findBestMatch(candidateVector, profiles, threshold)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun enrollFaceProfile(
        name: String,
        phone: String,
        faceVector: FloatArray,
        city: String = "डूँगरा जाट (स्थानीय)",
        photoUri: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val normalized = FaceEmbeddingEngine.l2Normalize(faceVector)
        val safeCity = if (city.isBlank()) "डूँगरा जाट (स्थानीय)" else city.trim()
        val cloudPhotoUrl = if (photoUri.isNotBlank() && !photoUri.startsWith("http://") && !photoUri.startsWith("https://")) {
            try {
                val rawPath = photoUri.removePrefix("file://")
                val f = java.io.File(rawPath)
                if (f.exists() && f.length() > 0) {
                    com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.uploadPhoto(f) ?: photoUri
                } else photoUri
            } catch (e: Exception) { photoUri }
        } else photoUri

        val cv = ContentValues().apply {
            put("patient_name", name)
            put("phone_number", phone)
            put("city", safeCity)
            put("face_vector", FaceEmbeddingEngine.vectorToBlob(normalized))
            put("photo_uri", cloudPhotoUrl)
            put("visit_count", 1)
            put("last_confidence", 1.0f)
            put("last_verified_at", System.currentTimeMillis())
            put("created_at", System.currentTimeMillis())
        }
        val insertId = db.insert("devotee_face_profiles", null, cv)
        if (insertId > 0) {
            try {
                com.example.shribalajikripadham.data.network.CentralFaceSyncManager.uploadFaceProfile(
                    appContext, name, phone, safeCity, normalized, cloudPhotoUrl
                )
            } catch (e: Exception) {}
        }
        insertId
    }

    /**
     * AUTO-UPDATE FACE EMBEDDING (Online Adaptive Learning):
     * When user confirms their details, this enriches their existing face vector using
     * Exponential Moving Average (EMA) with alpha=0.75 and updates SQLite BLOB.
     */
    suspend fun autoUpdateFaceProfile(
        profileId: Long,
        newCandidateVector: FloatArray,
        confidence: Float,
        newPhotoUri: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cursor = db.rawQuery("SELECT face_vector, visit_count FROM devotee_face_profiles WHERE id = ?", arrayOf(profileId.toString()))
        if (cursor.moveToFirst()) {
            val existingBlob = cursor.getBlob(0)
            val currentVisitCount = cursor.getInt(1)
            cursor.close()

            val existingVector = FaceEmbeddingEngine.blobToVector(existingBlob)
            val enrichedVector = FaceEmbeddingEngine.enrichEmbedding(existingVector, newCandidateVector)
            val enrichedBlob = FaceEmbeddingEngine.vectorToBlob(enrichedVector)

            val cloudPhotoUrl = if (newPhotoUri.isNotBlank() && !newPhotoUri.startsWith("http://") && !newPhotoUri.startsWith("https://")) {
                try {
                    val rawPath = newPhotoUri.removePrefix("file://")
                    val f = java.io.File(rawPath)
                    if (f.exists() && f.length() > 0) {
                        com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.uploadPhoto(f) ?: newPhotoUri
                    } else newPhotoUri
                } catch (e: Exception) { newPhotoUri }
            } else newPhotoUri

            val cv = ContentValues().apply {
                put("face_vector", enrichedBlob)
                put("visit_count", currentVisitCount + 1)
                put("last_confidence", confidence)
                put("last_verified_at", System.currentTimeMillis())
                if (cloudPhotoUrl.isNotBlank()) {
                    put("photo_uri", cloudPhotoUrl)
                }
            }
            val updated = db.update("devotee_face_profiles", cv, "id = ?", arrayOf(profileId.toString())) > 0
            if (updated) {
                try {
                    val pCursor = db.rawQuery("SELECT patient_name, phone_number, city FROM devotee_face_profiles WHERE id = ?", arrayOf(profileId.toString()))
                    if (pCursor.moveToFirst()) {
                        val pName = pCursor.getString(0) ?: ""
                        val pPhone = pCursor.getString(1) ?: ""
                        val pCity = pCursor.getString(2) ?: ""
                        pCursor.close()
                        com.example.shribalajikripadham.data.network.CentralFaceSyncManager.uploadFaceProfile(
                            appContext, pName, pPhone, pCity, enrichedVector, cloudPhotoUrl
                        )
                    } else {
                        pCursor.close()
                    }
                } catch (e: Exception) {}
            }
            updated
        } else {
            cursor.close()
            false
        }
    }

    suspend fun syncCentralFaceProfiles(): Int = withContext(Dispatchers.IO) {
        com.example.shribalajikripadham.data.network.CentralFaceSyncManager.fetchAndSyncFaceProfiles(appContext)
    }

    /**
     * Instant Token Generation from Verified Face Match:
     * 1. Validates geofence.
     * 2. Issues Sunday Token.
     * 3. Triggers background Auto-Update of devotee face profile embedding.
     */
    suspend fun confirmFaceAndGenerateToken(
        matchedProfile: DevoteeFaceProfile,
        deviceId: String,
        latitude: Double,
        longitude: Double,
        candidateVector: FloatArray,
        photoUri: String = "",
        isMockLocation: Boolean = false,
        locationAccuracy: Float = 10.0f,
        darbarVenue: String = "DUNGRA_JAAT"
    ): Pair<Token, Boolean> = withContext(Dispatchers.IO) {
        val settings = getSettings()
        val isTuesday = darbarVenue.equals("BULANDSHAHR", ignoreCase = true)
        val targetLat = if (isTuesday && settings.tuesdayLatitude != 0.0) settings.tuesdayLatitude else settings.latitude
        val targetLng = if (isTuesday && settings.tuesdayLongitude != 0.0) settings.tuesdayLongitude else settings.longitude
        val outstationKm = if (isTuesday) settings.tuesdayOutstationMinDistanceKm else settings.outstationMinDistanceKm

        val distM = if (latitude != 0.0 && longitude != 0.0 && targetLat != 0.0) {
            GeofenceLocationManager.calculateDistanceMeters(latitude, longitude, targetLat, targetLng)
        } else -1.0
        val finalCity = if (distM > outstationKm * 1000.0) {
            val autoCity = GeofenceLocationManager.resolveVillageAndCity(appContext, latitude, longitude)
            if (autoCity.isNotBlank()) autoCity else matchedProfile.city
        } else {
            matchedProfile.city.ifBlank { if (isTuesday) "बुलन्दशहर (स्थानीय)" else "डूँगरा जाट (स्थानीय)" }
        }

        // Register token with Server-Side Geofence, Mock Location, and Hardware Device Locking
        val token = registerToken(
            patientName = matchedProfile.patientName,
            phoneNumber = matchedProfile.phoneNumber,
            city = finalCity,
            deviceId = deviceId,
            latitude = latitude,
            longitude = longitude,
            registeredBy = "FACIAL_SCAN",
            photoUri = if (photoUri.isNotBlank()) photoUri else matchedProfile.photoUri,
            isMockLocation = isMockLocation,
            locationAccuracy = locationAccuracy,
            darbarVenue = darbarVenue
        )

        // Trigger Auto-Update / Profile Enrichment
        val updated = autoUpdateFaceProfile(
            profileId = matchedProfile.id,
            newCandidateVector = candidateVector,
            confidence = matchedProfile.lastConfidence,
            newPhotoUri = photoUri
        )

        Pair(token, updated)
    }

    // --- Token Cancellation & Permanent Deletion with Complete Audit Ledger ---
    suspend fun cancelToken(tokenId: Long, adminName: String = "SEVADAR", reason: String = "भक्त अनुपस्थित / निरस्त"): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        var tokenNum = 0
        var darbarDate = ""
        val cur = db.rawQuery("SELECT token_number, darbar_date FROM tokens WHERE id = ?", arrayOf(tokenId.toString()))
        if (cur.moveToFirst()) {
            tokenNum = cur.getInt(0)
            darbarDate = cur.getString(1)
        }
        cur.close()

        val cv = ContentValues().apply {
            put("status", TokenStatus.CANCELLED.name)
            put("is_darshan_completed", 0)
        }
        val updated = db.update("tokens", cv, "id = ?", arrayOf(tokenId.toString())) > 0
        if (updated && tokenNum > 0) {
            // 📜 Log to Audit Trail Ledger
            try {
                val role = if (adminName.contains("SUPER", true) || adminName.contains("अंकित", true)) "SUPER_ADMIN" else "SEVADAR"
                dbHelper.insertAuditLog(
                    action = "TOKEN_CANCELLED",
                    tokenNumber = tokenNum,
                    performedBy = adminName,
                    role = role,
                    reason = reason,
                    darbarDate = darbarDate,
                    details = "टोकन #$tokenNum निरस्त किया गया (Reason: $reason)"
                )
            } catch (e: Exception) {}

            try {
                com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.updateLiveTokenStatusInGitHub(
                    appContext, tokenNum, darbarDate, TokenStatus.CANCELLED
                )
            } catch (e: Exception) {}
            try {
                com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager.updateTokenStatusInSheet(
                    appContext, darbarDate, tokenNum, TokenStatus.CANCELLED.name, "रद्द (CANCELLED)"
                )
            } catch (e: Exception) {}
            try {
                com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.updateCentralTokenStatus(
                    tokenNum, darbarDate, TokenStatus.CANCELLED.name, false
                )
            } catch (e: Exception) {}
        }
        updated
    }

    suspend fun deleteToken(tokenId: Long, adminName: String = "SEVADAR", reason: String = "रिकॉर्ड हटाया गया"): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        var tokenNum = 0
        var darbarDate = ""
        var deviceId = ""
        val cur = db.rawQuery("SELECT token_number, darbar_date, device_id FROM tokens WHERE id = ?", arrayOf(tokenId.toString()))
        if (cur.moveToFirst()) {
            tokenNum = cur.getInt(0)
            darbarDate = cur.getString(1) ?: ""
            deviceId = if (cur.columnCount > 2) cur.getString(2) ?: "" else ""
        }
        cur.close()

        val deleted = db.delete("tokens", "id = ?", arrayOf(tokenId.toString())) > 0
        if (deleted && tokenNum > 0) {
            // 🗑️ Delete persistent device receipt on disk to eradicate old token
            try {
                if (deviceId.isNotBlank() && darbarDate.isNotBlank()) {
                    com.example.shribalajikripadham.hardware.PersistentTokenReceiptHelper.deleteReceipt(deviceId, darbarDate)
                }
            } catch (e: Exception) {}

            // Clean up device_registrations entry for this token
            try {
                if (darbarDate.isNotBlank()) {
                    db.delete("device_registrations", "darbar_date = ? AND token_number = ?", arrayOf(darbarDate, tokenNum.toString()))
                }
            } catch (e: Exception) {}

            // 📜 Log to Audit Trail Ledger
            try {
                val role = if (adminName.contains("SUPER", true) || adminName.contains("अंकित", true)) "SUPER_ADMIN" else "SEVADAR"
                dbHelper.insertAuditLog(
                    action = "TOKEN_DELETED",
                    tokenNumber = tokenNum,
                    performedBy = adminName,
                    role = role,
                    reason = reason,
                    darbarDate = darbarDate,
                    details = "टोकन #$tokenNum डेटाबेस से स्थायी रूप से हटाया गया"
                )
            } catch (e: Exception) {}

            try {
                com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.removeLiveTokenFromGitHub(
                    appContext, tokenNum, darbarDate
                )
            } catch (e: Exception) {}
            try {
                // Delete from Hostinger Central MySQL
                com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.deleteCentralToken(
                    tokenNum, darbarDate
                )
            } catch (e: Exception) {}
            try {
                // Keep immutable audit trail in Google Sheet
                com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager.updateTokenStatusInSheet(
                    appContext, darbarDate, tokenNum, "CANCELLED", "एडमिन द्वारा हटाया गया ($adminName)"
                )
            } catch (e: Exception) {}
        }
        deleted
    }

    /**
     * SuperAdmin: Delete ALL tokens for a selected Sunday/date (Both local and Hostinger Central)
     */
    suspend fun deleteAllTokensForDate(darbarDate: String, adminName: String = "SUPER_ADMIN"): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        try {
            val localCount = db.delete("tokens", "darbar_date = ?", arrayOf(darbarDate))
            try {
                db.delete("device_registrations", "darbar_date = ?", arrayOf(darbarDate))
            } catch (e: Exception) {}

            try {
                val cv = ContentValues().apply { put("running_token_number", 1) }
                db.update("ashram_settings", cv, "id = 1", null)
            } catch (e: Exception) {}

            try {
                com.example.shribalajikripadham.hardware.PersistentTokenReceiptHelper.clearAllReceipts(appContext)
            } catch (e: Exception) {}

            val centralRes = try {
                com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.deleteAllCentralTokensForDate(darbarDate)
            } catch (e: Exception) {
                Pair(true, "सर्वर सिंक")
            }

            try {
                dbHelper.insertAuditLog(
                    action = "SUNDAY_TOKENS_WIPED",
                    tokenNumber = 0,
                    performedBy = adminName,
                    role = "SUPER_ADMIN",
                    reason = "रविवार ($darbarDate) के टोकन मिटाए गए",
                    darbarDate = darbarDate,
                    details = "दिनांक $darbarDate का टोकन डेटा सुपरएडमिन द्वारा स्थायी रूप से साफ़ किया गया (हटाए गए: $localCount)"
                )
            } catch (e: Exception) {}

            Pair(true, "दिनांक $darbarDate का संपूर्ण टोकन डेटा सफलतापूर्वक साफ़ कर दिया गया।")
        } catch (e: Exception) {
            Pair(false, "हटाने में त्रुटि: ${e.localizedMessage}")
        }
    }

    /**
     * SuperAdmin: Delete ALL tokens across ALL dates (Master Wipe)
     * Resets running token number to 1, completely empties tokens and device_registrations tables,
     * clears persistent token receipts, and invokes Hostinger Central DELETE_ALL_HISTORY.
     */
    suspend fun deleteAllTokensAllTime(adminName: String = "SUPER_ADMIN"): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        try {
            val localCount = db.delete("tokens", null, null)
            try {
                db.delete("device_registrations", null, null)
            } catch (e: Exception) {}

            try {
                val cv = ContentValues().apply {
                    put("running_token_number", 1)
                    put("current_serving_token", 1)
                    put("tuesday_running_token_number", 1)
                    put("tuesday_current_serving_token", 1)
                }
                db.update("ashram_settings", cv, "id = 1", null)
            } catch (e: Exception) {}

            try {
                com.example.shribalajikripadham.hardware.PersistentTokenReceiptHelper.clearAllReceipts(appContext)
            } catch (e: Exception) {}

            val centralRes = try {
                com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.deleteAllCentralTokensHistory()
            } catch (e: Exception) {
                Pair(true, "सर्वर सिंक")
            }

            try {
                dbHelper.insertAuditLog(
                    action = "ALL_TOKENS_MASTER_WIPED",
                    tokenNumber = 0,
                    performedBy = adminName,
                    role = "SUPER_ADMIN",
                    reason = "आज तक के समस्त टोकन मिटाए गए (Master Wipe)",
                    darbarDate = "ALL_TIME",
                    details = "सुपरएडमिन ($adminName) द्वारा आज तक के सभी तिथियों के समस्त टोकन स्थायी रूप से मिटा दिए गए। (स्थानीय हटाए गए: $localCount)"
                )
            } catch (e: Exception) {}

            Pair(true, "आज तक के समस्त टोकन (कुल $localCount) डेटाबेस व केंद्रीय सर्वर से हमेशा के लिए साफ़ कर दिए गए।")
        } catch (e: Exception) {
            Pair(false, "मास्टर विलोपन में त्रुटि: ${e.localizedMessage}")
        }
    }

    // =========================================================================
    // 📜 AUDIT TRAIL, DATABASE BACKUP & LIVE DARBAR CONTROLS
    // =========================================================================

    suspend fun logAuditEvent(
        action: String,
        tokenNumber: Int = 0,
        performedBy: String = "SYSTEM",
        role: String = "SEVADAR",
        reason: String = "",
        details: String = ""
    ): Long = withContext(Dispatchers.IO) {
        dbHelper.insertAuditLog(
            action = action,
            tokenNumber = tokenNumber,
            performedBy = performedBy,
            role = role,
            reason = reason,
            darbarDate = DatabaseHelper.getTodayDateString(),
            details = details
        )
    }

    suspend fun getAuditLogs(limit: Int = 150): List<AuditLogEntry> = withContext(Dispatchers.IO) {
        dbHelper.getAuditLogs(limit)
    }

    suspend fun getCombinedAuditLogs(context: Context, limit: Int = 150): List<AuditLogEntry> = withContext(Dispatchers.IO) {
        val localLogs = dbHelper.getAuditLogs(limit)
        try {
            val cloudLogs = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchCloudSecurityLogs(context, limit)
            val combined = (localLogs + cloudLogs).distinctBy { "${it.action}_${it.timestamp / 1000}_${it.performedBy}" }
            combined.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            localLogs
        }
    }

    suspend fun exportDatabaseBackup(context: Context): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        dbHelper.exportDatabaseBackup(context)
    }

    suspend fun updateDarbarLiveNow(isLive: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_darbar_live_now", if (isLive) 1 else 0)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) persistCurrentSettingsToAllLayers()
        ok
    }

    suspend fun getTodayActiveTokenCount(): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val today = DatabaseHelper.getTodayDateString()
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM tokens WHERE darbar_date = ? AND status != 'CANCELLED'",
            arrayOf(today)
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        count
    }

    // --- Super Admin Master Password Management ---
    suspend fun updateSuperAdminPassword(newPassword: String): Boolean = withContext(Dispatchers.IO) {
        val passHash = DatabaseHelper.hashPassword(newPassword.trim())
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("password_hash", passHash)
        }
        val ok = db.update("admins", cv, "role = 'SUPER_ADMIN'", null) > 0
        if (ok) {
            try { publishAdminsToGitHub() } catch (e: Exception) {}
        }
        ok
    }

    suspend fun verifySuperAdminPassword(password: String): Boolean = withContext(Dispatchers.IO) {
        val passHash = DatabaseHelper.hashPassword(password.trim())
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id FROM admins WHERE role = 'SUPER_ADMIN' AND password_hash = ? AND is_active = 1 LIMIT 1",
            arrayOf(passHash)
        )
        val isValid = cursor.moveToFirst()
        cursor.close()
        isValid
    }

    // --- Custom City and Village Distances (Super Admin Managed) ---
    suspend fun addCustomCityDistance(cityName: String, distanceKm: Float): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("city_name", cityName.trim())
            put("distance_km", distanceKm)
            put("created_at", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
        }
        val res = db.insertWithOnConflict("custom_city_distances", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        if (res > 0) {
            DistanceCalculatorService.registerCustomDistance(cityName, distanceKm)
        }
        res > 0
    }

    suspend fun getAllCustomCityDistances(): List<CustomCityDistance> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<CustomCityDistance>()
        try {
            val cursor = db.rawQuery("SELECT * FROM custom_city_distances ORDER BY city_name ASC", null)
            while (cursor.moveToNext()) {
                list.add(
                    CustomCityDistance(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cityName = cursor.getString(cursor.getColumnIndexOrThrow("city_name")),
                        distanceKm = cursor.getFloat(cursor.getColumnIndexOrThrow("distance_km")),
                        createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            // Ignore if table not yet created
        }
        list
    }

    suspend fun deleteCustomCityDistance(id: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("custom_city_distances", "id = ?", arrayOf(id.toString())) > 0
    }

    // --- 1-Click Complete Database Backup & Restore ---
    suspend fun exportFullDatabaseBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", DatabaseHelper.DATABASE_VERSION)
        root.put("app_name", "Shri Balaji Kripa Dham")
        root.put("exported_at", System.currentTimeMillis())
        root.put("exported_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        val db = dbHelper.readableDatabase

        // 1. Settings
        val s = getSettings()
        val settingsObj = JSONObject().apply {
            put("ashram_name", s.ashramName)
            put("guruji_name", s.gurujiName)
            put("address", s.address)
            put("contact_phone", s.contactPhone)
            put("darbar_timings", s.darbarTimings)
            put("latitude", s.latitude)
            put("longitude", s.longitude)
            put("allowed_radius_meters", s.allowedRadiusMeters)
            put("is_outstation_advance_allowed", s.isOutstationAdvanceAllowed)
            put("outstation_min_distance_km", s.outstationMinDistanceKm)
            put("running_token_number", s.runningTokenNumber)
            put("is_darbar_active", s.isDarbarActive)
            put("darbar_date", s.darbarDate)
            put("max_daily_tokens", s.maxDailyTokens)
            put("is_ui_layout_enforced", s.isUiLayoutEnforced)
            put("active_ui_layout", s.activeUiLayout)
            put("cloud_sync_url", s.cloudSyncUrl)
            put("is_cloud_sync_enabled", s.isCloudSyncEnabled)
        }
        root.put("settings", settingsObj)

        // 2. Sevadars
        val sevadars = getAllActiveSevadars()
        val adminsArr = JSONArray()
        for (a in sevadars) {
            adminsArr.put(JSONObject().apply {
                put("name", a.name)
                put("username", a.username)
                put("phone", a.phoneNumber)
                put("role", a.role.name)
                put("pin_hash", a.pinHash)
                put("password_hash", a.passwordHash)
                put("can_manage_tokens", a.canManageTokens)
                put("can_issue_manual_tokens", a.canIssueManualTokens)
                put("can_manage_yatra", a.canManageYatra)
                put("can_manage_expenses", a.canManageExpenses)
                put("can_change_location", a.canChangeLocation)
                put("can_send_notifications", a.canSendNotifications)
                put("can_edit_ashram_info", a.canEditAshramInfo)
                put("can_manage_admins", a.canManageAdmins)
                put("can_view_devotee_photos", a.canViewDevoteePhotos)
                put("can_manage_havan", a.canManageHavan)
                put("photo_uri", a.photoUri)
            })
        }
        root.put("sevadars", adminsArr)

        // 3. Tokens
        val tokensArr = JSONArray()
        val tokenCursor = db.rawQuery("SELECT * FROM tokens ORDER BY id ASC", null)
        while (tokenCursor.moveToNext()) {
            tokensArr.put(JSONObject().apply {
                put("token_number", tokenCursor.getInt(tokenCursor.getColumnIndexOrThrow("token_number")))
                put("darbar_date", tokenCursor.getString(tokenCursor.getColumnIndexOrThrow("darbar_date")))
                put("patient_name", tokenCursor.getString(tokenCursor.getColumnIndexOrThrow("patient_name")))
                put("phone_number", tokenCursor.getString(tokenCursor.getColumnIndexOrThrow("phone_number")))
                put("city", tokenCursor.getString(tokenCursor.getColumnIndexOrThrow("city")))
                put("origin_address", try { tokenCursor.getString(tokenCursor.getColumnIndexOrThrow("origin_address")) } catch (e: Exception) { "" })
                put("destination_address", try { tokenCursor.getString(tokenCursor.getColumnIndexOrThrow("destination_address")) } catch (e: Exception) { "" })
                put("distance_km", try { tokenCursor.getDouble(tokenCursor.getColumnIndexOrThrow("distance_km")) } catch (e: Exception) { -1.0 })
                put("device_id", tokenCursor.getString(tokenCursor.getColumnIndexOrThrow("device_id")))
                put("latitude", tokenCursor.getDouble(tokenCursor.getColumnIndexOrThrow("latitude")))
                put("longitude", tokenCursor.getDouble(tokenCursor.getColumnIndexOrThrow("longitude")))
                put("status", tokenCursor.getString(tokenCursor.getColumnIndexOrThrow("status")))
                put("registered_by", tokenCursor.getString(tokenCursor.getColumnIndexOrThrow("registered_by")))
                put("is_darshan_completed", try { tokenCursor.getInt(tokenCursor.getColumnIndexOrThrow("is_darshan_completed")) } catch (e: Exception) { 0 })
                put("photo_uri", try { tokenCursor.getString(tokenCursor.getColumnIndexOrThrow("photo_uri")) } catch (e: Exception) { "" })
                put("created_at", tokenCursor.getLong(tokenCursor.getColumnIndexOrThrow("created_at")))
            })
        }
        tokenCursor.close()
        root.put("tokens", tokensArr)

        // 4. Custom Distances
        val customDistances = getAllCustomCityDistances()
        val distArr = JSONArray()
        for (d in customDistances) {
            distArr.put(JSONObject().apply {
                put("city_name", d.cityName)
                put("distance_km", d.distanceKm.toDouble())
                put("created_at", d.createdAt)
            })
        }
        root.put("custom_city_distances", distArr)

        // 5. UI Section Configurations
        val uiSections = getUiSectionConfigs()
        val uiSectionsArr = JSONArray()
        for (u in uiSections) {
            uiSectionsArr.put(JSONObject().apply {
                put("section_id", u.sectionId)
                put("title_hindi", u.titleHindi)
                put("title_english", u.titleEnglish)
                put("icon", u.icon)
                put("is_visible", u.isVisible)
                put("order_index", u.orderIndex)
            })
        }
        root.put("ui_sections", uiSectionsArr)

        // 6. Yatra & Ashram Expenses (Hisab-Kitab)
        val expensesArr = JSONArray()
        try {
            val expCursor = db.rawQuery("SELECT * FROM yatra_expenses ORDER BY id ASC", null)
            while (expCursor.moveToNext()) {
                expensesArr.put(JSONObject().apply {
                    put("title", expCursor.getString(expCursor.getColumnIndexOrThrow("title")))
                    put("category", expCursor.getString(expCursor.getColumnIndexOrThrow("category")))
                    put("amount", expCursor.getDouble(expCursor.getColumnIndexOrThrow("amount")))
                    put("receipt_uri", try { expCursor.getString(expCursor.getColumnIndexOrThrow("receipt_uri")) } catch (e: Exception) { "" })
                    put("added_by", expCursor.getString(expCursor.getColumnIndexOrThrow("added_by")))
                    put("expense_date", expCursor.getString(expCursor.getColumnIndexOrThrow("expense_date")))
                    put("created_at", expCursor.getLong(expCursor.getColumnIndexOrThrow("created_at")))
                })
            }
            expCursor.close()
        } catch (e: Exception) {}
        root.put("yatra_expenses", expensesArr)

        // 7. Payment Records (Donations & Bus Hisab-Kitab)
        val paymentsArr = JSONArray()
        try {
            val payCursor = db.rawQuery("SELECT * FROM payment_records ORDER BY id ASC", null)
            while (payCursor.moveToNext()) {
                paymentsArr.put(JSONObject().apply {
                    put("payment_id", payCursor.getString(payCursor.getColumnIndexOrThrow("payment_id")))
                    put("devotee_name", payCursor.getString(payCursor.getColumnIndexOrThrow("devotee_name")))
                    put("devotee_phone", payCursor.getString(payCursor.getColumnIndexOrThrow("devotee_phone")))
                    put("payment_app", payCursor.getString(payCursor.getColumnIndexOrThrow("payment_app")))
                    put("transaction_id", payCursor.getString(payCursor.getColumnIndexOrThrow("transaction_id")))
                    put("amount", payCursor.getDouble(payCursor.getColumnIndexOrThrow("amount")))
                    put("purpose", payCursor.getString(payCursor.getColumnIndexOrThrow("purpose")))
                    put("seat_numbers", try { payCursor.getString(payCursor.getColumnIndexOrThrow("seat_numbers")) } catch (e: Exception) { "" })
                    put("timestamp", payCursor.getLong(payCursor.getColumnIndexOrThrow("timestamp")))
                    put("payment_status", payCursor.getString(payCursor.getColumnIndexOrThrow("payment_status")))
                    put("payment_mode", payCursor.getString(payCursor.getColumnIndexOrThrow("payment_mode")))
                    put("verified_by", try { payCursor.getString(payCursor.getColumnIndexOrThrow("verified_by")) } catch (e: Exception) { "" })
                    put("notes", try { payCursor.getString(payCursor.getColumnIndexOrThrow("notes")) } catch (e: Exception) { "" })
                })
            }
            payCursor.close()
        } catch (e: Exception) {}
        root.put("payment_records", paymentsArr)

        // 8. Sacred Parchas
        val parchasArr = JSONArray()
        try {
            val parchaCursor = db.rawQuery("SELECT * FROM sacred_parchas ORDER BY id ASC", null)
            while (parchaCursor.moveToNext()) {
                parchasArr.put(JSONObject().apply {
                    put("parcha_id", parchaCursor.getString(parchaCursor.getColumnIndexOrThrow("parcha_id")))
                    put("title", parchaCursor.getString(parchaCursor.getColumnIndexOrThrow("title")))
                    put("category", parchaCursor.getString(parchaCursor.getColumnIndexOrThrow("category")))
                    put("subtitle", try { parchaCursor.getString(parchaCursor.getColumnIndexOrThrow("subtitle")) } catch (e: Exception) { "" })
                    put("samagri_list", try { parchaCursor.getString(parchaCursor.getColumnIndexOrThrow("samagri_list")) } catch (e: Exception) { "" })
                    put("vidhi_text", try { parchaCursor.getString(parchaCursor.getColumnIndexOrThrow("vidhi_text")) } catch (e: Exception) { "" })
                    put("precautions", try { parchaCursor.getString(parchaCursor.getColumnIndexOrThrow("precautions")) } catch (e: Exception) { "" })
                    put("mantra_text", try { parchaCursor.getString(parchaCursor.getColumnIndexOrThrow("mantra_text")) } catch (e: Exception) { "" })
                    put("image_uri", try { parchaCursor.getString(parchaCursor.getColumnIndexOrThrow("image_uri")) } catch (e: Exception) { "" })
                    put("is_published", parchaCursor.getInt(parchaCursor.getColumnIndexOrThrow("is_published")))
                    put("created_at", parchaCursor.getLong(parchaCursor.getColumnIndexOrThrow("created_at")))
                    put("updated_at", parchaCursor.getLong(parchaCursor.getColumnIndexOrThrow("updated_at")))
                })
            }
            parchaCursor.close()
        } catch (e: Exception) {}
        root.put("sacred_parchas", parchasArr)

        // 9. Devotee Master Directory
        val dirArr = JSONArray()
        try {
            val dirCursor = db.rawQuery("SELECT * FROM devotee_directory ORDER BY id ASC", null)
            while (dirCursor.moveToNext()) {
                dirArr.put(JSONObject().apply {
                    put("name", dirCursor.getString(dirCursor.getColumnIndexOrThrow("name")))
                    put("phone", dirCursor.getString(dirCursor.getColumnIndexOrThrow("phone")))
                    put("city", try { dirCursor.getString(dirCursor.getColumnIndexOrThrow("city")) } catch (e: Exception) { "" })
                    put("photo_uri", try { dirCursor.getString(dirCursor.getColumnIndexOrThrow("photo_uri")) } catch (e: Exception) { "" })
                    put("visits_count", dirCursor.getInt(dirCursor.getColumnIndexOrThrow("visits_count")))
                    put("source_module", try { dirCursor.getString(dirCursor.getColumnIndexOrThrow("source_module")) } catch (e: Exception) { "TOKEN" })
                    put("last_visit_date", try { dirCursor.getString(dirCursor.getColumnIndexOrThrow("last_visit_date")) } catch (e: Exception) { "" })
                })
            }
            dirCursor.close()
        } catch (e: Exception) {}
        root.put("devotee_directory", dirArr)

        root.toString(2)
    }

    suspend fun restoreFullDatabaseFromJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonStr)
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Restore Settings
            if (root.has("settings")) {
                val s = root.getJSONObject("settings")
                val cv = ContentValues().apply {
                    if (s.has("ashram_name")) put("ashram_name", s.getString("ashram_name"))
                    if (s.has("guruji_name")) put("guruji_name", s.getString("guruji_name"))
                    if (s.has("address")) put("address", s.getString("address"))
                    if (s.has("contact_phone")) put("contact_phone", s.getString("contact_phone"))
                    if (s.has("darbar_timings")) put("darbar_timings", s.getString("darbar_timings"))
                    if (s.has("latitude")) put("latitude", s.getDouble("latitude"))
                    if (s.has("longitude")) put("longitude", s.getDouble("longitude"))
                    if (s.has("allowed_radius_meters")) put("allowed_radius_meters", s.getDouble("allowed_radius_meters"))
                    if (s.has("is_outstation_advance_allowed")) put("is_outstation_advance_allowed", if (s.getBoolean("is_outstation_advance_allowed")) 1 else 0)
                    if (s.has("outstation_min_distance_km")) put("outstation_min_distance_km", s.getDouble("outstation_min_distance_km"))
                    if (s.has("running_token_number")) put("running_token_number", s.getInt("running_token_number"))
                    if (s.has("is_darbar_active")) put("is_darbar_active", if (s.getBoolean("is_darbar_active")) 1 else 0)
                    if (s.has("darbar_date")) put("darbar_date", s.getString("darbar_date"))
                    if (s.has("max_daily_tokens")) put("max_daily_tokens", s.getInt("max_daily_tokens"))
                    if (s.has("is_ui_layout_enforced")) put("is_ui_layout_enforced", if (s.getBoolean("is_ui_layout_enforced")) 1 else 0)
                    if (s.has("active_ui_layout")) put("active_ui_layout", s.getString("active_ui_layout"))
                    if (s.has("cloud_sync_url")) put("cloud_sync_url", s.getString("cloud_sync_url"))
                    if (s.has("is_cloud_sync_enabled")) put("is_cloud_sync_enabled", if (s.getBoolean("is_cloud_sync_enabled")) 1 else 0)
                }
                db.update("ashram_settings", cv, "id = 1", null)
            }

            // Restore Custom Distances
            if (root.has("custom_city_distances")) {
                val distArr = root.getJSONArray("custom_city_distances")
                for (i in 0 until distArr.length()) {
                    val d = distArr.getJSONObject(i)
                    val cv = ContentValues().apply {
                        put("city_name", d.getString("city_name"))
                        put("distance_km", d.getDouble("distance_km").toFloat())
                        put("created_at", d.optString("created_at", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())))
                    }
                    db.insertWithOnConflict("custom_city_distances", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }

            // Restore Tokens
            if (root.has("tokens")) {
                val tokensArr = root.getJSONArray("tokens")
                for (i in 0 until tokensArr.length()) {
                    val t = tokensArr.getJSONObject(i)
                    val cv = ContentValues().apply {
                        put("token_number", t.getInt("token_number"))
                        put("darbar_date", t.getString("darbar_date"))
                        put("patient_name", t.getString("patient_name"))
                        put("phone_number", t.getString("phone_number"))
                        put("city", t.optString("city", "डूँगरा जाट (स्थानीय)"))
                        put("origin_address", t.optString("origin_address", ""))
                        put("destination_address", t.optString("destination_address", "श्री बालाजी कृपा धाम, डुंगरा जाट"))
                        put("distance_km", t.optDouble("distance_km", -1.0).toFloat())
                        put("device_id", t.optString("device_id", "RESTORED"))
                        put("latitude", t.optDouble("latitude", 28.3972915))
                        put("longitude", t.optDouble("longitude", 78.1460410))
                        put("status", t.optString("status", "WAITING"))
                        put("registered_by", t.optString("registered_by", "RESTORE"))
                        put("photo_uri", t.optString("photo_uri", ""))
                        put("is_darshan_completed", t.optInt("is_darshan_completed", 0))
                        put("created_at", t.optLong("created_at", System.currentTimeMillis()))
                    }
                    db.insertWithOnConflict("tokens", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }

            // Restore UI Sections
            if (root.has("ui_sections")) {
                val uiArr = root.getJSONArray("ui_sections")
                val restoredList = mutableListOf<UiSectionConfig>()
                for (i in 0 until uiArr.length()) {
                    val u = uiArr.getJSONObject(i)
                    restoredList.add(
                        UiSectionConfig(
                            sectionId = u.getString("section_id"),
                            titleHindi = u.getString("title_hindi"),
                            titleEnglish = u.getString("title_english"),
                            icon = u.optString("icon", "📌"),
                            isVisible = u.optBoolean("is_visible", true),
                            orderIndex = u.optInt("order_index", i)
                        )
                    )
                }
                if (restoredList.isNotEmpty()) {
                    db.delete("ui_section_configs", null, null)
                    restoredList.forEachIndexed { index, item ->
                        val cv = ContentValues().apply {
                            put("section_id", item.sectionId)
                            put("title_hindi", item.titleHindi)
                            put("title_english", item.titleEnglish)
                            put("icon", item.icon)
                            put("is_visible", if (item.isVisible) 1 else 0)
                            put("order_index", index)
                        }
                        db.insertWithOnConflict("ui_section_configs", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }
            }

            // Restore Yatra Expenses (Hisab-Kitab)
            if (root.has("yatra_expenses")) {
                val expArr = root.getJSONArray("yatra_expenses")
                for (i in 0 until expArr.length()) {
                    val exp = expArr.getJSONObject(i)
                    val cv = ContentValues().apply {
                        put("title", exp.getString("title"))
                        put("category", exp.getString("category"))
                        put("amount", exp.getDouble("amount"))
                        put("receipt_uri", exp.optString("receipt_uri", ""))
                        put("added_by", exp.optString("added_by", "ADMIN"))
                        put("expense_date", exp.optString("expense_date", ""))
                        put("created_at", exp.optLong("created_at", System.currentTimeMillis()))
                    }
                    db.insert("yatra_expenses", null, cv)
                }
            }

            // Restore Payment Records
            if (root.has("payment_records")) {
                val payArr = root.getJSONArray("payment_records")
                for (i in 0 until payArr.length()) {
                    val p = payArr.getJSONObject(i)
                    val cv = ContentValues().apply {
                        put("payment_id", p.getString("payment_id"))
                        put("devotee_name", p.getString("devotee_name"))
                        put("devotee_phone", p.getString("devotee_phone"))
                        put("payment_app", p.getString("payment_app"))
                        put("transaction_id", p.getString("transaction_id"))
                        put("amount", p.getDouble("amount"))
                        put("purpose", p.getString("purpose"))
                        put("seat_numbers", p.optString("seat_numbers", ""))
                        put("timestamp", p.optLong("timestamp", System.currentTimeMillis()))
                        put("payment_status", p.optString("payment_status", "SUCCESS"))
                        put("payment_mode", p.optString("payment_mode", "UPI_QR"))
                        put("verified_by", p.optString("verified_by", ""))
                        put("notes", p.optString("notes", ""))
                    }
                    db.insertWithOnConflict("payment_records", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }

            // Restore Sacred Parchas
            if (root.has("sacred_parchas")) {
                val parArr = root.getJSONArray("sacred_parchas")
                for (i in 0 until parArr.length()) {
                    val pc = parArr.getJSONObject(i)
                    val cv = ContentValues().apply {
                        put("parcha_id", pc.getString("parcha_id"))
                        put("title", pc.getString("title"))
                        put("category", pc.getString("category"))
                        put("subtitle", pc.optString("subtitle", ""))
                        put("samagri_list", pc.optString("samagri_list", ""))
                        put("vidhi_text", pc.optString("vidhi_text", ""))
                        put("precautions", pc.optString("precautions", ""))
                        put("mantra_text", pc.optString("mantra_text", ""))
                        put("image_uri", pc.optString("image_uri", ""))
                        put("is_published", pc.optInt("is_published", 1))
                        put("created_at", pc.optLong("created_at", System.currentTimeMillis()))
                        put("updated_at", pc.optLong("updated_at", System.currentTimeMillis()))
                    }
                    db.insertWithOnConflict("sacred_parchas", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }

            // Restore Devotee Directory
            if (root.has("devotee_directory")) {
                val dirArr = root.getJSONArray("devotee_directory")
                for (i in 0 until dirArr.length()) {
                    val d = dirArr.getJSONObject(i)
                    val cv = ContentValues().apply {
                        put("name", d.getString("name"))
                        put("phone", d.getString("phone"))
                        put("city", d.optString("city", ""))
                        put("photo_uri", d.optString("photo_uri", ""))
                        put("visits_count", d.optInt("visits_count", 1))
                        put("source_module", d.optString("source_module", "RESTORE"))
                        put("last_visit_date", d.optString("last_visit_date", ""))
                    }
                    db.insertWithOnConflict("devotee_directory", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }

            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            false
        } finally {
            db.endTransaction()
        }
    }

    // --- Central Cloud Sync Endpoint Integration ---
    suspend fun syncWithCloudEndpoint(serverUrl: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val trimmed = serverUrl.trim()
        if (trimmed.isBlank() || (!trimmed.startsWith("http://") && !trimmed.startsWith("https://"))) {
            return@withContext Pair(false, "अमान्य क्लाउड सर्वर URL (Invalid URL, must start with https://)")
        }

        val resolvedUrl = when {
            trimmed.endsWith("/api/cloud_sync.php") -> trimmed
            trimmed.endsWith("/cloud_sync.php") -> trimmed
            trimmed.endsWith("/api/") -> "${trimmed}cloud_sync.php"
            trimmed.endsWith("/api") -> "$trimmed/cloud_sync.php"
            trimmed.endsWith("/") -> "${trimmed}api/cloud_sync.php"
            !trimmed.endsWith(".php") -> "$trimmed/api/cloud_sync.php"
            else -> trimmed
        }

        try {
            val backupJson = exportFullDatabaseBackupJson()
            val url = URL(resolvedUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.42.1")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(backupJson.toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val resp = conn.inputStream.bufferedReader().readText()
                if (resp.trim().startsWith("{")) {
                    try {
                        restoreFullDatabaseFromJson(resp)
                    } catch (e: Exception) {
                        // Ignore merge failure
                    }
                }
                Pair(true, "क्लाउड डेटा सिंक सफल (HTTP $code)")
            } else {
                Pair(false, "सर्वर त्रुटि: HTTP $code")
            }
        } catch (e: Exception) {
            Pair(false, "सिंक विफल: ${e.localizedMessage ?: "नेटवर्क अनुपलब्ध"}")
        }
    }

    // --- Dynamic UI Section Box Control ---
    suspend fun getUiSectionConfigs(): List<UiSectionConfig> = withContext(Dispatchers.IO) {
        val list = mutableListOf<UiSectionConfig>()
        val db = dbHelper.readableDatabase
        try {
            val cursor = db.query(
                "ui_section_configs",
                null,
                null,
                null,
                null,
                null,
                "order_index ASC"
            )
            cursor.use { c ->
                val idIdx = c.getColumnIndexOrThrow("section_id")
                val titleHiIdx = c.getColumnIndexOrThrow("title_hindi")
                val titleEnIdx = c.getColumnIndexOrThrow("title_english")
                val iconIdx = c.getColumnIndexOrThrow("icon")
                val visibleIdx = c.getColumnIndexOrThrow("is_visible")
                val orderIdx = c.getColumnIndexOrThrow("order_index")
                val subHiIdx = try { c.getColumnIndex("custom_subtitle_hindi") } catch (e: Exception) { -1 }
                val subEnIdx = try { c.getColumnIndex("custom_subtitle_english") } catch (e: Exception) { -1 }
                val cntHiIdx = try { c.getColumnIndex("custom_content_hindi") } catch (e: Exception) { -1 }
                val cntEnIdx = try { c.getColumnIndex("custom_content_english") } catch (e: Exception) { -1 }

                while (c.moveToNext()) {
                    list.add(
                        UiSectionConfig(
                            sectionId = c.getString(idIdx),
                            titleHindi = c.getString(titleHiIdx),
                            titleEnglish = c.getString(titleEnIdx),
                            icon = c.getString(iconIdx),
                            isVisible = c.getInt(visibleIdx) == 1,
                            orderIndex = c.getInt(orderIdx),
                            customSubtitleHindi = if (subHiIdx >= 0 && !c.isNull(subHiIdx)) c.getString(subHiIdx) else "",
                            customSubtitleEnglish = if (subEnIdx >= 0 && !c.isNull(subEnIdx)) c.getString(subEnIdx) else "",
                            customContentHindi = if (cntHiIdx >= 0 && !c.isNull(cntHiIdx)) c.getString(cntHiIdx) else "",
                            customContentEnglish = if (cntEnIdx >= 0 && !c.isNull(cntEnIdx)) c.getString(cntEnIdx) else ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Transient fallback
        }

        if (list.isEmpty()) {
            val defaults = UiSectionConfig.defaultSections()
            saveUiSectionConfigs(defaults)
            defaults
        } else {
            list
        }
    }

    suspend fun saveUiSectionConfigs(configs: List<UiSectionConfig>): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        val ok = try {
            configs.forEachIndexed { index, item ->
                val cv = ContentValues().apply {
                    put("section_id", item.sectionId)
                    put("title_hindi", item.titleHindi)
                    put("title_english", item.titleEnglish)
                    put("icon", item.icon)
                    put("is_visible", if (item.isVisible) 1 else 0)
                    put("order_index", index)
                    put("custom_subtitle_hindi", item.customSubtitleHindi)
                    put("custom_subtitle_english", item.customSubtitleEnglish)
                    put("custom_content_hindi", item.customContentHindi)
                    put("custom_content_english", item.customContentEnglish)
                }
                db.insertWithOnConflict("ui_section_configs", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }

            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            false
        } finally {
            db.endTransaction()
        }

        if (ok) {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.updateUiSections(appContext, configs)
                } catch (e: Exception) {}
            }
        }
        ok
    }

    suspend fun resetUiSectionConfigsToDefault(): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        try {
            db.delete("ui_section_configs", null, null)
            saveUiSectionConfigs(UiSectionConfig.defaultSections())
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Central GitHub Live Sync Methods ---
    suspend fun syncLiveConfigFromGitHub(): Pair<Boolean, LiveUiConfigDto?> = withContext(Dispatchers.IO) {
        // Pull from Hostinger Central MySQL Server (Real-time live settings sync)
        try {
            val hostingerJson = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchLiveConfig()
            if (hostingerJson != null && (hostingerJson.optBoolean("success", false) || hostingerJson.has("ashram_name") || hostingerJson.has("config"))) {
                val cfg = if (hostingerJson.has("config")) hostingerJson.getJSONObject("config") else hostingerJson
                val db = dbHelper.writableDatabase
                val cv = ContentValues()

                val currentServing = cfg.optInt("running_token_number", cfg.optInt("current_serving_token", -1))
                if (currentServing >= 0) cv.put("running_token_number", currentServing)

                val gurujiPhoto = cfg.optString("guruji_photo_url", "")
                if (gurujiPhoto.isNotBlank()) cv.put("guruji_photo_uri", gurujiPhoto)

                val bannerTitle = cfg.optString("banner_title", "")
                if (bannerTitle.isNotBlank()) cv.put("banner_title", bannerTitle)

                val bannerSub = cfg.optString("banner_subtitle", "")
                if (bannerSub.isNotBlank()) cv.put("banner_subtitle", bannerSub)

                if (cfg.has("is_banner_visible")) cv.put("is_banner_visible", if (cfg.optBoolean("is_banner_visible")) 1 else 0)

                val emNotice = cfg.optString("emergency_notice", "")
                cv.put("emergency_notice", emNotice)
                if (cfg.has("is_emergency_notice_visible")) cv.put("is_emergency_notice_visible", if (cfg.optBoolean("is_emergency_notice_visible")) 1 else 0)

                val timings = cfg.optString("darbar_timings", "")
                if (timings.isNotBlank()) cv.put("darbar_timings", timings)

                val ashName = cfg.optString("ashram_name", "")
                if (ashName.isNotBlank()) cv.put("ashram_name", ashName)

                if (cfg.has("is_darbar_active")) cv.put("is_darbar_active", if (cfg.optBoolean("is_darbar_active")) 1 else 0)
                if (cfg.has("is_token_service_enabled")) cv.put("is_token_service_enabled", if (cfg.optBoolean("is_token_service_enabled")) 1 else 0)

                // ⚡ Token Service Modes & Direct Scheduling Sync
                if (cfg.has("token_service_mode")) {
                    val mode = cfg.optString("token_service_mode", "").trim()
                    if (mode.isNotBlank()) {
                        cv.put("token_service_mode", mode)
                        if (mode.equals("FORCE_OPEN", ignoreCase = true)) {
                            cv.put("is_token_service_enabled", 1)
                            cv.put("is_darbar_active", 1)
                        } else if (mode.equals("FORCE_CLOSED", ignoreCase = true)) {
                            cv.put("is_token_service_enabled", 0)
                        }
                    }
                }
                if (cfg.has("scheduled_token_open_timestamp")) {
                    cv.put("scheduled_token_open_timestamp", cfg.optLong("scheduled_token_open_timestamp", 0L))
                }

                // ⚡ Tuesday Darbar Mode & Configuration Sync
                if (cfg.has("is_tuesday_darbar_enabled")) {
                    cv.put("is_tuesday_darbar_enabled", if (cfg.optBoolean("is_tuesday_darbar_enabled")) 1 else 0)
                }
                if (cfg.has("tuesday_token_service_mode")) {
                    val tMode = cfg.optString("tuesday_token_service_mode", "").trim()
                    if (tMode.isNotBlank()) cv.put("tuesday_token_service_mode", tMode)
                }
                if (cfg.has("tuesday_scheduled_open_timestamp")) {
                    cv.put("tuesday_scheduled_open_timestamp", cfg.optLong("tuesday_scheduled_open_timestamp", 0L))
                }
                if (cfg.has("tuesday_darbar_name")) {
                    val tName = cfg.optString("tuesday_darbar_name", "").trim()
                    if (tName.isNotBlank()) cv.put("tuesday_darbar_name", tName)
                }
                if (cfg.has("tuesday_darbar_address")) {
                    val tAddr = cfg.optString("tuesday_darbar_address", "").trim()
                    if (tAddr.isNotBlank()) cv.put("tuesday_darbar_address", tAddr)
                }
                if (cfg.has("tuesday_darbar_timings")) {
                    val tTime = cfg.optString("tuesday_darbar_timings", "").trim()
                    if (tTime.isNotBlank()) cv.put("tuesday_darbar_timings", tTime)
                }
                if (cfg.has("tuesday_darbar_date")) {
                    cv.put("tuesday_darbar_date", cfg.optString("tuesday_darbar_date", "").trim())
                }
                if (cfg.has("tuesday_current_serving_token")) {
                    cv.put("tuesday_current_serving_token", cfg.optInt("tuesday_current_serving_token", 0))
                }
                if (cfg.has("tuesday_running_token_number")) {
                    cv.put("tuesday_running_token_number", cfg.optInt("tuesday_running_token_number", 1))
                }
                if (cfg.has("tuesday_token_notice")) {
                    val tNotice = cfg.optString("tuesday_token_notice", "").trim()
                    if (tNotice.isNotBlank()) cv.put("tuesday_token_notice", tNotice)
                }
                if (cfg.has("tuesday_latitude")) {
                    cv.put("tuesday_latitude", cfg.optDouble("tuesday_latitude", 28.4069))
                }
                if (cfg.has("tuesday_longitude")) {
                    cv.put("tuesday_longitude", cfg.optDouble("tuesday_longitude", 77.8498))
                }
                if (cfg.has("tuesday_allowed_radius_meters")) {
                    cv.put("tuesday_allowed_radius_meters", cfg.optDouble("tuesday_allowed_radius_meters", 200.0))
                }
                if (cfg.has("tuesday_outstation_min_distance_km")) {
                    cv.put("tuesday_outstation_min_distance_km", cfg.optDouble("tuesday_outstation_min_distance_km", 30.0))
                }

                // ⚡ Geofence, Distance & Outstation Controls
                if (cfg.has("latitude") && cfg.has("longitude")) {
                    val lat = cfg.optDouble("latitude", 0.0)
                    val lng = cfg.optDouble("longitude", 0.0)
                    if (lat != 0.0 && lng != 0.0) {
                        cv.put("latitude", lat)
                        cv.put("longitude", lng)
                    }
                }
                if (cfg.has("allowed_radius_meters")) {
                    val rad = cfg.optDouble("allowed_radius_meters", 0.0)
                    if (rad > 0.0) cv.put("allowed_radius_meters", rad)
                }
                if (cfg.has("outstation_min_distance_km")) {
                    val dist = cfg.optDouble("outstation_min_distance_km", 0.0)
                    if (dist > 0.0) cv.put("outstation_min_distance_km", dist)
                }
                if (cfg.has("is_geofence_enforced")) {
                    cv.put("is_geofence_enforced", if (cfg.optBoolean("is_geofence_enforced")) 1 else 0)
                }
                if (cfg.has("is_outstation_advance_allowed")) {
                    cv.put("is_outstation_advance_allowed", if (cfg.optBoolean("is_outstation_advance_allowed")) 1 else 0)
                }

                // ⚡ Sunday Token Banner & Custom Notice
                if (cfg.has("sunday_token_banner_title")) {
                    val v = cfg.optString("sunday_token_banner_title", "").trim()
                    if (v.isNotBlank()) cv.put("sunday_token_banner_title", v)
                }
                if (cfg.has("sunday_token_banner_text")) {
                    val v = cfg.optString("sunday_token_banner_text", "").trim()
                    if (v.isNotBlank()) cv.put("sunday_token_banner_text", v)
                }
                if (cfg.has("sunday_token_custom_notice")) {
                    cv.put("sunday_token_custom_notice", cfg.optString("sunday_token_custom_notice", "").trim())
                }

                // ⚡ Token Limits, Reserved & Live Darbar Streaming
                if (cfg.has("max_daily_tokens")) {
                    cv.put("max_daily_tokens", cfg.optInt("max_daily_tokens", 0))
                } else if (cfg.has("daily_token_limit")) {
                    cv.put("max_daily_tokens", cfg.optInt("daily_token_limit", 0))
                }
                if (cfg.has("allow_admin_reserved_tokens")) {
                    cv.put("allow_admin_reserved_tokens", if (cfg.optBoolean("allow_admin_reserved_tokens")) 1 else 0)
                } else if (cfg.has("can_admin_issue_reserved_tokens")) {
                    cv.put("allow_admin_reserved_tokens", if (cfg.optBoolean("can_admin_issue_reserved_tokens")) 1 else 0)
                }
                if (cfg.has("is_darbar_live_now")) {
                    cv.put("is_darbar_live_now", if (cfg.optBoolean("is_darbar_live_now")) 1 else 0)
                }
                if (cfg.has("live_stream_title")) {
                    val v = cfg.optString("live_stream_title", "").trim()
                    if (v.isNotBlank()) cv.put("live_stream_title", v)
                }
                if (cfg.has("live_stream_url")) {
                    val v = cfg.optString("live_stream_url", "").trim()
                    if (v.isNotBlank()) cv.put("live_stream_url", v)
                }
                if (cfg.has("youtube_live_url")) {
                    val v = cfg.optString("youtube_live_url", "").trim()
                    if (v.isNotBlank()) cv.put("youtube_live_url", v)
                }
                if (cfg.has("facebook_live_url")) {
                    val v = cfg.optString("facebook_live_url", "").trim()
                    if (v.isNotBlank()) cv.put("facebook_live_url", v)
                }

                if (cfg.has("is_bus_booking_live")) cv.put("is_bus_booking_live", if (cfg.optBoolean("is_bus_booking_live")) 1 else 0)
                if (cfg.has("is_payment_feature_live")) cv.put("is_payment_feature_live", if (cfg.optBoolean("is_payment_feature_live")) 1 else 0)
                if (cfg.has("bus_seat_fare_amount")) {
                    cv.put("bus_seat_fare_amount", cfg.optInt("bus_seat_fare_amount", 0))
                }
                if (cfg.has("badi_arzi_rate")) {
                    val bRate = cfg.optDouble("badi_arzi_rate", -1.0)
                    if (bRate >= 0.0) cv.put("badi_arzi_rate", bRate)
                }
                if (cfg.has("chhoti_arzi_rate")) {
                    val cRate = cfg.optDouble("chhoti_arzi_rate", -1.0)
                    if (cRate >= 0.0) cv.put("chhoti_arzi_rate", cRate)
                }
                if (cfg.has("is_arzi_ledger_live")) cv.put("is_arzi_ledger_live", if (cfg.optBoolean("is_arzi_ledger_live")) 1 else 0)

                val cmsCols = listOf(
                    "top_bar_text", "guruji_title", "guruji_bio", "token_rules_notice",
                    "bank_name", "bank_account_holder", "bank_account_number", "bank_ifsc",
                    "bank_branch", "ashram_address", "ashram_directions", "contact_email",
                    "instagram_url", "whatsapp_channel_url", "footer_title", "footer_dedication",
                    "footer_copyright", "ashram_parichay_hindi", "ashram_history_hindi", "ashram_rules_hindi",
                    "havan_rules_notice"
                )
                for (col in cmsCols) {
                    if (cfg.has(col)) {
                        val v = cfg.optString(col, "")
                        if (v.isNotBlank()) cv.put(col, v)
                    }
                }

                if (cfg.has("havan_estimated_cost")) {
                    val hCost = cfg.optInt("havan_estimated_cost", -1)
                    if (hCost > 0) cv.put("havan_estimated_cost", hCost)
                }

                if (cfg.has("contact_phone")) {
                    val ph = cfg.optString("contact_phone", "").trim()
                    cv.put("contact_phone", ph)
                }
                if (cfg.has("whatsapp_number")) {
                    val wa = cfg.optString("whatsapp_number", "").trim()
                    cv.put("whatsapp_number", wa)
                }
                if (cfg.has("darbar_date")) {
                    cv.put("darbar_date", cfg.optString("darbar_date", ""))
                }
                if (cfg.has("upi_id")) {
                    val upi = cfg.optString("upi_id", "").trim()
                    cv.put("ashram_upi_id", upi)
                }
                if (cfg.has("upi_name")) {
                    cv.put("ashram_upi_name", cfg.optString("upi_name", ""))
                }

                if (cfg.has("sections")) {
                    val secArr = cfg.optJSONArray("sections")
                    if (secArr != null && secArr.length() > 0) {
                        val parsedSections = UiSectionConfig.fromJson(secArr.toString())
                        saveUiSectionConfigs(parsedSections)
                    }
                }

                if (cv.size() > 0) {
                    db.update("ashram_settings", cv, "id = 1", null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val remoteConfig = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.fetchLiveConfig()
        if (remoteConfig != null) {
            if (remoteConfig.sections.isNotEmpty()) {
                saveUiSectionConfigs(remoteConfig.sections)
            }

            try {
                val db = dbHelper.writableDatabase

                val countCursor = db.rawQuery("SELECT COUNT(*) FROM ashram_settings WHERE id = 1", null)
                var hasSettings = false
                if (countCursor.moveToFirst()) {
                    hasSettings = countCursor.getInt(0) > 0
                }
                countCursor.close()

                val cv = ContentValues()

                // Synchronize Ashram Details across all devices
                val det = remoteConfig.ashramDetails
                if (det.ashramName.isNotBlank()) cv.put("ashram_name", det.ashramName)
                if (det.gurujiName.isNotBlank()) cv.put("guruji_name", det.gurujiName)
                if (det.address.isNotBlank()) cv.put("address", det.address)
                if (det.contactPhone.isNotBlank()) cv.put("contact_phone", det.contactPhone)
                if (det.contactPhoneSecondary.isNotBlank()) cv.put("contact_phone_secondary", det.contactPhoneSecondary)
                if (det.darbarTimings.isNotBlank()) cv.put("darbar_timings", det.darbarTimings)
                if (det.freeDisclaimer.isNotBlank()) cv.put("free_disclaimer", det.freeDisclaimer)
                if (det.whatsappNumber.isNotBlank()) cv.put("whatsapp_number", det.whatsappNumber)
                if (det.whatsappGroupUrl.isNotBlank()) cv.put("whatsapp_group_url", det.whatsappGroupUrl)
                if (det.youtubeChannelUrl.isNotBlank()) cv.put("youtube_channel_url", det.youtubeChannelUrl)
                if (det.facebookPageUrl.isNotBlank()) cv.put("facebook_page_url", det.facebookPageUrl)
                if (det.instagramUrl.isNotBlank()) cv.put("instagram_url", det.instagramUrl)
                if (det.appShareUrl.isNotBlank()) cv.put("app_share_url", det.appShareUrl)
                if (det.gurujiPhotoUrl.isNotBlank()) cv.put("guruji_photo_uri", det.gurujiPhotoUrl)

                // Synchronize Emergency Broadcast Notice
                val em = remoteConfig.emergencyNotice
                if (em.noticeHindi.isNotBlank()) {
                    cv.put("emergency_notice", em.noticeHindi)
                    cv.put("is_emergency_notice_visible", if (em.isEnabled) 1 else 0)
                }

                // Automatically synchronize coordinates & geofence from cloud to local SQLite
                val loc = remoteConfig.locationConfig
                if (loc.latitude != 0.0 && loc.longitude != 0.0) {
                    cv.put("latitude", loc.latitude)
                    cv.put("longitude", loc.longitude)
                    cv.put("allowed_radius_meters", loc.allowedRadiusMeters.coerceIn(10.0, 50000.0))
                    cv.put("is_geofence_enforced", if (loc.isGeofenceEnforced) 1 else 0)
                    cv.put("is_outstation_advance_allowed", if (loc.isOutstationAdvanceAllowed) 1 else 0)
                    cv.put("outstation_min_distance_km", loc.outstationMinDistanceKm.coerceIn(1.0, 500.0))
                }

                if (remoteConfig.activeUiLayout.isNotBlank()) {
                    cv.put("active_ui_layout", remoteConfig.activeUiLayout)
                }

                val sc = remoteConfig.servicesConfig
                cv.put("is_token_service_enabled", if (sc.isTokenServiceEnabled) 1 else 0)
                if (sc.tokenServiceMode.isNotBlank()) {
                    cv.put("token_service_mode", sc.tokenServiceMode)
                    if (sc.tokenServiceMode.equals("FORCE_OPEN", ignoreCase = true)) {
                        cv.put("is_token_service_enabled", 1)
                        cv.put("is_darbar_active", 1)
                    } else if (sc.tokenServiceMode.equals("FORCE_CLOSED", ignoreCase = true)) {
                        cv.put("is_token_service_enabled", 0)
                    }
                }
                cv.put("is_yatra_service_enabled", if (sc.isYatraServiceEnabled) 1 else 0)
                cv.put("is_live_counter_visible", if (sc.isLiveCounterVisible) 1 else 0)
                cv.put("is_events_visible", if (sc.isEventsVisible) 1 else 0)
                cv.put("is_aarti_timings_visible", if (sc.isAartiTimingsVisible) 1 else 0)
                cv.put("is_guruji_info_visible", if (sc.isGurujiInfoVisible) 1 else 0)
                cv.put("is_emergency_notice_visible", if (sc.isEmergencyNoticeVisible) 1 else 0)
                cv.put("scheduled_token_open_timestamp", sc.scheduledTokenOpenTimestamp)
                cv.put("max_daily_tokens", sc.maxDailyTokens)
                if (sc.sundayTokenBannerTitle.isNotBlank()) cv.put("sunday_token_banner_title", sc.sundayTokenBannerTitle)
                if (sc.sundayTokenBannerText.isNotBlank()) cv.put("sunday_token_banner_text", sc.sundayTokenBannerText)
                if (sc.sundayTokenCustomNotice.isNotBlank()) cv.put("sunday_token_custom_notice", sc.sundayTokenCustomNotice)
                cv.put("is_bus_booking_live", if (sc.isBusBookingLive) 1 else 0)
                cv.put("is_payment_feature_live", if (sc.isPaymentFeatureLive) 1 else 0)
                cv.put("can_admin_view_payment_history", if (sc.canAdminViewPaymentHistory) 1 else 0)
                cv.put("can_devotee_view_payment_history", if (sc.canDevoteeViewPaymentHistory) 1 else 0)
                if (sc.ashramUpiId.isNotBlank()) cv.put("ashram_upi_id", sc.ashramUpiId)
                if (sc.ashramUpiName.isNotBlank()) cv.put("ashram_upi_name", sc.ashramUpiName)
                if (sc.customUpiQrUri.isNotBlank()) cv.put("custom_upi_qr_uri", sc.customUpiQrUri)
                if (!hasSettings) {
                    if (sc.busSeatFareAmount > 0) cv.put("bus_seat_fare_amount", sc.busSeatFareAmount)
                    if (sc.badiArziRate > 0) cv.put("badi_arzi_rate", sc.badiArziRate)
                    if (sc.chhotiArziRate > 0) cv.put("chhoti_arzi_rate", sc.chhotiArziRate)
                }
                if (sc.bannerTitle.isNotBlank()) cv.put("banner_title", sc.bannerTitle)
                if (sc.bannerSubtitle.isNotBlank()) cv.put("banner_subtitle", sc.bannerSubtitle)
                if (sc.bannerPhotoUri.isNotBlank()) cv.put("banner_photo_uri", sc.bannerPhotoUri)
                cv.put("is_banner_visible", if (sc.isBannerVisible) 1 else 0)
                if (sc.bannerActionUrl.isNotBlank()) cv.put("banner_action_url", sc.bannerActionUrl)
                cv.put("is_arzi_ledger_live", if (sc.isArziLedgerLive) 1 else 0)
                cv.put("can_admin_view_arzi_ledger", if (sc.canAdminViewArziLedger) 1 else 0)
                cv.put("can_devotee_view_arzi_ledger", if (sc.canDevoteeViewArziLedger) 1 else 0)
                cv.put("can_devotee_view_yatra_diary", if (sc.canDevoteeViewYatraDiary) 1 else 0)

                if (hasSettings) {
                    db.update("ashram_settings", cv, "id = 1", null)
                } else {
                    cv.put("id", 1)
                    db.insertWithOnConflict("ashram_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }

                // Synchronize App Auto-Update info from live cloud config (always safe)
                val upd = remoteConfig.appUpdate
                if (upd != null && upd.latestVersionCode > 0) {
                    val updCv = ContentValues().apply {
                        put("latest_version_code", upd.latestVersionCode)
                        if (upd.latestVersionName.isNotBlank()) put("latest_version_name", upd.latestVersionName)
                        if (upd.apkUrl.isNotBlank()) put("apk_download_url", upd.apkUrl)
                        if (upd.updateNotesHindi.isNotBlank()) put("update_notes", upd.updateNotesHindi)
                        put("is_force_update", if (upd.isForceUpdate) 1 else 0)
                    }
                    db.update("ashram_settings", updCv, "id = 1", null)
                }

                // Synchronize dynamic Ashram Events from cloud
                if (remoteConfig.events.isNotEmpty()) {
                    db.beginTransaction()
                    try {
                        db.delete("ashram_events", null, null)
                        for (ev in remoteConfig.events) {
                            val evCv = ContentValues().apply {
                                put("id", ev.id)
                                put("title_hindi", ev.titleHindi)
                                put("title_english", ev.titleEnglish)
                                put("date_desc_hindi", ev.dateDescriptionHindi)
                                put("date_desc_english", ev.dateDescriptionEnglish)
                                put("details_hindi", ev.detailsHindi)
                                put("details_english", ev.detailsEnglish)
                                put("is_active", if (ev.isActive) 1 else 0)
                                put("created_at", System.currentTimeMillis())
                            }
                            db.insertWithOnConflict("ashram_events", null, evCv, SQLiteDatabase.CONFLICT_REPLACE)
                        }
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Pair(true, remoteConfig)
        } else {
            val localSections = getUiSectionConfigs()
            Pair(true, com.example.shribalajikripadham.data.model.LiveUiConfigDto(sections = localSections))
        }
    }

    suspend fun updateHavanSettings(estimatedCost: Int, rulesNotice: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        try {
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN havan_estimated_cost INTEGER NOT NULL DEFAULT 14000")
            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN havan_rules_notice TEXT")
        } catch (ignored: Exception) {}
        val cv = ContentValues().apply {
            put("havan_estimated_cost", estimatedCost)
            put("havan_rules_notice", rulesNotice)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) {
            persistCurrentSettingsToAllLayers()
            try {
                val url = java.net.URL("https://shribalajikripadham.online/api/havan_service.php")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("X-SBKD-ADMIN-PIN", "1234")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.doOutput = true
                val payload = org.json.JSONObject().apply {
                    put("action", "SAVE_CONFIG")
                    put("admin_pin", "1234")
                    put("havan_estimated_cost", estimatedCost)
                    put("havan_rules_notice", rulesNotice)
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                conn.responseCode
            } catch (e: Exception) {}
        }
        ok
    }

    suspend fun publishLiveConfigToGitHub(
        sections: List<UiSectionConfig>,
        adminName: String = "Super Admin"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val currentSettings = getSettings()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        var finalGurujiPhotoUrl = currentSettings.gurujiPhotoUri
        if (finalGurujiPhotoUrl.isNotBlank() && !finalGurujiPhotoUrl.startsWith("http://") && !finalGurujiPhotoUrl.startsWith("https://")) {
            val uploadedUrl = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.uploadPhoto(
                appContext,
                finalGurujiPhotoUrl,
                "guruji_profile.jpg"
            )
            if (!uploadedUrl.isNullOrBlank()) {
                finalGurujiPhotoUrl = uploadedUrl
                updateGurujiPhoto(uploadedUrl)
            }
        }

        val currentEvents = getAllEvents().map { ev ->
            com.example.shribalajikripadham.data.model.AshramEventConfigDto(
                id = ev.id,
                titleHindi = ev.titleHindi,
                titleEnglish = ev.titleEnglish,
                dateDescriptionHindi = ev.dateDescriptionHindi,
                dateDescriptionEnglish = ev.dateDescriptionEnglish,
                detailsHindi = ev.detailsHindi,
                detailsEnglish = ev.detailsEnglish,
                isActive = ev.isActive
            )
        }

        val config = LiveUiConfigDto(
            updatedAt = isoFormat.format(Date()),
            updatedBy = adminName,
            version = 1,
            activeUiLayout = currentSettings.activeUiLayout,
            ashramDetails = com.example.shribalajikripadham.data.model.AshramDetailsConfigDto(
                ashramName = currentSettings.ashramName,
                gurujiName = currentSettings.gurujiName,
                address = currentSettings.address,
                contactPhone = currentSettings.contactPhone,
                contactPhoneSecondary = "",
                whatsappNumber = currentSettings.whatsappNumber,
                darbarTimings = currentSettings.darbarTimings,
                freeDisclaimer = currentSettings.freeDisclaimer,
                whatsappGroupUrl = currentSettings.whatsappGroupUrl,
                youtubeChannelUrl = currentSettings.youtubeChannelUrl,
                facebookPageUrl = currentSettings.facebookPageUrl,
                instagramUrl = currentSettings.instagramUrl,
                appShareUrl = currentSettings.appShareUrl,
                gurujiPhotoUrl = finalGurujiPhotoUrl
            ),
            emergencyNotice = com.example.shribalajikripadham.data.model.EmergencyNoticeDto(
                isEnabled = currentSettings.isEmergencyNoticeVisible,
                noticeHindi = currentSettings.emergencyNoticeText,
                noticeEnglish = currentSettings.emergencyNoticeText
            ),
            locationConfig = com.example.shribalajikripadham.data.model.LocationConfigDto(
                latitude = currentSettings.latitude,
                longitude = currentSettings.longitude,
                allowedRadiusMeters = currentSettings.allowedRadiusMeters,
                isGeofenceEnforced = currentSettings.isGeofenceEnforced,
                isOutstationAdvanceAllowed = currentSettings.isOutstationAdvanceAllowed,
                outstationMinDistanceKm = currentSettings.outstationMinDistanceKm,
                locationName = currentSettings.ashramName,
                updatedAt = System.currentTimeMillis()
            ),
            servicesConfig = com.example.shribalajikripadham.data.model.ServicesConfigDto(
                isTokenServiceEnabled = currentSettings.isTokenServiceEnabled,
                isYatraServiceEnabled = currentSettings.isYatraServiceEnabled,
                isLiveCounterVisible = currentSettings.isLiveCounterVisible,
                isEventsVisible = currentSettings.isEventsVisible,
                isAartiTimingsVisible = currentSettings.isAartiTimingsVisible,
                isGurujiInfoVisible = currentSettings.isGurujiInfoVisible,
                isEmergencyNoticeVisible = currentSettings.isEmergencyNoticeVisible,
                scheduledTokenOpenTimestamp = currentSettings.scheduledTokenOpenTimestamp,
                maxDailyTokens = currentSettings.maxDailyTokens,
                sundayTokenBannerTitle = currentSettings.sundayTokenBannerTitle,
                sundayTokenBannerText = currentSettings.sundayTokenBannerText,
                sundayTokenCustomNotice = currentSettings.sundayTokenCustomNotice,
                isBusBookingLive = currentSettings.isBusBookingLive,
                isPaymentFeatureLive = currentSettings.isPaymentFeatureLive,
                canAdminViewPaymentHistory = currentSettings.canAdminViewPaymentHistory,
                canDevoteeViewPaymentHistory = currentSettings.canDevoteeViewPaymentHistory,
                ashramUpiId = currentSettings.ashramUpiId,
                ashramUpiName = currentSettings.ashramUpiName,
                customUpiQrUri = currentSettings.customUpiQrUri,
                busSeatFareAmount = currentSettings.busSeatFareAmount,
                isArziLedgerLive = currentSettings.isArziLedgerLive,
                badiArziRate = currentSettings.badiArziRate,
                chhotiArziRate = currentSettings.chhotiArziRate,
                canAdminViewArziLedger = currentSettings.canAdminViewArziLedger,
                canDevoteeViewArziLedger = currentSettings.canDevoteeViewArziLedger,
                canDevoteeViewYatraDiary = currentSettings.canDevoteeViewYatraDiary,
                bannerTitle = currentSettings.bannerTitle,
                bannerSubtitle = currentSettings.bannerSubtitle,
                bannerPhotoUri = currentSettings.bannerPhotoUri,
                isBannerVisible = currentSettings.isBannerVisible,
                bannerActionUrl = currentSettings.bannerActionUrl
            ),
            sections = sections,
            events = currentEvents
        )
        // Also save locally
        saveUiSectionConfigs(sections)
        // Publish to GitHub
        com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishLiveConfig(appContext, config)
    }

    suspend fun publishCurrentSettingsToGitHub(adminName: String = "Super Admin"): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val sections = getUiSectionConfigs()
        publishLiveConfigToGitHub(sections, adminName)
    }

    suspend fun getAllTokensForDate(darbarDate: String = DatabaseHelper.getTodayDateString()): List<Token> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Token>()
        val cursor = db.rawQuery("SELECT * FROM tokens WHERE darbar_date = ? ORDER BY token_number ASC", arrayOf(darbarDate))
        while (cursor.moveToNext()) {
            list.add(
                Token(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    tokenNumber = cursor.getInt(cursor.getColumnIndexOrThrow("token_number")),
                    darbarDate = cursor.getString(cursor.getColumnIndexOrThrow("darbar_date")),
                    patientName = cursor.getString(cursor.getColumnIndexOrThrow("patient_name")),
                    phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone_number")),
                    city = try { cursor.getString(cursor.getColumnIndexOrThrow("city")) } catch (e: Exception) { "डूँगरा जाट (स्थानीय)" },
                    deviceId = cursor.getString(cursor.getColumnIndexOrThrow("device_id")),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    status = TokenStatus.fromRaw(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
                    registeredBy = cursor.getString(cursor.getColumnIndexOrThrow("registered_by")),
                    photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri")) ?: "",
                    isDarshanCompleted = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_darshan_completed")) == 1 } catch (e: Exception) { false },
                    darshanCompletedAt = try { cursor.getLong(cursor.getColumnIndexOrThrow("darshan_completed_at")) } catch (e: Exception) { 0L },
                    originAddress = try { cursor.getString(cursor.getColumnIndexOrThrow("origin_address")) } catch (e: Exception) { "" }.ifEmpty { cursor.getString(cursor.getColumnIndexOrThrow("city")) },
                    destinationAddress = try { cursor.getString(cursor.getColumnIndexOrThrow("destination_address")) } catch (e: Exception) { "श्री बालाजी कृपा धाम, डुंगरा जाट" },
                    distanceKm = try { cursor.getFloat(cursor.getColumnIndexOrThrow("distance_km")) } catch (e: Exception) { -1f },
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun getAllTokenDates(): List<String> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<String>()
        try {
            val cursor = db.rawQuery("SELECT DISTINCT darbar_date FROM tokens ORDER BY darbar_date DESC", null)
            while (cursor.moveToNext()) {
                val d = cursor.getString(0)
                if (!d.isNullOrBlank()) list.add(d)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val today = DatabaseHelper.getTodayDateString()
        if (!list.contains(today)) {
            list.add(0, today)
        }
        list
    }

    suspend fun pushAllTokensToGitHub(darbarDate: String = DatabaseHelper.getTodayDateString()): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val allTokens = getAllTokensForDate(darbarDate)
        if (allTokens.isEmpty()) {
            return@withContext Pair(true, "बैकअप हेतु कोई टोकन नहीं मिला।")
        }
        com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.uploadAllTokensConsolidated(appContext, allTokens, darbarDate)
    }

    suspend fun syncLiveTokensFromCloud(date: String = DatabaseHelper.getTodayDateString()): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
        var totalNew = 0
        try {
            val allRemote = mutableListOf<Token>()

            // 1. Fetch from Central Hostinger MySQL Server (Real-time live queue)
            try {
                val queueObj = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchLiveQueue(date)
                if (queueObj != null && queueObj.optBoolean("success", false)) {
                    val currentServingServer = queueObj.optInt("current_serving_token", 0)
                    if (currentServingServer > 0) {
                        try {
                            val db = dbHelper.writableDatabase
                            val cv = ContentValues().apply { put("running_token_number", currentServingServer) }
                            db.update("ashram_settings", cv, "id = 1", null)
                        } catch (e: Exception) {}
                    }

                    val arr = queueObj.optJSONArray("tokens")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            if (item.optInt("is_reserved_unfilled", 0) == 1) continue // Skip unfilled reserved placeholder slots
                            val tNum = item.optInt("token_number", 0)
                            if (tNum <= 0) continue
                            val statusStr = item.optString("status", "WAITING")
                            val tStatus = TokenStatus.fromRaw(statusStr)

                            val isDarshan = item.optInt("is_darshan_completed", 0) == 1 || tStatus == TokenStatus.COMPLETED
                            val tok = Token(
                                id = item.optLong("id", 0L),
                                tokenNumber = tNum,
                                darbarDate = item.optString("darbar_date", date).ifBlank { date },
                                patientName = item.optString("patient_name", "भक्त"),
                                phoneNumber = item.optString("phone_number", ""),
                                city = item.optString("city", "डूँगरा जाट (स्थानीय)"),
                                deviceId = item.optString("device_id", "HOSTINGER"),
                                latitude = item.optDouble("latitude", 28.3972915),
                                longitude = item.optDouble("longitude", 78.1460410),
                                status = tStatus,
                                registeredBy = item.optString("registered_by", "APP_ONLINE"),
                                photoUri = item.optString("photo_url", ""),
                                isDarshanCompleted = isDarshan,
                                darshanCompletedAt = item.optLong("darshan_completed_at", 0L),
                                originAddress = item.optString("origin_address", ""),
                                destinationAddress = item.optString("destination_address", "श्री बालाजी कृपा धाम, डुंगरा जाट"),
                                distanceKm = item.optDouble("distance_km", -1.0).toFloat(),
                                createdAt = item.optLong("created_at", System.currentTimeMillis())
                            )
                            allRemote.add(tok)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Fetch from GitHub live_tokens.json
            try {
                val ghTokens = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.fetchLiveTokensFromGitHub(appContext, date)
                for (gt in ghTokens) {
                    if (allRemote.none { it.tokenNumber == gt.tokenNumber && it.darbarDate == gt.darbarDate }) {
                        allRemote.add(gt)
                    }
                }
            } catch (e: Exception) {}

            // 3. Fetch from Google Sheet (if configured)
            try {
                val gsTokens = com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager.fetchTokensFromSheet(appContext, date)
                for (gt in gsTokens) {
                    if (allRemote.none { it.tokenNumber == gt.tokenNumber && it.darbarDate == gt.darbarDate }) {
                        allRemote.add(gt)
                    }
                }
            } catch (e: Exception) {}

            if (allRemote.isEmpty()) return@withContext Pair(false, 0)

            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                allRemote.forEach { t ->
                    val c = db.rawQuery(
                        "SELECT id, status FROM tokens WHERE darbar_date = ? AND token_number = ?",
                        arrayOf(t.darbarDate, t.tokenNumber.toString())
                    )
                    val exists = c.moveToFirst()
                    var localStatus = ""
                    var localId: Long = -1
                    if (exists) {
                        localId = c.getLong(0)
                        localStatus = c.getString(1)
                    }
                    c.close()

                    if (!exists) {
                        val cv = ContentValues().apply {
                            put("token_number", t.tokenNumber)
                            put("darbar_date", t.darbarDate)
                            put("patient_name", t.patientName)
                            put("phone_number", t.phoneNumber)
                            put("city", t.city)
                            put("device_id", t.deviceId)
                            put("latitude", t.latitude)
                            put("longitude", t.longitude)
                            put("status", t.status.name)
                            put("registered_by", t.registeredBy)
                            put("photo_uri", t.photoUri)
                            put("is_darshan_completed", if (t.isDarshanCompleted) 1 else 0)
                            put("darshan_completed_at", t.darshanCompletedAt)
                            put("origin_address", t.originAddress)
                            put("destination_address", t.destinationAddress)
                            put("distance_km", t.distanceKm)
                            put("created_at", t.createdAt)
                        }
                        db.insert("tokens", null, cv)
                        totalNew++
                    } else if (localStatus != t.status.name) {
                        val cv = ContentValues().apply {
                            put("status", t.status.name)
                            put("is_darshan_completed", if (t.isDarshanCompleted) 1 else 0)
                        }
                        db.update("tokens", cv, "id = ?", arrayOf(localId.toString()))
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            Pair(true, totalNew)
        } catch (e: Exception) {
            Pair(false, 0)
        }
    }

    suspend fun publishAdminsToGitHub(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val admins = getAllAdmins()
        val updatedAdmins = admins.map { a ->
            if (a.photoUri.isNotBlank() && !a.photoUri.startsWith("http://") && !a.photoUri.startsWith("https://")) {
                val safeUsername = a.username.replace(Regex("[^a-zA-Z0-9_]"), "_")
                val safeFileName = "sevadar_${safeUsername}.jpg"
                val cloudUrl = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.uploadPhoto(
                    appContext,
                    a.photoUri,
                    safeFileName
                )
                if (!cloudUrl.isNullOrBlank()) {
                    updateAdminPhoto(a.id, cloudUrl)
                    a.copy(photoUri = cloudUrl)
                } else a
            } else a
        }
        com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishLiveAdmins(appContext, updatedAdmins)
    }

    suspend fun syncAdminsFromGitHub(): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
        try {
            val remoteAdmins = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.fetchLiveAdmins(appContext)
            if (remoteAdmins.isEmpty()) return@withContext Pair(false, 0)
            val db = dbHelper.writableDatabase
            var synced = 0
            db.beginTransaction()
            try {
                remoteAdmins.forEach { a ->
                    val cv = ContentValues().apply {
                        put("name", a.name.trim())
                        put("phone", a.phoneNumber.trim())
                        put("role", a.role.name)
                        if (a.pinHash.isNotBlank()) put("pin_hash", a.pinHash)
                        if (a.passwordHash.isNotBlank()) put("password_hash", a.passwordHash)
                        if (a.photoUri.isNotBlank()) put("photo_uri", a.photoUri)
                        put("can_manage_tokens", if (a.canManageTokens) 1 else 0)
                        put("can_issue_manual_tokens", if (a.canIssueManualTokens) 1 else 0)
                        put("can_cancel_tokens", if (a.canCancelTokens) 1 else 0)
                        put("can_delete_tokens", if (a.canDeleteTokens) 1 else 0)
                        put("can_custom_token_number", if (a.canSetCustomTokenNumber) 1 else 0)
                        put("can_manage_yatra", if (a.canManageYatra) 1 else 0)
                        put("can_manage_expenses", if (a.canManageExpenses) 1 else 0)
                        put("can_change_location", if (a.canChangeLocation) 1 else 0)
                        put("can_send_notifications", if (a.canSendNotifications) 1 else 0)
                        put("can_edit_ashram_info", if (a.canEditAshramInfo) 1 else 0)
                        put("can_manage_admins", if (a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_view_devotee_photos", if (a.canViewDevoteePhotos) 1 else 0)
                        put("can_issue_tokens_anywhere", if (a.canIssueTokensAnywhere) 1 else 0)
                        put("can_scan_paper_register", if (a.canScanPaperRegister) 1 else 0)
                        put("can_manage_parchas", if (a.canManageParchas) 1 else 0)
                        put("can_manage_arzi", if (a.canManageArzi || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_manage_havan", if (a.canManageHavan || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_export_pdf", if (a.canExportPdf) 1 else 0)
                        put("can_manage_voice_settings", if (a.canManageVoiceSettings || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_manage_daily_darshan", if (a.canManageDailyDarshan || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_manage_live_studio", if (a.canManageLiveStudio || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_manage_aarti_bhajan", if (a.canManageAartiBhajan || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_manage_ui_control", if (a.canManageUiControl || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_manage_tuesday_darbar", if (a.canManageTuesdayDarbar || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_manage_id_cards", if (a.canManageIdCards || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_manage_helpdesk", if (a.canManageHelpdesk || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_view_payment_ledger", if (a.canViewPaymentLedger || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_manage_website", if (a.canManageWebsite || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_manage_services_toggles", if (a.canManageServicesToggles || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("can_manage_distances", if (a.canManageDistances || a.role == AdminRole.SUPER_ADMIN) 1 else 0)
                        put("is_active", if (a.isActive) 1 else 0)
                    }

                    if (a.role == AdminRole.SUPER_ADMIN) {
                        if (a.passwordHash.isNotBlank()) {
                            cv.put("password_hash", a.passwordHash)
                        } else {
                            cv.put("password_hash", DatabaseHelper.MASTER_PWD_SALTED_HASH)
                        }
                        val count = db.update("admins", cv, "role = 'SUPER_ADMIN'", null)
                        if (count > 0) synced++
                    } else {
                        val count = db.update("admins", cv, "username = ?", arrayOf(a.username))
                        if (count > 0) {
                            synced++
                        } else {
                            cv.put("username", a.username)
                            cv.put("created_at", a.createdAt)
                            val inserted = db.insert("admins", null, cv)
                            if (inserted != -1L) synced++
                        }
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            Pair(true, synced)
        } catch (e: Exception) {
            Pair(false, 0)
        }
    }

    suspend fun syncAdminsFromCentralHostinger(): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
        try {
            val remoteAdmins = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchAdminsFromCentralServer()
            val db = dbHelper.writableDatabase
            var synced = 0
            if (remoteAdmins.isNotEmpty()) {
                db.beginTransaction()
                try {
                    remoteAdmins.forEach { a ->
                        val uname = a.optString("username", "").trim()
                        if (uname.isNotBlank()) {
                            val cv = ContentValues().apply {
                                put("name", a.optString("name", "").trim())
                                put("phone", a.optString("phone_number", "").trim())
                                put("role", a.optString("role", "SEVADAR").trim())
                                val rPin = a.optString("display_pin", a.optString("raw_pin", a.optString("pin", "1234")))
                                val rPass = a.optString("display_password", a.optString("raw_password", ""))
                                put("raw_pin", rPin)
                                put("raw_password", rPass)
                                put("pin_hash", DatabaseHelper.hashPin(rPin))
                                if (rPass.isNotBlank()) put("password_hash", DatabaseHelper.hashPassword(rPass))
                                put("is_active", if (a.optInt("is_active", 1) == 1) 1 else 0)
                            }
                            if (uname.equals("admin", ignoreCase = true) || a.optString("role") == "SUPER_ADMIN") {
                                val count = db.update("admins", cv, "role = 'SUPER_ADMIN' OR username = 'admin'", null)
                                if (count > 0) synced++
                            } else {
                                val count = db.update("admins", cv, "username = ?", arrayOf(uname))
                                if (count > 0) {
                                    synced++
                                } else {
                                    cv.put("username", uname)
                                    cv.put("can_manage_tokens", 1)
                                    cv.put("can_issue_manual_tokens", 1)
                                    cv.put("can_manage_yatra", 1)
                                    cv.put("can_manage_expenses", 1)
                                    cv.put("can_change_location", 0)
                                    cv.put("can_send_notifications", 0)
                                    cv.put("can_edit_ashram_info", 0)
                                    cv.put("can_manage_admins", 0)
                                    cv.put("can_view_devotee_photos", 0)
                                    cv.put("can_issue_tokens_anywhere", 0)
                                    cv.put("can_scan_paper_register", 0)
                                    cv.put("can_manage_parchas", 0)
                                    cv.put("can_manage_arzi", 0)
                                    cv.put("can_manage_havan", 0)
                                    cv.put("can_cancel_tokens", 0)
                                    cv.put("can_delete_tokens", 0)
                                    cv.put("can_custom_token_number", 0)
                                    cv.put("can_export_pdf", 1)
                                    cv.put("can_manage_voice_settings", 0)
                                    cv.put("can_manage_daily_darshan", 0)
                                    cv.put("can_manage_live_studio", 0)
                                    cv.put("can_manage_aarti_bhajan", 0)
                                    cv.put("can_manage_ui_control", 0)
                                    cv.put("can_manage_tuesday_darbar", 0)
                                    cv.put("can_manage_id_cards", 0)
                                    cv.put("can_manage_helpdesk", 0)
                                    cv.put("can_view_payment_ledger", 0)
                                    cv.put("can_manage_website", 0)
                                    cv.put("can_manage_services_toggles", 0)
                                    cv.put("can_manage_distances", 0)
                                    cv.put("photo_uri", "")
                                    cv.put("created_at", a.optLong("created_at", System.currentTimeMillis()))
                                    val ins = db.insert("admins", null, cv)
                                    if (ins != -1L) synced++
                                }
                            }
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }

            // Two-way sync: Push any local admins that are not on the server
            val localAdmins = getAllAdmins()
            val serverUsernames = remoteAdmins.map { it.optString("username", "").lowercase() }.toSet()
            for (la in localAdmins) {
                if (!serverUsernames.contains(la.username.lowercase()) && la.username.isNotBlank()) {
                    try {
                        com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.saveAdminToCentralServer(
                            name = la.name,
                            username = la.username,
                            phone = la.phoneNumber,
                            role = la.role.name,
                            password = la.rawPassword,
                            pin = la.rawPin.ifBlank { "1234" },
                            isActive = la.isActive
                        )
                    } catch (_: Exception) {}
                }
            }

            // Immediately lock into permanent vault
            try {
                com.example.shribalajikripadham.data.local.AppPermanentVault.saveVault(appContext, getAllAdmins(), getSettings())
            } catch (_: Exception) {}

            Pair(true, synced)
        } catch (e: Exception) {
            Pair(false, 0)
        }
    }

    // --- Google Sheets Central Token Sync ---
    suspend fun syncTokensFromGoogleSheet(date: String = DatabaseHelper.getTodayDateString()): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
        try {
            val remoteTokens = com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager.fetchTokensFromSheet(appContext, date)
            if (remoteTokens.isEmpty()) {
                return@withContext Pair(false, 0)
            }
            val db = dbHelper.writableDatabase
            var newCount = 0
            db.beginTransaction()
            try {
                remoteTokens.forEach { t ->
                    val c = db.rawQuery(
                        "SELECT id FROM tokens WHERE darbar_date = ? AND token_number = ?",
                        arrayOf(t.darbarDate, t.tokenNumber.toString())
                    )
                    val exists = c.moveToFirst()
                    c.close()

                    if (!exists) {
                        val cv = ContentValues().apply {
                            put("token_number", t.tokenNumber)
                            put("darbar_date", t.darbarDate)
                            put("patient_name", t.patientName)
                            put("phone_number", t.phoneNumber)
                            put("city", t.city)
                            put("device_id", t.deviceId)
                            put("latitude", t.latitude)
                            put("longitude", t.longitude)
                            put("status", t.status.name)
                            put("registered_by", t.registeredBy)
                            put("photo_uri", t.photoUri)
                            put("is_darshan_completed", if (t.isDarshanCompleted) 1 else 0)
                            put("darshan_completed_at", t.darshanCompletedAt)
                            put("origin_address", t.originAddress)
                            put("destination_address", t.destinationAddress)
                            put("distance_km", t.distanceKm)
                            put("created_at", t.createdAt)
                        }
                        db.insertWithOnConflict("tokens", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                        newCount++
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            Pair(true, newCount)
        } catch (e: Exception) {
            Pair(false, 0)
        }
    }

    // --- Devotee Master Directory & Global Smart Auto-Fill ---

    suspend fun upsertDevoteeDirectory(
        name: String,
        phone: String,
        city: String = "",
        age: Int = 0,
        gender: String = "",
        photoUri: String = "",
        sourceModule: String = "TOKEN",
        lastVisitDate: String = DatabaseHelper.getTodayDateString()
    ) = withContext(Dispatchers.IO) {
        upsertDevoteeDirectoryInternal(name, phone, city, age, gender, photoUri, sourceModule, lastVisitDate)
    }

    private fun upsertDevoteeDirectoryInternal(
        name: String,
        phone: String,
        city: String = "",
        age: Int = 0,
        gender: String = "",
        photoUri: String = "",
        sourceModule: String = "TOKEN",
        lastVisitDate: String = DatabaseHelper.getTodayDateString()
    ) {
        val cleanName = name.trim()
        val cleanPhone = phone.trim().replace("+91", "").replace(" ", "").replace("-", "")
        if (cleanName.isBlank() && cleanPhone.isBlank()) return

        try {
            val db = dbHelper.writableDatabase
            val devId = if (cleanPhone.isNotBlank()) "DEV_$cleanPhone" else "DEV_NAME_${Math.abs(cleanName.hashCode())}"

            val cursor = db.rawQuery(
                "SELECT id, visit_count, city, age, gender, photo_uri FROM devotee_directory WHERE phone_number = ? OR devotee_id = ? OR patient_name = ? LIMIT 1",
                arrayOf(cleanPhone, devId, cleanName)
            )

            if (cursor.moveToFirst()) {
                val rowId = cursor.getLong(0)
                val currentVisits = cursor.getInt(1)
                val existingCity = cursor.getString(2) ?: ""
                val existingAge = cursor.getInt(3)
                val existingGender = cursor.getString(4) ?: ""
                val existingPhoto = cursor.getString(5) ?: ""
                cursor.close()

                val finalCity = if (city.isNotBlank() && city != "डूँगरा जाट (स्थानीय)") city else existingCity
                val finalAge = if (age > 0) age else existingAge
                val finalGender = if (gender.isNotBlank()) gender else existingGender
                val finalPhoto = if (photoUri.isNotBlank()) photoUri else existingPhoto

                val cv = ContentValues().apply {
                    put("patient_name", cleanName)
                    if (cleanPhone.isNotBlank()) put("phone_number", cleanPhone)
                    put("city", finalCity)
                    put("age", finalAge)
                    put("gender", finalGender)
                    put("photo_uri", finalPhoto)
                    put("visit_count", currentVisits + 1)
                    put("last_visit_date", lastVisitDate)
                    put("source_module", sourceModule)
                    put("updated_at", System.currentTimeMillis())
                }
                db.update("devotee_directory", cv, "id = ?", arrayOf(rowId.toString()))
            } else {
                cursor.close()
                val cv = ContentValues().apply {
                    put("devotee_id", devId)
                    put("patient_name", cleanName)
                    put("phone_number", cleanPhone)
                    put("city", city.trim())
                    put("age", age)
                    put("gender", gender)
                    put("photo_uri", photoUri)
                    put("visit_count", 1)
                    put("last_visit_date", lastVisitDate)
                    put("source_module", sourceModule)
                    put("updated_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict("devotee_directory", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
        } catch (e: Exception) {
            android.util.Log.e("AshramRepository", "Error upserting devotee directory: ${e.message}")
        }
    }

    /**
     * 🌐 Central Devotee Cloud Directory Fetcher (Hostinger central sync)
     * Powers instant auto-fill by name, phone, or village even on completely fresh installs or after Clear Data!
     */
    suspend fun fetchDevoteesFromCloud(query: String, limit: Int = 6): List<DevoteeDirectoryEntry> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()
        val list = mutableListOf<DevoteeDirectoryEntry>()
        try {
            val encodedQ = java.net.URLEncoder.encode(q, "UTF-8")
            val url = java.net.URL("https://shribalajikripadham.online/api/search_devotee.php?query=$encodedQ&limit=$limit")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 4000
                setRequestProperty("User-Agent", "ShriBalajiKripaDham-App/1.0")
            }
            if (conn.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = org.json.JSONObject(jsonStr)
                if (root.optBoolean("success", false)) {
                    val arr = root.optJSONArray("devotees")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val name = obj.optString("patient_name", "").trim()
                            val phone = obj.optString("phone_number", "").trim()
                            val city = obj.optString("city", "डूँगरा जाट (स्थानीय)").trim()
                            val photo = obj.optString("photo_url", "").trim()
                            val visits = obj.optInt("visit_count", 1)
                            val lastVisit = obj.optString("last_visit_date", "")
                            if (name.isNotBlank() || phone.isNotBlank()) {
                                val entry = DevoteeDirectoryEntry(
                                    id = 0,
                                    devoteeId = phone,
                                    patientName = name,
                                    phoneNumber = phone,
                                    city = city,
                                    photoUri = photo,
                                    visitCount = visits,
                                    lastVisitDate = lastVisit,
                                    sourceModule = "CLOUD"
                                )
                                list.add(entry)
                                // Cache locally in devotee_directory for instant offline search
                                try {
                                    upsertDevoteeDirectory(
                                        name = name,
                                        phone = phone,
                                        city = city,
                                        photoUri = photo,
                                        lastVisitDate = lastVisit.ifBlank { DatabaseHelper.getTodayDateString() },
                                        sourceModule = "CLOUD"
                                    )
                                } catch (e: Exception) {}
                            }
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            android.util.Log.w("AshramRepository", "Cloud devotee search exception: ${e.message}")
        }
        list
    }

    suspend fun searchDevoteeDirectory(query: String, limit: Int = 6): List<DevoteeDirectoryEntry> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()
        val cleanPhone = q.replace("+91", "").replace(" ", "").replace("-", "")
        val db = dbHelper.readableDatabase
        val results = mutableListOf<DevoteeDirectoryEntry>()

        try {
            val cursor = db.rawQuery(
                """
                SELECT id, devotee_id, patient_name, phone_number, city, age, gender, photo_uri, last_visit_date, visit_count, source_module, updated_at 
                FROM devotee_directory 
                WHERE phone_number LIKE ? OR patient_name LIKE ? OR devotee_id LIKE ? OR city LIKE ?
                ORDER BY visit_count DESC, updated_at DESC 
                LIMIT ?
                """.trimIndent(),
                arrayOf("%$cleanPhone%", "%$q%", "%$q%", "%$q%", limit.toString())
            )
            while (cursor.moveToNext()) {
                results.add(
                    DevoteeDirectoryEntry(
                        id = cursor.getLong(0),
                        devoteeId = cursor.getString(1),
                        patientName = cursor.getString(2),
                        phoneNumber = cursor.getString(3),
                        city = cursor.getString(4) ?: "",
                        age = cursor.getInt(5),
                        gender = cursor.getString(6) ?: "",
                        photoUri = cursor.getString(7) ?: "",
                        lastVisitDate = cursor.getString(8) ?: "",
                        visitCount = cursor.getInt(9),
                        sourceModule = cursor.getString(10) ?: "TOKEN",
                        updatedAt = cursor.getLong(11)
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            android.util.Log.e("AshramRepository", "Error searching devotee directory: ${e.message}")
        }

        // If local SQLite has fewer results, enrich from Hostinger Cloud directory:
        if (results.size < limit) {
            val needed = limit - results.size
            val cloudList = fetchDevoteesFromCloud(q, needed)
            for (c in cloudList) {
                if (results.none { it.phoneNumber == c.phoneNumber }) {
                    results.add(c)
                }
            }
        }

        results
    }

    suspend fun searchDevoteeByPhone(phone: String): DevoteeFaceProfile? = withContext(Dispatchers.IO) {
        val cleanPhone = phone.trim().replace("+91", "").replace(" ", "").replace("-", "")
        if (cleanPhone.length < 10) return@withContext null
        val db = dbHelper.readableDatabase

        // 1. Search devotee_directory first (covers tokens, arzis, manual tokens)
        try {
            val dirCursor = db.rawQuery(
                "SELECT patient_name, phone_number, city, photo_uri, visit_count FROM devotee_directory WHERE phone_number LIKE ? ORDER BY updated_at DESC LIMIT 1",
                arrayOf("%$cleanPhone%")
            )
            if (dirCursor.moveToFirst()) {
                val profile = DevoteeFaceProfile(
                    id = 0,
                    patientName = dirCursor.getString(0),
                    phoneNumber = dirCursor.getString(1),
                    city = try { dirCursor.getString(2) ?: "डूँगरा जाट (स्थानीय)" } catch (e: Exception) { "डूँगरा जाट (स्थानीय)" },
                    faceVector = FloatArray(0),
                    photoUri = dirCursor.getString(3) ?: "",
                    visitCount = dirCursor.getInt(4),
                    lastConfidence = 1.0f,
                    lastVerifiedAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                dirCursor.close()
                return@withContext profile
            }
            dirCursor.close()
        } catch (e: Exception) {}

        // 2. Fallback to devotee_face_profiles
        val cursor = db.rawQuery(
            "SELECT * FROM devotee_face_profiles WHERE phone_number LIKE ? ORDER BY last_verified_at DESC LIMIT 1",
            arrayOf("%$cleanPhone%")
        )
        var profile: DevoteeFaceProfile? = null
        if (cursor.moveToFirst()) {
            val blob = cursor.getBlob(cursor.getColumnIndexOrThrow("face_vector"))
            val vector = FaceEmbeddingEngine.blobToVector(blob)
            profile = DevoteeFaceProfile(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                patientName = cursor.getString(cursor.getColumnIndexOrThrow("patient_name")),
                phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone_number")),
                city = try { cursor.getString(cursor.getColumnIndexOrThrow("city")) } catch (e: Exception) { "डूँगरा जाट (स्थानीय)" },
                faceVector = vector,
                photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri")) ?: "",
                visitCount = cursor.getInt(cursor.getColumnIndexOrThrow("visit_count")),
                lastConfidence = cursor.getFloat(cursor.getColumnIndexOrThrow("last_confidence")),
                lastVerifiedAt = cursor.getLong(cursor.getColumnIndexOrThrow("last_verified_at")),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
            )
        }
        cursor.close()

        // 3. Fallback to Cloud search (Seamless recovery on fresh install/Clear Data!)
        if (profile == null) {
            val cloudMatches = fetchDevoteesFromCloud(cleanPhone, 1)
            val cloud = cloudMatches.firstOrNull()
            if (cloud != null) {
                profile = DevoteeFaceProfile(
                    id = 0,
                    patientName = cloud.patientName,
                    phoneNumber = cloud.phoneNumber,
                    city = cloud.city.ifBlank { "डूँगरा जाट (स्थानीय)" },
                    faceVector = FloatArray(0),
                    photoUri = cloud.photoUri,
                    visitCount = cloud.visitCount,
                    lastConfidence = 1.0f,
                    lastVerifiedAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
            }
        }

        profile
    }

    suspend fun searchDevoteesByName(query: String, limit: Int = 6): List<DevoteeFaceProfile> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val db = dbHelper.readableDatabase
        val list = mutableListOf<DevoteeFaceProfile>()

        // 1. Search devotee_directory
        try {
            val dirCursor = db.rawQuery(
                "SELECT patient_name, phone_number, city, photo_uri, visit_count FROM devotee_directory WHERE patient_name LIKE ? OR phone_number LIKE ? ORDER BY visit_count DESC, updated_at DESC LIMIT ?",
                arrayOf("%$trimmed%", "%$trimmed%", limit.toString())
            )
            while (dirCursor.moveToNext()) {
                list.add(
                    DevoteeFaceProfile(
                        id = 0,
                        patientName = dirCursor.getString(0),
                        phoneNumber = dirCursor.getString(1),
                        city = try { dirCursor.getString(2) ?: "डूँगरा जाट (स्थानीय)" } catch (e: Exception) { "डूँगरा जाट (स्थानीय)" },
                        faceVector = FloatArray(0),
                        photoUri = dirCursor.getString(3) ?: "",
                        visitCount = dirCursor.getInt(4),
                        lastConfidence = 1.0f,
                        lastVerifiedAt = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            dirCursor.close()
        } catch (e: Exception) {}

        // 2. Fallback to devotee_face_profiles
        if (list.size < limit) {
            val cursor = db.rawQuery(
                "SELECT * FROM devotee_face_profiles WHERE patient_name LIKE ? ORDER BY last_verified_at DESC LIMIT ?",
                arrayOf("%$trimmed%", limit.toString())
            )
            while (cursor.moveToNext()) {
                val blob = cursor.getBlob(cursor.getColumnIndexOrThrow("face_vector"))
                val vector = FaceEmbeddingEngine.blobToVector(blob)
                val pName = cursor.getString(cursor.getColumnIndexOrThrow("patient_name"))
                val pPhone = cursor.getString(cursor.getColumnIndexOrThrow("phone_number"))
                if (list.none { it.phoneNumber == pPhone }) {
                    list.add(
                        DevoteeFaceProfile(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                            patientName = pName,
                            phoneNumber = pPhone,
                            city = try { cursor.getString(cursor.getColumnIndexOrThrow("city")) } catch (e: Exception) { "डूँगरा जाट (स्थानीय)" },
                            faceVector = vector,
                            photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri")) ?: "",
                            visitCount = cursor.getInt(cursor.getColumnIndexOrThrow("visit_count")),
                            lastConfidence = cursor.getFloat(cursor.getColumnIndexOrThrow("last_confidence")),
                            lastVerifiedAt = cursor.getLong(cursor.getColumnIndexOrThrow("last_verified_at")),
                            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                        )
                    )
                }
            }
            cursor.close()
        }

        // 3. Fallback to Cloud search (Seamless recovery on fresh install/Clear Data!)
        if (list.size < limit) {
            val needed = limit - list.size
            val cloudList = fetchDevoteesFromCloud(trimmed, needed)
            for (c in cloudList) {
                if (list.none { it.phoneNumber == c.phoneNumber || (it.patientName.equals(c.patientName, ignoreCase = true) && it.city == c.city) }) {
                    list.add(
                        DevoteeFaceProfile(
                            id = 0,
                            patientName = c.patientName,
                            phoneNumber = c.phoneNumber,
                            city = c.city.ifBlank { "डूँगरा जाट (स्थानीय)" },
                            faceVector = FloatArray(0),
                            photoUri = c.photoUri,
                            visitCount = c.visitCount,
                            lastConfidence = 1.0f,
                            lastVerifiedAt = System.currentTimeMillis(),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        list
    }

    suspend fun upsertDevoteeProfile(
        name: String,
        phone: String,
        city: String = "डूँगरा जाट (स्थानीय)",
        faceVector: FloatArray? = null,
        photoUri: String = "",
        registeredBy: String = "APP"
    ): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cleanPhone = phone.trim().replace("+91", "").replace(" ", "").replace("-", "")
        val safeCity = if (city.isBlank()) "डूँगरा जाट (स्थानीय)" else city.trim()
        val safeName = name.trim()

        val cursor = db.rawQuery("SELECT id, face_vector FROM devotee_face_profiles WHERE phone_number = ? LIMIT 1", arrayOf(cleanPhone))
        val exists = cursor.moveToFirst()
        val existingId = if (exists) cursor.getLong(0) else -1L
        val existingBlob = if (exists) cursor.getBlob(1) else null
        cursor.close()

        val vector = when {
            faceVector != null && faceVector.isNotEmpty() -> FaceEmbeddingEngine.l2Normalize(faceVector)
            existingBlob != null -> FaceEmbeddingEngine.blobToVector(existingBlob)
            else -> FloatArray(128)
        }
        val blob = FaceEmbeddingEngine.vectorToBlob(vector)

        val cv = ContentValues().apply {
            put("patient_name", safeName)
            put("phone_number", cleanPhone)
            put("city", safeCity)
            put("face_vector", blob)
            if (photoUri.isNotBlank()) put("photo_uri", photoUri)
            put("last_verified_at", System.currentTimeMillis())
        }

        val resultId = if (exists && existingId > 0) {
            db.update("devotee_face_profiles", cv, "id = ?", arrayOf(existingId.toString()))
            existingId
        } else {
            cv.put("visit_count", 1)
            cv.put("last_confidence", 1.0f)
            cv.put("created_at", System.currentTimeMillis())
            db.insert("devotee_face_profiles", null, cv)
        }

        // 1. Primary Cloud Upload to Hostinger MySQL Central Devotee & Biometric Registry
        try {
            com.example.shribalajikripadham.data.network.CentralFaceSyncManager.uploadFaceProfile(
                context = appContext,
                name = safeName,
                phone = cleanPhone,
                city = safeCity,
                faceVector = vector,
                photoUri = photoUri
            )
        } catch (e: Exception) {}

        // 2. Secondary Cloud Mirror to Google Sheets Universal Devotee Registry
        try {
            val profile = DevoteeFaceProfile(
                id = resultId,
                patientName = safeName,
                phoneNumber = cleanPhone,
                city = safeCity,
                faceVector = vector,
                photoUri = photoUri
            )
            com.example.shribalajikripadham.data.network.CentralDevoteeSyncManager.uploadDevoteeProfile(
                appContext,
                profile,
                registeredBy
            )
        } catch (e: Exception) {}

        resultId
    }

    suspend fun syncDevoteesFromCloud(): Pair<Int, String> = withContext(Dispatchers.IO) {
        // 1. Primary Sync from Hostinger Central MySQL
        val hostingerCount = try {
            com.example.shribalajikripadham.data.network.CentralFaceSyncManager.fetchAndSyncFaceProfiles(appContext)
        } catch (e: Exception) { 0 }

        // 2. Secondary Sync from Google Sheets
        val sheetResult = try {
            com.example.shribalajikripadham.data.network.CentralDevoteeSyncManager.syncAllDevoteesFromCloud(appContext, dbHelper)
        } catch (e: Exception) { Pair(0, e.localizedMessage ?: "") }

        val total = hostingerCount + sheetResult.first
        Pair(total, "✓ केंद्रीय होस्टिंगर MySQL ($hostingerCount) व गूगल शीट (${sheetResult.first}) से कुल $total प्रोफाइल सिंक हुए!")
    }

    suspend fun getActiveDevicesTelemetry(): Triple<Int, Int, List<com.example.shribalajikripadham.data.model.DevicePresence>> {
        return com.example.shribalajikripadham.data.network.AppTelemetryManager.fetchActiveDevicesFromSheet(appContext)
    }

    fun getLocalActiveDevices(): List<com.example.shribalajikripadham.data.model.DevicePresence> {
        return com.example.shribalajikripadham.data.network.AppTelemetryManager.getLocalDevices(appContext)
    }


    // ==========================================
    // SACRED PARCHAS & DOCUMENTS REPOSITORY
    // ==========================================

    fun seedDefaultParchasIfEmpty() {
        // Strict Zero-Dummy Policy: Absolutely no auto-seeding of placeholder parchas.
        // If the table is empty, it remains clean until an admin/sevadar creates a real parcha.
    }

    private fun setParchasSeededFlag(b: Boolean) {
        // Zero-Dummy Policy: No dummy canonical seeding flag needed
    }

    fun getAllPublicParchas(): List<com.example.shribalajikripadham.data.model.SacredParcha> {
        try {
            val db = dbHelper.readableDatabase
            val list = mutableListOf<com.example.shribalajikripadham.data.model.SacredParcha>()
            val cursor = db.rawQuery(
                "SELECT * FROM sacred_parchas WHERE is_published = 1 AND is_hidden = 0 ORDER BY id ASC",
                null
            )
            while (cursor.moveToNext()) {
                list.add(parseParchaCursor(cursor))
            }
            cursor.close()
            return list
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    fun getAllAdminParchas(): List<com.example.shribalajikripadham.data.model.SacredParcha> {
        try {
            val db = dbHelper.readableDatabase
            val list = mutableListOf<com.example.shribalajikripadham.data.model.SacredParcha>()
            val cursor = db.rawQuery(
                "SELECT * FROM sacred_parchas ORDER BY id ASC",
                null
            )
            while (cursor.moveToNext()) {
                list.add(parseParchaCursor(cursor))
            }
            cursor.close()
            return list
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    fun getParchaById(parchaId: String): com.example.shribalajikripadham.data.model.SacredParcha? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM sacred_parchas WHERE parcha_id = ?", arrayOf(parchaId))
        var result: com.example.shribalajikripadham.data.model.SacredParcha? = null
        if (cursor.moveToFirst()) {
            result = parseParchaCursor(cursor)
        }
        cursor.close()
        return result
    }

    fun upsertParcha(parcha: com.example.shribalajikripadham.data.model.SacredParcha): Boolean {
        val db = dbHelper.writableDatabase
        val cv = android.content.ContentValues().apply {
            put("parcha_id", parcha.parchaId)
            put("title", parcha.title)
            put("category", parcha.category.name)
            put("subtitle", parcha.subtitle)
            put("samagri_list", parcha.samagriListToJson())
            put("vidhi_text", parcha.vidhiStepsToJson())
            put("precautions", parcha.precautionsToJson())
            put("mantra_text", parcha.mantraText)
            put("image_uri", parcha.imageUri)
            put("is_published", if (parcha.isPublished) 1 else 0)
            put("is_hidden", if (parcha.isHidden) 1 else 0)
            put("view_count", parcha.viewCount)
            put("download_count", parcha.downloadCount)
            put("created_by", parcha.createdBy)
            put("created_at", parcha.createdAt)
            put("updated_at", System.currentTimeMillis())
        }
        val rowId = db.insertWithOnConflict("sacred_parchas", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        setParchasSeededFlag(true)
        return rowId != -1L
    }

    fun toggleParchaHidden(parchaId: String, isHidden: Boolean): Boolean {
        val db = dbHelper.writableDatabase
        val cv = android.content.ContentValues().apply {
            put("is_hidden", if (isHidden) 1 else 0)
            put("updated_at", System.currentTimeMillis())
        }
        val affected = db.update("sacred_parchas", cv, "parcha_id = ?", arrayOf(parchaId))
        return affected > 0
    }

    fun deleteParcha(parchaId: String): Boolean {
        val db = dbHelper.writableDatabase
        val affected = db.delete("sacred_parchas", "parcha_id = ?", arrayOf(parchaId))
        return affected > 0
    }

    suspend fun upsertParchaAndPublish(
        parcha: com.example.shribalajikripadham.data.model.SacredParcha,
        adminName: String = "Super Admin"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val localOk = upsertParcha(parcha)
        if (localOk) {
            val (pubOk, pubMsg) = publishAllParchasToGitHub(adminName)
            Pair(pubOk, pubMsg)
        } else {
            Pair(false, "स्थानीय डेटाबेस में सुरक्षित नहीं हुआ")
        }
    }

    suspend fun toggleParchaHiddenAndPublish(
        parchaId: String,
        isHidden: Boolean,
        adminName: String = "Super Admin"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val localOk = toggleParchaHidden(parchaId, isHidden)
        if (localOk) {
            val (pubOk, pubMsg) = publishAllParchasToGitHub(adminName)
            Pair(pubOk, pubMsg)
        } else {
            Pair(false, "स्थिति अपडेट नहीं हो सकी")
        }
    }

    suspend fun deleteParchaAndPublish(
        parchaId: String,
        adminName: String = "Super Admin"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val localOk = deleteParcha(parchaId)
        if (localOk) {
            try {
                com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.deleteCentralParcha(parchaId)
            } catch (ignored: Exception) {}
            val (pubOk, pubMsg) = publishAllParchasToGitHub(adminName)
            Pair(true, "पर्चा सफलतापूर्वक हटा दिया गया")
        } else {
            Pair(false, "पर्चा हटाया नहीं जा सका")
        }
    }

    suspend fun syncLiveParchasFromGitHub(): Pair<Boolean, List<com.example.shribalajikripadham.data.model.SacredParcha>> = withContext(Dispatchers.IO) {
        val remoteList = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchLiveParchas()
            ?: com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.fetchLiveParchas()
        if (remoteList != null) {
            try {
                val db = dbHelper.writableDatabase
                val remoteIds = remoteList.map { it.parchaId }.toSet()

                // Remove parchas from local DB that were deleted by Super Admin in the cloud
                val localCursor = db.rawQuery("SELECT parcha_id FROM sacred_parchas", null)
                val toDelete = mutableListOf<String>()
                while (localCursor.moveToNext()) {
                    val pid = localCursor.getString(0)
                    if (!remoteIds.contains(pid)) {
                        toDelete.add(pid)
                    }
                }
                localCursor.close()

                for (pid in toDelete) {
                    db.delete("sacred_parchas", "parcha_id = ?", arrayOf(pid))
                }

                // Upsert all remote parchas with their updated state
                for (p in remoteList) {
                    val cv = android.content.ContentValues().apply {
                        put("parcha_id", p.parchaId)
                        put("title", p.title)
                        put("category", p.category.name)
                        put("subtitle", p.subtitle)
                        put("samagri_list", p.samagriListToJson())
                        put("vidhi_text", p.vidhiStepsToJson())
                        put("precautions", p.precautionsToJson())
                        put("mantra_text", p.mantraText)
                        put("image_uri", p.imageUri)
                        put("is_published", if (p.isPublished) 1 else 0)
                        put("is_hidden", if (p.isHidden) 1 else 0)
                        put("view_count", p.viewCount)
                        put("download_count", p.downloadCount)
                        put("created_by", p.createdBy)
                        put("created_at", p.createdAt)
                        put("updated_at", p.updatedAt)
                    }
                    db.insertWithOnConflict("sacred_parchas", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                }

                setParchasSeededFlag(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Pair(true, remoteList)
        } else {
            Pair(false, emptyList())
        }
    }

    suspend fun publishAllParchasToGitHub(adminName: String = "Super Admin"): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val allParchas = getAllAdminParchas()
        com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishLiveParchas(appContext, allParchas, adminName)
    }

    fun incrementParchaDownload(parchaId: String) {
        val db = dbHelper.writableDatabase
        db.execSQL("UPDATE sacred_parchas SET download_count = download_count + 1 WHERE parcha_id = ?", arrayOf(parchaId))
    }

    private fun parseParchaCursor(cursor: android.database.Cursor): com.example.shribalajikripadham.data.model.SacredParcha {
        val categoryStr = try { cursor.getString(cursor.getColumnIndexOrThrow("category")) } catch (e: Exception) { "OTHER" }
        val category = com.example.shribalajikripadham.data.model.ParchaCategory.fromString(categoryStr)
        val samagriJson = try { cursor.getString(cursor.getColumnIndexOrThrow("samagri_list")) } catch (e: Exception) { "" }
        val vidhiJson = try { cursor.getString(cursor.getColumnIndexOrThrow("vidhi_text")) } catch (e: Exception) { "" }
        val precautionsJson = try { cursor.getString(cursor.getColumnIndexOrThrow("precautions")) } catch (e: Exception) { "" }

        return com.example.shribalajikripadham.data.model.SacredParcha(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            parchaId = cursor.getString(cursor.getColumnIndexOrThrow("parcha_id")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
            category = category,
            subtitle = cursor.getString(cursor.getColumnIndexOrThrow("subtitle")),
            samagriList = com.example.shribalajikripadham.data.model.SacredParcha.parseJsonList(samagriJson),
            vidhiSteps = com.example.shribalajikripadham.data.model.SacredParcha.parseJsonList(vidhiJson),
            precautions = com.example.shribalajikripadham.data.model.SacredParcha.parseJsonList(precautionsJson),
            mantraText = cursor.getString(cursor.getColumnIndexOrThrow("mantra_text")),
            imageUri = cursor.getString(cursor.getColumnIndexOrThrow("image_uri")),
            isPublished = cursor.getInt(cursor.getColumnIndexOrThrow("is_published")) == 1,
            isHidden = cursor.getInt(cursor.getColumnIndexOrThrow("is_hidden")) == 1,
            viewCount = cursor.getInt(cursor.getColumnIndexOrThrow("view_count")),
            downloadCount = cursor.getInt(cursor.getColumnIndexOrThrow("download_count")),
            createdBy = cursor.getString(cursor.getColumnIndexOrThrow("created_by")),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
        )
    }

    // --- Admin Single-Device Session Management ---
    suspend fun registerAdminSession(
        adminId: String,
        role: String,
        deviceId: String,
        deviceModel: String
    ): Pair<Boolean, String> {
        return com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.registerAdminSession(
            context = appContext,
            adminId = adminId,
            role = role,
            deviceId = deviceId,
            deviceModel = deviceModel
        )
    }

    suspend fun checkAdminSessionActive(
        adminId: String,
        currentSessionId: String,
        currentDeviceId: String,
        myLoginTimestamp: Long = 0L
    ): Pair<Boolean, String?> {
        val now = System.currentTimeMillis()
        // 30-second initial grace window: never logout right after login
        if (myLoginTimestamp > 0L && (now - myLoginTimestamp) < 30_000L) {
            return Pair(true, null)
        }

        val sessions = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.fetchLiveAdminSessions(appContext)
        val sess = sessions[adminId] ?: return Pair(true, null)

        // Same phone is always safe
        if (sess.deviceId.isNotBlank() && currentDeviceId.isNotBlank() && sess.deviceId == currentDeviceId) {
            return Pair(true, null)
        }

        // Same session ID is valid
        if (sess.sessionId.isNotBlank() && currentSessionId.isNotBlank() && sess.sessionId == currentSessionId) {
            return Pair(true, null)
        }

        // Invalidation rule:
        // ONLY log out if the session in the cloud was created AFTER this device logged in (sess.loggedInAt > myLoginTimestamp)
        // AND was created on a DIFFERENT device (sess.deviceId != currentDeviceId)
        // If sess.loggedInAt <= myLoginTimestamp, this device is the NEWER login and must NEVER be kicked out!
        if (myLoginTimestamp > 0L && sess.loggedInAt > myLoginTimestamp) {
            val deviceModel = if (sess.deviceModel.isNotBlank()) sess.deviceModel else "अन्य फोन"
            return Pair(false, "खाता किसी नए फोन ($deviceModel) पर लॉगिन हो चुका है!")
        }

        return Pair(true, null)
    }

    suspend fun clearAdminSession(
        adminId: String,
        sessionId: String? = null
    ): Pair<Boolean, String> {
        return com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.clearAdminSession(
            context = appContext,
            adminId = adminId,
            sessionId = sessionId
        )
    }

    // ========================================================================
    // SACRED ARZI BOX DISTRIBUTION & UNIFIED MASTER FINANCIAL LEDGER
    // ========================================================================

    suspend fun updateArziSettings(
        isArziLedgerLive: Boolean,
        badiArziRate: Double,
        chhotiArziRate: Double,
        canAdminViewArziLedger: Boolean,
        canDevoteeViewArziLedger: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_arzi_ledger_live", if (isArziLedgerLive) 1 else 0)
            put("badi_arzi_rate", badiArziRate)
            put("chhoti_arzi_rate", chhotiArziRate)
            put("can_admin_view_arzi_ledger", if (canAdminViewArziLedger) 1 else 0)
            put("can_devotee_view_arzi_ledger", if (canDevoteeViewArziLedger) 1 else 0)
        }
        val res = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (res) {
            persistCurrentSettingsToAllLayers()
        }
        res
    }

    suspend fun updateArziSettings(
        isArziLedgerLive: Boolean,
        badiArziRate: Int,
        chhotiArziRate: Int,
        canAdminViewArziLedger: Boolean,
        canDevoteeViewArziLedger: Boolean
    ): Boolean = updateArziSettings(
        isArziLedgerLive = isArziLedgerLive,
        badiArziRate = badiArziRate.toDouble(),
        chhotiArziRate = chhotiArziRate.toDouble(),
        canAdminViewArziLedger = canAdminViewArziLedger,
        canDevoteeViewArziLedger = canDevoteeViewArziLedger
    )

    suspend fun getAllArziRecords(darbarDate: String = ""): List<ArziDistributionRecord> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<ArziDistributionRecord>()
        val query = if (darbarDate.isNotBlank()) {
            "SELECT * FROM arzi_distribution_records WHERE darbar_date = ? ORDER BY id DESC"
        } else {
            "SELECT * FROM arzi_distribution_records ORDER BY id DESC"
        }
        val args = if (darbarDate.isNotBlank()) arrayOf(darbarDate) else null
        val cursor = db.rawQuery(query, args)
        while (cursor.moveToNext()) {
            list.add(
                ArziDistributionRecord(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    devoteeName = cursor.getString(cursor.getColumnIndexOrThrow("devotee_name")),
                    phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone_number")),
                    bigArziQty = cursor.getInt(cursor.getColumnIndexOrThrow("big_arzi_qty")),
                    smallArziQty = cursor.getInt(cursor.getColumnIndexOrThrow("small_arzi_qty")),
                    bigArziRate = cursor.getDouble(cursor.getColumnIndexOrThrow("big_arzi_rate")),
                    smallArziRate = cursor.getDouble(cursor.getColumnIndexOrThrow("small_arzi_rate")),
                    totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount")),
                    isPaid = cursor.getInt(cursor.getColumnIndexOrThrow("is_paid")) == 1,
                    paymentMode = cursor.getString(cursor.getColumnIndexOrThrow("payment_mode")),
                    recordedBy = cursor.getString(cursor.getColumnIndexOrThrow("recorded_by")),
                    darbarDate = cursor.getString(cursor.getColumnIndexOrThrow("darbar_date")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"))
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun upsertArziRecord(record: ArziDistributionRecord): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("devotee_name", record.devoteeName.trim())
            put("phone_number", record.phoneNumber.trim())
            put("big_arzi_qty", record.bigArziQty)
            put("small_arzi_qty", record.smallArziQty)
            put("big_arzi_rate", record.bigArziRate)
            put("small_arzi_rate", record.smallArziRate)
            put("total_amount", record.totalAmount)
            put("is_paid", if (record.isPaid) 1 else 0)
            put("payment_mode", record.paymentMode)
            put("recorded_by", record.recordedBy)
            put("darbar_date", if (record.darbarDate.isNotBlank()) record.darbarDate else DatabaseHelper.getTodayDateString())
            put("timestamp", if (record.timestamp > 0L) record.timestamp else System.currentTimeMillis())
            put("notes", record.notes.trim())
        }
        val id = if (record.id > 0L) {
            db.update("arzi_distribution_records", cv, "id = ?", arrayOf(record.id.toString()))
            record.id
        } else {
            db.insert("arzi_distribution_records", null, cv)
        }

        // Auto-index devotee details into Master Directory
        upsertDevoteeDirectoryInternal(
            name = record.devoteeName,
            phone = record.phoneNumber,
            sourceModule = "ARZI",
            lastVisitDate = record.darbarDate.ifBlank { DatabaseHelper.getTodayDateString() }
        )
        try {
            val all = getAllArziRecords()
            com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishLiveArziRecords(appContext, all, record.recordedBy)
        } catch (e: Exception) {}
        try {
            com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager.postArziToSheet(appContext, record)
        } catch (e: Exception) {}
        id
    }

    suspend fun toggleArziPaymentStatus(recordId: Long, isPaid: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_paid", if (isPaid) 1 else 0)
        }
        val updated = db.update("arzi_distribution_records", cv, "id = ?", arrayOf(recordId.toString())) > 0
        if (updated) {
            try {
                val all = getAllArziRecords()
                com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishLiveArziRecords(appContext, all)
            } catch (e: Exception) {}
        }
        updated
    }

    suspend fun deleteArziRecord(recordId: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val deleted = db.delete("arzi_distribution_records", "id = ?", arrayOf(recordId.toString())) > 0
        if (deleted) {
            try {
                val all = getAllArziRecords()
                com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishLiveArziRecords(appContext, all)
            } catch (e: Exception) {}
        }
        deleted
    }

    suspend fun syncLiveArziFromCloud(): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
        try {
            val remote = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.fetchLiveArziRecords(appContext)
                ?: return@withContext Pair(false, 0)
            val db = dbHelper.writableDatabase
            var count = 0
            db.beginTransaction()
            try {
                remote.forEach { r ->
                    val checkCursor = db.rawQuery(
                        "SELECT id FROM arzi_distribution_records WHERE devotee_name = ? AND darbar_date = ? AND timestamp = ?",
                        arrayOf(r.devoteeName, r.darbarDate, r.timestamp.toString())
                    )
                    val exists = checkCursor.moveToFirst()
                    val existingId = if (exists) checkCursor.getLong(0) else 0L
                    checkCursor.close()

                    val cv = ContentValues().apply {
                        put("devotee_name", r.devoteeName)
                        put("phone_number", r.phoneNumber)
                        put("big_arzi_qty", r.bigArziQty)
                        put("small_arzi_qty", r.smallArziQty)
                        put("big_arzi_rate", r.bigArziRate)
                        put("small_arzi_rate", r.smallArziRate)
                        put("total_amount", r.totalAmount)
                        put("is_paid", if (r.isPaid) 1 else 0)
                        put("payment_mode", r.paymentMode)
                        put("recorded_by", r.recordedBy)
                        put("darbar_date", r.darbarDate)
                        put("timestamp", r.timestamp)
                        put("notes", r.notes)
                    }

                    if (exists) {
                        db.update("arzi_distribution_records", cv, "id = ?", arrayOf(existingId.toString()))
                    } else {
                        db.insert("arzi_distribution_records", null, cv)
                        count++
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            Pair(true, count)
        } catch (e: Exception) {
            Pair(false, 0)
        }
    }

    suspend fun publishLiveArziToCloud(recordedBy: String = "Admin"): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val records = getAllArziRecords()
        com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishLiveArziRecords(appContext, records, recordedBy)
    }

    suspend fun getUnifiedMasterFinancialSummary(darbarDate: String = ""): UnifiedMasterFinancialSummary = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val entries = mutableListOf<UnifiedLedgerEntry>()

        var busTotal = 0.0
        var busPaid = 0.0
        var busPending = 0.0
        var busSeatsCount = 0

        var arziTotal = 0.0
        var arziPaid = 0.0
        var arziPending = 0.0
        var arziBadiCount = 0
        var arziChhotiCount = 0

        var expenseTotal = 0.0
        var expenseCount = 0

        // 1. Fetch Bus Bookings
        try {
            val busCursor = db.rawQuery("SELECT seat_number, seat_label, passenger_name, phone_number, fare_amount, payment_status, payment_mode, yatra_date, booked_at FROM bus_seats WHERE is_booked = 1", null)
            while (busCursor.moveToNext()) {
                val seatNum = busCursor.getInt(0)
                val seatLabel = busCursor.getString(1)
                val passenger = busCursor.getString(2) ?: ""
                val phone = busCursor.getString(3) ?: ""
                val fare = busCursor.getInt(4).toDouble()
                val status = busCursor.getString(5) ?: "UNPAID"
                val mode = busCursor.getString(6) ?: "UPI_QR"
                val yatraDate = busCursor.getString(7) ?: ""
                val bookedAt = busCursor.getLong(8)

                val isPaid = (status == "PAID" || status == "SUCCESS")
                busSeatsCount++
                busTotal += fare
                if (isPaid) busPaid += fare else busPending += fare

                entries.add(
                    UnifiedLedgerEntry(
                        id = "BUS_$seatNum",
                        date = yatraDate,
                        category = "BUS_BOOKING",
                        categoryTitleHindi = "बालाजी बस सेवा (सीट $seatLabel)",
                        devoteeOrPerson = passenger.ifEmpty { "यात्री #$seatNum" },
                        phone = phone,
                        details = "सीट संख्या: $seatLabel | किराया: ₹$fare",
                        amount = fare,
                        isInflow = true,
                        isPaid = isPaid,
                        paymentMode = mode,
                        timestamp = if (bookedAt > 0L) bookedAt else System.currentTimeMillis()
                    )
                )
            }
            busCursor.close()
        } catch (e: Exception) {}

        // 2. Fetch Arzi Distributions
        try {
            val arziList = getAllArziRecords(darbarDate)
            for (a in arziList) {
                arziTotal += a.totalAmount
                arziBadiCount += a.bigArziQty
                arziChhotiCount += a.smallArziQty
                if (a.isPaid) arziPaid += a.totalAmount else arziPending += a.totalAmount

                entries.add(
                    UnifiedLedgerEntry(
                        id = "ARZI_${a.id}",
                        date = a.darbarDate,
                        category = "ARZI_BOX",
                        categoryTitleHindi = "पवित्र अर्जी डिब्बा वितरण",
                        devoteeOrPerson = a.devoteeName,
                        phone = a.phoneNumber,
                        details = "बड़ी अर्जी: ${a.bigArziQty}, छोटी अर्जी: ${a.smallArziQty} | दर: ₹${a.bigArziRate.toInt()}/₹${a.smallArziRate.toInt()}",
                        amount = a.totalAmount,
                        isInflow = true,
                        isPaid = a.isPaid,
                        paymentMode = a.paymentMode,
                        timestamp = a.timestamp,
                        recordedBy = a.recordedBy,
                        notes = a.notes
                    )
                )
            }
        } catch (e: Exception) {}

        // 3. Fetch Ashram Expenses
        try {
            val expCursor = db.rawQuery("SELECT id, title, category, amount, expense_date, added_by, created_at FROM yatra_expenses ORDER BY id DESC", null)
            while (expCursor.moveToNext()) {
                val expId = expCursor.getLong(0)
                val title = expCursor.getString(1) ?: ""
                val cat = expCursor.getString(2) ?: ""
                val amt = expCursor.getDouble(3)
                val expDate = expCursor.getString(4) ?: ""
                val addedBy = expCursor.getString(5) ?: ""
                val createdAt = expCursor.getLong(6)

                expenseCount++
                expenseTotal += amt

                entries.add(
                    UnifiedLedgerEntry(
                        id = "EXP_$expId",
                        date = expDate,
                        category = "ASHRAM_EXPENSE",
                        categoryTitleHindi = "आश्रम/यात्रा व्यय ($cat)",
                        devoteeOrPerson = addedBy.ifEmpty { "व्यवस्थापक" },
                        phone = "",
                        details = title,
                        amount = amt,
                        isInflow = false,
                        isPaid = true,
                        paymentMode = "CASH",
                        timestamp = createdAt,
                        recordedBy = addedBy
                    )
                )
            }
            expCursor.close()
        } catch (e: Exception) {}

        // 4. Fetch Direct QR Payments, Donations, Sewa & Dakshina
        var donationTotal = 0.0
        var donationPaid = 0.0
        var donationCount = 0

        try {
            val payments = getAllPayments()
            val dateFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            for (p in payments) {
                // If payment has seatNumbers or purpose is BUS_BOOKING, bus seats table already counts it
                val isBusPayment = p.seatNumbers.isNotBlank() || p.purpose.contains("BUS", ignoreCase = true)
                if (!isBusPayment) {
                    val isPaid = (p.paymentStatus.equals("PAID", ignoreCase = true) || p.paymentStatus.equals("SUCCESS", ignoreCase = true))
                    donationCount++
                    donationTotal += p.amount
                    if (isPaid) donationPaid += p.amount

                    val pDate = if (p.timestamp > 0L) dateFmt.format(java.util.Date(p.timestamp)) else ""

                    entries.add(
                        UnifiedLedgerEntry(
                            id = "PAY_${p.paymentId.ifEmpty { p.id.toString() }}",
                            date = pDate,
                            category = "UPI_QR_DONATION",
                            categoryTitleHindi = "दान / दक्षिणा / क्यूआर (${p.purpose.ifEmpty { "सहयोग राशि" }})",
                            devoteeOrPerson = p.devoteeName.ifEmpty { "अनाम भक्त" },
                            phone = p.devoteePhone,
                            details = "माध्यम: ${p.paymentApp.ifEmpty { p.paymentMode }} | ट्रांजैक्शन: ${p.transactionId.ifEmpty { "N/A" }}",
                            amount = p.amount,
                            isInflow = true,
                            isPaid = isPaid,
                            paymentMode = p.paymentMode.ifEmpty { "UPI_QR" },
                            timestamp = if (p.timestamp > 0L) p.timestamp else System.currentTimeMillis(),
                            recordedBy = p.verifiedBy.ifEmpty { "ADMIN" },
                            notes = p.notes
                        )
                    )
                }
            }
        } catch (e: Exception) {}

        // Sort all entries descending by timestamp
        entries.sortByDescending { it.timestamp }

        val totalInflow = busTotal + arziTotal + donationTotal
        val totalPaidInflow = busPaid + arziPaid + donationPaid
        val totalPendingInflow = busPending + arziPending + (donationTotal - donationPaid)
        val netBalance = totalPaidInflow - expenseTotal

        UnifiedMasterFinancialSummary(
            totalInflow = totalInflow,
            totalPaidInflow = totalPaidInflow,
            totalPendingInflow = totalPendingInflow,
            totalOutflow = expenseTotal,
            netBalance = netBalance,
            busTotalAmount = busTotal,
            busPaidAmount = busPaid,
            busPendingAmount = busPending,
            busBookedSeatsCount = busSeatsCount,
            arziTotalAmount = arziTotal,
            arziPaidAmount = arziPaid,
            arziPendingAmount = arziPending,
            arziBadiCount = arziBadiCount,
            arziChhotiCount = arziChhotiCount,
            donationTotalAmount = donationTotal,
            donationPaidAmount = donationPaid,
            donationCount = donationCount,
            expenseTotalAmount = expenseTotal,
            expenseCount = expenseCount,
            entries = entries
        )
    }

    // ========================================================================
    // HOSTINGER LIVE ADMIN DATA SYNC
    // ========================================================================

    suspend fun syncHostingerExpenses(): Int = withContext(Dispatchers.IO) {
        val (ok, list) = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchLiveExpenses()
        if (!ok || list.isEmpty()) return@withContext 0
        val db = dbHelper.writableDatabase
        var count = 0
        for (item in list) {
            val title = item.optString("title", "")
            val amount = item.optDouble("amount", 0.0)
            val cat = item.optString("category", "GENERAL")
            val date = item.optString("expense_date", DatabaseHelper.getTodayDateString())
            val spentBy = item.optString("spent_by", "आश्रम व्यवस्थापक")
            val receipt = item.optString("receipt_photo_url", "")
            val createdAt = item.optLong("created_at", System.currentTimeMillis())

            val checkCursor = db.rawQuery("SELECT id FROM yatra_expenses WHERE title = ? AND amount = ? AND expense_date = ?", arrayOf(title, amount.toString(), date))
            val exists = checkCursor.moveToFirst()
            checkCursor.close()

            if (!exists) {
                val cv = ContentValues().apply {
                    put("title", title)
                    put("category", cat)
                    put("amount", amount)
                    put("receipt_uri", receipt)
                    put("added_by", spentBy)
                    put("expense_date", date)
                    put("created_at", createdAt)
                }
                if (db.insert("yatra_expenses", null, cv) > 0) count++
            }
        }
        count
    }

    suspend fun syncHostingerPayments(): Int = withContext(Dispatchers.IO) {
        val (ok, list) = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchLivePayments()
        if (!ok || list.isEmpty()) return@withContext 0
        val db = dbHelper.writableDatabase
        var count = 0
        for (item in list) {
            val receiptNo = item.optString("receipt_number", "")
            if (receiptNo.isBlank()) continue

            val cv = ContentValues().apply {
                put("payment_id", receiptNo)
                put("devotee_name", item.optString("devotee_name", ""))
                put("devotee_phone", item.optString("phone_number", ""))
                put("payment_app", item.optString("payment_mode", "UPI"))
                put("transaction_id", item.optString("transaction_id", ""))
                put("amount", item.optDouble("amount", 0.0))
                put("purpose", item.optString("purpose", "दान / सहयोग राशि"))
                put("timestamp", item.optLong("created_at", System.currentTimeMillis()))
                put("payment_status", item.optString("status", "SUCCESS"))
                put("payment_mode", item.optString("payment_mode", "UPI"))
                put("verified_by", item.optString("collected_by", "ADMIN"))
                put("notes", item.optString("notes", ""))
            }
            if (db.insertWithOnConflict("payment_records", null, cv, SQLiteDatabase.CONFLICT_REPLACE) > 0) {
                count++
            }
        }
        count
    }

    /**
     * Master Live Sync for Admin Dashboard:
     * Pulls latest tokens, bills/expenses, devotee payments, and live settings from Hostinger MySQL.
     * All Admin phones see changes immediately!
     */
    suspend fun syncFullHostingerToLocal(targetDate: String = ""): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        var tokenCount = 0
        try {
            val queueJson = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchLiveQueue(targetDate)
            if (queueJson != null && queueJson.optBoolean("success", false)) {
                val tokensArray = queueJson.optJSONArray("tokens")
                if (tokensArray != null && tokensArray.length() > 0) {
                    for (i in 0 until tokensArray.length()) {
                        val t = tokensArray.getJSONObject(i)
                        val ok = insertOrUpdateCentralToken(
                            tokenNumber = t.optInt("token_number"),
                            darbarDate = t.optString("darbar_date", targetDate).ifBlank { targetDate.ifBlank { DatabaseHelper.getTodayDateString() } },
                            patientName = t.optString("patient_name"),
                            phoneNumber = t.optString("phone_number"),
                            city = t.optString("city", "डूँगरा जाट (स्थानीय)"),
                            deviceId = t.optString("device_id", "HOSTINGER"),
                            latitude = t.optDouble("latitude", 28.3972915),
                            longitude = t.optDouble("longitude", 78.1460410),
                            distanceKm = t.optDouble("distance_km", 0.0).toFloat(),
                            photoUri = t.optString("photo_url", ""),
                            registeredBy = t.optString("registered_by", "HOSTINGER"),
                            status = t.optString("status", "WAITING"),
                            isDarshanCompleted = t.optInt("is_darshan_completed", 0) == 1,
                            createdAt = t.optLong("created_at", System.currentTimeMillis())
                        )
                        if (ok) tokenCount++
                    }
                }
            }
        } catch (e: Exception) {}

        val expCount = try { syncHostingerExpenses() } catch (e: Exception) { 0 }
        val payCount = try { syncHostingerPayments() } catch (e: Exception) { 0 }

        // Also refresh live settings
        try {
            val cfg = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchLiveConfig()
            if (cfg != null && cfg.optBoolean("success", false)) {
                val db = dbHelper.writableDatabase
                val cv = ContentValues().apply {
                    if (cfg.has("current_serving_token")) put("running_token_number", cfg.optInt("current_serving_token", 1).coerceAtLeast(1))
                    if (cfg.has("is_token_service_enabled")) put("is_token_service_enabled", if (cfg.optBoolean("is_token_service_enabled", true)) 1 else 0)
                    if (cfg.has("is_bus_booking_live")) put("is_bus_booking_live", if (cfg.optBoolean("is_bus_booking_live", false)) 1 else 0)
                    if (cfg.has("is_live_counter_visible")) put("is_live_counter_visible", if (cfg.optBoolean("is_live_counter_visible", true)) 1 else 0)
                    if (cfg.has("is_darbar_active")) put("is_darbar_active", if (cfg.optBoolean("is_darbar_active", true)) 1 else 0)
                    if (cfg.has("emergency_notice") && cfg.optString("emergency_notice").isNotBlank()) put("emergency_notice", cfg.optString("emergency_notice", ""))
                    if (cfg.has("is_emergency_notice_visible")) put("is_emergency_notice_visible", if (cfg.optBoolean("is_emergency_notice_visible", false)) 1 else 0)
                    if (cfg.has("banner_title") && cfg.optString("banner_title").isNotBlank()) put("banner_title", cfg.optString("banner_title"))
                    if (cfg.has("banner_subtitle") && cfg.optString("banner_subtitle").isNotBlank()) put("banner_subtitle", cfg.optString("banner_subtitle"))
                    if (cfg.has("is_banner_visible")) put("is_banner_visible", if (cfg.optBoolean("is_banner_visible", true)) 1 else 0)
                    // New website CMS fields
                    if (cfg.has("top_bar_text") && cfg.optString("top_bar_text").isNotBlank()) put("top_bar_text", cfg.optString("top_bar_text"))
                    if (cfg.has("guruji_title") && cfg.optString("guruji_title").isNotBlank()) put("guruji_title", cfg.optString("guruji_title"))
                    if (cfg.has("guruji_bio") && cfg.optString("guruji_bio").isNotBlank()) put("guruji_bio", cfg.optString("guruji_bio"))
                    if (cfg.has("ashram_history") && cfg.optString("ashram_history").isNotBlank()) put("ashram_history", cfg.optString("ashram_history"))
                    if (cfg.has("token_rules_summary") && cfg.optString("token_rules_summary").isNotBlank()) put("token_rules_summary", cfg.optString("token_rules_summary"))
                    if (cfg.has("youtube_live_video_id") && cfg.optString("youtube_live_video_id").isNotBlank()) put("youtube_live_video_id", cfg.optString("youtube_live_video_id"))
                    if (cfg.has("aarti_mangala_time") && cfg.optString("aarti_mangala_time").isNotBlank()) put("aarti_mangala_time", cfg.optString("aarti_mangala_time"))
                    if (cfg.has("aarti_sandhya_time") && cfg.optString("aarti_sandhya_time").isNotBlank()) put("aarti_sandhya_time", cfg.optString("aarti_sandhya_time"))
                    if (cfg.has("aarti_maha_time") && cfg.optString("aarti_maha_time").isNotBlank()) put("aarti_maha_time", cfg.optString("aarti_maha_time"))
                    if (cfg.has("bank_name") && cfg.optString("bank_name").isNotBlank()) put("bank_name", cfg.optString("bank_name"))
                    if (cfg.has("bank_account_holder") && cfg.optString("bank_account_holder").isNotBlank()) put("bank_account_holder", cfg.optString("bank_account_holder"))
                    if (cfg.has("bank_account_number") && cfg.optString("bank_account_number").isNotBlank()) put("bank_account_number", cfg.optString("bank_account_number"))
                    if (cfg.has("bank_ifsc") && cfg.optString("bank_ifsc").isNotBlank()) put("bank_ifsc", cfg.optString("bank_ifsc"))
                    if (cfg.has("bank_branch") && cfg.optString("bank_branch").isNotBlank()) put("bank_branch", cfg.optString("bank_branch"))
                    if (cfg.has("bank_upi_id") && cfg.optString("bank_upi_id").isNotBlank()) put("bank_upi_id", cfg.optString("bank_upi_id"))
                    if (cfg.has("ashram_address") && cfg.optString("ashram_address").isNotBlank()) put("ashram_address", cfg.optString("ashram_address"))
                    if (cfg.has("ashram_directions") && cfg.optString("ashram_directions").isNotBlank()) put("ashram_directions", cfg.optString("ashram_directions"))
                    if (cfg.has("footer_copyright") && cfg.optString("footer_copyright").isNotBlank()) put("footer_copyright", cfg.optString("footer_copyright"))
                    // Tuesday Bulandshahr Darbar CMS fields
                    if (cfg.has("is_tuesday_darbar_enabled")) put("is_tuesday_darbar_enabled", if (cfg.optBoolean("is_tuesday_darbar_enabled", false)) 1 else 0)
                    if (cfg.has("tuesday_darbar_name") && cfg.optString("tuesday_darbar_name").isNotBlank()) put("tuesday_darbar_name", cfg.optString("tuesday_darbar_name"))
                    if (cfg.has("tuesday_darbar_address") && cfg.optString("tuesday_darbar_address").isNotBlank()) put("tuesday_darbar_address", cfg.optString("tuesday_darbar_address"))
                    if (cfg.has("tuesday_latitude")) put("tuesday_latitude", cfg.optDouble("tuesday_latitude", 0.0))
                    if (cfg.has("tuesday_longitude")) put("tuesday_longitude", cfg.optDouble("tuesday_longitude", 0.0))
                    if (cfg.has("tuesday_allowed_radius_meters")) put("tuesday_allowed_radius_meters", cfg.optDouble("tuesday_allowed_radius_meters", 200.0))
                    if (cfg.has("tuesday_outstation_min_distance_km")) put("tuesday_outstation_min_distance_km", cfg.optDouble("tuesday_outstation_min_distance_km", 30.0))
                    if (cfg.has("tuesday_darbar_timings") && cfg.optString("tuesday_darbar_timings").isNotBlank()) put("tuesday_darbar_timings", cfg.optString("tuesday_darbar_timings"))
                    if (cfg.has("tuesday_token_service_mode") && cfg.optString("tuesday_token_service_mode").isNotBlank()) put("tuesday_token_service_mode", cfg.optString("tuesday_token_service_mode"))
                    if (cfg.has("tuesday_scheduled_open_timestamp")) put("tuesday_scheduled_open_timestamp", cfg.optLong("tuesday_scheduled_open_timestamp", 0L))
                    if (cfg.has("tuesday_darbar_date") && cfg.optString("tuesday_darbar_date").isNotBlank()) put("tuesday_darbar_date", cfg.optString("tuesday_darbar_date"))
                    if (cfg.has("tuesday_current_serving_token")) put("tuesday_current_serving_token", cfg.optInt("tuesday_current_serving_token", 0))
                    if (cfg.has("tuesday_running_token_number")) put("tuesday_running_token_number", cfg.optInt("tuesday_running_token_number", 0))
                    if (cfg.has("tuesday_token_notice") && cfg.optString("tuesday_token_notice").isNotBlank()) put("tuesday_token_notice", cfg.optString("tuesday_token_notice"))
                }
                db.update("ashram_settings", cv, "id = 1", null)
            }
        } catch (e: Exception) {}

        Triple(tokenCount, expCount, payCount)
    }


    // --- VIP Reserved Tokens & Permissions ---
    suspend fun updateAllowAdminReservedTokens(allow: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("allow_admin_reserved_tokens", if (allow) 1 else 0)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) {
            persistCurrentSettingsToAllLayers()
        }
        ok
    }

    // --- Sevadars Management (App & Website Synchronized) ---
    suspend fun getAllSevadars(): List<SevadarProfile> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM sevadars WHERE is_active = 1 ORDER BY display_order ASC, id ASC", null)
        val list = mutableListOf<SevadarProfile>()
        while (cursor.moveToNext()) {
            list.add(
                SevadarProfile(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    roleTitleHindi = cursor.getString(cursor.getColumnIndexOrThrow("role")),
                    roleTitleEnglish = cursor.getString(cursor.getColumnIndexOrThrow("role")),
                    phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                    photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri")),
                    displayOrder = cursor.getInt(cursor.getColumnIndexOrThrow("display_order")),
                    isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1
                )
            )
        }
        cursor.close()
        list
    }

    /**
     * Synchronize Sevadars from Hostinger Central MySQL to Local SQLite
     * Ensures devotee phones immediately reflect removals or additions!
     */
    suspend fun syncSevadarsFromCloud(): List<SevadarProfile> = withContext(Dispatchers.IO) {
        val remoteSevadars = try {
            com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchCentralSevadars()
        } catch (e: Exception) {
            null
        }

        if (remoteSevadars != null) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                // Clear local and replace with authoritative server sevadars
                db.delete("sevadars", null, null)
                remoteSevadars.forEach { sev ->
                    val cv = ContentValues().apply {
                        put("id", sev.id)
                        put("name", sev.name)
                        put("role", sev.roleTitleHindi)
                        put("phone", sev.phoneNumber)
                        put("photo_uri", sev.photoUri)
                        put("display_order", sev.displayOrder)
                        put("is_active", if (sev.isActive) 1 else 0)
                    }
                    db.insertWithOnConflict("sevadars", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
                val prefs = appContext.getSharedPreferences("ashram_vault_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("has_seeded_initial_sevadars", true).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                db.endTransaction()
            }
        }
        getAllSevadars()
    }

    /**
     * Synchronize Donors from Hostinger Central MySQL to Local SQLite
     */
    suspend fun syncDonorsFromCloud(): List<DonorProfile> = withContext(Dispatchers.IO) {
        val remoteDonors = try {
            com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchCentralDonors()
        } catch (e: Exception) {
            null
        }

        if (remoteDonors != null) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                // Clear local and replace with authoritative server donors
                db.delete("donors", null, null)
                remoteDonors.forEach { d ->
                    val cv = ContentValues().apply {
                        put("id", d.id)
                        put("name", d.name)
                        put("city_address", d.cityAddress)
                        put("title", d.title)
                        put("photo_uri", d.photoUri)
                        put("phone", d.phone)
                        put("notes", d.notes)
                        put("display_order", d.displayOrder)
                        put("is_active", if (d.isActive) 1 else 0)
                    }
                    db.insertWithOnConflict("donors", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                db.endTransaction()
            }
        }
        getAllDonors()
    }

    suspend fun saveSevadar(sevadar: SevadarProfile): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase

        val cloudPhotoUrl = if (sevadar.photoUri.isNotBlank() && !sevadar.photoUri.startsWith("http://") && !sevadar.photoUri.startsWith("https://")) {
            try {
                val rawPath = sevadar.photoUri.removePrefix("file://")
                val f = java.io.File(rawPath)
                if (f.exists() && f.length() > 0) {
                    com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.uploadPhoto(f) ?: sevadar.photoUri
                } else sevadar.photoUri
            } catch (e: Exception) { sevadar.photoUri }
        } else sevadar.photoUri

        val cv = ContentValues().apply {
            put("name", sevadar.name)
            put("role", sevadar.roleTitleHindi)
            put("phone", sevadar.phoneNumber)
            put("photo_uri", cloudPhotoUrl)
            put("display_order", sevadar.displayOrder)
            put("is_active", if (sevadar.isActive) 1 else 0)
        }
        val rowId = if (sevadar.id > 0) {
            val updated = db.update("sevadars", cv, "id = ?", arrayOf(sevadar.id.toString()))
            if (updated > 0) {
                sevadar.id
            } else {
                cv.put("id", sevadar.id)
                db.insertWithOnConflict("sevadars", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
            }
        } else {
            db.insert("sevadars", null, cv)
        }

        // Sync to Central Hostinger MySQL
        try {
            com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.saveCentralSevadar(
                name = sevadar.name,
                role = sevadar.roleTitleHindi,
                phone = sevadar.phoneNumber,
                photoUrl = cloudPhotoUrl,
                displayOrder = sevadar.displayOrder,
                id = rowId
            )
        } catch (ignored: Exception) {}

        rowId > 0
    }

    suspend fun deleteSevadar(id: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val deleted = db.delete("sevadars", "id = ?", arrayOf(id.toString())) > 0
        try {
            com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.deleteCentralSevadar(id)
        } catch (ignored: Exception) {}
        deleted
    }

    // --- Prominent Donors Management (STRICT PRIVACY: NO PHONE NUMBERS ON WEBSITE) ---
    suspend fun getAllDonors(): List<DonorProfile> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM donors WHERE is_active = 1 ORDER BY display_order ASC, id ASC", null)
        val list = mutableListOf<DonorProfile>()
        while (cursor.moveToNext()) {
            list.add(
                DonorProfile(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cityAddress = cursor.getString(cursor.getColumnIndexOrThrow("city_address")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri")),
                    phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                    notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")),
                    displayOrder = cursor.getInt(cursor.getColumnIndexOrThrow("display_order")),
                    isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun saveDonor(donor: DonorProfile): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase

        val cloudPhotoUrl = if (donor.photoUri.isNotBlank() && !donor.photoUri.startsWith("http://") && !donor.photoUri.startsWith("https://")) {
            try {
                val rawPath = donor.photoUri.removePrefix("file://")
                val f = java.io.File(rawPath)
                if (f.exists() && f.length() > 0) {
                    com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.uploadPhoto(f) ?: donor.photoUri
                } else donor.photoUri
            } catch (e: Exception) { donor.photoUri }
        } else donor.photoUri

        val cv = ContentValues().apply {
            put("name", donor.name)
            put("city_address", donor.cityAddress)
            put("title", donor.title)
            put("photo_uri", cloudPhotoUrl)
            put("phone", donor.phone)
            put("notes", donor.notes)
            put("display_order", donor.displayOrder)
            put("is_active", if (donor.isActive) 1 else 0)
        }
        val rowId = if (donor.id > 0) {
            val updated = db.update("donors", cv, "id = ?", arrayOf(donor.id.toString()))
            if (updated > 0) {
                donor.id
            } else {
                cv.put("id", donor.id)
                db.insertWithOnConflict("donors", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
            }
        } else {
            db.insert("donors", null, cv)
        }

        // Sync to Central Hostinger MySQL (Excludes phone on website queries!)
        try {
            com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.saveCentralDonor(
                name = donor.name,
                cityAddress = donor.cityAddress,
                title = donor.title,
                photoUrl = cloudPhotoUrl,
                phone = donor.phone,
                notes = donor.notes,
                displayOrder = donor.displayOrder,
                id = rowId
            )
        } catch (ignored: Exception) {}

        rowId > 0
    }

    suspend fun deleteDonor(id: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val deleted = db.delete("donors", "id = ?", arrayOf(id.toString())) > 0
        try {
            com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.deleteCentralDonor(id)
        } catch (ignored: Exception) {}
        deleted
    }

    // --- Website CMS Dynamic Editor Operations ---
    suspend fun updateWebsiteHeroBanner(title: String, subtitle: String, isVisible: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("banner_title", title.trim())
            put("banner_subtitle", subtitle.trim())
            put("is_banner_visible", if (isVisible) 1 else 0)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) {
            persistCurrentSettingsToAllLayers()
        }
        ok
    }

    suspend fun updateEmergencyNoticeBanner(notice: String, isVisible: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("emergency_notice", notice.trim())
            put("is_emergency_notice_visible", if (isVisible) 1 else 0)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) {
            persistCurrentSettingsToAllLayers()
        }
        ok
    }

    suspend fun updateDarbarScheduleTimings(darbarTimings: String, aartiTimings: String = ""): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("darbar_timings", darbarTimings.trim())
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) {
            persistCurrentSettingsToAllLayers()
        }
        ok
    }

    suspend fun updateDarbarActiveStatus(isActive: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_darbar_active", if (isActive) 1 else 0)
        }
        val ok = db.update("ashram_settings", cv, "id = 1", null) > 0
        if (ok) {
            persistCurrentSettingsToAllLayers()
        }
        ok
    }

    // --- Master 1-Click Publish to Website & Cloud ---
    suspend fun publishEverythingToWebsiteAndCloud(adminName: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val s = getSettings()
            val (hOk, hMsg) = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.updateFullLiveConfig(s, getAllSevadars(), getUiSectionConfigs())

            // Background GitHub commit so if GitHub throws 409 Conflict or rate limit, it NEVER fails the Super Admin action!
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    publishCurrentSettingsToGitHub(adminName)
                } catch (e: Exception) {}
            }

            if (hOk) {
                Pair(true, "✅ वेबसाइट (shribalajikripadham.online) और सभी भक्तों के ऐप पर सारा डेटा 100% लाइव पब्लिश हो गया!")
            } else {
                Pair(false, "पब्लिश संदेश: $hMsg")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "पब्लिश त्रुटि")
        }
    }

    // ==========================================
    // 🎵 SACRED AUDIO TRACKS & MP3 REPOSITORY
    // ==========================================
    suspend fun getSacredTracks(publishedOnly: Boolean = true): List<com.example.shribalajikripadham.data.sacred.SacredTrack> = withContext(Dispatchers.IO) {
        if (publishedOnly) dbHelper.getPublishedTracks() else dbHelper.getAllTracks()
    }

    suspend fun syncSacredTracksFromHostinger(admin: Boolean = false): Pair<Boolean, List<com.example.shribalajikripadham.data.sacred.SacredTrack>> = withContext(Dispatchers.IO) {
        val (ok, remoteList) = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchSacredTracks(admin)
        if (ok && remoteList.isNotEmpty()) {
            dbHelper.saveAllTracks(remoteList)
        }
        val localList = if (admin) dbHelper.getAllTracks() else dbHelper.getPublishedTracks()
        Pair(ok, localList)
    }

    suspend fun saveSacredTrack(track: com.example.shribalajikripadham.data.sacred.SacredTrack): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val localId = dbHelper.insertOrUpdateTrack(track)
        val trackToSync = if (track.id <= 0) track.copy(id = localId) else track
        val (ok, msg) = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.saveSacredTrack(trackToSync)
        Pair(ok, msg)
    }

    suspend fun deleteSacredTrack(id: Long): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        dbHelper.deleteTrack(id)
        val (ok, msg) = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.deleteSacredTrack(id)
        Pair(ok, msg)
    }

    suspend fun uploadSacredAudio(file: java.io.File): String? = withContext(Dispatchers.IO) {
        com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.uploadAudio(appContext, file)
    }

    // ==========================================
    // 🔴 LIVE DARBAR STREAMING BROADCAST CONTROLS
    // ==========================================
    suspend fun updateLiveStreamingStatus(
        isLive: Boolean,
        title: String = "श्री बालाजी कृपा धाम दिव्य दरबार लाइव",
        liveUrl: String = "",
        ytUrl: String = "",
        fbUrl: String = ""
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val (ok, msg) = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.updateLiveStatus(
            isLive = isLive,
            title = title,
            liveUrl = liveUrl,
            ytUrl = ytUrl,
            fbUrl = fbUrl
        )
        try {
            val db = dbHelper.writableDatabase
            val cv = ContentValues().apply {
                put("is_darbar_live_now", if (isLive) 1 else 0)
            }
            db.update("ashram_settings", cv, "id = 1", null)
        } catch (ignored: Exception) {}
        Pair(ok, msg)
    }

    // ==========================================
    // 🔥 SACRED HAVAN APPLICATION SUBMISSION
    // ==========================================
    suspend fun submitDevoteeHavanApplication(
        devoteeName: String,
        phoneNumber: String,
        whatsappNumber: String = "",
        preferredDate: String = "आश्रम द्वारा तय होगी",
        address: String,
        villageCity: String = "",
        district: String = "",
        state: String = "उत्तर प्रदेश",
        pincode: String = "",
        gotra: String = "",
        familyMembersCount: Int = 4,
        havanPurpose: String = "पावन हवन अनुष्ठान",
        problemDetails: String = "",
        costAcknowledged: Boolean = true,
        travelFareAcknowledged: Boolean = true
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val datePart = java.text.SimpleDateFormat("yyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val randNum = (1000..9999).random()
        val appNo = "HVN-$datePart-$randNum"
        val now = System.currentTimeMillis()

        // 1. Save in local database
        try {
            val db = dbHelper.writableDatabase
            val cv = ContentValues().apply {
                put("application_no", appNo)
                put("devotee_name", devoteeName)
                put("phone_number", phoneNumber)
                put("whatsapp_number", if (whatsappNumber.isNotBlank()) whatsappNumber else phoneNumber)
                put("preferred_date", preferredDate)
                put("address", address)
                put("village_city", villageCity)
                put("district", district)
                put("state", state)
                put("pincode", pincode)
                put("gotra", gotra)
                put("family_members_count", familyMembersCount)
                put("havan_purpose", havanPurpose)
                put("problem_details", problemDetails)
                put("estimated_cost", 14000.0)
                put("cost_acknowledged", if (costAcknowledged) 1 else 0)
                put("travel_fare_acknowledged", if (travelFareAcknowledged) 1 else 0)
                put("status", "PENDING")
                put("created_at", now)
                put("synced_to_cloud", 0)
            }
            db.insertWithOnConflict("havan_applications", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Transmit to server API
        try {
            val jsonPayload = org.json.JSONObject().apply {
                put("action", "SUBMIT_APPLICATION")
                put("devotee_name", devoteeName)
                put("phone_number", phoneNumber)
                put("whatsapp_number", if (whatsappNumber.isNotBlank()) whatsappNumber else phoneNumber)
                put("preferred_date", preferredDate)
                put("address", address)
                put("village_city", villageCity)
                put("district", district)
                put("state", state)
                put("pincode", pincode)
                put("gotra", gotra)
                put("family_members_count", familyMembersCount)
                put("havan_purpose", havanPurpose)
                put("problem_details", problemDetails)
                put("cost_acknowledged", costAcknowledged)
                put("travel_fare_acknowledged", travelFareAcknowledged)
                put("application_no", appNo)
                put("source", "DEVOTEE_APP")
            }

            val url = java.net.URL("https://shribalajikripadham.online/api/havan_service.php")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(jsonPayload.toString().toByteArray(Charsets.UTF_8))
            }

            val respCode = conn.responseCode
            val respText = if (respCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }

            val respJson = try { org.json.JSONObject(respText) } catch (e: Exception) { null }
            if (respJson != null && respJson.optBoolean("success", false)) {
                val serverAppNo = respJson.optString("application_no", appNo)
                try {
                    val db = dbHelper.writableDatabase
                    val cv = ContentValues().apply {
                        put("application_no", serverAppNo)
                        put("synced_to_cloud", 1)
                    }
                    db.update("havan_applications", cv, "application_no = ?", arrayOf(appNo))
                } catch (e: Exception) {}
                Pair(true, serverAppNo)
            } else {
                Pair(true, appNo) // Still success locally!
            }
        } catch (e: Exception) {
            // Local success
            Pair(true, appNo)
        }
    }

    suspend fun getAdminById(adminId: Long): Admin? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM admins WHERE id = ? LIMIT 1", arrayOf(adminId.toString()))
        var admin: Admin? = null
        if (cursor.moveToFirst()) {
            admin = parseAdminCursor(cursor)
        }
        cursor.close()
        admin
    }

    suspend fun updateAdminCredentials(
        adminId: Long,
        newPassword: String?,
        newPin: String?
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (newPassword.isNullOrBlank() && newPin.isNullOrBlank()) {
            return@withContext Pair(false, "नया पासवर्ड अथवा 4-अंकीय पिन दर्ज करें")
        }
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            if (!newPassword.isNullOrBlank()) {
                put("password_hash", DatabaseHelper.hashPassword(newPassword.trim()))
                put("raw_password", newPassword.trim())
            }
            if (!newPin.isNullOrBlank()) {
                put("pin_hash", DatabaseHelper.hashPin(newPin.trim()))
                put("raw_pin", newPin.trim())
            }
        }
        val count = db.update("admins", cv, "id = ?", arrayOf(adminId.toString()))
        val ok = count > 0
        if (ok) {
            try { com.example.shribalajikripadham.data.local.AppPermanentVault.saveVault(appContext, getAllAdmins(), getSettings()) } catch (e: Exception) {}
            try { publishAdminsToGitHub() } catch (e: Exception) {}
            // Sync with Central Server admin_auth.php
            try {
                val adminObj = getAdminById(adminId)
                if (adminObj != null) {
                    val url = java.net.URL("https://shribalajikripadham.online/api/admin_auth.php")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000
                    conn.doOutput = true

                    val payload = JSONObject().apply {
                        put("action", "CHANGE_PASSWORD")
                        put("admin_id", adminId.toString())
                        put("username", adminObj.username)
                        put("phone", adminObj.phoneNumber)
                        if (!newPassword.isNullOrBlank()) put("new_password", newPassword.trim())
                        if (!newPin.isNullOrBlank()) put("new_pin", newPin.trim())
                    }

                    conn.outputStream.use { os ->
                        os.write(payload.toString().toByteArray(Charsets.UTF_8))
                    }
                    val code = conn.responseCode
                    conn.disconnect()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Pair(ok, if (ok) "पासवर्ड/पिन सफलतापूर्वक बदल दिया गया!" else "डेटाबेस में सुरक्षित नहीं हो सका")
    }

    // --- Devotee & Admin Helpdesk / Query Management System ---

    suspend fun submitAppQuery(
        name: String,
        phone: String,
        city: String = "",
        role: String = "DEVOTEE",
        category: String = "OTHER",
        subject: String = "",
        message: String = "",
        deviceId: String = ""
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (name.isBlank() || phone.isBlank() || message.isBlank()) {
            return@withContext Pair(false, "नाम, फोन नंबर और समस्या विवरण दर्ज करना अनिवार्य है")
        }
        val now = System.currentTimeMillis()
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("sender_name", name.trim())
            put("sender_phone", phone.trim())
            put("sender_city", city.trim())
            put("sender_role", role.trim())
            put("category", category.trim())
            put("subject", subject.trim())
            put("message", message.trim())
            put("status", "PENDING")
            put("admin_reply", "")
            put("replied_by", "")
            put("replied_at", 0L)
            put("created_at", now)
        }
        val localId = db.insert("app_queries", null, cv)

        var serverMsg = "आपकी समस्या/सुझाव सुपर एडमिन को सफलतापूर्वक भेज दिया गया है!"
        var success = localId > 0

        try {
            val url = java.net.URL("https://shribalajikripadham.online/api/app_queries.php")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("action", "SUBMIT")
                put("sender_name", name.trim())
                put("sender_phone", phone.trim())
                put("sender_city", city.trim())
                put("sender_role", role.trim())
                put("category", category.trim())
                put("subject", subject.trim())
                put("message", message.trim())
                put("device_id", deviceId)
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode in 200..299) {
                val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(respStr)
                if (json.optBoolean("success", false)) {
                    val remoteId = json.optLong("query_id", 0L)
                    if (remoteId > 0L && localId > 0L) {
                        val upCv = ContentValues().apply { put("remote_id", remoteId) }
                        db.update("app_queries", upCv, "id = ?", arrayOf(localId.toString()))
                    }
                    serverMsg = json.optString("message", serverMsg)
                    success = true
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Pair(success, serverMsg)
    }

    suspend fun getMyQueries(phone: String, deviceId: String = ""): List<AppQuery> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AppQuery>()

        // 1. Fetch remote updates first
        try {
            val cleanPhone = java.net.URLEncoder.encode(phone.trim(), "UTF-8")
            val cleanDid = java.net.URLEncoder.encode(deviceId.trim(), "UTF-8")
            val url = java.net.URL("https://shribalajikripadham.online/api/app_queries.php?action=GET_MY&phone=$cleanPhone&device_id=$cleanDid")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 6000
            conn.readTimeout = 6000

            if (conn.responseCode in 200..299) {
                val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(respStr)
                if (json.optBoolean("success", false)) {
                    val arr = json.optJSONArray("queries")
                    if (arr != null) {
                        val db = dbHelper.writableDatabase
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val rId = item.optLong("id", 0L)
                            val sName = item.optString("sender_name", "")
                            val sPhone = item.optString("sender_phone", "")
                            val sCity = item.optString("sender_city", "")
                            val sRole = item.optString("sender_role", "DEVOTEE")
                            val cat = item.optString("category", "OTHER")
                            val sub = item.optString("subject", "")
                            val msg = item.optString("message", "")
                            val st = item.optString("status", "PENDING")
                            val aReply = item.optString("admin_reply", "")
                            val rBy = item.optString("replied_by", "")
                            val rAt = item.optLong("replied_at", 0L)
                            val cAt = item.optLong("created_at", System.currentTimeMillis() / 1000) * 1000L

                            val c = db.rawQuery("SELECT id FROM app_queries WHERE remote_id = ? OR (sender_phone = ? AND message = ?)", arrayOf(rId.toString(), sPhone, msg))
                            if (c.moveToFirst()) {
                                val localId = c.getLong(0)
                                val cv = ContentValues().apply {
                                    put("remote_id", rId)
                                    put("status", st)
                                    put("admin_reply", aReply)
                                    put("replied_by", rBy)
                                    put("replied_at", rAt)
                                }
                                db.update("app_queries", cv, "id = ?", arrayOf(localId.toString()))
                            } else {
                                val cv = ContentValues().apply {
                                    put("remote_id", rId)
                                    put("sender_name", sName)
                                    put("sender_phone", sPhone)
                                    put("sender_city", sCity)
                                    put("sender_role", sRole)
                                    put("category", cat)
                                    put("subject", sub)
                                    put("message", msg)
                                    put("status", st)
                                    put("admin_reply", aReply)
                                    put("replied_by", rBy)
                                    put("replied_at", rAt)
                                    put("created_at", cAt)
                                }
                                db.insert("app_queries", null, cv)
                            }
                            c.close()
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Read from local SQLite
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM app_queries WHERE sender_phone = ? OR sender_phone LIKE ? ORDER BY id DESC", arrayOf(phone, "%${phone.takeLast(10)}%"))
            while (cursor.moveToNext()) {
                list.add(parseAppQueryCursor(cursor))
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun getAllQueriesForSuperAdmin(): List<AppQuery> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AppQuery>()

        // 1. Fetch live from central server
        try {
            val url = java.net.URL("https://shribalajikripadham.online/api/app_queries.php?action=GET_ALL")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode in 200..299) {
                val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(respStr)
                if (json.optBoolean("success", false)) {
                    val arr = json.optJSONArray("queries")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            list.add(AppQuery(
                                id = item.optLong("id", 0L),
                                remoteId = item.optLong("id", 0L),
                                senderName = item.optString("sender_name", ""),
                                senderPhone = item.optString("sender_phone", ""),
                                senderCity = item.optString("sender_city", ""),
                                senderRole = item.optString("sender_role", "DEVOTEE"),
                                category = item.optString("category", "OTHER"),
                                subject = item.optString("subject", ""),
                                message = item.optString("message", ""),
                                attachmentUrl = item.optString("attachment_url", ""),
                                status = item.optString("status", "PENDING"),
                                adminReply = item.optString("admin_reply", ""),
                                repliedBy = item.optString("replied_by", ""),
                                repliedAt = item.optLong("replied_at", 0L),
                                createdAt = item.optLong("created_at", System.currentTimeMillis() / 1000) * 1000L
                            ))
                        }
                        return@withContext list
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to local SQLite if offline
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM app_queries ORDER BY id DESC", null)
            while (cursor.moveToNext()) {
                list.add(parseAppQueryCursor(cursor))
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun replyToAppQuery(
        queryId: Long,
        replyMessage: String,
        repliedBy: String = "सुपर एडमिन (अंकित चौधरी)"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (replyMessage.isBlank()) {
            return@withContext Pair(false, "उत्तर संदेश दर्ज करें")
        }
        val now = System.currentTimeMillis()
        var ok = false
        var msg = "उत्तर सफलतापूर्वक दर्ज हो गया!"

        try {
            val url = java.net.URL("https://shribalajikripadham.online/api/app_queries.php")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("action", "REPLY")
                put("id", queryId)
                put("reply", replyMessage.trim())
                put("replied_by", repliedBy.trim())
                put("status", "REPLIED")
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode in 200..299) {
                val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(respStr)
                if (json.optBoolean("success", false)) {
                    ok = true
                    msg = json.optString("message", msg)
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val db = dbHelper.writableDatabase
            val cv = ContentValues().apply {
                put("admin_reply", replyMessage.trim())
                put("replied_by", repliedBy.trim())
                put("replied_at", now / 1000)
                put("status", "REPLIED")
            }
            db.update("app_queries", cv, "remote_id = ? OR id = ?", arrayOf(queryId.toString(), queryId.toString()))
            ok = true
        } catch (e: Exception) {}

        Pair(ok, msg)
    }

    suspend fun deleteAppQuery(queryId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("https://shribalajikripadham.online/api/app_queries.php")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("action", "DELETE")
                put("id", queryId)
            }
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {}

        try {
            val db = dbHelper.writableDatabase
            db.delete("app_queries", "remote_id = ? OR id = ?", arrayOf(queryId.toString(), queryId.toString())) > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun parseAppQueryCursor(cursor: Cursor): AppQuery {
        return AppQuery(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            remoteId = try { cursor.getLong(cursor.getColumnIndexOrThrow("remote_id")) } catch (e: Exception) { 0L },
            senderName = cursor.getString(cursor.getColumnIndexOrThrow("sender_name")),
            senderPhone = cursor.getString(cursor.getColumnIndexOrThrow("sender_phone")),
            senderCity = try { cursor.getString(cursor.getColumnIndexOrThrow("sender_city")) } catch (e: Exception) { "" } ?: "",
            senderRole = try { cursor.getString(cursor.getColumnIndexOrThrow("sender_role")) } catch (e: Exception) { "DEVOTEE" } ?: "DEVOTEE",
            category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
            subject = try { cursor.getString(cursor.getColumnIndexOrThrow("subject")) } catch (e: Exception) { "" } ?: "",
            message = cursor.getString(cursor.getColumnIndexOrThrow("message")),
            attachmentUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("attachment_url")) } catch (e: Exception) { "" } ?: "",
            status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
            adminReply = try { cursor.getString(cursor.getColumnIndexOrThrow("admin_reply")) } catch (e: Exception) { "" } ?: "",
            repliedBy = try { cursor.getString(cursor.getColumnIndexOrThrow("replied_by")) } catch (e: Exception) { "" } ?: "",
            repliedAt = try { cursor.getLong(cursor.getColumnIndexOrThrow("replied_at")) } catch (e: Exception) { 0L },
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        )
    }
}

