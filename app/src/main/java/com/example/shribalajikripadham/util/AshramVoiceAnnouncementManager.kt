package com.example.shribalajikripadham.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class VoicePresetInfo(
    val id: String,
    val nameHindi: String,
    val nameEnglish: String,
    val gender: String, // "MALE", "FEMALE", "CUSTOM", or "DEVICE"
    val description: String,
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val icon: String = "🎙️",
    val category: String = gender,
    val speechStyle: String = "DEVOTIONAL",
    val neuralVoiceHint: String = ""
)

object AshramVoiceAnnouncementManager {
    private const val TAG = "VoiceAnnouncement"
    private const val PREFS_NAME = "sbkd_voice_announcement_prefs"
    private const val KEY_IS_MUTED = "is_tts_muted"
    private const val KEY_VOICE_PRESET = "selected_voice_preset"

    const val PRESET_NATURAL_MALE = "NATURAL_MALE"
    const val PRESET_NATURAL_FEMALE = "NATURAL_FEMALE"
    const val PRESET_CUSTOM_RECORDED = "CUSTOM_RECORDED"
    const val PRESET_OFFLINE_DEVICE = "OFFLINE_DEVICE"

    // Backward compatibility aliases
    const val PRESET_GURU_CALM = PRESET_NATURAL_MALE
    const val PRESET_FEMALE_SWEET = PRESET_NATURAL_FEMALE
    const val PRESET_ANNOUNCER_MALE = PRESET_NATURAL_MALE
    const val PRESET_SEVIKA_FEMALE = PRESET_NATURAL_FEMALE
    const val PRESET_YOUTH_CRISP = PRESET_NATURAL_MALE
    const val PRESET_TRADITIONAL_VYAS = PRESET_NATURAL_MALE

    val AVAILABLE_VOICE_PRESETS = listOf(
        VoicePresetInfo(
            id = PRESET_NATURAL_MALE,
            nameHindi = "धीर-गंभीर पुरुष स्वर (HD Hindi Male Voice)",
            nameEnglish = "HD Devotional Male Voice",
            gender = "MALE",
            category = "MALE",
            description = "स्पष्ट, धीर-गंभीर उद्घोषक स्वर (Google HD देववाणी)",
            pitch = 0.88f,
            speechRate = 0.86f,
            icon = "👨",
            speechStyle = "DEVOTIONAL"
        ),
        VoicePresetInfo(
            id = PRESET_NATURAL_FEMALE,
            nameHindi = "मधुर सेविका महिला स्वर (HD Hindi Female Voice)",
            nameEnglish = "HD Devotional Female Voice",
            gender = "FEMALE",
            category = "FEMALE",
            description = "अत्यंत मधुर, शांत व वात्सल्यमयी स्वर (Google HD देववाणी)",
            pitch = 1.05f,
            speechRate = 0.88f,
            icon = "👩",
            speechStyle = "SWEET"
        ),
        VoicePresetInfo(
            id = PRESET_CUSTOM_RECORDED,
            nameHindi = "आश्रम की वास्तविक रिकॉर्डेड आवाज़ (Ashram Real Voice)",
            nameEnglish = "Ashram Custom Recorded Voice",
            gender = "CUSTOM",
            category = "CUSTOM",
            description = "आश्रम के माइक से स्वयं रिकॉर्ड की गई 100% असली इंसानी आवाज़",
            pitch = 1.0f,
            speechRate = 1.0f,
            icon = "🎙️",
            speechStyle = "REAL_HUMAN"
        ),
        VoicePresetInfo(
            id = PRESET_OFFLINE_DEVICE,
            nameHindi = "डिवाइस का सामान्य ऑफ़लाइन स्वर (Device Hindi TTS)",
            nameEnglish = "Offline Device Built-in TTS",
            gender = "DEVICE",
            category = "DEVICE",
            description = "फ़ोन का अंतर्निर्मित ऑफ़लाइन हिंदी स्वर",
            pitch = 1.0f,
            speechRate = 1.0f,
            icon = "📱",
            speechStyle = "OFFLINE"
        )
    )

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingSpeech: String? = null
    private var lastAnnouncementText: String = ""

    // Media Recorder & Player for Live In-App Recordings
    private var activeMediaRecorder: MediaRecorder? = null
    private var activeMediaPlayer: MediaPlayer? = null
    var isCurrentlyRecording: Boolean = false
        private set

    private fun getCustomVoiceFile(context: Context): File {
        return File(context.filesDir, "custom_token_voice.m4a")
    }

    fun hasCustomRecording(context: Context): Boolean {
        val file = getCustomVoiceFile(context)
        return file.exists() && file.length() > 1024
    }

    fun startCustomRecording(context: Context): Boolean {
        return try {
            stopAudioPlayback()
            val file = getCustomVoiceFile(context)
            if (file.exists()) file.delete()

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            activeMediaRecorder = recorder
            isCurrentlyRecording = true
            Log.d(TAG, "Started custom voice recording")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recording", e)
            isCurrentlyRecording = false
            activeMediaRecorder = null
            false
        }
    }

    fun stopCustomRecording(context: Context): Boolean {
        return try {
            activeMediaRecorder?.apply {
                stop()
                release()
            }
            activeMediaRecorder = null
            isCurrentlyRecording = false
            Log.d(TAG, "Stopped custom voice recording. File size: ${getCustomVoiceFile(context).length()} bytes")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping custom recording", e)
            activeMediaRecorder = null
            isCurrentlyRecording = false
            false
        }
    }

    fun deleteCustomRecording(context: Context): Boolean {
        stopAudioPlayback()
        val file = getCustomVoiceFile(context)
        return if (file.exists()) file.delete() else false
    }

