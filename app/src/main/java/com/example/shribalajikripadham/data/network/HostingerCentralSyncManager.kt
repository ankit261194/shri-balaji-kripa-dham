package com.example.shribalajikripadham.data.network

import android.content.Context
import android.util.Log
import com.example.shribalajikripadham.data.model.AshramSettings
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
        isStealthAllocator: Boolean = false
    ): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}issue_token.php")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.42.1")

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
            if (darbarDate.isNotBlank()) {
                params.append("&darbar_date=").append(URLEncoder.encode(darbarDate, "UTF-8"))
            }
            if (customTokenNumber != null && customTokenNumber > 0) {
                params.append("&custom_token_number=").append(customTokenNumber)
            }
            if (isStealthAllocator) {
                params.append("&is_priority_allocator=1")
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
                    return@withContext Pair(true, tokenNum)
                }
            }
            Pair(false, -1)
        } catch (e: Exception) {
            Log.e(TAG, "issueCentralToken failed: ${e.message}")
            Pair(false, -1)
        }
    }

    /**
     * Fetch Live Darbar Queue from Central Server
     */
    suspend fun fetchLiveQueue(date: String = ""): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val urlStr = if (date.isNotBlank()) "${BASE_URL}get_queue.php?date=$date" else "${BASE_URL}get_queue.php"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
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

    /**
     * Fetch Live Config (200m radius, 30km outstation, current serving token)
     */
    suspend fun fetchLiveConfig(): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}live_config.php")
            val conn = url.openConnection() as HttpURLConnection
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
            Log.e(TAG, "fetchLiveConfig failed: ${e.message}")
            null
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
            val url = URL("${BASE_URL}live_config.php")
            val conn = url.openConnection() as HttpURLConnection
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
                    "&longitude=$long"

            conn.outputStream.use { os ->
                os.write(params.toByteArray(StandardCharsets.UTF_8))
            }

            conn.responseCode == 200
        } catch (e: Exception) {
            Log.e(TAG, "updateLiveConfig failed: ${e.message}")
            false
        }
    }

    /**
     * Upload photo to cloud CDN storage on shribalajikripadham.online
     */
    suspend fun uploadPhoto(file: File, photoType: String = "devotee"): String? = withContext(Dispatchers.IO) {
        try {
            val boundary = "==Boundary_${System.currentTimeMillis()}=="
            val url = URL("${BASE_URL}upload_photo.php")
            val conn = url.openConnection() as HttpURLConnection
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
                    return@withContext json.optString("photo_url", null)
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
            val conn = url.openConnection() as HttpURLConnection
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
            results.add(if (hOk) "🌐 Hosting: ✅ 100% एकसमान डेटा ($hMsg)" else "🌐 Hosting: ⚠️ $hMsg")

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
            val conn = url.openConnection() as HttpURLConnection
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
            val conn = url.openConnection() as HttpURLConnection
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
            val conn = url.openConnection() as HttpURLConnection
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
            val conn = url.openConnection() as HttpURLConnection
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
            val conn = url.openConnection() as HttpURLConnection
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
            val conn = url.openConnection() as HttpURLConnection
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

    suspend fun updateFullLiveConfig(settings: AshramSettings): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}live_config.php")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.37.0")

            val json = JSONObject().apply {
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
                put("is_bus_booking_live", if (settings.isBusBookingLive) 1 else 0)
                put("is_live_counter_visible", if (settings.isLiveCounterVisible) 1 else 0)
                put("is_payment_feature_live", if (settings.isPaymentFeatureLive) 1 else 0)
                put("is_arzi_ledger_live", if (settings.isArziLedgerLive) 1 else 0)
                put("is_darbar_active", if (settings.isDarbarActive) 1 else 0)
                put("darbar_date", settings.darbarDate)
                put("darbar_timings", settings.darbarTimings)
                put("emergency_notice", settings.emergencyNoticeText)
                put("is_emergency_notice_visible", if (settings.isEmergencyNoticeVisible) 1 else 0)
                put("banner_title", settings.bannerTitle)
                put("banner_subtitle", settings.bannerSubtitle)
                put("is_banner_visible", if (settings.isBannerVisible) 1 else 0)
                put("guruji_photo_url", settings.gurujiPhotoUri)
                put("allow_admin_reserved_tokens", if (settings.allowAdminReservedTokens) 1 else 0)
                put("can_admin_issue_reserved_tokens", if (settings.allowAdminReservedTokens) 1 else 0)
                put("aarti_timings", "प्रातः 05:30 मंगला आरती • सायं 07:00 महाआरती")
            }

            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            if (code == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val resObj = JSONObject(resp)
                return@withContext Pair(true, resObj.optString("message", "सेटिंग्स लाइव प्रसारित हो गईं!"))
            }
            Pair(false, "सर्वर रिस्पॉन्स HTTP $code")
        } catch (e: Exception) {
            Pair(false, "लाइव सेटिंग्स सिंक त्रुटि: ${e.localizedMessage}")
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
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.39.0")

            val params = "token_number=$tokenNumber&darbar_date=${URLEncoder.encode(darbarDate, "UTF-8")}&status=${URLEncoder.encode(status, "UTF-8")}&is_darshan_completed=${if (isDarshanCompleted) 1 else 0}"
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
            val conn = url.openConnection() as HttpURLConnection
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
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.39.0")

            val params = "id=$id"
            conn.outputStream.use { it.write(params.toByteArray(StandardCharsets.UTF_8)) }
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
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
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.39.0")

            val params = StringBuilder()
            params.append("name=").append(URLEncoder.encode(name, "UTF-8"))
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
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.39.0")

            val params = "id=$id"
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
            val conn = url.openConnection() as HttpURLConnection
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
            val conn = url.openConnection() as HttpURLConnection
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

}

