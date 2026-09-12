package com.example.shribalajikripadham.data.network

import android.content.ContentValues
import android.content.Context
import com.example.shribalajikripadham.ai.FaceEmbeddingEngine
import com.example.shribalajikripadham.data.local.DatabaseHelper
import com.example.shribalajikripadham.data.model.DevoteeFaceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object CentralDevoteeSyncManager {

    /**
     * Upload or update a devotee profile and face embedding to the central Google Sheet registry.
     * This makes their profile available to ANY phone immediately.
     */
    suspend fun uploadDevoteeProfile(
        context: Context,
        profile: DevoteeFaceProfile,
        registeredBy: String = "APP"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val webhookUrl = GoogleSheetTokenSyncManager.getWebhookUrl(context)
        if (webhookUrl.isBlank() || !webhookUrl.startsWith("https://script.google.com/")) {
            return@withContext Pair(false, "Google Sheet वेबहुक लिंक कॉन्फ़िगर नहीं है")
        }

        try {
            val faceB64 = if (profile.faceVector.isNotEmpty()) {
                FaceEmbeddingEngine.vectorToBase64(profile.faceVector)
            } else {
                ""
            }

            val payload = JSONObject().apply {
                put("action", "UPSERT_DEVOTEE")
                put("phone_number", profile.phoneNumber)
                put("patient_name", profile.patientName)
                put("city", profile.city)
                put("face_vector_b64", faceB64)
                put("has_photo", profile.photoUri.isNotBlank() || faceB64.isNotBlank())
                put("registered_by", registeredBy)
            }

            var currentUrl = webhookUrl
            var redirectCount = 0
            var finalCode = -1

            while (redirectCount < 4) {
                val url = URL(currentUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.8")

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
                    return@withContext Pair(true, "भक्त का विवरण सेंट्रल रजिस्ट्री में सुरक्षित हुआ!")
                }

                break
            }

            Pair(false, "HTTP " + finalCode + ": Google Sheet से कनेक्ट नहीं हो सका")
        } catch (e: Exception) {
            Pair(false, "सिंक त्रुटि: " + (e.localizedMessage ?: "अज्ञात त्रुटि"))
        }
    }

    /**
     * Pull all devotee profiles and face vectors from Google Sheet and merge into local SQLite.
     * This enables ANY phone to recognize any devotee enrolled from any other phone.
     */
    suspend fun syncAllDevoteesFromCloud(
        context: Context,
        dbHelper: DatabaseHelper
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val webhookUrl = GoogleSheetTokenSyncManager.getWebhookUrl(context)
        if (webhookUrl.isBlank() || !webhookUrl.startsWith("https://script.google.com/")) {
            return@withContext Pair(0, "Google Sheet लिंक सेट नहीं है")
        }

        try {
            val delimiter = if (webhookUrl.contains("?")) "&" else "?"
            var currentUrl = webhookUrl + delimiter + "action=get_registry"
            var redirectCount = 0
            var finalCode = -1
            var responseString = ""

            while (redirectCount < 4) {
                val url = URL(currentUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 12000
                conn.readTimeout = 12000
                conn.instanceFollowRedirects = true
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "ShriBalajiApp/2.8")

                finalCode = conn.responseCode
                if (finalCode in 300..399) {
                    val newLoc = conn.getHeaderField("Location")
                    if (!newLoc.isNullOrBlank()) {
                        currentUrl = newLoc
                        redirectCount++
                        continue
                    }
                }

                if (finalCode in 200..299) {
                    responseString = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    break
                }
                break
            }

            if (finalCode !in 200..299 || responseString.isBlank()) {
                return@withContext Pair(0, "क्लाउड से डेटा नहीं मिला (HTTP " + finalCode + ")")
            }

            val json = JSONObject(responseString)
            if (json.optString("status") != "SUCCESS") {
                return@withContext Pair(0, "त्रुटि: " + json.optString("message", "अमान्य प्रत्युत्तर"))
            }

            val array = json.optJSONArray("devotees") ?: return@withContext Pair(0, "कोई भक्त रिकॉर्ड नहीं मिला")
            val db = dbHelper.writableDatabase
            var updatedCount = 0

            db.beginTransaction()
            try {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val phone = item.optString("phone_number", "").trim()
                    val name = item.optString("patient_name", "").trim()
                    val city = item.optString("city", "डूँगरा जाट (स्थानीय)").trim()
                    val b64 = item.optString("face_vector_b64", "").trim()

                    if (phone.isBlank() && name.isBlank()) continue

                    val vector = if (b64.isNotBlank()) {
                        FaceEmbeddingEngine.base64ToVector(b64)
                    } else {
                        FloatArray(128)
                    }
                    val blob = FaceEmbeddingEngine.vectorToBlob(vector)

                    // Check if already in SQLite by phone
                    val cursor = db.rawQuery(
                        "SELECT id FROM devotee_face_profiles WHERE phone_number = ? LIMIT 1",
                        arrayOf(phone)
                    )
                    val exists = cursor.moveToFirst()
                    val existingId = if (exists) cursor.getLong(0) else -1L
                    cursor.close()

                    val cv = ContentValues().apply {
                        put("patient_name", name)
                        put("phone_number", phone)
                        put("city", if (city.isBlank()) "डूँगरा जाट (स्थानीय)" else city)
                        if (b64.isNotBlank()) {
                            put("face_vector", blob)
                        }
                        put("last_verified_at", System.currentTimeMillis())
                    }

                    if (exists && existingId > 0) {
                        db.update("devotee_face_profiles", cv, "id = ?", arrayOf(existingId.toString()))
                    } else {
                        cv.put("face_vector", blob)
                        cv.put("photo_uri", "")
                        cv.put("visit_count", 1)
                        cv.put("last_confidence", 1.0f)
                        cv.put("created_at", System.currentTimeMillis())
                        db.insert("devotee_face_profiles", null, cv)
                    }
                    updatedCount++
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            Pair(updatedCount, "सफलतापूर्वक " + updatedCount + " भक्तों का डेटा सिंक हुआ!")
        } catch (e: Exception) {
            Pair(0, "सिंक विफलता: " + (e.localizedMessage ?: "अज्ञात त्रुटि"))
        }
    }
}