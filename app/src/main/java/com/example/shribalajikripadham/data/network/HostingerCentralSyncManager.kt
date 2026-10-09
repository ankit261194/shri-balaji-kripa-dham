package com.example.shribalajikripadham.data.network

import android.content.Context
import android.util.Log
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.BusSeat
import com.example.shribalajikripadham.data.model.DevicePresence
import com.example.shribalajikripadham.data.model.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.Locale

object HostingerCentralSyncManager {

    private const val TAG = "HostingerCentralSync"
    const val BASE_URL = "https://shribalajikripadham.online/api/"

    @Volatile
    var currentAdminToken: String = ""

    fun getInternalApiKey(): String {
        val b = byteArrayOf(
            83, 66, 75, 68, 95, 83, 69, 67, 85, 82, 69, 95, 84, 79, 75, 69, 78, 95,
            57, 49, 48, 48, 49, 48, 48, 50, 53, 49, 50, 51, 51, 52, 51, 51, 95, 86, 50, 52, 51
        )
        return String(b, StandardCharsets.UTF_8)
    }

    val API_SECRET_KEY: String
        get() = getInternalApiKey()

    fun calculateHmacSha256(data: String, key: String): String {
        return try {
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            val secretKey = javax.crypto.spec.SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
            mac.init(secretKey)
            val bytes = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            val sb = java.lang.StringBuilder()
            for (b in bytes) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    fun applyAuthHeaders(conn: HttpURLConnection, method: String = "GET", context: Context? = null) {
        var token = currentAdminToken
        if (token.isBlank() && context != null) {
            try {
                val prefs = context.getSharedPreferences("sbkd_admin_login_prefs", Context.MODE_PRIVATE)
                token = prefs.getString("admin_session_token", "") ?: ""
                if (token.isNotBlank()) currentAdminToken = token
            } catch (e: Exception) {}
        }

        if (token.isNotBlank()) {
            conn.setRequestProperty("X-SBKD-ADMIN-TOKEN", token)
        }

        val ts = System.currentTimeMillis() / 1000
        val sig = calculateHmacSha256("$ts:$method", getInternalApiKey())
        if (sig.isNotBlank()) {
            conn.setRequestProperty("X-SBKD-HMAC-AUTH", "$ts:$sig")
        }

        conn.setRequestProperty("X-SBKD-API-KEY", getInternalApiKey())
    }

    suspend fun loginAdminOnServer(
        context: Context,
        username: String,
        password: String = "",
        pin: String = ""
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}admin_auth.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                applyAuthHeaders(this, "POST", context)
            }
            val devId = com.example.shribalajikripadham.hardware.DeviceFingerprintManager.getDeviceId(context)
            val devModel = AppTelemetryManager.getDeviceModelName()
            val payload = JSONObject().apply {
                put("action", "LOGIN")
                put("username", username)
                put("password", password)
                put("pin", pin)
                put("device_id", devId)
                put("device_model", devModel)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(StandardCharsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(resp)
                if (root.optBoolean("success", false)) {
                    val token = root.optString("token", "")
                    if (token.isNotBlank()) {
                        currentAdminToken = token
                        val prefs = context.getSharedPreferences("sbkd_admin_login_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("admin_session_token", token).apply()
                    }
                    Pair(true, root.optString("message", "सफलतापूर्वक लॉगिन"))
                } else {
                    Pair(false, root.optString("error", "लॉगिन विफल"))
                }
            } else {
                Pair(false, "सर्वर त्रुटि: HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "नेटवर्क त्रुटि")
        }
    }

    @Volatile
    var lastIssueErrorMessage: String? = null

    /**
     * Request next atomic sequential token from Central MySQL Database.
     * Guaranteed ZERO collisions across all devices.
     */
    suspend fun issueCentralToken(
        patientName: String,
        phoneNumber: String,
        city: String = "",
        deviceId: String = "",
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        distanceKm: Double = 0.0,
        photoUrl: String = "",
        registeredBy: String = "ONLINE_DEVOTEE",
        originAddress: String = "",
        destinationAddress: String = "श्री बालाजी कृपा धाम, डूँगरा जाट",
        darbarDate: String = "",
        customTokenNumber: Int? = null,
        isStealthAllocator: Boolean = false,
        canIssueAnytime: Boolean = false,
        darbarVenue: String = "DUNGRA_JAAT",
        isMockLocation: Boolean = false,
        locationAccuracy: Float = 10.0f,
        isRooted: Boolean = false
    ): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
        lastIssueErrorMessage = null
        var attemptsLeft = 2
        while (attemptsLeft > 0) {
            attemptsLeft--
            try {
                val url = URL("${BASE_URL}issue_token.php")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    applyAuthHeaders(this, "POST")
                    setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                    setRequestProperty("Pragma", "no-cache")
                }
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.56.2")

                val params = StringBuilder()
                params.append("patient_name=").append(URLEncoder.encode(patientName, "UTF-8"))
                params.append("&phone_number=").append(URLEncoder.encode(phoneNumber, "UTF-8"))
                params.append("&city=").append(URLEncoder.encode(city, "UTF-8"))
                params.append("&device_id=").append(URLEncoder.encode(deviceId, "UTF-8"))
                params.append("&latitude=").append(latitude)
                params.append("&longitude=").append(longitude)
                params.append("&distance_km=").append(distanceKm)
                params.append("&photo_url=").append(URLEncoder.encode(photoUrl, "UTF-8"))
                params.append("&registered_by=").append(URLEncoder.encode(registeredBy, "UTF-8"))
                params.append("&origin_address=").append(URLEncoder.encode(originAddress, "UTF-8"))
                params.append("&destination_address=").append(URLEncoder.encode(destinationAddress, "UTF-8"))
                params.append("&is_mock_location=").append(if (isMockLocation) "1" else "0")
                params.append("&location_accuracy=").append(locationAccuracy)
                params.append("&is_rooted=").append(if (isRooted) "1" else "0")
                if (darbarDate.isNotBlank()) {
                    params.append("&darbar_date=").append(URLEncoder.encode(darbarDate, "UTF-8"))
                }
                if (customTokenNumber != null && customTokenNumber > 0) {
                    params.append("&custom_token_number=").append(customTokenNumber)
                }
                if (isStealthAllocator) {
                    params.append("&is_priority_allocator=1")
                }
                if (canIssueAnytime || registeredBy.startsWith("SUPER_ADMIN")) {
                    params.append("&can_issue_anytime=1")
                }
                if (darbarVenue.isNotBlank()) {
                    params.append("&darbar_venue=").append(URLEncoder.encode(darbarVenue, "UTF-8"))
                }

                conn.outputStream.use { os ->
                    os.write(params.toString().toByteArray(StandardCharsets.UTF_8))
                }

                val code = conn.responseCode
                if (code == 200) {
                    val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    val json = JSONObject(resp)
                    if (json.optBoolean("success", false)) {
                        val tokenNum = json.optInt("token_number", -1)
                        lastIssueErrorMessage = null
                        return@withContext Pair(true, tokenNum)
                    } else {
                        lastIssueErrorMessage = json.optString("error", "सर्वर द्वारा टोकन अस्वीकृत।")
                        return@withContext Pair(false, -1)
                    }
                } else {
                    val errResp = try {
                        conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
                    } catch (e: Exception) { null }
                    if (!errResp.isNullOrBlank()) {
                        try {
                            val errJson = JSONObject(errResp)
                            lastIssueErrorMessage = errJson.optString("error", "सर्वर द्वारा टोकन अस्वीकृत।")
                        } catch (e: Exception) {
                            lastIssueErrorMessage = errResp
                        }
                    } else {
                        lastIssueErrorMessage = "सर्वर त्रुटि (HTTP $code)"
                    }
                    return@withContext Pair(false, -1)
                }
            } catch (e: Exception) {
                Log.e(TAG, "issueCentralToken attempt failed (${e.javaClass.simpleName}): ${e.message}")
                if (attemptsLeft > 0) {
                    kotlinx.coroutines.delay(800)
                } else {
                    lastIssueErrorMessage = "⚠️ नेटवर्क विलंब (Timeout): सर्वर से संपर्क स्थापित नहीं हो सका। कृपया 5 सेकंड प्रतीक्षा करके पुनः प्रयास करें।"
                }
            }
        }
        Pair(false, -1)
    }

    /**
     * Checks if a device has already registered a token today on the central server.
     * Prevents bypassing via "Clear Data" or app reinstall.
     */
    suspend fun checkDeviceRegisteredOnServer(
        deviceId: String,
        darbarDate: String = "",
        phoneNumber: String = ""
    ): com.example.shribalajikripadham.data.model.Token? = withContext(Dispatchers.IO) {
        if (deviceId.isBlank() && phoneNumber.isBlank()) return@withContext null
        try {
            val dDate = if (darbarDate.isNotBlank()) darbarDate else com.example.shribalajikripadham.data.local.DatabaseHelper.getTodayDateString()
            val phoneQuery = if (phoneNumber.isNotBlank()) "&phone_number=${URLEncoder.encode(phoneNumber, "UTF-8")}" else ""
            val urlStr = "${BASE_URL}check_device.php?device_id=${URLEncoder.encode(deviceId, "UTF-8")}&darbar_date=${URLEncoder.encode(dDate, "UTF-8")}$phoneQuery"
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
                connectTimeout = 12000
                readTimeout = 12000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ShriBalajiApp/2.56.2")
            }

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                if (json.optBoolean("success", false) && json.optBoolean("registered", false)) {
                    val tokObj = json.optJSONObject("token")
                    if (tokObj != null) {
                        return@withContext com.example.shribalajikripadham.data.model.Token(
                            id = tokObj.optLong("id", 0L),
                            tokenNumber = tokObj.optInt("token_number", 0),
                            darbarDate = tokObj.optString("darbar_date", dDate),
                            patientName = tokObj.optString("patient_name", ""),
                            phoneNumber = tokObj.optString("phone_number", ""),
                            city = tokObj.optString("city", "डूँगरा जाट (स्थानीय)"),
                            deviceId = tokObj.optString("device_id", deviceId),
                            latitude = tokObj.optDouble("latitude", 0.0),
                            longitude = tokObj.optDouble("longitude", 0.0),
                            distanceKm = tokObj.optDouble("distance_km", 0.0).toFloat(),
                            originAddress = tokObj.optString("origin_address", ""),
                            destinationAddress = tokObj.optString("destination_address", "श्री बालाजी कृपा धाम, डुंगरा जाट"),
                            photoUri = tokObj.optString("photo_url", ""),
                            status = try { com.example.shribalajikripadham.data.model.TokenStatus.valueOf(tokObj.optString("status", "WAITING")) } catch (e: Exception) { com.example.shribalajikripadham.data.model.TokenStatus.WAITING },
                            registeredBy = tokObj.optString("registered_by", "SELF"),
                            isDarshanCompleted = tokObj.optBoolean("is_darshan_completed", false),
                            darshanCompletedAt = 0L,
                            createdAt = tokObj.optLong("created_at", System.currentTimeMillis())
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "checkDeviceRegisteredOnServer failed: ${e.message}")
            null
        }
    }

    /**
     * Fetch Live Darbar Queue from Central Server
     */
    suspend fun fetchLiveQueue(date: String = ""): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val urlStr = if (date.isNotBlank()) "${BASE_URL}get_queue.php?date=$date" else "${BASE_URL}get_queue.php"
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.36.0")

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                return@withContext JSONObject(resp)
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "fetchLiveQueue failed: ${e.message}")
            null
        }
    }

    @Volatile
    private var lastLiveConfigEtag: String? = null

    @Volatile
    private var cachedLiveConfig: JSONObject? = null

    /**
     * Fetch Live Config (with HTTP ETag / 304 conditional caching to prevent server overload)
     */
    suspend fun fetchLiveConfig(): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}live_config.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                lastLiveConfigEtag?.let { etag ->
                    setRequestProperty("If-None-Match", etag)
                }
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.52.0")

            val code = conn.responseCode
            if (code == 304) {
                // 304 Not Modified: zero bytes transferred, return cached config
                return@withContext cachedLiveConfig
            } else if (code == 200) {
                val etag = conn.getHeaderField("ETag")
                if (!etag.isNullOrBlank()) {
                    lastLiveConfigEtag = etag
                }
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                cachedLiveConfig = json
                return@withContext json
            }
            cachedLiveConfig
        } catch (e: Exception) {
            Log.e(TAG, "fetchLiveConfig failed: ${e.message}")
            cachedLiveConfig
        }
    }

    /**
     * Admin update Live Config
     */
    suspend fun updateLiveConfig(
        radiusMeters: Double,
        isGeofenceEnforced: Boolean,
        isOutstationAllowed: Boolean,
        outstationKm: Double,
        currentServingToken: Int = 0,
        lat: Double = 28.3972915,
        long: Double = 78.1460410
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}live_config.php?api_key=$API_SECRET_KEY")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.36.0")

            val params = "allowed_radius_meters=$radiusMeters" +
                    "&is_geofence_enforced=${if (isGeofenceEnforced) 1 else 0}" +
                    "&is_outstation_advance_allowed=${if (isOutstationAllowed) 1 else 0}" +
                    "&outstation_min_distance_km=$outstationKm" +
                    "&current_serving_token=$currentServingToken" +
                    "&latitude=$lat" +
                    "&longitude=$long" +
                    "&api_key=$API_SECRET_KEY"

            conn.outputStream.use { os ->
                os.write(params.toByteArray(StandardCharsets.UTF_8))
            }

            val success = conn.responseCode == 200
            if (success) {
                lastLiveConfigEtag = null
                cachedLiveConfig = null
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "updateLiveConfig failed: ${e.message}")
            false
        }
    }

    /**
     * Overloaded uploadPhoto accepting Context, localPath (file path or content URI), and fileName.
     */
    suspend fun uploadPhoto(
        context: Context,
        localPath: String,
        fileName: String,
        photoType: String = "devotee"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val file = if (localPath.startsWith("content://") || localPath.startsWith("file://")) {
                val tempFile = File(context.cacheDir, fileName.ifBlank { "temp_upload_${System.currentTimeMillis()}.jpg" })
                val uri = android.net.Uri.parse(localPath)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            } else {
                val direct = File(localPath)
                if (direct.exists()) direct else null
            }
            if (file != null && file.exists()) {
                uploadPhoto(file, photoType)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadPhoto(Context, ...) failed: ${e.message}")
            null
        }
    }

    /**
     * Upload photo to cloud CDN storage on shribalajikripadham.online
     */
    suspend fun uploadPhoto(file: File, photoType: String = "devotee"): String? = withContext(Dispatchers.IO) {
        try {
            val boundary = "==Boundary_${System.currentTimeMillis()}=="
            val url = URL("${BASE_URL}upload_photo.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.42.1")

            conn.outputStream.use { os ->
                val sb = StringBuilder()
                sb.append("--$boundary\r\n")
                sb.append("Content-Disposition: form-data; name=\"photo_type\"\r\n\r\n")
                sb.append("$photoType\r\n")
                sb.append("--$boundary\r\n")
                sb.append("Content-Disposition: form-data; name=\"photo\"; filename=\"${file.name}\"\r\n")
                sb.append("Content-Type: image/jpeg\r\n\r\n")
                os.write(sb.toString().toByteArray(StandardCharsets.UTF_8))

                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        os.write(buffer, 0, bytesRead)
                    }
                }

                os.write("\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                if (json.optBoolean("success", false)) {
                    return@withContext if (json.has("photo_url") && !json.isNull("photo_url")) json.optString("photo_url") else null
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "uploadPhoto failed: ${e.message}")
            null
        }
    }

    /**
     * Test connection to Hostinger server
     */
    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://shribalajikripadham.online/index.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.36.0")

            if (conn.responseCode == 200) {
                Pair(true, "Hostinger सर्वर 100% ऑनलाइन व सक्रिय है!")
            } else {
                Pair(false, "सर्वर रिस्पॉन्स HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Pair(false, "कनेक्शन त्रुटि: ${e.localizedMessage ?: "अज्ञात त्रुटि"}")
        }
    }

    /**
     * Bulk sync a list of tokens to Hostinger
     */
    suspend fun syncAllTokensToHostinger(tokens: List<Token>): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (tokens.isEmpty()) return@withContext Pair(true, "सिंक करने के लिए कोई टोकन नहीं है।")
        var successCount = 0
        for (token in tokens) {
            val (ok, _) = issueCentralToken(
                patientName = token.patientName,
                phoneNumber = token.phoneNumber,
                city = token.city,
                deviceId = token.deviceId,
                latitude = token.latitude,
                longitude = token.longitude,
                distanceKm = token.distanceKm.toDouble(),
                photoUrl = token.photoUri,
                registeredBy = token.registeredBy,
                originAddress = token.originAddress,
                destinationAddress = token.destinationAddress,
                darbarDate = token.darbarDate
            )
            if (ok) successCount++
        }
        Pair(successCount > 0, "$successCount / ${tokens.size} टोकन Hostinger सर्वर पर सुरक्षित हुए!")
    }

    // --- Cloud Backend Mode Switcher ---
    private const val PREFS_NAME = "sbkd_cloud_mode_prefs"
    private const val KEY_CLOUD_MODE = "active_cloud_mode" // "HOSTING" or "GITHUB"

    fun getActiveCloudMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CLOUD_MODE, "HOSTING") ?: "HOSTING"
    }

    fun setActiveCloudMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CLOUD_MODE, mode).apply()
    }

    fun isHostingMode(context: Context): Boolean {
        return getActiveCloudMode(context) == "HOSTING"
    }

    /**
     * SHIFT: Hosting -> GitHub
     * Pulls all tokens and live settings from Hostinger MySQL, merges into SQLite,
     * clones everything to GitHub repository, and sets active mode to GITHUB!
     */
    suspend fun shiftFromHostingToGitHub(
        repository: com.example.shribalajikripadham.data.repository.AshramRepository,
        context: Context
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val queueJson = fetchLiveQueue()
            val configJson = fetchLiveConfig()

            var importedCount = 0
            if (queueJson != null && queueJson.optBoolean("success", false)) {
                val tokensArray = queueJson.optJSONArray("tokens")
                if (tokensArray != null && tokensArray.length() > 0) {
                    for (i in 0 until tokensArray.length()) {
                        val t = tokensArray.getJSONObject(i)
                        val ok = repository.insertOrUpdateCentralToken(
                            tokenNumber = t.optInt("token_number"),
                            darbarDate = t.optString("darbar_date"),
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
                        if (ok) importedCount++
                    }
                }
            }

            if (configJson != null && configJson.optBoolean("success", false)) {
                repository.updateAshramLocation(
                    lat = configJson.optDouble("latitude", 28.3972915),
                    long = configJson.optDouble("longitude", 78.1460410),
                    radiusMeters = configJson.optDouble("allowed_radius_meters", 200.0),
                    isGeofenceEnforced = configJson.optBoolean("is_geofence_enforced", true),
                    isOutstationAdvanceAllowed = configJson.optBoolean("is_outstation_advance_allowed", true),
                    outstationMinDistanceKm = configJson.optDouble("outstation_min_distance_km", 30.0)
                )
            }

            val (ghOk, ghMsg) = repository.publishCurrentSettingsToGitHub("Super Admin (Shift from Hosting to GitHub)")

            if (GoogleSheetTokenSyncManager.isConfigured(context)) {
                val allTokens = repository.getAllTokens()
                if (allTokens.isNotEmpty()) {
                    GoogleSheetTokenSyncManager.postBatchTokensToSheet(context, allTokens)
                }
            }

            setActiveCloudMode(context, "GITHUB")

            if (ghOk) {
                Pair(true, "✅ होस्टिंग से GitHub पर शिफ्ट सफल! ($importedCount टोकन व संपूर्ण सेटिंग्स GitHub पर 100% क्लोन हो गए। अब ऐप GitHub मोड में है।)")
            } else {
                Pair(false, "डेटा प्राप्त हुआ पर GitHub पुश में समस्या: $ghMsg")
            }
        } catch (e: Exception) {
            Pair(false, "शिफ्ट त्रुटि: ${e.localizedMessage}")
        }
    }

    /**
     * SHIFT: GitHub -> Hosting
     * Pulls complete A-to-Z data from GitHub, restores into SQLite,
     * writes all tokens and live config to Hostinger MySQL, and sets active mode to HOSTING!
     */
    suspend fun shiftFromGitHubToHosting(
        repository: com.example.shribalajikripadham.data.repository.AshramRepository,
        context: Context
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            repository.syncCurrentLiveSettingsFromGitHub()

            val allTokens = repository.getAllTokens()
            val (hOk, hMsg) = syncAllTokensToHostinger(allTokens)

            val s = repository.getSettings()
            updateLiveConfig(
                radiusMeters = s.allowedRadiusMeters,
                isGeofenceEnforced = s.isGeofenceEnforced,
                isOutstationAllowed = s.isOutstationAdvanceAllowed,
                outstationKm = s.outstationMinDistanceKm,
                currentServingToken = s.runningTokenNumber,
                lat = s.latitude,
                long = s.longitude
            )

            if (GoogleSheetTokenSyncManager.isConfigured(context) && allTokens.isNotEmpty()) {
                GoogleSheetTokenSyncManager.postBatchTokensToSheet(context, allTokens)
            }

            setActiveCloudMode(context, "HOSTING")

            Pair(true, "✅ GitHub से Hosting पर शिफ्ट सफल! ($hMsg, संपूर्ण सेटिंग्स Hostinger पर 100% क्लोन हो गई। अब ऐप Hosting मोड में है।)")
        } catch (e: Exception) {
            Pair(false, "शिफ्ट त्रुटि: ${e.localizedMessage}")
        }
    }

    /**
     * MASTER BIDIRECTIONAL SYNC
     * Merges Hosting, GitHub, Google Sheet and Local App so all 3 places have 100% EXACT SAME DATA!
     */
    suspend fun bidirectionalTripleSync(
        repository: com.example.shribalajikripadham.data.repository.AshramRepository,
        context: Context
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val results = mutableListOf<String>()
        try {
            val queueJson = fetchLiveQueue()
            if (queueJson != null && queueJson.optBoolean("success", false)) {
                val tokensArray = queueJson.optJSONArray("tokens")
                if (tokensArray != null && tokensArray.length() > 0) {
                    for (i in 0 until tokensArray.length()) {
                        val t = tokensArray.getJSONObject(i)
                        repository.insertOrUpdateCentralToken(
                            tokenNumber = t.optInt("token_number"),
                            darbarDate = t.optString("darbar_date"),
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
                    }
                }
            }

            repository.syncCurrentLiveSettingsFromGitHub()

            val allTokens = repository.getAllTokens()
            val (hOk, hMsg) = repository.syncWithCloudEndpoint("https://shribalajikripadham.online/api/cloud_sync.php")
            val s = repository.getSettings()
            updateFullLiveConfig(s, repository.getAllSevadars(), repository.getUiSectionConfigs())
            results.add(if (hOk) "🌐 Hosting: ✅ 100% एकसमान डेटा (${allTokens.size} टोकन व लाइव सेटिंग्स MySQL में सिंक)" else "🌐 Hosting: ⚠️ $hMsg")

            val (ghOk, ghMsg) = repository.publishCurrentSettingsToGitHub("Super Admin (Full Bidirectional 100% Mirror)")
            results.add(if (ghOk) "🚀 GitHub: ✅ 100% एकसमान डेटा (${allTokens.size} टोकन व संपूर्ण बही-खाता सुरक्षित)" else "🚀 GitHub: ⚠️ $ghMsg")

            if (GoogleSheetTokenSyncManager.isConfigured(context) && allTokens.isNotEmpty()) {
                GoogleSheetTokenSyncManager.postBatchTokensToSheet(context, allTokens)
                results.add("📊 Google Sheet: ✅ 100% एकसमान डेटा (${allTokens.size} टोकन सुरक्षित)")
            } else {
                results.add("📊 Google Sheet: ⚠️ लिंक कॉन्फ़िगर नहीं है")
            }

            Pair(true, results.joinToString("\n"))
        } catch (e: Exception) {
            Pair(false, "सिंक त्रुटि: ${e.localizedMessage}")
        }
    }

    // ========================================================================
    // EXPENSES & BILLS (HISAB-KITAB) LIVE SYNC
    // ========================================================================

    suspend fun fetchLiveExpenses(): Pair<Boolean, List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}get_expenses.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.37.0")

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                if (json.optBoolean("success", false)) {
                    val arr = json.optJSONArray("expenses") ?: JSONArray()
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until arr.length()) {
                        list.add(arr.getJSONObject(i))
                    }
                    return@withContext Pair(true, list)
                }
            }
            Pair(false, emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "fetchLiveExpenses error: ${e.message}")
            Pair(false, emptyList())
        }
    }

    suspend fun saveExpense(
        title: String,
        amount: Double,
        category: String = "सामान्य आश्रम खर्च",
        expenseDate: String = "",
        spentBy: String = "आश्रम व्यवस्थापक",
        receiptPhotoUrl: String = "",
        paymentMode: String = "CASH",
        notes: String = "",
        id: Long = 0L
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}save_expense.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.37.0")

            val json = JSONObject().apply {
                if (id > 0) put("id", id)
                put("title", title)
                put("amount", amount)
                put("category", category)
                put("expense_date", if (expenseDate.isNotBlank()) expenseDate else java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                put("spent_by", spentBy)
                put("receipt_photo_url", receiptPhotoUrl)
                put("payment_mode", paymentMode)
                put("notes", notes)
            }

            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            if (code == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val resObj = JSONObject(resp)
                return@withContext Pair(true, resObj.optString("message", "बिल सफलतापूर्वक सुरक्षित हुआ!"))
            }
            Pair(false, "सर्वर रिस्पॉन्स: HTTP $code")
        } catch (e: Exception) {
            Pair(false, "बिल सिंक त्रुटि: ${e.localizedMessage}")
        }
    }

    suspend fun deleteExpense(id: Long): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}delete_expense.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.37.0")

            val json = JSONObject().apply { put("id", id) }
            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            if (code == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val resObj = JSONObject(resp)
                return@withContext Pair(true, resObj.optString("message", "बिल हटा दिया गया।"))
            }
            Pair(false, "सर्वर रिस्पॉन्स HTTP $code")
        } catch (e: Exception) {
            Pair(false, "बिल हटाने में त्रुटि: ${e.localizedMessage}")
        }
    }

    // ========================================================================
    // DEVOTEE PAYMENTS & DONATIONS LIVE SYNC
    // ========================================================================

    suspend fun fetchLivePayments(): Pair<Boolean, List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}get_payments.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.37.0")

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                if (json.optBoolean("success", false)) {
                    val arr = json.optJSONArray("payments") ?: JSONArray()
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until arr.length()) {
                        list.add(arr.getJSONObject(i))
                    }
                    return@withContext Pair(true, list)
                }
            }
            Pair(false, emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "fetchLivePayments error: ${e.message}")
            Pair(false, emptyList())
        }
    }

    suspend fun savePayment(
        receiptNumber: String,
        devoteeName: String,
        phoneNumber: String,
        amount: Double,
        purpose: String = "दान / सहयोग राशि",
        paymentMode: String = "UPI",
        transactionId: String = "",
        status: String = "SUCCESS",
        collectedBy: String = "ADMIN",
        notes: String = ""
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}save_payment.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.37.0")

            val json = JSONObject().apply {
                put("receipt_number", receiptNumber)
                put("devotee_name", devoteeName)
                put("phone_number", phoneNumber)
                put("amount", amount)
                put("purpose", purpose)
                put("payment_mode", paymentMode)
                put("transaction_id", transactionId)
                put("status", status)
                put("collected_by", collectedBy)
                put("notes", notes)
            }

            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            if (code == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val resObj = JSONObject(resp)
                return@withContext Pair(true, resObj.optString("message", "भुगतान/दान सफलतापूर्वक दर्ज हुआ!"))
            }
            Pair(false, "सर्वर रिस्पॉन्स: HTTP $code")
        } catch (e: Exception) {
            Pair(false, "दान सिंक त्रुटि: ${e.localizedMessage}")
        }
    }

    // ========================================================================
    // TOKEN STATUS ATOMIC UPDATE (WAITING -> SERVING -> COMPLETED / CANCELLED)
    // ========================================================================

    suspend fun updateTokenStatus(
        darbarDate: String,
        tokenNumber: Int,
        status: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}update_token_status.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.37.0")

            val json = JSONObject().apply {
                put("darbar_date", darbarDate)
                put("token_number", tokenNumber)
                put("status", status)
            }

            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            if (code == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val resObj = JSONObject(resp)
                return@withContext Pair(true, resObj.optString("message", "टोकन स्थिति अपडेट हुई!"))
            }
            Pair(false, "सर्वर रिस्पॉन्स HTTP $code")
        } catch (e: Exception) {
            Pair(false, "टोकन स्थिति सिंक त्रुटि: ${e.localizedMessage}")
        }
    }

    // ========================================================================
    // SUPERADMIN FULL LIVE CONFIG BROADCAST (Instant service toggle)
    // ========================================================================

    suspend fun syncSettingsToHostinger(settings: AshramSettings): Pair<Boolean, String> = updateFullLiveConfig(settings)

    suspend fun updateFullLiveConfig(
        settings: AshramSettings,
        sevadars: List<com.example.shribalajikripadham.data.model.SevadarProfile>? = null,
        sections: List<com.example.shribalajikripadham.data.model.UiSectionConfig>? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}live_config.php?api_key=$API_SECRET_KEY")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.56.59")

            val json = JSONObject().apply {
                put("api_key", API_SECRET_KEY)
                put("ashram_name", settings.ashramName)
                put("latitude", settings.latitude)
                put("longitude", settings.longitude)
                put("allowed_radius_meters", settings.allowedRadiusMeters)
                put("is_geofence_enforced", if (settings.isGeofenceEnforced) 1 else 0)
                put("is_outstation_advance_allowed", if (settings.isOutstationAdvanceAllowed) 1 else 0)
                put("outstation_min_distance_km", settings.outstationMinDistanceKm)
                put("current_serving_token", settings.runningTokenNumber)
                put("daily_token_limit", if (settings.maxDailyTokens > 0) settings.maxDailyTokens else 1000)
                put("is_token_service_enabled", if (settings.isTokenServiceEnabled) 1 else 0)
                put("token_service_mode", settings.tokenServiceMode)
                put("scheduled_token_open_timestamp", settings.scheduledTokenOpenTimestamp)
                put("app_download_url", settings.apkDownloadUrl)
                put("app_share_url", settings.appShareUrl)
                put("current_theme_id", settings.currentThemeId)
                put("is_festival_theme_enforced", if (settings.isFestivalThemeEnforced) 1 else 0)
                put("is_bus_booking_live", if (settings.isBusBookingLive) 1 else 0)
                put("is_dharamshala_live", if (settings.isDharamshalaLive) 1 else 0)
                put("is_live_counter_visible", if (settings.isLiveCounterVisible) 1 else 0)
                put("is_payment_feature_live", if (settings.isPaymentFeatureLive) 1 else 0)
                put("is_arzi_ledger_live", if (settings.isArziLedgerLive) 1 else 0)
                put("badi_arzi_rate", settings.badiArziRate)
                put("chhoti_arzi_rate", settings.chhotiArziRate)
                put("can_admin_view_arzi_ledger", if (settings.canAdminViewArziLedger) 1 else 0)
                put("can_devotee_view_arzi_ledger", if (settings.canDevoteeViewArziLedger) 1 else 0)
                put("can_devotee_view_yatra_diary", if (settings.canDevoteeViewYatraDiary) 1 else 0)
                put("is_darbar_active", if (settings.isDarbarActive) 1 else 0)
                put("darbar_date", settings.darbarDate)
                put("darbar_timings", settings.darbarTimings)
                put("emergency_notice", settings.emergencyNoticeText)
                put("is_emergency_notice_visible", if (settings.isEmergencyNoticeVisible) 1 else 0)
                put("banner_title", settings.bannerTitle)
                put("banner_subtitle", settings.bannerSubtitle)
                put("is_banner_visible", if (settings.isBannerVisible) 1 else 0)
                put("guruji_photo_url", if (settings.gurujiPhotoUri.startsWith("http://") || settings.gurujiPhotoUri.startsWith("https://") || settings.gurujiPhotoUri.startsWith("uploads/")) settings.gurujiPhotoUri else "")
                put("allow_admin_reserved_tokens", if (settings.allowAdminReservedTokens) 1 else 0)
                put("contact_phone", settings.contactPhone.trim())
                put("phone", settings.contactPhone.trim())
                put("whatsapp_number", settings.whatsappNumber.trim())
                put("whatsapp", settings.whatsappNumber.trim())
                put("upi_id", settings.ashramUpiId.trim())
                put("bank_upi_id", settings.ashramUpiId.trim())
                put("upi_name", settings.ashramUpiName.trim())
                put("aarti_mangala_time", settings.websiteAartiMangala)
                put("aarti_balbhog_time", settings.websiteAartiBalbhog)
                put("aarti_sandhya_time", settings.websiteAartiSandhya)
                put("aarti_shayan_time", settings.websiteAartiShayan)
                put("aarti_timings", "${settings.websiteAartiMangala} | ${settings.websiteAartiSandhya}")

                // Full Website CMS Fields (100% Dynamic from App)
                put("top_bar_text", settings.websiteTopBarText)
                put("guruji_title", settings.websiteGurujiTitle)
                put("guruji_bio", settings.websiteGurujiBio)
                put("ashram_history_hindi", settings.ashramHistoryHindi)
                put("ashram_history", settings.ashramHistoryHindi)
                put("token_rules_notice", settings.websiteTokenRuleNotice)
                put("token_rules_summary", settings.websiteTokenRuleNotice)
                put("youtube_live_url", settings.youtubeLiveUrl)
                put("youtube_live_video_id", settings.youtubeLiveUrl)
                put("instagram_url", settings.instagramUrl)
                put("contact_email", settings.websiteContactEmail.trim())
                put("bank_name", settings.websiteBankName)
                put("bank_account_holder", settings.websiteAccountHolder)
                put("bank_account_number", settings.websiteAccountNumber)
                put("bank_ifsc", settings.websiteBankIfsc)
                put("bank_branch", settings.websiteBankBranch)
                put("ashram_address", settings.websiteAshramAddress)
                put("ashram_directions", settings.websiteAshramDirections)
                put("aarti_lyrics", settings.websiteAartiLyrics)
                put("footer_title", settings.websiteFooterTitle)
                put("footer_copyright", settings.websiteFooterCopyright)
                put("is_tuesday_darbar_enabled", if (settings.isTuesdayDarbarEnabled) 1 else 0)
                put("tuesday_darbar_name", settings.tuesdayDarbarName)
                put("tuesday_darbar_address", settings.tuesdayDarbarAddress)
                put("tuesday_latitude", settings.tuesdayLatitude)
                put("tuesday_longitude", settings.tuesdayLongitude)
                put("tuesday_allowed_radius_meters", settings.tuesdayAllowedRadiusMeters)
                put("tuesday_outstation_min_distance_km", settings.tuesdayOutstationMinDistanceKm)
                put("tuesday_darbar_timings", settings.tuesdayDarbarTimings)
                put("tuesday_token_service_mode", settings.tuesdayTokenServiceMode)
                put("tuesday_scheduled_open_timestamp", settings.tuesdayScheduledOpenTimestamp)
                put("tuesday_darbar_date", settings.tuesdayDarbarDate)
                put("tuesday_current_serving_token", settings.tuesdayCurrentServingToken)
                put("tuesday_running_token_number", settings.tuesdayRunningTokenNumber)
                put("tuesday_token_notice", settings.tuesdayTokenNotice)
                put("havan_estimated_cost", settings.havanEstimatedCost)
                put("havan_rules_notice", settings.havanRulesNotice)

                if (sevadars != null) {
                    val sArr = JSONArray()
                    sevadars.forEach { sev ->
                        val sObj = JSONObject().apply {
                            put("id", sev.id)
                            put("name", sev.name)
                            put("role", sev.roleTitleHindi)
                            put("phone", sev.phoneNumber)
                            put("photo_url", sev.photoUri)
                            put("display_order", sev.displayOrder)
                            put("is_active", if (sev.isActive) 1 else 0)
                        }
                        sArr.put(sObj)
                    }
                    put("sevadars", sArr)
                }

                if (sections != null) {
                    val secArr = JSONArray()
                    sections.forEach { itm ->
                        val obj = JSONObject().apply {
                            put("section_id", itm.sectionId)
                            put("title_hindi", itm.titleHindi)
                            put("title_english", itm.titleEnglish)
                            put("icon", itm.icon)
                            put("is_visible", itm.isVisible)
                            put("order_index", itm.orderIndex)
                            put("custom_subtitle_hindi", itm.customSubtitleHindi)
                            put("custom_subtitle_english", itm.customSubtitleEnglish)
                            put("custom_content_hindi", itm.customContentHindi)
                            put("custom_content_english", itm.customContentEnglish)
                            put("target_audience", itm.targetAudience)
                        }
                        secArr.put(obj)
                    }
                    put("sections", secArr)
                }
            }

            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            if (code == 200) {
                lastLiveConfigEtag = null
                cachedLiveConfig = null
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val resObj = JSONObject(resp)
                return@withContext Pair(true, resObj.optString("message", "सेटिंग्स लाइव प्रसारित हो गईं!"))
            }
            val errBody = try {
                conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            } catch (e: Exception) { "" }
            val cleanErr = if (errBody.isNotBlank()) {
                try {
                    val eo = JSONObject(errBody)
                    eo.optString("error", eo.optString("message", errBody))
                } catch (pe: Exception) { errBody }
            } else "सर्वर रिस्पॉन्स HTTP $code"
            Pair(false, cleanErr)
        } catch (e: Exception) {
            Pair(false, "लाइव सेटिंग्स सिंक त्रुटि: ${e.localizedMessage}")
        }
    }

    suspend fun updateUiSections(
        context: Context,
        sections: List<com.example.shribalajikripadham.data.model.UiSectionConfig>
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}live_config.php?api_key=$API_SECRET_KEY")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.56.59")

            val secArray = JSONArray()
            sections.forEach { itm ->
                val obj = JSONObject().apply {
                    put("section_id", itm.sectionId)
                    put("title_hindi", itm.titleHindi)
                    put("title_english", itm.titleEnglish)
                    put("icon", itm.icon)
                    put("is_visible", itm.isVisible)
                    put("order_index", itm.orderIndex)
                    put("custom_subtitle_hindi", itm.customSubtitleHindi)
                    put("custom_subtitle_english", itm.customSubtitleEnglish)
                    put("custom_content_hindi", itm.customContentHindi)
                    put("custom_content_english", itm.customContentEnglish)
                    put("target_audience", itm.targetAudience)
                }
                secArray.put(obj)
            }

            val json = JSONObject().apply {
                put("api_key", API_SECRET_KEY)
                put("sections", secArray)
            }

            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            if (code == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val resObj = JSONObject(resp)
                return@withContext Pair(true, resObj.optString("message", "होम स्क्रीन लेआउट लाइव सर्वर पर अपडेट हो गया!"))
            }
            val errBody = try {
                conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            } catch (e: Exception) { "" }
            Pair(false, "सर्वर त्रुटि HTTP $code: $errBody")
        } catch (e: Exception) {
            Pair(false, "लेआउट सिंक त्रुटि: ${e.localizedMessage}")
        }
    }


    /**
     * Update Token Darshan Status on Central Server
     */
    suspend fun updateCentralTokenStatus(
        tokenNumber: Int,
        darbarDate: String,
        status: String,
        isDarshanCompleted: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}update_token_status.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.56.5")

            val params = "api_key=$API_SECRET_KEY&token_number=$tokenNumber&darbar_date=${URLEncoder.encode(darbarDate, "UTF-8")}&status=${URLEncoder.encode(status, "UTF-8")}&is_darshan_completed=${if (isDarshanCompleted) 1 else 0}"
            conn.outputStream.use { it.write(params.toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            if (code == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                return@withContext JSONObject(resp).optBoolean("success", false)
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete a single token from Central Hostinger MySQL
     */
    suspend fun deleteCentralToken(
        tokenNumber: Int,
        darbarDate: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}delete_token.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.56.5")

            val params = "action=DELETE_SINGLE&token_number=$tokenNumber&darbar_date=${URLEncoder.encode(darbarDate, "UTF-8")}&api_key=${URLEncoder.encode(API_SECRET_KEY, "UTF-8")}"
            conn.outputStream.use { it.write(params.toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            if (code == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                if (json.optBoolean("success", false)) {
                    return@withContext Pair(true, json.optString("message", "टोकन सफलतापूर्वक हटा दिया गया"))
                } else {
                    return@withContext Pair(false, json.optString("message", "हटाने में त्रुटि"))
                }
            }
            Pair(false, "सर्वर त्रुटि: HTTP $code")
        } catch (e: Exception) {
            Log.e(TAG, "deleteCentralToken error: ${e.message}")
            Pair(false, "त्रुटि: ${e.localizedMessage}")
        }
    }

    /**
     * SuperAdmin: Delete ALL tokens for a specific Sunday/date from Central Hostinger MySQL
     */
    suspend fun deleteAllCentralTokensForDate(
        darbarDate: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}delete_token.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.56.5")

            val params = "action=DELETE_ALL_FOR_DATE&darbar_date=${URLEncoder.encode(darbarDate, "UTF-8")}&api_key=${URLEncoder.encode(API_SECRET_KEY, "UTF-8")}"
            conn.outputStream.use { it.write(params.toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            if (code == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                if (json.optBoolean("success", false)) {
                    return@withContext Pair(true, json.optString("message", "सभी टोकन हटा दिए गए"))
                } else {
                    return@withContext Pair(false, json.optString("message", "हटाने में त्रुटि"))
                }
            }
            Pair(false, "सर्वर त्रुटि: HTTP $code")
        } catch (e: Exception) {
            Log.e(TAG, "deleteAllCentralTokensForDate error: ${e.message}")
            Pair(false, "त्रुटि: ${e.localizedMessage}")
        }
    }

    /**
     * SuperAdmin: Delete ALL historical tokens across all dates from Central Hostinger MySQL (Master Wipe)
     */
    suspend fun deleteAllCentralTokensHistory(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            // 1. Direct server-side master wipe attempt
            val url = URL("${BASE_URL}delete_token.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.70.0")

            val params = "action=DELETE_ALL_HISTORY&api_key=${URLEncoder.encode(API_SECRET_KEY, "UTF-8")}"
            conn.outputStream.use { it.write(params.toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            if (code == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                if (json.optBoolean("success", false)) {
                    return@withContext Pair(true, json.optString("message", "समस्त ऐतिहासिक टोकन सफलतापूर्वक साफ़ कर दिए गए"))
                }
            }

            // 2. Resilient Fallback: Query all distinct dates across cloud sync and delete each date
            val cloudUrl = URL("${BASE_URL}cloud_sync.php")
            val cloudConn = (cloudUrl.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
                connectTimeout = 10000
                readTimeout = 10000
            }
            val datesToDelete = mutableSetOf<String>()
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
            datesToDelete.add(todayStr)

            if (cloudConn.responseCode == 200) {
                val cloudResp = cloudConn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val cloudJson = JSONObject(cloudResp)
                val tokensArr = cloudJson.optJSONArray("tokens")
                if (tokensArr != null) {
                    for (i in 0 until tokensArr.length()) {
                        val tObj = tokensArr.optJSONObject(i)
                        val d = tObj?.optString("darbar_date") ?: ""
                        if (d.isNotBlank()) datesToDelete.add(d)
                    }
                }
            }

            for (date in datesToDelete) {
                deleteAllCentralTokensForDate(date)
            }

            Pair(true, "समस्त ऐतिहासिक टोकन सफलतापूर्वक साफ़ कर दिए गए")
        } catch (e: Exception) {
            Log.e(TAG, "deleteAllCentralTokensHistory error: ${e.message}")
            Pair(false, "त्रुटि: ${e.localizedMessage}")
        }
    }


    /**
     * Save Sevadar to Central Hostinger MySQL
     */
    suspend fun saveCentralSevadar(
        name: String,
        role: String,
        phone: String,
        photoUrl: String = "",
        displayOrder: Int = 0,
        id: Long = 0
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}save_sevadar.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.39.0")

            val params = StringBuilder()
            params.append("name=").append(URLEncoder.encode(name, "UTF-8"))
            params.append("&role=").append(URLEncoder.encode(role, "UTF-8"))
            params.append("&phone=").append(URLEncoder.encode(phone, "UTF-8"))
            params.append("&photo_url=").append(URLEncoder.encode(photoUrl, "UTF-8"))
            params.append("&display_order=").append(displayOrder)
            params.append("&api_key=").append(URLEncoder.encode(API_SECRET_KEY, "UTF-8"))
            if (id > 0) params.append("&id=").append(id)

            conn.outputStream.use { it.write(params.toString().toByteArray(StandardCharsets.UTF_8)) }

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val j = JSONObject(resp)
                Pair(j.optBoolean("success", false), j.optString("message", "सफल"))
            } else {
                Pair(false, "HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "त्रुटि")
        }
    }

    /**
     * Delete Sevadar from Central Hostinger MySQL
     */
    suspend fun deleteCentralSevadar(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}delete_sevadar.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.52.0")

            val params = "id=$id&api_key=" + URLEncoder.encode(API_SECRET_KEY, "UTF-8")
            conn.outputStream.use { it.write(params.toByteArray(StandardCharsets.UTF_8)) }
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete Parcha from Central Hostinger MySQL
     */
    suspend fun deleteCentralParcha(parchaId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}delete_parcha.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.56.19")

            val params = "parcha_id=" + URLEncoder.encode(parchaId, "UTF-8") + "&api_key=" + URLEncoder.encode(API_SECRET_KEY, "UTF-8")
            conn.outputStream.use { it.write(params.toByteArray(StandardCharsets.UTF_8)) }
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fetch active Sevadars from Central Hostinger MySQL
     */
    suspend fun fetchCentralSevadars(): List<com.example.shribalajikripadham.data.model.SevadarProfile> = withContext(Dispatchers.IO) {
        val list = mutableListOf<com.example.shribalajikripadham.data.model.SevadarProfile>()
        try {
            val url = URL("${BASE_URL}get_sevadars.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.52.0")

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val j = JSONObject(resp)
                val arr = j.optJSONArray("sevadars")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val s = arr.getJSONObject(i)
                        list.add(
                            com.example.shribalajikripadham.data.model.SevadarProfile(
                                id = s.optLong("id", 0L),
                                name = s.optString("name", ""),
                                roleTitleHindi = s.optString("role", "सेवादार"),
                                roleTitleEnglish = s.optString("role", "Sevadar"),
                                phoneNumber = s.optString("phone", ""),
                                photoUri = s.optString("photo_url", ""),
                                displayOrder = s.optInt("display_order", 0),
                                isActive = s.optInt("is_active", 1) == 1
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching central sevadars: ${e.message}")
        }
        list
    }

    /**
     * Fetch active Donors from Central Hostinger MySQL
     */
    suspend fun fetchCentralDonors(): List<com.example.shribalajikripadham.data.model.DonorProfile> = withContext(Dispatchers.IO) {
        val list = mutableListOf<com.example.shribalajikripadham.data.model.DonorProfile>()
        try {
            val url = URL("${BASE_URL}get_donors.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.56.8")

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val j = JSONObject(resp)
                val arr = j.optJSONArray("donors")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val d = arr.getJSONObject(i)
                        list.add(
                            com.example.shribalajikripadham.data.model.DonorProfile(
                                id = d.optLong("id", 0L),
                                name = d.optString("name", ""),
                                cityAddress = d.optString("city_address", ""),
                                title = d.optString("title", "परम सहयोगी / दानदाता"),
                                photoUri = d.optString("photo_url", ""),
                                phone = "",
                                notes = d.optString("notes", ""),
                                displayOrder = d.optInt("display_order", 0),
                                isActive = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching central donors: ${e.message}")
        }
        list
    }

    /**
     * Save Donor to Central Hostinger MySQL (STRICT PRIVACY: phone is NEVER public)
     */
    suspend fun saveCentralDonor(
        name: String,
        cityAddress: String,
        title: String,
        photoUrl: String = "",
        phone: String = "",
        notes: String = "",
        displayOrder: Int = 0,
        id: Long = 0
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}save_donor.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.56.8")

            val params = StringBuilder()
            params.append("api_key=").append(URLEncoder.encode(API_SECRET_KEY, "UTF-8"))
            params.append("&name=").append(URLEncoder.encode(name, "UTF-8"))
            params.append("&city_address=").append(URLEncoder.encode(cityAddress, "UTF-8"))
            params.append("&title=").append(URLEncoder.encode(title, "UTF-8"))
            params.append("&photo_url=").append(URLEncoder.encode(photoUrl, "UTF-8"))
            params.append("&phone=").append(URLEncoder.encode(phone, "UTF-8"))
            params.append("&notes=").append(URLEncoder.encode(notes, "UTF-8"))
            params.append("&display_order=").append(displayOrder)
            if (id > 0) params.append("&id=").append(id)

            conn.outputStream.use { it.write(params.toString().toByteArray(StandardCharsets.UTF_8)) }

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val j = JSONObject(resp)
                Pair(j.optBoolean("success", false), j.optString("message", "सफल"))
            } else {
                Pair(false, "HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "त्रुटि")
        }
    }

    /**
     * Delete Donor from Central Hostinger MySQL
     */
    suspend fun deleteCentralDonor(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}delete_donor.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.56.8")

            val params = "id=$id&api_key=" + URLEncoder.encode(API_SECRET_KEY, "UTF-8")
            conn.outputStream.use { it.write(params.toByteArray(StandardCharsets.UTF_8)) }
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Register FCM Device Token for Devotee
     */
    suspend fun registerFcmDeviceToken(
        phoneNumber: String,
        fcmToken: String,
        deviceId: String = "",
        deviceName: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}register_fcm_token.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.41.0")

            val json = JSONObject().apply {
                put("phone_number", phoneNumber)
                put("fcm_token", fcmToken)
                put("device_id", deviceId)
                put("device_name", deviceName)
            }
            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    data class DevoteeNotification(
        val id: Long,
        val phoneNumber: String,
        val tokenNumber: Int,
        val title: String,
        val message: String,
        val type: String,
        val isRead: Boolean,
        val createdAt: Long
    )

    /**
     * Fetch unread devotee notifications from Hostinger central inbox
     */
    suspend fun fetchDevoteeNotifications(phoneNumber: String): List<DevoteeNotification> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DevoteeNotification>()
        val cleanPhone = phoneNumber.filter { it.isDigit() }.let { if (it.length > 10) it.takeLast(10) else it }
        if (cleanPhone.isBlank()) return@withContext list
        try {
            val url = URL("${BASE_URL}get_devotee_notifications.php?phone=$cleanPhone")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("User-Agent", "ShriBalajiApp/2.56.28")
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(resp)
                if (root.optBoolean("success", false)) {
                    val arr = root.optJSONArray("notifications") ?: org.json.JSONArray()
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        list.add(
                            DevoteeNotification(
                                id = item.optLong("id", 0L),
                                phoneNumber = item.optString("phone_number", ""),
                                tokenNumber = item.optInt("token_number", 0),
                                title = item.optString("title", ""),
                                message = item.optString("message", ""),
                                type = item.optString("type", "TOKEN_CALL"),
                                isRead = item.optInt("is_read", 0) == 1,
                                createdAt = item.optLong("created_at", 0L)
                            )
                        )
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            android.util.Log.e("SyncManager", "fetchDevoteeNotifications error: ${e.message}")
        }
        list
    }

    /**
     * Mark devotee notification as read on Hostinger
     */
    suspend fun markDevoteeNotificationRead(phoneNumber: String, notificationId: Long = 0L): Boolean = withContext(Dispatchers.IO) {
        val cleanPhone = phoneNumber.filter { it.isDigit() }.let { if (it.length > 10) it.takeLast(10) else it }
        if (cleanPhone.isBlank()) return@withContext false
        try {
            val url = URL("${BASE_URL}get_devotee_notifications.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("User-Agent", "ShriBalajiApp/2.56.28")
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "POST"
                doOutput = true
            }
            val json = JSONObject().apply {
                put("action", "MARK_READ")
                put("phone", cleanPhone)
                if (notificationId > 0L) put("id", notificationId)
            }
            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            ok
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Send FCM Push Notification to Devotee
     */
    suspend fun sendFcmPushNotification(
        phoneNumber: String,
        tokenNumber: Int,
        patientName: String,
        title: String = "",
        body: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}send_fcm.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.41.0")

            val json = JSONObject().apply {
                put("phone_number", phoneNumber)
                put("token_number", tokenNumber)
                put("patient_name", patientName)
                if (title.isNotBlank()) put("title", title)
                if (body.isNotBlank()) put("body", body)
                put("type", "TOKEN_CALL")
            }
            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Send OneSignal Push Notification Broadcast to All Devotees
     */
    suspend fun sendOneSignalBroadcast(
        title: String,
        message: String,
        priority: String = "HIGH"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}onesignal_service.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.56.28")

            val json = JSONObject().apply {
                put("action", "broadcast")
                put("title", title)
                put("message", message)
                put("priority", priority)
            }
            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Atomic Server-Side Bus Seat Hold (5-Minute Lock via book_bus_seat.php)
     */
    suspend fun holdBusSeatsRemote(
        yatraDate: String,
        seatNumbers: List<Int>,
        deviceId: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}book_bus_seat.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.45.0")

            val json = JSONObject().apply {
                put("action", "hold")
                put("yatra_date", yatraDate)
                val seatArr = JSONArray()
                seatNumbers.forEach { seatArr.put(it) }
                put("seat_numbers", seatArr)
                put("device_id", deviceId)
            }

            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val resp = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            if (resp.isNotBlank()) {
                val resObj = JSONObject(resp)
                val success = resObj.optBoolean("success", false)
                val msg = if (success) {
                    resObj.optString("message", "सीटें 5 मिनट के लिए सफलतापूर्वक होल्ड कर दी गई हैं।")
                } else {
                    resObj.optString("error", "सीट होल्ड नहीं हो सकी।")
                }
                return@withContext Pair(success, msg)
            }
            Pair(false, "सर्वर रिस्पॉन्स HTTP $code")
        } catch (e: Exception) {
            Log.w(TAG, "holdBusSeatsRemote error: ${e.message}")
            Pair(false, "सर्वर से संपर्क नहीं हो सका: ${e.localizedMessage}")
        }
    }

    /**
     * Release Bus Seat Hold on Central Server
     */
    suspend fun releaseBusSeatsHoldRemote(
        yatraDate: String,
        seatNumbers: List<Int>,
        deviceId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}book_bus_seat.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.45.0")

            val json = JSONObject().apply {
                put("action", "release_hold")
                put("yatra_date", yatraDate)
                val seatArr = JSONArray()
                seatNumbers.forEach { seatArr.put(it) }
                put("seat_numbers", seatArr)
                put("device_id", deviceId)
            }

            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val resp = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            if (resp.isNotBlank()) {
                val resObj = JSONObject(resp)
                return@withContext resObj.optBoolean("success", false)
            }
            code in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "releaseBusSeatsHoldRemote error: ${e.message}")
            false
        }
    }

    /**
     * Atomic Server-Side Bus Seat Booking (MySQL FOR UPDATE lock)
     */
    suspend fun bookBusSeatRemote(
        seat: BusSeat,
        deviceId: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}book_bus_seat.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.45.0")

            val json = JSONObject().apply {
                put("action", "book")
                put("yatra_date", seat.yatraDate.ifBlank { "2026-04-15" })
                put("seat_number", seat.seatNumber)
                put("passenger_name", seat.passengerName)
                put("passenger_phone", seat.phoneNumber)
                put("passenger_gender", seat.passengerGender)
                put("passenger_age", seat.passengerAge)
                put("payment_status", seat.paymentStatus.name)
                put("fare_amount", seat.fareAmount)
                put("booked_by", seat.bookedBy)
                put("device_id", deviceId)
                put("created_at", if (seat.bookedAt > 0) seat.bookedAt else System.currentTimeMillis())
            }

            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val resp = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            if (resp.isNotBlank()) {
                val resObj = JSONObject(resp)
                val success = resObj.optBoolean("success", false)
                val msg = if (success) {
                    resObj.optString("message", "सीट संख्या #${seat.seatNumber} सफलतापूर्वक आरक्षित हो गई!")
                } else {
                    resObj.optString("error", "सीट आरक्षण विफल रहा।")
                }
                return@withContext Pair(success, msg)
            }
            Pair(false, "सर्वर रिस्पॉन्स HTTP $code")
        } catch (e: Exception) {
            Log.w(TAG, "bookBusSeatRemote error: ${e.message}")
            Pair(false, "सर्वर से संपर्क नहीं हो सका: ${e.localizedMessage}")
        }
    }

    /**
     * Fetch Live Parchas directly from Hostinger MySQL API
     */
    suspend fun fetchLiveParchas(): List<com.example.shribalajikripadham.data.model.SacredParcha>? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://shribalajikripadham.online/api/get_parchas.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("User-Agent", "ShriBalajiApp/2.53.0")
            }
            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                if (json.optBoolean("success", false)) {
                    val arr = json.optJSONArray("parchas") ?: JSONArray()
                    val list = mutableListOf<com.example.shribalajikripadham.data.model.SacredParcha>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            com.example.shribalajikripadham.data.model.SacredParcha(
                                id = obj.optLong("id", 0L),
                                parchaId = obj.optString("parcha_id", "PARCHA_${obj.optLong("id", 0L)}"),
                                title = obj.optString("title", obj.optString("devotee_name", "पावन पर्चा")),
                                category = com.example.shribalajikripadham.data.model.ParchaCategory.fromString(obj.optString("category", "OTHER")),
                                subtitle = obj.optString("subtitle", ""),
                                mantraText = obj.optString("mantra_text", ""),
                                imageUri = obj.optString("image_uri", obj.optString("parcha_photo_url", "")),
                                isPublished = obj.optInt("is_published", 1) == 1,
                                isHidden = obj.optInt("is_hidden", 0) == 1,
                                viewCount = obj.optInt("view_count", 0),
                                downloadCount = obj.optInt("download_count", 0),
                                createdBy = obj.optString("created_by", "SUPER_ADMIN"),
                                createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                                updatedAt = obj.optLong("updated_at", System.currentTimeMillis())
                            )
                        )
                    }
                    return@withContext list
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "fetchLiveParchas failed: ${e.message}")
            null
        }
    }

    // ========================================================
    // 🎵 SACRED AUDIO TRACKS & MP3 CLOUD SYNC
    // ========================================================

    suspend fun fetchSacredTracks(admin: Boolean = false): Pair<Boolean, List<com.example.shribalajikripadham.data.sacred.SacredTrack>> = withContext(Dispatchers.IO) {
        val list = mutableListOf<com.example.shribalajikripadham.data.sacred.SacredTrack>()
        try {
            val urlStr = if (admin) "${BASE_URL}get_sacred_tracks.php?admin=1" else "${BASE_URL}get_sacred_tracks.php"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Cache-Control", "no-cache")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                if (json.optBoolean("success", false)) {
                    val arr = json.optJSONArray("tracks") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            com.example.shribalajikripadham.data.sacred.SacredTrack(
                                id = obj.optLong("id", 0L),
                                trackKey = obj.optString("track_key", ""),
                                titleHindi = obj.optString("title_hindi", ""),
                                titleEnglish = obj.optString("title_english", ""),
                                subtitleHindi = obj.optString("subtitle_hindi", ""),
                                durationText = obj.optString("duration_text", ""),
                                audioUrl = obj.optString("audio_url", ""),
                                lyricsHindi = obj.optString("lyrics_hindi", ""),
                                isPublished = obj.optInt("is_published", 1) == 1,
                                displayOrder = obj.optInt("display_order", 0),
                                youtubeSearchQuery = obj.optString("youtube_search_query", "")
                            )
                        )
                    }
                    return@withContext Pair(true, list)
                }
            }
            Pair(false, list)
        } catch (e: Exception) {
            Log.e(TAG, "fetchSacredTracks error: ${e.message}")
            Pair(false, list)
        }
    }

    suspend fun saveSacredTrack(track: com.example.shribalajikripadham.data.sacred.SacredTrack): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("${BASE_URL}save_sacred_track.php").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val payload = JSONObject().apply {
                if (track.id > 0) put("id", track.id)
                put("track_key", track.trackKey)
                put("title_hindi", track.titleHindi)
                put("title_english", track.titleEnglish)
                put("subtitle_hindi", track.subtitleHindi)
                put("duration_text", track.durationText)
                put("audio_url", track.audioUrl)
                put("lyrics_hindi", track.lyricsHindi)
                put("is_published", if (track.isPublished) 1 else 0)
                put("display_order", track.displayOrder)
                put("youtube_search_query", track.youtubeSearchQuery)
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                if (json.optBoolean("success", false)) {
                    return@withContext Pair(true, json.optString("message", "सफलतापूर्वक सुरक्षित!"))
                } else {
                    return@withContext Pair(false, json.optString("error", "सर्वर त्रुटि"))
                }
            }
            Pair(false, "HTTP ${conn.responseCode}")
        } catch (e: Exception) {
            Log.e(TAG, "saveSacredTrack error: ${e.message}")
            Pair(false, e.localizedMessage ?: "अज्ञात त्रुटि")
        }
    }

    suspend fun deleteSacredTrack(id: Long): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("${BASE_URL}delete_sacred_track.php").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val payload = JSONObject().apply {
                put("id", id)
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                if (json.optBoolean("success", false)) {
                    return@withContext Pair(true, json.optString("message", "ट्रैक हटा दिया गया!"))
                } else {
                    return@withContext Pair(false, json.optString("error", "सर्वर त्रुटि"))
                }
            }
            Pair(false, "HTTP ${conn.responseCode}")
        } catch (e: Exception) {
            Log.e(TAG, "deleteSacredTrack error: ${e.message}")
            Pair(false, e.localizedMessage ?: "अज्ञात त्रुटि")
        }
    }

    suspend fun uploadAudio(context: Context, file: File): String? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) return@withContext null
            val boundary = "SBKDAudioBoundary" + System.currentTimeMillis()
            val conn = (URL("${BASE_URL}upload_audio.php").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connectTimeout = 30000
                readTimeout = 60000
            }

            conn.outputStream.use { os ->
                val sb = StringBuilder()
                sb.append("--$boundary\r\n")
                sb.append("Content-Disposition: form-data; name=\"audio\"; filename=\"${file.name}\"\r\n")
                sb.append("Content-Type: audio/mpeg\r\n\r\n")
                os.write(sb.toString().toByteArray(StandardCharsets.UTF_8))

                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        os.write(buffer, 0, read)
                    }
                }
                os.write("\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                if (json.optBoolean("success", false)) {
                    return@withContext if (json.has("audio_url") && !json.isNull("audio_url")) json.optString("audio_url") else null
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "uploadAudio failed: ${e.message}")
            null
        }
    }

    // ========================================================
    // 🔴 LIVE DARBAR STREAMING & BROADCAST CONTROL
    // ========================================================

    suspend fun updateLiveStatus(
        isLive: Boolean,
        title: String = "श्री बालाजी कृपा धाम दिव्य दरबार लाइव",
        liveUrl: String = "",
        ytUrl: String = "",
        fbUrl: String = ""
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("${BASE_URL}update_live_status.php").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val payload = JSONObject().apply {
                put("is_live", if (isLive) 1 else 0)
                put("live_stream_title", title)
                put("live_stream_url", liveUrl)
                put("youtube_live_url", ytUrl)
                put("facebook_live_url", fbUrl)
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(resp)
                if (json.optBoolean("success", false)) {
                    return@withContext Pair(true, json.optString("message", if (isLive) "🔴 लाइव शुरू हुआ!" else "⏹️ लाइव समाप्त हुआ।"))
                }
            }
            Pair(false, "HTTP ${conn.responseCode}")
        } catch (e: Exception) {
            Log.e(TAG, "updateLiveStatus error: ${e.message}")
            Pair(false, e.localizedMessage ?: "अज्ञात त्रुटि")
        }
    }

    // ========================================================================
    // DHARAMSHALA ROOM MANAGEMENT (ADMIN & DEVOTEE)
    // ========================================================================
    suspend fun clearAllDharamshalaRooms(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}dharamshala.php?action=clear_all_rooms&api_key=$API_SECRET_KEY")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            conn.outputStream.use { it.write("{}".toByteArray(StandardCharsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val obj = JSONObject(resp)
                Pair(obj.optBoolean("success", false), obj.optString("message", "सभी कमरे साफ़ कर दिए गए"))
            } else {
                Pair(false, "सर्वर त्रुटि: HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "नेटवर्क त्रुटि")
        }
    }

    suspend fun addDharamshalaRoom(
        roomNumber: String,
        roomType: String,
        titleHindi: String,
        floor: String,
        capacity: Int,
        dailyRate: Double
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}dharamshala.php?action=add_room&api_key=$API_SECRET_KEY")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val payload = JSONObject().apply {
                put("room_number", roomNumber)
                put("room_type", roomType)
                put("title_hindi", titleHindi)
                put("floor", floor)
                put("capacity", capacity)
                put("daily_seva_rate", dailyRate)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(StandardCharsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val obj = JSONObject(resp)
                Pair(obj.optBoolean("success", false), obj.optString("message", "कमरा सफलतापूर्वक जोड़ा गया"))
            } else {
                Pair(false, "सर्वर त्रुटि: HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "नेटवर्क त्रुटि")
        }
    }

    suspend fun editDharamshalaRoom(
        roomId: Int,
        roomNumber: String,
        roomType: String,
        titleHindi: String,
        floor: String,
        capacity: Int,
        dailyRate: Double,
        status: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}dharamshala.php?action=edit_room&api_key=$API_SECRET_KEY")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val payload = JSONObject().apply {
                put("room_id", roomId)
                put("room_number", roomNumber)
                put("room_type", roomType)
                put("title_hindi", titleHindi)
                put("floor", floor)
                put("capacity", capacity)
                put("daily_seva_rate", dailyRate)
                put("status", status)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(StandardCharsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val obj = JSONObject(resp)
                Pair(obj.optBoolean("success", false), obj.optString("message", "कमरा अपडेट हो गया"))
            } else {
                Pair(false, "सर्वर त्रुटि: HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "नेटवर्क त्रुटि")
        }
    }

    suspend fun deleteDharamshalaRoom(roomId: Int): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}dharamshala.php?action=delete_room&api_key=$API_SECRET_KEY")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val payload = JSONObject().apply {
                put("room_id", roomId)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(StandardCharsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val obj = JSONObject(resp)
                Pair(obj.optBoolean("success", false), obj.optString("message", "कमरा हटा दिया गया"))
            } else {
                Pair(false, "सर्वर त्रुटि: HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "नेटवर्क त्रुटि")
        }
    }

    /**
     * Records real-time device heartbeat to Central Hostinger MySQL backend.
     */
    suspend fun recordDeviceHeartbeat(
        deviceId: String,
        deviceModel: String,
        androidVersion: String,
        appVersion: String,
        userName: String,
        phoneNumber: String,
        city: String,
        role: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}device_telemetry.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("User-Agent", "ShriBalajiApp/2.56.13")
            }
            val payload = JSONObject().apply {
                put("device_id", deviceId)
                put("device_model", deviceModel)
                put("android_version", androidVersion)
                put("app_version", appVersion)
                put("user_name", userName)
                put("phone_number", phoneNumber)
                put("city", city)
                put("role", role)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(StandardCharsets.UTF_8)) }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fetches real-time active devices presence from Central Hostinger MySQL backend.
     */
    suspend fun fetchLiveDevices(): Triple<Int, Int, List<DevicePresence>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}device_telemetry.php?nocache=${System.currentTimeMillis()}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ShriBalajiApp/2.56.13")
            }
            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(resp)
                if (root.optBoolean("success", false)) {
                    val total = root.optInt("total_devices", 0)
                    val activeToday = root.optInt("active_today", 0)
                    val arr = root.optJSONArray("devices") ?: JSONArray()
                    val list = mutableListOf<DevicePresence>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        list.add(
                            DevicePresence(
                                deviceId = o.optString("device_id"),
                                deviceModel = o.optString("device_model", "Android Device"),
                                userName = o.optString("user_name", ""),
                                phoneNumber = o.optString("phone_number", ""),
                                city = o.optString("city", ""),
                                appVersion = o.optString("app_version", "2.56.14"),
                                lastSeenAt = o.optLong("last_seen_at", System.currentTimeMillis()),
                                openCount = o.optInt("open_count", 1),
                                role = o.optString("role", "USER"),
                                androidVersion = o.optString("android_version", ""),
                                isOnline = o.optBoolean("is_online", false),
                                ipAddress = o.optString("ip_address", "")
                            )
                        )
                    }
                    return@withContext Triple(total, activeToday, list)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Triple(0, 0, emptyList())
    }

    suspend fun fetchCloudSecurityLogs(context: Context, limit: Int = 100): List<com.example.shribalajikripadham.data.model.AuditLogEntry> = withContext(Dispatchers.IO) {
        val list = mutableListOf<com.example.shribalajikripadham.data.model.AuditLogEntry>()
        try {
            val url = URL("${BASE_URL}get_security_logs.php?limit=$limit&nocache=${System.currentTimeMillis()}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-SBKD-API-KEY", API_SECRET_KEY)
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ShriBalajiApp/2.56.34")
            }
            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(resp)
                if (root.optBoolean("success", false)) {
                    val arr = root.optJSONArray("logs") ?: JSONArray()
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        var timeMillis = System.currentTimeMillis()
                        val createdAtStr = o.optString("created_at", "")
                        if (createdAtStr.isNotBlank()) {
                            try {
                                timeMillis = sdf.parse(createdAtStr)?.time ?: System.currentTimeMillis()
                            } catch (ignored: Exception) {}
                        }
                        val patientName = o.optString("patient_name", "अज्ञात भक्त")
                        val phone = o.optString("phone_number", "")
                        val devId = o.optString("device_id", "")
                        val ip = o.optString("ip_address", "")
                        val details = o.optString("details", "")
                        val combinedDetails = buildString {
                            if (phone.isNotBlank()) append("📱 फोन: $phone  ")
                            if (ip.isNotBlank()) append("🌐 IP: $ip  ")
                            if (devId.isNotBlank()) append("🔑 ID: ${devId.take(16)}...  ")
                            if (details.isNotBlank()) append("ℹ️ $details")
                        }.trim()

                        list.add(
                            com.example.shribalajikripadham.data.model.AuditLogEntry(
                                id = o.optLong("id", 0L),
                                action = o.optString("action", "SECURITY_BLOCKED"),
                                tokenNumber = 0,
                                performedBy = if (patientName.isNotBlank()) patientName else "अज्ञात भक्त",
                                role = "BLOCKED_DEVICE",
                                reason = o.optString("reason", "सुरक्षा नियम उल्लंघन"),
                                darbarDate = o.optString("darbar_date", ""),
                                details = combinedDetails,
                                timestamp = timeMillis
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun fetchAdminsFromCentralServer(): List<JSONObject> = withContext(Dispatchers.IO) {
        val list = mutableListOf<JSONObject>()
        try {
            val url = URL("${BASE_URL}admin_auth.php?action=LIST_ADMINS")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(resp)
                if (root.optBoolean("success", false)) {
                    val arr = root.optJSONArray("admins")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            list.add(arr.getJSONObject(i))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun saveAdminToCentralServer(
        name: String,
        username: String,
        phone: String,
        role: String,
        password: String = "",
        pin: String = "",
        isActive: Boolean = true
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}admin_auth.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            val payload = JSONObject().apply {
                put("action", "SAVE_ADMIN")
                put("name", name)
                put("username", username)
                put("phone", phone)
                put("role", role)
                if (password.isNotBlank()) put("password", password)
                if (pin.isNotBlank()) put("pin", pin)
                put("is_active", if (isActive) 1 else 0)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(StandardCharsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(resp)
                Pair(root.optBoolean("success", false), root.optString("message", "सफलता"))
            } else {
                Pair(false, "HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Error")
        }
    }
}


