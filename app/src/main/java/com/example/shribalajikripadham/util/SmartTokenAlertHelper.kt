package com.example.shribalajikripadham.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.shribalajikripadham.MainActivity
import com.example.shribalajikripadham.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट)
 * 🔔 प्रो स्मार्ट टोकन कॉलिंग व वाइब्रेशन अलर्ट इंजन (Pro Smart Token Calling Alert Engine)
 *
 * यह इंजन रविवार व मंगलवार दरबार में भक्त के टोकन की स्थिति की लाइव निगरानी करता है।
 * जैसे ही भक्त का टोकन समीप आता है (5 या 10 टोकन शेष) अथवा नंबर आ जाता है, यह इंजन:
 * 1. 📳 तीव्र मल्टी-पल्स वाइब्रेशन (Strong Multi-Pulse Haptic Vibration)
 * 2. 🔊 अलार्म स्तर की पावन घंटी / रिंगटोन (Sacred Alarm Chime)
 * 3. 🗣️ शुद्ध हिंदी में स्पष्ट वॉइस अनाउंसमेंट (Hindi Text-to-Speech Voice Announcement)
 * 4. 📲 हेड्स-अप हाई-प्रायोरिटी नोटिफिकेशन (High Priority Notification with Sound & Lights)
 * 5. 🔔 इन-ऐप गोल्डन अलर्ट डायलॉग (Interactive In-App Alert Dialog)
 * निष्पादित करता है।
 */
object SmartTokenAlertHelper {

    private const val TAG = "SmartTokenAlertHelper"

    const val CHANNEL_ID = "sbkd_pro_token_calling"
    const val CHANNEL_NAME = "🔔 श्री बालाजी टोकन कॉलिंग अलर्ट (Pro)"
    const val CHANNEL_DESCRIPTION = "दरबार में भक्त का टोकन नंबर आने या समीप होने पर तत्काल तीव्र वाइब्रेशन, घंटी व हिंदी वॉइस अलर्ट।"

    private const val PREFS_NAME = "sbkd_token_alert_prefs"
    const val KEY_ALERT_ENABLED = "is_alert_enabled"
    const val KEY_VOICE_ENABLED = "is_voice_enabled"
    const val KEY_VIBRATE_ENABLED = "is_vibrate_enabled"
    const val KEY_ALERT_BEFORE_COUNT = "alert_before_count" // default 5 tokens

    data class InAppTokenAlert(
        val tokenNumber: Int,
        val currentServing: Int,
        val remaining: Int,
        val isTurnNow: Boolean,
        val title: String,
        val message: String
    )

    private val _inAppAlertState = MutableStateFlow<InAppTokenAlert?>(null)
    val inAppAlertState = _inAppAlertState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingSpeech: String? = null

