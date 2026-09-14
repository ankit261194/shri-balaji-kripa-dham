package com.example.shribalajikripadham.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.shribalajikripadham.data.model.BusSeat
import com.example.shribalajikripadham.data.model.PaymentRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates an official, high-resolution A4 PDF bus pilgrimage ticket.
 * Automatically saves to the user's phone Downloads directory and registers with Android MediaScanner.
 */
object BusTicketPdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 (72 DPI)
    private const val PAGE_HEIGHT = 842 // Standard A4 (72 DPI)
    private const val MARGIN = 30f

    fun generateA4BusTicket(
        context: Context,
        seats: List<BusSeat>,
        paymentRecord: PaymentRecord? = null,
        yatraDate: String = "आगामी रविवार / पूर्णिमा",
        boardingPoint: String = "श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, बुलन्दशहर"
    ): File? {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        val maroonColor = Color.rgb(139, 0, 0)
        val saffronColor = Color.rgb(220, 100, 0)
        val darkGrayColor = Color.rgb(50, 50, 50)
        val lightGoldBg = Color.rgb(255, 248, 230)
        val greenColor = Color.rgb(34, 139, 34)

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

        var currentY = MARGIN + 24f

        // 2. Temple Header
        val subHeaderPaint = Paint().apply {
            color = saffronColor
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("।। ॐ श्री हनुमते नमः ।।   ।। ॐ श्री गुरुदेवाय नमः ।।   ।। जय श्री बालाजी महाराज ।।", PAGE_WIDTH / 2f, currentY, subHeaderPaint)
        currentY += 22f

        val titleHeaderPaint = Paint().apply {
            color = maroonColor
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("श्री बालाजी कृपा धाम", PAGE_WIDTH / 2f, currentY, titleHeaderPaint)
        currentY += 15f

        val addressPaint = Paint().apply {
            color = darkGrayColor
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ग्राम डूँगरा जाट, तहसील शिकारपुर, ज़िला बुलन्दशहर (उ.प्र.) | परम पूज्य गुरुजी तेजवीर सिंह जी", PAGE_WIDTH / 2f, currentY, addressPaint)
        currentY += 16f

        // Ribbon Banner: Official Bus Ticket
        val bannerPaint = Paint().apply {
            color = maroonColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val bannerRect = RectF(MARGIN + 12f, currentY, PAGE_WIDTH - MARGIN - 12f, currentY + 28f)
        canvas.drawRoundRect(bannerRect, 6f, 6f, bannerPaint)

        val bannerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🚌 श्री बालाजी पावन यात्रा — आधिकारिक यात्रा टिकट (60-सीटर डीलक्स 3x2 व्यवस्था)", PAGE_WIDTH / 2f, currentY + 18f, bannerTextPaint)
        currentY += 38f

        // 3. Ticket & Booking Meta Card
        val metaBoxBg = Paint().apply {
            color = lightGoldBg
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val metaBorder = Paint().apply {
            color = Color.rgb(220, 180, 100)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        val metaBoxRect = RectF(MARGIN + 12f, currentY, PAGE_WIDTH - MARGIN - 12f, currentY + 68f)
        canvas.drawRoundRect(metaBoxRect, 8f, 8f, metaBoxBg)
        canvas.drawRoundRect(metaBoxRect, 8f, 8f, metaBorder)

        val metaLabelPaint = Paint().apply {
            color = Color.rgb(100, 100, 100)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val metaValuePaint = Paint().apply {
            color = Color.rgb(20, 20, 20)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bookingId = "SBKD-BUS-${System.currentTimeMillis().toString().takeLast(6)}"
        val issueTime = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("hi", "IN")).format(Date())

        // Meta Column 1
        canvas.drawText("बुकिंग आईडी (Booking ID):", MARGIN + 22f, currentY + 18f, metaLabelPaint)
        canvas.drawText(bookingId, MARGIN + 22f, currentY + 31f, metaValuePaint)

        canvas.drawText("जारी दिनांक व समय:", MARGIN + 22f, currentY + 47f, metaLabelPaint)
        canvas.drawText(issueTime, MARGIN + 22f, currentY + 60f, metaValuePaint)

        // Meta Column 2
        canvas.drawText("यात्रा दिनांक (Yatra Date):", PAGE_WIDTH / 2f + 10f, currentY + 18f, metaLabelPaint)
        canvas.drawText(yatraDate, PAGE_WIDTH / 2f + 10f, currentY + 31f, metaValuePaint)

        canvas.drawText("बोर्डिंग पॉइंट (Boarding):", PAGE_WIDTH / 2f + 10f, currentY + 47f, metaLabelPaint)
        val displayBoarding = if (boardingPoint.length > 32) boardingPoint.take(30) + "..." else boardingPoint
        canvas.drawText(displayBoarding, PAGE_WIDTH / 2f + 10f, currentY + 60f, metaValuePaint)

        currentY += 80f

        // 4. Passenger Details Table
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(240, 240, 245)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val tableBorderPaint = Paint().apply {
            color = Color.rgb(200, 200, 210)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

        val tableY = currentY
        val tableRowHeight = 22f
        val tableWidth = PAGE_WIDTH - (MARGIN + 12f) * 2
        val headerHeight = 22f

        canvas.drawRect(MARGIN + 12f, tableY, MARGIN + 12f + tableWidth, tableY + headerHeight, tableHeaderPaint)
        canvas.drawRect(MARGIN + 12f, tableY, MARGIN + 12f + tableWidth, tableY + headerHeight, tableBorderPaint)

        val col1X = MARGIN + 20f          // सीट क्र.
        val col2X = MARGIN + 85f          // यात्री नाम
        val col3X = MARGIN + 240f         // आयु / लिंग
        val col4X = MARGIN + 330f         // मोबाइल नंबर
        val col5X = MARGIN + 440f         // किराया

        val thPaint = Paint().apply {
            color = maroonColor
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText("सीट क्र.", col1X, tableY + 15f, thPaint)
        canvas.drawText("यात्री का नाम", col2X, tableY + 15f, thPaint)
        canvas.drawText("आयु / लिंग", col3X, tableY + 15f, thPaint)
        canvas.drawText("मोबाइल नंबर", col4X, tableY + 15f, thPaint)
        canvas.drawText("किराया", col5X, tableY + 15f, thPaint)

        currentY += headerHeight

        val tdPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val tdBold = Paint().apply {
            color = maroonColor
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var totalFare = 0
        for ((index, seat) in seats.withIndex()) {
            val rowY = currentY + (index * tableRowHeight)
            val isEven = index % 2 == 0
            if (isEven) {
                val rowBgPaint = Paint().apply {
                    color = Color.rgb(250, 250, 250)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(MARGIN + 12f, rowY, MARGIN + 12f + tableWidth, rowY + tableRowHeight, rowBgPaint)
            }
            canvas.drawRect(MARGIN + 12f, rowY, MARGIN + 12f + tableWidth, rowY + tableRowHeight, tableBorderPaint)

            canvas.drawText(seat.seatLabel.ifEmpty { "#${seat.seatNumber}" }, col1X, rowY + 15f, tdBold)
            val pName = seat.passengerName.ifEmpty { "यात्री ${index + 1}" }
            canvas.drawText(if (pName.length > 20) pName.take(18) + "..." else pName, col2X, rowY + 15f, tdPaint)
            val ageGender = "${if (seat.passengerAge > 0) "${seat.passengerAge} वर्ष" else "-"} / ${seat.passengerGender.ifEmpty { "-" }}"
            canvas.drawText(ageGender, col3X, rowY + 15f, tdPaint)
            canvas.drawText(seat.phoneNumber.ifEmpty { "-" }, col4X, rowY + 15f, tdPaint)
            val fare = if (seat.fareAmount > 0) seat.fareAmount else 1500
            totalFare += fare
            canvas.drawText("₹$fare", col5X, rowY + 15f, tdPaint)
        }

        currentY += (seats.size * tableRowHeight) + 12f

        // 5. Payment Audit Ledger Box
        val payBoxBg = Paint().apply {
            color = Color.rgb(240, 248, 255) // Alice Blue
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val payBoxBorder = Paint().apply {
            color = Color.rgb(176, 196, 222)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        val payBoxRect = RectF(MARGIN + 12f, currentY, PAGE_WIDTH - MARGIN - 12f, currentY + 84f)
        canvas.drawRoundRect(payBoxRect, 8f, 8f, payBoxBg)
        canvas.drawRoundRect(payBoxRect, 8f, 8f, payBoxBorder)

        val payTitlePaint = Paint().apply {
            color = Color.rgb(25, 25, 112) // Midnight Blue
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("💰 आश्रम भुगतान एवं ऑडिट विवरण (Payment Audit Ledger):", MARGIN + 22f, currentY + 18f, payTitlePaint)

        val finalAmount = if (paymentRecord != null && paymentRecord.amount > 0) paymentRecord.amount else totalFare.toDouble()
        val pApp = paymentRecord?.paymentApp?.ifBlank { "UPI QR कोड" } ?: "UPI QR कोड"
        val pTxn = paymentRecord?.transactionId?.ifBlank { seats.firstOrNull()?.transactionId?.ifBlank { "CASH-COUNTER" } ?: "CASH-COUNTER" } ?: "CASH-COUNTER"
        val pMode = paymentRecord?.paymentMode ?: if (pTxn.contains("CASH")) "ऑफलाइन (कैश)" else "UPI QR कोड"

        // Pay Col 1
        canvas.drawText("कुल किराया राशि:", MARGIN + 22f, currentY + 36f, metaLabelPaint)
        val amountPaint = Paint().apply {
            color = maroonColor
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("₹${finalAmount.toInt()} (${seats.size} सीटें)", MARGIN + 22f, currentY + 50f, amountPaint)

        canvas.drawText("भुगतान माध्यम व ऐप:", MARGIN + 22f, currentY + 64f, metaLabelPaint)
        canvas.drawText("$pMode ($pApp)", MARGIN + 22f, currentY + 76f, metaValuePaint)

        // Pay Col 2
        canvas.drawText("ट्रांजेक्शन आईडी / यूटीआर (UTR):", PAGE_WIDTH / 2f + 10f, currentY + 36f, metaLabelPaint)
        val utrPaint = Paint().apply {
            color = Color.rgb(0, 100, 0)
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(pTxn, PAGE_WIDTH / 2f + 10f, currentY + 49f, utrPaint)

        canvas.drawText("भुगतान स्थिति (Status):", PAGE_WIDTH / 2f + 10f, currentY + 64f, metaLabelPaint)
        val statusPaint = Paint().apply {
            color = greenColor
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("✓ भुगतान संपन्न एवं सीट आरक्षित (PAID / RESERVED)", PAGE_WIDTH / 2f + 10f, currentY + 76f, statusPaint)

        currentY += 96f

        // 6. Guidelines & Important Rules for Pilgrims Box
        val rulesBoxBg = Paint().apply {
            color = Color.rgb(255, 250, 240) // Floral White
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val rulesBorder = Paint().apply {
            color = Color.rgb(230, 210, 180)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        val rulesRect = RectF(MARGIN + 12f, currentY, PAGE_WIDTH - MARGIN - 12f, currentY + 115f)
        canvas.drawRoundRect(rulesRect, 8f, 8f, rulesBoxBg)
        canvas.drawRoundRect(rulesRect, 8f, 8f, rulesBorder)

        val rulesTitlePaint = Paint().apply {
            color = maroonColor
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("📜 यात्रियों के लिए आवश्यक नियम व दिशानिर्देश (Yatra Guidelines):", MARGIN + 22f, currentY + 18f, rulesTitlePaint)

        val rulePaint = Paint().apply {
            color = Color.rgb(40, 40, 40)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val rulesList = listOf(
            "१. यात्रा के दौरान सभी भक्त अपना मूल आधार कार्ड अथवा सरकारी पहचान पत्र अवश्य साथ रखें।",
            "२. बस प्रस्थान के निर्धारित समय से कम से कम ३० मिनट पूर्व आश्रम परिसर में उपस्थित होना अनिवार्य है।",
            "३. यह पावन धार्मिक यात्रा है; किसी भी प्रकार का नशा, मदिरा, तंबाकू या मांसाहार पूर्णतः वर्जित है।",
            "४. बस में बुजुर्गों, माताओं व बहनों के प्रति विनम्रता का व्यवहार रखें एवं स्वच्छता का विशेष ध्यान दें।",
            "५. वास्तविक बस व धर्मशाला व्यय के अतिरिक्त आश्रम द्वारा कोई अन्य शुल्क या कमीशन नहीं लिया जाता।"
        )

        var ruleY = currentY + 34f
        for (rule in rulesList) {
            canvas.drawText(rule, MARGIN + 22f, ruleY, rulePaint)
            ruleY += 16f
        }

        currentY += 128f

        // 7. Ashram Blessings & Verification Footer
        val footerBg = Paint().apply {
            color = lightGoldBg
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val footerRect = RectF(MARGIN + 12f, PAGE_HEIGHT - MARGIN - 60f, PAGE_WIDTH - MARGIN - 12f, PAGE_HEIGHT - MARGIN - 8f)
        canvas.drawRoundRect(footerRect, 6f, 6f, footerBg)

        val blessingsPaint = Paint().apply {
            color = maroonColor
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("।। संकट कटै मिटै सब पीरा। जो सुमिरै हनुमत बलबीरा ।।", PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN - 42f, blessingsPaint)

        val contactFooterPaint = Paint().apply {
            color = darkGrayColor
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("श्री बालाजी कृपा धाम प्रबंधन समिति | हेल्पलाइन: +91 9876543210 | www.shribalajikripadham.org", PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN - 26f, contactFooterPaint)

        val autoGenPaint = Paint().apply {
            color = Color.GRAY
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("यह डिजिटल पावती एवं यात्रा टिकट सिस्टम जनरेटेड है। किसी भौतिक हस्ताक्षर की आवश्यकता नहीं है।", PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN - 14f, autoGenPaint)

        pdfDoc.finishPage(page)

        // Save file to phone Downloads folder
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
                downloadsDir
            } else {
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            }

            val safeSeats = seats.joinToString("-") { it.seatLabel.ifEmpty { "${it.seatNumber}" } }
            val fileName = "SBKD_BusTicket_Seats_${safeSeats}_${System.currentTimeMillis() % 10000}.pdf"
            val pdfFile = File(targetDir, fileName)

            val fos = FileOutputStream(pdfFile)
            pdfDoc.writeTo(fos)
            fos.flush()
            fos.close()
            pdfDoc.close()

            // Trigger MediaScanner so it instantly reflects in device gallery and file explorer
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(pdfFile.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDoc.close()
            null
        }
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
                putExtra(
                    Intent.EXTRA_TEXT,
                    "जय श्री बालाजी! श्री बालाजी कृपा धाम, डूँगरा जाट की बालाजी यात्रा का आधिकारिक बस टिकट (A4 Ticket) संलग्न है।"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "बस टिकट शेयर करें / PDF देखें"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
