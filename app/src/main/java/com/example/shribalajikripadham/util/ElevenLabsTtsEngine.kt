package com.example.shribalajikripadham.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Data model representing real-time credit status of an ElevenLabs API Key.
 */
data class ElevenLabsKeyInfo(
    val key: String,
    val slotNumber: Int,
    val tier: String = "free",
    val characterCount: Int = 0,
    val characterLimit: Int = 10000,
    val remainingCharacters: Int = 10000,
    val nextResetUnix: Long = 0L,
    val isValid: Boolean = true,
    val errorMsg: String? = null
) {
    val usagePercent: Float
        get() = if (characterLimit > 0) (characterCount.toFloat() / characterLimit.toFloat()).coerceIn(0f, 1f) else 1f

    val remainingPercent: Float
        get() = (1f - usagePercent).coerceIn(0f, 1f)
}

/**
 * ElevenLabs High-Fidelity Human Voice Engine with Multi-Key Auto-Failover & Balance Monitoring.
 * Features:
 * 1. 4-Key Pool with automatic failover (40,000 Free Credits/Month!).
 * 2. Live credit balance fetching via /v1/user/subscription.
 * 3. Permanent local disk caching (synthesizes only new devotee names once, costing ~10 chars).
 * 4. Ultra-smooth audio stitching integration.
 */
object ElevenLabsTtsEngine {
    private const val TAG = "ElevenLabsTtsEngine"
    private const val TTS_BASE_ENDPOINT = "https://api.elevenlabs.io/v1/text-to-speech"
    private const val SUBSCRIPTION_ENDPOINT = "https://api.elevenlabs.io/v1/user/subscription"

    // Default Voices
    const val VOICE_MALE_BRIAN = "nPczCjzI2devNBz1zQrb"  // Deep, resonant, calm male temple announcer
    const val VOICE_FEMALE_SARAH = "EXAVITQu4vr4xnSDxMaL" // Reassuring, clear, respectful female temple voice

    const val MODEL_MULTILINGUAL_V2 = "eleven_multilingual_v2"

