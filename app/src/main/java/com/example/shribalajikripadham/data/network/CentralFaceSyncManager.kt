package com.example.shribalajikripadham.data.network

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.example.shribalajikripadham.ai.FaceEmbeddingEngine
import com.example.shribalajikripadham.data.local.DatabaseHelper
import com.example.shribalajikripadham.hardware.DeviceFingerprintManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object CentralFaceSyncManager {

    private const val TAG = "CentralFaceSync"
    private const val BASE_URL = "https://shribalajikripadham.online/api/"

    /**
     * Upload single devotee face profile with 192-d biometric embedding vector to Hostinger MySQL.
     */
    suspend fun uploadFaceProfile(
        context: Context,
        name: String,
        phone: String,
        city: String = "डूँगरा जाट (स्थानीय)",
        faceVector: FloatArray,
        photoUri: String = "",
        deviceId: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        if (name.isBlank() || phone.isBlank()) return@withContext false
        try {
            val url = URL("${BASE_URL}sync_face_profile.php")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.41.0")

            val b64Vector = FaceEmbeddingEngine.vectorToBase64(faceVector)
            val devId = if (deviceId.isNotBlank()) deviceId else DeviceFingerprintManager.getDeviceId(context)

            val cloudPhotoUrl = if (photoUri.isNotBlank() && !photoUri.startsWith("http://") && !photoUri.startsWith("https://")) {
                try {
                    val rawPath = photoUri.removePrefix("file://")
                    val f = File(rawPath)
                    if (f.exists() && f.length() > 0) {
                        HostingerCentralSyncManager.uploadPhoto(f) ?: photoUri
                    } else photoUri
                } catch (e: Exception) { photoUri }
            } else photoUri

            val json = JSONObject().apply {
                put("patient_name", name.trim())
                put("phone_number", phone.trim())
                put("city", if (city.isBlank()) "डूँगरा जाट (स्थानीय)" else city.trim())
                put("face_vector_b64", b64Vector)
                put("photo_url", cloudPhotoUrl.trim())
                put("registered_by", "SEVADAR_APP")
                put("device_id", devId)
            }

            conn.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }

            val code = conn.responseCode
            if (code == 200) {
                val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val j = JSONObject(resp)
                val success = j.optBoolean("success", false)
                Log.d(TAG, "Uploaded face profile for $name ($phone): success=$success")
                return@withContext success
            }
            Log.w(TAG, "Face upload returned HTTP $code")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading face profile: ${e.localizedMessage}")
            false
        }
    }

    /**
     * Download all enrolled face profiles from Hostinger MySQL and merge into local SQLite.
     * Guarantees seamless recognition across ALL sevadar devices and gates!
     */
    suspend fun fetchAndSyncFaceProfiles(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BASE_URL}get_face_profiles.php?limit=1000")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.41.0")

            val code = conn.responseCode
            if (code != 200) {
                Log.w(TAG, "get_face_profiles.php returned HTTP $code")
                return@withContext 0
            }

            val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val root = JSONObject(resp)
            if (!root.optBoolean("success", false)) return@withContext 0

            val profilesArray = root.optJSONArray("profiles") ?: return@withContext 0
            val dbHelper = DatabaseHelper(context)
            val db = dbHelper.writableDatabase

            var mergedCount = 0
            db.beginTransaction()
            try {
                for (i in 0 until profilesArray.length()) {
                    val p = profilesArray.getJSONObject(i)
                    val phone = p.optString("phone_number", "").trim()
                    val name = p.optString("patient_name", "").trim()
                    val city = p.optString("city", "डूँगरा जाट (स्थानीय)").trim()
                    val b64Vector = p.optString("face_vector_b64", "").trim()
                    val photoUrl = p.optString("photo_url", "").trim()
                    val visits = p.optInt("visit_count", 1)
                    val lastVerified = p.optLong("last_verified_at", System.currentTimeMillis())
                    val createdAt = p.optLong("created_at", System.currentTimeMillis())

                    if (phone.isBlank() || b64Vector.isBlank()) continue

                    val vector = FaceEmbeddingEngine.base64ToVector(b64Vector)
                    val blob = FaceEmbeddingEngine.vectorToBlob(vector)

                    val cv = ContentValues().apply {
                        put("patient_name", name)
                        put("phone_number", phone)
                        put("city", city)
                        put("face_vector", blob)
                        put("photo_uri", photoUrl)
                        put("visit_count", visits)
                        put("last_confidence", 1.0f)
                        put("last_verified_at", lastVerified)
                        put("created_at", createdAt)
                    }

                    val cur = db.rawQuery("SELECT id FROM devotee_face_profiles WHERE phone_number = ? LIMIT 1", arrayOf(phone))
                    if (cur.moveToFirst()) {
                        val existingId = cur.getLong(0)
                        db.update("devotee_face_profiles", cv, "id = ?", arrayOf(existingId.toString()))
                    } else {
                        db.insert("devotee_face_profiles", null, cv)
                    }
                    cur.close()
                    mergedCount++
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            Log.d(TAG, "Successfully synced $mergedCount face profiles from central MySQL!")
            mergedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading face profiles: ${e.localizedMessage}")
            0
        }
    }

    /**
     * Pushes all local profiles that exist in SQLite to central MySQL (backward compatibility backup).
     */
    suspend fun syncAllLocalProfilesToCentral(context: Context) = withContext(Dispatchers.IO) {
        try {
            val dbHelper = DatabaseHelper(context)
            val db = dbHelper.readableDatabase
            val cur = db.rawQuery("SELECT patient_name, phone_number, city, face_vector, photo_uri FROM devotee_face_profiles", null)
            val list = mutableListOf<Triple<String, String, String>>()
            val vectors = mutableListOf<FloatArray>()
            val photos = mutableListOf<String>()

            while (cur.moveToNext()) {
                val name = cur.getString(0) ?: ""
                val phone = cur.getString(1) ?: ""
                val city = cur.getString(2) ?: ""
                val blob = cur.getBlob(3)
                val photo = cur.getString(4) ?: ""
                if (name.isNotBlank() && phone.isNotBlank() && blob != null) {
                    list.add(Triple(name, phone, city))
                    vectors.add(FaceEmbeddingEngine.blobToVector(blob))
                    photos.add(photo)
                }
            }
            cur.close()

            for (i in list.indices) {
                val (name, phone, city) = list[i]
                uploadFaceProfile(context, name, phone, city, vectors[i], photos[i])
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncAllLocalProfilesToCentral error: ${e.localizedMessage}")
        }
    }
}
