package com.example.shribalajikripadham.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

object AshramVoiceAnnouncementManager {
    private const val TAG = "VoiceAnnouncement"
    private const val PREFS_NAME = "sbkd_voice_announcement_prefs"
    private const val KEY_IS_MUTED = "is_tts_muted"

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingSpeech: String? = null
    private var lastAnnouncementText: String = ""

    fun isMuted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_MUTED, false)
    }

    fun setMuted(context: Context, muted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_MUTED, muted).apply()
        if (muted) {
            stop()
        }
    }

    fun initIfNeeded(context: Context) {
        if (tts != null && isInitialized) return
        val appContext = context.applicationContext
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val hindiLocale = Locale("hi", "IN")
                val res = tts?.setLanguage(hindiLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Hindi TTS locale not directly supported, falling back to default locale")
                    tts?.language = Locale.getDefault()
                }
                tts?.setSpeechRate(0.92f) // Slightly slower for respectful, clear pronunciation
                tts?.setPitch(1.05f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS announcement started: $utteranceId")
                    }
                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS announcement completed: $utteranceId")
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS announcement error on $utteranceId")
                    }
                })
                isInitialized = true
                pendingSpeech?.let { speech ->
                    speakRaw(speech)
                    pendingSpeech = null
                }
            } else {
                Log.e(TAG, "Failed to initialize TextToSpeech: status $status")
            }
        }
    }

    /**
     * Speaks the Next Token announcement in clear, respectful Hindi.
     * Announcement Format:
     * "टोकन नंबर [X], श्री [नाम] जी, [शहर] से, आपका नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
     */
    fun announceNextToken(
        context: Context,
        tokenNumber: Int,
        devoteeName: String = "",
        city: String = ""
    ) {
        if (isMuted(context)) return

        val cleanName = devoteeName.trim()
        val cleanCity = city.trim()

        val textToSpeak = when {
            cleanName.isNotBlank() && cleanCity.isNotBlank() -> {
                "टोकन नंबर $tokenNumber, श्री $cleanName जी, $cleanCity से, आपका नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
            }
            cleanName.isNotBlank() -> {
                "टोकन नंबर $tokenNumber, श्री $cleanName जी, आपका नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
            }
            else -> {
                "टोकन नंबर $tokenNumber, आपका नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
            }
        }

        lastAnnouncementText = textToSpeak
        speak(context, textToSpeak)
    }

    fun repeatLastAnnouncement(context: Context) {
        if (lastAnnouncementText.isNotBlank()) {
            speak(context, lastAnnouncementText)
        }
    }

    fun speak(context: Context, text: String) {
        if (isMuted(context)) return
        initIfNeeded(context)
        if (!isInitialized) {
            pendingSpeech = text
        } else {
            speakRaw(text)
        }
    }

    private fun speakRaw(text: String) {
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "token_announcement_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error executing speakRaw", e)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
