package com.example.shribalajikripadham.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Base64
import com.example.shribalajikripadham.data.model.RegisterEntry
import com.example.shribalajikripadham.data.network.HostingerCentralSyncManager
import com.example.shribalajikripadham.util.AshramVoiceAnnouncementManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
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
     * Multimodal Gemini Vision AI Handwriting Recognition:
     * Reads complex cursive handwritten Hindi paper registers, journals, and lined notebooks.
     * Extracts structured devotee records: Serial, Patient Name, Phone, and City.
     * Returns Pair(List<RegisterEntry>, rawTranscriptionOrError)
     */
    suspend fun scanRegisterWithGeminiVision(
        context: Context,
        bitmap: Bitmap,
        customApiKey: String? = null
    ): Pair<List<RegisterEntry>, String> = withContext(Dispatchers.IO) {
        try {
            // 1. Prepare and downscale bitmap to optimal size (max 1280px on longest dimension)
            val maxDimension = 1280
            val scale = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            } else 1.0f

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else bitmap

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val imageBytes = baos.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            // 2. Resolve API Key: custom key -> saved Google TTS/AI key in admin settings
            val resolvedKey = if (!customApiKey.isNullOrBlank()) {
                customApiKey.trim()
            } else {
                val savedTtsKey = AshramVoiceAnnouncementManager.getGoogleTtsApiKey(context).trim()
                if (savedTtsKey.isNotBlank()) savedTtsKey else ""
            }

            // If we have an API Key, call Generative Language API directly
            if (resolvedKey.isNotBlank()) {
                val directResult = callGeminiVisionApiDirect(base64Image, resolvedKey)
                if (directResult.first.isNotEmpty()) {
                    return@withContext directResult
                }
            }

            // 3. Fallback to Hostinger Central AI Gateway endpoint
            val gatewayResult = callHostingerGeminiGateway(base64Image)
            if (gatewayResult.first.isNotEmpty()) {
                return@withContext gatewayResult
            }

            // 4. If online AI fails (e.g. offline device), fallback gracefully to on-device ML Kit OCR
            val localOcr = recognizeTextFromBitmap(bitmap, enhanceForHandwriting = true)
            if (localOcr.isNotBlank()) {
                val entries = parseRegisterText(localOcr)
                return@withContext Pair(entries, localOcr)
            }

            Pair(emptyList(), "फोटो में कोई अक्षर नहीं मिला। कृपया पुनः प्रयास करें।")
        } catch (e: Exception) {
            val localOcr = recognizeTextFromBitmap(bitmap, enhanceForHandwriting = true)
            if (localOcr.isNotBlank()) {
                Pair(parseRegisterText(localOcr), localOcr)
            } else {
                Pair(emptyList(), "त्रुटि: ${e.message}")
            }
        }
    }

    private fun callGeminiVisionApiDirect(base64Image: String, apiKey: String): Pair<List<RegisterEntry>, String> {
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 18000
                readTimeout = 18000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            val promptText = "You are an expert handwriting recognition processor for Indian temple & ashram devotee paper registers. " +
                    "The image contains handwritten Hindi rows of devotees. Each row contains serial number (क्र. सं.), " +
                    "devotee/patient name (भक्त या मरीज का नाम), mobile phone number (10 अंकों का मोबाइल), and city/village (गाँव/शहर/पता). " +
                    "Extract all devotee rows as a JSON array of objects with keys: 'serial' (number), 'name' (Hindi string), " +
                    "'phone' (10 digit string or blank), and 'city' (Hindi string or 'डूँगरा जाट'). " +
                    "Return strictly a raw JSON array without markdown code blocks."

            val partsArray = JSONArray().apply {
                put(JSONObject().put("text", promptText))
                put(JSONObject().put("inline_data", JSONObject().apply {
                    put("mime_type", "image/jpeg")
                    put("data", base64Image)
                }))
            }

            val payload = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                return parseGeminiResponseJson(responseText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(emptyList(), "")
    }

    private fun callHostingerGeminiGateway(base64Image: String): Pair<List<RegisterEntry>, String> {
        try {
            val url = URL("https://shribalajikripadham.online/api/scan_register_gemini.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("X-SBKD-API-KEY", HostingerCentralSyncManager.API_SECRET_KEY)
            }

            val payload = JSONObject().apply {
                put("image_base64", base64Image)
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val root = JSONObject(responseText)
                if (root.optBoolean("success", false)) {
                    val entriesArr = root.optJSONArray("entries")
                    if (entriesArr != null) {
                        return parseJsonEntriesArray(entriesArr)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(emptyList(), "")
    }

    private fun parseGeminiResponseJson(apiResponse: String): Pair<List<RegisterEntry>, String> {
        try {
            val root = JSONObject(apiResponse)
            val candidates = root.optJSONArray("candidates") ?: return Pair(emptyList(), "")
            if (candidates.length() == 0) return Pair(emptyList(), "")
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return Pair(emptyList(), "")
            val parts = content.optJSONArray("parts") ?: return Pair(emptyList(), "")
            if (parts.length() == 0) return Pair(emptyList(), "")
            val rawText = parts.getJSONObject(0).optString("text", "")

            var cleanJson = rawText.trim()
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replace(Regex("""^```(?:json)?\s*"""), "").replace(Regex("""```$"""), "").trim()
            }

            val jsonArray = JSONArray(cleanJson)
            return parseJsonEntriesArray(jsonArray)
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(emptyList(), "")
        }
    }

    private fun parseJsonEntriesArray(jsonArray: JSONArray): Pair<List<RegisterEntry>, String> {
        val entries = mutableListOf<RegisterEntry>()
        val sb = StringBuilder()
        var currentSerial = 1

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val serial = obj.optInt("serial", currentSerial)
            val rawName = obj.optString("name", "").trim()
            val rawPhone = obj.optString("phone", "").trim()
            val rawCity = obj.optString("city", "डूँगरा जाट").trim()

            val repairedPhone = repairHandwrittenPhone(rawPhone).ifBlank {
                rawPhone.filter { it.isDigit() }.let { if (it.length == 10 && it.first() in '6'..'9') it else "" }
            }

            val finalCity = if (rawCity.isBlank()) "डूँगरा जाट (स्थानीय)" else cleanCityName(rawCity)
            val finalName = cleanDevoteeName(rawName).ifBlank {
                if (repairedPhone.isNotBlank()) "भक्त ($repairedPhone)" else "भक्त $serial"
            }

            val entry = RegisterEntry(
                serialNumber = serial,
                patientName = finalName,
                phoneNumber = repairedPhone,
                city = finalCity,
                confidence = calculateConfidence(finalName, repairedPhone, finalCity, true).coerceAtLeast(88)
            )
            entries.add(entry)
            sb.append("$serial. $finalName | $repairedPhone | $finalCity\n")
            currentSerial = serial + 1
        }
        return Pair(entries, sb.toString())
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
