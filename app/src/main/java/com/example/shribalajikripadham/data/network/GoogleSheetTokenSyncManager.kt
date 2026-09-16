package com.example.shribalajikripadham.data.network

import android.content.ContentValues
import android.content.Context
import com.example.shribalajikripadham.data.local.DatabaseHelper
import com.example.shribalajikripadham.data.model.ArziDistributionRecord
import com.example.shribalajikripadham.data.model.PaymentRecord
import com.example.shribalajikripadham.data.model.Token
import com.example.shribalajikripadham.data.model.TokenStatus
import com.example.shribalajikripadham.data.model.YatraExpense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
        val url = prefs.getString(KEY_WEBHOOK_URL, "") ?: ""
        if (url.isNotBlank()) return url.trim()

        // Fallback: check SQLite ashram_settings cloud_sync_url
        return try {
            val dbHelper = DatabaseHelper(context)
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT cloud_sync_url FROM ashram_settings WHERE id = 1 LIMIT 1", null)
            var dbUrl = ""
            if (cursor.moveToFirst()) {
                dbUrl = cursor.getString(0) ?: ""
            }
            cursor.close()
            if (dbUrl.isNotBlank()) {
                prefs.edit().putString(KEY_WEBHOOK_URL, dbUrl.trim()).apply()
            }
            dbUrl.trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun saveWebhookUrl(context: Context, url: String) {
        val trimmed = url.trim()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WEBHOOK_URL, trimmed).apply()

        // Synchronize to SQLite ashram_settings as well
        try {
            val dbHelper = DatabaseHelper(context)
            val db = dbHelper.writableDatabase
            val cv = ContentValues().apply {
                put("cloud_sync_url", trimmed)
                if (trimmed.isNotBlank()) {
                    put("is_cloud_sync_enabled", 1)
                }
            }
            db.update("ashram_settings", cv, "id = 1", null)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun isConfigured(context: Context): Boolean {
        val url = getWebhookUrl(context)
        return url.isNotBlank() && url.startsWith("https://script.google.com/")
    }

    /**
     * Reusable HTTP POST handler with automatic Google Apps Script 302 redirect following.
     */
    private fun executePost(webhookUrl: String, payload: JSONObject, timeoutMs: Int = 12000): Pair<Boolean, String> {
        return try {
            var currentUrl = webhookUrl
            var redirectCount = 0
            var finalCode = -1

            while (redirectCount < 4) {
                val url = URL(currentUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = timeoutMs
                conn.readTimeout = timeoutMs
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.34.4")

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
                    return Pair(true, respText)
                } else {
                    break
                }
            }

            Pair(false, "HTTP " + finalCode)
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "नेटवर्क त्रुटि")
        }
    }

    /**
     * Post a single newly generated token to Sunday_Tokens sheet.
     */
    suspend fun postTokenToSheet(
        context: Context,
        token: Token
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val webhookUrl = getWebhookUrl(context)
        if (!isConfigured(context)) {
            return@withContext Pair(false, "Google Sheet वेबहुक लिंक कॉन्फ़िगर नहीं है")
        }

        try {
            val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeStr = timeFormatter.format(Date(token.createdAt))

            val payload = JSONObject().apply {
                put("action", "RECORD_TOKEN")
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
            }

            val (success, resp) = executePost(webhookUrl, payload)
            if (success) {
                Pair(true, "टोकन #" + token.tokenNumber + " Google Sheet में दर्ज हुआ!")
            } else {
                Pair(false, "Google Sheet सिंक विफल: " + resp)
            }
        } catch (e: Exception) {
            Pair(false, "त्रुटि: " + (e.localizedMessage ?: "अज्ञात"))
        }
    }

    /**
     * Post a batch of tokens (e.g. from Paper Register OCR Scan) to Google Spreadsheet.
     */
    suspend fun postBatchTokensToSheet(
        context: Context,
        tokens: List<Token>
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val webhookUrl = getWebhookUrl(context)
        if (!isConfigured(context)) {
            return@withContext Pair(false, "Google Sheet वेबहुक लिंक कॉन्फ़िगर नहीं है")
        }
        if (tokens.isEmpty()) return@withContext Pair(true, "कोई टोकन नहीं")

        try {
            val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val tokenArray = JSONArray()

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
                }
                tokenArray.put(item)
            }

            val payload = JSONObject().apply {
                put("action", "BATCH_TOKENS")
                put("tokens", tokenArray)
            }

            val (success, resp) = executePost(webhookUrl, payload, timeoutMs = 15000)
            if (success) {
                Pair(true, "" + tokens.size + " टोकन Google Sheet में सफलतापूर्वक दर्ज हुए!")
            } else {
                Pair(false, "Google Sheet सिंक विफल: " + resp)
            }
        } catch (e: Exception) {
            Pair(false, "सिंक त्रुटि: " + (e.localizedMessage ?: "अज्ञात"))
        }
    }

    /**
     * Update token status in Google Sheet without deleting the row (Audit Ledger Compliant).
     */
    suspend fun updateTokenStatusInSheet(
        context: Context,
        darbarDate: String,
        tokenNumber: Int,
        newStatus: String,
        cancelReason: String = ""
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val webhookUrl = getWebhookUrl(context)
        if (!isConfigured(context)) {
            return@withContext Pair(false, "Google Sheet वेबहुक लिंक कॉन्फ़िगर नहीं है")
        }

        try {
            val payload = JSONObject().apply {
                put("action", "UPDATE_TOKEN_STATUS")
                put("darbar_date", darbarDate)
                put("token_number", tokenNumber)
                put("new_status", newStatus)
                put("cancel_reason", cancelReason)
            }

            val (success, resp) = executePost(webhookUrl, payload)
            if (success) {
                Pair(true, "टोकन #" + tokenNumber + " का स्टेटस शीट में अपडेट हुआ: " + newStatus)
            } else {
                Pair(false, "शीट स्टेटस अपडेट त्रुटि: " + resp)
            }
        } catch (e: Exception) {
            Pair(false, "त्रुटि: " + (e.localizedMessage ?: "अज्ञात"))
        }
    }

    /**
     * Post a Payment Record (Dharmashala room, Donation, Bus) to Dharmashala_Payments sheet.
     */
    suspend fun postPaymentToSheet(
        context: Context,
        payment: PaymentRecord
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val webhookUrl = getWebhookUrl(context)
        if (!isConfigured(context)) {
            return@withContext Pair(false, "Google Sheet वेबहुक लिंक कॉन्फ़िगर नहीं है")
        }

        try {
            val payload = JSONObject().apply {
                put("action", "RECORD_PAYMENT")
                put("payment_id", payment.paymentId)
                put("devotee_name", payment.devoteeName)
                put("devotee_phone", payment.devoteePhone)
                put("purpose", payment.purpose)
                put("amount", payment.amount)
                put("payment_mode", payment.paymentMode)
                put("transaction_id", payment.transactionId)
                put("verified_by", payment.verifiedBy)
                put("notes", payment.notes)
            }

            val (success, resp) = executePost(webhookUrl, payload)
            if (success) {
                Pair(true, "पेमेंट रसीद ₹" + payment.amount + " Google Sheet में दर्ज हुई!")
            } else {
                Pair(false, "पेमेंट शीट सिंक त्रुटि: " + resp)
            }
        } catch (e: Exception) {
            Pair(false, "त्रुटि: " + (e.localizedMessage ?: "अज्ञात"))
        }
    }

    /**
     * Post an Arzi distribution record to Arzi_Box_Ledger sheet.
     */
    suspend fun postArziToSheet(
        context: Context,
        arzi: ArziDistributionRecord
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val webhookUrl = getWebhookUrl(context)
        if (!isConfigured(context)) {
            return@withContext Pair(false, "Google Sheet वेबहुक लिंक कॉन्फ़िगर नहीं है")
        }

        try {
            val payload = JSONObject().apply {
                put("action", "RECORD_ARZI")
                put("darbar_date", arzi.darbarDate)
                put("devotee_name", arzi.devoteeName)
                put("phone_number", arzi.phoneNumber)
                put("big_arzi_qty", arzi.bigArziQty)
                put("small_arzi_qty", arzi.smallArziQty)
                put("total_amount", arzi.totalAmount)
                put("is_paid", arzi.isPaid)
                put("payment_mode", arzi.paymentMode)
                put("recorded_by", arzi.recordedBy)
                put("notes", arzi.notes)
            }

            val (success, resp) = executePost(webhookUrl, payload)
            if (success) {
                Pair(true, "अर्जी विवरण Google Sheet में दर्ज हुआ!")
            } else {
                Pair(false, "अर्जी शीट सिंक त्रुटि: " + resp)
            }
        } catch (e: Exception) {
            Pair(false, "त्रुटि: " + (e.localizedMessage ?: "अज्ञात"))
        }
    }

    /**
     * Post an expense to Daily_Expenses sheet.
     */
    suspend fun postExpenseToSheet(
        context: Context,
        expense: YatraExpense
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val webhookUrl = getWebhookUrl(context)
        if (!isConfigured(context)) {
            return@withContext Pair(false, "Google Sheet वेबहुक लिंक कॉन्फ़िगर नहीं है")
        }

        try {
            val payload = JSONObject().apply {
                put("action", "RECORD_EXPENSE")
                put("expense_date", expense.expenseDate)
                put("title", expense.title)
                put("category", expense.category.displayNameHindi)
                put("amount", expense.amount)
                put("added_by", expense.addedByAdminName)
                put("receipt_uri", expense.receiptUri)
            }

            val (success, resp) = executePost(webhookUrl, payload)
            if (success) {
                Pair(true, "खर्च विवरण Google Sheet में दर्ज हुआ!")
            } else {
                Pair(false, "खर्च शीट सिंक त्रुटि: " + resp)
            }
        } catch (e: Exception) {
            Pair(false, "त्रुटि: " + (e.localizedMessage ?: "अज्ञात"))
        }
    }

    /**
     * Fetch all tokens from Google Spreadsheet for a given date.
     */
    suspend fun fetchTokensFromSheet(
        context: Context,
        date: String = ""
    ): List<Token> = withContext(Dispatchers.IO) {
        val webhookUrl = getWebhookUrl(context)
        if (!isConfigured(context)) {
            return@withContext emptyList()
        }

        val list = mutableListOf<Token>()
        try {
            var currentUrl = if (date.isNotBlank()) webhookUrl + "?date=" + date else webhookUrl
            var redirectCount = 0

            while (redirectCount < 4) {
                val url = URL(currentUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.34.4")

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
                            val status = if (statusStr.contains("दर्शन") || statusStr.contains("COMPLETED")) {
                                TokenStatus.COMPLETED
                            } else if (statusStr.contains("रद्द") || statusStr.contains("CANCELLED")) {
                                TokenStatus.CANCELLED
                            } else {
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

    /**
     * Test connection to the webhook. Returns (Success, SpreadsheetName/Message).
     */
    suspend fun testConnection(context: Context): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val webhookUrl = getWebhookUrl(context)
        if (!isConfigured(context)) {
            return@withContext Pair(false, "कृपया वैध Google Apps Script URL दर्ज करें")
        }

        try {
            var currentUrl = webhookUrl + "?action=get_summary"
            var redirectCount = 0

            while (redirectCount < 4) {
                val url = URL(currentUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.34.4")

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
                    val ssName = root.optString("spreadsheet_name", "Google Spreadsheet")
                    val totTokens = root.optInt("total_tokens", 0)
                    return@withContext Pair(true, "सफल कनेक्शन! शीट: " + ssName + " (कुल टोकन: " + totTokens + ")")
                } else {
                    return@withContext Pair(false, "सर्वर रिस्पॉन्स कोड: HTTP " + code)
                }
            }
            Pair(false, "कनेक्शन टाइमआउट या रीडायरेक्ट सीमा पार")
        } catch (e: Exception) {
            Pair(false, "कनेक्शन विफल: " + (e.localizedMessage ?: "नेटवर्क अनुपलब्ध"))
        }
    }
}