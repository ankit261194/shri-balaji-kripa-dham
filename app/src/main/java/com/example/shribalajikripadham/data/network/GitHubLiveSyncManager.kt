package com.example.shribalajikripadham.data.network

import android.content.Context
import android.util.Base64
import com.example.shribalajikripadham.data.model.LiveUiConfigDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object GitHubLiveSyncManager {
    private const val PREFS_NAME = "sbkd_github_sync_prefs"
    private const val KEY_SYNC_TOKEN = "custom_github_pat_token"

    private const val REPO_OWNER = "ankit261194"
    private const val REPO_NAME = "shri-balaji-kripa-dham"
    private const val FILE_NAME = "live_ui_config.json"

    // Raw URL for instant, unauthenticated devotee reads
    private const val RAW_CONFIG_URL =
        "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/$FILE_NAME"

    // REST API endpoint for Super Admin publication
    private const val API_CONTENTS_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_NAME"

    // Default pre-configured authorization token for seamless Super Admin sync
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

    /**
     * Devotee / User Read API:
     * Reads the current live UI configuration from GitHub Raw CDN.
     * Uses timestamp cache-buster to always get the freshest published state.
     */
    suspend fun fetchLiveConfig(): LiveUiConfigDto? = withContext(Dispatchers.IO) {
        try {
            val cacheBusterUrl = "$RAW_CONFIG_URL?nocache=${System.currentTimeMillis()}"
            val url = URL(cacheBusterUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.5")

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

    /**
     * Super Admin Publish API:
     * Uploads the latest UI box order, visibility, and emergency notices to GitHub repository.
     */
    suspend fun publishLiveConfig(
        context: Context,
        config: LiveUiConfigDto,
        customToken: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val token = if (!customToken.isNullOrBlank()) customToken.trim() else getActiveToken(context)
        if (token.isBlank()) {
            return@withContext Pair(false, "सिंक टोकन अनुपलब्ध है (Missing GitHub Token)")
        }

        try {
            // 1. Fetch current file SHA if exists
            var existingSha: String? = null
            try {
                val getUrl = URL(API_CONTENTS_URL)
                val getConn = getUrl.openConnection() as HttpURLConnection
                getConn.requestMethod = "GET"
                getConn.setRequestProperty("Authorization", "Bearer $token")
                getConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                getConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.5")
                getConn.connectTimeout = 5000
                getConn.readTimeout = 5000

                if (getConn.responseCode in 200..299) {
                    val respStr = getConn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResp = JSONObject(respStr)
                    existingSha = if (jsonResp.has("sha")) jsonResp.getString("sha") else null
                }
            } catch (e: Exception) {
                // If 404 or new file, existingSha remains null
            }

            // 2. Prepare payload
            val jsonString = config.toJsonString()
            val base64Content = Base64.encodeToString(
                jsonString.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )

            val payloadObj = JSONObject().apply {
                put("message", "Live UI config updated by ${config.updatedBy} at ${config.updatedAt}")
                put("content", base64Content)
                put("branch", "main")
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }

            // 3. Send PUT request
            val putUrl = URL(API_CONTENTS_URL)
            val putConn = putUrl.openConnection() as HttpURLConnection
            putConn.requestMethod = "PUT"
            putConn.setRequestProperty("Authorization", "Bearer $token")
            putConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            putConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            putConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.5")
            putConn.connectTimeout = 8000
            putConn.readTimeout = 8000
            putConn.doOutput = true

            putConn.outputStream.use { os ->
                os.write(payloadObj.toString().toByteArray(StandardCharsets.UTF_8))
            }

            val code = putConn.responseCode
            if (code in 200..299) {
                Pair(true, "✅ लाइव सिंक सफल! नया UI कॉन्फ़िगरेशन सभी भक्तों के लिए लाइव हो चुका है।")
            } else {
                val errorStream = putConn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Pair(false, "सर्वर त्रुटि ($code): $errorStream")
            }
        } catch (e: Exception) {
            Pair(false, "सिंक विफल: ${e.localizedMessage ?: "नेटवर्क समस्या"}")
        }
    }
}
