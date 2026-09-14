package com.example.shribalajikripadham.util

data class ParsedArziSpeech(
    val rawSpokenText: String,
    val devoteeName: String,
    val phoneNumber: String,
    val bigArziQty: Int,
    val smallArziQty: Int,
    val confidence: Float = 1.0f
)

object ArziVoiceParser {

    private val hindiNumberWordMap = mapOf(
        "शून्य" to 0, "जीरो" to 0, "zero" to 0,
        "एक" to 1, "वन" to 1, "one" to 1,
        "दो" to 2, "टू" to 2, "two" to 2,
        "तीन" to 3, "थ्री" to 3, "three" to 3,
        "चार" to 4, "फोर" to 4, "four" to 4,
        "पांच" to 5, "पाँच" to 5, "फाइव" to 5, "five" to 5,
        "छह" to 6, "छः" to 6, "छे" to 6, "सिक्स" to 6, "six" to 6,
        "सात" to 7, "सेवन" to 7, "seven" to 7,
        "आठ" to 8, "एट" to 8, "eight" to 8,
        "नौ" to 9, "नाइन" to 9, "nine" to 9,
        "दस" to 10, "टेन" to 10, "ten" to 10,
        "ग्यारह" to 11, "बारह" to 12, "तेरह" to 13, "चौदह" to 14, "पंद्रह" to 15,
        "सोलह" to 16, "सत्रह" to 17, "अठारह" to 18, "उन्नीस" to 19, "बीस" to 20,
        "इक्कीस" to 21, "बाईस" to 22, "तेईस" to 23, "चौबीस" to 24, "पच्चीस" to 25,
        "छब्बीस" to 26, "सत्ताईस" to 27, "अट्ठाईस" to 28, "उनतीस" to 29, "तीस" to 30,
        "चालीस" to 40, "पचास" to 50, "साठ" to 60, "सत्तर" to 70, "अस्सी" to 80, "नब्बे" to 90, "सौ" to 100
    )

    /**
     * Converts words like "पांच", "5", "तीन" into integer number.
     */
    fun parseNumberToken(token: String): Int? {
        val clean = token.trim().lowercase()
        clean.toIntOrNull()?.let { return it }
        return hindiNumberWordMap[clean]
    }

    /**
     * Extracts phone numbers (10 digits sequence or spoken digits)
     */
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
     * Main Voice Parser:
     * Parses speech phrases like:
     * - "रोहित पर पांच बड़ी अर्जी चार छोटी अर्जी"
     * - "अमित दो बड़ी तीन छोटी"
     * - "सुरेश दस बड़ी अर्जी"
     * - "विकास 3 छोटी अर्जी"
     */
    fun parseSpeech(rawText: String): ParsedArziSpeech {
        if (rawText.isBlank()) {
            return ParsedArziSpeech(rawText, "", "", 0, 0, 0f)
        }

        var workingText = rawText.trim()
        val (phone, textWithoutPhone) = extractPhoneNumber(workingText)
        workingText = textWithoutPhone

        // Normalize variations
        // e.g. "बड़ी", "बड़ी", "badi", "big"
        // "छोटी", "chhoti", "small"
        val normalizedTokens = workingText.split(Regex("\\s+")).filter { it.isNotBlank() }

        var bigQty = 0
        var smallQty = 0

        // Search for patterns of:
        // [Number] (बड़ी|बड़ी|badi|big)
        // (बड़ी|बड़ी|badi|big) [Number]
        // [Number] (छोटी|chhoti|small)
        // (छोटी|chhoti|small) [Number]

        val nameWords = mutableListOf<String>()
        var i = 0
        var foundArziIndicator = false

        while (i < normalizedTokens.size) {
            val token = normalizedTokens[i]
            val nextToken = if (i + 1 < normalizedTokens.size) normalizedTokens[i + 1] else null
            val prevToken = if (i > 0) normalizedTokens[i - 1] else null

            val isBigKeyword = token.matches(Regex("^(बड़ी|बड़ी|बड़ा|बड़े|badi|big)$", RegexOption.IGNORE_CASE))
            val isSmallKeyword = token.matches(Regex("^(छोटी|छोटा|छोटे|chhoti|choti|small)$", RegexOption.IGNORE_CASE))
            val isArziWord = token.matches(Regex("^(अर्जी|अर्ज़ी|arzi|डिब्बा|डिब्बे|dibba)$", RegexOption.IGNORE_CASE))

            when {
                // Case: [Number] बड़ी
                isBigKeyword -> {
                    foundArziIndicator = true
                    val numFromPrev = prevToken?.let { parseNumberToken(it) }
                    val numFromNext = nextToken?.let { parseNumberToken(it) }
                    if (numFromPrev != null && bigQty == 0) {
                        bigQty = numFromPrev
                        // Remove prev token from nameWords if it was accidentally added
                        if (nameWords.isNotEmpty() && parseNumberToken(nameWords.last()) != null) {
                            nameWords.removeAt(nameWords.size - 1)
                        }
                    } else if (numFromNext != null && bigQty == 0) {
                        bigQty = numFromNext
                        i++ // skip next
                    } else if (bigQty == 0) {
                        bigQty = 1 // default if just "बड़ी अर्जी"
                    }
                }
                // Case: [Number] छोटी
                isSmallKeyword -> {
                    foundArziIndicator = true
                    val numFromPrev = prevToken?.let { parseNumberToken(it) }
                    val numFromNext = nextToken?.let { parseNumberToken(it) }
                    if (numFromPrev != null && smallQty == 0) {
                        smallQty = numFromPrev
                        // Remove prev token from nameWords if it was accidentally added
                        if (nameWords.isNotEmpty() && parseNumberToken(nameWords.last()) != null) {
                            nameWords.removeAt(nameWords.size - 1)
                        }
                    } else if (numFromNext != null && smallQty == 0) {
                        smallQty = numFromNext
                        i++ // skip next
                    } else if (smallQty == 0) {
                        smallQty = 1 // default if just "छोटी अर्जी"
                    }
                }
                isArziWord -> {
                    foundArziIndicator = true
                    // Just a filler word like "अर्जी" or "डिब्बा"
                }
                else -> {
                    // Check if this token is a number immediately before a big/small keyword
                    val nextIsKeyword = nextToken?.matches(Regex("^(बड़ी|बड़ी|बड़ा|बड़े|छोटी|छोटा|छोटे|badi|big|chhoti|choti|small)$", RegexOption.IGNORE_CASE)) == true
                    if (!foundArziIndicator && !nextIsKeyword) {
                        // Skip common Hindi filler particles like "पर", "को", "ने", "का", "की", "के", "और"
                        if (!token.matches(Regex("^(पर|को|ने|का|की|के|और|and|with|to)$", RegexOption.IGNORE_CASE))) {
                            nameWords.add(token)
                        }
                    }
                }
            }
            i++
        }

        // Clean extracted devotee name
        val devoteeName = nameWords.joinToString(" ")
            .replace(Regex("^(श्री|श्रीमती|भक्त|श्रीमान)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(जी|कुमार|सिंह)?$", RegexOption.IGNORE_CASE)) { it.value } // Keep polite titles if part of name
            .trim()

        return ParsedArziSpeech(
            rawSpokenText = rawText,
            devoteeName = if (devoteeName.isNotBlank()) devoteeName else "अज्ञात भक्त",
            phoneNumber = phone,
            bigArziQty = bigQty,
            smallArziQty = smallQty,
            confidence = if (bigQty > 0 || smallQty > 0) 0.95f else 0.5f
        )
    }
}