    fun getCachedAudioFile(context: Context, text: String, voiceId: String): File {
        val cacheDir = File(context.cacheDir, "voice_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val key = "el_${text.trim()}_${voiceId.trim()}"
        val hash = md5(key)
        return File(cacheDir, "$hash.mp3")
    }

    private fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digested = md.digest(input.toByteArray(Charsets.UTF_8))
            digested.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input.hashCode().toString()
        }
    }

    /**
     * Fetches live credit balance and status for an ElevenLabs API key.
     */
    suspend fun fetchKeyBalance(apiKey: String, slotNumber: Int): ElevenLabsKeyInfo = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext ElevenLabsKeyInfo(
                key = "",
                slotNumber = slotNumber,
                isValid = false,
                errorMsg = "कुंजी खाली है"
            )
        }

        try {
            val url = URL(SUBSCRIPTION_ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 6000
                setRequestProperty("xi-api-key", cleanKey)
                setRequestProperty("User-Agent", "ShriBalajiKripaDham-Admin/1.0")
            }

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val json = JSONObject(body)
                val tier = json.optString("tier", "free")
                val charCount = json.optInt("character_count", 0)
                val charLimit = json.optInt("character_limit", 10000)
                val nextReset = json.optLong("next_character_count_reset_unix", 0L)
                val remaining = (charLimit - charCount).coerceAtLeast(0)

                return@withContext ElevenLabsKeyInfo(
                    key = cleanKey,
                    slotNumber = slotNumber,
                    tier = tier,
                    characterCount = charCount,
                    characterLimit = charLimit,
                    remainingCharacters = remaining,
                    nextResetUnix = nextReset,
                    isValid = true,
                    errorMsg = if (remaining <= 0) "कोटा समाप्त (Exhausted)" else null
                )
            } else {
                val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                val msg = if (code == 401) "अमान्य API Key (Unauthorized)" else "HTTP $code: $err"
                return@withContext ElevenLabsKeyInfo(
                    key = cleanKey,
                    slotNumber = slotNumber,
                    isValid = false,
                    errorMsg = msg
                )
            }
        } catch (e: Exception) {
            return@withContext ElevenLabsKeyInfo(
                key = cleanKey,
                slotNumber = slotNumber,
                isValid = false,
                errorMsg = "कनेक्शन त्रुटि: ${e.message}"
            )
        }
    }

    /**
     * Synthesizes audio using a Multi-Key Pool with automatic failover.
     * Iterates through available keys. If Key 1 fails (exhausted/rate-limit), automatically falls back to Key 2, etc.
     */
    suspend fun synthesizeSpeechWithPool(
        context: Context,
        text: String,
        apiKeys: List<String>,
        voiceId: String = VOICE_MALE_BRIAN,
        stability: Double = 0.65,
        similarityBoost: Double = 0.80
    ): File? = withContext(Dispatchers.IO) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return@withContext null

        // 1. Check disk cache first (0 credits cost)
        val cachedFile = getCachedAudioFile(context, cleanText, voiceId)
        if (cachedFile.exists() && cachedFile.length() > 500) {
            Log.d(TAG, "Cache HIT for ElevenLabs TTS: ${cachedFile.name}")
            return@withContext cachedFile
        }

        val validKeys = apiKeys.map { it.trim() }.filter { it.isNotBlank() }
        if (validKeys.isEmpty()) {
            Log.d(TAG, "No ElevenLabs API keys configured in pool.")
            return@withContext null
        }

        // 2. Try each key in sequence
        for ((idx, key) in validKeys.withIndex()) {
            val file = synthesizeSingleKey(context, cleanText, key, voiceId, cachedFile, stability, similarityBoost)
            if (file != null && file.exists() && file.length() > 500) {
                Log.i(TAG, "Speech synthesized successfully using Key #${idx + 1}")
                return@withContext file
            } else {
                Log.w(TAG, "Key #${idx + 1} failed or exhausted. Trying next key in pool...")
            }
        }

        Log.e(TAG, "All ElevenLabs API keys in pool failed or exhausted.")
        return@withContext null
    }

    /**
     * Single-key synthesis worker.
     */
    private fun synthesizeSingleKey(
        context: Context,
        cleanText: String,
        apiKey: String,
        voiceId: String,
        cachedFile: File,
        stability: Double,
        similarityBoost: Double
    ): File? {
        try {
            val endpoint = "$TTS_BASE_ENDPOINT/$voiceId"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 7000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("xi-api-key", apiKey)
                setRequestProperty("User-Agent", "ShriBalajiKripaDham-StudioVoice/1.0")
            }

            val payload = JSONObject().apply {
                put("text", cleanText)
                put("model_id", MODEL_MULTILINGUAL_V2)
                put("voice_settings", JSONObject().apply {
                    put("stability", stability)
                    put("similarity_boost", similarityBoost)
                })
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.use { input ->
                    FileOutputStream(cachedFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
                Log.i(TAG, "ElevenLabs TTS generated & cached: ${cachedFile.name} (${cachedFile.length()} bytes)")
                return cachedFile
            } else {
                val errorStream = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                Log.w(TAG, "ElevenLabs TTS HTTP $responseCode: $errorStream")
            }
        } catch (e: Exception) {
            Log.w(TAG, "ElevenLabs TTS network failure: ${e.message}")
        }
        return null
    }

    /**
     * Backward-compatible single-key method.
     */
    suspend fun synthesizeSpeechToFile(
        context: Context,
        text: String,
        apiKey: String,
        voiceId: String = VOICE_MALE_BRIAN,
        stability: Double = 0.65,
        similarityBoost: Double = 0.80
    ): File? {
        return synthesizeSpeechWithPool(context, text, listOf(apiKey), voiceId, stability, similarityBoost)
    }

    /**
     * Clears cached audio files to free space if needed.
     */
    fun clearCache(context: Context): Boolean {
        return try {
            val cacheDir = File(context.cacheDir, "voice_cache")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            } else true
        } catch (e: Exception) {
            false
        }
    }
}
