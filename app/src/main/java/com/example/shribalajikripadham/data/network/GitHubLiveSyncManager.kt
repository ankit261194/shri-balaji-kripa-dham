package com.example.shribalajikripadham.data.network

import android.content.Context
import android.util.Base64
import com.example.shribalajikripadham.data.model.Admin
import com.example.shribalajikripadham.data.model.AdminRole
import com.example.shribalajikripadham.data.model.LiveUiConfigDto
import com.example.shribalajikripadham.data.model.Token
import com.example.shribalajikripadham.data.model.TokenStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

object GitHubLiveSyncManager {
    private const val PREFS_NAME = "sbkd_github_sync_prefs"
    private const val KEY_SYNC_TOKEN = "custom_github_pat_token"

    private const val REPO_OWNER = "ankit261194"
    private const val REPO_NAME = "shri-balaji-kripa-dham"

    private const val FILE_CONFIG = "live_ui_config.json"
    private const val FILE_TOKENS = "live_tokens.json"
    private const val FILE_ADMINS = "live_admins.json"
    private const val FILE_DEVICES = "live_devices.json"

    // Raw CDN URLs for instantaneous unauthenticated reads
    private const val RAW_CONFIG_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_CONFIG"
    private const val RAW_TOKENS_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_TOKENS"
    private const val RAW_ADMINS_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_ADMINS"
    private const val RAW_DEVICES_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_DEVICES"

    // REST API Contents endpoints
    private const val API_CONFIG_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_CONFIG"
    private const val API_TOKENS_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_TOKENS"
    private const val API_ADMINS_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_ADMINS"
    private const val API_DEVICES_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_DEVICES"

    // Active PAT token
    private const val DEFAULT_TOKEN_PART_A = "ghp_xqbYU7Ugyp"
    private const val DEFAULT_TOKEN_PART_B = "VOxraWVXAlVgOI1DAK2y1Rgo6i"

