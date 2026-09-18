package com.example.shribalajikripadham.panchang

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.*

enum class ChoghadiyaNature(val labelHindi: String, val isAuspicious: Boolean, val colorHex: Long) {
    AMRIT("अमृत (सर्वोत्तम)", true, 0xFF2E7D32),   // Green
    SHUBH("शुभ (उत्तम)", true, 0xFF388E3C),     // Green
    LABH("लाभ (उन्नतिकारक)", true, 0xFF43A047), // Green
    CHAR("चर (सामान्य)", true, 0xFF1976D2),     // Blue
    UDVEG("उद्वेग (अशुभ)", false, 0xFFE65100),  // Orange/Red
    ROG("रोग (हानिकारक)", false, 0xFFD32F2F),    // Red
    KAAL("काल (अशुभ)", false, 0xFFC2185B)       // Dark Red
}

data class ChoghadiyaSlot(
    val name: String,
    val nature: ChoghadiyaNature,
    val startTime: String,
    val endTime: String,
    val isCurrentlyActive: Boolean = false,
    val isDay: Boolean = true
)

data class VedicPanchangData(
    val dateString: String,
    val dayOfWeekHindi: String,
    val vikramSamvat: Int,
    val shakSamvat: Int,
    val maasHindi: String,
    val pakshaHindi: String,
    val tithiHindi: String,
    val nakshatraHindi: String,
    val yogaHindi: String,
    val karanaHindi: String,
    val sunriseTime: String,
    val sunsetTime: String,
    val rahuKaalTime: String,
    val yamagandaTime: String,
    val gulikaTime: String,
    val abhijitMuhuratTime: String,
    val brahmaMuhuratTime: String,
    val balajiSpecialPujaTime: String,
    val dayChoghadiya: List<ChoghadiyaSlot>,
    val nightChoghadiya: List<ChoghadiyaSlot>,
    val currentChoghadiya: ChoghadiyaSlot?
)

object VedicPanchangEngine {

    // Dungra Jat, Alwar, Rajasthan coordinates
    private const val ASHRAM_LAT = 27.82
    private const val ASHRAM_LON = 76.62
    private const val TIMEZONE_OFFSET_HOURS = 5.5 // Indian Standard Time (UTC+5:30)

    private val TITHI_NAMES = listOf(
        "प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पंचमी",
        "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी",
        "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "पूर्णिमा / अमावस्या"
    )

    private val NAKSHATRA_NAMES = listOf(
        "अश्विनी", "भरणी", "कृत्तिका", "रोहिणी", "मृगशिरा", "आर्द्रा",
        "पुनर्वसु", "पुष्य", "आश्लेषा", "मघा", "पूर्वाफाल्गुनी", "उत्तराफाल्गुनी",
        "हस्त", "चित्रा", "स्वाती", "विशाखा", "अनुराधा", "ज्येष्ठा",
        "मूल", "पूर्वाषाढ़ा", "उत्तराषाढ़ा", "श्रवण", "धनिष्ठा", "शतभिषा",
        "पूर्वाभाद्रपद", "उत्तराभाद्रपद", "रेवती"
    )

    private val YOGA_NAMES = listOf(
        "विष्कुम्भ", "प्रीति", "आयुष्मान", "सौभाग्य", "शोभन", "अतिगण्ड",
        "सुकर्मा", "धृति", "शूल", "गण्ड", "वृद्धि", "ध्रुव",
        "व्याघात", "हर्षण", "वज्र", "सिद्धि", "व्यतीपात", "वरीयान्",
        "परिघ", "शिव", "सिद्ध", "साध्य", "शुभ", "शुक्ल",
        "ब्रह्म", "इन्द्र", "वैधृति"
    )

    private val KARANA_NAMES = listOf(
        "बव", "बालव", "कौलव", "तैतिल", "गर", "वणिज", "विष्टि (भद्रा)",
        "शकुनि", "चतुष्पद", "नाग", "किंस्तुघ्न"
    )

    private val HINDI_MONTHS = listOf(
        "चैत्र", "वैशाख", "ज्येष्ठ", "आषाढ़", "श्रावण", "भाद्रपद",
        "आश्विन", "कार्तिक", "मार्गशीर्ष", "पौष", "माघ", "फाल्गुन"
    )

    private val DAY_OF_WEEK_NAMES = listOf(
        "रविवार", "सोमवार", "मंगलवार", "बुधवार", "गुरुवार", "शुक्रवार", "शनिवार"
    )