    fun playCustomRecording(context: Context, onComplete: (() -> Unit)? = null) {
        val file = getCustomVoiceFile(context)
        if (!file.exists() || file.length() < 1024) {
            onComplete?.invoke()
            return
        }
        playAudioFile(file, onComplete)
    }

    private fun playAudioFile(file: File, onComplete: (() -> Unit)? = null) {
        try {
            stopAudioPlayback()
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    it.release()
                    activeMediaPlayer = null
                    onComplete?.invoke()
                }
                start()
            }
            activeMediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file: ${file.name}", e)
            activeMediaPlayer = null
            onComplete?.invoke()
        }
    }

    private fun stopAudioPlayback() {
        try {
            activeMediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            activeMediaPlayer = null
        } catch (e: Exception) {
            activeMediaPlayer = null
        }
    }

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
        return prefs.getString(KEY_VOICE_PRESET, PRESET_NATURAL_MALE) ?: PRESET_NATURAL_MALE
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
                    tts?.language = Locale.getDefault()
                }
                val currentPreset = getSelectedVoicePreset(context)
                applyVoiceSettings(context, currentPreset)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {}
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {}
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
            tts?.setPitch(preset.pitch)
            tts?.setSpeechRate(preset.speechRate)

            tts?.voices?.let { allVoices ->
                val hindiVoices = allVoices.filter { 
                    it.locale.language.equals("hi", ignoreCase = true) || 
                    it.locale.toLanguageTag().startsWith("hi", ignoreCase = true) 
                }
                if (hindiVoices.isNotEmpty()) {
                    val matchingVoice = when (preset.gender) {
                        "FEMALE" -> hindiVoices.find {
                            it.name.contains("female", ignoreCase = true) ||
                            it.name.contains("-c-", ignoreCase = true) ||
                            it.name.contains("-a-", ignoreCase = true)
                        } ?: hindiVoices.first()
                        "MALE" -> hindiVoices.find {
                            it.name.contains("male", ignoreCase = true) ||
                            it.name.contains("-b-", ignoreCase = true) ||
                            it.name.contains("-d-", ignoreCase = true)
                        } ?: hindiVoices.first()
                        else -> hindiVoices.first()
                    }
                    tts?.voice = matchingVoice
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying voice settings", e)
        }
    }

    /**
     * Sacred Dual-Tone Temple Bell Chime:
     * Plays a resonant sacred temple bell chime tone to alert the darbar hall before calling a token.
     */
    fun playTempleChime(onFinished: (() -> Unit)? = null) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 95)
            // Resonant sacred chime tone
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 380)
            CoroutineScope(Dispatchers.IO).launch {
                delay(420)
                try {
                    toneGen.release()
                } catch (e: Exception) {}
                withContext(Dispatchers.Main) {
                    onFinished?.invoke()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Temple chime fallback: ${e.message}")
            onFinished?.invoke()
        }
    }

    fun announceNextToken(
        context: Context,
        tokenNumber: Int,
        devoteeName: String = "",
        city: String = ""
    ) {
        if (isMuted(context)) return

        val cleanName = devoteeName.trim()
        val cleanCity = city.trim()

        val fullAnnouncementText = when {
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

        lastAnnouncementText = fullAnnouncementText
        val activePreset = getSelectedVoicePreset(context)

        // 🔔 Play sacred temple bell chime first, then announce clearly
        playTempleChime {
            if (activePreset == PRESET_CUSTOM_RECORDED && hasCustomRecording(context)) {
                // Play authentic custom recorded human announcement from ashram
                playCustomRecording(context) {
                    if (cleanName.isNotBlank()) {
                        speakWithCurrentPreset(context, "श्री $cleanName जी, कृपया पधारें।")
                    }
                }
            } else {
                speakWithCurrentPreset(context, fullAnnouncementText)
            }
        }
    }

    fun testVoice(context: Context, presetId: String) {
        if (presetId == PRESET_CUSTOM_RECORDED) {
            if (hasCustomRecording(context)) {
                playTempleChime {
                    playCustomRecording(context)
                }
            } else {
                initIfNeeded(context)
                speakRaw("कृपया नीचे दिए गए माइक रिकॉर्ड बटन से आश्रम की अपनी वास्तविक आवाज़ रिकॉर्ड करें।")
            }
            return
        }

        val testText = when (presetId) {
            PRESET_NATURAL_FEMALE -> "जय श्री बालाजी! टोकन नंबर एक, श्री रमेश कुमार जी, आपका नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
            PRESET_NATURAL_MALE -> "जय श्री बालाजी! टोकन नंबर एक, श्री रमेश कुमार जी, आपका नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
            else -> "जय श्री बालाजी! टोकन नंबर एक, श्री रमेश कुमार जी, आपका नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
        }

        playTempleChime {
            initIfNeeded(context)
            applyVoiceSettings(context, presetId)
            speakRaw(testText)
        }
    }

    fun repeatLastAnnouncement(context: Context) {
        if (lastAnnouncementText.isNotBlank()) {
            announceNextToken(context, 0, lastAnnouncementText)
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

    fun isHindiLanguageAvailable(): Boolean {
        return try {
            val res = tts?.isLanguageAvailable(Locale("hi", "IN")) ?: TextToSpeech.LANG_NOT_SUPPORTED
            res >= TextToSpeech.LANG_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    fun promptInstallHindiVoiceIfNeeded(context: Context) {
        try {
            val installIntent = android.content.Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot launch ACTION_INSTALL_TTS_DATA: ${e.message}")
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
        stopAudioPlayback()
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        stopAudioPlayback()
        try {
            activeMediaRecorder?.release()
            activeMediaRecorder = null
            isCurrentlyRecording = false
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down", e)
        }
    }
}
