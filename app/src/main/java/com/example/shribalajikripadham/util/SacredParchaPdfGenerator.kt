package com.example.shribalajikripadham.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.shribalajikripadham.data.model.SacredParcha
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates an elegant, high-resolution A4 PDF document for Ashram Parchas (Hawan, Utara, Ardas, etc.).
 * Includes sacred borders, temple insignia, 2-column samagri list, step-by-step vidhi, and ashram blessings.
 */
object SacredParchaPdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 (72 DPI)
    private const val PAGE_HEIGHT = 842 // Standard A4 (72 DPI)
    private const val MARGIN = 30f

    fun generateA4ParchaPdf(context: Context, parcha: SacredParcha): File? {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        val maroonColor = Color.rgb(139, 0, 0)
        val saffronColor = Color.rgb(220, 100, 0)
        val darkGrayColor = Color.rgb(50, 50, 50)
        val lightGoldBg = Color.rgb(255, 248, 230)
        val lightRedBg = Color.rgb(255, 240, 240)

        // 1. Page Background & Double Sacred Border
        canvas.drawColor(Color.WHITE)

        val outerBorderPaint = Paint().apply {
            color = maroonColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRect(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN, outerBorderPaint)

        val innerBorderPaint = Paint().apply {
            color = saffronColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRect(MARGIN + 4f, MARGIN + 4f, PAGE_WIDTH - MARGIN - 4f, PAGE_HEIGHT - MARGIN - 4f, innerBorderPaint)

        // Corner Sacred Symbols
        val symbolPaint = Paint().apply {
            color = maroonColor
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ॐ", MARGIN + 12f, MARGIN + 15f, symbolPaint)
        canvas.drawText("卐", PAGE_WIDTH - MARGIN - 12f, MARGIN + 15f, symbolPaint)
        canvas.drawText("卐", MARGIN + 12f, PAGE_HEIGHT - MARGIN - 8f, symbolPaint)
        canvas.drawText("ॐ", PAGE_WIDTH - MARGIN - 12f, PAGE_HEIGHT - MARGIN - 8f, symbolPaint)

        var currentY = MARGIN + 22f

        // 2. Temple Header
        val subHeaderPaint = Paint().apply {
            color = saffronColor
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("।। ॐ श्री हनुमते नमः ।।   ।। ॐ श्री गुरुदेवाय नमः ।।", PAGE_WIDTH / 2f, currentY, subHeaderPaint)
        currentY += 20f

        val titleHeaderPaint = Paint().apply {
            color = maroonColor
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("श्री बालाजी कृपा धाम", PAGE_WIDTH / 2f, currentY, titleHeaderPaint)
        currentY += 14f

        val addressPaint = Paint().apply {
            color = darkGrayColor
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ग्राम डूँगरा जाट, निकट जहांगीराबाद, बुलन्दशहर (उ.प्र.) | हेल्पलाइन: +91 9927581561", PAGE_WIDTH / 2f, currentY, addressPaint)
        currentY += 12f

        // Divider
        canvas.drawLine(MARGIN + 15f, currentY, PAGE_WIDTH - MARGIN - 15f, currentY, innerBorderPaint)
        currentY += 16f

        // 3. Parcha Title Box
        val titleBoxHeight = if (parcha.subtitle.isNotBlank()) 46f else 34f
        val titleBoxRect = RectF(MARGIN + 15f, currentY, PAGE_WIDTH - MARGIN - 15f, currentY + titleBoxHeight)
        val titleBoxBg = Paint().apply {
            color = lightGoldBg
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(titleBoxRect, 6f, 6f, titleBoxBg)
        canvas.drawRoundRect(titleBoxRect, 6f, 6f, innerBorderPaint)

        val parchaTitlePaint = Paint().apply {
            color = maroonColor
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(parcha.title, PAGE_WIDTH / 2f, currentY + 18f, parchaTitlePaint)

        if (parcha.subtitle.isNotBlank()) {
            val parchaSubPaint = Paint().apply {
                color = saffronColor
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(parcha.subtitle, PAGE_WIDTH / 2f, currentY + 34f, parchaSubPaint)
        }
        currentY += titleBoxHeight + 14f

        val contentWidth = PAGE_WIDTH - (MARGIN * 2f) - 30f
        val startX = MARGIN + 15f

        val sectionHeaderPaint = Paint().apply {
            color = maroonColor
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val itemTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val bulletPaint = Paint().apply {
            color = saffronColor
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // 4. Section: Required Samagri (2-column layout)
        if (parcha.samagriList.isNotEmpty()) {
            canvas.drawText("🪔 आवश्यक पूजा व अनुष्ठान सामग्री:", startX, currentY, sectionHeaderPaint)
            currentY += 14f

            val colWidth = contentWidth / 2f
            var col = 0
            var rowStartY = currentY

            for (item in parcha.samagriList) {
                val itemX = if (col == 0) startX + 6f else startX + colWidth + 6f
                canvas.drawText("•", itemX, currentY, bulletPaint)
                canvas.drawText(item, itemX + 10f, currentY, itemTextPaint)

                if (col == 0) {
                    col = 1
                } else {
                    col = 0
                    currentY += 15f
                }
            }
            if (col == 1) {
                currentY += 15f
            }
            currentY += 8f
        }

        // 5. Section: Step-by-Step Vidhi
        if (parcha.vidhiSteps.isNotEmpty()) {
            canvas.drawText("📜 चरणबद्ध संपूर्ण विधि व नियम:", startX, currentY, sectionHeaderPaint)
            currentY += 14f

            for (step in parcha.vidhiSteps) {
                // Word wrap step
                val wrappedLines = wrapText(step, itemTextPaint, contentWidth - 12f)
                for (line in wrappedLines) {
                    canvas.drawText(line, startX + 6f, currentY, itemTextPaint)
                    currentY += 13.5f
                }
                currentY += 3f
            }
            currentY += 6f
        }

        // 6. Section: Precautions & Rules Box
        if (parcha.precautions.isNotEmpty()) {
            val preStartY = currentY
            var measuredHeight = 22f
            for (p in parcha.precautions) {
                val lines = wrapText("⚠️ $p", itemTextPaint, contentWidth - 20f)
                measuredHeight += lines.size * 13.5f + 2f
            }

            val preRect = RectF(startX, preStartY, startX + contentWidth, preStartY + measuredHeight)
            val preBg = Paint().apply {
                color = lightRedBg
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(preRect, 5f, 5f, preBg)
            canvas.drawRoundRect(preRect, 5f, 5f, Paint().apply {
                color = maroonColor
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            })

            canvas.drawText("महत्वपूर्ण सावधानियाँ व परहेज:", startX + 8f, preStartY + 14f, sectionHeaderPaint)
            var preY = preStartY + 27f
            for (p in parcha.precautions) {
                val lines = wrapText("• $p", itemTextPaint, contentWidth - 20f)
                for (line in lines) {
                    canvas.drawText(line, startX + 10f, preY, itemTextPaint)
                    preY += 13.5f
                }
            }
            currentY = preStartY + measuredHeight + 12f
        }

        // 7. Section: Sacred Mantras Box (Center aligned)
        if (parcha.mantraText.isNotBlank()) {
            val mantraLines = parcha.mantraText.lines().map { it.trim() }.filter { it.isNotBlank() }
            val mantraBoxHeight = mantraLines.size * 15f + 16f
            val mantraRect = RectF(startX, currentY, startX + contentWidth, currentY + mantraBoxHeight)
            val mantraBg = Paint().apply {
                color = lightGoldBg
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(mantraRect, 5f, 5f, mantraBg)
            canvas.drawRoundRect(mantraRect, 5f, 5f, innerBorderPaint)

            val mantraPaint = Paint().apply {
                color = maroonColor
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            var mY = currentY + 16f
            for (mLine in mantraLines) {
                canvas.drawText(mLine, PAGE_WIDTH / 2f, mY, mantraPaint)
                mY += 15f
            }
            currentY += mantraBoxHeight + 12f
        }

        // 8. Sacred Footer
        val footerY = PAGE_HEIGHT - MARGIN - 26f
        val blessingsPaint = Paint().apply {
            color = maroonColor
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("।। संकट कटै मिटै सब पीरा। जो सुमिरै हनुमत बलबीरा ।।", PAGE_WIDTH / 2f, footerY, blessingsPaint)

        val footerMetaPaint = Paint().apply {
            color = darkGrayColor
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("hi", "IN")).format(Date())
        canvas.drawText("श्री बालाजी कृपा धाम, डूँगरा जाट | निःशुल्क जनसेवा | प्रिंट समय: $genDate", PAGE_WIDTH / 2f, footerY + 12f, footerMetaPaint)

        pdfDoc.finishPage(page)

        // Save file to Downloads / Documents
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = if (downloadsDir != null && downloadsDir.exists()) downloadsDir else (context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir)

            val safeTitle = parcha.title.replace(Regex("[^a-zA-Z0-9\\u0900-\\u097F]"), "_").take(30)
            val pdfFile = File(targetDir, "SBKD_${safeTitle}_${parcha.parchaId}.pdf")

            val fos = FileOutputStream(pdfFile)
            pdfDoc.writeTo(fos)
            fos.flush()
            fos.close()
            pdfDoc.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDoc.close()
            null
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    fun viewOrSharePdf(context: Context, pdfFile: File, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "जय श्री बालाजी! श्री बालाजी कृपा धाम, डूँगरा जाट का आधिकारिक पर्चा: $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "पर्चा शेयर करें / PDF देखें"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