    fun calculatePanchang(calendar: Calendar = Calendar.getInstance()): VedicPanchangData {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // 1-12
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeekIdx = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday..6=Saturday
        val dayOfWeekHindi = DAY_OF_WEEK_NAMES[dayOfWeekIdx]

        // Julian Day Number
        val jd = toJulianDay(year, month, day, 12, 0)
        val t = (jd - 2451545.0) / 36525.0

        // Sun astronomical coordinates
        val sunMeanLong = (280.46646 + 36000.76983 * t) % 360.0
        val sunMeanAnomaly = (357.52911 + 35999.05029 * t) % 360.0
        val sunEqCtr = (1.914602 - 0.004817 * t) * sin(Math.toRadians(sunMeanAnomaly)) +
                (0.019993 - 0.000101 * t) * sin(Math.toRadians(2 * sunMeanAnomaly))
        val sunTrueLong = (sunMeanLong + sunEqCtr + 360.0) % 360.0

        // Moon astronomical coordinates
        val moonMeanLong = (218.3165 + 481267.8813 * t) % 360.0
        val moonMeanAnomaly = (134.9634 + 477198.8675 * t) % 360.0
        val moonEve = 1.2739 * sin(Math.toRadians(2 * (moonMeanLong - sunTrueLong) - moonMeanAnomaly))
        val moonVar = 0.6583 * sin(Math.toRadians(2 * (moonMeanLong - sunTrueLong)))
        val moonEqCtr = 6.2886 * sin(Math.toRadians(moonMeanAnomaly))
        val moonTrueLong = (moonMeanLong + moonEve + moonVar + moonEqCtr + 360.0) % 360.0

        // Lahiri Ayanamsha correction for Sidereal Zodiac
        val ayanamsha = 23.85 + (year - 2000) * 0.01397
        val moonSidereal = (moonTrueLong - ayanamsha + 360.0) % 360.0
        val sunSidereal = (sunTrueLong - ayanamsha + 360.0) % 360.0

        // 1. TITHI Calculation (Angular separation of Moon and Sun)
        val tithiAngle = (moonTrueLong - sunTrueLong + 360.0) % 360.0
        val rawTithiNum = (tithiAngle / 12.0).toInt() + 1 // 1 to 30
        val isShukla = rawTithiNum <= 15
        val pakshaHindi = if (isShukla) "शुक्ल पक्ष" else "कृष्ण पक्ष"
        val tithiIndex = if (isShukla) rawTithiNum - 1 else rawTithiNum - 16
        val tithiName = if (tithiIndex in 0..13) {
            TITHI_NAMES[tithiIndex]
        } else {
            if (isShukla) "पूर्णिमा" else "अमावस्या"
        }
        val tithiHindi = "$tithiName ($pakshaHindi)"

        // 2. NAKSHATRA Calculation (13°20' = 13.333333°)
        val nakshatraIdx = ((moonSidereal / (360.0 / 27.0)).toInt() % 27 + 27) % 27
        val nakshatraHindi = NAKSHATRA_NAMES[nakshatraIdx]

        // 3. YOGA Calculation ((Sun + Moon) / 13°20')
        val yogaAngle = (sunSidereal + moonSidereal) % 360.0
        val yogaIdx = ((yogaAngle / (360.0 / 27.0)).toInt() % 27 + 27) % 27
        val yogaHindi = YOGA_NAMES[yogaIdx]

        // 4. KARANA Calculation (Half of Tithi = 6°)
        val karanaIdx = ((tithiAngle / 6.0).toInt() % 11 + 11) % 11
        val karanaHindi = KARANA_NAMES[karanaIdx]

        // 5. VIKRAM SAMVAT & SHAK SAMVAT
        val vikramSamvat = year + 57
        val shakSamvat = year - 78

        // Lunar Hindu Month approximation
        val monthIdx = ((sunSidereal / 30.0).toInt() + 11) % 12
        val maasHindi = HINDI_MONTHS[monthIdx]

        // 6. SUNRISE & SUNSET for Dungra Jat (Alwar)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val solarDeclination = 23.44 * sin(Math.toRadians((360.0 / 365.0) * (dayOfYear - 81)))
        val latRad = Math.toRadians(ASHRAM_LAT)
        val decRad = Math.toRadians(solarDeclination)
        val cosHourAngle = -tan(latRad) * tan(decRad)
        val clampedCos = cosHourAngle.coerceIn(-1.0, 1.0)
        val hourAngleDeg = Math.toDegrees(acos(clampedCos))
        val hourAngleHours = hourAngleDeg / 15.0

        // Equation of Time approximation
        val b = Math.toRadians((360.0 / 365.0) * (dayOfYear - 81))
        val eqTimeMinutes = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)

