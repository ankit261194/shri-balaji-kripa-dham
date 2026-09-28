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

    fun getAndroidVersionName(): String {
        val release = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT
        return "Android $release (API $sdkInt)"
    }

    /**
     * Resolves the best known identity (devotee name, phone, city, role) for this device
     * from Admin preferences, Token preferences, and local SQLite tables.
     */
    fun resolveCurrentDeviceIdentity(context: Context): Triple<String, String, Triple<String, String, String>> {
        var resolvedName = ""
        var resolvedPhone = ""
        var resolvedCity = ""
        var resolvedRole = "USER"

        try {
            // 1. Check logged-in Admin preferences
            val adminPrefs = context.getSharedPreferences("sbkd_admin_login_prefs", Context.MODE_PRIVATE)
            val adminName = adminPrefs.getString("admin_name", "")?.trim() ?: ""
            val adminPhone = adminPrefs.getString("admin_phone", "")?.trim() ?: ""
            val adminRole = adminPrefs.getString("admin_role", "")?.trim() ?: ""

            if (adminName.isNotBlank()) {
                resolvedName = adminName
                resolvedPhone = adminPhone
                resolvedRole = if (adminRole.isNotBlank()) adminRole else "SUPER_ADMIN"
            }

            // 2. Check Devotee Token preferences if name is still blank
            val tokenPrefs = context.getSharedPreferences("sbkd_devotee_my_token_prefs", Context.MODE_PRIVATE)
            val tokName = tokenPrefs.getString("my_patient_name", "")?.trim() ?: ""
            val tokPhone = tokenPrefs.getString("my_phone_number", "")?.trim() ?: ""
            val tokCity = tokenPrefs.getString("my_city", "")?.trim() ?: ""

            if (resolvedName.isBlank() && tokName.isNotBlank()) {
                resolvedName = tokName
            }
            if (resolvedPhone.isBlank() && tokPhone.isNotBlank()) {
                resolvedPhone = tokPhone
            }
            if (resolvedCity.isBlank() && tokCity.isNotBlank()) {
                resolvedCity = tokCity
            }

            // 3. Fallback to local SQLite database if still blank
            if (resolvedName.isBlank() || resolvedPhone.isBlank() || resolvedCity.isBlank()) {
                val dbHelper = DatabaseHelper(context)
                val db = dbHelper.readableDatabase
                val cursor = db.rawQuery(
                    "SELECT patient_name, phone_number, village_or_city FROM tokens WHERE patient_name != '' ORDER BY id DESC LIMIT 1",
                    null
                )
                if (cursor.moveToFirst()) {
                    if (resolvedName.isBlank()) resolvedName = cursor.getString(0) ?: ""
                    if (resolvedPhone.isBlank()) resolvedPhone = cursor.getString(1) ?: ""
                    if (resolvedCity.isBlank()) resolvedCity = cursor.getString(2) ?: ""
                }
                cursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Triple(resolvedName, resolvedPhone, Triple(resolvedCity, resolvedRole, ""))
    }

    /**
     * Guarantees that the device currently executing this code is present in the list,
     * marked as "This Device", online now, and placed prominently at the top.
     */
    fun ensureCurrentDeviceInList(
        context: Context,
        list: List<DevicePresence>
    ): List<DevicePresence> {
        val currentDeviceId = DeviceFingerprintManager.getDeviceId(context)
        val myModel = getDeviceModelName()
        val myAndroid = getAndroidVersionName()
        val myAppVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "2.56.14"
        } catch (e: Exception) {
            "2.56.14"
        }

        val identity = resolveCurrentDeviceIdentity(context)
        val resolvedName = identity.first
        val resolvedPhone = identity.second
        val resolvedCity = identity.third.first
        val resolvedRole = identity.third.second

        val result = list.toMutableList()
        val existingIndex = result.indexOfFirst { it.deviceId == currentDeviceId }

        val currentPresence = if (existingIndex != -1) {
            val existing = result[existingIndex]
            existing.copy(
                deviceModel = if (myModel.isNotBlank()) myModel else existing.deviceModel,
                androidVersion = if (myAndroid.isNotBlank()) myAndroid else existing.androidVersion,
                appVersion = myAppVersion,
                userName = if (resolvedName.isNotBlank()) resolvedName else existing.userName,
                phoneNumber = if (resolvedPhone.isNotBlank()) resolvedPhone else existing.phoneNumber,
                city = if (resolvedCity.isNotBlank()) resolvedCity else existing.city,
                role = if (resolvedRole != "USER") resolvedRole else existing.role,
                lastSeenAt = System.currentTimeMillis(),
                isOnline = true
            )
        } else {
            DevicePresence(
                deviceId = currentDeviceId,
                deviceModel = myModel,
                userName = resolvedName,
                phoneNumber = resolvedPhone,
                city = resolvedCity,
                appVersion = myAppVersion,
                lastSeenAt = System.currentTimeMillis(),
                openCount = 1,
                role = resolvedRole,
                androidVersion = myAndroid,
                isOnline = true
            )
        }

        if (existingIndex != -1) {
            result.removeAt(existingIndex)
        }
        result.add(0, currentPresence)

        // Cache this current presence locally
        saveDeviceLocally(context, currentPresence)

        return result
    }

    /**
     * Records an app launch heartbeat on the local device, pushes live presence to
     * Central Hostinger MySQL backend, and optionally Google Sheets Webhook.
     */
    suspend fun recordAppHeartbeat(
        context: Context,
        devoteeName: String = "",
        devoteePhone: String = "",
        city: String = "",
        role: String = "USER"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceId = DeviceFingerprintManager.getDeviceId(context)
            val deviceModel = getDeviceModelName()
            val androidVersion = getAndroidVersionName()
            val appVersion = try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "2.56.14"
            } catch (e: Exception) {
                "2.56.14"
            }

            // Auto-resolve identity if arguments are blank
            val identity = resolveCurrentDeviceIdentity(context)
            val finalName = devoteeName.ifBlank { identity.first }
            val finalPhone = devoteePhone.ifBlank { identity.second }
            val finalCity = city.ifBlank { identity.third.first }
            val finalRole = if (role != "USER") role else identity.third.second

            val presence = DevicePresence(
                deviceId = deviceId,
                deviceModel = deviceModel,
                userName = finalName,
                phoneNumber = finalPhone,
                city = finalCity,
                appVersion = appVersion,
                lastSeenAt = System.currentTimeMillis(),
                role = finalRole,
                androidVersion = androidVersion,
                isOnline = true
            )

            // 1. Cache to local SQLite database
            saveDeviceLocally(context = context, presence = presence)

            // 2. Push to Central Hostinger MySQL backend (Primary Authentic Cloud)
            val hostingerSuccess = HostingerCentralSyncManager.recordDeviceHeartbeat(
                deviceId = deviceId,
                deviceModel = deviceModel,
                androidVersion = androidVersion,
                appVersion = appVersion,
                userName = finalName,
                phoneNumber = finalPhone,
                city = finalCity,
                role = finalRole
            )

            // 3. Post to central Google Sheet Webhook if configured
            val webhookUrl = GoogleSheetTokenSyncManager.getWebhookUrl(context)
            if (webhookUrl.isNotBlank() && webhookUrl.startsWith("https://script.google.com/")) {
                try {
                    val payload = JSONObject().apply {
                        put("action", "DEVICE_HEARTBEAT")
                        put("device_id", deviceId)
                        put("device_model", deviceModel)
                        put("android_version", androidVersion)
                        put("user_name", finalName)
                        put("phone_number", finalPhone)
                        put("city", finalCity)
                        put("app_version", appVersion)
                        put("role", finalRole)
                    }

                    val url = URL(webhookUrl)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        connectTimeout = 6000
                        readTimeout = 6000
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        setRequestProperty("Accept", "application/json")
                        instanceFollowRedirects = true
                    }

                    conn.outputStream.use { os ->
                        os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                        os.flush()
                    }
                    conn.responseCode
                } catch (e: Exception) {}
            }

            hostingerSuccess
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

            // Ensure android_version column exists in SQLite table
            try {
                db.execSQL("ALTER TABLE active_device_telemetry ADD COLUMN android_version TEXT NOT NULL DEFAULT ''")
            } catch (e: Exception) {}

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
                put("role", presence.role)
                if (presence.androidVersion.isNotBlank()) put("android_version", presence.androidVersion)
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
     * Fetches real-time active devices presence for Super Admin.
     * Prioritizes Hostinger Central MySQL backend, falling back to GitHub Live CDN,
     * Google Sheets, and local SQLite cache.
     *
     * Returns Triple(totalDevices, activeTodayCount, listOfDevices).
     */
    suspend fun fetchActiveDevicesFromSheet(
        context: Context
    ): Triple<Int, Int, List<DevicePresence>> = withContext(Dispatchers.IO) {
        // 1. Primary Authentic Source: Central Hostinger MySQL backend
        try {
            val hostingerResult = HostingerCentralSyncManager.fetchLiveDevices()
            if (hostingerResult.first > 0 || hostingerResult.third.isNotEmpty()) {
                // Cache devices locally
                for (dev in hostingerResult.third) {
                    saveDeviceLocally(context, dev)
                }
                val mergedList = ensureCurrentDeviceInList(context, hostingerResult.third)
                val now = System.currentTimeMillis()
                val oneDayAgo = now - 24 * 3600 * 1000L
                val activeToday = mergedList.count { it.lastSeenAt >= oneDayAgo }
                return@withContext Triple(mergedList.size, activeToday.coerceAtLeast(1), mergedList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Secondary Source: GitHub Live Cloud
        try {
            val ghResult = GitHubLiveSyncManager.fetchLiveDevices(context)
            if (ghResult.first > 0 || ghResult.third.isNotEmpty()) {
                val mergedList = ensureCurrentDeviceInList(context, ghResult.third)
                val now = System.currentTimeMillis()
                val oneDayAgo = now - 24 * 3600 * 1000L
                val activeToday = mergedList.count { it.lastSeenAt >= oneDayAgo }
                return@withContext Triple(mergedList.size, activeToday.coerceAtLeast(1), mergedList)
            }
        } catch (e: Exception) {}

        // 3. Tertiary Source: Google Sheet Webhook (if configured)
        val webhookUrl = GoogleSheetTokenSyncManager.getWebhookUrl(context)
        if (webhookUrl.isNotBlank() && webhookUrl.startsWith("https://script.google.com/")) {
            try {
                val queryUrl = if (webhookUrl.contains("?")) {
                    "$webhookUrl&action=get_devices"
                } else {
                    "$webhookUrl?action=get_devices"
                }

                val conn = (URL(queryUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/json")
                    instanceFollowRedirects = true
                }

                if (conn.responseCode in 200..299) {
                    val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseBody)
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
                                appVersion = obj.optString("app_version", "2.56.14"),
                                lastSeenAt = obj.optLong("last_seen_at", System.currentTimeMillis()),
                                openCount = obj.optInt("open_count", 1),
                                role = obj.optString("role", "USER"),
                                androidVersion = obj.optString("android_version", "")
                            )
                            list.add(dev)
                            saveDeviceLocally(context, dev)
                        }
                    }

                    if (list.isNotEmpty()) {
                        val mergedList = ensureCurrentDeviceInList(context, list)
                        val now = System.currentTimeMillis()
                        val oneDayAgo = now - 24 * 3600 * 1000L
                        val activeToday = mergedList.count { it.lastSeenAt >= oneDayAgo }
                        return@withContext Triple(mergedList.size, activeToday.coerceAtLeast(1), mergedList)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Fallback Source: Local SQLite database
        val localList = ensureCurrentDeviceInList(context, getLocalDevices(context))
        val now = System.currentTimeMillis()
        val oneDayAgo = now - 24 * 3600 * 1000L
        val activeToday = localList.count { it.lastSeenAt >= oneDayAgo }
        Triple(localList.size, activeToday.coerceAtLeast(1), localList)
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
            val roleCol = cursor.getColumnIndex("role")
            val androidCol = cursor.getColumnIndex("android_version")
            val now = System.currentTimeMillis()
            val fifteenMinsAgo = now - 15 * 60 * 1000L

            while (cursor.moveToNext()) {
                val roleVal = if (roleCol != -1) cursor.getString(roleCol) ?: "USER" else "USER"
                val androidVal = if (androidCol != -1) cursor.getString(androidCol) ?: "" else ""
                val lastSeen = cursor.getLong(cursor.getColumnIndexOrThrow("last_seen_at"))
                val isOnline = (lastSeen >= fifteenMinsAgo)

                list.add(
                    DevicePresence(
                        deviceId = cursor.getString(cursor.getColumnIndexOrThrow("device_id")),
                        deviceModel = cursor.getString(cursor.getColumnIndexOrThrow("device_model")),
                        userName = cursor.getString(cursor.getColumnIndexOrThrow("user_name")),
                        phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone_number")),
                        city = cursor.getString(cursor.getColumnIndexOrThrow("city")),
                        appVersion = cursor.getString(cursor.getColumnIndexOrThrow("app_version")),
                        lastSeenAt = lastSeen,
                        openCount = cursor.getInt(cursor.getColumnIndexOrThrow("open_count")),
                        role = roleVal,
                        androidVersion = androidVal,
                        isOnline = isOnline
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
