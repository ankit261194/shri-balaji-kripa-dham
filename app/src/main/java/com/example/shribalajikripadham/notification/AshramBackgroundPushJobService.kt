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

        private const val RAW_BROADCASTS_URL =
            "https://raw.githubusercontent.com/ankit261194/shri-balaji-kripa-dham/main/live_broadcasts.json"

        /**
         * Checks online update and broadcasts immediately in a coroutine.
         * Safe to call from anywhere (JobService, BootReceiver, or App launch).
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
