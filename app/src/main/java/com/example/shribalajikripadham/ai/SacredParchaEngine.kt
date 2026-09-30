package com.example.shribalajikripadham.ai

import com.example.shribalajikripadham.data.model.ParchaCategory
import com.example.shribalajikripadham.data.model.SacredParcha

/**
 * Intelligent Parcha OCR Text Parser & Sacred Layout Engine.
 * Formats handwritten or scanned paper parchas into structured A4-ready documents.
 */
object SacredParchaEngine {

    /**
     * Parses unstructured OCR text from a photographed paper slip into structured SacredParcha components.
     */
    fun parseScannedParchaText(rawText: String, defaultCategory: ParchaCategory = ParchaCategory.OTHER): SacredParcha {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return SacredParcha(
                title = "नया आश्रम पर्चा",
                category = defaultCategory
            )
        }

        var title = lines[0]
        var subtitle = ""
        val samagri = mutableListOf<String>()
        val vidhi = mutableListOf<String>()
        val precautions = mutableListOf<String>()
        val mantras = mutableListOf<String>()

        var currentSection = 0 // 0=Header/Subtitle, 1=Samagri, 2=Vidhi, 3=Precautions, 4=Mantra

        // Detect category from title keywords
        var category = defaultCategory
        val lowerText = rawText.lowercase()
        if (lowerText.contains("हवन") || lowerText.contains("hawan")) category = ParchaCategory.HAWAN
        else if (lowerText.contains("उतारा") || lowerText.contains("मैया") || lowerText.contains("utara")) category = ParchaCategory.UTARA
        else if (lowerText.contains("अर्जी") || lowerText.contains("अरदास") || lowerText.contains("arji")) category = ParchaCategory.ARJI_ARDAS
        else if (lowerText.contains("नियम") || lowerText.contains("परहेज") || lowerText.contains("झाड़ा")) category = ParchaCategory.NIYAM_PARHEZ
        else if (lowerText.contains("आरती") || lowerText.contains("चालीसा") || lowerText.contains("स्तुति")) category = ParchaCategory.AARTI_STUTI

        for (i in lines.indices) {
            val line = lines[i]
            val lowerLine = line.lowercase()

            if (i == 0) {
                title = line.trimStart('#', '*', ' ', '-')
                continue
            }

            // Detect section triggers
            if (lowerLine.contains("सामग्री") || lowerLine.contains("आवश्यक") || lowerLine.contains("वस्तु") || lowerLine.contains("सामान")) {
                currentSection = 1
                continue
            } else if (lowerLine.contains("विधि") || lowerLine.contains("नियम से करें") || lowerLine.contains("प्रक्रिया") || lowerLine.contains("तरीका")) {
                currentSection = 2
                continue
            } else if (lowerLine.contains("परहेज") || lowerLine.contains("सावधानी") || lowerLine.contains("वर्जित") || lowerLine.contains("ध्यान दें")) {
                currentSection = 3
                continue
            } else if (lowerLine.contains("मंत्र") || lowerLine.contains("श्लोक") || lowerLine.contains("स्तुति") || lowerLine.contains("दोहा") || line.startsWith("।।")) {
                currentSection = 4
                mantras.add(line)
                continue
            }

            val cleanedLine = line.trimStart('-', '*', '•', '~', '>', ' ', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '.', ')')
            if (cleanedLine.isBlank()) continue

            when (currentSection) {
                0 -> {
                    if (subtitle.isBlank()) subtitle = line
                    else samagri.add(cleanedLine)
                }
                1 -> samagri.add(cleanedLine)
                2 -> vidhi.add(cleanedLine)
                3 -> precautions.add(cleanedLine)
                4 -> mantras.add(line)
            }
        }

        return SacredParcha(
            title = title.ifBlank { "आश्रम सेवा पर्चा" },
            subtitle = subtitle,
            category = category,
            samagriList = samagri,
            vidhiSteps = vidhi,
            precautions = precautions,
            mantraText = mantras.joinToString("\n"),
            isPublished = true,
            isHidden = false
        )
    }

    /**
     * Strict Zero-Dummy Policy: No hardcoded or fake canonical parchas.
     * All parchas must be created by Super Admin / Sevadar.
     */
    fun getCanonicalParchas(): List<SacredParcha> = emptyList()
}
