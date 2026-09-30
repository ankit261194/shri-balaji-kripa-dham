package com.example.shribalajikripadham.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin


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

    private val _isAnnouncing = MutableStateFlow(false)
    val isAnnouncing: StateFlow<Boolean> = _isAnnouncing.asStateFlow()

    private val _currentAnnouncedToken = MutableStateFlow<Int?>(null)
    val currentAnnouncedToken: StateFlow<Int?> = _currentAnnouncedToken.asStateFlow()

    private val _currentAnnouncedText = MutableStateFlow<String>("")
    val currentAnnouncedText: StateFlow<String> = _currentAnnouncedText.asStateFlow()


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
                    override fun onStart(utteranceId: String?) {
                        _isAnnouncing.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isAnnouncing.value = false
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isAnnouncing.value = false
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
     * Converts western digits 0-9 into pure Devanagari numerals (०-९)
     */
    fun toDevanagariDigits(num: Int): String {
        val digits = arrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
        return num.toString().map { if (it in '0'..'9') digits[it - '0'] else it }.joinToString("")
    }

    /**
     * Converts numeric token numbers (1 to 999) into pure spoken Devanagari Hindi words
     * so every listener in the temple courtyard clearly hears the number without distortion.
     */
    fun numberToHindiWords(num: Int): String {
        if (num <= 0) return num.toString()
        val words1To100 = arrayOf(
            "", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ", "दस",
            "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस", "बीस",
            "इक्कीस", "बाईस", "तेईस", "चौबीस", "पच्चीस", "छब्बीस", "सत्ताईस", "अट्ठाईस", "उनतीस", "तीस",
            "इकतीस", "बत्तीस", "तैंतीस", "चौंतीस", "पैंतीस", "छत्तीस", "सैंतीस", "अड़तीस", "उनतालीस", "चालीस",
            "इकतालीस", "बयालीस", "तैंतालीस", "चवालीस", "पैंतालीस", "छियालीस", "सैंतालीस", "अड़तालीस", "उनचास", "पचास",
            "इक्यावन", "बावन", "तिरेपन", "चौवन", "पचपन", "छप्पन", "सत्तावन", "अट्ठावन", "उनसठ", "साठ",
            "इकसठ", "बासठ", "तिरेसठ", "चौंसठ", "पैंसठ", "छियासठ", "सरसठ", "अड़सठ", "उनहत्तर", "सत्तर",
            "इकहत्तर", "बहत्तर", "तिहत्तर", "चौहत्तर", "पचहत्तर", "छिहत्तर", "सतहत्तर", "अठहत्तर", "उन्नासी", "अस्सी",
            "इक्यासी", "बयासी", "तिरासी", "चौरासी", "पचासी", "छियासी", "सत्तासी", "अट्ठासी", "नवासी", "नब्बे",
            "इक्यानवे", "बानवे", "तिरानवे", "चौरानवे", "पंचानवे", "छियानवे", "सत्तानवे", "अट्ठानवे", "निन्यानवे", "एक सौ"
        )
        if (num in 1..100) return words1To100[num]
        if (num in 101..999) {
            val hundreds = num / 100
            val rem = num % 100
            val prefix = when (hundreds) {
                1 -> "एक सौ"
                2 -> "दो सौ"
                3 -> "तीन सौ"
                4 -> "चार सौ"
                5 -> "पाँच सौ"
                6 -> "छह सौ"
                7 -> "सात सौ"
                8 -> "आठ सौ"
                9 -> "नौ सौ"
                else -> "$hundreds सौ"
            }
            return if (rem == 0) prefix else "$prefix ${words1To100[rem]}"
        }
        return num.toString()
    }

    /**
     * Sacred Harmonic Temple Bell / Chime Synthesizer:
     * Generates a resonant dual-frequency acoustic brass bell tone (528 Hz + 1056 Hz harmonic overtone)
     * with exponential acoustic decay to alert the ashram hall before calling a token.
     */
    fun playTempleChime(onFinished: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sampleRate = 44100
                val durationSeconds = 0.85
                val numSamples = (sampleRate * durationSeconds).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    // Dual sacred harmonic: 528 Hz (fundamental) + 1056 Hz (octave) with exponential decay
                    val decay = exp(-4.2 * t)
                    val sampleVal = decay * (0.68 * sin(2.0 * PI * 528.0 * t) + 0.32 * sin(2.0 * PI * 1056.0 * t))
                    buffer[i] = (sampleVal * Short.MAX_VALUE * 0.95).toInt().toShort()
                }
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                delay(880)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {}
            } catch (e: Exception) {
                Log.w(TAG, "AudioTrack chime fallback to ToneGenerator: ${e.message}")
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 95)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
                    delay(380)
                    toneGen.release()
                } catch (ex: Exception) {}
            }
            withContext(Dispatchers.Main) {
                onFinished?.invoke()
            }
        }
    }

    /**
     * Loudspeaker Volume Boost Helper:
     * Raises media volume to ensure announcements are clear over Bluetooth speakers and outdoor horns.
     */
    fun boostAudioVolumeForLoudspeaker(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVol < maxVol * 0.85) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.9).toInt(), 0)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot boost audio volume: ${e.message}")
        }
    }

    /**
     * 📢 ACOUSTIC TEMPLE LOUDSPEAKER TOKEN ANNOUNCER
     * 
     * Formats announcement with authentic reverence:
     * "ध्यान दें... टोकन नंबर ४५, पैंतालीस... श्री रमेश कुमार जी, बुलन्दशहर से... कृपया पावन दरबार कक्ष में पधारें।"
     */
    fun announceNextToken(
        context: Context,
        tokenNumber: Int,
        devoteeName: String = "",
        city: String = "",
        repeatCount: Int = 1
    ) {
        if (isMuted(context)) return

        boostAudioVolumeForLoudspeaker(context)

        val cleanName = devoteeName.trim()
        val cleanCity = city.trim()
        val devDigits = toDevanagariDigits(tokenNumber)
        val hindiWords = numberToHindiWords(tokenNumber)

        val tokenSpoken = if (hindiWords.isNotBlank() && hindiWords != tokenNumber.toString()) {
            "टोकन नंबर $devDigits... $hindiWords"
        } else {
            "टोकन नंबर $tokenNumber"
        }

        val primaryAnnouncementText = when {
            cleanName.isNotBlank() && cleanCity.isNotBlank() -> {
                "ध्यान दें... $tokenSpoken... श्री $cleanName जी, $cleanCity से... आपका नंबर आ गया है, कृपया पावन दरबार कक्ष में पधारें।"
            }
            cleanName.isNotBlank() -> {
                "ध्यान दें... $tokenSpoken... श्री $cleanName जी... आपका नंबर आ गया है, कृपया पावन दरबार कक्ष में पधारें।"
            }
            else -> {
                "ध्यान दें... $tokenSpoken... कृपया पावन दरबार कक्ष में पधारें।"
            }
        }

        val repeatAnnouncementText = when {
            cleanName.isNotBlank() -> {
                "एक बार पुनः ध्यान दें... $tokenSpoken... श्री $cleanName जी... कृपया पावन दरबार कक्ष में पधारें।"
            }
            else -> {
                "एक बार पुनः ध्यान दें... $tokenSpoken... कृपया पावन दरबार कक्ष में पधारें।"
            }
        }

        lastAnnouncementText = primaryAnnouncementText
        _currentAnnouncedToken.value = tokenNumber
        _currentAnnouncedText.value = primaryAnnouncementText
        _isAnnouncing.value = true

        val activePreset = getSelectedVoicePreset(context)

        // 🔔 1. Play sacred temple bell chime first, then announce clearly
        playTempleChime {
            if (activePreset == PRESET_CUSTOM_RECORDED && hasCustomRecording(context)) {
                // Play authentic custom recorded human announcement from ashram
                playCustomRecording(context) {
                    if (cleanName.isNotBlank()) {
                        speakWithCurrentPreset(context, "श्री $cleanName जी, कृपया पधारें।")
                    } else {
                        _isAnnouncing.value = false
                    }
                }
            } else {
                speakWithCurrentPreset(context, primaryAnnouncementText)

                // Optional 2nd announcement after pause
                if (repeatCount > 1) {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(6500)
                        playTempleChime {
                            speakWithCurrentPreset(context, repeatAnnouncementText)
                        }
                    }
                }
            }
        }
    }

    fun testVoice(context: Context, presetId: String) {
        boostAudioVolumeForLoudspeaker(context)
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

        val testText = "जय श्री बालाजी महाराज! ध्यान दें... टोकन नंबर एक... श्री रमेश कुमार जी, बुलन्दशहर से... आपका नंबर आ गया है, कृपया पावन दरबार कक्ष में पधारें।"

        playTempleChime {
            initIfNeeded(context)
            applyVoiceSettings(context, presetId)
            speakRaw(testText)
        }
    }

    fun repeatLastAnnouncement(context: Context) {
        if (lastAnnouncementText.isNotBlank()) {
            val tokenNum = _currentAnnouncedToken.value ?: 0
            announceNextToken(context, tokenNum, lastAnnouncementText)
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
            _isAnnouncing.value = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "token_announcement_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error executing speakRaw", e)
            _isAnnouncing.value = false
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