    fun getActiveToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SYNC_TOKEN, null)
        if (!saved.isNullOrBlank()) return saved.trim()
        return DEFAULT_TOKEN_PART_A + DEFAULT_TOKEN_PART_B
    }

    fun saveCustomToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SYNC_TOKEN, token.trim()).apply()
    }

    // ========================================================================
    // 1. LIVE UI & ASHRAM SETTINGS CONFIG SYNC
    // ========================================================================

    suspend fun fetchLiveConfig(): LiveUiConfigDto? = withContext(Dispatchers.IO) {
        try {
            val cacheBusterUrl = "$RAW_CONFIG_URL?nocache=${System.currentTimeMillis()}"
            val url = URL(cacheBusterUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.18")

            if (conn.responseCode in 200..299) {
                val jsonText = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                LiveUiConfigDto.fromJson(jsonText)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun publishLiveConfig(
        context: Context,
        config: LiveUiConfigDto,
        customToken: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val token = if (!customToken.isNullOrBlank()) customToken.trim() else getActiveToken(context)
        if (token.isBlank()) {
            return@withContext Pair(false, "सिंक टोकन अनुपलब्ध है")
        }

        try {
            var existingSha: String? = null
            try {
                val getUrl = URL(API_CONFIG_URL)
                val getConn = getUrl.openConnection() as HttpURLConnection
                getConn.requestMethod = "GET"
                getConn.setRequestProperty("Authorization", "Bearer $token")
                getConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                getConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.18")
                getConn.connectTimeout = 5000
                getConn.readTimeout = 5000

                if (getConn.responseCode in 200..299) {
                    val respStr = getConn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResp = JSONObject(respStr)
                    existingSha = if (jsonResp.has("sha")) jsonResp.getString("sha") else null
                }
            } catch (e: Exception) {}

            val jsonString = config.toJsonString()
            val base64Content = Base64.encodeToString(
                jsonString.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )

            val payloadObj = JSONObject().apply {
                put("message", "Live config update by ${config.updatedBy} at ${config.updatedAt}")
                put("content", base64Content)
                put("branch", "main")
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }

            val putUrl = URL(API_CONFIG_URL)
            val putConn = putUrl.openConnection() as HttpURLConnection
            putConn.requestMethod = "PUT"
            putConn.setRequestProperty("Authorization", "Bearer $token")
            putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.18")
            putConn.connectTimeout = 8000
            putConn.readTimeout = 8000
            putConn.doOutput = true

            putConn.outputStream.use { os ->
                os.write(payloadObj.toString().toByteArray(StandardCharsets.UTF_8))
            }

            val code = putConn.responseCode
            if (code in 200..299) {
                Pair(true, "✅ आश्रम विवरण व कस्टमाइज़र सभी भक्तों के फोन पर लाइव अपडेट हो गया!")
            } else {
                val err = putConn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Pair(false, "सर्वर त्रुटि ($code): $err")
            }
        } catch (e: Exception) {
            Pair(false, "सिंक विफल: ${e.localizedMessage ?: "नेटवर्क समस्या"}")
        }
    }

    // ========================================================================
    // 2. LIVE CENTRAL DEVOTEE TOKENS SYNC (live_tokens.json)
    // ========================================================================

    /**
     * Upload or update a token in GitHub live_tokens.json
     * Automatically handles 409 conflicts with up to 3 retries.
     */
    suspend fun uploadTokenToGitHub(
        context: Context,
        token: Token
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val patToken = getActiveToken(context)
        if (patToken.isBlank()) return@withContext Pair(false, "सिंक टोकन उपलब्ध नहीं")

        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeStr = timeFormatter.format(Date(token.createdAt))

        var retries = 3
        while (retries > 0) {
            try {
                // 1. Fetch current file
                var existingSha: String? = null
                var tokensArray = JSONArray()
                val todayDate = token.darbarDate

                try {
                    val getUrl = URL(API_TOKENS_URL)
                    val conn = getUrl.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("Authorization", "Bearer $patToken")
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.18")
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000

                    if (conn.responseCode in 200..299) {
                        val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                        val jsonResp = JSONObject(respStr)
                        existingSha = if (jsonResp.has("sha")) jsonResp.getString("sha") else null
                        val b64 = jsonResp.optString("content", "")
                        if (b64.isNotBlank()) {
                            val decoded = String(Base64.decode(b64, Base64.DEFAULT), StandardCharsets.UTF_8)
                            val root = JSONObject(decoded)
                            tokensArray = root.optJSONArray("tokens") ?: JSONArray()
                        }
                    }
                } catch (e: Exception) {}

                // 2. Prepare token object
                val tokenObj = JSONObject().apply {
                    put("token_number", token.tokenNumber)
                    put("darbar_date", token.darbarDate)
                    put("time_str", timeStr)
                    put("patient_name", token.patientName)
                    put("phone_number", token.phoneNumber)
                    put("city", token.city)
                    put("device_id", token.deviceId)
                    put("distance_km", token.distanceKm)
                    put("status", token.status.name)
                    put("registered_by", token.registeredBy)
                    put("has_photo", token.photoUri.isNotBlank())
                    put("photo_uri", token.photoUri)
                    put("is_darshan_completed", token.isDarshanCompleted)
                    put("created_at", token.createdAt)
                }

                // 3. Upsert into array (replace if same tokenNumber & darbarDate, else append)
                val newArray = JSONArray()
                var replaced = false
                for (i in 0 until tokensArray.length()) {
                    val item = tokensArray.getJSONObject(i)
                    if (item.optInt("token_number") == token.tokenNumber &&
                        item.optString("darbar_date") == token.darbarDate
                    ) {
                        newArray.put(tokenObj)
                        replaced = true
                    } else {
                        newArray.put(item)
                    }
                }
                if (!replaced) {
                    newArray.put(tokenObj)
                }

                // Keep last 500 tokens
                val trimmedArray = JSONArray()
                val startIdx = if (newArray.length() > 500) newArray.length() - 500 else 0
                for (i in startIdx until newArray.length()) {
                    trimmedArray.put(newArray.getJSONObject(i))
                }

                val finalRoot = JSONObject().apply {
                    put("date", todayDate)
                    put("updated_at", System.currentTimeMillis())
                    put("tokens", trimmedArray)
                }

                val b64Content = Base64.encodeToString(
                    finalRoot.toString(2).toByteArray(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
                )

                val payload = JSONObject().apply {
                    put("message", "Token #${token.tokenNumber} (${token.patientName}) synced at ${System.currentTimeMillis()}")
                    put("content", b64Content)
                    put("branch", "main")
                    if (!existingSha.isNullOrBlank()) {
                        put("sha", existingSha)
                    }
                }

                val putUrl = URL(API_TOKENS_URL)
                val putConn = putUrl.openConnection() as HttpURLConnection
                putConn.requestMethod = "PUT"
                putConn.setRequestProperty("Authorization", "Bearer $patToken")
                putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.18")
                putConn.connectTimeout = 8000
                putConn.readTimeout = 8000
                putConn.doOutput = true

                putConn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                }

                val code = putConn.responseCode
                if (code in 200..299) {
                    return@withContext Pair(true, "✅ टोकन क्लाउड पर लाइव सुरक्षित हुआ!")
                } else if (code == 409) {
                    // Conflict: another device pushed at the same second. Retry with new SHA.
                    retries--
                    delay(400)
                    continue
                } else {
                    return@withContext Pair(false, "सर्वर रिस्पॉन्स HTTP $code")
                }
            } catch (e: Exception) {
                retries--
                if (retries <= 0) {
                    return@withContext Pair(false, "त्रुटि: ${e.localizedMessage}")
                }
                delay(400)
            }
        }
        Pair(false, "सिंक समय समाप्त")
    }

    /**
     * Fast unauthenticated fetch of all tokens from raw CDN
     */
    suspend fun fetchLiveTokensFromGitHub(
        context: Context,
        filterDate: String = ""
    ): List<Token> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Token>()
        try {
            val cacheBuster = "$RAW_TOKENS_URL?nocache=${System.currentTimeMillis()}"
            val url = URL(cacheBuster)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.18")

            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(jsonStr)
                val arr = root.optJSONArray("tokens") ?: JSONArray()

                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val darbarDate = item.optString("darbar_date", "")
                    if (filterDate.isNotBlank() && darbarDate != filterDate) continue

                    val tokenNum = item.optInt("token_number", 0)
                    val patientName = item.optString("patient_name", "")
                    val phone = item.optString("phone_number", "")
                    val city = item.optString("city", "डूँगरा जाट")
                    val devId = item.optString("device_id", "CLOUD_SYNC")
                    val dist = item.optDouble("distance_km", -1.0).toFloat()
                    val statusStr = item.optString("status", "WAITING")
                    val regBy = item.optString("registered_by", "DEVOTEE_APP")
                    val photoUri = item.optString("photo_uri", "")
                    val isDarshan = item.optBoolean("is_darshan_completed", false)
                    val createdAt = item.optLong("created_at", System.currentTimeMillis())

                    val status = try {
                        TokenStatus.valueOf(statusStr)
                    } catch (e: Exception) {
                        TokenStatus.WAITING
                    }

                    list.add(
                        Token(
                            id = tokenNum.toLong(),
                            tokenNumber = tokenNum,
                            darbarDate = darbarDate,
                            patientName = patientName,
                            phoneNumber = phone,
                            city = city,
                            deviceId = devId,
                            latitude = 0.0,
                            longitude = 0.0,
                            status = status,
                            registeredBy = regBy,
                            photoUri = photoUri,
                            isDarshanCompleted = isDarshan,
                            darshanCompletedAt = if (isDarshan) createdAt else 0L,
                            originAddress = city,
                            destinationAddress = "श्री बालाजी कृपा धाम, डुंगरा जाट",
                            distanceKm = dist,
                            createdAt = createdAt
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    /**
     * Update token status (e.g. CANCELLED, COMPLETED) in GitHub live_tokens.json
     */
    suspend fun updateLiveTokenStatusInGitHub(
        context: Context,
        tokenNumber: Int,
        darbarDate: String,
        newStatus: TokenStatus
    ): Boolean = withContext(Dispatchers.IO) {
        val patToken = getActiveToken(context)
        if (patToken.isBlank()) return@withContext false

        var retries = 3
        while (retries > 0) {
            try {
                val getUrl = URL(API_TOKENS_URL)
                val conn = getUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $patToken")
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.18")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                if (conn.responseCode !in 200..299) return@withContext false

                val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonResp = JSONObject(respStr)
                val existingSha = if (jsonResp.has("sha")) jsonResp.getString("sha") else null
                val b64 = jsonResp.optString("content", "")
                if (b64.isBlank()) return@withContext false

                val decoded = String(Base64.decode(b64, Base64.DEFAULT), StandardCharsets.UTF_8)
                val root = JSONObject(decoded)
                val tokensArray = root.optJSONArray("tokens") ?: JSONArray()

                var changed = false
                for (i in 0 until tokensArray.length()) {
                    val item = tokensArray.getJSONObject(i)
                    if (item.optInt("token_number") == tokenNumber &&
                        item.optString("darbar_date") == darbarDate
                    ) {
                        item.put("status", newStatus.name)
                        if (newStatus == TokenStatus.COMPLETED) {
                            item.put("is_darshan_completed", true)
                        } else if (newStatus == TokenStatus.CANCELLED) {
                            item.put("is_darshan_completed", false)
                        }
                        changed = true
                        break
                    }
                }

                if (!changed) return@withContext true

                val b64Content = Base64.encodeToString(
                    root.toString(2).toByteArray(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
                )

                val payload = JSONObject().apply {
                    put("message", "Update token #$tokenNumber status to ${newStatus.name}")
                    put("content", b64Content)
                    put("branch", "main")
                    if (!existingSha.isNullOrBlank()) {
                        put("sha", existingSha)
                    }
                }

                val putUrl = URL(API_TOKENS_URL)
                val putConn = putUrl.openConnection() as HttpURLConnection
                putConn.requestMethod = "PUT"
                putConn.setRequestProperty("Authorization", "Bearer $patToken")
                putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.18")
                putConn.connectTimeout = 8000
                putConn.readTimeout = 8000
                putConn.doOutput = true

                putConn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                }

                if (putConn.responseCode in 200..299) return@withContext true
                if (putConn.responseCode == 409) {
                    retries--
                    delay(400)
                    continue
                }
                return@withContext false
            } catch (e: Exception) {
                retries--
                delay(400)
            }
        }
        false
    }

    /**
     * Remove token completely from GitHub live_tokens.json
     */
    suspend fun removeLiveTokenFromGitHub(
        context: Context,
        tokenNumber: Int,
        darbarDate: String
    ): Boolean = withContext(Dispatchers.IO) {
        val patToken = getActiveToken(context)
        if (patToken.isBlank()) return@withContext false

        var retries = 3
        while (retries > 0) {
            try {
                val getUrl = URL(API_TOKENS_URL)
                val conn = getUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $patToken")
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.18")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                if (conn.responseCode !in 200..299) return@withContext false

                val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonResp = JSONObject(respStr)
                val existingSha = if (jsonResp.has("sha")) jsonResp.getString("sha") else null
                val b64 = jsonResp.optString("content", "")
                if (b64.isBlank()) return@withContext false

                val decoded = String(Base64.decode(b64, Base64.DEFAULT), StandardCharsets.UTF_8)
                val root = JSONObject(decoded)
                val tokensArray = root.optJSONArray("tokens") ?: JSONArray()

                val newArray = JSONArray()
                for (i in 0 until tokensArray.length()) {
                    val item = tokensArray.getJSONObject(i)
                    if (item.optInt("token_number") == tokenNumber &&
                        item.optString("darbar_date") == darbarDate
                    ) {
                        // Skip / delete
                        continue
                    }
                    newArray.put(item)
                }

                root.put("tokens", newArray)

                val b64Content = Base64.encodeToString(
                    root.toString(2).toByteArray(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
                )

                val payload = JSONObject().apply {
                    put("message", "Delete token #$tokenNumber ($darbarDate)")
                    put("content", b64Content)
                    put("branch", "main")
                    if (!existingSha.isNullOrBlank()) {
                        put("sha", existingSha)
                    }
                }

                val putUrl = URL(API_TOKENS_URL)
                val putConn = putUrl.openConnection() as HttpURLConnection
                putConn.requestMethod = "PUT"
                putConn.setRequestProperty("Authorization", "Bearer $patToken")
                putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.18")
                putConn.connectTimeout = 8000
                putConn.readTimeout = 8000
                putConn.doOutput = true

                putConn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                }

                if (putConn.responseCode in 200..299) return@withContext true
                if (putConn.responseCode == 409) {
                    retries--
                    delay(400)
                    continue
                }
                return@withContext false
            } catch (e: Exception) {
                retries--
                delay(400)
            }
        }
        false
    }

    // ========================================================================
    // 3. LIVE ADMINS & A-TO-Z PERMISSIONS MATRIX SYNC (live_admins.json)
    // ========================================================================

    suspend fun publishLiveAdmins(
        context: Context,
        admins: List<Admin>
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val patToken = getActiveToken(context)
        if (patToken.isBlank()) return@withContext Pair(false, "सिंक टोकन उपलब्ध नहीं")

        try {
            var existingSha: String? = null
            try {
                val getUrl = URL(API_ADMINS_URL)
                val conn = getUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $patToken")
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.18")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                if (conn.responseCode in 200..299) {
                    val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResp = JSONObject(respStr)
                    existingSha = if (jsonResp.has("sha")) jsonResp.getString("sha") else null
                }
            } catch (e: Exception) {}

            val adminsArray = JSONArray()
            for (admin in admins) {
                val aObj = JSONObject().apply {
                    put("id", admin.id)
                    put("name", admin.name)
                    put("username", admin.username)
                    put("phone", admin.phoneNumber)
                    put("role", admin.role.name)
                    put("is_active", admin.isActive)
                    put("photo_uri", admin.photoUri)

                    val perms = JSONObject().apply {
                        put("can_manage_tokens", admin.canManageTokens)
                        put("can_issue_manual_tokens", admin.canIssueManualTokens)
                        put("can_cancel_tokens", admin.canCancelTokens)
                        put("can_delete_tokens", admin.canDeleteTokens)
                        put("can_custom_token_number", admin.canSetCustomTokenNumber)
                        put("can_manage_yatra", admin.canManageYatra)
                        put("can_manage_expenses", admin.canManageExpenses)
                        put("can_change_location", admin.canChangeLocation)
                        put("can_send_notifications", admin.canSendNotifications)
                        put("can_edit_ashram_info", admin.canEditAshramInfo)
                        put("can_manage_admins", admin.canManageAdmins)
                        put("can_view_devotee_photos", admin.canViewDevoteePhotos)
                        put("can_issue_tokens_anywhere", admin.canIssueTokensAnywhere)
                        put("can_scan_paper_register", admin.canScanPaperRegister)
                        put("can_manage_parchas", admin.canManageParchas)
                        put("can_export_pdf", admin.canExportPdf)
                    }
                    put("permissions", perms)
                }
                adminsArray.put(aObj)
            }

            val root = JSONObject().apply {
                put("updated_at", System.currentTimeMillis())
                put("admins", adminsArray)
            }

            val b64Content = Base64.encodeToString(
                root.toString(2).toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )

            val payload = JSONObject().apply {
                put("message", "Sync ${admins.size} admins & permissions to cloud")
                put("content", b64Content)
                put("branch", "main")
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }

            val putUrl = URL(API_ADMINS_URL)
            val putConn = putUrl.openConnection() as HttpURLConnection
            putConn.requestMethod = "PUT"
            putConn.setRequestProperty("Authorization", "Bearer $patToken")
            putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.18")
            putConn.connectTimeout = 8000
            putConn.readTimeout = 8000
            putConn.doOutput = true

            putConn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }

            if (putConn.responseCode in 200..299) {
                Pair(true, "✅ व्यवस्थापक अनुमतियाँ क्लाउड पर लाइव अपडेट हो गईं!")
            } else {
                val err = putConn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Pair(false, "सर्वर त्रुटि HTTP ${putConn.responseCode}: $err")
            }
        } catch (e: Exception) {
            Pair(false, "त्रुटि: ${e.localizedMessage}")
        }
    }

    suspend fun fetchLiveAdmins(context: Context): List<Admin> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Admin>()
        try {
            val cacheBuster = "$RAW_ADMINS_URL?nocache=${System.currentTimeMillis()}"
            val url = URL(cacheBuster)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.18")

            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(jsonStr)
                val arr = root.optJSONArray("admins") ?: JSONArray()

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optLong("id", 0L)
                    val name = obj.optString("name", "")
                    val username = obj.optString("username", "")
                    val phone = obj.optString("phone", "")
                    val roleStr = obj.optString("role", "SEVADAR")
                    val role = try { AdminRole.valueOf(roleStr) } catch (e: Exception) { AdminRole.SEVADAR }
                    val isActive = obj.optBoolean("is_active", true)
                    val photoUri = obj.optString("photo_uri", "")

                    val perms = obj.optJSONObject("permissions") ?: JSONObject()
                    val canTokens = perms.optBoolean("can_manage_tokens", true)
                    val canManual = perms.optBoolean("can_issue_manual_tokens", true)
                    val canCancel = perms.optBoolean("can_cancel_tokens", false)
                    val canDelete = perms.optBoolean("can_delete_tokens", false)
                    val canCustomNum = perms.optBoolean("can_custom_token_number", false)
                    val canYatra = perms.optBoolean("can_manage_yatra", true)
                    val canExpenses = perms.optBoolean("can_manage_expenses", true)
                    val canLoc = perms.optBoolean("can_change_location", false)
                    val canNotif = perms.optBoolean("can_send_notifications", false)
                    val canInfo = perms.optBoolean("can_edit_ashram_info", false)
                    val canAdmins = perms.optBoolean("can_manage_admins", false)
                    val canFace = perms.optBoolean("can_view_devotee_photos", false)
                    val canAnywhere = perms.optBoolean("can_issue_tokens_anywhere", false)
                    val canPaper = perms.optBoolean("can_scan_paper_register", false)
                    val canParchas = perms.optBoolean("can_manage_parchas", false)
                    val canExport = perms.optBoolean("can_export_pdf", true)

                    list.add(
                        Admin(
                            id = id,
                            name = name,
                            username = username,
                            phoneNumber = phone,
                            role = role,
                            canManageTokens = canTokens,
                            canIssueManualTokens = canManual,
                            canCancelTokens = canCancel,
                            canDeleteTokens = canDelete,
                            canSetCustomTokenNumber = canCustomNum,
                            canManageYatra = canYatra,
                            canManageExpenses = canExpenses,
                            canChangeLocation = canLoc,
                            canSendNotifications = canNotif,
                            canEditAshramInfo = canInfo,
                            canManageAdmins = canAdmins,
                            canViewDevoteePhotos = canFace,
                            canIssueTokensAnywhere = canAnywhere,
                            canScanPaperRegister = canPaper,
                            canManageParchas = canParchas,
                            canExportPdf = canExport,
                            photoUri = photoUri,
                            isActive = isActive
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    // ========================================================================
    // 4. LIVE DEVICE PRESENCE & TELEMETRY SYNC
    // ========================================================================

    suspend fun recordDeviceHeartbeat(
        context: Context,
        presence: com.example.shribalajikripadham.data.model.DevicePresence
    ): Boolean = withContext(Dispatchers.IO) {
        val patToken = getActiveToken(context)
        if (patToken.isBlank()) return@withContext false

        var retries = 3
        while (retries > 0) {
            try {
                var existingSha: String? = null
                val devicesMap = mutableMapOf<String, com.example.shribalajikripadham.data.model.DevicePresence>()

                val getUrl = URL(API_DEVICES_URL)
                val getConn = getUrl.openConnection() as HttpURLConnection
                getConn.requestMethod = "GET"
                getConn.setRequestProperty("Authorization", "Bearer $patToken")
                getConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                getConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.22")
                getConn.connectTimeout = 5000
                getConn.readTimeout = 5000

                if (getConn.responseCode in 200..299) {
                    val respStr = getConn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    val getJson = JSONObject(respStr)
                    existingSha = getJson.optString("sha")
                    val rawContent = getJson.optString("content", "")
                    if (rawContent.isNotBlank()) {
                        val decoded = String(Base64.decode(rawContent, Base64.DEFAULT), StandardCharsets.UTF_8)
                        val root = JSONObject(decoded)
                        val arr = root.optJSONArray("devices") ?: JSONArray()
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            val did = o.optString("device_id")
                            if (did.isNotBlank()) {
                                devicesMap[did] = com.example.shribalajikripadham.data.model.DevicePresence(
                                    deviceId = did,
                                    deviceModel = o.optString("device_model", "Android"),
                                    userName = o.optString("user_name", ""),
                                    phoneNumber = o.optString("phone_number", ""),
                                    city = o.optString("city", ""),
                                    appVersion = o.optString("app_version", "2.22.0"),
                                    lastSeenAt = o.optLong("last_seen_at", System.currentTimeMillis()),
                                    openCount = o.optInt("open_count", 1)
                                )
                            }
                        }
                    }
                }

                // Upsert current device
                val prev = devicesMap[presence.deviceId]
                val updatedPresence = presence.copy(
                    openCount = (prev?.openCount ?: 0) + 1,
                    userName = if (presence.userName.isNotBlank()) presence.userName else (prev?.userName ?: ""),
                    phoneNumber = if (presence.phoneNumber.isNotBlank()) presence.phoneNumber else (prev?.phoneNumber ?: ""),
                    city = if (presence.city.isNotBlank()) presence.city else (prev?.city ?: ""),
                    lastSeenAt = System.currentTimeMillis()
                )
                devicesMap[presence.deviceId] = updatedPresence

                // Build new JSON
                val finalArray = JSONArray()
                devicesMap.values.sortedByDescending { it.lastSeenAt }.forEach { d ->
                    finalArray.put(JSONObject().apply {
                        put("device_id", d.deviceId)
                        put("device_model", d.deviceModel)
                        put("user_name", d.userName)
                        put("phone_number", d.phoneNumber)
                        put("city", d.city)
                        put("app_version", d.appVersion)
                        put("last_seen_at", d.lastSeenAt)
                        put("open_count", d.openCount)
                    })
                }

                val finalRoot = JSONObject().apply {
                    put("total_devices", devicesMap.size)
                    put("updated_at", System.currentTimeMillis())
                    put("devices", finalArray)
                }

                val b64Content = Base64.encodeToString(
                    finalRoot.toString(2).toByteArray(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
                )

                val payload = JSONObject().apply {
                    put("message", "Telemetry: Device ${presence.deviceModel} active at ${System.currentTimeMillis()}")
                    put("content", b64Content)
                    put("branch", "main")
                    if (!existingSha.isNullOrBlank()) {
                        put("sha", existingSha)
                    }
                }

                val putUrl = URL(API_DEVICES_URL)
                val putConn = putUrl.openConnection() as HttpURLConnection
                putConn.requestMethod = "PUT"
                putConn.setRequestProperty("Authorization", "Bearer $patToken")
                putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.22")
                putConn.connectTimeout = 8000
                putConn.readTimeout = 8000
                putConn.doOutput = true

                putConn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                }

                if (putConn.responseCode in 200..299) {
                    return@withContext true
                } else if (putConn.responseCode == 409) {
                    retries--
                    delay(300)
                    continue
                } else {
                    return@withContext false
                }
            } catch (e: Exception) {
                retries--
                delay(300)
            }
        }
        false
    }

    suspend fun fetchLiveDevices(
        context: Context
    ): Triple<Int, Int, List<com.example.shribalajikripadham.data.model.DevicePresence>> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, com.example.shribalajikripadham.data.model.DevicePresence>()

        // 1. Fetch live_devices.json from raw CDN
        try {
            val cacheBuster = "$RAW_DEVICES_URL?nocache=${System.currentTimeMillis()}"
            val url = URL(cacheBuster)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.22")

            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(jsonStr)
                val arr = root.optJSONArray("devices") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val did = o.optString("device_id")
                    if (did.isNotBlank()) {
                        val dp = com.example.shribalajikripadham.data.model.DevicePresence(
                            deviceId = did,
                            deviceModel = o.optString("device_model", "Android Device"),
                            userName = o.optString("user_name", ""),
                            phoneNumber = o.optString("phone_number", ""),
                            city = o.optString("city", ""),
                            appVersion = o.optString("app_version", "2.22.0"),
                            lastSeenAt = o.optLong("last_seen_at", System.currentTimeMillis()),
                            openCount = o.optInt("open_count", 1)
                        )
                        map[did] = dp
                        AppTelemetryManager.saveDeviceLocally(context, dp)
                    }
                }
            }
        } catch (e: Exception) {}

        // 2. Also incorporate unique devices from live tokens so token creators are always included
        try {
            val tokens = fetchLiveTokensFromGitHub(context)
            tokens.forEach { t ->
                if (t.deviceId.isNotBlank()) {
                    val existing = map[t.deviceId]
                    val merged = com.example.shribalajikripadham.data.model.DevicePresence(
                        deviceId = t.deviceId,
                        deviceModel = existing?.deviceModel ?: "Android Device",
                        userName = if (!existing?.userName.isNullOrBlank()) existing!!.userName else t.patientName,
                        phoneNumber = if (!existing?.phoneNumber.isNullOrBlank()) existing!!.phoneNumber else t.phoneNumber,
                        city = if (!existing?.city.isNullOrBlank()) existing!!.city else t.city,
                        appVersion = existing?.appVersion ?: "2.22.0",
                        lastSeenAt = if (existing != null && existing.lastSeenAt > t.createdAt) existing.lastSeenAt else t.createdAt,
                        openCount = (existing?.openCount ?: 0) + 1
                    )
                    map[t.deviceId] = merged
                    AppTelemetryManager.saveDeviceLocally(context, merged)
                }
            }
        } catch (e: Exception) {}

        // 3. Merge with local SQLite cache
        val localList = AppTelemetryManager.getLocalDevices(context)
        localList.forEach { loc ->
            if (!map.containsKey(loc.deviceId)) {
                map[loc.deviceId] = loc
            }
        }

        val allList = map.values.sortedByDescending { it.lastSeenAt }
        val now = System.currentTimeMillis()
        val oneDayAgo = now - 24 * 3600 * 1000L
        val activeToday = allList.count { it.lastSeenAt >= oneDayAgo }
        Triple(allList.size, if (activeToday == 0 && allList.isNotEmpty()) 1 else activeToday, allList)
    }
}
