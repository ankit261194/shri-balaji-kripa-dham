package com.example.shribalajikripadham.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.shribalajikripadham.data.model.RegisterEntry
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Intelligent OCR & Paper Register Sequence Parser Engine.
 *
 * Specifically engineered for temple & ashram paper registers / hand-written notebook pages:
 * 1. Preprocesses image using adaptive grayscale & contrast expansion to distinguish ballpoint/gel pen ink from lined paper.
 * 2. Real on-device Devanagari text recognition with 2-pass fallback.
 * 3. Lookalike repair for handwritten phone numbers (e.g. O->0, I/l->1, S->5, B->8).
 * 4. Converts Devanagari numerals (०१२३४५६७८९) to standard digits.
 * 5. Preserves strict sequential row order.
 */
object PaperRegisterScannerEngine {

    /**
     * Preprocesses bitmap for handwriting: enhances contrast, strips color cast,
     * and binarizes faint ink strokes with adaptive contrast curve.
     */
    fun preprocessBitmapForHandwriting(src: Bitmap): Bitmap {
        return try {
            val width = src.width
            val height = src.height
            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Step 1: Grayscale
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)

            // Step 2: High contrast adaptive matrix (scale = 1.8x, shifted to enhance ink strokes)
            val contrast = 1.8f
            val translate = (-0.5f * contrast + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(contrastMatrix)

            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(src, 0f, 0f, paint)
            output
        } catch (e: Exception) {
            src
        }
    }

    /**
     * Real On-Device Google ML Kit Devanagari (Hindi) Text Recognition.
     * Uses 2-pass recognition (Handwriting-enhanced pass + Raw fallback) for maximum accuracy.
     */
    suspend fun recognizeTextFromBitmap(bitmap: Bitmap, enhanceForHandwriting: Boolean = true): String {
        val targetBitmap = if (enhanceForHandwriting) preprocessBitmapForHandwriting(bitmap) else bitmap
        var result = runMlKitRecognition(targetBitmap)

        // If handwriting pass returned sparse results, try raw pass as fallback
        if (enhanceForHandwriting && (result.isBlank() || result.lines().count { it.trim().isNotBlank() } < 2)) {
            val rawResult = runMlKitRecognition(bitmap)
            if (rawResult.length > result.length) {
                result = rawResult
            }
        }
        return result
    }

