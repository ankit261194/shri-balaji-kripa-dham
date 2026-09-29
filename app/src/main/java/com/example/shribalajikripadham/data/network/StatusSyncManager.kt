package com.example.shribalajikripadham.data.network

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.shribalajikripadham.data.model.BhaktiStatusItem
import com.example.shribalajikripadham.data.model.DailySuvichar
import com.example.shribalajikripadham.data.model.StatusViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object StatusSyncManager {

    private const val TAG = "StatusSyncManager"
    private const val STATUS_API_URL = "https://shribalajikripadham.online/api/status_service.php"

    /**
     * Fetch active statuses and daily divine suvichar
     */
    suspend fun fetchActiveStatuses(): Result<Pair<List<BhaktiStatusItem>, DailySuvichar>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$STATUS_API_URL?action=get_active_statuses&t=${System.currentTimeMillis()}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(jsonStr)
                if (root.optBoolean("success", false)) {
                    val arr = root.optJSONArray("statuses") ?: org.json.JSONArray()
                    val list = mutableListOf<BhaktiStatusItem>()
                    for (i in 0 until arr.length()) {
                        list.add(BhaktiStatusItem.fromJson(arr.getJSONObject(i)))
                    }
                    val suvichar = DailySuvichar.fromJson(root.optJSONObject("daily_suvichar"))
                    Result.success(Pair(list, suvichar))
                } else {
                    Result.failure(Exception(root.optString("error", "Failed to fetch statuses")))
                }
            } else {
                Result.failure(Exception("HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching active statuses", e)
            Result.failure(e)
        }
    }

    /**
     * Upload devotee status (compressed image + metadata)
     */
    suspend fun uploadDevoteeStatus(
        context: Context,
        deviceId: String,
        userName: String,
        phone: String,
        city: String,
        caption: String,
        bitmap: Bitmap
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Compress bitmap to JPEG ~85% quality
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

            val postParams = listOf(
                "action" to "upload_status",
                "device_id" to deviceId,
                "user_name" to userName,
                "phone_number" to phone,
                "city" to city,
                "caption" to caption,
                "image_base64" to base64
            ).joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }

            val url = URL(STATUS_API_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 25000
                readTimeout = 25000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

            conn.outputStream.use { os ->
                os.write(postParams.toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }

            val responseCode = conn.responseCode
            val responseStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val respStr = responseStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val root = JSONObject(respStr)

            if (root.optBoolean("success", false)) {
                Result.success(root.optString("message", "स्टेटस सफलतापूर्वक लग गया!"))
            } else {
                Result.failure(Exception(root.optString("error", "अपलोड विफल रहा")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading status", e)
            Result.failure(e)
        }
    }

    /**
     * Record status view
     */
    suspend fun recordStatusView(
        statusId: Long,
        viewerDeviceId: String,
        viewerName: String,
        viewerPhone: String
    ) = withContext(Dispatchers.IO) {
        try {
            val postParams = listOf(
                "action" to "record_view",
                "status_id" to statusId.toString(),
                "viewer_device_id" to viewerDeviceId,
                "viewer_name" to viewerName,
                "viewer_phone" to viewerPhone
            ).joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }

            val url = URL(STATUS_API_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

            conn.outputStream.use { it.write(postParams.toByteArray(StandardCharsets.UTF_8)) }
            conn.responseCode // trigger request
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record status view", e)
        }
    }

    /**
     * Fetch status viewers list for Admin
     */
    suspend fun fetchStatusViewers(statusId: Long): Result<List<StatusViewer>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$STATUS_API_URL?action=get_viewers&status_id=$statusId&t=${System.currentTimeMillis()}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
            }

            if (conn.responseCode == 200) {
                val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(respStr)
                if (root.optBoolean("success", false)) {
                    val arr = root.optJSONArray("viewers") ?: org.json.JSONArray()
                    val list = mutableListOf<StatusViewer>()
                    for (i in 0 until arr.length()) {
                        list.add(StatusViewer.fromJson(arr.getJSONObject(i)))
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception(root.optString("error", "व्यूअर्स फेच नहीं हो सके")))
                }
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin Delete Status
     */
    suspend fun deleteStatus(statusId: Long, adminDeviceId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val postParams = listOf(
                "action" to "delete_status",
                "status_id" to statusId.toString(),
                "admin_device_id" to adminDeviceId
            ).joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }

            val url = URL(STATUS_API_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

            conn.outputStream.use { it.write(postParams.toByteArray(StandardCharsets.UTF_8)) }
            val respStr = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(respStr)
            if (root.optBoolean("success", false)) {
                Result.success(root.optString("message", "स्टेटस डिलीट कर दिया गया"))
            } else {
                Result.failure(Exception(root.optString("error", "डिलीट नहीं हो सका")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
