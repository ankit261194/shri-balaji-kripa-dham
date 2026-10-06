package com.example.shribalajikripadham.hardware

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

/**
 * Immutable Hardware-Level Device Identification Engine (1 Phone = 1 Token).
 *
 * Utilizes purely permanent silicon & SoC hardware attributes that:
 * 1. SURVIVE "Clear Data / Storage Wipe" in Android Settings (100% Zero Drift).
 * 2. SURVIVE App Uninstall & Reinstallation.
 * 3. Remain strictly identical across app launches, foreground/background transitions,
 *    and CPU power-saving modes (Zero dynamic CPU/file parameters).
 */
object DeviceFingerprintManager {

    @Volatile
    private var cachedDeviceId: String? = null

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        cachedDeviceId?.let { if (it.length == 64) return it }

        // 1. Android ID: Unique 64-bit hex generated at first boot.
        // On Android 8.0+ (API 26+), this SURVIVES "Clear Data / Storage Wipe" 100% reliably.
        val androidId = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )?.trim()?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }

        // 2. Permanent Silicon & SoC Hardware Attributes (Read-only from ROM /system/build.prop)
        // These NEVER change on Clear Data, Cache Wipe, or Reinstall.
        val board = Build.BOARD?.trim()?.lowercase() ?: ""
        val hardware = Build.HARDWARE?.trim()?.lowercase() ?: ""
        val bootloader = Build.BOOTLOADER?.trim()?.lowercase() ?: ""
        val brand = Build.BRAND?.trim()?.lowercase() ?: ""
        val device = Build.DEVICE?.trim()?.lowercase() ?: ""
        val model = Build.MODEL?.trim()?.lowercase() ?: ""
        val manufacturer = Build.MANUFACTURER?.trim()?.lowercase() ?: ""
        val product = Build.PRODUCT?.trim()?.lowercase() ?: ""

        // Assemble 100% deterministic, immutable hardware composite (Zero volatile CPU/file parameters)
        val rawComposite = buildString {
            append("AID:").append(androidId).append(";")
            append("BRD:").append(brand).append(";")
            append("MDL:").append(model).append(";")
            append("MFG:").append(manufacturer).append(";")
            append("DEV:").append(device).append(";")
            append("PRD:").append(product).append(";")
            append("BLD:").append(board).append(";")
            append("HWR:").append(hardware).append(";")
            append("BTL:").append(bootloader).append(";")
            append("SALT:SBKD_IMMUTABLE_HARDWARE_LOCK_2026")
        }

        val computedId = sha256(rawComposite)
        cachedDeviceId = computedId
        return computedId
    }

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }
}