    private suspend fun runMlKitRecognition(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions.Builder().build()
            )
            val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    if (continuation.isActive) {
                        continuation.resume(text)
                    }
                    try { recognizer.close() } catch (e: Exception) {}
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    if (continuation.isActive) {
                        continuation.resume("")
                    }
                    try { recognizer.close() } catch (ex: Exception) {}
                }
        } catch (e: Exception) {
            e.printStackTrace()
            if (continuation.isActive) {
                continuation.resume("")
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
     * Repairs common handwritten OCR character confusions in Indian mobile numbers:
     * e.g. O/o/Q/D -> 0, I/l/|/]/) -> 1, Z/z -> 2, E -> 3, A/h -> 4, S/s/$ -> 5, G/b/C -> 6, T/t -> 7, B/& -> 8, g/q/P -> 9
     * Also strips +91 or leading 0 prefix.
     */
    fun repairHandwrittenPhone(token: String): String {
        val cleaned = token
            .replace('O', '0').replace('o', '0').replace('Q', '0').replace('D', '0')
            .replace('I', '1').replace('l', '1').replace('|', '1').replace('/', '1').replace('!', '1')
            .replace(']', '1').replace(')', '1').replace('L', '1')
            .replace('Z', '2').replace('z', '2')
            .replace('E', '3')
            .replace('A', '4').replace('h', '4')
            .replace('S', '5').replace('s', '5').replace('$', '5')
            .replace('G', '6').replace('b', '6').replace('C', '6')
            .replace('T', '7').replace('t', '7')
            .replace('B', '8').replace('&', '8')
            .replace('g', '9').replace('q', '9').replace('P', '9')
        var digitsOnly = cleaned.filter { it.isDigit() }
        
        // Strip country code +91 / 91 or leading 0
        if (digitsOnly.length == 12 && digitsOnly.startsWith("91")) {
            digitsOnly = digitsOnly.substring(2)
        } else if (digitsOnly.length == 11 && digitsOnly.startsWith("0")) {
            digitsOnly = digitsOnly.substring(1)
        }

        if (digitsOnly.length == 10 && digitsOnly.first() in '6'..'9') {
            return digitsOnly
        }
        return ""
    }

    /**
     * Calculates an OCR extraction confidence score (0 - 100%) for quality grading.
     */
    fun calculateConfidence(name: String, phone: String, city: String, hasSerial: Boolean): Int {
        var score = 0
        if (phone.length == 10 && phone.first() in '6'..'9') {
            score += 40
        } else if (phone.isNotBlank()) {
            score += 15
        }
        if (name.length >= 3 && !name.startsWith("भक्त (")) {
            score += 35
        } else if (name.isNotBlank()) {
            score += 15
        }
        if (city.isNotBlank() && city != "डूँगरा जाट (स्थानीय)") {
            score += 15
        } else {
            score += 10
        }
        if (hasSerial) {
            score += 10
        }
        return score.coerceIn(25, 100)
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

            // 1. Detect optional leading serial number (e.g., "1.", "1)", "1 -", "#1", "[1]", "क्र. 1")
            val serialRegex = Regex("""^(?:क्र[\.\s]*सं[\.\s]*|क[\.\s]*सं[\.\s]*|#)?(\d{1,4})[\.\)\-\:\s\—\–]+(.*)""")
            val serialMatch = serialRegex.find(line)

            var parsedSerial = serialCounter
            var content = line
            var hasSerial = false

            if (serialMatch != null) {
                val num = serialMatch.groupValues[1].toIntOrNull()
                if (num != null) {
                    parsedSerial = num
                    hasSerial = true
                }
                content = serialMatch.groupValues[2].trim()
            }

            // Strip leading bullets (-, *, •, ~, >, :, ,, space)
            content = content.trimStart('-', '*', '•', '~', '>', ':', ',', ' ')

            if (content.isBlank()) continue

            // 2. Extract 10-digit Indian mobile number (standard regex)
            val phoneRegex = Regex("""(?:\+91[\-\s]?)?([6-9]\d{9})\b""")
            val phoneMatch = phoneRegex.find(content)
            var phoneNumber = ""

            if (phoneMatch != null) {
                phoneNumber = phoneMatch.groupValues[1]
                content = content.replace(phoneMatch.value, "").trim()
            } else {
                // Try handwritten lookalike repair on words/tokens with numbers/separators
                val words = content.split(Regex("""\s+"""))
                for (w in words) {
                    val repaired = repairHandwrittenPhone(w)
                    if (repaired.isNotBlank()) {
                        phoneNumber = repaired
                        content = content.replace(w, "").trim()
                        break
                    }
                }
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

            // Clean up devotee name & village
            patientName = cleanDevoteeName(patientName)
            city = cleanCityName(city)

            if (patientName.isBlank() && phoneNumber.isNotBlank()) {
                patientName = "भक्त ($phoneNumber)"
            }

            if (patientName.isNotBlank()) {
                val finalCity = if (city.isBlank()) "डूँगरा जाट (स्थानीय)" else city
                val confidence = calculateConfidence(patientName, phoneNumber, finalCity, hasSerial)
                entries.add(
                    RegisterEntry(
                        serialNumber = parsedSerial,
                        patientName = patientName,
                        phoneNumber = phoneNumber,
                        city = finalCity,
                        confidence = confidence
                    )
                )
                serialCounter = parsedSerial + 1
            }
        }

        return entries
    }

    private fun cleanDevoteeName(raw: String): String {
        return raw.replace(Regex("""[^a-zA-Z\u0900-\u097F\s\.]"""), " ")
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
            "syana", "anupshahr", "sikandrabad", "jahangirabad", "dungra", "dill", "jaat",
            "mathura", "vrindavan", "agra", "faridabad", "gurgaon", "gurugram", "moradabad", "bareilly",
            "दिल्ली", "मेरठ", "बुलंदशहर", "नोएडा", "गाजियाबाद", "अलीगढ़", "हापुड़", "खुर्जा",
            "स्याना", "अनूपशहर", "सिकंदराबाद", "जहांगीराबाद", "डुंगरा", "डूँगरा", "जाट",
            "मथुरा", "वृंदावन", "आगरा", "फरीदाबाद", "गुड़गांव", "गुरुग्राम", "मुरादाबाद", "बरेली",
            "गाँव", "गांव", "ग्राम", "तहसील", "जिला"
        )
        return commonCities.any { lower.contains(it) || it.contains(lower) }
    }
}
