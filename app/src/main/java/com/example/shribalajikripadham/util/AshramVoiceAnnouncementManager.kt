package com.example.shribalajikripadham.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

data class VoicePresetInfo(
    val id: String,
    val nameHindi: String,
    val nameEnglish: String,
    val gender: String, // "MALE" or "FEMALE"
    val description: String,
    val pitch: Float,
    val speechRate: Float,
    val icon: String
)

object AshramVoiceAnnouncementManager {
    private const val TAG = "VoiceAnnouncement"
    private const val PREFS_NAME = "sbkd_voice_announcement_prefs"
    private const val KEY_IS_MUTED = "is_tts_muted"
    private const val KEY_VOICE_PRESET = "selected_voice_preset"

    const val PRESET_GURU_CALM = "GURU_CALM"
    const val PRESET_FEMALE_SWEET = "FEMALE_SWEET"
    const val PRESET_ANNOUNCER_MALE = "ANNOUNCER_MALE"
    const val PRESET_SEVIKA_FEMALE = "SEVIKA_FEMALE"
    const val PRESET_YOUTH_CRISP = "YOUTH_CRISP"
    const val PRESET_TRADITIONAL_VYAS = "TRADITIONAL_VYAS"

    val AVAILABLE_VOICE_PRESETS = listOf(
        VoicePresetInfo(
            id = PRESET_GURU_CALM,
            nameHindi = "गुरुवाणी व शांत उद्घोषणा",
            nameEnglish = "Male - Calm & Reverent Devotional",
            gender = "MALE",
            description = "गंभीर, आदरणीय एवं शांत पुरुष वाणी (आश्रम दरबार हेतु सर्वोत्तम)",
            pitch = 0.90f,
            speechRate = 0.88f,
            icon = "🙏"
        ),
        VoicePresetInfo(
            id = PRESET_FEMALE_SWEET,
            nameHindi = "देवी वंदना मधुर स्वर",
            nameEnglish = "Female - Sweet & Devotional",
            gender = "FEMALE",
            description = "मधुर, शांत एवं सौम्य महिला वाणी",
            pitch = 1.18f,
            speechRate = 0.90f,
            icon = "🌸"
        ),
        VoicePresetInfo(
            id = PRESET_ANNOUNCER_MALE,
            nameHindi = "रेडियो उद्घोषक बुलन्द वाणी",
            nameEnglish = "Male - Clear Temple Announcer",
            gender = "MALE",
            description = "स्पष्ट, बुलन्द एवं आधिकारिक पुरुष उद्घोषक स्वर",
            pitch = 0.84f,
            speechRate = 0.95f,
            icon = "📢"
        ),
        VoicePresetInfo(
            id = PRESET_SEVIKA_FEMALE,
            nameHindi = "आदरणीय सेविका उद्घोषणा",
            nameEnglish = "Female - Respectful Ashram Sevika",
            gender = "FEMALE",
            description = "विनम्र, स्पष्ट व आदरणीय आश्रम सेविका स्वर",
            pitch = 1.05f,
            speechRate = 0.90f,
            icon = "💐"
        ),
        VoicePresetInfo(
            id = PRESET_YOUTH_CRISP,
            nameHindi = "युवा ऊर्जावान उद्घोषणा",
            nameEnglish = "Youth - Crisp & Energetic",
            gender = "MALE",
            description = "तेज़, स्पष्ट और ऊर्जावान युवा स्वर",
            pitch = 1.0f,
            speechRate = 1.02f,
            icon = "⚡"
        ),
        VoicePresetInfo(
            id = PRESET_TRADITIONAL_VYAS,
            nameHindi = "शास्त्रीय व्यास कथावाचक",
            nameEnglish = "Traditional Narrator / Vyas Style",
            gender = "MALE",
            description = "पारंपरिक धीर-गंभीर कथावाचक व्यास शैली",
            pitch = 0.78f,
            speechRate = 0.82f,
            icon = "🕉️"
        )
    )

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

    fun getSelectedVoicePreset(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VOICE_PRESET, PRESET_GURU_CALM) ?: PRESET_GURU_CALM
    }

    fun setVoicePreset(context: Context, presetId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_VOICE_PRESET, presetId).apply()
        applyVoiceSettings(context, presetId)
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

                val currentPreset = getSelectedVoicePreset(context)
                applyVoiceSettings(context, currentPreset)

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

    private fun applyVoiceSettings(context: Context, presetId: String) {
        val preset = AVAILABLE_VOICE_PRESETS.find { it.id == presetId } ?: AVAILABLE_VOICE_PRESETS[0]
        try {
            // Apply pitch and speech rate modulation for human resonance
            tts?.setPitch(preset.pitch)
            tts?.setSpeechRate(preset.speechRate)

            // Inspect available native system voices for Hindi (hi-IN)
            tts?.voices?.let { allVoices ->
                val hindiVoices = allVoices.filter { it.locale.language == "hi" }
                if (hindiVoices.isNotEmpty()) {
                    val matchingVoice = when (preset.gender) {
                        "FEMALE" -> hindiVoices.find { it.name.contains("female", ignoreCase = true) || it.name.contains("-c-", ignoreCase = true) || it.name.contains("-a-", ignoreCase = true) }
                        else -> hindiVoices.find { it.name.contains("male", ignoreCase = true) || it.name.contains("-b-", ignoreCase = true) || it.name.contains("-d-", ignoreCase = true) }
                    } ?: hindiVoices.first()
                    tts?.voice = matchingVoice
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying voice settings for preset $presetId", e)
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
        speakWithCurrentPreset(context, textToSpeak)
    }

    /**
     * Speaks a test phrase for the given voice preset so Super Admin / Admin can preview.
     */
    fun testVoice(context: Context, presetId: String) {
        initIfNeeded(context)
        applyVoiceSettings(context, presetId)
        val testText = "टोकन नंबर 1, श्री रमेश कुमार जी, आपका नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
        speakRaw(testText)
    }

    fun repeatLastAnnouncement(context: Context) {
        if (lastAnnouncementText.isNotBlank()) {
            speakWithCurrentPreset(context, lastAnnouncementText)
        }
    }

    fun speak(context: Context, text: String) {
        speakWithCurrentPreset(context, text)
    }

    private fun speakWithCurrentPreset(context: Context, text: String) {
        if (isMuted(context)) return
        initIfNeeded(context)
        val preset = getSelectedVoicePreset(context)
        applyVoiceSettings(context, preset)
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
