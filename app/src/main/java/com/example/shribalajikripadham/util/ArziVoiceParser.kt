package com.example.shribalajikripadham.util

data class ParsedArziSpeech(
    val rawSpokenText: String,
    val devoteeName: String,
    val phoneNumber: String,
    val bigArziQty: Int,
    val smallArziQty: Int,
    val isPaid: Boolean = false,
    val paymentMode: String = "CASH",
    val confidence: Float = 1.0f
)

object ArziVoiceParser {

    private val hindiNumberWordMap = mapOf(
        "शून्य" to 0, "जीरो" to 0, "zero" to 0,
        "एक" to 1, "वन" to 1, "one" to 1, "पहला" to 1, "पहली" to 1,
        "दो" to 2, "टू" to 2, "two" to 2, "दूसरा" to 2, "दूसरी" to 2,
        "तीन" to 3, "थ्री" to 3, "three" to 3,
        "चार" to 4, "फोर" to 4, "four" to 4,
        "पांच" to 5, "पाँच" to 5, "फाइव" to 5, "five" to 5,
        "छह" to 6, "छः" to 6, "छे" to 6, "सिक्स" to 6, "six" to 6,
        "सात" to 7, "सेवन" to 7, "seven" to 7,
        "आठ" to 8, "एट" to 8, "eight" to 8,
        "नौ" to 9, "नाइन" to 9, "nine" to 9, "नो" to 9,
        "दस" to 10, "टेन" to 10, "ten" to 10,
        "ग्यारह" to 11, "बारह" to 12, "तेरह" to 13, "चौदह" to 14, "पंद्रह" to 15,
        "सोलह" to 16, "सत्रह" to 17, "अठारह" to 18, "उन्नीस" to 19, "बीस" to 20,
        "इक्कीस" to 21, "बाईस" to 22, "तेईस" to 23, "चौबीस" to 24, "पच्चीस" to 25,
        "छब्बीस" to 26, "सत्ताईस" to 27, "अट्ठाईस" to 28, "उनतीस" to 29, "तीस" to 30,
        "इकतीस" to 31, "बत्तीस" to 32, "तैंतीस" to 33, "चौंतीस" to 34, "पैंतीस" to 35,
        "छत्तीस" to 36, "सैंतीस" to 37, "अड़तीस" to 38, "उनतालीस" to 39, "चालीस" to 40,
        "इकतालीस" to 41, "बयालीस" to 42, "तैंतालीस" to 43, "चवालीस" to 44, "पैंतालीस" to 45,
        "छियालीस" to 46, "सैंतालीस" to 47, "अड़तालीस" to 48, "उनचास" to 49, "पचास" to 50,
        "साठ" to 60, "सत्तर" to 70, "अस्सी" to 80, "नब्बे" to 90, "सौ" to 100, "hundred" to 100
    )

    fun parseNumberToken(token: String): Int? {
        val clean = token.trim().lowercase()
        clean.toIntOrNull()?.let { return it }
        return hindiNumberWordMap[clean]
    }

    private fun extractPhoneNumber(text: String): Pair<String, String> {
        val phoneRegex = Regex("""(\+91[\-\s]?)?[6789]\d{9}""")
        val match = phoneRegex.find(text)
        if (match != null) {
            val phone = match.value.replace(Regex("[^0-9]"), "")
            val remaining = text.removeRange(match.range).trim()
            return Pair(phone, remaining)
        }
        return Pair("", text)
    }

    /**
     * Splits concatenated number-word strings like "5बड़ी" -> "5 बड़ी", "2छोती" -> "2 छोती"
     */
    private fun preprocessSpeechText(raw: String): String {
        var text = raw.trim()
        // Split digits and letters/devanagari
        text = text.replace(Regex("(\\d+)([\\p{IsDevanagari}a-zA-Z]+)"), "$1 $2")
        text = text.replace(Regex("([\\p{IsDevanagari}a-zA-Z]+)(\\d+)"), "$1 $2")
        // Remove commas and punctuation
        text = text.replace(Regex("[,.:;!|?]"), " ")
        // Normalize multiple spaces
        return text.replace(Regex("\\s+"), " ")
    }

