package com.example.shribalajikripadham.util

import android.content.Context
import android.util.Base64
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
 * Google Cloud Text-to-Speech (Neural2 Hindi) Engine for Shri Balaji Kripa Dham.
 * Features:
 * 1. Ultra-realistic, respectful, calm Indian Hindi voices (hi-IN-Neural2-B male & hi-IN-Neural2-A female).
 * 2. Permanent local disk caching (voice_cache/{md5}.mp3) to prevent repeated quota consumption and network latency.
 * 3. Graceful timeout and error handling for rural offline fallback.
 */
object GoogleCloudTtsEngine {
    private const val TAG = "GoogleCloudTtsEngine"
    private const val TTS_ENDPOINT = "https://texttospeech.googleapis.com/v1/text:synthesize"

    const val VOICE_NEURAL2_MALE = "hi-IN-Neural2-B" // Deep, polite, respectful male temple announcer
    const val VOICE_NEURAL2_FEMALE = "hi-IN-Neural2-A" // Sweet, soothing, respectful female temple voice
    const val VOICE_JOURNEY_MALE = "hi-IN-Journey-D"
    const val VOICE_JOURNEY_FEMALE = "hi-IN-Journey-F"

    fun getCachedAudioFile(context: Context, text: String, voiceName: String): File {
        val cacheDir = File(context.cacheDir, "voice_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val key = "${text.trim()}_${voiceName.trim()}"
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
     * Synthesizes audio using Google Cloud Neural2 Hindi voice or retrieves from disk cache.
     * Returns the local File if successful, or null if network/quota fails or API key is absent.
     */
    suspend fun synthesizeSpeechToFile(
        context: Context,
        text: String,
        apiKey: String,
        voiceName: String = VOICE_NEURAL2_MALE,
        speakingRate: Double = 0.88,
        pitch: Double = 0.0
    ): File? = withContext(Dispatchers.IO) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return@withContext null

        val cachedFile = getCachedAudioFile(context, cleanText, voiceName)
        if (cachedFile.exists() && cachedFile.length() > 500) {
            Log.d(TAG, "Cache HIT for TTS: ${cachedFile.name} (${cachedFile.length()} bytes)")
            return@withContext cachedFile
        }

        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            Log.d(TAG, "No Google Cloud TTS API key configured; skipping cloud synthesis.")
            return@withContext null
        }

        try {
            val url = URL("$TTS_ENDPOINT?key=$cleanKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3500 // Quick timeout to prevent stalling queue at counter
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("User-Agent", "ShriBalajiKripaDham-NeuralTTS/1.0")
            }

            val payload = JSONObject().apply {
                put("input", JSONObject().put("text", cleanText))
                put("voice", JSONObject().apply {
                    put("languageCode", "hi-IN")
                    put("name", voiceName)
                })
                put("audioConfig", JSONObject().apply {
                    put("audioEncoding", "MP3")
                    put("speakingRate", speakingRate)
                    put("pitch", pitch)
                })
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseStr = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val respJson = JSONObject(responseStr)
                val base64Audio = respJson.optString("audioContent", "")
                if (base64Audio.isNotBlank()) {
                    val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                    FileOutputStream(cachedFile).use { fos ->
                        fos.write(audioBytes)
                        fos.flush()
                    }
                    Log.i(TAG, "Google Cloud TTS generated & cached: ${cachedFile.name} (${audioBytes.size} bytes)")
                    return@withContext cachedFile
                }
            } else {
                val errorStream = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                Log.w(TAG, "Google Cloud TTS synthesis returned HTTP $responseCode: $errorStream")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Google Cloud TTS synthesis failed: ${e.message}")
        }

        return@withContext null
    }

    /**
     * Clears all cached voice MP3 files to free storage if requested by admin.
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
