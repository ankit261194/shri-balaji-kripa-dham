package com.example.shribalajikripadham.util

import com.example.shribalajikripadham.data.model.AshramSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class SundayScheduleState {
    object Open : SundayScheduleState()
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

    /**
     * Calculates the upcoming Sunday at 8:30 AM.
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
                // Today is Sunday before 8:30 AM -> this morning!
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
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("hi", "IN"))
        return sdf.format(nextSunday.time)
    }

    fun formatNextSundayDateEnglish(nextSunday: Calendar): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
        return sdf.format(nextSunday.time)
    }

    fun evaluateSchedule(settings: AshramSettings, nowMillis: Long = System.currentTimeMillis()): SundayScheduleState {
        if (!settings.isTokenServiceEnabled) {
            return SundayScheduleState.ServiceDisabled(
                messageHindi = "रविवार टोकन सेवा वर्तमान में व्यवस्थापक द्वारा स्थगित की गई है।",
                messageEnglish = "Sunday token service is currently paused by Administration."
            )
        }

        // Custom admin schedule override if set in future
        if (settings.scheduledTokenOpenTimestamp > nowMillis) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val formatted = sdf.format(Date(settings.scheduledTokenOpenTimestamp))
            return SundayScheduleState.CustomScheduled(
                openTimestamp = settings.scheduledTokenOpenTimestamp,
                formattedDate = formatted,
                messageHindi = "टोकन पंजीकरण अभी बंद है। खुलने का समय: $formatted",
                messageEnglish = "Token registration is scheduled to open at: $formatted"
            )
        }

        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val currentMinutes = hour * 60 + minute

        val startMinutes = SUNDAY_START_HOUR * 60 + SUNDAY_START_MINUTE // 8*60 + 30 = 510
        val endMinutes = SUNDAY_END_HOUR * 60 + SUNDAY_END_MINUTE       // 17*60 = 1020

        val nextSun = getNextSundayDate(cal)
        val nextSunHindi = formatNextSundayDateHindi(nextSun)
        val nextSunEng = formatNextSundayDateEnglish(nextSun)

        if (dayOfWeek != Calendar.SUNDAY) {
            return SundayScheduleState.NonSunday(
                nextSundayDateStr = nextSunHindi,
                messageHindi = "टोकन केवल रविवार को सुबह 8:30 बजे से शाम 5:00 बजे तक आश्रम परिसर में दिए जाते हैं। आप आगामी रविवार, $nextSunHindi को सुबह 8:30 बजे से आश्रम लोकेशन पर आकर टोकन प्राप्त कर सकते हैं।",
                messageEnglish = "Tokens are issued only on Sundays from 8:30 AM to 5:00 PM at Ashram premises. You can visit on Sunday, $nextSunEng from 8:30 AM to get your token."
            )
        }

        // It is Sunday!
        if (currentMinutes < startMinutes) {
            return SundayScheduleState.SundayBeforeStart(
                messageHindi = "आज (रविवार) का टोकन वितरण सुबह 8:30 बजे से शुरू होगा। कृपया सुबह 8:30 बजे आश्रम लोकेशन पर आकर टोकन प्राप्त करें।",
                messageEnglish = "Today's Sunday token distribution will start at 8:30 AM. Please arrive at Ashram premises at 8:30 AM to get token."
            )
        }

        if (currentMinutes >= endMinutes) {
            return SundayScheduleState.SundayClosedEvening(
                nextSundayDateStr = nextSunHindi,
                messageHindi = "आज के टोकन पूरे हो गए हैं। अब टोकन आगामी रविवार, $nextSunHindi को सुबह 8:30 बजे से मिलना शुरू होंगे।",
                messageEnglish = "Today's tokens are complete. Next tokens will be available on Sunday, $nextSunEng from 8:30 AM onwards."
            )
        }

        return SundayScheduleState.Open
    }
}