        // Solar Noon in IST
        val solarNoonIST = 12.0 - ((ASHRAM_LON - (TIMEZONE_OFFSET_HOURS * 15.0)) / 15.0) - (eqTimeMinutes / 60.0)
        val sunriseDecimal = solarNoonIST - hourAngleHours
        val sunsetDecimal = solarNoonIST + hourAngleHours

        val sunriseTime = formatDecimalTime(sunriseDecimal)
        val sunsetTime = formatDecimalTime(sunsetDecimal)

        // 7. RAHU KAAL, YAMAGANDA & GULIKA
        val dayLength = sunsetDecimal - sunriseDecimal
        val partLength = dayLength / 8.0

        // Rahu Kaal slot by day of week (Sunday=0 .. Saturday=6)
        // Sun:8, Mon:2, Tue:7, Wed:5, Thu:6, Fri:4, Sat:3
        val rahuSlots = listOf(7, 1, 6, 4, 5, 3, 2)
        val rahuStart = sunriseDecimal + (rahuSlots[dayOfWeekIdx] * partLength)
        val rahuEnd = rahuStart + partLength
        val rahuKaalTime = "${formatDecimalTime(rahuStart)} से ${formatDecimalTime(rahuEnd)}"

        // Yamaganda slots
        val yamaSlots = listOf(4, 3, 2, 1, 0, 6, 5)
        val yamaStart = sunriseDecimal + (yamaSlots[dayOfWeekIdx] * partLength)
        val yamaEnd = yamaStart + partLength
        val yamagandaTime = "${formatDecimalTime(yamaStart)} से ${formatDecimalTime(yamaEnd)}"

        // Gulika slots
        val gulikaSlots = listOf(6, 5, 4, 3, 2, 1, 0)
        val gulikaStart = sunriseDecimal + (gulikaSlots[dayOfWeekIdx] * partLength)
        val gulikaEnd = gulikaStart + partLength
        val gulikaTime = "${formatDecimalTime(gulikaStart)} से ${formatDecimalTime(gulikaEnd)}"

        // 8. ABHIJIT MUHURAT (Midday +- 24 mins)
        val abhijitStart = solarNoonIST - (24.0 / 60.0)
        val abhijitEnd = solarNoonIST + (24.0 / 60.0)
        val abhijitMuhuratTime = "${formatDecimalTime(abhijitStart)} से ${formatDecimalTime(abhijitEnd)}"

        // Brahma Muhurat (approx 1h 36m before sunrise)
        val brahmaStart = sunriseDecimal - (96.0 / 60.0)
        val brahmaEnd = sunriseDecimal - (48.0 / 60.0)
        val brahmaMuhuratTime = "${formatDecimalTime(brahmaStart)} से ${formatDecimalTime(brahmaEnd)}"

        // Balaji Special Puja Muhurat (Tuesday / Saturday special)
        val balajiSpecial = when (dayOfWeekIdx) {
            2 -> "मंगलवार विशेष बालाजी सिंदूर व चोला पूजन: प्रातः 08:30 से 10:30 एवं संध्या 06:15 से 08:00"
            6 -> "शनिवार पावन दर्शन व संकट निवारण महाआरती: प्रातः 09:00 से 11:30 एवं सायं 06:30 से 08:30"
            else -> "नित्य महाआरती व दर्शन: प्रातः 06:00 (मंगल), दोपहर 12:00 (भोग), सायं 07:00 (संध्या)"
        }

        // 9. DAY & NIGHT CHOGHADIYA
        val (dayChoghadiyas, nightChoghadiyas) = generateChoghadiyaSlots(
            dayOfWeekIdx,
            sunriseDecimal,
            sunsetDecimal,
            calendar
        )

