package com.example.shribalajikripadham.notification

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.util.Log
import com.example.shribalajikripadham.util.AppUpdateManager
import com.example.shribalajikripadham.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class AshramBackgroundPushJobService : JobService() {

    companion object {
        private const val TAG = "AshramPushJobService"
        private const val PREFS_NAME = "sbkd_background_push_prefs"
        private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version_code"
        private const val KEY_LAST_NOTIFIED_BROADCAST_TS = "last_notified_broadcast_timestamp"
        private const val KEY_LAST_EMERGENCY_HASH = "last_notified_emergency_hash"
        private const val KEY_LAST_DARBAR_STATUS = "last_notified_darbar_status"
        private const val KEY_LAST_DARBAR_STARTED_DATE = "last_darbar_started_date"
        private const val KEY_LAST_DARBAR_ENDED_DATE = "last_darbar_ended_date"
        private const val KEY_LAST_SUVICHAR_DATE = "last_suvichar_date"
        private const val KEY_LAST_SERVING_TOKEN = "last_notified_serving_token"

        private const val RAW_BROADCASTS_URL =
            "https://raw.githubusercontent.com/ankit261194/shri-balaji-kripa-dham/main/live_broadcasts.json"
        private const val HOSTINGER_CONFIG_URL =
            "https://shribalajikripadham.online/api/live_config.php"
        private const val RAW_UI_CONFIG_URL =
            "https://raw.githubusercontent.com/ankit261194/shri-balaji-kripa-dham/main/live_ui_config.json"

        /**
         * Checks online update, broadcasts, and live Darbar state in a coroutine.
         * Safe to call from anywhere (JobService, BootReceiver, or App launch).
         * Completely eliminates external FCM dependency with 100% native Android notifications.
         */
        suspend fun executeBackgroundCheck(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // 1. Check for New App Updates
            try {
                val currentVersionCode = AppUpdateManager.getCurrentVersionCode(context)
                val onlineInfo = AppUpdateManager.fetchLatestUpdateFromOnline()
                if (onlineInfo != null && onlineInfo.versionCode > currentVersionCode) {
                    val lastNotifiedVersion = prefs.getInt(KEY_LAST_NOTIFIED_VERSION, 0)
                    if (onlineInfo.versionCode > lastNotifiedVersion) {
                        NotificationHelper.showSystemNotification(
                            context = context,
                            title = "⚡ नया अपडेट उपलब्ध है: v${onlineInfo.versionName}",
                            message = "सुरक्षा पैच एवं सिस्टम स्थिरता सुधार। दर्शन व टोकन सेवा हेतु तुरंत अपडेट करें।",
                            notificationId = 10001
                        )
                        prefs.edit().putInt(KEY_LAST_NOTIFIED_VERSION, onlineInfo.versionCode).apply()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed: ${e.message}")
            }

            // 2. Check for New Broadcast Notice from Guruji / Super Admin
            try {
                val cacheBusterUrl = "$RAW_BROADCASTS_URL?nocache=${System.currentTimeMillis()}"
                val url = URL(cacheBusterUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.useCaches = false
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "BalajiApp-PushCheck")

                if (conn.responseCode in 200..299) {
                    val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    conn.disconnect()
                    if (resp.isNotBlank() && resp.startsWith("{")) {
                        val json = JSONObject(resp)
                        val title = json.optString("title", "")
                        val message = json.optString("message", "")
                        val timestamp = json.optLong("timestamp", 0L)

                        if (title.isNotBlank() && message.isNotBlank() && timestamp > 0L) {
                            val lastSeenTs = prefs.getLong(KEY_LAST_NOTIFIED_BROADCAST_TS, 0L)
                            if (timestamp > lastSeenTs) {
                                NotificationHelper.showSystemNotification(
                                    context = context,
                                    title = "📢 $title",
                                    message = message,
                                    notificationId = 10002
                                )
                                prefs.edit().putLong(KEY_LAST_NOTIFIED_BROADCAST_TS, timestamp).apply()
                            }
                        }
                    }
                } else {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Broadcast check failed: ${e.message}")
            }

            // 3. Autonomous Live Config Check (Emergency Notices, Darbar Active/Inactive, Serving Tokens)
            try {
                var configJson: JSONObject? = null
                val primaryUrl = "$HOSTINGER_CONFIG_URL?nocache=${System.currentTimeMillis()}"
                try {
                    val conn = (URL(primaryUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 6000
                        useCaches = false
                        requestMethod = "GET"
                        setRequestProperty("X-SBKD-API-KEY", com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.API_SECRET_KEY)
                        setRequestProperty("User-Agent", "BalajiApp-PushCheck")
                    }
                    if (conn.responseCode in 200..299) {
                        val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                        if (resp.isNotBlank() && resp.startsWith("{")) {
                            configJson = JSONObject(resp)
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    Log.w(TAG, "Hostinger config fetch failed, trying GitHub mirror: ${e.message}")
                }

                // Fallback to GitHub raw mirror
                if (configJson == null) {
                    val fallbackUrl = "$RAW_UI_CONFIG_URL?nocache=${System.currentTimeMillis()}"
                    val conn = (URL(fallbackUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5000
                        readTimeout = 5000
                        useCaches = false
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "BalajiApp-PushCheck")
                    }
                    if (conn.responseCode in 200..299) {
                        val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                        if (resp.isNotBlank() && resp.startsWith("{")) {
                            configJson = JSONObject(resp)
                        }
                    }
                    conn.disconnect()
                }

                if (configJson != null) {
                    val root = if (configJson.has("config")) configJson.optJSONObject("config") ?: configJson else configJson

                    // 3A. Emergency Notice Alert
                    val isNoticeVisible = root.optBoolean("is_emergency_notice_visible", false)
                    val emergencyNotice = root.optString("emergency_notice", "").trim()
                    if (isNoticeVisible && emergencyNotice.isNotBlank()) {
                        val currentHash = emergencyNotice.hashCode()
                        val lastHash = prefs.getInt(KEY_LAST_EMERGENCY_HASH, 0)
                        if (currentHash != lastHash) {
                            NotificationHelper.showSystemNotification(
                                context = context,
                                title = "🚨 आवश्यक सूचना | श्री बालाजी कृपा धाम",
                                message = emergencyNotice,
                                notificationId = 10003
                            )
                            prefs.edit().putInt(KEY_LAST_EMERGENCY_HASH, currentHash).apply()
                        }
                    }

                    // 3B. Darbar Start / End State Alert (STRICTLY ON SUNDAY / DARBAR DAYS ONLY)
                    val cal = java.util.Calendar.getInstance()
                    val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
                    val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                    val todayDateStr = com.example.shribalajikripadham.data.local.DatabaseHelper.getTodayDateString()
                    val isTuesdayDarbar = (root.optBoolean("is_tuesday_token_enabled", false) || root.optBoolean("is_tuesday_darbar_enabled", false)) && dayOfWeek == java.util.Calendar.TUESDAY
                    val isSundayDarbar = dayOfWeek == java.util.Calendar.SUNDAY
                    val isDarbarDay = isSundayDarbar || isTuesdayDarbar

                    val isDarbarActive = root.optBoolean("is_darbar_active", false)

                    if (isDarbarDay) {
                        if (prefs.contains(KEY_LAST_DARBAR_STATUS)) {
                            val prevDarbarStatus = prefs.getBoolean(KEY_LAST_DARBAR_STATUS, false)
                            if (prevDarbarStatus != isDarbarActive) {
                                if (isDarbarActive) {
                                    val lastStartedDate = prefs.getString(KEY_LAST_DARBAR_STARTED_DATE, "")
                                    if (lastStartedDate != todayDateStr && currentHour in 6..18) {
                                        val timings = root.optString("darbar_timings", "प्रातःकाल 8:00 बजे से")
                                        NotificationHelper.showSystemNotification(
                                            context = context,
                                            title = "🚩 पावन दरबार प्रारंभ हो चुका है!",
                                            message = "पूज्य गुरुजी द्वारा दिव्य दरबार प्रारंभ हो गया है ($timings)। श्रद्धालु दर्शन व आशीर्वाद प्राप्त करें।",
                                            notificationId = 10004
                                        )
                                        prefs.edit().putString(KEY_LAST_DARBAR_STARTED_DATE, todayDateStr).apply()
                                    }
                                } else {
                                    // Darbar ended notification: ONLY if it was previously active, it's afternoon/evening (>= 12), and not notified yet today
                                    val lastEndedDate = prefs.getString(KEY_LAST_DARBAR_ENDED_DATE, "")
                                    if (lastEndedDate != todayDateStr && currentHour >= 12 && prevDarbarStatus) {
                                        NotificationHelper.showSystemNotification(
                                            context = context,
                                            title = "🙏 आज का दिव्य दरबार संपन्न हुआ",
                                            message = "आज का पावन दरबार संपन्न हो चुका है। सभी श्रद्धालुओं पर बालाजी महाराज की कृपा बनी रहे।",
                                            notificationId = 10004
                                        )
                                        prefs.edit().putString(KEY_LAST_DARBAR_ENDED_DATE, todayDateStr).apply()
                                    }
                                }
                                prefs.edit().putBoolean(KEY_LAST_DARBAR_STATUS, isDarbarActive).apply()
                            }
                        } else {
                            prefs.edit().putBoolean(KEY_LAST_DARBAR_STATUS, isDarbarActive).apply()
                        }
                    } else {
                        // Weekday / Non-Darbar Day: Reset status to false so weekday syncs NEVER trigger "दरबार संपन्न"
                        prefs.edit().putBoolean(KEY_LAST_DARBAR_STATUS, false).apply()
                    }

                    // 3C. Daily Morning Sacred Suvichar Notification (6:30 AM to 11:30 AM)
                    val lastSuvicharDate = prefs.getString(KEY_LAST_SUVICHAR_DATE, "")
                    if (lastSuvicharDate != todayDateStr && currentHour in 6..12) {
                        try {
                            val fullQuote = com.example.shribalajikripadham.ui.home.DailyDarshanHelper.getTodayGuruVichar()
                            val cleanQuote = fullQuote.replace("\n", " ").trim()
                            val shortQuote = if (cleanQuote.length > 140) cleanQuote.take(137) + "..." else cleanQuote
                            NotificationHelper.showSystemNotification(
                                context = context,
                                title = "🌅 आज का पावन सुविचार | श्री बालाजी कृपा धाम",
                                message = shortQuote,
                                notificationId = 10008
                            )
                            prefs.edit().putString(KEY_LAST_SUVICHAR_DATE, todayDateStr).apply()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to send morning suvichar notification: ${e.message}")
                        }
                    }

                    // 3C. Live Serving Token Updates & Devotee Proximity Alerts
                    val currentServingToken = root.optInt("current_serving_token", root.optInt("running_token_number", 0))
                    if (currentServingToken > 0) {
                        try {
                            val myTokPrefs = context.getSharedPreferences("sbkd_devotee_my_token_prefs", Context.MODE_PRIVATE)
                            val myToken = myTokPrefs.getInt("my_token_number", 0)
                            val myTokenDate = myTokPrefs.getString("my_token_date", "") ?: ""
                            val todayStr = com.example.shribalajikripadham.data.local.DatabaseHelper.getTodayDateString()

                            if (myToken > 0 && (myTokenDate == todayStr || myTokenDate.isBlank())) {
                                com.example.shribalajikripadham.util.SmartTokenAlertHelper.evaluateAndTriggerAlert(
                                    context = context,
                                    myToken = myToken,
                                    currentServing = currentServingToken
                                )
                            }
                        } catch (e: Exception) {}

                        // Only send queue advancement notification if the devotee actually holds a token for today
                        val myTokPrefs = try {
                            context.getSharedPreferences("sbkd_devotee_my_token_prefs", Context.MODE_PRIVATE)
                        } catch (e: Exception) { null }
                        val devoteeToken = myTokPrefs?.getInt("my_token_number", 0) ?: 0
                        val devoteeDate = myTokPrefs?.getString("my_token_date", "") ?: ""
                        val todayStr = com.example.shribalajikripadham.data.local.DatabaseHelper.getTodayDateString()
                        val hasTokenToday = devoteeToken > 0 && (devoteeDate == todayStr || devoteeDate.isBlank())

                        if (hasTokenToday && prefs.contains(KEY_LAST_SERVING_TOKEN)) {
                            val lastToken = prefs.getInt(KEY_LAST_SERVING_TOKEN, 0)
                            if (lastToken > 0 && currentServingToken > lastToken) {
                                NotificationHelper.showSystemNotification(
                                    context = context,
                                    title = "🎫 लाइव कतार अपडेट",
                                    message = "दरबार में अब टोकन नंबर $currentServingToken बुलाया जा रहा है। (आपका टोकन #$devoteeToken)",
                                    notificationId = 10005
                                )
                                prefs.edit().putInt(KEY_LAST_SERVING_TOKEN, currentServingToken).apply()
                            }
                        } else {
                            prefs.edit().putInt(KEY_LAST_SERVING_TOKEN, currentServingToken).apply()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Live config push check failed: ${e.message}")
            }

            // 4. Autonomous Devotee Personal Inbox Check (Token Call & Direct Alerts)
            try {
                val fcmPrefs = context.getSharedPreferences(
                    com.example.shribalajikripadham.notification.AshramFirebaseMessagingService.PREFS_FCM,
                    Context.MODE_PRIVATE
                )
                val registeredPhone = fcmPrefs.getString(
                    com.example.shribalajikripadham.notification.AshramFirebaseMessagingService.KEY_LAST_PHONE,
                    null
                )
                if (!registeredPhone.isNullOrBlank()) {
                    val notifications = com.example.shribalajikripadham.data.network.HostingerCentralSyncManager.fetchDevoteeNotifications(registeredPhone)
                    val lastSeenNotifId = prefs.getLong("last_seen_inbox_notif_id", 0L)
                    var maxNotifId = lastSeenNotifId

                    for (notif in notifications) {
                        if (!notif.isRead && notif.id > lastSeenNotifId) {
                            NotificationHelper.showSystemNotification(
                                context = context,
                                title = notif.title.ifBlank { "🔔 श्री बालाजी कृपा धाम" },
                                message = notif.message,
                                notificationId = (notif.id % 100000).toInt()
                            )
                            if (notif.id > maxNotifId) {
                                maxNotifId = notif.id
                            }
                        }
                    }

                    if (maxNotifId > lastSeenNotifId) {
                        prefs.edit().putLong("last_seen_inbox_notif_id", maxNotifId).apply()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Devotee inbox background check failed: ${e.message}")
            }
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                executeBackgroundCheck(applicationContext)
            } finally {
                jobFinished(params, false)
            }
        }
        return true // Asynchronous execution
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true // Reschedule if interrupted
    }
}
