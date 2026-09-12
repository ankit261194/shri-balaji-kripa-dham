package com.example.shribalajikripadham.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.shribalajikripadham.ai.FaceEmbeddingEngine
import com.example.shribalajikripadham.data.local.DatabaseHelper
import com.example.shribalajikripadham.data.model.*
import com.example.shribalajikripadham.hardware.GeofenceLocationManager
import com.example.shribalajikripadham.util.DistanceCalculatorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class AshramRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dbHelper = DatabaseHelper(appContext)

    // --- Ashram Settings & Customization ---
    suspend fun getSettings(): AshramSettings = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ashram_settings WHERE id = 1", null)
        var settings = AshramSettings()
        if (cursor.moveToFirst()) {
            settings = AshramSettings(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                ashramName = cursor.getString(cursor.getColumnIndexOrThrow("ashram_name")),
                gurujiName = cursor.getString(cursor.getColumnIndexOrThrow("guruji_name")),
                address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                allowedRadiusMeters = cursor.getDouble(cursor.getColumnIndexOrThrow("allowed_radius_meters")),
                runningTokenNumber = cursor.getInt(cursor.getColumnIndexOrThrow("running_token_number")),
                isDarbarActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_darbar_active")) == 1,
                darbarDate = cursor.getString(cursor.getColumnIndexOrThrow("darbar_date")),
                darbarTimings = cursor.getString(cursor.getColumnIndexOrThrow("darbar_timings")),
                freeDisclaimer = cursor.getString(cursor.getColumnIndexOrThrow("free_disclaimer")),
                contactPhone = cursor.getString(cursor.getColumnIndexOrThrow("contact_phone")),
                emergencyNoticeText = cursor.getString(cursor.getColumnIndexOrThrow("emergency_notice")),
                isTokenServiceEnabled = cursor.getInt(cursor.getColumnIndexOrThrow("is_token_service_enabled")) == 1,
                isYatraServiceEnabled = cursor.getInt(cursor.getColumnIndexOrThrow("is_yatra_service_enabled")) == 1,
                isLiveCounterVisible = cursor.getInt(cursor.getColumnIndexOrThrow("is_live_counter_visible")) == 1,
                isEventsVisible = cursor.getInt(cursor.getColumnIndexOrThrow("is_events_visible")) == 1,
                isAartiTimingsVisible = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_aarti_timings_visible")) == 1 } catch (e: Exception) { true },
                isGurujiInfoVisible = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_guruji_info_visible")) == 1 } catch (e: Exception) { true },
                isEmergencyNoticeVisible = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_emergency_notice_visible")) == 1 } catch (e: Exception) { true },
                scheduledTokenOpenTimestamp = try { cursor.getLong(cursor.getColumnIndexOrThrow("scheduled_token_open_timestamp")) } catch (e: Exception) { 0L },
                isGeofenceEnforced = cursor.getInt(cursor.getColumnIndexOrThrow("is_geofence_enforced")) == 1,
                latestVersionCode = cursor.getInt(cursor.getColumnIndexOrThrow("latest_version_code")),
                latestVersionName = cursor.getString(cursor.getColumnIndexOrThrow("latest_version_name")),
                updateNotes = cursor.getString(cursor.getColumnIndexOrThrow("update_notes")),
                apkDownloadUrl = cursor.getString(cursor.getColumnIndexOrThrow("apk_download_url")),
                isForceUpdate = cursor.getInt(cursor.getColumnIndexOrThrow("is_force_update")) == 1,
                whatsappGroupUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("whatsapp_group_url")) } catch (e: Exception) { "https://chat.whatsapp.com/invite" },
                whatsappNumber = try { cursor.getString(cursor.getColumnIndexOrThrow("whatsapp_number")) } catch (e: Exception) { "+919876543210" },
                youtubeChannelUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("youtube_channel_url")) } catch (e: Exception) { "https://www.youtube.com/@ShriBalajiKripaDham" },
                facebookPageUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("facebook_page_url")) } catch (e: Exception) { "https://www.facebook.com/ShriBalajiKripaDham" },
                instagramUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("instagram_url")) } catch (e: Exception) { "https://www.instagram.com/shribalajikripadham" },
                appShareUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("app_share_url")) } catch (e: Exception) { "https://shribalajikripadham.org/app" },
                currentThemeId = try { cursor.getString(cursor.getColumnIndexOrThrow("current_theme_id")) } catch (e: Exception) { "maroon" },
                gurujiPhotoUri = try { cursor.getString(cursor.getColumnIndexOrThrow("guruji_photo_uri")) } catch (e: Exception) { "" } ?: "",
                activeUiLayout = try { cursor.getString(cursor.getColumnIndexOrThrow("active_ui_layout")) } catch (e: Exception) { "CLASSIC_DARBAR" } ?: "CLASSIC_DARBAR",
                maxDailyTokens = try { cursor.getInt(cursor.getColumnIndexOrThrow("max_daily_tokens")) } catch (e: Exception) { 0 },
                isUiLayoutEnforced = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_ui_layout_enforced")) == 1 } catch (e: Exception) { false },
                cloudSyncUrl = try { cursor.getString(cursor.getColumnIndexOrThrow("cloud_sync_url")) } catch (e: Exception) { "" } ?: "",
                isCloudSyncEnabled = try { cursor.getInt(cursor.getColumnIndexOrThrow("is_cloud_sync_enabled")) == 1 } catch (e: Exception) { false }
            )
        }
        cursor.close()
        settings
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
        db.update("ashram_settings", cv, "id = 1", null) > 0
    }

    suspend fun updateGurujiPhoto(photoUri: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("guruji_photo_uri", photoUri.trim())
        }
        db.update("ashram_settings", cv, "id = 1", null) > 0
    }

    suspend fun updateActiveUiLayout(layoutKey: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("active_ui_layout", layoutKey.trim())
        }
        db.update("ashram_settings", cv, "id = 1", null) > 0
    }

    suspend fun updateActiveUiLayoutEnforced(layoutKey: String, isEnforced: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("active_ui_layout", layoutKey.trim())
            put("is_ui_layout_enforced", if (isEnforced) 1 else 0)
        }
        db.update("ashram_settings", cv, "id = 1", null) > 0
    }

    suspend fun updateMaxDailyTokens(maxTokens: Int): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("max_daily_tokens", maxTokens)
        }
        db.update("ashram_settings", cv, "id = 1", null) > 0
    }

    suspend fun updateCloudSyncSettings(url: String, isEnabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("cloud_sync_url", url.trim())
            put("is_cloud_sync_enabled", if (isEnabled) 1 else 0)
        }
        db.update("ashram_settings", cv, "id = 1", null) > 0
    }

    suspend fun updateAshramLocation(
        requestingAdmin: Admin,
        newLat: Double,
        newLong: Double,
        newRadius: Double,
        isGeofenceEnforced: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (!requestingAdmin.canChangeLocation && requestingAdmin.role != AdminRole.SUPER_ADMIN) {
            throw SecurityException("Unauthorized: Admin lacks 'can_change_location' permission.")
        }
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("latitude", newLat)
            put("longitude", newLong)
            put("allowed_radius_meters", newRadius)
            put("is_geofence_enforced", if (isGeofenceEnforced) 1 else 0)
        }
        val updated = db.update("ashram_settings", cv, "id = 1", null) > 0

        // Broadcast to cloud (GitHub Live Sync) so all users' apps automatically receive the new coordinates
        if (updated) {
            try {
                val existing = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.fetchLiveConfig()
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val updatedConfig = (existing ?: com.example.shribalajikripadham.data.model.LiveUiConfigDto()).copy(
                    updatedAt = isoFormat.format(Date()),
                    updatedBy = requestingAdmin.name,
                    locationConfig = com.example.shribalajikripadham.data.model.LocationConfigDto(
                        latitude = newLat,
                        longitude = newLong,
                        allowedRadiusMeters = newRadius,
                        isGeofenceEnforced = isGeofenceEnforced,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishLiveConfig(appContext, updatedConfig)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        updated
    }

    suspend fun updateScheduledTokenOpenTime(timestamp: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("scheduled_token_open_timestamp", timestamp)
        }
        db.update("ashram_settings", cv, "id = 1", null) > 0
    }

    suspend fun updateMasterVisibilityToggles(
        isTokenEnabled: Boolean,
        isYatraEnabled: Boolean,
        isLiveCounterVisible: Boolean,
        isEventsVisible: Boolean,
        isAartiTimingsVisible: Boolean,
        isGurujiInfoVisible: Boolean,
        isEmergencyNoticeVisible: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_token_service_enabled", if (isTokenEnabled) 1 else 0)
            put("is_yatra_service_enabled", if (isYatraEnabled) 1 else 0)
            put("is_live_counter_visible", if (isLiveCounterVisible) 1 else 0)
            put("is_events_visible", if (isEventsVisible) 1 else 0)
            put("is_aarti_timings_visible", if (isAartiTimingsVisible) 1 else 0)
            put("is_guruji_info_visible", if (isGurujiInfoVisible) 1 else 0)
            put("is_emergency_notice_visible", if (isEmergencyNoticeVisible) 1 else 0)
        }
        db.update("ashram_settings", cv, "id = 1", null) > 0
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
            put("whatsapp_group_url", whatsappGroupUrl)
            put("whatsapp_number", whatsappNumber)
            put("youtube_channel_url", youtubeUrl)
            put("facebook_page_url", facebookUrl)
            put("instagram_url", instagramUrl)
            put("app_share_url", appShareUrl)
        }
        db.update("ashram_settings", cv, "id = 1", null) > 0
    }

    suspend fun updateCurrentTheme(themeId: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("current_theme_id", themeId)
        }
        db.update("ashram_settings", cv, "id = 1", null) > 0
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
        db.update("ashram_settings", cv, "id = 1", null) > 0
    }

    suspend fun updateRunningTokenNumber(tokenNum: Int): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("running_token_number", tokenNum)
        }
        db.update("ashram_settings", cv, "id = 1", null) > 0
    }

    // --- Tokens & Devices ---
    suspend fun checkDeviceRegisteredToday(deviceId: String): Token? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val today = DatabaseHelper.getTodayDateString()
        val cursor = db.rawQuery(
            "SELECT * FROM tokens WHERE device_id = ? AND darbar_date = ? LIMIT 1",
            arrayOf(deviceId, today)
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
                status = TokenStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
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

        // 🛡️ ANTI-BYPASS: If app was uninstalled & reinstalled, local SQLite is empty!
        // Check persistent hardware receipt stored in public device storage:
        if (token == null) {
            val persistent = com.example.shribalajikripadham.hardware.PersistentTokenReceiptHelper.readPersistentReceipt(deviceId, today)
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
        bypassGeofence: Boolean = false
    ): Token = withContext(Dispatchers.IO) {
        val today = DatabaseHelper.getTodayDateString()
        val db = dbHelper.writableDatabase

        val isSuperAdmin = registeredBy.startsWith("SUPER_ADMIN")
        val isAdminDesk = registeredBy.startsWith("ADMIN") || registeredBy == "SEVADAR_DESK"
        val isDevoteeRequest = !isSuperAdmin && !isAdminDesk

        // Geofence & Anti-Spoof bypass: Super Admin ALWAYS bypasses; Admins bypass IF bypassGeofence is granted
        val shouldBypassGeofence = isSuperAdmin || (isAdminDesk && bypassGeofence)

        // 0. PRE-SCHEDULED TOKEN OPENING CHECK (Devotees only)
        if (isDevoteeRequest) {
            val settings = getSettings()
            if (settings.scheduledTokenOpenTimestamp > System.currentTimeMillis()) {
                throw IllegalStateException("टोकन पंजीकरण अभी शुरू नहीं हुआ है। यह पूर्व निर्धारित समय पर स्वतः खुलेगा।")
            }
        }

        // 1. LOCATION & GEOFENCE CHECKS (Enforced for devotees and non-exempt admins)
        if (!shouldBypassGeofence) {
            val settings = getSettings()
            if (isMockLocation) {
                throw SecurityException("Security Exception: Spoofed Location or Duplicate Device Request Denied.")
            }

            if (locationAccuracy > GeofenceLocationManager.MAX_ALLOWED_ACCURACY_METERS) {
                throw SecurityException("Security Exception: Inaccurate GPS signal (${String.format("%.1f", locationAccuracy)}m). Please stand in open area.")
            }

            if (settings.isGeofenceEnforced) {
                val distance = GeofenceLocationManager.calculateDistanceMeters(
                    latitude, longitude,
                    settings.latitude, settings.longitude
                )
                if (distance > settings.allowedRadiusMeters) {
                    throw SecurityException("Security Exception: Spoofed Location or Duplicate Device Request Denied.")
                }
            }
        }

        // 2. HARDWARE-LEVEL DEVICE LOCKING (Strict 1 Device = 1 Token per Sunday for devotees)
        if (isDevoteeRequest) {
            val checkCursor = db.rawQuery(
                "SELECT token_number FROM device_registrations WHERE device_id = ? AND darbar_date = ?",
                arrayOf(deviceId, today)
            )
            if (checkCursor.moveToFirst()) {
                checkCursor.close()
                throw SecurityException("Security Exception: Spoofed Location or Duplicate Device Request Denied.")
            }
            checkCursor.close()
        }

        val settings = getSettings()
        if (settings.maxDailyTokens > 0) {
            val countCursor = db.rawQuery(
                "SELECT COUNT(*) FROM tokens WHERE darbar_date = ? AND status != 'CANCELLED'",
                arrayOf(today)
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

        val maxTokenCursor = db.rawQuery(
            "SELECT MAX(token_number) FROM tokens WHERE darbar_date = ?",
            arrayOf(today)
        )
        var nextTokenNum = 1
        if (maxTokenCursor.moveToFirst() && !maxTokenCursor.isNull(0)) {
            nextTokenNum = maxTokenCursor.getInt(0) + 1
        }
        maxTokenCursor.close()

        val safeCity = if (city.isBlank()) "डूँगरा जाट (स्थानीय)" else city.trim()
        val safeOrigin = if (originAddress.isNotBlank()) originAddress.trim() else safeCity

        // Automatic Road/Driving Distance Calculation to Shri Balaji Kripa Dham, Dungra Jaat
        val calculatedDistance = if (distanceKm >= 0f) {
            distanceKm
        } else {
            DistanceCalculatorService.resolveDrivingDistance(
                origin = safeOrigin,
                deviceLat = latitude,
                deviceLng = longitude
            ).distanceKm
        }

        val tokenValues = ContentValues().apply {
            put("token_number", nextTokenNum)
            put("darbar_date", today)
            put("patient_name", patientName)
            put("phone_number", phoneNumber)
            put("city", safeCity)
            put("device_id", deviceId)
            put("latitude", latitude)
            put("longitude", longitude)
            put("status", TokenStatus.WAITING.name)
            put("registered_by", registeredBy)
            put("photo_uri", photoUri)
            put("is_darshan_completed", 0)
            put("darshan_completed_at", 0L)
            put("origin_address", safeOrigin)
            put("destination_address", destinationAddress)
            put("distance_km", calculatedDistance)
            put("created_at", System.currentTimeMillis())
        }

        var insertedId: Long = -1
        db.beginTransaction()
        try {
            insertedId = db.insertOrThrow("tokens", null, tokenValues)

            if (isDevoteeRequest) {
                val devValues = ContentValues().apply {
                    put("device_id", deviceId)
                    put("darbar_date", today)
                    put("token_number", nextTokenNum)
                    put("patient_name", patientName)
                    put("created_at", System.currentTimeMillis())
                }
                db.insertOrThrow("device_registrations", null, devValues)
            }
            db.setTransactionSuccessful()
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            throw SecurityException("Security Exception: Spoofed Location or Duplicate Device Request Denied.")
        } finally {
            db.endTransaction()
        }

        val createdToken = Token(
            id = insertedId,
            tokenNumber = nextTokenNum,
            darbarDate = today,
            patientName = patientName,
            phoneNumber = phoneNumber,
            city = safeCity,
            deviceId = deviceId,
            latitude = latitude,
            longitude = longitude,
            status = TokenStatus.WAITING,
            registeredBy = registeredBy,
            photoUri = photoUri,
            isDarshanCompleted = false,
            darshanCompletedAt = 0L,
            originAddress = safeOrigin,
            destinationAddress = destinationAddress,
            distanceKm = calculatedDistance,
            createdAt = System.currentTimeMillis()
        )

        // 📊 Universal Real-Time Google Sheets Sync for ALL tokens (Devotees + Admin + Sevadar)
        try {
            com.example.shribalajikripadham.data.network.GoogleSheetTokenSyncManager.postTokenToSheet(appContext, createdToken)
        } catch (e: Exception) {}

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

        // 🛡️ ANTI-BYPASS: Save hardware-bound persistent receipt into public device storage
        try {
            com.example.shribalajikripadham.hardware.PersistentTokenReceiptHelper.savePersistentReceipt(createdToken)
        } catch (e: Exception) {}

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
                    put("latitude", 28.4089)
                    put("longitude", 77.8789)
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

                val insertedId = db.insertOrThrow("tokens", null, cv)

                val token = Token(
                    id = insertedId,
                    tokenNumber = nextTokenNum,
                    darbarDate = today,
                    patientName = safeName,
                    phoneNumber = safePhone,
                    city = safeCity,
                    deviceId = "REGISTER_SCAN",
                    latitude = 28.4089,
                    longitude = 77.8789,
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
                    status = TokenStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
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

    suspend fun toggleDarshanCompleted(tokenId: Long, completed: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_darshan_completed", if (completed) 1 else 0)
            put("darshan_completed_at", if (completed) System.currentTimeMillis() else 0L)
            if (completed) {
                put("status", TokenStatus.COMPLETED.name)
            } else {
                put("status", TokenStatus.WAITING.name)
            }
        }
        db.update("tokens", cv, "id = ?", arrayOf(tokenId.toString())) > 0
    }

    suspend fun updateTokenStatus(tokenId: Long, status: TokenStatus): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("status", status.name)
        }
        db.update("tokens", cv, "id = ?", arrayOf(tokenId.toString())) > 0
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
                    phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone_number")) ?: "",
                    boardingPoint = cursor.getString(cursor.getColumnIndexOrThrow("boarding_point")) ?: "",
                    paymentStatus = PaymentStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("payment_status"))),
                    paymentMode = cursor.getString(cursor.getColumnIndexOrThrow("payment_mode")) ?: "CASH",
                    fareAmount = cursor.getInt(cursor.getColumnIndexOrThrow("fare_amount")),
                    yatraDate = cursor.getString(cursor.getColumnIndexOrThrow("yatra_date")) ?: "",
                    notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")) ?: ""
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
        boardingPoint: String = "Ashram",
        fareAmount: Int = 1500,
        notes: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_booked", if (isBooked) 1 else 0)
            put("passenger_name", if (isBooked) passengerName else "")
            put("phone_number", if (isBooked) phoneNumber else "")
            put("boarding_point", if (isBooked) boardingPoint else "")
            put("payment_status", paymentStatus.name)
            put("payment_mode", paymentMode)
            put("fare_amount", fareAmount)
            put("notes", notes)
        }
        db.update("bus_seats", cv, "seat_number = ?", arrayOf(seatNumber.toString())) > 0
    }

    suspend fun bookBusSeat(
        seatNumber: Int,
        passengerName: String,
        phoneNumber: String,
        boardingPoint: String,
        paymentStatus: PaymentStatus,
        paymentMode: String,
        fareAmount: Int,
        notes: String
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_booked", 1)
            put("passenger_name", passengerName)
            put("phone_number", phoneNumber)
            put("boarding_point", boardingPoint)
            put("payment_status", paymentStatus.name)
            put("payment_mode", paymentMode)
            put("fare_amount", fareAmount)
            put("notes", notes)
        }
        db.update("bus_seats", cv, "seat_number = ?", arrayOf(seatNumber.toString())) > 0
    }

    suspend fun cancelBusSeatBooking(seatNumber: Int): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("is_booked", 0)
            put("passenger_name", "")
            put("phone_number", "")
            put("boarding_point", "")
            put("payment_status", PaymentStatus.UNPAID.name)
            put("payment_mode", "CASH")
            put("notes", "")
        }
        db.update("bus_seats", cv, "seat_number = ?", arrayOf(seatNumber.toString())) > 0
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
        db.insert("yatra_expenses", null, cv) > 0
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
        val hashed = DatabaseHelper.hashPin(pin)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM admins WHERE pin_hash = ? AND is_active = 1 LIMIT 1", arrayOf(hashed))
        var admin: Admin? = null
        if (cursor.moveToFirst()) {
            admin = parseAdminCursor(cursor)
        }
        cursor.close()
        admin
    }

    suspend fun authenticateAdminByCredentials(username: String, password: String): Admin? = withContext(Dispatchers.IO) {
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()
        val db = dbHelper.readableDatabase

        val passHash = DatabaseHelper.hashPassword(trimmedPass)
        val cursor = db.rawQuery(
            "SELECT * FROM admins WHERE LOWER(username) = LOWER(?) AND password_hash = ? AND is_active = 1 LIMIT 1",
            arrayOf(trimmedUser, passHash)
        )
        var admin: Admin? = null
        if (cursor.moveToFirst()) {
            admin = parseAdminCursor(cursor)
        }
        cursor.close()
        admin
    }

    suspend fun authenticateSuperAdminByPasswordOnly(password: String): Admin? = withContext(Dispatchers.IO) {
        val trimmedPass = password.trim()
        val db = dbHelper.readableDatabase
        val passHash = DatabaseHelper.hashPassword(trimmedPass)
        val cursor = db.rawQuery(
            "SELECT * FROM admins WHERE role = 'SUPER_ADMIN' AND password_hash = ? AND is_active = 1 LIMIT 1",
            arrayOf(passHash)
        )
        var admin: Admin? = null
        if (cursor.moveToFirst()) {
            admin = parseAdminCursor(cursor)
        }
        cursor.close()
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
        photoUri: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", name)
            put("username", username.trim())
            put("phone", phone)
            put("role", role.name)
            put("pin_hash", DatabaseHelper.hashPin(if (pin.isNotEmpty()) pin else "1234"))
            put("password_hash", DatabaseHelper.hashPassword(password))
            put("can_manage_tokens", if (canManageTokens) 1 else 0)
            put("can_issue_manual_tokens", if (canIssueManualTokens) 1 else 0)
            put("can_manage_yatra", if (canManageYatra) 1 else 0)
            put("can_manage_expenses", if (canManageExpenses) 1 else 0)
            put("can_change_location", if (canChangeLocation) 1 else 0)
            put("can_send_notifications", if (canSendNotifications) 1 else 0)
            put("can_edit_ashram_info", if (canEditAshramInfo) 1 else 0)
            put("can_manage_admins", if (role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_view_devotee_photos", if (canViewDevoteePhotos || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_issue_tokens_anywhere", if (canIssueTokensAnywhere || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_scan_paper_register", if (canScanPaperRegister || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("can_manage_parchas", if (canManageParchas || role == AdminRole.SUPER_ADMIN) 1 else 0)
            put("photo_uri", photoUri.trim())
            put("is_active", 1)
            put("created_at", System.currentTimeMillis())
        }
        db.insert("admins", null, cv) > 0
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
            put("is_active", if (isActive) 1 else 0)
        }
        db.update("admins", cv, "id = ?", arrayOf(adminId.toString())) > 0
    }

    suspend fun updateAdminParchaPermission(adminId: Long, canManageParchas: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("can_manage_parchas", if (canManageParchas) 1 else 0)
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
        db.delete("admins", "id = ?", arrayOf(adminId.toString())) > 0
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
        val cv = ContentValues().apply {
            put("patient_name", name)
            put("phone_number", phone)
            put("city", safeCity)
            put("face_vector", FaceEmbeddingEngine.vectorToBlob(normalized))
            put("photo_uri", photoUri)
            put("visit_count", 1)
            put("last_confidence", 1.0f)
            put("last_verified_at", System.currentTimeMillis())
            put("created_at", System.currentTimeMillis())
        }
        db.insert("devotee_face_profiles", null, cv)
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

            val cv = ContentValues().apply {
                put("face_vector", enrichedBlob)
                put("visit_count", currentVisitCount + 1)
                put("last_confidence", confidence)
                put("last_verified_at", System.currentTimeMillis())
                if (newPhotoUri.isNotBlank()) {
                    put("photo_uri", newPhotoUri)
                }
            }
            db.update("devotee_face_profiles", cv, "id = ?", arrayOf(profileId.toString())) > 0
        } else {
            cursor.close()
            false
        }
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
        locationAccuracy: Float = 10.0f
    ): Pair<Token, Boolean> = withContext(Dispatchers.IO) {
        // Register token with Server-Side Geofence, Mock Location, and Hardware Device Locking
        val token = registerToken(
            patientName = matchedProfile.patientName,
            phoneNumber = matchedProfile.phoneNumber,
            city = matchedProfile.city,
            deviceId = deviceId,
            latitude = latitude,
            longitude = longitude,
            registeredBy = "FACIAL_SCAN",
            photoUri = if (photoUri.isNotBlank()) photoUri else matchedProfile.photoUri,
            isMockLocation = isMockLocation,
            locationAccuracy = locationAccuracy
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

    // --- Token Cancellation & Permanent Deletion ---
    suspend fun cancelToken(tokenId: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("status", TokenStatus.CANCELLED.name)
            put("is_darshan_completed", 0)
        }
        db.update("tokens", cv, "id = ?", arrayOf(tokenId.toString())) > 0
    }

    suspend fun deleteToken(tokenId: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("tokens", "id = ?", arrayOf(tokenId.toString())) > 0
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
        db.update("admins", cv, "role = 'SUPER_ADMIN'", null) > 0
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
                        put("latitude", t.optDouble("latitude", 28.4089))
                        put("longitude", t.optDouble("longitude", 77.8789))
                        put("status", t.optString("status", "WAITING"))
                        put("registered_by", t.optString("registered_by", "RESTORE"))
                        put("is_darshan_completed", t.optInt("is_darshan_completed", 0))
                        put("created_at", t.optLong("created_at", System.currentTimeMillis()))
                    }
                    db.insert("tokens", null, cv)
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

        try {
            val backupJson = exportFullDatabaseBackupJson()
            val url = URL(trimmed)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.1")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
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

                while (c.moveToNext()) {
                    list.add(
                        UiSectionConfig(
                            sectionId = c.getString(idIdx),
                            titleHindi = c.getString(titleHiIdx),
                            titleEnglish = c.getString(titleEnIdx),
                            icon = c.getString(iconIdx),
                            isVisible = c.getInt(visibleIdx) == 1,
                            orderIndex = c.getInt(orderIdx)
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
        try {
            configs.forEachIndexed { index, item ->
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
            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            false
        } finally {
            db.endTransaction()
        }
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
        val remoteConfig = com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.fetchLiveConfig()
        if (remoteConfig != null) {
            if (remoteConfig.sections.isNotEmpty()) {
                saveUiSectionConfigs(remoteConfig.sections)
            }
            // Automatically synchronize coordinates from cloud to local SQLite
            val loc = remoteConfig.locationConfig
            if (loc.latitude != 0.0 && loc.longitude != 0.0) {
                try {
                    val db = dbHelper.writableDatabase
                    val cv = ContentValues().apply {
                        put("latitude", loc.latitude)
                        put("longitude", loc.longitude)
                        put("allowed_radius_meters", loc.allowedRadiusMeters)
                        put("is_geofence_enforced", if (loc.isGeofenceEnforced) 1 else 0)
                    }
                    db.update("ashram_settings", cv, "id = 1", null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Pair(true, remoteConfig)
        } else {
            Pair(false, null)
        }
    }

    suspend fun publishLiveConfigToGitHub(
        sections: List<UiSectionConfig>,
        adminName: String = "Super Admin"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val config = LiveUiConfigDto(
            updatedAt = isoFormat.format(Date()),
            updatedBy = adminName,
            version = 1,
            sections = sections
        )
        // Also save locally
        saveUiSectionConfigs(sections)
        // Publish to GitHub
        com.example.shribalajikripadham.data.network.GitHubLiveSyncManager.publishLiveConfig(appContext, config)
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

    // --- Devotee Registry & Cross-Device Auto-Fill ---

    suspend fun searchDevoteeByPhone(phone: String): DevoteeFaceProfile? = withContext(Dispatchers.IO) {
        val cleanPhone = phone.trim().replace("+91", "").replace(" ", "").replace("-", "")
        if (cleanPhone.length < 10) return@withContext null
        val db = dbHelper.readableDatabase
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
        profile
    }

    suspend fun searchDevoteesByName(query: String, limit: Int = 6): List<DevoteeFaceProfile> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM devotee_face_profiles WHERE patient_name LIKE ? ORDER BY last_verified_at DESC LIMIT ?",
            arrayOf("%$trimmed%", limit.toString())
        )
        val list = mutableListOf<DevoteeFaceProfile>()
        while (cursor.moveToNext()) {
            val blob = cursor.getBlob(cursor.getColumnIndexOrThrow("face_vector"))
            val vector = FaceEmbeddingEngine.blobToVector(blob)
            list.add(
                DevoteeFaceProfile(
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
            )
        }
        cursor.close()
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

        // Background Cloud Upload to Google Sheets Universal Devotee Registry
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
        com.example.shribalajikripadham.data.network.CentralDevoteeSyncManager.syncAllDevoteesFromCloud(appContext, dbHelper)
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
        try {
            val db = dbHelper.writableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM sacred_parchas", null)
            var count = 0
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()

            if (count == 0) {
                val canonicals = com.example.shribalajikripadham.ai.SacredParchaEngine.getCanonicalParchas()
                for (p in canonicals) {
                    upsertParcha(p)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllPublicParchas(): List<com.example.shribalajikripadham.data.model.SacredParcha> {
        try {
            seedDefaultParchasIfEmpty()
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
            if (list.isNotEmpty()) return list
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Fallback: If DB query fails for any reason, return canonical parchas!
        return com.example.shribalajikripadham.ai.SacredParchaEngine.getCanonicalParchas().filter { it.isPublished && !it.isHidden }
    }

    fun getAllAdminParchas(): List<com.example.shribalajikripadham.data.model.SacredParcha> {
        try {
            seedDefaultParchasIfEmpty()
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
            if (list.isNotEmpty()) return list
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Fallback: Return all canonical parchas
        return com.example.shribalajikripadham.ai.SacredParchaEngine.getCanonicalParchas()
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
}

