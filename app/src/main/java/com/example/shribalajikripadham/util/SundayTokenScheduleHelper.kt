package com.example.shribalajikripadham.util

import com.example.shribalajikripadham.data.model.AshramSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class SundayScheduleState {
    object Open : SundayScheduleState()
    data class CountdownActive(
        val openTimestamp: Long,
        val remainingMillis: Long,
        val formattedTarget: String,
        val messageHindi: String,
        val messageEnglish: String
    ) : SundayScheduleState()
    data class SundayBeforeStart(val messageHindi: String, val messageEnglish: String) : SundayScheduleState()
    data class SundayClosedEvening(val nextSundayDateStr: String, val messageHindi: String, val messageEnglish: String) : SundayScheduleState()
    data class NonSunday(val nextSundayDateStr: String, val messageHindi: String, val messageEnglish: String) : SundayScheduleState()
    data class ServiceDisabled(val messageHindi: String, val messageEnglish: String) : SundayScheduleState()
    data class CustomScheduled(val openTimestamp: Long, val formattedDate: String, val messageHindi: String, val messageEnglish: String) : SundayScheduleState()
}

object SundayTokenScheduleHelper {

    // Sunday window: 8:30 AM to 5:00 PM (17:00)
    const val SUNDAY_START_HOUR = 8
    const val SUNDAY_START_MINUTE = 30
    const val SUNDAY_END_HOUR = 17
    const val SUNDAY_END_MINUTE = 0

    // 12-hour pre-registration countdown window in milliseconds (12 * 60 * 60 * 1000)
    const val COUNTDOWN_WINDOW_MILLIS = 12 * 60 * 60 * 1000L

    /**
     * Calculates the upcoming Sunday at 8:00 AM.
     */
    fun getNextSundayDate(fromCal: Calendar = Calendar.getInstance()): Calendar {
        val cal = fromCal.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, SUNDAY_START_HOUR)
        cal.set(Calendar.MINUTE, SUNDAY_START_MINUTE)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dayOfWeek = fromCal.get(Calendar.DAY_OF_WEEK)
        val currentMinutes = fromCal.get(Calendar.HOUR_OF_DAY) * 60 + fromCal.get(Calendar.MINUTE)
        val startMinutes = SUNDAY_START_HOUR * 60 + SUNDAY_START_MINUTE

