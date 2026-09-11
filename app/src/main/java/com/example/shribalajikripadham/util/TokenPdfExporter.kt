package com.example.shribalajikripadham.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.Token
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TokenPdfExporter {

    private const val PAGE_WIDTH = 595 // Standard A4 (72 DPI)
    private const val PAGE_HEIGHT = 842 // Standard A4 (72 DPI)
    private const val ROWS_PER_PAGE = 22

    /**
     * Generates a printable PDF file of tokens and launches the viewer/share sheet.
     */
    fun exportTokensToPdf(
        context: Context,
        tokens: List<Token>,
        settings: AshramSettings,
        dateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): File? {
        val pdfDoc = PdfDocument()

        val titlePaint = Paint().apply {
            color = Color.rgb(128, 0, 0) // Royal Maroon
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val subTitlePaint = Paint().apply {
            color = Color.rgb(180, 80, 0) // Saffron
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val metaPaint = Paint().apply {
            color = Color.rgb(60, 60, 60)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val tableHeaderBgPaint = Paint().apply {
            color = Color.rgb(128, 0, 0)
            style = Paint.Style.FILL
        }

        val rowTextPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val rowCompletedPaint = Paint().apply {
            color = Color.rgb(0, 130, 40) // Green
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val rowPendingPaint = Paint().apply {
            color = Color.rgb(200, 80, 0) // Orange
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val rowEvenBgPaint = Paint().apply {
            color = Color.rgb(250, 246, 240)
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(210, 210, 210)
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }

        val totalTokens = tokens.size
        val completedCount = tokens.count { it.isDarshanCompleted }
        val pendingCount = totalTokens - completedCount

        val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val totalPages = if (tokens.isEmpty()) 1 else (tokens.size + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE

        for (pageNum in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // 1. Header Border & Background
            val margin = 24f
            val topBoxHeight = 84f
            val headerRect = RectF(margin, margin, PAGE_WIDTH - margin, margin + topBoxHeight)
            val headerBg = Paint().apply {
                color = Color.rgb(255, 250, 240)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(headerRect, 8f, 8f, headerBg)
            canvas.drawRoundRect(headerRect, 8f, 8f, borderPaint)

            // Header Texts
            canvas.drawText(
                settings.ashramName.ifEmpty { "श्री बालाजी कृपा धाम (डूँगरा जाट)" },
                PAGE_WIDTH / 2f,
                margin + 22f,
                titlePaint
            )

            canvas.drawText(
                "परम पूज्य गुरुजी तेजवीर सिंह जी | दैनिक रविवार दरबार टोकन सूची",
                PAGE_WIDTH / 2f,
                margin + 38f,
                subTitlePaint
            )

            val infoLine = "दिनांक: $dateString   |   कुल टोकन: $totalTokens   |   दर्शन संपन्न: $completedCount   |   दर्शन शेष: $pendingCount"
            val infoPaint = Paint(subTitlePaint).apply {
                color = Color.rgb(50, 50, 50)
                textSize = 8.5f
            }
            canvas.drawText(infoLine, PAGE_WIDTH / 2f, margin + 56f, infoPaint)

            val addressLine = "${settings.address} | हेल्पलाइन: ${settings.contactPhone}"
            canvas.drawText(addressLine, PAGE_WIDTH / 2f, margin + 72f, metaPaint.apply { textAlign = Paint.Align.CENTER })
            metaPaint.textAlign = Paint.Align.LEFT

            // 2. Table Column Dimensions
            val tableTop = margin + topBoxHeight + 14f
            val tableWidth = PAGE_WIDTH - (margin * 2)
            val rowHeight = 22f

            val colXToken = margin + 6f
            val colXName = margin + 52f
            val colXPhone = margin + 175f
            val colXCity = margin + 265f
            val colXDistance = margin + 370f
            val colXTime = margin + 445f
            val colXStatus = margin + 495f

            // Table Header Bar
            val thRect = RectF(margin, tableTop, margin + tableWidth, tableTop + rowHeight)
            canvas.drawRoundRect(thRect, 4f, 4f, tableHeaderBgPaint)

            val thY = tableTop + 14f
            canvas.drawText("टोकन #", colXToken, thY, tableHeaderPaint)
            canvas.drawText("भक्त का नाम", colXName, thY, tableHeaderPaint)
            canvas.drawText("मोबाइल नंबर", colXPhone, thY, tableHeaderPaint)
            canvas.drawText("शहर / ग्राम", colXCity, thY, tableHeaderPaint)
            canvas.drawText("आश्रम से दूरी", colXDistance, thY, tableHeaderPaint)
            canvas.drawText("समय", colXTime, thY, tableHeaderPaint)
            canvas.drawText("दर्शन स्थिति", colXStatus, thY, tableHeaderPaint)

            // Table Rows
            val startIndex = (pageNum - 1) * ROWS_PER_PAGE
            val endIndex = minOf(startIndex + ROWS_PER_PAGE, tokens.size)

            var currentY = tableTop + rowHeight

            if (tokens.isEmpty()) {
                val emptyY = currentY + 40f
                val emptyPaint = Paint(metaPaint).apply {
                    textSize = 12f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("आज की तिथि में कोई टोकन पंजीकृत नहीं है।", PAGE_WIDTH / 2f, emptyY, emptyPaint)
            } else {
                for (i in startIndex until endIndex) {
                    val token = tokens[i]
                    val isEven = (i % 2 == 0)

                    if (isEven) {
                        val rowBgRect = RectF(margin, currentY, margin + tableWidth, currentY + rowHeight)
                        canvas.drawRect(rowBgRect, rowEvenBgPaint)
                    }

                    // Divider line
                    canvas.drawLine(margin, currentY + rowHeight, margin + tableWidth, currentY + rowHeight, borderPaint)

                    val textY = currentY + 14f

                    // 1. Token Number
                    canvas.drawText("#${token.tokenNumber}", colXToken, textY, rowTextPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                    rowTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                    // 2. Patient Name (Truncated if too long)
                    val safeName = if (token.patientName.length > 20) token.patientName.take(19) + "…" else token.patientName
                    canvas.drawText(safeName, colXName, textY, rowTextPaint)

                    // 3. Phone Number
                    val safePhone = if (token.phoneNumber.length > 13) token.phoneNumber.take(13) else token.phoneNumber
                    canvas.drawText(safePhone, colXPhone, textY, rowTextPaint)

                    // 4. City
                    val safeCity = if (token.city.length > 16) token.city.take(15) + "…" else token.city
                    canvas.drawText(safeCity, colXCity, textY, rowTextPaint)

                    // 5. Distance
                    val distKm = TokenDistanceHelper.calculateDistanceKm(
                        settings.latitude, settings.longitude, token.latitude, token.longitude
                    )
                    val distStr = if (distKm < 0f) "काउंटर" else if (distKm < 1f) "< 1 किमी" else "%.1f किमी".format(distKm)
                    canvas.drawText(distStr, colXDistance, textY, rowTextPaint)

                    // 6. Time
                    val timeStr = timeSdf.format(Date(token.createdAt))
                    canvas.drawText(timeStr, colXTime, textY, rowTextPaint)

                    // 7. Status Checkmark
                    if (token.isDarshanCompleted) {
                        canvas.drawText("✓ संपन्न", colXStatus, textY, rowCompletedPaint)
                    } else {
                        canvas.drawText("⏳ शेष", colXStatus, textY, rowPendingPaint)
                    }

                    currentY += rowHeight
                }
            }

            // 3. Footer
            val footerY = PAGE_HEIGHT - 22f
            val footerPaint = Paint().apply {
                color = Color.rgb(120, 120, 120)
                textSize = 7.5f
                isAntiAlias = true
            }
            canvas.drawText(
                "श्री बालाजी कृपा धाम | 100% निःशुल्क सेवा | पृष्ठ $pageNum / $totalPages",
                margin,
                footerY,
                footerPaint
            )

            val generatedOn = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())
            val genPaint = Paint(footerPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("रिपोर्ट समय: $generatedOn", PAGE_WIDTH - margin, footerY, genPaint)

            pdfDoc.finishPage(page)
        }

        // Save PDF to Documents Directory
        return try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!docsDir.exists()) docsDir.mkdirs()

            val sanitizedDate = dateString.replace("-", "_")
            val pdfFile = File(docsDir, "Balaji_Tokens_${sanitizedDate}.pdf")

            val outStream = FileOutputStream(pdfFile)
            pdfDoc.writeTo(outStream)
            outStream.flush()
            outStream.close()
            pdfDoc.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDoc.close()
            null
        }
    }

    /**
     * Generates a dedicated high-quality single Token Receipt PDF matching the official prompt layout:
     * Token ID: #BKD-XXXX
     * Devotee Name: [User Name]
     * Resident Of / From: [User Origin Location]
     * Destination: Shri Balaji Kripa Dham, Dungra Jaat
     * Total Distance: [XX KM]
     * Date & Time: [Timestamp]
     */
    fun generateSingleTokenReceiptPdf(
        context: Context,
        token: Token,
        settings: AshramSettings
    ): File? {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(420, 595, 1).create() // A5 size for receipt
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        return try {
            val bgPaint = Paint().apply {
                color = Color.rgb(255, 253, 248)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 420f, 595f, bgPaint)

            val borderPaint = Paint().apply {
                color = Color.rgb(180, 0, 0)
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            val innerBorder = Paint().apply {
                color = Color.rgb(212, 175, 55) // Gold
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }
            canvas.drawRect(14f, 14f, 406f, 581f, borderPaint)
            canvas.drawRect(18f, 18f, 402f, 577f, innerBorder)

            // Header Background
            val headerBg = Paint().apply {
                color = Color.rgb(128, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRect(18f, 18f, 402f, 95f, headerBg)

            val titleWhite = Paint().apply {
                color = Color.WHITE
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val subWhite = Paint().apply {
                color = Color.rgb(255, 235, 180)
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            canvas.drawText("॥ श्री हनुमते नमः ॥", 210f, 38f, subWhite)
            canvas.drawText(settings.ashramName.ifEmpty { "श्री बालाजी कृपा धाम (डूँगरा जाट)" }, 210f, 58f, titleWhite)
            canvas.drawText("परम पूज्य गुरुजी तेजवीर सिंह जी | अधिकृत दर्शन टोकन रसीद", 210f, 75f, subWhite)
            canvas.drawText("ग्राम डूँगरा जाट, बुलंदशहर, उत्तर प्रदेश", 210f, 88f, subWhite)

            // Large Token Number Box
            val tokenBoxBg = Paint().apply {
                color = Color.rgb(255, 243, 224)
                style = Paint.Style.FILL
            }
            val tokenBoxBorder = Paint().apply {
                color = Color.rgb(230, 81, 0)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            val boxRect = RectF(50f, 110f, 370f, 185f)
            canvas.drawRoundRect(boxRect, 12f, 12f, tokenBoxBg)
            canvas.drawRoundRect(boxRect, 12f, 12f, tokenBoxBorder)

            val tokenBadgeLabel = Paint().apply {
                color = Color.rgb(120, 50, 0)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("॥ आपका अधिकृत रविवार दर्शन टोकन ॥", 210f, 128f, tokenBadgeLabel)

            val tokenNumPaint = Paint().apply {
                color = Color.rgb(180, 0, 0)
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("#BKD-${token.tokenNumber}", 210f, 168f, tokenNumPaint)

            // Key Value Details Table
            val labelPaint = Paint().apply {
                color = Color.rgb(70, 70, 70)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val valuePaint = Paint().apply {
                color = Color.rgb(20, 20, 20)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            val highlightPaint = Paint().apply {
                color = Color.rgb(180, 0, 0)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            var startY = 215f
            val lineSpacing = 32f
            val col1X = 40f
            val col2X = 180f

            fun drawRow(label: String, value: String, isHighlight: Boolean = false) {
                canvas.drawText(label, col1X, startY, labelPaint)
                canvas.drawText(value, col2X, startY, if (isHighlight) highlightPaint else valuePaint)
                canvas.drawLine(col1X, startY + 8f, 380f, startY + 8f, Paint().apply {
                    color = Color.rgb(235, 235, 235)
                    strokeWidth = 0.8f
                })
                startY += lineSpacing
            }

            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val dateFormatted = sdf.format(Date(token.createdAt))

            drawRow("Token ID:", "#BKD-${token.tokenNumber}", true)
            drawRow("Devotee Name:", token.patientName, true)
            drawRow("Mobile Number:", token.phoneNumber)
            drawRow("Resident Of / From:", token.originAddress.ifEmpty { token.city })
            drawRow("Destination:", token.destinationAddress)

            val distFormatted = when {
                token.distanceKm >= 0f -> "%.1f KM (सड़क मार्ग)".format(token.distanceKm)
                else -> "आश्रम परिसर (स्थानीय)"
            }
            drawRow("Total Distance:", distFormatted, true)
            drawRow("Date & Time:", dateFormatted)

            val darshanStatusText = if (token.isDarshanCompleted) "✓ दर्शन संपन्न (COMPLETED)" else "⏳ कतार में सक्रिय (WAITING)"
            drawRow("Darshan Status:", darshanStatusText)

            // Disclaimer Box
            val discRect = RectF(30f, 480f, 390f, 555f)
            val discBg = Paint().apply {
                color = Color.rgb(255, 250, 235)
                style = Paint.Style.FILL
            }
            val discBorder = Paint().apply {
                color = Color.rgb(230, 200, 140)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(discRect, 8f, 8f, discBg)
            canvas.drawRoundRect(discRect, 8f, 8f, discBorder)

            val discTextPaint = Paint().apply {
                color = Color.rgb(100, 50, 0)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val discBoldPaint = Paint().apply {
                color = Color.rgb(180, 0, 0)
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            canvas.drawText("॥ महत्वपूर्ण सूचना ॥", 210f, 498f, discBoldPaint)
            canvas.drawText("भूत-प्रेत व मानसिक समस्याओं का पूर्णतः निःशुल्क (FREE) इलाज।", 210f, 514f, discTextPaint)
            canvas.drawText("आश्रम में किसी भी प्रकार का कोई शुल्क या दक्षिणा नहीं ली जाती।", 210f, 528f, discTextPaint)
            canvas.drawText("हेल्पलाइन: ${settings.contactPhone} | ग्राम डूँगरा जाट, बुलंदशहर", 210f, 544f, discTextPaint)

            pdfDoc.finishPage(page)

            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!docsDir.exists()) docsDir.mkdirs()

            val receiptFile = File(docsDir, "Balaji_Token_${token.tokenNumber}_Receipt.pdf")
            val outStream = FileOutputStream(receiptFile)
            pdfDoc.writeTo(outStream)
            outStream.flush()
            outStream.close()
            pdfDoc.close()

            receiptFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDoc.close()
            null
        }
    }

    /**
     * Directly shares or prints single token receipt PDF.
     */
    fun shareSingleTokenReceipt(context: Context, token: Token, settings: AshramSettings) {
        val file = generateSingleTokenReceiptPdf(context, token, settings)
        if (file != null && file.exists()) {
            openOrSharePdf(context, file)
        }
    }

    /**
     * Opens the generated PDF using FileProvider and an external viewer / share sheet.
     */
    fun openOrSharePdf(context: Context, pdfFile: File?) {
        if (pdfFile == null || !pdfFile.exists()) return
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooser = Intent.createChooser(viewIntent, "PDF टोकन रिपोर्ट खोलें / साझा करें").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
