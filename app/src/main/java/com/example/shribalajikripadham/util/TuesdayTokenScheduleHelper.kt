package com.example.shribalajikripadham.util

import com.example.shribalajikripadham.data.model.AshramSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class TuesdayScheduleState {
    object Open : TuesdayScheduleState()
    data class CountdownActive(
        val openTimestamp: Long,
        val remainingMillis: Long,
        val formattedTarget: String,
        val messageHindi: String,
        val messageEnglish: String
    ) : TuesdayScheduleState()
    data class TuesdayBeforeStart(val messageHindi: String, val messageEnglish: String) : TuesdayScheduleState()
    data class TuesdayClosedEvening(val nextTuesdayDateStr: String, val messageHindi: String, val messageEnglish: String) : TuesdayScheduleState()
    data class NonTuesday(val nextTuesdayDateStr: String, val messageHindi: String, val messageEnglish: String) : TuesdayScheduleState()
    data class ServiceDisabled(val messageHindi: String, val messageEnglish: String) : TuesdayScheduleState()
    data class CustomScheduled(val openTimestamp: Long, val formattedDate: String, val messageHindi: String, val messageEnglish: String) : TuesdayScheduleState()
}

object TuesdayTokenScheduleHelper {

    // Tuesday window: 8:00 AM to 5:00 PM (17:00)
    const val TUESDAY_START_HOUR = 8
    const val TUESDAY_START_MINUTE = 0
    const val TUESDAY_END_HOUR = 17
    const val TUESDAY_END_MINUTE = 0

    // 12-hour pre-registration countdown window in milliseconds (12 * 60 * 60 * 1000)
    const val COUNTDOWN_WINDOW_MILLIS = 12 * 60 * 60 * 1000L

    /**
     * Calculates the upcoming Tuesday at 8:00 AM.
     */
    fun getNextTuesdayDate(fromCal: Calendar = Calendar.getInstance()): Calendar {
        val cal = fromCal.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, TUESDAY_START_HOUR)
        cal.set(Calendar.MINUTE, TUESDAY_START_MINUTE)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dayOfWeek = fromCal.get(Calendar.DAY_OF_WEEK)
        val currentMinutes = fromCal.get(Calendar.HOUR_OF_DAY) * 60 + fromCal.get(Calendar.MINUTE)
        val startMinutes = TUESDAY_START_HOUR * 60 + TUESDAY_START_MINUTE

