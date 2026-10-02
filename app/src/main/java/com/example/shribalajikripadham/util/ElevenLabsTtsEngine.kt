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
 * ElevenLabs High-Fidelity Human Voice Engine for Shri Balaji Kripa Dham.
 * Features:
 * 1. Studio-grade Hindi voices ("Brian" for Male, "Sarah" for Female).
 * 2. Permanent local disk caching to minimize API usage:
 *    Only synthesizes dynamic devotee names (costing ~10-15 chars once per new name).
 * 3. Works seamlessly alongside pre-baked static audio clips (Audio Stitching).
 */
object ElevenLabsTtsEngine {
    private const val TAG = "ElevenLabsTtsEngine"
    private const val TTS_BASE_ENDPOINT = "https://api.elevenlabs.io/v1/text-to-speech"

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
     * Synthesizes audio using ElevenLabs or retrieves from permanent disk cache.
     * Returns local File if successful, or null if network/quota fails or API key is blank.
     */
    suspend fun synthesizeSpeechToFile(
        context: Context,
        text: String,
        apiKey: String,
        voiceId: String = VOICE_MALE_BRIAN,
        stability: Double = 0.65,
        similarityBoost: Double = 0.80
    ): File? = withContext(Dispatchers.IO) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return@withContext null

        val cachedFile = getCachedAudioFile(context, cleanText, voiceId)
        if (cachedFile.exists() && cachedFile.length() > 500) {
            Log.d(TAG, "Cache HIT for ElevenLabs TTS: ${cachedFile.name} (${cachedFile.length()} bytes)")
            return@withContext cachedFile
        }

        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            Log.d(TAG, "No ElevenLabs API key configured; skipping cloud synthesis.")
            return@withContext null
        }

        try {
            val endpoint = "$TTS_BASE_ENDPOINT/$voiceId"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 7000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("xi-api-key", cleanKey)
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
                return@withContext cachedFile
            } else {
                val errorStream = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                Log.w(TAG, "ElevenLabs TTS synthesis returned HTTP $responseCode: $errorStream")
            }
        } catch (e: Exception) {
            Log.w(TAG, "ElevenLabs TTS synthesis failed: ${e.message}")
        }

        return@withContext null
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
