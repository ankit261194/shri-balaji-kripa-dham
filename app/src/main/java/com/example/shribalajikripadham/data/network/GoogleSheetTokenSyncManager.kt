package com.example.shribalajikripadham.data.network

import android.content.Context
import com.example.shribalajikripadham.data.model.Token
import com.example.shribalajikripadham.data.model.TokenStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

object GoogleSheetTokenSyncManager {
    private const val PREFS_NAME = "sbkd_google_sheet_prefs"
    private const val KEY_WEBHOOK_URL = "google_sheet_webhook_url"

    fun getWebhookUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_WEBHOOK_URL, "") ?: ""
    }

    fun saveWebhookUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WEBHOOK_URL, url.trim()).apply()
    }

    fun isConfigured(context: Context): Boolean {
        val url = getWebhookUrl(context)
        return url.isNotBlank() && url.startsWith("https://script.google.com/")
    }

    /**
     * Post a newly created token to the central Google Spreadsheet.
     * Works for both devotee-created and admin-created tokens.
     */
    suspend fun postTokenToSheet(
        context: Context,
        token: Token
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val webhookUrl = getWebhookUrl(context)
        if (webhookUrl.isBlank() || !webhookUrl.startsWith("https://script.google.com/")) {
            return@withContext Pair(false, "Google Sheet वेबहुक लिंक कॉन्फ़िगर नहीं है")
        }

        try {
            val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeStr = timeFormatter.format(Date(token.createdAt))

            val payload = JSONObject().apply {
                put("token_number", token.tokenNumber)
                put("darbar_date", token.darbarDate)
                put("time_str", timeStr)
                put("patient_name", token.patientName)
                put("phone_number", token.phoneNumber)
                put("city", token.city)
                put("registered_by", token.registeredBy)
                put("distance_km", token.distanceKm)
                put("status", token.status.name)
                put("has_photo", token.photoUri.isNotBlank())
                put("photo_uri", token.photoUri)
            }

            var currentUrl = webhookUrl
            var redirectCount = 0
            var finalCode = -1

            while (redirectCount < 4) {
                val url = URL(currentUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.6")

                if (redirectCount == 0) {
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.doOutput = true
                    conn.outputStream.use { os ->
                        os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                    }
                } else {
                    conn.requestMethod = "GET"
                }

                finalCode = conn.responseCode
                if (finalCode in 300..399) {
                    val newLocation = conn.getHeaderField("Location")
                    if (!newLocation.isNullOrBlank()) {
                        currentUrl = newLocation
                        redirectCount++
                        continue
                    }
                }

                if (finalCode in 200..299) {
                    val respText = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    return@withContext Pair(true, "टोकन Google Sheet में सफलतापूर्वक दर्ज हुआ!")
                } else {
                    break
                }
            }

            Pair(false, "Google Sheet सर्वर रिस्पॉन्स: HTTP $finalCode")
        } catch (e: Exception) {
            Pair(false, "सिंक त्रुटि: ${e.localizedMessage ?: "नेटवर्क उपलब्ध नहीं"}")
        }
    }

    /**
     * Post a batch of tokens (e.g. from Paper Register Scan) to Google Spreadsheet.
     */
    suspend fun postBatchTokensToSheet(
        context: Context,
        tokens: List<Token>
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val webhookUrl = getWebhookUrl(context)
        if (webhookUrl.isBlank() || !webhookUrl.startsWith("https://script.google.com/")) {
            return@withContext Pair(false, "Google Sheet वेबहुक लिंक कॉन्फ़िगर नहीं है")
        }
        if (tokens.isEmpty()) return@withContext Pair(true, "कोई टोकन नहीं")

        try {
            val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val tokenArray = org.json.JSONArray()

            for (token in tokens) {
                val timeStr = timeFormatter.format(Date(token.createdAt))
                val item = JSONObject().apply {
                    put("token_number", token.tokenNumber)
                    put("darbar_date", token.darbarDate)
                    put("time_str", timeStr)
                    put("patient_name", token.patientName)
                    put("phone_number", token.phoneNumber)
                    put("city", token.city)
                    put("registered_by", token.registeredBy)
                    put("distance_km", token.distanceKm)
                    put("status", token.status.name)
                    put("has_photo", token.photoUri.isNotBlank())
                    put("photo_uri", token.photoUri)
                }
                tokenArray.put(item)
            }

            val payload = JSONObject().apply {
                put("action", "BATCH_TOKENS")
                put("tokens", tokenArray)
            }

            var currentUrl = webhookUrl
            var redirectCount = 0
            var finalCode = -1

            while (redirectCount < 4) {
                val url = URL(currentUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 12000
                conn.readTimeout = 12000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.9")

                if (redirectCount == 0) {
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.doOutput = true
                    conn.outputStream.use { os ->
                        os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                    }
                } else {
                    conn.requestMethod = "GET"
                }

                finalCode = conn.responseCode
                if (finalCode in 300..399) {
                    val newLocation = conn.getHeaderField("Location")
                    if (!newLocation.isNullOrBlank()) {
                        currentUrl = newLocation
                        redirectCount++
                        continue
                    }
                }

                if (finalCode in 200..299) {
                    return@withContext Pair(true, "${tokens.size} टोकन Google Sheet में सफलतापूर्वक दर्ज हुए!")
                } else {
                    break
                }
            }

            Pair(false, "Google Sheet सर्वर रिस्पॉन्स: HTTP $finalCode")
        } catch (e: Exception) {
            Pair(false, "सिंक त्रुटि: ${e.localizedMessage ?: "नेटवर्क उपलब्ध नहीं"}")
        }
    }

    /**
     * Fetch all tokens from Google Spreadsheet for today.
     * Allows Admin to sync all devotee submissions into Admin app.
     */
    suspend fun fetchTokensFromSheet(
        context: Context,
        date: String = ""
    ): List<Token> = withContext(Dispatchers.IO) {
        val webhookUrl = getWebhookUrl(context)
        if (webhookUrl.isBlank() || !webhookUrl.startsWith("https://script.google.com/")) {
            return@withContext emptyList()
        }

        val list = mutableListOf<Token>()
        try {
            var currentUrl = if (date.isNotBlank()) "$webhookUrl?date=$date" else webhookUrl
            var redirectCount = 0

            while (redirectCount < 4) {
                val url = URL(currentUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.6")

                val code = conn.responseCode
                if (code in 300..399) {
                    val newLocation = conn.getHeaderField("Location")
                    if (!newLocation.isNullOrBlank()) {
                        currentUrl = newLocation
                        redirectCount++
                        continue
                    }
                }

                if (code in 200..299) {
                    val jsonStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    val root = JSONObject(jsonStr)
                    val arr = root.optJSONArray("tokens")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val tokenNum = item.optInt("token_number", i + 1)
                            val darbarDate = item.optString("darbar_date", "")
                            val patientName = item.optString("patient_name", "")
                            val phone = item.optString("phone_number", "")
                            val city = item.optString("city", "")
                            val regBy = item.optString("registered_by", "GOOGLE_SHEET")
                            val dist = item.optDouble("distance_km", 0.0).toFloat()
                            val statusStr = item.optString("status", "WAITING")
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
                                    deviceId = "SHEET_SYNC",
                                    latitude = 0.0,
                                    longitude = 0.0,
                                    status = status,
                                    registeredBy = regBy,
                                    photoUri = "",
                                    isDarshanCompleted = status == TokenStatus.COMPLETED,
                                    darshanCompletedAt = 0L,
                                    originAddress = city,
                                    destinationAddress = "श्री बालाजी कृपा धाम, डुंगरा जाट",
                                    distanceKm = dist,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
                break
            }
        } catch (e: Exception) {
            // Return empty list on failure
        }
        list
    }
}
