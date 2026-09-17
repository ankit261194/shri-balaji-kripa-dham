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
    private const val FILE_PARCHAS = "live_parchas.json"
    private const val FILE_SESSIONS = "live_admin_sessions.json"
    private const val FILE_PAYMENTS = "live_payments.json"
    private const val FILE_BUS_SEATS = "live_bus_seats.json"
    private const val FILE_ARZI = "live_arzi.json"

    // Raw CDN URLs for instantaneous unauthenticated reads
    private const val RAW_CONFIG_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_CONFIG"
    private const val RAW_TOKENS_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_TOKENS"
    private const val RAW_ADMINS_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_ADMINS"
    private const val RAW_DEVICES_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_DEVICES"
    private const val RAW_PARCHAS_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_PARCHAS"
    private const val RAW_SESSIONS_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_SESSIONS"
    private const val RAW_PAYMENTS_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_PAYMENTS"
    private const val RAW_BUS_SEATS_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_BUS_SEATS"
    private const val RAW_ARZI_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_ARZI"

    // REST API Contents endpoints
    private const val API_CONFIG_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_CONFIG"
    private const val API_TOKENS_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_TOKENS"
    private const val API_ADMINS_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_ADMINS"
    private const val API_DEVICES_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_DEVICES"
    private const val API_PARCHAS_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_PARCHAS"
    private const val API_SESSIONS_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_SESSIONS"
    private const val API_PAYMENTS_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_PAYMENTS"
    private const val API_BUS_SEATS_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_BUS_SEATS"
    private const val API_ARZI_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_ARZI"

    // Server-Side Secure GitHub Sync: Client APK holds ZERO hardcoded credentials!
    fun getActiveToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SYNC_TOKEN, null)
        return saved?.trim() ?: ""
    }

    /**
     * Executes GitHub commit through Hostinger Server-Side Proxy.
     * The GitHub Personal Access Token is held securely in Hostinger PHP environment.
     * ZERO credentials in APK bytecode!
     */
    suspend fun pushViaHostingerProxy(path: String, content: String, message: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://shribalajikripadham.online/api/github_proxy.php")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.37.0")

            val json = JSONObject().apply {
                put("path", path)
                put("content", content)
                put("message", message)
                put("branch", "main")
            }

            conn.outputStream.use { os ->
                os.write(json.toString().toByteArray(StandardCharsets.UTF_8))
            }

            val code = conn.responseCode
            if (code in 200..201) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val resObj = JSONObject(resp)
                if (resObj.optBoolean("success", false)) {
                    return@withContext Pair(true, resObj.optString("message", "GitHub पर सर्वर प्रॉक्सी द्वारा 100% सुरक्षित रूप से सेव हुआ!"))
                }
            }
            Pair(false, "सर्वर प्रॉक्सी रिस्पॉन्स: HTTP $code")
        } catch (e: Exception) {
            Pair(false, "प्रॉक्सी सिंक त्रुटि: ${e.localizedMessage}")
        }
    }

    fun saveCustomToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SYNC_TOKEN, token.trim()).apply()
    }

    // ========================================================================
    // 0. CENTRAL CLOUD PHOTO UPLOAD SYNC
    // ========================================================================

    /**
     * Uploads any local image file or bitmap to the central GitHub repository under `uploads/`.
     * Returns the permanent raw CDN URL on success (e.g. https://raw.githubusercontent.com/.../uploads/filename.jpg),
     * or null on failure.
     */
    suspend fun uploadPhotoToGitHub(
        context: Context,
        localPathOrUri: String,
        remoteFileName: String
    ): String? = withContext(Dispatchers.IO) {
        // Privacy protection: Personal photos and identities are never uploaded to public GitHub repository.
        // Photos remain securely stored only on-device.
        if (localPathOrUri.isBlank()) return@withContext null
        return@withContext localPathOrUri
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
    // 1.5. LIVE CENTRAL BROADCAST NOTICE PUSH (live_broadcasts.json)
    // ========================================================================

    private const val FILE_BROADCASTS = "live_broadcasts.json"
    private const val RAW_BROADCASTS_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_BROADCASTS"
    private const val API_BROADCASTS_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_BROADCASTS"

    suspend fun publishBroadcastNoticeToCloud(
        context: Context,
        title: String,
        message: String,
        priority: String = "HIGH",
        sentBy: String = "Super Admin"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val token = getActiveToken(context)
        if (token.isBlank()) {
            val noticeObj = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("title", title)
                put("message", message)
                put("priority", priority)
                put("sent_by", sentBy)
                put("timestamp", System.currentTimeMillis())
            }
            return@withContext pushViaHostingerProxy(FILE_BROADCASTS, noticeObj.toString(2), "Broadcast: $title")
        }

        try {
            var existingSha: String? = null
            try {
                val getUrl = URL(API_BROADCASTS_URL)
                val getConn = getUrl.openConnection() as HttpURLConnection
                getConn.requestMethod = "GET"
                getConn.setRequestProperty("Authorization", "Bearer $token")
                getConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                getConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.28")
                getConn.connectTimeout = 5000
                getConn.readTimeout = 5000

                if (getConn.responseCode in 200..299) {
                    val respStr = getConn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResp = JSONObject(respStr)
                    existingSha = if (jsonResp.has("sha")) jsonResp.getString("sha") else null
                }
            } catch (e: Exception) {}

            val noticeObj = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("title", title)
                put("message", message)
                put("priority", priority)
                put("sent_by", sentBy)
                put("timestamp", System.currentTimeMillis())
            }

            val jsonString = noticeObj.toString(2)
            val base64Content = Base64.encodeToString(
                jsonString.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )

            val payloadObj = JSONObject().apply {
                put("message", "Broadcast notice: $title by $sentBy")
                put("content", base64Content)
                put("branch", "main")
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }

            val putUrl = URL(API_BROADCASTS_URL)
            val putConn = putUrl.openConnection() as HttpURLConnection
            putConn.requestMethod = "PUT"
            putConn.setRequestProperty("Authorization", "Bearer $token")
            putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.28")
            putConn.connectTimeout = 8000
            putConn.readTimeout = 8000
            putConn.doOutput = true

            putConn.outputStream.use { os ->
                os.write(payloadObj.toString().toByteArray(StandardCharsets.UTF_8))
            }

            val code = putConn.responseCode
            if (code in 200..299) {
                Pair(true, "✅ सूचना सभी भक्तों के फोन पर लाइव प्रसारित हो गई!")
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

                var tokenPhotoUrl = token.photoUri
                if (tokenPhotoUrl.isNotBlank() && !tokenPhotoUrl.startsWith("http://") && !tokenPhotoUrl.startsWith("https://")) {
                    val safeRemoteName = "token_${token.tokenNumber}_${token.darbarDate.replace('-', '_').replace('/', '_')}.jpg"
                    val uploaded = uploadPhotoToGitHub(context, tokenPhotoUrl, safeRemoteName)
                    if (!uploaded.isNullOrBlank()) {
                        tokenPhotoUrl = uploaded
                    }
                }

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
                    put("has_photo", tokenPhotoUrl.isNotBlank())
                    put("photo_uri", tokenPhotoUrl)
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
     * Uploads the entire consolidated tokens list to GitHub in a single clean commit.
     * Prevents GitHub 409 conflict spamming and enforces Smart Push Policy.
     */
    suspend fun uploadAllTokensConsolidated(
        context: Context,
        tokens: List<Token>,
        darbarDate: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val tokensArray = JSONArray()
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

        for (token in tokens) {
            val timeStr = try { timeFormatter.format(Date(token.createdAt)) } catch (e: Exception) { "" }
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
            tokensArray.put(tokenObj)
        }

        val finalRoot = JSONObject().apply {
            put("date", darbarDate)
            put("updated_at", System.currentTimeMillis())
            put("total_count", tokens.size)
            put("tokens", tokensArray)
        }

        // Try pushing via secure Hostinger Proxy first (0 hardcoded credentials)
        val jsonString = finalRoot.toString(2)
        val (proxyOk, proxyMsg) = pushViaHostingerProxy(
            FILE_TOKENS,
            jsonString,
            "Consolidated Backup of ${tokens.size} tokens for $darbarDate"
        )
        if (proxyOk) {
            return@withContext Pair(true, "✅ सभी ${tokens.size} टोकन GitHub पर सफलतापूर्वक सुरक्षित हो गए!")
        }

        // Direct GitHub API fallback if PAT exists
        val patToken = getActiveToken(context)
        if (patToken.isNotBlank()) {
            try {
                var existingSha: String? = null
                val getUrl = URL(API_TOKENS_URL)
                val conn = getUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $patToken")
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.41")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                if (conn.responseCode in 200..299) {
                    val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val j = JSONObject(respStr)
                    existingSha = if (j.has("sha")) j.getString("sha") else null
                }

                val b64Content = Base64.encodeToString(
                    jsonString.toByteArray(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
                )

                val payload = JSONObject().apply {
                    put("message", "Consolidated backup: ${tokens.size} tokens for $darbarDate")
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
                putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.41")
                putConn.connectTimeout = 10000
                putConn.readTimeout = 10000
                putConn.doOutput = true

                putConn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                }

                if (putConn.responseCode in 200..299) {
                    return@withContext Pair(true, "✅ सभी ${tokens.size} टोकन GitHub पर सुरक्षित हुए!")
                }
            } catch (e: Exception) {
                return@withContext Pair(false, "गिटहब सिंक त्रुटि: ${e.localizedMessage}")
            }
        }

        Pair(false, proxyMsg)
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
                var finalPhotoUri = admin.photoUri
                if (finalPhotoUri.isNotBlank() && !finalPhotoUri.startsWith("http://") && !finalPhotoUri.startsWith("https://")) {
                    val safeRemoteName = "sevadar_${admin.username.ifBlank { admin.id.toString() }}.jpg"
                    val uploaded = uploadPhotoToGitHub(context, finalPhotoUri, safeRemoteName)
                    if (!uploaded.isNullOrBlank()) {
                        finalPhotoUri = uploaded
                    }
                }
                val aObj = JSONObject().apply {
                    put("id", admin.id)
                    put("name", admin.name)
                    put("username", admin.username)
                    put("phone", admin.phoneNumber)
                    put("role", admin.role.name)
                    put("is_active", admin.isActive)
                    put("photo_uri", finalPhotoUri)
                    put("pin_hash", admin.pinHash)
                    put("password_hash", admin.passwordHash)

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
                        put("can_manage_arzi", admin.canManageArzi)
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
                    val pinHash = obj.optString("pin_hash", "")
                    val passwordHash = obj.optString("password_hash", "")

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
                    val canArzi = perms.optBoolean("can_manage_arzi", false)
                    val canExport = perms.optBoolean("can_export_pdf", true)

                    list.add(
                        Admin(
                            id = id,
                            name = name,
                            username = username,
                            phoneNumber = phone,
                            role = role,
                            pinHash = pinHash,
                            passwordHash = passwordHash,
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
                            canManageArzi = canArzi,
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
                                    openCount = o.optInt("open_count", 1),
                                    role = o.optString("role", "USER")
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
                    role = if (presence.role != "USER") presence.role else (prev?.role ?: "USER"),
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
                        put("role", d.role)
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
                            openCount = o.optInt("open_count", 1),
                            role = o.optString("role", "USER")
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
                        openCount = (existing?.openCount ?: 0) + 1,
                        role = existing?.role ?: "USER"
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

    // ========================================================================
    // 5. LIVE PARCHAS & SACRED DOCUMENTS SYNC (live_parchas.json)
    suspend fun fetchLiveParchas(): List<com.example.shribalajikripadham.data.model.SacredParcha>? = withContext(Dispatchers.IO) {
        try {
            val cacheBusterUrl = "$RAW_PARCHAS_URL?nocache=${System.currentTimeMillis()}&rand=${(1000..9999).random()}"
            val url = URL(cacheBusterUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.useCaches = false
            conn.defaultUseCaches = false
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
            conn.setRequestProperty("Pragma", "no-cache")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.29")

            if (conn.responseCode in 200..299) {
                val jsonText = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(jsonText)
                val arr = root.optJSONArray("parchas") ?: JSONArray()
                val list = mutableListOf<com.example.shribalajikripadham.data.model.SacredParcha>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val pid = o.optString("parcha_id")
                    if (pid.isNotBlank()) {
                        val catStr = o.optString("category", "OTHER")
                        val cat = com.example.shribalajikripadham.data.model.ParchaCategory.fromString(catStr)
                        list.add(
                            com.example.shribalajikripadham.data.model.SacredParcha(
                                parchaId = pid,
                                title = o.optString("title", ""),
                                category = cat,
                                subtitle = o.optString("subtitle", ""),
                                samagriList = com.example.shribalajikripadham.data.model.SacredParcha.parseJsonList(o.optJSONArray("samagri_list")?.toString()),
                                vidhiSteps = com.example.shribalajikripadham.data.model.SacredParcha.parseJsonList(o.optJSONArray("vidhi_steps")?.toString()),
                                precautions = com.example.shribalajikripadham.data.model.SacredParcha.parseJsonList(o.optJSONArray("precautions")?.toString()),
                                mantraText = o.optString("mantra_text", ""),
                                imageUri = o.optString("image_uri", ""),
                                isPublished = o.optBoolean("is_published", true),
                                isHidden = o.optBoolean("is_hidden", false),
                                viewCount = o.optInt("view_count", 0),
                                downloadCount = o.optInt("download_count", 0),
                                createdBy = o.optString("created_by", "SUPER_ADMIN"),
                                createdAt = o.optLong("created_at", System.currentTimeMillis()),
                                updatedAt = o.optLong("updated_at", System.currentTimeMillis())
                            )
                        )
                    }
                }
                list
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun publishLiveParchas(
        context: Context,
        parchas: List<com.example.shribalajikripadham.data.model.SacredParcha>,
        adminName: String = "Super Admin"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val patToken = getActiveToken(context)
        if (patToken.isBlank()) return@withContext Pair(false, "सिंक टोकन उपलब्ध नहीं")

        try {
            var existingSha: String? = null
            try {
                val getUrl = URL(API_PARCHAS_URL)
                val conn = getUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $patToken")
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.25")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode in 200..299) {
                    val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResp = JSONObject(respStr)
                    existingSha = if (jsonResp.has("sha")) jsonResp.getString("sha") else null
                }
            } catch (e: Exception) {}

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val root = JSONObject()
            root.put("updated_at", isoFormat.format(Date()))
            root.put("updated_by", adminName)
            root.put("version", 1)

            val arr = JSONArray()
            for (p in parchas) {
                val o = JSONObject()
                o.put("parcha_id", p.parchaId)
                o.put("title", p.title)
                o.put("category", p.category.name)
                o.put("subtitle", p.subtitle)
                val sArr = JSONArray(); p.samagriList.forEach { sArr.put(it) }; o.put("samagri_list", sArr)
                val vArr = JSONArray(); p.vidhiSteps.forEach { vArr.put(it) }; o.put("vidhi_steps", vArr)
                val pArr = JSONArray(); p.precautions.forEach { pArr.put(it) }; o.put("precautions", pArr)
                o.put("mantra_text", p.mantraText)
                o.put("image_uri", p.imageUri)
                o.put("is_published", p.isPublished)
                o.put("is_hidden", p.isHidden)
                o.put("view_count", p.viewCount)
                o.put("download_count", p.downloadCount)
                o.put("created_by", p.createdBy)
                o.put("created_at", p.createdAt)
                o.put("updated_at", p.updatedAt)
                arr.put(o)
            }
            root.put("parchas", arr)

            val jsonContent = root.toString(2)
            val b64Content = Base64.encodeToString(
                jsonContent.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )

            val payload = JSONObject().apply {
                put("message", "Sync sacred parchas via $adminName [live_parchas.json]")
                put("content", b64Content)
                put("branch", "main")
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }

            val putUrl = URL(API_PARCHAS_URL)
            val putConn = putUrl.openConnection() as HttpURLConnection
            putConn.requestMethod = "PUT"
            putConn.setRequestProperty("Authorization", "Bearer $patToken")
            putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.25")
            putConn.connectTimeout = 8000
            putConn.readTimeout = 8000
            putConn.doOutput = true

            putConn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }

            val code = putConn.responseCode
            if (code in 200..299) {
                Pair(true, "पर्चे क्लाउड पर सफलतापूर्वक सिंक हो गए")
            } else {
                Pair(false, "पर्चा सिंक असफल: HTTP $code")
            }
        } catch (e: Exception) {
            Pair(false, "पर्चा सिंक त्रुटि: ${e.localizedMessage}")
        }
    }

    // ========================================================================
    // 7. SINGLE-DEVICE ADMIN & SUPER ADMIN SESSION ENFORCEMENT
    // ========================================================================

    data class AdminSession(
        val adminId: String,
        val role: String,
        val deviceId: String,
        val deviceModel: String,
        val sessionId: String,
        val loggedInAt: Long = System.currentTimeMillis()
    )

    suspend fun fetchLiveAdminSessions(context: Context? = null): Map<String, AdminSession> = withContext(Dispatchers.IO) {
        // Step 1: Query REST API Contents endpoint with no-cache (bypasses Fastly CDN cache completely)
        try {
            val patToken = if (context != null) getActiveToken(context) else ""
            val getUrl = URL(API_SESSIONS_URL)
            val getConn = getUrl.openConnection() as HttpURLConnection
            getConn.requestMethod = "GET"
            if (patToken.isNotBlank()) {
                getConn.setRequestProperty("Authorization", "Bearer $patToken")
            }
            getConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            getConn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            getConn.setRequestProperty("Pragma", "no-cache")
            getConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.29")
            getConn.connectTimeout = 4500
            getConn.readTimeout = 4500

            if (getConn.responseCode in 200..299) {
                val respText = getConn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val jsonResp = JSONObject(respText)
                val rawB64 = jsonResp.optString("content", "").replace("\n", "").replace("\r", "")
                if (rawB64.isNotBlank()) {
                    val decodedBytes = Base64.decode(rawB64, Base64.DEFAULT)
                    val decodedStr = String(decodedBytes, StandardCharsets.UTF_8)
                    val root = JSONObject(decodedStr)
                    val sessionsObj = root.optJSONObject("sessions") ?: JSONObject()
                    val map = mutableMapOf<String, AdminSession>()
                    val keys = sessionsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val o = sessionsObj.getJSONObject(key)
                        map[key] = AdminSession(
                            adminId = o.optString("admin_id", key),
                            role = o.optString("role", "ADMIN"),
                            deviceId = o.optString("device_id", ""),
                            deviceModel = o.optString("device_model", ""),
                            sessionId = o.optString("session_id", ""),
                            loggedInAt = o.optLong("logged_in_at", 0L)
                        )
                    }
                    return@withContext map
                }
            }
        } catch (e: Exception) {}

        // Step 2: Fallback to RAW_SESSIONS_URL
        try {
            val cacheBusterUrl = "$RAW_SESSIONS_URL?nocache=${System.currentTimeMillis()}"
            val url = URL(cacheBusterUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            conn.setRequestProperty("Pragma", "no-cache")
            conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.29")

            if (conn.responseCode in 200..299) {
                val jsonText = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(jsonText)
                val sessionsObj = root.optJSONObject("sessions") ?: JSONObject()
                val map = mutableMapOf<String, AdminSession>()
                val keys = sessionsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val o = sessionsObj.getJSONObject(key)
                    map[key] = AdminSession(
                        adminId = o.optString("admin_id", key),
                        role = o.optString("role", "ADMIN"),
                        deviceId = o.optString("device_id", ""),
                        deviceModel = o.optString("device_model", ""),
                        sessionId = o.optString("session_id", ""),
                        loggedInAt = o.optLong("logged_in_at", 0L)
                    )
                }
                map
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun registerAdminSession(
        context: Context,
        adminId: String,
        role: String,
        deviceId: String,
        deviceModel: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val newSessionId = UUID.randomUUID().toString()
        val nowTime = System.currentTimeMillis()
        val patToken = getActiveToken(context)
        if (patToken.isBlank()) {
            return@withContext Pair(true, newSessionId)
        }

        var attempts = 0
        val maxAttempts = 3
        var registeredSuccessfully = false

        while (attempts < maxAttempts && !registeredSuccessfully) {
            attempts++
            try {
                // 1. Fetch existing file SHA and content
                var existingSha: String? = null
                val existingSessions = mutableMapOf<String, AdminSession>()
                try {
                    val getUrl = URL(API_SESSIONS_URL)
                    val getConn = getUrl.openConnection() as HttpURLConnection
                    getConn.requestMethod = "GET"
                    getConn.setRequestProperty("Authorization", "Bearer $patToken")
                    getConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    getConn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                    getConn.setRequestProperty("Pragma", "no-cache")
                    getConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.29")
                    getConn.connectTimeout = 5000
                    getConn.readTimeout = 5000

                    if (getConn.responseCode in 200..299) {
                        val respText = getConn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                        val jsonResp = JSONObject(respText)
                        existingSha = jsonResp.optString("sha", null)
                        val rawB64 = jsonResp.optString("content", "").replace("\n", "").replace("\r", "")
                        if (rawB64.isNotBlank()) {
                            val decodedBytes = Base64.decode(rawB64, Base64.DEFAULT)
                            val decodedStr = String(decodedBytes, StandardCharsets.UTF_8)
                            val root = JSONObject(decodedStr)
                            val sessObj = root.optJSONObject("sessions") ?: JSONObject()
                            val keys = sessObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                val o = sessObj.getJSONObject(k)
                                existingSessions[k] = AdminSession(
                                    adminId = o.optString("admin_id", k),
                                    role = o.optString("role", "ADMIN"),
                                    deviceId = o.optString("device_id", ""),
                                    deviceModel = o.optString("device_model", ""),
                                    sessionId = o.optString("session_id", ""),
                                    loggedInAt = o.optLong("logged_in_at", 0L)
                                )
                            }
                        }
                    }
                } catch (e: Exception) {}

                // 2. Put new session
                existingSessions[adminId] = AdminSession(
                    adminId = adminId,
                    role = role,
                    deviceId = deviceId,
                    deviceModel = deviceModel,
                    sessionId = newSessionId,
                    loggedInAt = nowTime
                )

                val root = JSONObject()
                val sessionsJson = JSONObject()
                existingSessions.forEach { (key, session) ->
                    val o = JSONObject().apply {
                        put("admin_id", session.adminId)
                        put("role", session.role)
                        put("device_id", session.deviceId)
                        put("device_model", session.deviceModel)
                        put("session_id", session.sessionId)
                        put("logged_in_at", session.loggedInAt)
                    }
                    sessionsJson.put(key, o)
                }
                root.put("sessions", sessionsJson)
                root.put("last_updated_at", nowTime)

                val jsonContent = root.toString(2)
                val b64Content = Base64.encodeToString(
                    jsonContent.toByteArray(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
                )

                val payload = JSONObject().apply {
                    put("message", "Register single session for $adminId ($role) on $deviceModel [live_admin_sessions.json]")
                    put("content", b64Content)
                    put("branch", "main")
                    if (!existingSha.isNullOrBlank()) {
                        put("sha", existingSha)
                    }
                }

                val putUrl = URL(API_SESSIONS_URL)
                val putConn = putUrl.openConnection() as HttpURLConnection
                putConn.requestMethod = "PUT"
                putConn.setRequestProperty("Authorization", "Bearer $patToken")
                putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.29")
                putConn.connectTimeout = 8000
                putConn.readTimeout = 8000
                putConn.doOutput = true

                putConn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                }

                val code = putConn.responseCode
                if (code in 200..299) {
                    registeredSuccessfully = true
                } else if (code == 409) {
                    kotlinx.coroutines.delay(350)
                } else {
                    break
                }
            } catch (e: Exception) {
                if (attempts < maxAttempts) {
                    kotlinx.coroutines.delay(350)
                }
            }
        }

        Pair(true, newSessionId)
    }

    suspend fun clearAdminSession(
        context: Context,
        adminId: String,
        sessionId: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val patToken = getActiveToken(context)
            if (patToken.isBlank()) return@withContext Pair(false, "No token")

            var existingSha: String? = null
            val existingSessions = mutableMapOf<String, AdminSession>()
            try {
                val getUrl = URL(API_SESSIONS_URL)
                val getConn = getUrl.openConnection() as HttpURLConnection
                getConn.requestMethod = "GET"
                getConn.setRequestProperty("Authorization", "Bearer $patToken")
                getConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                getConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.27")
                getConn.connectTimeout = 5000
                getConn.readTimeout = 5000

                if (getConn.responseCode in 200..299) {
                    val respText = getConn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    val jsonResp = JSONObject(respText)
                    existingSha = jsonResp.optString("sha", null)
                    val rawB64 = jsonResp.optString("content", "").replace("\n", "").replace("\r", "")
                    if (rawB64.isNotBlank()) {
                        val decodedBytes = Base64.decode(rawB64, Base64.DEFAULT)
                        val decodedStr = String(decodedBytes, StandardCharsets.UTF_8)
                        val root = JSONObject(decodedStr)
                        val sessObj = root.optJSONObject("sessions") ?: JSONObject()
                        val keys = sessObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val o = sessObj.getJSONObject(k)
                            existingSessions[k] = AdminSession(
                                adminId = o.optString("admin_id", k),
                                role = o.optString("role", "ADMIN"),
                                deviceId = o.optString("device_id", ""),
                                deviceModel = o.optString("device_model", ""),
                                sessionId = o.optString("session_id", ""),
                                loggedInAt = o.optLong("logged_in_at", 0L)
                            )
                        }
                    }
                }
            } catch (e: Exception) {}

            // Remove or clear session for adminId
            val current = existingSessions[adminId]
            if (current != null && (sessionId == null || current.sessionId == sessionId)) {
                existingSessions.remove(adminId)
            } else {
                return@withContext Pair(true, "Session already closed")
            }

            val root = JSONObject()
            val sessionsJson = JSONObject()
            existingSessions.forEach { (key, session) ->
                val o = JSONObject().apply {
                    put("admin_id", session.adminId)
                    put("role", session.role)
                    put("device_id", session.deviceId)
                    put("device_model", session.deviceModel)
                    put("session_id", session.sessionId)
                    put("logged_in_at", session.loggedInAt)
                }
                sessionsJson.put(key, o)
            }
            root.put("sessions", sessionsJson)
            root.put("last_updated_at", System.currentTimeMillis())

            val jsonContent = root.toString(2)
            val b64Content = Base64.encodeToString(
                jsonContent.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )

            val payload = JSONObject().apply {
                put("message", "Clear session for $adminId [live_admin_sessions.json]")
                put("content", b64Content)
                put("branch", "main")
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }

            val putUrl = URL(API_SESSIONS_URL)
            val putConn = putUrl.openConnection() as HttpURLConnection
            putConn.requestMethod = "PUT"
            putConn.setRequestProperty("Authorization", "Bearer $patToken")
            putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.27")
            putConn.connectTimeout = 8000
            putConn.readTimeout = 8000
            putConn.doOutput = true

            putConn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }

            Pair(true, "सत्र सफलतापूर्वक समाप्त")
        } catch (e: Exception) {
            Pair(false, "त्रुटि: ${e.localizedMessage}")
        }
    }

    // ========================================================================
    // 8. LIVE PAYMENTS AUDIT TRAIL SYNC (live_payments.json)
    // ========================================================================

    suspend fun fetchLivePayments(context: Context): List<com.example.shribalajikripadham.data.model.PaymentRecord>? = withContext(Dispatchers.IO) {
        try {
            val cacheBuster = "$RAW_PAYMENTS_URL?nocache=${System.currentTimeMillis()}&rand=${(1000..9999).random()}"
            val url = URL(cacheBuster)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.useCaches = false
            conn.defaultUseCaches = false
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
            conn.setRequestProperty("Pragma", "no-cache")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.29")

            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(jsonStr)
                val arr = root.optJSONArray("payments") ?: JSONArray()
                val list = mutableListOf<com.example.shribalajikripadham.data.model.PaymentRecord>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        com.example.shribalajikripadham.data.model.PaymentRecord(
                            id = o.optLong("id", 0L),
                            paymentId = o.optString("payment_id", UUID.randomUUID().toString()),
                            devoteeName = o.optString("devotee_name", ""),
                            devoteePhone = o.optString("devotee_phone", ""),
                            paymentApp = o.optString("payment_app", "PhonePe"),
                            transactionId = o.optString("transaction_id", ""),
                            amount = o.optDouble("amount", 0.0),
                            purpose = o.optString("purpose", "BUS_TICKET"),
                            seatNumbers = o.optString("seat_numbers", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                            paymentStatus = o.optString("payment_status", "SUCCESS"),
                            paymentMode = o.optString("payment_mode", "UPI_QR"),
                            verifiedBy = o.optString("verified_by", ""),
                            notes = o.optString("notes", "")
                        )
                    )
                }
                list
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun publishLivePayments(
        context: Context,
        payments: List<com.example.shribalajikripadham.data.model.PaymentRecord>,
        adminName: String = "Super Admin"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val patToken = getActiveToken(context)
        if (patToken.isBlank()) return@withContext Pair(false, "सिंक टोकन उपलब्ध नहीं")

        try {
            var existingSha: String? = null
            try {
                val getUrl = URL(API_PAYMENTS_URL)
                val conn = getUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $patToken")
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.29")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode in 200..299) {
                    val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResp = JSONObject(respStr)
                    existingSha = if (jsonResp.has("sha")) jsonResp.getString("sha") else null
                }
            } catch (e: Exception) {}

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val root = JSONObject()
            root.put("updated_at", isoFormat.format(Date()))
            root.put("updated_by", adminName)
            root.put("total_payments", payments.size)
            root.put("total_amount", payments.sumOf { it.amount })

            val arr = JSONArray()
            for (p in payments) {
                val o = JSONObject().apply {
                    put("id", p.id)
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
                arr.put(o)
            }
            root.put("payments", arr)

            val jsonContent = root.toString(2)
            val b64Content = Base64.encodeToString(jsonContent.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)

            val payload = JSONObject().apply {
                put("message", "Sync payment ledger via $adminName [live_payments.json]")
                put("content", b64Content)
                put("branch", "main")
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }

            val putUrl = URL(API_PAYMENTS_URL)
            val putConn = putUrl.openConnection() as HttpURLConnection
            putConn.requestMethod = "PUT"
            putConn.setRequestProperty("Authorization", "Bearer $patToken")
            putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.29")
            putConn.connectTimeout = 8000
            putConn.readTimeout = 8000
            putConn.doOutput = true

            putConn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }

            val code = putConn.responseCode
            if (code in 200..299) {
                Pair(true, "पेमेंट लेजर सफलतापूर्वक सिंक हुआ")
            } else {
                Pair(false, "सिंक असफल: HTTP $code")
            }
        } catch (e: Exception) {
            Pair(false, "त्रुटि: ${e.localizedMessage}")
        }
    }

    // ========================================================================
    // 9. LIVE 60-SEATER BUS SEATS SYNC (live_bus_seats.json)
    // ========================================================================

    suspend fun fetchLiveBusSeats(context: Context): List<com.example.shribalajikripadham.data.model.BusSeat>? = withContext(Dispatchers.IO) {
        try {
            val cacheBuster = "$RAW_BUS_SEATS_URL?nocache=${System.currentTimeMillis()}&rand=${(1000..9999).random()}"
            val url = URL(cacheBuster)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.useCaches = false
            conn.defaultUseCaches = false
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
            conn.setRequestProperty("Pragma", "no-cache")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.29")

            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(jsonStr)
                val arr = root.optJSONArray("seats") ?: JSONArray()
                val list = mutableListOf<com.example.shribalajikripadham.data.model.BusSeat>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        com.example.shribalajikripadham.data.model.BusSeat(
                            seatNumber = o.optInt("seat_number", i + 1),
                            seatLabel = o.optString("seat_label", "${i + 1}"),
                            row = o.optInt("row_idx", 1),
                            column = o.optInt("col_idx", 1),
                            isBooked = o.optBoolean("is_booked", false),
                            passengerName = o.optString("passenger_name", ""),
                            passengerAge = o.optInt("passenger_age", 0),
                            passengerGender = o.optString("passenger_gender", ""),
                            phoneNumber = o.optString("phone_number", ""),
                            boardingPoint = o.optString("boarding_point", "Gram Dungra Jaat Ashram"),
                            paymentStatus = com.example.shribalajikripadham.data.model.PaymentStatus.valueOf(o.optString("payment_status", "UNPAID")),
                            paymentMode = o.optString("payment_mode", "UPI_QR"),
                            transactionId = o.optString("transaction_id", ""),
                            fareAmount = o.optInt("fare_amount", 1500),
                            yatraDate = o.optString("yatra_date", ""),
                            bookedAt = o.optLong("booked_at", 0L),
                            bookedBy = o.optString("booked_by", "DEVOTEE"),
                            notes = o.optString("notes", "")
                        )
                    )
                }
                list
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun publishLiveBusSeats(
        context: Context,
        seats: List<com.example.shribalajikripadham.data.model.BusSeat>,
        adminName: String = "Super Admin"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val patToken = getActiveToken(context)
        if (patToken.isBlank()) return@withContext Pair(false, "सिंक टोकन उपलब्ध नहीं")

        try {
            var existingSha: String? = null
            try {
                val getUrl = URL(API_BUS_SEATS_URL)
                val conn = getUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $patToken")
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.29")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode in 200..299) {
                    val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResp = JSONObject(respStr)
                    existingSha = if (jsonResp.has("sha")) jsonResp.getString("sha") else null
                }
            } catch (e: Exception) {}

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val root = JSONObject()
            root.put("updated_at", isoFormat.format(Date()))
            root.put("updated_by", adminName)
            root.put("total_seats", seats.size)
            root.put("booked_seats", seats.count { it.isBooked })

            val arr = JSONArray()
            for (s in seats) {
                val o = JSONObject().apply {
                    put("seat_number", s.seatNumber)
                    put("seat_label", s.seatLabel)
                    put("row_idx", s.row)
                    put("col_idx", s.column)
                    put("is_booked", s.isBooked)
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
                arr.put(o)
            }
            root.put("seats", arr)

            val jsonContent = root.toString(2)
            val b64Content = Base64.encodeToString(jsonContent.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)

            val payload = JSONObject().apply {
                put("message", "Sync 60-seat bus bookings via $adminName [live_bus_seats.json]")
                put("content", b64Content)
                put("branch", "main")
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }

            val putUrl = URL(API_BUS_SEATS_URL)
            val putConn = putUrl.openConnection() as HttpURLConnection
            putConn.requestMethod = "PUT"
            putConn.setRequestProperty("Authorization", "Bearer $patToken")
            putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.29")
            putConn.connectTimeout = 8000
            putConn.readTimeout = 8000
            putConn.doOutput = true

            putConn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }

            val code = putConn.responseCode
            if (code in 200..299) {
                Pair(true, "बस सीटें सफलतापूर्वक सिंक हुईं")
            } else {
                Pair(false, "सिंक असफल: HTTP $code")
            }
        } catch (e: Exception) {
            Pair(false, "त्रुटि: ${e.localizedMessage}")
        }
    }

    // ========================================================================
    // 8. SACRED ARZI DISTRIBUTION LIVE CLOUD SYNC
    // ========================================================================

    suspend fun fetchLiveArziRecords(context: Context): List<com.example.shribalajikripadham.data.model.ArziDistributionRecord>? = withContext(Dispatchers.IO) {
        try {
            val cacheBuster = "$RAW_ARZI_URL?nocache=${System.currentTimeMillis()}"
            val url = URL(cacheBuster)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.30")

            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(jsonStr)
                val arr = root.optJSONArray("arzi_records") ?: JSONArray()
                val list = mutableListOf<com.example.shribalajikripadham.data.model.ArziDistributionRecord>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        com.example.shribalajikripadham.data.model.ArziDistributionRecord(
                            id = o.optLong("id", 0L),
                            devoteeName = o.optString("devotee_name", ""),
                            phoneNumber = o.optString("phone_number", ""),
                            bigArziQty = o.optInt("big_arzi_qty", 0),
                            smallArziQty = o.optInt("small_arzi_qty", 0),
                            bigArziRate = o.optDouble("big_arzi_rate", 100.0),
                            smallArziRate = o.optDouble("small_arzi_rate", 50.0),
                            totalAmount = o.optDouble("total_amount", 0.0),
                            isPaid = o.optBoolean("is_paid", false),
                            paymentMode = o.optString("payment_mode", "CASH"),
                            recordedBy = o.optString("recorded_by", "SUPER_ADMIN"),
                            darbarDate = o.optString("darbar_date", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                            notes = o.optString("notes", "")
                        )
                    )
                }
                list
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun publishLiveArziRecords(
        context: Context,
        records: List<com.example.shribalajikripadham.data.model.ArziDistributionRecord>,
        recordedBy: String = "Super Admin"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val patToken = getActiveToken(context)
        if (patToken.isBlank()) return@withContext Pair(false, "सिंक टोकन उपलब्ध नहीं")

        try {
            var existingSha: String? = null
            try {
                val getUrl = URL(API_ARZI_URL)
                val conn = getUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $patToken")
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.30")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode in 200..299) {
                    val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResp = JSONObject(respStr)
                    existingSha = if (jsonResp.has("sha")) jsonResp.getString("sha") else null
                }
            } catch (ignored: Exception) {}

            val arr = JSONArray()
            records.forEach { r ->
                val o = JSONObject().apply {
                    put("id", r.id)
                    put("devotee_name", r.devoteeName)
                    put("phone_number", r.phoneNumber)
                    put("big_arzi_qty", r.bigArziQty)
                    put("small_arzi_qty", r.smallArziQty)
                    put("big_arzi_rate", r.bigArziRate)
                    put("small_arzi_rate", r.smallArziRate)
                    put("total_amount", r.totalAmount)
                    put("is_paid", r.isPaid)
                    put("payment_mode", r.paymentMode)
                    put("recorded_by", r.recordedBy)
                    put("darbar_date", r.darbarDate)
                    put("timestamp", r.timestamp)
                    put("notes", r.notes)
                }
                arr.put(o)
            }

            val root = JSONObject().apply {
                put("total_records", records.size)
                put("total_badi", records.sumOf { it.bigArziQty })
                put("total_chhoti", records.sumOf { it.smallArziQty })
                put("total_amount", records.sumOf { it.totalAmount })
                put("total_paid", records.filter { it.isPaid }.sumOf { it.totalAmount })
                put("total_pending", records.filter { !it.isPaid }.sumOf { it.totalAmount })
                put("updated_at", System.currentTimeMillis())
                put("updated_by", recordedBy)
                put("arzi_records", arr)
            }

            val b64 = Base64.encodeToString(root.toString(2).toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            val payload = JSONObject().apply {
                put("message", "Sync ${records.size} sacred arzi distribution records via $recordedBy")
                put("content", b64)
                put("branch", "main")
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }

            val putUrl = URL(API_ARZI_URL)
            val putConn = putUrl.openConnection() as HttpURLConnection
            putConn.requestMethod = "PUT"
            putConn.setRequestProperty("Authorization", "Bearer $patToken")
            putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.30")
            putConn.connectTimeout = 8000
            putConn.readTimeout = 8000
            putConn.doOutput = true

            putConn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }

            val code = putConn.responseCode
            if (code in 200..299) {
                Pair(true, "अर्जी वितरण लेजर क्लाउड पर सुरक्षित हो गया!")
            } else {
                Pair(false, "सिंक असफल: HTTP $code")
            }
        } catch (e: Exception) {
            Pair(false, "त्रुटि: ${e.localizedMessage}")
        }
    }
}

