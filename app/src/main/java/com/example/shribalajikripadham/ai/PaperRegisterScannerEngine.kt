package com.example.shribalajikripadham.ai

import android.graphics.Bitmap
import com.example.shribalajikripadham.data.model.RegisterEntry

/**
 * Intelligent OCR & Paper Register Sequence Parser Engine.
 *
 * Designed specifically for temple & ashram paper registers / notebook pages:
 * 1. Automatically converts Devanagari numerals (०१२३४५६७८९) to standard digits.
 * 2. Robust regex extraction for Serial Number, Devotee Name, 10-digit Phone, and City/Village.
 * 3. Strips punctuation, list bullets, and noise artifacts.
 * 4. Preserves STRICT serial sequence from top to bottom.
 */
object PaperRegisterScannerEngine {

    /**
     * Real On-Device Google ML Kit Devanagari (Hindi) Text Recognition.
     * Extracts multi-line text directly from scanned register/notebook photos.
     */
    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): String = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions.Builder().build()
            )
            val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    if (continuation.isActive) {
                        continuation.resume(text) { _, _, _ -> }
                    }
                    try { recognizer.close() } catch (e: Exception) {}
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    if (continuation.isActive) {
                        continuation.resume("") { _, _, _ -> }
                    }
                    try { recognizer.close() } catch (ex: Exception) {}
                }
        } catch (e: Exception) {
            e.printStackTrace()
            if (continuation.isActive) {
                continuation.resume("") { _, _, _ -> }
            }
        }
    }

    /**
     * Replaces Devanagari numerals with standard ASCII digits.
     */
    fun normalizeDevanagariDigits(input: String): String {
        val devanagariDigits = "०१२३४५६७८९"
        var result = input
        for (i in devanagariDigits.indices) {
            result = result.replace(devanagariDigits[i], ('0' + i))
        }
        return result
    }

    /**
     * Parses multi-line register text into structured RegisterEntry items.
     * Each entry preserves the exact sequential row order.
     */
    fun parseRegisterText(rawText: String): List<RegisterEntry> {
        val normalized = normalizeDevanagariDigits(rawText)
        val lines = normalized.lines()
        val entries = mutableListOf<RegisterEntry>()

        var serialCounter = 1

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.length < 2) continue

            // 1. Detect optional leading serial number (e.g., "1.", "1)", "1 -", "#1", "[1]")
            val serialRegex = Regex("""^(\d{1,4})[\.\)\-\:\s\—\–]+(.*)""")
            val serialMatch = serialRegex.find(line)

            var parsedSerial = serialCounter
            var content = line

            if (serialMatch != null) {
                val num = serialMatch.groupValues[1].toIntOrNull()
                if (num != null) {
                    parsedSerial = num
                }
                content = serialMatch.groupValues[2].trim()
            }

            // Strip leading bullets (-, *, •, ~)
            content = content.trimStart('-', '*', '•', '~', '>', ':', ',', ' ')

            if (content.isBlank()) continue

            // 2. Extract 10-digit Indian mobile number
            val phoneRegex = Regex("""\b([6-9]\d{9})\b""")
            val phoneMatch = phoneRegex.find(content)
            var phoneNumber = ""
            if (phoneMatch != null) {
                phoneNumber = phoneMatch.value
                content = content.replace(phoneNumber, "").trim()
            }

            // 3. Extract city if separated by delimiter (comma, dash, slash, tab, or parenthesis)
            var patientName = content
            var city = ""

            val delimiterRegex = Regex("""[\,\–\—\-\|\/\(\)]+""")
            val parts = content.split(delimiterRegex).map { it.trim() }.filter { it.isNotBlank() }

            if (parts.size >= 2) {
                patientName = parts[0]
                city = parts.subList(1, parts.size).joinToString(", ")
            } else {
                // If no delimiter, check if the last word is a likely city/district
                val words = content.split(Regex("""\s+""")).filter { it.isNotBlank() }
                if (words.size >= 2) {
                    val lastWord = words.last()
                    if (isLikelyCity(lastWord)) {
                        city = lastWord
                        patientName = words.dropLast(1).joinToString(" ")
                    }
                }
            }

            // Clean up name
            patientName = cleanDevoteeName(patientName)
            city = cleanCityName(city)

            if (patientName.isBlank() && phoneNumber.isNotBlank()) {
                patientName = "भक्त ($phoneNumber)"
            }

            if (patientName.isNotBlank()) {
                entries.add(
                    RegisterEntry(
                        serialNumber = parsedSerial,
                        patientName = patientName,
                        phoneNumber = phoneNumber,
                        city = if (city.isBlank()) "डूँगरा जाट (स्थानीय)" else city
                    )
                )
                serialCounter = parsedSerial + 1
            }
        }

        return entries
    }

    private fun cleanDevoteeName(raw: String): String {
        return raw.replace(Regex("""[^a-zA-Z\u0900-\u097F\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun cleanCityName(raw: String): String {
        return raw.replace(Regex("""[^a-zA-Z\u0900-\u097F\s\,\-]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun isLikelyCity(word: String): Boolean {
        val lower = word.lowercase()
        val commonCities = listOf(
            "delhi", "meerut", "bulandshahr", "noida", "ghaziabad", "aligarh", "hapur", "khurja",
            "syana", "anupshahr", "dungra", "dill", "jaat",
            "दिल्ली", "मेरठ", "बुलंदशहर", "नोएडा", "गाजियाबाद", "अलीगढ़", "हापुड़", "खुर्जा",
            "स्याना", "अनूपशहर", "डुंगरा", "जाट", "गाँव", "गांव", "ग्राम"
        )
        return commonCities.any { lower.contains(it) || it.contains(lower) }
    }
}
