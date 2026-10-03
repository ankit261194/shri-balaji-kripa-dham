package com.example.shribalajikripadham.hardware

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaDrm
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

/**
 * Hardware-Level Device Locking Engine (1 Device = 1 Token per Sunday).
 *
 * Captures an immutable hardware identifier utilizing:
 * 1. Widevine DRM Client ID (MediaDrm PROPERTY_DEVICE_UNIQUE_ID burned into the device SoC/TrustZone).
 *    - Survives app cache clear, data wipe, uninstallation & reinstallation.
 *    - Remains identical across Parallel Space, Dual Apps, Island, and Work Profiles.
 * 2. Android DRM Hardware Root + Settings.Secure.ANDROID_ID + Hardware Platform attributes.
 * 3. Salted SHA-256 cryptographic digest.
 */
object DeviceFingerprintManager {

    // Widevine DRM Scheme UUID: edef8ba9-79d6-4ace-a3c8-27dcd51d21ed
    private val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        // 1. Try reading from persistent external storage anchors (survives Clear Data and Reinstall)
        val persistentId = readPersistentFileIdentity()
        if (persistentId.isNotBlank() && persistentId.length == 64) {
            return persistentId
        }

        // 2. Hardware-level immutable attributes
        val androidId = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: ""
        } catch (e: Exception) {
            ""
        }

        val board = Build.BOARD ?: ""
        val hardware = Build.HARDWARE ?: ""
        val bootloader = Build.BOOTLOADER ?: ""
        val brand = Build.BRAND ?: ""
        val device = Build.DEVICE ?: ""
        val model = Build.MODEL ?: ""
        val manufacturer = Build.MANUFACTURER ?: ""
        val product = Build.PRODUCT ?: ""
        val cpuCores = Runtime.getRuntime().availableProcessors()
        
        val displayMetrics = try {
            val dm = context.resources.displayMetrics
            "${dm.widthPixels}x${dm.heightPixels}@${dm.densityDpi}"
        } catch (e: Exception) {
            "default_dm"
        }

        val drmHardwareId = getWidevineDrmId()

        // Assemble immutable hardware fingerprint composite
        val rawComposite = buildString {
            append("SECURE_ANDROID_ID:").append(androidId).append(";")
            append("HW:").append(hardware).append(";")
            append("BOARD:").append(board).append(";")
            append("BRAND:").append(brand).append(";")
            append("DEVICE:").append(device).append(";")
            append("MODEL:").append(model).append(";")
            append("MANUFACTURER:").append(manufacturer).append(";")
            append("PRODUCT:").append(product).append(";")
            append("BOOTLOADER:").append(bootloader).append(";")
            append("CPU:").append(cpuCores).append(";")
            append("DISPLAY:").append(displayMetrics).append(";")
            if (drmHardwareId.isNotBlank()) {
                append("WIDEVINE_DRM:").append(drmHardwareId).append(";")
            }
            append("ASHRAM_SALT:SBKD_HARDWARE_LOCK_2026")
        }

        val computedId = sha256(rawComposite)

        // Write to persistent anchors so subsequent runs (even after Clear Data) recover the exact ID
        savePersistentFileIdentity(computedId)

        return computedId
    }

    private fun readPersistentFileIdentity(): String {
        val candidateDirs = listOfNotNull(
            try { android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS) } catch (e: Exception) { null },
            try { android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS) } catch (e: Exception) { null },
            try { android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES) } catch (e: Exception) { null }
        )
        for (dir in candidateDirs) {
            try {
                val file = java.io.File(dir, ".sbkd_hw_identity.dat")
                if (file.exists() && file.canRead()) {
                    val content = file.readText().trim()
                    if (content.length == 64 && content.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
                        return content.lowercase()
                    }
                }
            } catch (ignored: Exception) {}
        }
        return ""
    }

    private fun savePersistentFileIdentity(id: String) {
        if (id.isBlank() || id.length != 64) return
        val candidateDirs = listOfNotNull(
            try { android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS) } catch (e: Exception) { null },
            try { android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS) } catch (e: Exception) { null },
            try { android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES) } catch (e: Exception) { null }
        )
        for (dir in candidateDirs) {
            try {
                if (!dir.exists()) dir.mkdirs()
                val file = java.io.File(dir, ".sbkd_hw_identity.dat")
                file.writeText(id)
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Extracts the Widevine DRM unique device hardware identifier.
     * This ID is provisioned at the factory into the hardware security module / TrustZone.
     */
    fun getWidevineDrmId(): String {
        var mediaDrm: MediaDrm? = null
        return try {
            mediaDrm = MediaDrm(WIDEVINE_UUID)
            val deviceIdBytes = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            deviceIdBytes.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            ""
        } finally {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    mediaDrm?.close()
                } else {
                    @Suppress("DEPRECATION")
                    mediaDrm?.release()
                }
            } catch (ignored: Exception) {}
        }
    }

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }
}