        if (dayOfWeek == Calendar.TUESDAY) {
            if (currentMinutes < startMinutes) {
                // Today is Tuesday before 8:00 AM -> this morning!
                return cal
            } else {
                // Today is Tuesday during/after darbar -> 7 days later
                cal.add(Calendar.DAY_OF_YEAR, 7)
                return cal
            }
        } else {
            // Advance until Tuesday
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.TUESDAY) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal
        }
    }

    fun formatNextTuesdayDateHindi(nextTuesday: Calendar): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("hi", "IN"))
        return sdf.format(nextTuesday.time)
    }

    fun formatNextTuesdayDateEnglish(nextTuesday: Calendar): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
        return sdf.format(nextTuesday.time)
    }

    fun formatCountdown(remainingMillis: Long): String {
        if (remainingMillis <= 0L) return "00:00:00"
        val totalSeconds = remainingMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

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

    fun evaluateSchedule(settings: AshramSettings, nowMillis: Long = System.currentTimeMillis()): TuesdayScheduleState {
        // 0. Master Switch check: Is Tuesday Darbar Enabled?
        if (!settings.isTuesdayDarbarEnabled) {
            return TuesdayScheduleState.ServiceDisabled(
                messageHindi = "मंगलवार बुलन्दशहर दरबार सेवा वर्तमान में बंद है।",
                messageEnglish = "Tuesday Bulandshahr Darbar is currently disabled by Super Admin."
            )
        }

        // 1. SuperAdmin Manual Mode Override Check:
        if (settings.tuesdayTokenServiceMode.equals("FORCE_OPEN", ignoreCase = true)) {
            return TuesdayScheduleState.Open
        }
        if (settings.tuesdayTokenServiceMode.equals("FORCE_CLOSED", ignoreCase = true)) {
            return TuesdayScheduleState.ServiceDisabled(
                messageHindi = "मंगलवार टोकन सेवा वर्तमान में सुपर एडमिन द्वारा स्थगित की गई है।",
                messageEnglish = "Tuesday token service is currently paused by Super Admin."
            )
        }

        // 2. Custom admin schedule override if set in future
        if (settings.tuesdayScheduledOpenTimestamp > nowMillis) {
            val remaining = settings.tuesdayScheduledOpenTimestamp - nowMillis
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val formatted = sdf.format(Date(settings.tuesdayScheduledOpenTimestamp))
            if (remaining <= COUNTDOWN_WINDOW_MILLIS) {
                val clock = formatCountdown(remaining)
                val clockHindi = formatCountdownHindi(remaining)
                return TuesdayScheduleState.CountdownActive(
                    openTimestamp = settings.tuesdayScheduledOpenTimestamp,
                    remainingMillis = remaining,
                    formattedTarget = formatted,
                    messageHindi = "मंगलवार टोकन पंजीकरण निर्धारित समय ($formatted) पर स्वतः खुलेगा। शेष समय: $clockHindi [ $clock ]।",
                    messageEnglish = "Tuesday token registration will open automatically at $formatted. Remaining: $clock."
                )
            }
            return TuesdayScheduleState.CustomScheduled(
                openTimestamp = settings.tuesdayScheduledOpenTimestamp,
                formattedDate = formatted,
                messageHindi = "मंगलवार टोकन पंजीकरण अभी बंद है। खुलने का समय: $formatted",
                messageEnglish = "Tuesday token registration is scheduled to open at: $formatted"
            )
        }

        // 3. AUTO_TUESDAY Schedule Evaluation
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val currentMinutes = hour * 60 + minute

        val startMinutes = TUESDAY_START_HOUR * 60 + TUESDAY_START_MINUTE // 8:00 AM (480)
        val endMinutes = TUESDAY_END_HOUR * 60 + TUESDAY_END_MINUTE       // 17:00 (1020)

        val nextTues = getNextTuesdayDate(cal)
        val nextTuesHindi = formatNextTuesdayDateHindi(nextTues)
        val nextTuesEng = formatNextTuesdayDateEnglish(nextTues)
        val nextTuesStartMillis = nextTues.timeInMillis

        if (dayOfWeek == Calendar.TUESDAY) {
            if (currentMinutes in startMinutes until endMinutes) {
                // Tuesday between 8:00 AM and 5:00 PM: AUTOMATICALLY OPEN!
                return TuesdayScheduleState.Open
            } else if (currentMinutes < startMinutes) {
                // Tuesday morning before 8:00 AM (Within countdown!)
                val remainingMillis = (nextTuesStartMillis - nowMillis).coerceAtLeast(0L)
                val clock = formatCountdown(remainingMillis)
                val clockHindi = formatCountdownHindi(remainingMillis)
                return TuesdayScheduleState.CountdownActive(
                    openTimestamp = nextTuesStartMillis,
                    remainingMillis = remainingMillis,
                    formattedTarget = "आज मंगलवार प्रातः 8:00 बजे",
                    messageHindi = "आज मंगलवार बुलन्दशहर दरबार टोकन पंजीकरण प्रातः 8:00 बजे से स्वतः प्रारंभ होगा। शेष समय: $clockHindi [ $clock ]।",
                    messageEnglish = "Today's Tuesday Bulandshahr token registration will start automatically at 8:00 AM. Remaining: $clock."
                )
            } else {
                // Tuesday after 5:00 PM
                return TuesdayScheduleState.TuesdayClosedEvening(
                    nextTuesdayDateStr = nextTuesHindi,
                    messageHindi = "आज के मंगलवार टोकन पूरे हो गए हैं। अब टोकन आगामी मंगलवार, $nextTuesHindi को प्रातः 8:00 बजे से मिलना शुरू होंगे।",
                    messageEnglish = "Today's Tuesday tokens are complete. Next tokens will be available on Tuesday, $nextTuesEng from 8:00 AM onwards."
                )
            }
        } else {
            // Other days: Monday 8:00 PM onwards is within 12 hours countdown!
            val timeUntilNextTuesdayStart = nextTuesStartMillis - nowMillis
            if (timeUntilNextTuesdayStart in 1..COUNTDOWN_WINDOW_MILLIS) {
                val clock = formatCountdown(timeUntilNextTuesdayStart)
                val clockHindi = formatCountdownHindi(timeUntilNextTuesdayStart)
                return TuesdayScheduleState.CountdownActive(
                    openTimestamp = nextTuesStartMillis,
                    remainingMillis = timeUntilNextTuesdayStart,
                    formattedTarget = "कल मंगलवार प्रातः 8:00 बजे",
                    messageHindi = "मंगलवार टोकन पंजीकरण 12 घंटे पूर्व उल्टी गिनती जारी है। शेष समय: $clockHindi [ $clock ]। कल प्रातः 8:00 बजे टोकन स्वतः खुल जाएंगे।",
                    messageEnglish = "Tuesday token countdown active. Remaining: $clock. Tokens will open automatically tomorrow at 8:00 AM."
                )
            } else {
                return TuesdayScheduleState.NonTuesday(
                    nextTuesdayDateStr = nextTuesHindi,
                    messageHindi = "टोकन प्रत्येक मंगलवार को प्रातः 8:00 बजे से शाम 5:00 बजे तक बुलन्दशहर दरबार हेतु दिए जाते हैं। आप आगामी मंगलवार, $nextTuesHindi को टोकन प्राप्त कर सकते हैं। (सोमवार रात 8:00 बजे से उल्टी गिनती शुरू होगी)",
                    messageEnglish = "Tokens are issued on Tuesdays from 8:00 AM to 5:00 PM for Bulandshahr Darbar. Registration opens on Tuesday, $nextTuesEng from 8:00 AM."
                )
            }
        }
    }
}