        if (dayOfWeek == Calendar.SUNDAY) {
            if (currentMinutes < startMinutes) {
                // Today is Sunday before 8:00 AM -> this morning!
                return cal
            } else {
                // Today is Sunday during/after darbar -> 7 days later
                cal.add(Calendar.DAY_OF_YEAR, 7)
                return cal
            }
        } else {
            // Monday to Saturday -> advance until Sunday
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal
        }
    }

    fun formatNextSundayDateHindi(nextSunday: Calendar): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("hi-IN"))
        return sdf.format(nextSunday.time)
    }

    fun formatNextSundayDateEnglish(nextSunday: Calendar): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
        return sdf.format(nextSunday.time)
    }

    /**
     * Formats remaining milliseconds into HH:MM:SS format
     */
    fun formatCountdown(remainingMillis: Long): String {
        if (remainingMillis <= 0L) return "00:00:00"
        val totalSeconds = remainingMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Formats remaining milliseconds into descriptive Hindi text
     */
    fun formatCountdownHindi(remainingMillis: Long): String {
        if (remainingMillis <= 0L) return "0 सेकंड"
        val totalSeconds = remainingMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return buildString {
            if (hours > 0) append("$hours घंटे ")
            if (minutes > 0 || hours > 0) append("$minutes मिनट ")
            append("$seconds सेकंड")
        }.trim()
    }

    fun evaluateSchedule(
        mode: String = "AUTO_SUNDAY",
        scheduledTimestamp: Long = 0L,
        nowMillis: Long = System.currentTimeMillis(),
        isServiceEnabled: Boolean = true
    ): SundayScheduleState {
        val tempSettings = AshramSettings(
            tokenServiceMode = mode,
            scheduledTokenOpenTimestamp = scheduledTimestamp,
            isTokenServiceEnabled = isServiceEnabled
        )
        return evaluateSchedule(tempSettings, nowMillis)
    }

    fun evaluateSchedule(settings: AshramSettings, nowMillis: Long = System.currentTimeMillis()): SundayScheduleState {
        // 1. SuperAdmin Manual Mode Override Check:
        if (settings.tokenServiceMode.equals("FORCE_OPEN", ignoreCase = true)) {
            // SuperAdmin forced tokens OPEN at any day/time
            return SundayScheduleState.Open
        }
        if (settings.tokenServiceMode.equals("FORCE_CLOSED", ignoreCase = true)) {
            // SuperAdmin forced tokens CLOSED at any day/time
            return SundayScheduleState.ServiceDisabled(
                messageHindi = "रविवार टोकन सेवा वर्तमान में सुपर एडमिन द्वारा बंद/स्थगित की गई है।",
                messageEnglish = "Sunday token service is currently paused by Super Admin."
            )
        }
        if (!settings.isTokenServiceEnabled) {
            return SundayScheduleState.ServiceDisabled(
                messageHindi = "रविवार टोकन सेवा वर्तमान में व्यवस्थापक द्वारा स्थगित की गई है।",
                messageEnglish = "Sunday token service is currently paused by Administration."
            )
        }

        // 2. Custom admin schedule override if set in future
        if (settings.scheduledTokenOpenTimestamp > nowMillis) {
            val remaining = settings.scheduledTokenOpenTimestamp - nowMillis
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val formatted = sdf.format(Date(settings.scheduledTokenOpenTimestamp))
            if (remaining <= COUNTDOWN_WINDOW_MILLIS) {
                val clock = formatCountdown(remaining)
                val clockHindi = formatCountdownHindi(remaining)
                return SundayScheduleState.CountdownActive(
                    openTimestamp = settings.scheduledTokenOpenTimestamp,
                    remainingMillis = remaining,
                    formattedTarget = formatted,
                    messageHindi = "टोकन पंजीकरण निर्धारित समय ($formatted) पर स्वतः खुलेगा। शेष समय: $clockHindi [ $clock ]।",
                    messageEnglish = "Token registration will open automatically at $formatted. Remaining: $clock."
                )
            }
            return SundayScheduleState.CustomScheduled(
                openTimestamp = settings.scheduledTokenOpenTimestamp,
                formattedDate = formatted,
                messageHindi = "टोकन पंजीकरण अभी बंद है। खुलने का समय: $formatted",
                messageEnglish = "Token registration is scheduled to open at: $formatted"
            )
        }

        // 3. AUTO_SUNDAY Schedule Evaluation
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val currentMinutes = hour * 60 + minute

        val startMinutes = SUNDAY_START_HOUR * 60 + SUNDAY_START_MINUTE // 8:30 AM (510)
        val endMinutes = SUNDAY_END_HOUR * 60 + SUNDAY_END_MINUTE       // 17:00 (1020)

        val nextSun = getNextSundayDate(cal)
        val nextSunHindi = formatNextSundayDateHindi(nextSun)
        val nextSunEng = formatNextSundayDateEnglish(nextSun)
        val nextSunStartMillis = nextSun.timeInMillis

        if (dayOfWeek == Calendar.SUNDAY) {
            if (currentMinutes in startMinutes until endMinutes) {
                // Sunday between 8:30 AM and 5:00 PM: AUTOMATICALLY OPEN!
                return SundayScheduleState.Open
            } else if (currentMinutes < startMinutes) {
                // Sunday morning before 8:30 AM (Within 12-hour countdown!)
                val remainingMillis = (nextSunStartMillis - nowMillis).coerceAtLeast(0L)
                val clock = formatCountdown(remainingMillis)
                val clockHindi = formatCountdownHindi(remainingMillis)
                return SundayScheduleState.CountdownActive(
                    openTimestamp = nextSunStartMillis,
                    remainingMillis = remainingMillis,
                    formattedTarget = "आज रविवार प्रातः 8:30 बजे",
                    messageHindi = "आज रविवार का टोकन पंजीकरण प्रातः 8:30 बजे से स्वतः प्रारंभ होगा। शेष समय: $clockHindi [ $clock ]।",
                    messageEnglish = "Today's Sunday token registration will start automatically at 8:30 AM. Remaining: $clock."
                )
            } else {
                // Sunday after 5:00 PM
                return SundayScheduleState.SundayClosedEvening(
                    nextSundayDateStr = nextSunHindi,
                    messageHindi = "आज के टोकन पूरे हो गए हैं। अब टोकन आगामी रविवार, $nextSunHindi को प्रातः 8:30 बजे से मिलना शुरू होंगे।",
                    messageEnglish = "Today's tokens are complete. Next tokens will be available on Sunday, $nextSunEng from 8:30 AM onwards."
                )
            }
        } else {
            // Monday to Saturday
            val timeUntilNextSundayStart = nextSunStartMillis - nowMillis
            if (timeUntilNextSundayStart in 1..COUNTDOWN_WINDOW_MILLIS) {
                // Within 12 hours of Sunday 8:30 AM (Saturday 8:30 PM onwards!)
                val clock = formatCountdown(timeUntilNextSundayStart)
                val clockHindi = formatCountdownHindi(timeUntilNextSundayStart)
                return SundayScheduleState.CountdownActive(
                    openTimestamp = nextSunStartMillis,
                    remainingMillis = timeUntilNextSundayStart,
                    formattedTarget = "कल रविवार प्रातः 8:30 बजे",
                    messageHindi = "रविवार टोकन पंजीकरण 12 घंटे पूर्व उल्टी गिनती जारी है। शेष समय: $clockHindi [ $clock ]। कल प्रातः 8:30 बजे टोकन स्वतः खुल जाएंगे।",
                    messageEnglish = "Sunday token countdown active. Remaining: $clock. Tokens will open automatically tomorrow at 8:30 AM."
                )
            } else {
                return SundayScheduleState.NonSunday(
                    nextSundayDateStr = nextSunHindi,
                    messageHindi = "टोकन प्रत्येक रविवार को प्रातः 8:30 बजे से शाम 5:00 बजे तक दिए जाते हैं। आप आगामी रविवार, $nextSunHindi को टोकन प्राप्त कर सकते हैं। (शनिवार रात 8:30 बजे से 12 घंटे पूर्व उल्टी गिनती शुरू होगी)",
                    messageEnglish = "Tokens are issued on Sundays from 8:30 AM to 5:00 PM at Ashram premises. Registration opens on Sunday, $nextSunEng from 8:30 AM."
                )
            }
        }
    }

    fun calculateQueueEta(
        myToken: Int,
        currentServing: Int,
        averageMinutesPerToken: Double = 2.5
    ): QueueEtaResult {
        if (myToken <= 0 || currentServing <= 0) {
            return QueueEtaResult(
                myToken = myToken,
                currentServing = currentServing,
                peopleAhead = 0,
                estimatedMinutesRemaining = 0,
                estimatedDarshanTimeStr = "--",
                statusTextHindi = if (currentServing > 0) "वर्तमान में टोकन #$currentServing का दर्शन चल रहा है" else "दरबार प्रारंभ होने की प्रतीक्षा है",
                statusTextEnglish = if (currentServing > 0) "Currently serving Token #$currentServing" else "Waiting for Darbar to start",
                isNowServing = false,
                hasPassed = false,
                isWaiting = false
            )
        }

        if (myToken < currentServing) {
            val passedCount = currentServing - myToken
            return QueueEtaResult(
                myToken = myToken,
                currentServing = currentServing,
                peopleAhead = 0,
                estimatedMinutesRemaining = 0,
                estimatedDarshanTimeStr = "निकल चुका / Passed",
                statusTextHindi = "आपका टोकन #$myToken निकल चुका है ($passedCount टोकन पहले)। कृपया तुरंत आश्रम सेवादार से संपर्क करें।",
                statusTextEnglish = "Your token #$myToken has passed. Please contact Ashram Sevadar.",
                isNowServing = false,
                hasPassed = true,
                isWaiting = false
            )
        }

        if (myToken == currentServing) {
            return QueueEtaResult(
                myToken = myToken,
                currentServing = currentServing,
                peopleAhead = 0,
                estimatedMinutesRemaining = 0,
                estimatedDarshanTimeStr = "अभी / Right Now",
                statusTextHindi = "🔔 आपका नंबर आ चुका है! कृपया तुरंत दरबार हॉल में पूज्य गुरुजी के समक्ष पधारें।",
                statusTextEnglish = "🔔 Your turn is now! Please enter Darbar Hall immediately.",
                isNowServing = true,
                hasPassed = false,
                isWaiting = false
            )
        }

        val peopleAhead = myToken - currentServing
        val minutesRemaining = Math.max(1, Math.round(peopleAhead * averageMinutesPerToken).toInt())
        val etaCal = Calendar.getInstance()
        etaCal.add(Calendar.MINUTE, minutesRemaining)
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.forLanguageTag("hi-IN"))
        val etaTimeStr = timeFmt.format(etaCal.time)

        return QueueEtaResult(
            myToken = myToken,
            currentServing = currentServing,
            peopleAhead = peopleAhead,
            estimatedMinutesRemaining = minutesRemaining,
            estimatedDarshanTimeStr = etaTimeStr,
            statusTextHindi = "आपसे आगे $peopleAhead भक्त हैं • संभावित दर्शन समय: $etaTimeStr (लगभग $minutesRemaining मिनट शेष)",
            statusTextEnglish = "$peopleAhead devotees ahead • Est. darshan: $etaTimeStr (~$minutesRemaining min remaining)",
            isNowServing = false,
            hasPassed = false,
            isWaiting = true
        )
    }
}

data class QueueEtaResult(
    val myToken: Int,
    val currentServing: Int,
    val peopleAhead: Int,
    val estimatedMinutesRemaining: Int,
    val estimatedDarshanTimeStr: String,
    val statusTextHindi: String,
    val statusTextEnglish: String,
    val isNowServing: Boolean,
    val hasPassed: Boolean,
    val isWaiting: Boolean
)