        val currentSlot = (dayChoghadiyas + nightChoghadiyas).find { it.isCurrentlyActive }

        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("hi", "IN"))
        val dateString = sdf.format(calendar.time)

        return VedicPanchangData(
            dateString = dateString,
            dayOfWeekHindi = dayOfWeekHindi,
            vikramSamvat = vikramSamvat,
            shakSamvat = shakSamvat,
            maasHindi = maasHindi,
            pakshaHindi = pakshaHindi,
            tithiHindi = tithiHindi,
            nakshatraHindi = nakshatraHindi,
            yogaHindi = yogaHindi,
            karanaHindi = karanaHindi,
            sunriseTime = sunriseTime,
            sunsetTime = sunsetTime,
            rahuKaalTime = rahuKaalTime,
            yamagandaTime = yamagandaTime,
            gulikaTime = gulikaTime,
            abhijitMuhuratTime = abhijitMuhuratTime,
            brahmaMuhuratTime = brahmaMuhuratTime,
            balajiSpecialPujaTime = balajiSpecial,
            dayChoghadiya = dayChoghadiyas,
            nightChoghadiya = nightChoghadiyas,
            currentChoghadiya = currentSlot
        )
    }

    private fun generateChoghadiyaSlots(
        dayOfWeek: Int,
        sunriseDec: Double,
        sunsetDec: Double,
        nowCal: Calendar
    ): Pair<List<ChoghadiyaSlot>, List<ChoghadiyaSlot>> {
        // Day Sequences by Day of Week
        val daySequences = listOf(
            listOf(ChoghadiyaNature.UDVEG, ChoghadiyaNature.CHAR, ChoghadiyaNature.LABH, ChoghadiyaNature.AMRIT, ChoghadiyaNature.KAAL, ChoghadiyaNature.SHUBH, ChoghadiyaNature.ROG, ChoghadiyaNature.UDVEG), // Sun
            listOf(ChoghadiyaNature.AMRIT, ChoghadiyaNature.KAAL, ChoghadiyaNature.SHUBH, ChoghadiyaNature.ROG, ChoghadiyaNature.UDVEG, ChoghadiyaNature.CHAR, ChoghadiyaNature.LABH, ChoghadiyaNature.AMRIT), // Mon
            listOf(ChoghadiyaNature.ROG, ChoghadiyaNature.UDVEG, ChoghadiyaNature.CHAR, ChoghadiyaNature.LABH, ChoghadiyaNature.AMRIT, ChoghadiyaNature.KAAL, ChoghadiyaNature.SHUBH, ChoghadiyaNature.ROG), // Tue
            listOf(ChoghadiyaNature.UDVEG, ChoghadiyaNature.CHAR, ChoghadiyaNature.LABH, ChoghadiyaNature.AMRIT, ChoghadiyaNature.KAAL, ChoghadiyaNature.SHUBH, ChoghadiyaNature.ROG, ChoghadiyaNature.UDVEG), // Wed
            listOf(ChoghadiyaNature.SHUBH, ChoghadiyaNature.ROG, ChoghadiyaNature.UDVEG, ChoghadiyaNature.CHAR, ChoghadiyaNature.LABH, ChoghadiyaNature.AMRIT, ChoghadiyaNature.KAAL, ChoghadiyaNature.SHUBH), // Thu
            listOf(ChoghadiyaNature.CHAR, ChoghadiyaNature.LABH, ChoghadiyaNature.AMRIT, ChoghadiyaNature.KAAL, ChoghadiyaNature.SHUBH, ChoghadiyaNature.ROG, ChoghadiyaNature.UDVEG, ChoghadiyaNature.CHAR), // Fri
            listOf(ChoghadiyaNature.KAAL, ChoghadiyaNature.SHUBH, ChoghadiyaNature.ROG, ChoghadiyaNature.UDVEG, ChoghadiyaNature.CHAR, ChoghadiyaNature.LABH, ChoghadiyaNature.AMRIT, ChoghadiyaNature.KAAL)  // Sat
        )

        // Night Sequences
        val nightSequences = listOf(
            listOf(ChoghadiyaNature.SHUBH, ChoghadiyaNature.AMRIT, ChoghadiyaNature.CHAR, ChoghadiyaNature.ROG, ChoghadiyaNature.KAAL, ChoghadiyaNature.LABH, ChoghadiyaNature.UDVEG, ChoghadiyaNature.SHUBH), // Sun
            listOf(ChoghadiyaNature.CHAR, ChoghadiyaNature.ROG, ChoghadiyaNature.KAAL, ChoghadiyaNature.LABH, ChoghadiyaNature.UDVEG, ChoghadiyaNature.SHUBH, ChoghadiyaNature.AMRIT, ChoghadiyaNature.CHAR), // Mon
            listOf(ChoghadiyaNature.KAAL, ChoghadiyaNature.LABH, ChoghadiyaNature.UDVEG, ChoghadiyaNature.SHUBH, ChoghadiyaNature.AMRIT, ChoghadiyaNature.CHAR, ChoghadiyaNature.ROG, ChoghadiyaNature.KAAL), // Tue
            listOf(ChoghadiyaNature.LABH, ChoghadiyaNature.UDVEG, ChoghadiyaNature.SHUBH, ChoghadiyaNature.AMRIT, ChoghadiyaNature.CHAR, ChoghadiyaNature.ROG, ChoghadiyaNature.KAAL, ChoghadiyaNature.LABH), // Wed
            listOf(ChoghadiyaNature.AMRIT, ChoghadiyaNature.CHAR, ChoghadiyaNature.ROG, ChoghadiyaNature.KAAL, ChoghadiyaNature.LABH, ChoghadiyaNature.UDVEG, ChoghadiyaNature.SHUBH, ChoghadiyaNature.AMRIT), // Thu
            listOf(ChoghadiyaNature.UDVEG, ChoghadiyaNature.SHUBH, ChoghadiyaNature.AMRIT, ChoghadiyaNature.CHAR, ChoghadiyaNature.ROG, ChoghadiyaNature.KAAL, ChoghadiyaNature.LABH, ChoghadiyaNature.UDVEG), // Fri
            listOf(ChoghadiyaNature.ROG, ChoghadiyaNature.KAAL, ChoghadiyaNature.LABH, ChoghadiyaNature.UDVEG, ChoghadiyaNature.SHUBH, ChoghadiyaNature.AMRIT, ChoghadiyaNature.CHAR, ChoghadiyaNature.ROG)  // Sat
        )

        val currentDecTime = nowCal.get(Calendar.HOUR_OF_DAY) + (nowCal.get(Calendar.MINUTE) / 60.0)

        // Day slots
        val dayPart = (sunsetDec - sunriseDec) / 8.0
        val daySlots = mutableListOf<ChoghadiyaSlot>()
        for (i in 0 until 8) {
            val start = sunriseDec + (i * dayPart)
            val end = start + dayPart
            val nature = daySequences[dayOfWeek][i]
            val active = currentDecTime >= start && currentDecTime < end
            daySlots.add(
                ChoghadiyaSlot(
                    name = nature.name,
                    nature = nature,
                    startTime = formatDecimalTime(start),
                    endTime = formatDecimalTime(end),
                    isCurrentlyActive = active,
                    isDay = true
                )
            )
        }

        // Night slots (Sunset to Sunrise next day)
        val nightDuration = (24.0 - sunsetDec) + sunriseDec
        val nightPart = nightDuration / 8.0
        val nightSlots = mutableListOf<ChoghadiyaSlot>()
        for (i in 0 until 8) {
            var start = sunsetDec + (i * nightPart)
            var end = start + nightPart
            val nature = nightSequences[dayOfWeek][i]

            val active = if (start < 24.0 && end <= 24.0) {
                currentDecTime >= start && currentDecTime < end
            } else if (start < 24.0 && end > 24.0) {
                currentDecTime >= start || currentDecTime < (end - 24.0)
            } else {
                val sNorm = start - 24.0
                val eNorm = end - 24.0
                currentDecTime >= sNorm && currentDecTime < eNorm
            }

            val startStr = formatDecimalTime(if (start >= 24.0) start - 24.0 else start)
            val endStr = formatDecimalTime(if (end >= 24.0) end - 24.0 else end)

            nightSlots.add(
                ChoghadiyaSlot(
                    name = nature.name,
                    nature = nature,
                    startTime = startStr,
                    endTime = endStr,
                    isCurrentlyActive = active,
                    isDay = false
                )
            )
        }

        return Pair(daySlots, nightSlots)
    }

    private fun formatDecimalTime(decHours: Double): String {
        var h = decHours.toInt()
        val m = ((decHours - h) * 60.0).roundToInt()
        if (m == 60) {
            h += 1
        }
        val adjustedH = h % 24
        val amPm = if (adjustedH < 12) "AM" else "PM"
        val displayH = when {
            adjustedH == 0 -> 12
            adjustedH > 12 -> adjustedH - 12
            else -> adjustedH
        }
        val finalM = if (m == 60) 0 else m
        return String.format(Locale.US, "%02d:%02d %s", displayH, finalM, amPm)
    }

    private fun toJulianDay(year: Int, month: Int, day: Int, hour: Int, minute: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val dayFraction = (hour + minute / 60.0) / 24.0
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + dayFraction + b - 1524.5
    }
}
