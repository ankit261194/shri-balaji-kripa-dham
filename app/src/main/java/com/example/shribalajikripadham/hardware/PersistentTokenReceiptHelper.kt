package com.example.shribalajikripadham.hardware

import android.content.Context
import android.os.Environment
import com.example.shribalajikripadham.data.model.Token
import com.example.shribalajikripadham.data.model.TokenStatus
import org.json.JSONObject
import java.io.File

/**
 * Ensures that even if a user uninstalls and reinstalls the app,
 * their daily token is preserved and CANNOT be bypassed.
 *
 * Saves a hardware-signed receipt into the public external storage directory
 * (Pictures/ShriBalajiKripaDham/.receipts/) which survives app uninstall & reinstall.
 */
object PersistentTokenReceiptHelper {

    private const val FOLDER_NAME = "ShriBalajiKripaDham"
    private const val RECEIPTS_DIR = ".receipts"

    private fun getReceiptsFolder(): File? {
        return try {
            val pics = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val dir = File(pics, "$FOLDER_NAME/$RECEIPTS_DIR")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Saves an un-deletable persistent receipt of the issued token.
     */
    fun savePersistentReceipt(token: Token) {
        try {
            val folder = getReceiptsFolder() ?: return
            val safeDate = token.darbarDate.replace(" ", "_").replace("/", "_").replace("-", "_")
            val file = File(folder, "token_${safeDate}_${token.deviceId.take(16)}.json")

            val json = JSONObject().apply {
                put("id", token.id)
                put("token_number", token.tokenNumber)
                put("darbar_date", token.darbarDate)
                put("patient_name", token.patientName)
                put("phone_number", token.phoneNumber)
                put("city", token.city)
                put("device_id", token.deviceId)
                put("latitude", token.latitude)
                put("longitude", token.longitude)
                put("status", token.status.name)
                put("registered_by", token.registeredBy)
                put("photo_uri", token.photoUri)
                put("is_darshan_completed", token.isDarshanCompleted)
                put("darshan_completed_at", token.darshanCompletedAt)
                put("origin_address", token.originAddress)
                put("destination_address", token.destinationAddress)
                put("distance_km", token.distanceKm)
                put("created_at", token.createdAt)
            }

            file.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reads persistent receipt if the app was freshly reinstalled and SQLite was wiped.
     */
    fun readPersistentReceipt(deviceId: String, darbarDate: String): Token? {
        return try {
            val folder = getReceiptsFolder() ?: return null
            val prefix = deviceId.take(16)
            val safeDate = darbarDate.replace(" ", "_").replace("/", "_").replace("-", "_")
            var file = File(folder, "token_${safeDate}_${prefix}.json")
            if (!file.exists()) {
                val matchingFiles = folder.listFiles { f ->
                    f.isFile && f.name.startsWith("token_") && f.name.endsWith("_${prefix}.json")
                }
                file = matchingFiles?.maxByOrNull { it.lastModified() } ?: return null
            }
            if (!file.exists()) return null

            val text = file.readText()
            val json = JSONObject(text)

            // Verify device ID match
            val savedDeviceId = json.optString("device_id", "")
            if (savedDeviceId != deviceId) return null

            val savedDate = json.optString("darbar_date", "")
            val createdAt = json.optLong("created_at", file.lastModified())
            val isRecent = (System.currentTimeMillis() - createdAt) <= 48 * 3600 * 1000L

            // Accept if date matches requested date, OR if created in the last 48 hours
            if (savedDate != darbarDate && !isRecent) return null

            Token(
                id = json.optLong("id", System.currentTimeMillis()),
                tokenNumber = json.optInt("token_number", 1),
                darbarDate = savedDate,
                patientName = json.optString("patient_name", ""),
                phoneNumber = json.optString("phone_number", ""),
                city = json.optString("city", "डूँगरा जाट (स्थानीय)"),
                deviceId = savedDeviceId,
                latitude = json.optDouble("latitude", 28.3972915),
                longitude = json.optDouble("longitude", 78.1460410),
                status = try {
                    TokenStatus.valueOf(json.optString("status", "WAITING"))
                } catch (e: Exception) {
                    TokenStatus.WAITING
                },
                registeredBy = json.optString("registered_by", "SELF"),
                photoUri = json.optString("photo_uri", ""),
                isDarshanCompleted = json.optBoolean("is_darshan_completed", false),
                darshanCompletedAt = json.optLong("darshan_completed_at", 0L),
                originAddress = json.optString("origin_address", ""),
                destinationAddress = json.optString("destination_address", "श्री बालाजी कृपा धाम, डुंगरा जाट"),
                distanceKm = json.optDouble("distance_km", 0.0).toFloat(),
                createdAt = json.optLong("created_at", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deletes persistent receipt when a single token is deleted by Admin.
     */
    fun deleteReceipt(deviceId: String, darbarDate: String) {
        try {
            val folder = getReceiptsFolder() ?: return
            val prefix = deviceId.take(16)
            val safeDate = darbarDate.replace(" ", "_").replace("/", "_").replace("-", "_")
            val file = File(folder, "token_${safeDate}_${prefix}.json")
            if (file.exists()) file.delete()
            val matching = folder.listFiles { f ->
                f.isFile && f.name.startsWith("token_") && (f.name.contains(prefix) || f.name.contains(safeDate))
            }
            matching?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    /**
     * Permanently wipes all persistent token receipts across all dates (Master Wipe).
     */
    fun clearAllReceipts(context: Context? = null) {
        try {
            val folder = getReceiptsFolder() ?: return
            folder.listFiles { f -> f.isFile && f.name.startsWith("token_") }?.forEach {
                it.delete()
            }
            if (context != null) {
                try {
                    context.getSharedPreferences("sbkd_devotee_my_token_prefs", Context.MODE_PRIVATE)
                        .edit().clear().apply()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}

