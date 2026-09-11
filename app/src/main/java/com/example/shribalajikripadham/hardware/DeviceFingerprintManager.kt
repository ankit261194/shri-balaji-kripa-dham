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
        val drmHardwareId = getWidevineDrmId()
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

        // Assemble immutable hardware fingerprint composite
        val rawComposite = buildString {
            if (drmHardwareId.isNotBlank()) {
                append("WIDEVINE_DRM:").append(drmHardwareId).append(";")
            }
            append("SECURE_ANDROID_ID:").append(androidId).append(";")
            append("HW:").append(hardware).append(";")
            append("BOARD:").append(board).append(";")
            append("BOOTLOADER:").append(bootloader).append(";")
            append("BRAND:").append(brand).append(";")
            append("ASHRAM_SALT:SBKD_HARDWARE_LOCK_2026")
        }

        return sha256(rawComposite)
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