    /**
     * Comprehensive Hindi & Hinglish Speech Parser for Arzi:
     * Examples:
     * - "रोहित पर पांच बड़ी अर्जी चार छोटी अर्जी"
     * - "अमित दो बड़ी तीन छोटी चुकता"
     * - "सुरेश 3 बडी 1 छोती"
     * - "2 बड़ी वाली 1 छोटी वाली"
     * - "दीपक कुमार 5 बड़ी अर्जी"
     */
    fun parseSpeech(rawText: String): ParsedArziSpeech {
        if (rawText.isBlank()) {
            return ParsedArziSpeech(rawText, "", "", 0, 0, false, "CASH", 0f)
        }

        var workingText = preprocessSpeechText(rawText)
        val (phone, textWithoutPhone) = extractPhoneNumber(workingText)
        workingText = textWithoutPhone

        // Detect payment status
        val isPaidSpoken = workingText.contains(Regex("(चुकता|नकद|कैश|paid|पूरा भुगतान|ऑनलाइन)", RegexOption.IGNORE_CASE))
        val paymentMode = if (workingText.contains(Regex("(ऑनलाइन|upi|qr|फोनपे|गूगल पे)", RegexOption.IGNORE_CASE))) "UPI" else "CASH"

        var bigQty = 0
        var smallQty = 0

        val bigKeywordsRegex = "(बड़ी|बड़ी|बडी|बड़ा|बड़ा|बडा|बड़े|बडे|badi|badee|bada|bade|big|large)"
        val smallKeywordsRegex = "(छोटी|छोती|छोटा|छोता|छोटे|छोते|chhoti|choti|small)"
        val fillerWordsRegex = "(?:डिब्बे|डिब्बा|पैकेट|अर्जी|अर्ज़ी|वाली|वाले|का|की|के|पर|और|तथा)?"

        // Pass 1: Match [Number] + [Big Keyword]
        val numBeforeBigRegex = Regex("""(\d+|[^\s\d]+)\s*$fillerWordsRegex\s*$bigKeywordsRegex""", RegexOption.IGNORE_CASE)
        val match1 = numBeforeBigRegex.find(workingText)
        if (match1 != null) {
            val numStr = match1.groupValues[1]
            val parsed = parseNumberToken(numStr)
            if (parsed != null && parsed > 0) {
                bigQty = parsed
            } else if (bigQty == 0) {
                bigQty = 1
            }
        }

        // Pass 2: Match [Big Keyword] + [Number]
        if (bigQty == 0) {
            val numAfterBigRegex = Regex("""$bigKeywordsRegex\s*$fillerWordsRegex\s*(\d+|[^\s\d]+)""", RegexOption.IGNORE_CASE)
            val match2 = numAfterBigRegex.find(workingText)
            if (match2 != null) {
                val numStr = match2.groupValues[match2.groupValues.size - 1]
                val parsed = parseNumberToken(numStr)
                if (parsed != null && parsed > 0) {
                    bigQty = parsed
                } else if (bigQty == 0) {
                    bigQty = 1
                }
            }
        }

        // Pass 3: If still 0, check if standalone "बड़ी" is spoken
        if (bigQty == 0 && workingText.contains(Regex(bigKeywordsRegex, RegexOption.IGNORE_CASE))) {
            bigQty = 1
        }

        // Pass 4: Match [Number] + [Small Keyword]
        val numBeforeSmallRegex = Regex("""(\d+|[^\s\d]+)\s*$fillerWordsRegex\s*$smallKeywordsRegex""", RegexOption.IGNORE_CASE)
        val match3 = numBeforeSmallRegex.find(workingText)
        if (match3 != null) {
            val numStr = match3.groupValues[1]
            val parsed = parseNumberToken(numStr)
            if (parsed != null && parsed > 0) {
                smallQty = parsed
            } else if (smallQty == 0) {
                smallQty = 1
            }
        }

        // Pass 5: Match [Small Keyword] + [Number]
        if (smallQty == 0) {
            val numAfterSmallRegex = Regex("""$smallKeywordsRegex\s*$fillerWordsRegex\s*(\d+|[^\s\d]+)""", RegexOption.IGNORE_CASE)
            val match4 = numAfterSmallRegex.find(workingText)
            if (match4 != null) {
                val numStr = match4.groupValues[match4.groupValues.size - 1]
                val parsed = parseNumberToken(numStr)
                if (parsed != null && parsed > 0) {
                    smallQty = parsed
                } else if (smallQty == 0) {
                    smallQty = 1
                }
            }
        }

        // Pass 6: If still 0, check if standalone "छोटी" is spoken
        if (smallQty == 0 && workingText.contains(Regex(smallKeywordsRegex, RegexOption.IGNORE_CASE))) {
            smallQty = 1
        }

        // Clean extracted devotee name by stripping numbers and arzi keywords
        var nameCleaning = workingText
        nameCleaning = nameCleaning.replace(Regex("""$bigKeywordsRegex""", RegexOption.IGNORE_CASE), " ")
        nameCleaning = nameCleaning.replace(Regex("""$smallKeywordsRegex""", RegexOption.IGNORE_CASE), " ")
        nameCleaning = nameCleaning.replace(Regex("""(अर्जी|अर्ज़ी|डिब्बा|डिब्बे|पैकेट|वाली|वाले|चुकता|नकद|कैश|paid|बकाया|उधार)""", RegexOption.IGNORE_CASE), " ")
        nameCleaning = nameCleaning.replace(Regex("""\b(पर|को|ने|का|की|के|और|तथा|एवं|with|and|to|for)\b""", RegexOption.IGNORE_CASE), " ")

        // Remove number tokens from name text
        val nameWords = nameCleaning.split(Regex("\\s+")).filter { word ->
            word.isNotBlank() && parseNumberToken(word) == null && !word.matches(Regex("^\\d+$"))
        }

        val devoteeName = nameWords.joinToString(" ")
            .replace(Regex("^(श्री|श्रीमती|भक्त|श्रीमान)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()

        return ParsedArziSpeech(
            rawSpokenText = rawText,
            devoteeName = if (devoteeName.isNotBlank()) devoteeName else "अज्ञात भक्त",
            phoneNumber = phone,
            bigArziQty = bigQty,
            smallArziQty = smallQty,
            isPaid = isPaidSpoken,
            paymentMode = paymentMode,
            confidence = if (bigQty > 0 || smallQty > 0) 0.98f else 0.6f
        )
    }
}
