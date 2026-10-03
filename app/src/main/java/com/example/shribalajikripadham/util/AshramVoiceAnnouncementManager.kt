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
import kotlinx.coroutines.Job
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
    private const val KEY_GOOGLE_TTS_API_KEY = "google_cloud_tts_api_key"
    private const val KEY_ELEVENLABS_API_KEY = "elevenlabs_studio_tts_api_key"
    private const val KEY_ELEVENLABS_API_KEY_1 = "elevenlabs_studio_tts_api_key_1"
    private const val KEY_ELEVENLABS_API_KEY_2 = "elevenlabs_studio_tts_api_key_2"
    private const val KEY_ELEVENLABS_API_KEY_3 = "elevenlabs_studio_tts_api_key_3"
    private const val KEY_ELEVENLABS_API_KEY_4 = "elevenlabs_studio_tts_api_key_4"
    private const val KEY_ELEVENLABS_API_KEY_5 = "elevenlabs_studio_tts_api_key_5"
    private const val KEY_ELEVENLABS_API_KEY_6 = "elevenlabs_studio_tts_api_key_6"
    private const val KEY_ELEVENLABS_API_KEY_7 = "elevenlabs_studio_tts_api_key_7"
    private const val KEY_ELEVENLABS_API_KEY_8 = "elevenlabs_studio_tts_api_key_8"
    private const val KEY_ELEVENLABS_API_KEY_9 = "elevenlabs_studio_tts_api_key_9"
    private const val KEY_ELEVENLABS_API_KEY_10 = "elevenlabs_studio_tts_api_key_10"
    private const val KEY_ELEVENLABS_API_KEY_11 = "elevenlabs_studio_tts_api_key_11"

    const val DEFAULT_ELEVENLABS_API_KEY_1 = "sk_13bf5df7b1cf804ee9b3b03590b2f64d90463109bddffbfe"
    const val DEFAULT_ELEVENLABS_API_KEY_2 = "sk_61cb841ba07dec2d666c7b8d9cc516655565ba6bdb7b806d"
    const val DEFAULT_ELEVENLABS_API_KEY_3 = "sk_a5c124d02a3c357ae95bfac2afb7947a2c8448504aff9dc6"
    const val DEFAULT_ELEVENLABS_API_KEY_4 = "sk_fd84cbff22e602f6a663c34b2e2427fddb9cc2d14f349031"
    const val DEFAULT_ELEVENLABS_API_KEY_5 = "sk_6468884e17cc4dbeba98e4ec00a94297d7e70b524d645017"
    const val DEFAULT_ELEVENLABS_API_KEY_6 = "sk_d3636bbea293787f3ca937b115a4461aa34f86546879a762"
    const val DEFAULT_ELEVENLABS_API_KEY_7 = "sk_882062ab3f14c8aa28291937c82a20b6c064a63c37d8d6eb"
    const val DEFAULT_ELEVENLABS_API_KEY_8 = "sk_26cd0d47e5169a3735452d9d97ed44554282d68400980f87"
    const val DEFAULT_ELEVENLABS_API_KEY_9 = "sk_4b2b220fc9db07f3361bdb062b970f931af485b281c94cb5"
    const val DEFAULT_ELEVENLABS_API_KEY_10 = "sk_a864c493d39f6227a02fbcb05e16a44b982560981290d6a0"
    const val DEFAULT_ELEVENLABS_API_KEY_11 = "sk_5434fbed62951af7525c89680509487d9598971b4ad1ccc7"
    const val DEFAULT_ELEVENLABS_API_KEY = DEFAULT_ELEVENLABS_API_KEY_1

    // Master Kill-Switch & Smart Scheduler Keys
    private const val KEY_VOICE_MASTER_ENABLED = "voice_master_enabled"
    private const val KEY_VOICE_SCHEDULE_MODE = "voice_schedule_mode" // "ALWAYS", "DAYS", "DATES"
    private const val KEY_VOICE_SCHEDULE_DAYS = "voice_schedule_days" // "TUESDAY,SATURDAY"
    private const val KEY_VOICE_SCHEDULE_DATES = "voice_schedule_dates" // "2026-10-05,2026-10-06"

    private const val KEY_AUTO_NEXT_ENABLED = "tts_auto_next_enabled"
    private const val KEY_AUTO_NEXT_DELAY = "tts_auto_next_delay_seconds"
    private const val KEY_PRIMARY_TEMPLATE = "tts_primary_template"
    private const val KEY_STANDBY_TEMPLATE = "tts_standby_template"
    private const val KEY_VOICE_KEY_SELECTION_MODE = "voice_key_selection_mode"
    private const val KEY_VOICE_MANUAL_SLOT = "voice_manual_slot"

    const val DEFAULT_PRIMARY_TEMPLATE = "टोकन नंबर {tokenNumber}, श्री {devoteeName} जी, आपका नंबर आ गया है, तुरंत गुरुजी के समीप आएं।"
    const val DEFAULT_STANDBY_TEMPLATE = "टोकन नंबर {nextTokenNumber}, श्री {nextDevoteeName} जी, इसके बाद आपका नंबर है। कृपया आप {currentDevoteeName} जी के पीछे जाके बैठ जाएं, और बाकी सारे लोग पीछे जाके आराम से बैठ जाएं, जब तुम्हारा नंबर आएगा तो तुम्हें सूचित किया जाएगा।"

    const val PRESET_ELEVENLABS_MALE = "ELEVENLABS_MALE"
    const val PRESET_ELEVENLABS_FEMALE = "ELEVENLABS_FEMALE"
    const val PRESET_NATURAL_MALE = "NATURAL_MALE"
    const val PRESET_NATURAL_FEMALE = "NATURAL_FEMALE"
    const val PRESET_CUSTOM_RECORDED = "CUSTOM_RECORDED"
    const val PRESET_OFFLINE_DEVICE = "OFFLINE_DEVICE"

    // Backward compatibility aliases
    const val PRESET_GURU_CALM = PRESET_ELEVENLABS_MALE
    const val PRESET_FEMALE_SWEET = PRESET_ELEVENLABS_FEMALE
    const val PRESET_ANNOUNCER_MALE = PRESET_ELEVENLABS_MALE
    const val PRESET_SEVIKA_FEMALE = PRESET_ELEVENLABS_FEMALE
    const val PRESET_YOUTH_CRISP = PRESET_ELEVENLABS_MALE
    const val PRESET_TRADITIONAL_VYAS = PRESET_ELEVENLABS_MALE

    val AVAILABLE_VOICE_PRESETS = listOf(
        VoicePresetInfo(
            id = PRESET_ELEVENLABS_MALE,
            nameHindi = "HD स्टूडियो पुरुष स्वर (ElevenLabs Brian)",
            nameEnglish = "HD Devotional Male Voice (ElevenLabs)",
            gender = "MALE",
            category = "MALE",
            description = "100% असली स्टूडियो रिकॉर्डेड उद्घोषक स्वर (1 से 150 तक प्री-लोडेड)",
            pitch = 1.0f,
            speechRate = 1.0f,
            icon = "🎙️",
            speechStyle = "STUDIO_HUMAN"
        ),
        VoicePresetInfo(
            id = PRESET_ELEVENLABS_FEMALE,
            nameHindi = "HD स्टूडियो महिला स्वर (ElevenLabs Sarah)",
            nameEnglish = "HD Devotional Female Voice (ElevenLabs)",
            gender = "FEMALE",
            category = "FEMALE",
            description = "100% असली स्टूडियो रिकॉर्डेड सेविका स्वर (1 से 150 तक प्री-लोडेड)",
            pitch = 1.0f,
            speechRate = 1.0f,
            icon = "👩",
            speechStyle = "STUDIO_HUMAN"
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

    // --- Standby Devotee & Crowd Control State ---
    private var standbyCountdownJob: Job? = null
    private val _standbySecondsRemaining = MutableStateFlow<Int?>(null)
    val standbySecondsRemaining: StateFlow<Int?> = _standbySecondsRemaining.asStateFlow()

    private val _standbyNextToken = MutableStateFlow<Int?>(null)
    val standbyNextToken: StateFlow<Int?> = _standbyNextToken.asStateFlow()

    private val _standbyNextName = MutableStateFlow<String>("")
    val standbyNextName: StateFlow<String> = _standbyNextName.asStateFlow()

    private val _currentDevoteeName = MutableStateFlow<String>("")
    val currentDevoteeName: StateFlow<String> = _currentDevoteeName.asStateFlow()

    // --- ElevenLabs Multi-Key Balance Monitoring State ---
    private val _elevenLabsKeyBalances = MutableStateFlow<List<ElevenLabsKeyInfo>>(emptyList())
    val elevenLabsKeyBalances: StateFlow<List<ElevenLabsKeyInfo>> = _elevenLabsKeyBalances.asStateFlow()

    private val _isRefreshingBalances = MutableStateFlow(false)
    val isRefreshingBalances: StateFlow<Boolean> = _isRefreshingBalances.asStateFlow()

    // Media Recorder & Player for Live In-App Recordings
    private var activeMediaRecorder: MediaRecorder? = null
    private var activeMediaPlayer: MediaPlayer? = null
    var isCurrentlyRecording: Boolean = false
        private set

    // --- Preferences Getters & Setters ---

    fun isVoiceMasterEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_VOICE_MASTER_ENABLED, true)
    }

    fun setVoiceMasterEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VOICE_MASTER_ENABLED, enabled).apply()
        if (!enabled) stop()
    }

    fun getVoiceScheduleMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VOICE_SCHEDULE_MODE, "ALWAYS") ?: "ALWAYS"
    }

    fun setVoiceScheduleMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_VOICE_SCHEDULE_MODE, mode).apply()
    }

    fun getVoiceScheduleDays(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VOICE_SCHEDULE_DAYS, "TUESDAY,SATURDAY") ?: "TUESDAY,SATURDAY"
    }

    fun setVoiceScheduleDays(context: Context, days: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_VOICE_SCHEDULE_DAYS, days).apply()
    }

    fun getVoiceScheduleDates(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VOICE_SCHEDULE_DATES, "") ?: ""
    }

    fun setVoiceScheduleDates(context: Context, dates: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_VOICE_SCHEDULE_DATES, dates).apply()
    }

    fun isVoiceServiceActiveToday(context: Context): Boolean {
        if (!isVoiceMasterEnabled(context)) return false
        val mode = getVoiceScheduleMode(context)
        return when (mode) {
            "DAYS" -> {
                val cal = java.util.Calendar.getInstance()
                val dayOfWeek = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
                    java.util.Calendar.SUNDAY -> "SUNDAY"
                    java.util.Calendar.MONDAY -> "MONDAY"
                    java.util.Calendar.TUESDAY -> "TUESDAY"
                    java.util.Calendar.WEDNESDAY -> "WEDNESDAY"
                    java.util.Calendar.THURSDAY -> "THURSDAY"
                    java.util.Calendar.FRIDAY -> "FRIDAY"
                    java.util.Calendar.SATURDAY -> "SATURDAY"
                    else -> ""
                }
                val allowedDays = getVoiceScheduleDays(context).split(",").map { it.trim().uppercase() }
                allowedDays.contains(dayOfWeek)
            }
            "DATES" -> {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val todayStr = sdf.format(java.util.Date())
                val allowedDates = getVoiceScheduleDates(context).split(",").map { it.trim() }
                allowedDates.contains(todayStr)
            }
            else -> true // "ALWAYS"
        }
    }

    fun getGoogleTtsApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GOOGLE_TTS_API_KEY, "") ?: ""
    }

    fun setGoogleTtsApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GOOGLE_TTS_API_KEY, key.trim()).apply()
    }

    fun isVoiceKeyManualMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VOICE_KEY_SELECTION_MODE, "AUTO") == "MANUAL"
    }

    fun setVoiceKeyManualMode(context: Context, isManual: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_VOICE_KEY_SELECTION_MODE, if (isManual) "MANUAL" else "AUTO").apply()
    }

    fun getVoiceManualSlot(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_VOICE_MANUAL_SLOT, 1)
    }

    fun setVoiceManualSlot(context: Context, slot: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_VOICE_MANUAL_SLOT, slot.coerceIn(1, 11)).apply()
    }

    fun getElevenLabsApiKeyList(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isManual = prefs.getString(KEY_VOICE_KEY_SELECTION_MODE, "AUTO") == "MANUAL"
        val manualSlot = prefs.getInt(KEY_VOICE_MANUAL_SLOT, 1)

        if (isManual) {
            val key = getElevenLabsApiKey(context, manualSlot)
            if (key.isNotBlank()) {
                return listOf(key)
            }
        }

        val list = mutableListOf<String>()
        for (slot in 1..11) {
            val key = getElevenLabsApiKey(context, slot)
            if (key.isNotBlank()) list.add(key)
        }
        return list
    }

    fun getElevenLabsApiKey(context: Context, slot: Int = 1): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (slot) {
            1 -> {
                val k1 = prefs.getString(KEY_ELEVENLABS_API_KEY_1, null)?.trim()
                val legacy = prefs.getString(KEY_ELEVENLABS_API_KEY, null)?.trim()
                when {
                    !k1.isNullOrBlank() -> k1
                    !legacy.isNullOrBlank() -> legacy
                    else -> DEFAULT_ELEVENLABS_API_KEY_1
                }
            }
            2 -> {
                val k = prefs.getString(KEY_ELEVENLABS_API_KEY_2, null)?.trim()
                if (!k.isNullOrBlank()) k else DEFAULT_ELEVENLABS_API_KEY_2
            }
            3 -> {
                val k = prefs.getString(KEY_ELEVENLABS_API_KEY_3, null)?.trim()
                if (!k.isNullOrBlank()) k else DEFAULT_ELEVENLABS_API_KEY_3
            }
            4 -> {
                val k = prefs.getString(KEY_ELEVENLABS_API_KEY_4, null)?.trim()
                if (!k.isNullOrBlank()) k else DEFAULT_ELEVENLABS_API_KEY_4
            }
            5 -> {
                val k = prefs.getString(KEY_ELEVENLABS_API_KEY_5, null)?.trim()
                if (!k.isNullOrBlank()) k else DEFAULT_ELEVENLABS_API_KEY_5
            }
            6 -> {
                val k = prefs.getString(KEY_ELEVENLABS_API_KEY_6, null)?.trim()
                if (!k.isNullOrBlank()) k else DEFAULT_ELEVENLABS_API_KEY_6
            }
            7 -> {
                val k = prefs.getString(KEY_ELEVENLABS_API_KEY_7, null)?.trim()
                if (!k.isNullOrBlank()) k else DEFAULT_ELEVENLABS_API_KEY_7
            }
            8 -> {
                val k = prefs.getString(KEY_ELEVENLABS_API_KEY_8, null)?.trim()
                if (!k.isNullOrBlank()) k else DEFAULT_ELEVENLABS_API_KEY_8
            }
            9 -> {
                val k = prefs.getString(KEY_ELEVENLABS_API_KEY_9, null)?.trim()
                if (!k.isNullOrBlank()) k else DEFAULT_ELEVENLABS_API_KEY_9
            }
            10 -> {
                val k = prefs.getString(KEY_ELEVENLABS_API_KEY_10, null)?.trim()
                if (!k.isNullOrBlank()) k else DEFAULT_ELEVENLABS_API_KEY_10
            }
            11 -> {
                val k = prefs.getString(KEY_ELEVENLABS_API_KEY_11, null)?.trim()
                if (!k.isNullOrBlank()) k else DEFAULT_ELEVENLABS_API_KEY_11
            }
            else -> ""
        }
    }

    fun setElevenLabsApiKey(context: Context, key: String, slot: Int = 1) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val clean = key.trim()
        when (slot) {
            1 -> {
                prefs.edit().putString(KEY_ELEVENLABS_API_KEY_1, clean).putString(KEY_ELEVENLABS_API_KEY, clean).apply()
            }
            2 -> prefs.edit().putString(KEY_ELEVENLABS_API_KEY_2, clean).apply()
            3 -> prefs.edit().putString(KEY_ELEVENLABS_API_KEY_3, clean).apply()
            4 -> prefs.edit().putString(KEY_ELEVENLABS_API_KEY_4, clean).apply()
            5 -> prefs.edit().putString(KEY_ELEVENLABS_API_KEY_5, clean).apply()
            6 -> prefs.edit().putString(KEY_ELEVENLABS_API_KEY_6, clean).apply()
            7 -> prefs.edit().putString(KEY_ELEVENLABS_API_KEY_7, clean).apply()
            8 -> prefs.edit().putString(KEY_ELEVENLABS_API_KEY_8, clean).apply()
            9 -> prefs.edit().putString(KEY_ELEVENLABS_API_KEY_9, clean).apply()
            10 -> prefs.edit().putString(KEY_ELEVENLABS_API_KEY_10, clean).apply()
            11 -> prefs.edit().putString(KEY_ELEVENLABS_API_KEY_11, clean).apply()
        }
    }

    fun refreshAllKeyBalances(context: Context, onComplete: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            _isRefreshingBalances.value = true
            val results = mutableListOf<ElevenLabsKeyInfo>()
            for (slot in 1..11) {
                val key = getElevenLabsApiKey(context, slot)
                if (key.isNotBlank()) {
                    val info = ElevenLabsTtsEngine.fetchKeyBalance(key, slot)
                    results.add(info)
                }
            }
            _elevenLabsKeyBalances.value = results
            _isRefreshingBalances.value = false
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    fun isAutoNextEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_NEXT_ENABLED, true)
    }

    fun setAutoNextEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_NEXT_ENABLED, enabled).apply()
    }

    fun getAutoNextDelaySeconds(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_AUTO_NEXT_DELAY, 20)
    }

    fun setAutoNextDelaySeconds(context: Context, seconds: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_AUTO_NEXT_DELAY, seconds).apply()
    }

    fun getPrimaryTemplate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PRIMARY_TEMPLATE, DEFAULT_PRIMARY_TEMPLATE) ?: DEFAULT_PRIMARY_TEMPLATE
    }

    fun setPrimaryTemplate(context: Context, template: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PRIMARY_TEMPLATE, template).apply()
    }

    fun getStandbyTemplate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_STANDBY_TEMPLATE, DEFAULT_STANDBY_TEMPLATE) ?: DEFAULT_STANDBY_TEMPLATE
    }

    fun setStandbyTemplate(context: Context, template: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_STANDBY_TEMPLATE, template).apply()
    }

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

    fun playAudioFile(file: File, onComplete: (() -> Unit)? = null) {
        try {
            stopAudioPlayback()
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    try { it.release() } catch (e: Exception) {}
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

    fun stopAudioPlayback() {
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

    sealed class AudioSegment {
        data class RawRes(val resId: Int) : AudioSegment()
        data class Asset(val path: String) : AudioSegment()
        data class FileAudio(val file: File) : AudioSegment()
    }

    /**
     * Plays a sequence of audio clips back-to-back with zero-latency human-like pacing (Audio Stitching).
     */
    fun playAudioSegments(
        context: Context,
        segments: List<AudioSegment>,
        onFinished: (() -> Unit)? = null
    ) {
        if (segments.isEmpty()) {
            onFinished?.invoke()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            _isAnnouncing.value = true
            stopAudioPlayback()

            var index = 0

            fun playNext() {
                if (index >= segments.size) {
                    _isAnnouncing.value = false
                    onFinished?.invoke()
                    return
                }

                val currentSeg = segments[index]
                index++

                try {
                    val player = MediaPlayer()
                    var prepared = false

                    when (currentSeg) {
                        is AudioSegment.RawRes -> {
                            val afd = context.resources.openRawResourceFd(currentSeg.resId)
                            if (afd != null) {
                                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                afd.close()
                                player.prepare()
                                prepared = true
                            }
                        }
                        is AudioSegment.Asset -> {
                            try {
                                val afd = context.assets.openFd(currentSeg.path)
                                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                afd.close()
                                player.prepare()
                                prepared = true
                            } catch (e: Exception) {
                                val tempFile = File(context.cacheDir, "temp_asset_${System.currentTimeMillis()}.mp3")
                                context.assets.open(currentSeg.path).use { input ->
                                    FileOutputStream(tempFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                player.setDataSource(tempFile.absolutePath)
                                player.prepare()
                                prepared = true
                            }
                        }
                        is AudioSegment.FileAudio -> {
                            if (currentSeg.file.exists() && currentSeg.file.length() > 500) {
                                player.setDataSource(currentSeg.file.absolutePath)
                                player.prepare()
                                prepared = true
                            }
                        }
                    }

                    if (prepared) {
                        activeMediaPlayer = player
                        player.setOnCompletionListener {
                            try { it.release() } catch (e: Exception) {}
                            activeMediaPlayer = null
                            playNext()
                        }
                        player.start()
                    } else {
                        try { player.release() } catch (e: Exception) {}
                        playNext()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error playing audio segment: ${e.message}")
                    playNext()
                }
            }

            playNext()
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
        val saved = prefs.getString(KEY_VOICE_PRESET, PRESET_ELEVENLABS_MALE) ?: PRESET_ELEVENLABS_MALE
        return if (saved == PRESET_ELEVENLABS_MALE || saved == PRESET_ELEVENLABS_FEMALE || saved == PRESET_CUSTOM_RECORDED) {
            saved
        } else {
            PRESET_ELEVENLABS_MALE
        }
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
     */
    fun numberToHindiWords(num: Int): String {
        if (num <= 0) return num.toString()
        val words1To100 = arrayOf(
            "", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ", "दस",
            "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस", "बीस",
            "इक्कीस", "बाईस", "तेईस", "चौबीस", "पच्चीस", "छब्बीस", "सत्ताईस", "अट्ठाईस", "उनतीस", "तीस",
            "इकतीस", "बत्तीस", "तैंतीस", "चौंतीस", "पैंतीस", "छत्तीस", "सैंतीस", "अड़तीस", "उनतालीस", "चालीस",
            "इकतालीस", "बयालीस", "तैंतालीस", "चवालीस", "पैंतालीस", "छियालीस", "सैंतालीस", "अड़तीस", "उनचास", "पचास",
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
     * Resonant Temple Bell Chime:
     * First attempts to play high-fidelity brass temple bell WAV from res/raw/temple_bell.wav.
     * Falls back to dual-harmonic synthesis (528 Hz + 1056 Hz) via AudioTrack if file unavailable.
     */
    fun playTempleBell(context: Context, onFinished: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            var played = false
            try {
                val resId = context.resources.getIdentifier("temple_bell", "raw", context.packageName)
                if (resId != 0) {
                    val player = MediaPlayer.create(context, resId)
                    if (player != null) {
                        player.setOnCompletionListener { mp ->
                            try { mp.release() } catch (e: Exception) {}
                            onFinished?.invoke()
                        }
                        player.start()
                        played = true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error playing raw temple_bell: ${e.message}")
            }
            if (!played) {
                playTempleChime(onFinished)
            }
        }
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
     * Speaks devotional text using Google Cloud Neural2 (if key configured / cached)
     * with automatic seamless fallback to local high-quality Android TTS.
     */
    fun speakDevotionalText(
        context: Context,
        text: String,
        onFinished: (() -> Unit)? = null
    ) {
        if (isMuted(context) || !isVoiceServiceActiveToday(context)) {
            onFinished?.invoke()
            return
        }

        val apiKey = getGoogleTtsApiKey(context)
        val preset = getSelectedVoicePreset(context)

        CoroutineScope(Dispatchers.Main).launch {
            _isAnnouncing.value = true
            var handledByCloud = false

            if (apiKey.isNotBlank() && preset != PRESET_OFFLINE_DEVICE && preset != PRESET_CUSTOM_RECORDED) {
                val voiceName = if (preset == PRESET_NATURAL_FEMALE) {
                    GoogleCloudTtsEngine.VOICE_NEURAL2_FEMALE
                } else {
                    GoogleCloudTtsEngine.VOICE_NEURAL2_MALE
                }

                val audioFile = GoogleCloudTtsEngine.synthesizeSpeechToFile(
                    context = context,
                    text = text,
                    apiKey = apiKey,
                    voiceName = voiceName
                )

                if (audioFile != null && audioFile.exists() && audioFile.length() > 500) {
                    playAudioFile(audioFile) {
                        _isAnnouncing.value = false
                        onFinished?.invoke()
                    }
                    handledByCloud = true
                }
            }

            if (!handledByCloud) {
                initIfNeeded(context)
                applyVoiceSettings(context, preset)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isAnnouncing.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isAnnouncing.value = false
                        CoroutineScope(Dispatchers.Main).launch {
                            onFinished?.invoke()
                        }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isAnnouncing.value = false
                        CoroutineScope(Dispatchers.Main).launch {
                            onFinished?.invoke()
                        }
                    }
                })
                speakRaw(text)
            }
        }
    }

    /**
     * 📢 PRIMARY & TWO-STAGE TEMPLE TOKEN CALLING WITH CROWD CONTROL
     * 
     * Stage 1: "टोकन नंबर {४}, श्री रमेश कुमार जी, आपका नंबर आ गया है, तुरंत गुरुजी के समीप आएं।"
     * Stage 2 (Auto 20s or manual tap):
     * "टोकन नंबर {५}, श्री अंकित कुमार जी, अगला नंबर आपका है, कृपया रमेश कुमार जी के पीछे आकर बैठें, और सब पीछे जाके बैठ जाओ।"
     */
    fun announceNextToken(
        context: Context,
        tokenNumber: Int,
        devoteeName: String = "",
        city: String = "",
        repeatCount: Int = 1,
        nextTokenNumber: Int = 0,
        nextDevoteeName: String = "",
        autoNextSeconds: Int = -1
    ) {
        if (isMuted(context) || !isVoiceServiceActiveToday(context)) return

        // Cancel any pending countdown from a previous token call
        standbyCountdownJob?.cancel()
        _standbySecondsRemaining.value = null

        boostAudioVolumeForLoudspeaker(context)

        val cleanName = devoteeName.trim()
        val devDigits = toDevanagariDigits(tokenNumber)
        val hindiWords = numberToHindiWords(tokenNumber)

        val tokenSpoken = if (hindiWords.isNotBlank() && hindiWords != tokenNumber.toString()) {
            hindiWords
        } else {
            tokenNumber.toString()
        }

        // Format Primary announcement using configured template
        val template = getPrimaryTemplate(context)
        val primaryAnnouncementText = if (cleanName.isNotBlank()) {
            template.replace("{tokenNumber}", tokenSpoken)
                .replace("{devoteeName}", cleanName)
        } else {
            template.replace("{tokenNumber}", tokenSpoken)
                .replace("श्री {devoteeName} जी,", "")
                .replace("श्री {devoteeName} जी", "")
        }.trim()

        lastAnnouncementText = primaryAnnouncementText
        _currentAnnouncedToken.value = tokenNumber
        _currentAnnouncedText.value = primaryAnnouncementText
        _currentDevoteeName.value = cleanName
        _standbyNextToken.value = if (nextTokenNumber > 0) nextTokenNumber else null
        _standbyNextName.value = nextDevoteeName.trim()

        val activePreset = getSelectedVoicePreset(context)

        // 🌟 ELEVENLABS AUDIO STITCHING ARCHITECTURE:
        // Zero-credit, ultra-realistic human voice combining pre-baked audio clips (1 to 150)
        // with dynamic devotee name & high token synthesis from ElevenLabs (cached forever).
        // Uses Multi-Key Failover Pool (Keys 1 to 11) automatically!
        if (activePreset == PRESET_ELEVENLABS_MALE || activePreset == PRESET_ELEVENLABS_FEMALE) {
            val genderDir = if (activePreset == PRESET_ELEVENLABS_FEMALE) "female" else "male"
            val voiceId = if (activePreset == PRESET_ELEVENLABS_FEMALE) ElevenLabsTtsEngine.VOICE_FEMALE_SARAH else ElevenLabsTtsEngine.VOICE_MALE_BRIAN
            val apiKeys = getElevenLabsApiKeyList(context)

            CoroutineScope(Dispatchers.Main).launch {
                val segments = mutableListOf<AudioSegment>()

                // 1. Unified Token Number: "टोकन नंबर चार" (Zero English accent, pure Indian intonation)
                val tokenNumAsset = "audio/$genderDir/token_num_$tokenNumber.mp3"
                if (assetExists(context, tokenNumAsset)) {
                    segments.add(AudioSegment.Asset(tokenNumAsset))
                } else {
                    // For tokens beyond pre-baked asset (or > 150), synthesize "टोकन नंबर [संख्या]" using ElevenLabs pool & cache!
                    val tokenIntroText = "टोकन नंबर $tokenSpoken"
                    val tokenNumFile = if (apiKeys.isNotEmpty()) {
                        ElevenLabsTtsEngine.synthesizeSpeechWithPool(
                            context = context,
                            text = tokenIntroText,
                            apiKeys = apiKeys,
                            voiceId = voiceId
                        )
                    } else {
                        val cached = ElevenLabsTtsEngine.getCachedAudioFile(context, tokenIntroText, voiceId)
                        if (cached.exists() && cached.length() > 500) cached else null
                    }

                    if (tokenNumFile != null && tokenNumFile.exists()) {
                        segments.add(AudioSegment.FileAudio(tokenNumFile))
                    } else {
                        segments.add(AudioSegment.Asset("audio/$genderDir/token_intro.mp3"))
                        val numAsset = "audio/$genderDir/num_$tokenNumber.mp3"
                        if (assetExists(context, numAsset)) {
                            segments.add(AudioSegment.Asset(numAsset))
                        }
                    }
                }

                if (cleanName.isNotBlank()) {
                    // 3. Salutation: "श्री"
                    segments.add(AudioSegment.Asset("audio/$genderDir/shri.mp3"))

                    // 4. Devotee Name (Dynamic Synthesis with Multi-Key Failover Pool / Local Cache)
                    val nameFile = if (apiKeys.isNotEmpty()) {
                        ElevenLabsTtsEngine.synthesizeSpeechWithPool(
                            context = context,
                            text = cleanName,
                            apiKeys = apiKeys,
                            voiceId = voiceId
                        )
                    } else {
                        val cached = ElevenLabsTtsEngine.getCachedAudioFile(context, cleanName, voiceId)
                        if (cached.exists() && cached.length() > 500) cached else null
                    }

                    if (nameFile != null && nameFile.exists()) {
                        segments.add(AudioSegment.FileAudio(nameFile))
                    }

                    // 5. Guruji call prompt
                    segments.add(AudioSegment.Asset("audio/$genderDir/call_guruji.mp3"))
                } else {
                    segments.add(AudioSegment.Asset("audio/$genderDir/call_guruji_direct.mp3"))
                }

                playAudioSegments(context, segments) {
                    triggerStandbyCountdownIfNeeded(context, cleanName, nextTokenNumber, nextDevoteeName, autoNextSeconds)
                }
            }
            return
        }

        // Custom recording or cloud fallback (No bell chime)
        if (activePreset == PRESET_CUSTOM_RECORDED && hasCustomRecording(context)) {
            playCustomRecording(context) {
                if (cleanName.isNotBlank()) {
                    speakDevotionalText(context, "श्री $cleanName जी, कृपया पधारें।") {
                        triggerStandbyCountdownIfNeeded(context, cleanName, nextTokenNumber, nextDevoteeName, autoNextSeconds)
                    }
                } else {
                    _isAnnouncing.value = false
                    triggerStandbyCountdownIfNeeded(context, cleanName, nextTokenNumber, nextDevoteeName, autoNextSeconds)
                }
            }
        } else {
            speakDevotionalText(context, primaryAnnouncementText) {
                triggerStandbyCountdownIfNeeded(context, cleanName, nextTokenNumber, nextDevoteeName, autoNextSeconds)
            }
        }
    }

    private fun triggerStandbyCountdownIfNeeded(
        context: Context,
        currentDevoteeName: String,
        nextTokenNumber: Int,
        nextDevoteeName: String,
        autoNextSecondsParam: Int
    ) {
        val isAutoEnabled = isAutoNextEnabled(context)
        if (!isAutoEnabled || nextTokenNumber <= 0) {
            _standbySecondsRemaining.value = null
            return
        }

        val delaySecs = if (autoNextSecondsParam >= 0) autoNextSecondsParam else getAutoNextDelaySeconds(context)
        if (delaySecs <= 0) {
            announceStandbyDevotee(context, currentDevoteeName, nextTokenNumber, nextDevoteeName)
            return
        }

        standbyCountdownJob?.cancel()
        standbyCountdownJob = CoroutineScope(Dispatchers.Main).launch {
            for (sec in delaySecs downTo 1) {
                _standbySecondsRemaining.value = sec
                delay(1000)
            }
            _standbySecondsRemaining.value = 0
            delay(300)
            _standbySecondsRemaining.value = null
            announceStandbyDevotee(context, currentDevoteeName, nextTokenNumber, nextDevoteeName)
        }
    }

    /**
     * 📢 STANDBY DEVOTEE & CROWD CONTROL ANNOUNCEMENT:
     * "टोकन नंबर {next}, श्री {nextDevotee} जी, अगला नंबर आपका है, कृपया {currentDevotee} जी के पीछे आकर बैठें, और सब पीछे जाके बैठ जाओ।"
     */
    fun announceStandbyDevotee(
        context: Context,
        currentDevoteeName: String = _currentDevoteeName.value,
        nextTokenNumber: Int = _standbyNextToken.value ?: 0,
        nextDevoteeName: String = _standbyNextName.value
    ) {
        if (isMuted(context) || !isVoiceServiceActiveToday(context) || nextTokenNumber <= 0) return

        standbyCountdownJob?.cancel()
        _standbySecondsRemaining.value = null

        val cleanNextName = nextDevoteeName.trim()
        val cleanCurrentName = currentDevoteeName.trim()

        val activePreset = getSelectedVoicePreset(context)
        if (activePreset == PRESET_ELEVENLABS_MALE || activePreset == PRESET_ELEVENLABS_FEMALE) {
            val genderDir = if (activePreset == PRESET_ELEVENLABS_FEMALE) "female" else "male"
            val voiceId = if (activePreset == PRESET_ELEVENLABS_FEMALE) ElevenLabsTtsEngine.VOICE_FEMALE_SARAH else ElevenLabsTtsEngine.VOICE_MALE_BRIAN
            val apiKeys = getElevenLabsApiKeyList(context)

            val nextHindiWords = numberToHindiWords(nextTokenNumber)
            val nextTokenSpoken = if (nextHindiWords.isNotBlank() && nextHindiWords != nextTokenNumber.toString()) {
                nextHindiWords
            } else {
                nextTokenNumber.toString()
            }

            CoroutineScope(Dispatchers.Main).launch {
                val segments = mutableListOf<AudioSegment>()

                // 1. Unified Token Number: "टोकन नंबर पांच" (Zero English accent, pure Indian intonation)
                val tokenNumAsset = "audio/$genderDir/token_num_$nextTokenNumber.mp3"
                if (assetExists(context, tokenNumAsset)) {
                    segments.add(AudioSegment.Asset(tokenNumAsset))
                } else {
                    // Dynamic synthesis for tokens beyond pre-baked assets (or > 150)
                    val tokenIntroText = "टोकन नंबर $nextTokenSpoken"
                    val tokenNumFile = if (apiKeys.isNotEmpty()) {
                        ElevenLabsTtsEngine.synthesizeSpeechWithPool(
                            context = context,
                            text = tokenIntroText,
                            apiKeys = apiKeys,
                            voiceId = voiceId
                        )
                    } else {
                        val cached = ElevenLabsTtsEngine.getCachedAudioFile(context, tokenIntroText, voiceId)
                        if (cached.exists() && cached.length() > 500) cached else null
                    }

                    if (tokenNumFile != null && tokenNumFile.exists()) {
                        segments.add(AudioSegment.FileAudio(tokenNumFile))
                    } else {
                        segments.add(AudioSegment.Asset("audio/$genderDir/token_intro.mp3"))
                        val numAsset = "audio/$genderDir/num_$nextTokenNumber.mp3"
                        if (assetExists(context, numAsset)) {
                            segments.add(AudioSegment.Asset(numAsset))
                        }
                    }
                }

                if (cleanNextName.isNotBlank()) {
                    // 3. Salutation: "श्री"
                    segments.add(AudioSegment.Asset("audio/$genderDir/shri.mp3"))

                    // 4. Next Devotee Name (Dynamic Synthesis with Multi-Key Failover Pool / Local Cache)
                    val nextNameFile = if (apiKeys.isNotEmpty()) {
                        ElevenLabsTtsEngine.synthesizeSpeechWithPool(
                            context = context,
                            text = cleanNextName,
                            apiKeys = apiKeys,
                            voiceId = voiceId
                        )
                    } else {
                        val cached = ElevenLabsTtsEngine.getCachedAudioFile(context, cleanNextName, voiceId)
                        if (cached.exists() && cached.length() > 500) cached else null
                    }

                    if (nextNameFile != null && nextNameFile.exists()) {
                        segments.add(AudioSegment.FileAudio(nextNameFile))
                    }

                    // 5. Standby prompt: "कृपया इनके पीछे आकर बैठें..."
                    segments.add(AudioSegment.Asset("audio/$genderDir/standby_behind_prompt.mp3"))
                } else {
                    segments.add(AudioSegment.Asset("audio/$genderDir/standby_direct.mp3"))
                }

                playAudioSegments(context, segments)
            }
            return
        }

        val nextDevDigits = toDevanagariDigits(nextTokenNumber)
        val nextHindiWords = numberToHindiWords(nextTokenNumber)

        val nextTokenSpoken = if (nextHindiWords.isNotBlank() && nextHindiWords != nextTokenNumber.toString()) {
            nextHindiWords
        } else {
            nextTokenNumber.toString()
        }

        val template = getStandbyTemplate(context)
        var standbyText = template.replace("{nextTokenNumber}", nextTokenSpoken)

        standbyText = if (cleanNextName.isNotBlank()) {
            standbyText.replace("{nextDevoteeName}", cleanNextName)
        } else {
            standbyText.replace("श्री {nextDevoteeName} जी,", "")
                .replace("श्री {nextDevoteeName} जी", "")
        }

        standbyText = if (cleanCurrentName.isNotBlank()) {
            standbyText.replace("{currentDevoteeName}", cleanCurrentName)
        } else {
            standbyText.replace("कृपया {currentDevoteeName} जी के पीछे", "कृपया आगे")
                .replace("{currentDevoteeName} जी", "आगे वाले भक्त")
        }

        speakDevotionalText(context, standbyText)
    }

    /**
     * Triggered by admin tapping [📢 अगला टोकन {X} तैयार करें] manually.
     */
    fun announceStandbyImmediately(context: Context) {
        val nextNum = _standbyNextToken.value ?: return
        announceStandbyDevotee(
            context = context,
            currentDevoteeName = _currentDevoteeName.value,
            nextTokenNumber = nextNum,
            nextDevoteeName = _standbyNextName.value
        )
    }

    /**
     * Cancels the active auto-standby countdown if sevadar does not want it to play.
     */
    fun cancelStandbyCountdown() {
        standbyCountdownJob?.cancel()
        _standbySecondsRemaining.value = null
    }

    fun testVoice(context: Context, presetId: String) {
        boostAudioVolumeForLoudspeaker(context)
        if (presetId == PRESET_CUSTOM_RECORDED) {
            if (hasCustomRecording(context)) {
                playCustomRecording(context)
            } else {
                initIfNeeded(context)
                speakRaw("कृपया नीचे दिए गए माइक रिकॉर्ड बटन से आश्रम की अपनी वास्तविक आवाज़ रिकॉर्ड करें।")
            }
            return
        }

        if (presetId == PRESET_ELEVENLABS_MALE || presetId == PRESET_ELEVENLABS_FEMALE) {
            val genderDir = if (presetId == PRESET_ELEVENLABS_FEMALE) "female" else "male"
            val voiceId = if (presetId == PRESET_ELEVENLABS_FEMALE) ElevenLabsTtsEngine.VOICE_FEMALE_SARAH else ElevenLabsTtsEngine.VOICE_MALE_BRIAN
            val apiKey = getElevenLabsApiKey(context)

            CoroutineScope(Dispatchers.Main).launch {
                val segments1 = mutableListOf<AudioSegment>()
                val tokenNum1Asset = "audio/$genderDir/token_num_1.mp3"
                if (assetExists(context, tokenNum1Asset)) {
                    segments1.add(AudioSegment.Asset(tokenNum1Asset))
                } else {
                    segments1.add(AudioSegment.Asset("audio/$genderDir/token_intro.mp3"))
                    segments1.add(AudioSegment.Asset("audio/$genderDir/num_1.mp3"))
                }
                segments1.add(AudioSegment.Asset("audio/$genderDir/shri.mp3"))

                val testNameFile = if (apiKey.isNotBlank()) {
                    ElevenLabsTtsEngine.synthesizeSpeechToFile(context, "रमेश कुमार", apiKey, voiceId)
                } else null
                if (testNameFile != null && testNameFile.exists()) {
                    segments1.add(AudioSegment.FileAudio(testNameFile))
                }
                segments1.add(AudioSegment.Asset("audio/$genderDir/call_guruji.mp3"))

                playAudioSegments(context, segments1) {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(1200)
                        val segments2 = mutableListOf<AudioSegment>()
                        val tokenNum2Asset = "audio/$genderDir/token_num_2.mp3"
                        if (assetExists(context, tokenNum2Asset)) {
                            segments2.add(AudioSegment.Asset(tokenNum2Asset))
                        } else {
                            segments2.add(AudioSegment.Asset("audio/$genderDir/token_intro.mp3"))
                            segments2.add(AudioSegment.Asset("audio/$genderDir/num_2.mp3"))
                        }
                        segments2.add(AudioSegment.Asset("audio/$genderDir/shri.mp3"))

                        val testStandbyNameFile = if (apiKey.isNotBlank()) {
                            ElevenLabsTtsEngine.synthesizeSpeechToFile(context, "अंकित कुमार", apiKey, voiceId)
                        } else null
                        if (testStandbyNameFile != null && testStandbyNameFile.exists()) {
                            segments2.add(AudioSegment.FileAudio(testStandbyNameFile))
                        }
                        segments2.add(AudioSegment.Asset("audio/$genderDir/standby_behind_prompt.mp3"))

                        playAudioSegments(context, segments2)
                    }
                }
            }
            return
        }

        val testPrimary = "टोकन नंबर एक, श्री रमेश कुमार जी, आपका नंबर आ गया है, तुरंत गुरुजी के समीप आएं।"
        val testStandby = "टोकन नंबर दो, श्री अंकित कुमार जी, इसके बाद आपका नंबर है। कृपया आप राकेश कुमार जी के पीछे जाके बैठ जाएं, और बाकी सारे लोग पीछे जाके आराम से बैठ जाएं, जब तुम्हारा नंबर आएगा तो तुम्हें सूचित किया जाएगा।"

        speakDevotionalText(context, testPrimary) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(1200)
                speakDevotionalText(context, testStandby)
            }
        }
    }

    fun repeatLastAnnouncement(context: Context) {
        if (lastAnnouncementText.isNotBlank()) {
            val tokenNum = _currentAnnouncedToken.value ?: 0
            announceNextToken(context, tokenNum, lastAnnouncementText)
        }
    }

    fun speak(context: Context, text: String) {
        speakDevotionalText(context, text)
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
        standbyCountdownJob?.cancel()
        _standbySecondsRemaining.value = null
        stopAudioPlayback()
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        standbyCountdownJob?.cancel()
        _standbySecondsRemaining.value = null
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

    private fun assetExists(context: Context, path: String): Boolean {
        return try {
            context.assets.open(path).close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
