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
        darbarDate: String = ""
    ): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}issue_token.php")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.36.0")

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
    suspend fun uploadPhoto(file: File): String? = withContext(Dispatchers.IO) {
        try {
            val boundary = "==Boundary_${System.currentTimeMillis()}=="
            val url = URL("${BASE_URL}upload_photo.php")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.36.0")

            conn.outputStream.use { os ->
                val sb = StringBuilder()
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
                distanceKm = token.distanceKm,
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
}