    /**
     * TextToSpeech इंजन को इनिशियलाइज़ करता है (Hindi Locale)
     */
    fun initTts(context: Context) {
        if (tts == null) {
            try {
                tts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        val result = tts?.setLanguage(Locale.forLanguageTag("hi-IN"))
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            tts?.setLanguage(Locale.getDefault())
                        }
                        tts?.setPitch(1.0f)
                        tts?.setSpeechRate(0.92f) // थोड़ा सहज और स्पष्ट बोलने हेतु
                        isTtsInitialized = true
                        pendingSpeech?.let { text ->
                            speak(text)
                            pendingSpeech = null
                        }
                    } else {
                        Log.e(TAG, "TTS initialization failed: $status")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception initializing TTS: ${e.message}")
            }
        }
    }

    /**
     * हिंदी में वॉइस अनाउंसमेंट बोलता है
     */
    fun speak(text: String) {
        try {
            if (isTtsInitialized && tts != null) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SBKD_TOKEN_ALERT")
            } else {
                pendingSpeech = text
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to speak: ${e.message}")
        }
    }

    /**
     * तीव्र वाइब्रेशन ट्रिगर करता है (जेब में फोन होने पर भी महसूस हो)
     */
    fun vibrate(context: Context) {
        try {
            val pattern = longArrayOf(0, 800, 250, 800, 250, 1200) // 3 शक्तिशाली वाइब्रेशन झटके
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibrate error: ${e.message}")
        }
    }

    private var activeMediaPlayer: MediaPlayer? = null
    private var activeRingtone: Ringtone? = null
    private val chimeHandler = Handler(Looper.getMainLooper())
    private val stopChimeRunnable = Runnable {
        stopChimeOnly()
    }

    private fun stopChimeOnly() {
        try {
            activeMediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {}
        activeMediaPlayer = null

        try {
            activeRingtone?.let {
                if (it.isPlaying) it.stop()
            }
        } catch (e: Exception) {}
        activeRingtone = null
    }

    /**
     * तत्काल सभी ध्वनि, रिंगटोन, वाइब्रेशन और टीटीएस उद्घोषणा को रोक देता है
     */
    fun stopAllAlerts(context: Context? = null) {
        chimeHandler.removeCallbacks(stopChimeRunnable)
        stopChimeOnly()
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        } catch (e: Exception) {}
        context?.let { ctx ->
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.cancel()
            } catch (e: Exception) {}
        }
        _inAppAlertState.value = null
    }

    /**
     * मंदिर की पावन घंटी (temple_bell.wav, 1.6s) या छोटी सौम्य नोटिफिकेशन बीप बजाता है।
     * किसी भी परिस्थिति में 30-60 सेकंड का लंबा अलार्म नहीं बजाएगा।
     * अधिकतम 2.5 सेकंड में स्वतः पूर्णतः शांत हो जाएगा।
     */
    fun playAlertChime(context: Context) {
        stopChimeOnly()
        chimeHandler.removeCallbacks(stopChimeRunnable)

        try {
            // 1. प्राथमिकता: श्री बालाजी मंदिर की पावन पीतल घंटी WAV (res/raw/temple_bell.wav, 1.6s)
            val resId = context.resources.getIdentifier("temple_bell", "raw", context.packageName)
            if (resId != 0) {
                val mp = MediaPlayer.create(context.applicationContext, resId)
                if (mp != null) {
                    activeMediaPlayer = mp
                    mp.setOnCompletionListener {
                        try { mp.release() } catch (e: Exception) {}
                        if (activeMediaPlayer == mp) activeMediaPlayer = null
                    }
                    mp.start()
                    // 2.5 सेकंड बाद स्वतः फोर्स-स्टॉप ताकि बैकग्राउंड में कभी न अटके
                    chimeHandler.postDelayed(stopChimeRunnable, 2500L)
                    return
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Temple bell raw playback fallback: ${e.message}")
        }

        // 2. वैकल्पिक: छोटी सौम्य नोटिफिकेशन बीप (TYPE_NOTIFICATION - कतई TYPE_ALARM नहीं)
        try {
            val notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, notifUri)
            if (ringtone != null) {
                activeRingtone = ringtone
                ringtone.play()
                // 2.0 सेकंड बाद स्वतः बंद
                chimeHandler.postDelayed(stopChimeRunnable, 2000L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Notification chime play error: ${e.message}")
        }
    }

    /**
     * प्रो नोटिफिकेशन चैनल तैयार करता है (सौम्य नोटिफिकेशन टोन व वाइब्रेशन सहित)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400) // संक्षिप्त सौम्य कंपन
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(soundUri, audioAttributes)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * हाई-प्रायोरिटी सिस्टम नोटिफिकेशन भेजता है
     */
    fun showTokenNotification(
        context: Context,
        title: String,
        message: String,
        isTurnNow: Boolean,
        notificationId: Int = if (isTurnNow) 10006 else 10007
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_DEVOTEE_TOKEN", true)
            putExtra("NOTIFICATION_TITLE", title)
            putExtra("NOTIFICATION_MSG", message)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
        } catch (e: Exception) { null }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_balaji)
            .apply {
                if (largeIcon != null) setLargeIcon(largeIcon)
            }
            .setColor(0xFFB71C1C.toInt()) // गहरा लाल / सिंदूरी
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * वर्तमान सेवारत टोकन नंबर को पहले से देखा हुआ (Acknowledged) चिह्नित करता है
     * ताकि टोकन जनरेट करने या टोकन देखने पर तुरंत अलार्म न बजे।
     */
    fun markCurrentServingAcknowledged(context: Context, servingNumber: Int) {
        if (servingNumber <= 0) return
        try {
            val myTokPrefs = context.getSharedPreferences("sbkd_devotee_my_token_prefs", Context.MODE_PRIVATE)
            myTokPrefs.edit().putInt("last_alerted_serving", servingNumber).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark serving acknowledged: ${e.message}")
        }
    }

    /**
     * टोकन अलर्ट की स्थिति की जांच कर आवश्यक होने पर सौम्य घंटी (1.6s), वाइब्रेट, वॉइस व नोटिफिकेशन ट्रिगर करता है
     * केवल तभी ट्रिगर होता है जब कतार वास्तव में आगे बढ़ती है (currentServing > lastAlertServing)।
     */
    fun evaluateAndTriggerAlert(
        context: Context,
        myToken: Int,
        currentServing: Int
    ) {
        if (myToken <= 0 || currentServing <= 0) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_ALERT_ENABLED, true)
        if (!isEnabled) return

        val isVoiceEnabled = prefs.getBoolean(KEY_VOICE_ENABLED, true)
        val isVibrateEnabled = prefs.getBoolean(KEY_VIBRATE_ENABLED, true)
        val alertBeforeCount = prefs.getInt(KEY_ALERT_BEFORE_COUNT, 5)

        val myTokPrefs = context.getSharedPreferences("sbkd_devotee_my_token_prefs", Context.MODE_PRIVATE)
        val lastAlertServing = myTokPrefs.getInt("last_alerted_serving", 0)

        // केवल तभी बजे जब कतार वास्तव में आगे बढ़ी हो (कदापि पहले से देखे नंबर या पुराने नंबर पर नहीं)
        if (currentServing <= lastAlertServing) return

        val isTurnNow = currentServing == myToken
        val remaining = myToken - currentServing
        val isApproaching = remaining in 1..alertBeforeCount

        if (isTurnNow) {
            val title = "🔔 आपका टोकन नंबर $myToken आ चुका है!"
            val msg = "जय श्री बालाजी! आपका पावन दर्शन हेतु नंबर आ गया है। कृपया तुरंत पूज्य गुरुजी के समक्ष दरबार में पधारें!"
            val speech = "जय श्री बालाजी महाराज! आपका टोकन नंबर $myToken आ चुका है। कृपया दरबार में पधारें।"

            // 1. वाइब्रेशन (संक्षिप्त)
            if (isVibrateEnabled) vibrate(context)
            // 2. मंदिर की पावन घंटी (1.6 सेकंड)
            playAlertChime(context)
            // 3. संक्षिप्त वॉइस अनाउंसमेंट
            if (isVoiceEnabled) {
                initTts(context)
                speak(speech)
            }
            // 4. हेड्स-अप नोटिफिकेशन
            showTokenNotification(context, title, msg, isTurnNow = true)
            // 5. इन-ऐप डायलॉग
            _inAppAlertState.value = InAppTokenAlert(
                tokenNumber = myToken,
                currentServing = currentServing,
                remaining = 0,
                isTurnNow = true,
                title = title,
                message = msg
            )

            myTokPrefs.edit().putInt("last_alerted_serving", currentServing).apply()

        } else if (isApproaching) {
            val title = "🚨 आपका टोकन समीप है (केवल $remaining टोकन शेष)"
            val msg = "वर्तमान में टोकन #$currentServing बुलाया जा रहा है। आपका टोकन #$myToken है। कृपया तुरंत आश्रम हॉल में उपस्थित रहें!"
            val speech = "जय श्री बालाजी महाराज! टोकन नंबर $myToken समीप है, केवल $remaining टोकन शेष हैं।"

            // 1. वाइब्रेशन
            if (isVibrateEnabled) vibrate(context)
            // 2. मंदिर की पावन घंटी (1.6 सेकंड)
            playAlertChime(context)
            // 3. संक्षिप्त वॉइस अनाउंसमेंट
            if (isVoiceEnabled) {
                initTts(context)
                speak(speech)
            }
            // 4. हेड्स-अप नोटिफिकेशन
            showTokenNotification(context, title, msg, isTurnNow = false)
            // 5. इन-ऐप डायलॉग
            _inAppAlertState.value = InAppTokenAlert(
                tokenNumber = myToken,
                currentServing = currentServing,
                remaining = remaining,
                isTurnNow = false,
                title = title,
                message = msg
            )

            myTokPrefs.edit().putInt("last_alerted_serving", currentServing).apply()
        }
    }

    /**
     * भक्त को अपनी आवाज़ और मंदिर घंटी टेस्ट करने की सुविधा देता है (1.6 सेकंड में स्वतः शांत)
     */
    fun testAlert(context: Context) {
        initTts(context)
        vibrate(context)
        playAlertChime(context)

        val testSpeech = "जय श्री बालाजी महाराज! टोकन अलर्ट सक्रिय है।"
        speak(testSpeech)

        Toast.makeText(
            context,
            "🔔 पावन मंदिर घंटी व अलर्ट टेस्ट (1.6 सेकंड में स्वतः शांत)",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun dismissInAppAlert() {
        stopAllAlerts()
        _inAppAlertState.value = null
    }

    // --- Preferences Getters / Setters ---
    fun isAlertEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ALERT_ENABLED, true)

    fun setAlertEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ALERT_ENABLED, enabled).apply()
    }

    fun isVoiceEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_VOICE_ENABLED, true)

    fun setVoiceEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply()
    }

    fun isVibrateEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_VIBRATE_ENABLED, true)

    fun setVibrateEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_VIBRATE_ENABLED, enabled).apply()
    }

    fun getAlertBeforeCount(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_ALERT_BEFORE_COUNT, 5)

    fun setAlertBeforeCount(context: Context, count: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_ALERT_BEFORE_COUNT, count).apply()
    }
}
