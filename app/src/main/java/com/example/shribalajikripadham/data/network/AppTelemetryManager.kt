package com.example.shribalajikripadham.data.network

import android.content.ContentValues
import android.content.Context
import android.os.Build
import com.example.shribalajikripadham.data.local.DatabaseHelper
import com.example.shribalajikripadham.data.model.DevicePresence
import com.example.shribalajikripadham.hardware.DeviceFingerprintManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

object AppTelemetryManager {

    fun getDeviceModelName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    /**
     * Records an app launch heartbeat on the local device and pushes live presence to Google Sheets.
     */
    suspend fun recordAppHeartbeat(
        context: Context,
        devoteeName: String = "",
        devoteePhone: String = "",
        city: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceId = DeviceFingerprintManager.getDeviceId(context)
            val deviceModel = getDeviceModelName()
            val appVersion = try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "2.9.0"
            } catch (e: Exception) {
                "2.9.0"
            }

            // 1. Cache to local SQLite database
            saveDeviceLocally(
                context = context,
                presence = DevicePresence(
                    deviceId = deviceId,
                    deviceModel = deviceModel,
                    userName = devoteeName,
                    phoneNumber = devoteePhone,
                    city = city,
                    appVersion = appVersion,
                    lastSeenAt = System.currentTimeMillis()
                )
            )

            // 2. Post to central Google Sheet Webhook if configured
            val webhookUrl = GoogleSheetTokenSyncManager.getWebhookUrl(context)
            if (webhookUrl.isBlank() || !webhookUrl.startsWith("https://script.google.com/")) {
                return@withContext false
            }

            val payload = JSONObject().apply {
                put("action", "DEVICE_HEARTBEAT")
                put("device_id", deviceId)
                put("device_model", deviceModel)
                put("user_name", devoteeName)
                put("phone_number", devoteePhone)
                put("city", city)
                put("app_version", appVersion)
            }

            val url = URL(webhookUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                instanceFollowRedirects = true
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }

            val responseCode = conn.responseCode
            responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Saves or updates device presence row in local SQLite database.
     */
    fun saveDeviceLocally(context: Context, presence: DevicePresence) {
        try {
            val dbHelper = DatabaseHelper(context)
            val db = dbHelper.writableDatabase

            val checkCursor = db.rawQuery(
                "SELECT open_count FROM active_device_telemetry WHERE device_id = ?",
                arrayOf(presence.deviceId)
            )
            var curCount = 0
            val exists = checkCursor.moveToFirst()
            if (exists) {
                curCount = checkCursor.getInt(0)
            }
            checkCursor.close()

            val cv = ContentValues().apply {
                put("device_id", presence.deviceId)
                put("device_model", presence.deviceModel)
                if (presence.userName.isNotBlank()) put("user_name", presence.userName)
                if (presence.phoneNumber.isNotBlank()) put("phone_number", presence.phoneNumber)
                if (presence.city.isNotBlank()) put("city", presence.city)
                put("app_version", presence.appVersion)
                put("last_seen_at", presence.lastSeenAt)
                put("open_count", curCount + 1)
            }

            if (exists) {
                db.update("active_device_telemetry", cv, "device_id = ?", arrayOf(presence.deviceId))
            } else {
                db.insert("active_device_telemetry", null, cv)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Fetches real-time active devices presence from Google Sheets for Super Admin.
     * Returns Triple(totalDevices, activeTodayCount, listOfDevices).
     */
    suspend fun fetchActiveDevicesFromSheet(
        context: Context
    ): Triple<Int, Int, List<DevicePresence>> = withContext(Dispatchers.IO) {
        val webhookUrl = GoogleSheetTokenSyncManager.getWebhookUrl(context)
        if (webhookUrl.isBlank() || !webhookUrl.startsWith("https://script.google.com/")) {
            val localList = getLocalDevices(context)
            return@withContext Triple(localList.size, localList.size, localList)
        }

        try {
            val queryUrl = if (webhookUrl.contains("?")) {
                "$webhookUrl&action=get_devices"
            } else {
                "$webhookUrl?action=get_devices"
            }

            val conn = (URL(queryUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
                instanceFollowRedirects = true
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val localList = getLocalDevices(context)
                return@withContext Triple(localList.size, localList.size, localList)
            }

            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)
            val total = json.optInt("total", 0)
            val activeToday = json.optInt("active_today", 0)
            val devArray = json.optJSONArray("devices")

            val list = mutableListOf<DevicePresence>()
            if (devArray != null) {
                for (i in 0 until devArray.length()) {
                    val obj = devArray.getJSONObject(i)
                    val dev = DevicePresence(
                        deviceId = obj.optString("device_id"),
                        deviceModel = obj.optString("device_model", "Android Device"),
                        userName = obj.optString("user_name", ""),
                        phoneNumber = obj.optString("phone_number", ""),
                        city = obj.optString("city", ""),
                        appVersion = obj.optString("app_version", "2.9.0"),
                        lastSeenAt = System.currentTimeMillis(), // fallback
                        openCount = obj.optInt("open_count", 1)
                    )
                    list.add(dev)
                    // Update local cache
                    saveDeviceLocally(context, dev)
                }
            }

            Triple(total, activeToday, list)
        } catch (e: Exception) {
            e.printStackTrace()
            val localList = getLocalDevices(context)
            Triple(localList.size, localList.size, localList)
        }
    }

    /**
     * Reads all cached devices from local SQLite database.
     */
    fun getLocalDevices(context: Context): List<DevicePresence> {
        val list = mutableListOf<DevicePresence>()
        try {
            val dbHelper = DatabaseHelper(context)
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM active_device_telemetry ORDER BY last_seen_at DESC",
                null
            )
            while (cursor.moveToNext()) {
                list.add(
                    DevicePresence(
                        deviceId = cursor.getString(cursor.getColumnIndexOrThrow("device_id")),
                        deviceModel = cursor.getString(cursor.getColumnIndexOrThrow("device_model")),
                        userName = cursor.getString(cursor.getColumnIndexOrThrow("user_name")),
                        phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone_number")),
                        city = cursor.getString(cursor.getColumnIndexOrThrow("city")),
                        appVersion = cursor.getString(cursor.getColumnIndexOrThrow("app_version")),
                        lastSeenAt = cursor.getLong(cursor.getColumnIndexOrThrow("last_seen_at")),
                        openCount = cursor.getInt(cursor.getColumnIndexOrThrow("open_count"))
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
