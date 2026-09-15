package com.example.shribalajikripadham.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.ui.graphics.toArgb
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

data class SevadarIdCardData(
    val sevadarId: String,
    val name: String,
    val role: String,
    val dutyArea: String,
    val phoneNumber: String,
    val bloodGroup: String = "O+",
    val validityYear: String = "2026 - 2027",
    val ashramAddress: String = "ग्राम डूँगरा जाट, शिकारपुर, बुलंदशहर (उ.प्र.)",
    val emergencyPhone: String = "+91 98765 43210",
    val photoUri: String = "",
    val templateId: String = "RG_01"
)

object PhotoshopPsdGenerator {

    const val CARD_WIDTH = 600   // 2.0 inches @ 300 DPI
    const val CARD_HEIGHT = 1050 // 3.5 inches @ 300 DPI

    /**
     * Generates a 300 DPI Bitmap of the ID Card (Front or Back)
     */
    fun generateIdCardBitmap(
        context: Context,
        data: SevadarIdCardData,
        template: IdCardTemplate,
        isBackSide: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val primaryArgb = template.primaryColor.toArgb()
        val secondaryArgb = template.secondaryColor.toArgb()
        val accentArgb = template.accentColor.toArgb()
        val bgArgb = template.backgroundColor.toArgb()
        val textArgb = template.textColor.toArgb()

        if (!isBackSide) {
            // ==========================================
            // FRONT SIDE RENDERING
            // ==========================================
            // 1. Background Fill
            val bgPaint = Paint().apply {
                color = bgArgb
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bgPaint)

            // 2. Outer Ornate Border
            val borderPaint = Paint().apply {
                color = primaryArgb
                style = Paint.Style.STROKE
                strokeWidth = 10f
            }
            canvas.drawRoundRect(RectF(12f, 12f, CARD_WIDTH - 12f, CARD_HEIGHT - 12f), 24f, 24f, borderPaint)

            val innerGoldPaint = Paint().apply {
                color = secondaryArgb
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawRoundRect(RectF(20f, 20f, CARD_WIDTH - 20f, CARD_HEIGHT - 20f), 18f, 18f, innerGoldPaint)

            // 3. Top Header Ribbon / Banner
            val headerPaint = Paint().apply {
                color = primaryArgb
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(22f, 22f, CARD_WIDTH - 22f, 210f), 16f, 16f, headerPaint)

            val headerGoldLine = Paint().apply {
                color = secondaryArgb
                style = Paint.Style.FILL
            }
            canvas.drawRect(22f, 204f, CARD_WIDTH - 22f, 210f, headerGoldLine)

            // Header Text: Ashram Title & Emblem
            val titlePaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("${template.emblem} श्री बालाजी कृपा धाम ${template.emblem}", (CARD_WIDTH / 2).toFloat(), 70f, titlePaint)

            val subtitlePaint = Paint().apply {
                color = secondaryArgb
                textSize = 19f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("ग्राम डूँगरा जाट, बुलंदशहर (उ.प्र.)", (CARD_WIDTH / 2).toFloat(), 108f, subtitlePaint)

            val blessingsPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 15f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार", (CARD_WIDTH / 2).toFloat(), 145f, blessingsPaint)

            val badgeTitlePaint = Paint().apply {
                color = secondaryArgb
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("★ अधिकृत आश्रम पहचान पत्र ★", (CARD_WIDTH / 2).toFloat(), 185f, badgeTitlePaint)

            // 4. Sevadar Photo (Centered with ornate border)
            val photoCenterX = (CARD_WIDTH / 2).toFloat()
            val photoCenterY = 360f
            val photoRadius = 110f

            val photoBorderPaint = Paint().apply {
                color = secondaryArgb
                style = Paint.Style.STROKE
                strokeWidth = 8f
                isAntiAlias = true
            }
            canvas.drawCircle(photoCenterX, photoCenterY, photoRadius + 4f, photoBorderPaint)

            val photoBgPaint = Paint().apply {
                color = primaryArgb
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(photoCenterX, photoCenterY, photoRadius, photoBgPaint)

            // Load and draw photo if available
            var photoDrawn = false
            if (data.photoUri.isNotBlank()) {
                try {
                    val loadedBitmap = DevoteePhotoHelper.loadBitmap(context, data.photoUri)
                    if (loadedBitmap != null) {
                        val scaledPhoto = Bitmap.createScaledBitmap(loadedBitmap, (photoRadius * 2).toInt(), (photoRadius * 2).toInt(), true)
                        canvas.save()
                        val path = android.graphics.Path().apply {
                            addCircle(photoCenterX, photoCenterY, photoRadius - 2f, android.graphics.Path.Direction.CCW)
                        }
                        canvas.clipPath(path)
                        canvas.drawBitmap(scaledPhoto, photoCenterX - photoRadius, photoCenterY - photoRadius, null)
                        canvas.restore()
                        photoDrawn = true
                    }
                } catch (e: Exception) {}
            }

            if (!photoDrawn) {
                val initialPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 55f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val initial = if (data.name.isNotBlank()) data.name.take(2) else "सेवा"
                canvas.drawText(initial, photoCenterX, photoCenterY + 20f, initialPaint)
            }

            // 5. Sevadar Name
            val namePaint = Paint().apply {
                color = primaryArgb
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(data.name.ifBlank { "सेवादार का नाम" }, photoCenterX, 520f, namePaint)

            // 6. Role Ribbon / Badge
            val roleRibbonPaint = Paint().apply {
                color = primaryArgb
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(60f, 545f, CARD_WIDTH - 60f, 600f), 14f, 14f, roleRibbonPaint)

            val roleGoldBorder = Paint().apply {
                color = secondaryArgb
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawRoundRect(RectF(60f, 545f, CARD_WIDTH - 60f, 600f), 14f, 14f, roleGoldBorder)

            val roleTextPaint = Paint().apply {
                color = secondaryArgb
                textSize = 21f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(data.role.ifBlank { "अधिकृत सेवादार" }, photoCenterX, 582f, roleTextPaint)

            // 7. Details Table / List
            val labelPaint = Paint().apply {
                color = textArgb
                textSize = 19f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val valuePaint = Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 19f
                isAntiAlias = true
            }

            var startY = 645f
            val lineSpacing = 42f

            fun drawDetailRow(label: String, value: String) {
                canvas.drawText(label, 60f, startY, labelPaint)
                canvas.drawText(": $value", 210f, startY, valuePaint)
                startY += lineSpacing
            }

            drawDetailRow("आईडी नंबर", data.sevadarId.ifBlank { "SBKD-SEVA-001" })
            drawDetailRow("सेवा क्षेत्र", data.dutyArea.ifBlank { "कतार व टोकन व्यवस्था" })
            drawDetailRow("मोबाइल", data.phoneNumber.ifBlank { "+91 98765 00000" })
            drawDetailRow("रक्त समूह", data.bloodGroup.ifBlank { "O+ (Positive)" })
            drawDetailRow("वैधता सत्र", data.validityYear.ifBlank { "2026 - 2027" })

            // 8. Verification QR Code
            val qrSize = 130
            val qrX = (CARD_WIDTH - qrSize - 40).toFloat()
            val qrY = (CARD_HEIGHT - qrSize - 50).toFloat()

            try {
                val qrWriter = QRCodeWriter()
                val qrContent = "SHRIBALAJIKRIPADHAM-SEVADAR|ID:${data.sevadarId}|NAME:${data.name}|ROLE:${data.role}|PHONE:${data.phoneNumber}"
                val bitMatrix = qrWriter.encode(qrContent, BarcodeFormat.QR_CODE, qrSize, qrSize)
                val qrBitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.RGB_565)
                for (x in 0 until qrSize) {
                    for (y in 0 until qrSize) {
                        qrBitmap.setPixel(x, y, if (bitMatrix.get(x, y)) primaryArgb else android.graphics.Color.WHITE)
                    }
                }
                canvas.drawBitmap(qrBitmap, qrX, qrY, null)
            } catch (e: Exception) {}

            // Digital Seal & Signatory on left
            val sealPaint = Paint().apply {
                color = primaryArgb
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("डिजिटल रूप से प्रमाणित", 60f, CARD_HEIGHT - 120f, sealPaint)
            val signPaint = Paint().apply {
                color = secondaryArgb
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("✓ आश्रम सेवा समिति", 60f, CARD_HEIGHT - 95f, signPaint)
            val authPaint = Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 13f
                isAntiAlias = true
            }
            canvas.drawText("अधिकृत हस्ताक्षरकर्ता", 60f, CARD_HEIGHT - 70f, authPaint)

        } else {
            // ==========================================
            // BACK SIDE RENDERING
            // ==========================================
            val bgPaint = Paint().apply {
                color = bgArgb
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bgPaint)

            val borderPaint = Paint().apply {
                color = primaryArgb
                style = Paint.Style.STROKE
                strokeWidth = 8f
            }
            canvas.drawRoundRect(RectF(14f, 14f, CARD_WIDTH - 14f, CARD_HEIGHT - 14f), 20f, 20f, borderPaint)

            // Header Ribbon
            val headerPaint = Paint().apply {
                color = primaryArgb
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(20f, 20f, CARD_WIDTH - 20f, 130f), 14f, 14f, headerPaint)

            val titlePaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("📜 सेवादार नियम एवं निर्देश", (CARD_WIDTH / 2).toFloat(), 65f, titlePaint)

            val subTitlePaint = Paint().apply {
                color = secondaryArgb
                textSize = 16f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("श्री बालाजी कृपा धाम (डूँगरा जाट)", (CARD_WIDTH / 2).toFloat(), 105f, subTitlePaint)

            // Instructions Text
            val rules = listOf(
                "1. यह कार्ड आश्रम परिसर में गले में पहनना अनिवार्य है।",
                "2. यह कार्ड अहस्तांतरणीय (Non-Transferable) है।",
                "3. पीड़ितों व भक्तों से नम्रता व सम्मानपूर्वक व्यवहार करें।",
                "4. किसी भी भक्त से कोई दान, दक्षिणा या भेंट न लें।",
                "5. आश्रम के सभी नियम व अनुशासन का कड़ाई से पालन करें।",
                "6. कार्ड खोने पर तुरंत मुख्य प्रबंधक को सूचित करें।"
            )

            var ruleY = 180f
            val rulePaint = Paint().apply {
                color = textArgb
                textSize = 17.5f
                isAntiAlias = true
            }

            for (rule in rules) {
                canvas.drawText(rule, 40f, ruleY, rulePaint)
                ruleY += 48f
            }

            // Emergency Contact & Address Card
            val boxPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(40f, ruleY + 20f, CARD_WIDTH - 40f, ruleY + 220f), 14f, 14f, boxPaint)

            val boxBorder = Paint().apply {
                color = primaryArgb
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRoundRect(RectF(40f, ruleY + 20f, CARD_WIDTH - 40f, ruleY + 220f), 14f, 14f, boxBorder)

            val boxTextPaint = Paint().apply {
                color = primaryArgb
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("📍 आश्रम मुख्य पता व आपातकालीन संपर्क:", 60f, ruleY + 60f, boxTextPaint)

            val addressPaint = Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 15f
                isAntiAlias = true
            }
            canvas.drawText(data.ashramAddress, 60f, ruleY + 100f, addressPaint)
            canvas.drawText("हेल्पलाइन: ${data.emergencyPhone}", 60f, ruleY + 140f, addressPaint)
            canvas.drawText("ईमेल / वेबसाइट: shribalajikripadham.org", 60f, ruleY + 180f, addressPaint)

            // Bottom Footer
            val footerPaint = Paint().apply {
                color = primaryArgb
                style = Paint.Style.FILL
            }
            canvas.drawRect(20f, CARD_HEIGHT - 80f, CARD_WIDTH - 20f, CARD_HEIGHT - 20f, footerPaint)

            val footerTextPaint = Paint().apply {
                color = secondaryArgb
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("🚩 जय श्री राम | जय श्री बालाजी महाराज 🚩", (CARD_WIDTH / 2).toFloat(), CARD_HEIGHT - 44f, footerTextPaint)
        }

        return bitmap
    }

    /**
     * Encodes a genuine Adobe Photoshop (.PSD) binary document.
     * Compatible with Adobe Photoshop, Photopea, GIMP, and Paint.NET.
     */
    suspend fun generatePhotoshopPsd(
        context: Context,
        data: SevadarIdCardData,
        template: IdCardTemplate
    ): File = withContext(Dispatchers.IO) {
        val frontBitmap = generateIdCardBitmap(context, data, template, isBackSide = false)
        val width = frontBitmap.width
        val height = frontBitmap.height

        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // -------------------------------------------------------------
        // SECTION 1: PSD HEADER (26 Bytes)
        // -------------------------------------------------------------
        dos.writeBytes("8BPS") // Signature
        dos.writeShort(1)      // Version (1 = PSD)
        dos.write(ByteArray(6)) // 6 Reserved bytes
        dos.writeShort(3)      // Number of Channels (3 = RGB)
        dos.writeInt(height)   // Height in pixels (1050)
        dos.writeInt(width)    // Width in pixels (600)
        dos.writeShort(8)      // Bits per channel (8-bit)
        dos.writeShort(3)      // Color Mode (3 = RGB)

        // -------------------------------------------------------------
        // SECTION 2: COLOR MODE DATA (4 Bytes)
        // -------------------------------------------------------------
        dos.writeInt(0) // Length 0 for RGB mode

        // -------------------------------------------------------------
        // SECTION 3: IMAGE RESOURCES (Resolution Info 300 DPI)
        // -------------------------------------------------------------
        val resBaos = ByteArrayOutputStream()
        val resDos = DataOutputStream(resBaos)
        // 8BIM block for ResolutionInfo (ID = 0x03ED)
        resDos.writeBytes("8BIM")
        resDos.writeShort(0x03ED)
        resDos.writeByte(0) // Empty Pascal name
        resDos.writeByte(0) // Padding
        resDos.writeInt(16) // Resource Data Length (16 bytes)
        // 300 DPI = 300 * 65536 = 19660800 in Fixed-point 16.16
        resDos.writeInt(300 shl 16) // Horizontal resolution
        resDos.writeShort(1)        // Unit (1 = pixels/inch)
        resDos.writeShort(1)        // Display unit
        resDos.writeInt(300 shl 16) // Vertical resolution
        resDos.writeShort(1)        // Unit (1 = pixels/inch)
        resDos.writeShort(1)        // Display unit

        val resBytes = resBaos.toByteArray()
        dos.writeInt(resBytes.size)
        dos.write(resBytes)

        // -------------------------------------------------------------
        // SECTION 4: LAYER AND MASK INFORMATION (Empty or minimal)
        // -------------------------------------------------------------
        dos.writeInt(0) // Length 0 (merged composite primary)

        // -------------------------------------------------------------
        // SECTION 5: IMAGE DATA (Planar Uncompressed RGB)
        // -------------------------------------------------------------
        dos.writeShort(0) // Compression = 0 (Raw planar bytes)

        val pixels = IntArray(width * height)
        frontBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val redChannel = ByteArray(width * height)
        val greenChannel = ByteArray(width * height)
        val blueChannel = ByteArray(width * height)

        for (i in pixels.indices) {
            val color = pixels[i]
            redChannel[i] = ((color shr 16) and 0xFF).toByte()
            greenChannel[i] = ((color shr 8) and 0xFF).toByte()
            blueChannel[i] = (color and 0xFF).toByte()
        }

        dos.write(redChannel)
        dos.write(greenChannel)
        dos.write(blueChannel)
        dos.flush()

        // Save to Downloads folder
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val cleanName = data.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(20)
        val fileName = "SBKD_IDCard_${cleanName}_${template.id}.psd"
        val targetFile = File(downloadsDir, fileName)

        FileOutputStream(targetFile).use { fos ->
            fos.write(baos.toByteArray())
        }

        targetFile
    }

    /**
     * Generates a 300 DPI high-definition printable PVC card PDF (Front and Back Side)
     */
    suspend fun generatePrintablePdf(
        context: Context,
        data: SevadarIdCardData,
        template: IdCardTemplate
    ): File = withContext(Dispatchers.IO) {
        val pdfDoc = PdfDocument()

        // Page 1: Front Side
        val frontBitmap = generateIdCardBitmap(context, data, template, isBackSide = false)
        val pageInfo1 = PdfDocument.PageInfo.Builder(CARD_WIDTH, CARD_HEIGHT, 1).create()
        val page1 = pdfDoc.startPage(pageInfo1)
        page1.canvas.drawBitmap(frontBitmap, 0f, 0f, null)
        pdfDoc.finishPage(page1)

        // Page 2: Back Side
        val backBitmap = generateIdCardBitmap(context, data, template, isBackSide = true)
        val pageInfo2 = PdfDocument.PageInfo.Builder(CARD_WIDTH, CARD_HEIGHT, 2).create()
        val page2 = pdfDoc.startPage(pageInfo2)
        page2.canvas.drawBitmap(backBitmap, 0f, 0f, null)
        pdfDoc.finishPage(page2)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val cleanName = data.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(20)
        val fileName = "SBKD_IDCard_${cleanName}_${template.id}_Printable.pdf"
        val targetFile = File(downloadsDir, fileName)

        FileOutputStream(targetFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()

        targetFile
    }

    /**
     * Saves Front and Back side HD PNG images to Downloads folder
     */
    suspend fun saveIdCardPng(
        context: Context,
        data: SevadarIdCardData,
        template: IdCardTemplate
    ): Pair<File, File> = withContext(Dispatchers.IO) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val cleanName = data.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(20)

        // Front
        val frontBitmap = generateIdCardBitmap(context, data, template, isBackSide = false)
        val frontFile = File(downloadsDir, "SBKD_IDCard_${cleanName}_Front.png")
        FileOutputStream(frontFile).use { fos ->
            frontBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        // Back
        val backBitmap = generateIdCardBitmap(context, data, template, isBackSide = true)
        val backFile = File(downloadsDir, "SBKD_IDCard_${cleanName}_Back.png")
        FileOutputStream(backFile).use { fos ->
            backBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        Pair(frontFile, backFile)
    }
}
