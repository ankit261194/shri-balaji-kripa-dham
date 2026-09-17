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

                    // 3B. Darbar Start / End State Alert
                    val isDarbarActive = root.optBoolean("is_darbar_active", true)
                    if (prefs.contains(KEY_LAST_DARBAR_STATUS)) {
                        val prevDarbarStatus = prefs.getBoolean(KEY_LAST_DARBAR_STATUS, true)
                        if (prevDarbarStatus != isDarbarActive) {
                            if (isDarbarActive) {
                                val timings = root.optString("darbar_timings", "प्रत्येक रविवार प्रातःकाल 8:00 बजे से")
                                NotificationHelper.showSystemNotification(
                                    context = context,
                                    title = "🚩 पावन दरबार प्रारंभ हो चुका है!",
                                    message = "पूज्य गुरुजी द्वारा दिव्य दरबार प्रारंभ हो गया है ($timings)। श्रद्धालु दर्शन व आशीर्वाद प्राप्त करें।",
                                    notificationId = 10004
                                )
                            } else {
                                NotificationHelper.showSystemNotification(
                                    context = context,
                                    title = "🙏 आज का दिव्य दरबार संपन्न हुआ",
                                    message = "आज का पावन दरबार संपन्न हो चुका है। सभी श्रद्धालुओं पर बालाजी महाराज की कृपा बनी रहे।",
                                    notificationId = 10004
                                )
                            }
                            prefs.edit().putBoolean(KEY_LAST_DARBAR_STATUS, isDarbarActive).apply()
                        }
                    } else {
                        prefs.edit().putBoolean(KEY_LAST_DARBAR_STATUS, isDarbarActive).apply()
                    }

                    // 3C. Live Serving Token Updates
                    val currentServingToken = root.optInt("current_serving_token", root.optInt("running_token_number", 0))
                    if (currentServingToken > 0) {
                        if (prefs.contains(KEY_LAST_SERVING_TOKEN)) {
                            val lastToken = prefs.getInt(KEY_LAST_SERVING_TOKEN, 0)
                            if (lastToken > 0 && currentServingToken > lastToken) {
                                NotificationHelper.showSystemNotification(
                                    context = context,
                                    title = "🎫 लाइव टोकन अपडेट",
                                    message = "वर्तमान में टोकन नंबर $currentServingToken बुलाया जा रहा है। कृपया अपनी बारी हेतु तैयार रहें।",
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
