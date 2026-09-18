package com.example.shribalajikripadham.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object TokenCardExporter {

    private const val CARD_WIDTH = 1080
    private const val CARD_HEIGHT = 1520

    /**
     * Renders a high-resolution, print-quality Royal Golden & Saffron Token Card Bitmap.
     */
    fun renderRoyalTokenCardBitmap(
        context: Context,
        token: Token,
        settings: AshramSettings
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Radiant Parchment Background
        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#FFFDF9"),
                    Color.parseColor("#FFF9EE"),
                    Color.parseColor("#FFF3E0")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bgPaint)

        // 2. Outer Royal Golden Gradient Border (16px)
        val goldBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 16f
            shader = LinearGradient(
                0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#D4AF37"), // Metallic Gold
                    Color.parseColor("#FFD700"), // Brilliant Gold
                    Color.parseColor("#FF8C00"), // Dark Orange
                    Color.parseColor("#D4AF37")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(16f, 16f, CARD_WIDTH - 16f, CARD_HEIGHT - 16f), 32f, 32f, goldBorderPaint)

        // 3. Inner Sacred Maroon Inset Border (4px)
        val innerBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.parseColor("#800000") // Sacred Maroon
        }
        canvas.drawRoundRect(RectF(32f, 32f, CARD_WIDTH - 32f, CARD_HEIGHT - 32f), 24f, 24f, innerBorderPaint)

        // 4. Sacred Header Banner Box
        val headerBannerPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 40f, 0f, 260f,
                intArrayOf(Color.parseColor("#800000"), Color.parseColor("#5A0000")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        val headerRect = RectF(48f, 48f, CARD_WIDTH - 48f, 260f)
        canvas.drawRoundRect(headerRect, 20f, 20f, headerBannerPaint)

        // Golden outline for header banner
        val bannerOutline = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#FFD700")
        }
        canvas.drawRoundRect(headerRect, 20f, 20f, bannerOutline)

        // Header Sacred Inscription
        val mantraPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFD700")
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🚩 ॥ श्री हनुमते नमः ॥ 🚩", (CARD_WIDTH / 2).toFloat(), 95f, mantraPaint)

        // Official Sanstha Title
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }
        canvas.drawText(settings.ashramName.ifEmpty { "श्री बालाजी कृपा धाम, डूँगरा जाट" }, (CARD_WIDTH / 2).toFloat(), 155f, titlePaint)

        // Address & Sanstha Location
        val addressPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFE082")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(settings.address.ifEmpty { "ग्राम डूँगरा जाट, बुलन्दशहर (उत्तर प्रदेश)" }, (CARD_WIDTH / 2).toFloat(), 195f, addressPaint)

        // Darbar Frequency Notice
        val frequencyPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("प्रत्येक रविवार पावन दिव्य दरबार दर्शन पास", (CARD_WIDTH / 2).toFloat(), 235f, frequencyPaint)

        // 5. Guruji Sub-Header Bar
        val gurujiBarPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFF3E0")
            style = Paint.Style.FILL
        }
        val gurujiRect = RectF(60f, 280f, CARD_WIDTH - 60f, 350f)
        canvas.drawRoundRect(gurujiRect, 14f, 14f, gurujiBarPaint)

        val gurujiStroke = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#FFA000")
        }
        canvas.drawRoundRect(gurujiRect, 14f, 14f, gurujiStroke)

        val gurujiTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#800000")
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("👑 परम पूज्य गुरुजी तेजवीर सिंह जी के पावन सानिध्य में 👑", (CARD_WIDTH / 2).toFloat(), 325f, gurujiTextPaint)

        // 6. Central Royal Token Box
        val tokenBoxShader = LinearGradient(
            0f, 380f, 0f, 620f,
            intArrayOf(Color.parseColor("#FFF8E7"), Color.parseColor("#FFEACC")),
            null,
            Shader.TileMode.CLAMP
        )
        val tokenBoxPaint = Paint().apply {
            isAntiAlias = true
            shader = tokenBoxShader
        }
        val tokenBoxRect = RectF(80f, 375f, CARD_WIDTH - 80f, 625f)
        canvas.drawRoundRect(tokenBoxRect, 24f, 24f, tokenBoxPaint)

        val tokenBoxBorder = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.parseColor("#D4AF37")
        }
        canvas.drawRoundRect(tokenBoxRect, 24f, 24f, tokenBoxBorder)

        // Token Subtitle
        val tokenSubPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#795548")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("॥ आपका अधिकृत दर्शन टोकन क्रमांक ॥", (CARD_WIDTH / 2).toFloat(), 420f, tokenSubPaint)

        // Massive Token Number
        val hugeTokenPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#D84315") // Deep Saffron Red
            textSize = 120f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 3f, 3f, Color.parseColor("#FFE0B2"))
        }
        canvas.drawText("#${token.tokenNumber}", (CARD_WIDTH / 2).toFloat(), 535f, hugeTokenPaint)

        // Status Pill
        val statusPillPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#E8F5E9")
        }
        val statusRect = RectF((CARD_WIDTH / 2 - 160).toFloat(), 565f, (CARD_WIDTH / 2 + 160).toFloat(), 605f)
        canvas.drawRoundRect(statusRect, 20f, 20f, statusPillPaint)

        val statusTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1B5E20")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✅ कतार में सक्रिय (ACTIVE IN QUEUE)", (CARD_WIDTH / 2).toFloat(), 593f, statusTextPaint)

        // 7. Core Devotee Details Table
        val tableBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
        }
        val tableRect = RectF(60f, 650f, CARD_WIDTH - 60f, 1140f)
        canvas.drawRoundRect(tableRect, 18f, 18f, tableBgPaint)

        val tableBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#E0E0E0")
        }
        canvas.drawRoundRect(tableRect, 18f, 18f, tableBorderPaint)

        // Table Header
        val tableHeaderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#800000")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("📋 भक्त एवं टोकन विवरण (Devotee & Pass Data):", 90f, 700f, tableHeaderPaint)

        // Horizontal line under table header
        val linePaint = Paint().apply {
            isAntiAlias = true
            strokeWidth = 1.5f
            color = Color.parseColor("#EEEEEE")
        }
        canvas.drawLine(80f, 720f, CARD_WIDTH - 80f, 720f, linePaint)

        // Rows for Core Fields
        val sdfDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val genTimeFormatted = sdfDate.format(Date(token.createdAt))

        val rows = listOf(
            Pair("भक्त का नाम (Devotee Name):", token.patientName),
            Pair("मोबाइल नंबर (Mobile No):", token.phoneNumber),
            Pair("आगमन स्थान (Coming From):", token.city.ifEmpty { "डूँगरा जाट (स्थानीय)" }),
            Pair("टोकन जारी समय (Issued Time):", genTimeFormatted),
            Pair("दरबार तिथि (Darbar Date):", token.darbarDate.ifEmpty { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }),
            Pair("सुरक्षा ID (Device Lock):", token.deviceId.take(12).uppercase() + " (DRM-LOCKED)")
        )

        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#616161")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        val valuePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#212121")
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        var currentY = 770f
        for ((label, value) in rows) {
            canvas.drawText(label, 90f, currentY, labelPaint)
            val truncatedVal = if (value.length > 28) value.take(26) + "..." else value
            canvas.drawText(truncatedVal, CARD_WIDTH - 90f, currentY, valuePaint)

            canvas.drawLine(85f, currentY + 18f, CARD_WIDTH - 85f, currentY + 18f, linePaint)
            currentY += 65f
        }

        // 8. Free Treatment Manifesto Box
        val manifestoPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFF8E1")
        }
        val manifestoRect = RectF(60f, 1160f, CARD_WIDTH - 60f, 1280f)
        canvas.drawRoundRect(manifestoRect, 14f, 14f, manifestoPaint)

        val manifestoBorder = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#FFC107")
        }
        canvas.drawRoundRect(manifestoRect, 14f, 14f, manifestoBorder)

        val manifestoTitle = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#B71C1C")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("॥ निःशुल्क सेवा संकल्प (100% FREE SERVICE) ॥", (CARD_WIDTH / 2).toFloat(), 1205f, manifestoTitle)

        val manifestoBody = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#37474F")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "भूत-प्रेत व मानसिक समस्याओं का पूर्णतः निःशुल्क इलाज, केवल पूजा-पाठ द्वारा।",
            (CARD_WIDTH / 2).toFloat(),
            1245f,
            manifestoBody
        )

        // 9. Verification & Anti-Spoofing Barcode/Hash Footer
        val stampShader = LinearGradient(
            0f, 1300f, 0f, 1420f,
            intArrayOf(Color.parseColor("#FAFAFA"), Color.parseColor("#F5F5F5")),
            null,
            Shader.TileMode.CLAMP
        )
        val stampPaint = Paint().apply {
            isAntiAlias = true
            shader = stampShader
        }
        val stampRect = RectF(60f, 1300f, CARD_WIDTH - 60f, 1420f)
        canvas.drawRoundRect(stampRect, 12f, 12f, stampPaint)

        val hashPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#424242")
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val integritySig = com.example.shribalajikripadham.data.local.DatabaseHelper.generateTokenIntegrityHash(
            token.tokenNumber,
            token.patientName,
            token.darbarDate,
            token.createdAt
        )
        val verificationCode = "SBKD-PASS-TOKEN-${token.tokenNumber}-SHA256-${integritySig}"
        canvas.drawText(verificationCode, (CARD_WIDTH / 2).toFloat(), 1345f, hashPaint)

        val cautionPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#757575")
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("यह पास डिजिटल रूप से हस्ताक्षरित व सत्यापित है। क्रम संख्या पुकारे जाने पर पंक्ति में आएं।", (CARD_WIDTH / 2).toFloat(), 1385f, cautionPaint)

        // 10. Developer Credit at absolute bottom
        val creditPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#9E9E9E")
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Developer: Ankit Chaudhary (Anti Gravity) • Native Android Architecture", (CARD_WIDTH / 2).toFloat(), 1480f, creditPaint)

        return bitmap
    }

    /**
     * Exports the high-resolution Token Card bitmap to the app cache directory for instant sharing.
     */
    fun exportTokenToCache(
        context: Context,
        token: Token,
        settings: AshramSettings
    ): Uri? {
        return try {
            val bitmap = renderRoyalTokenCardBitmap(context, token, settings)
            val shareDir = File(context.cacheDir, "token_shares")
            if (!shareDir.exists()) {
                shareDir.mkdirs()
            }
            val shareFile = File(shareDir, "ShriBalaji_Token_${token.tokenNumber}.png")
            FileOutputStream(shareFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
            }
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                shareFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 1-Click Direct WhatsApp Share for Token Slip (Option 5)
     * Directly opens WhatsApp with the high-resolution token card image and formatted caption.
     * Gracefully falls back to WhatsApp Business or system share chooser if not found.
     */
    fun shareTokenViaWhatsApp(
        context: Context,
        token: Token,
        settings: AshramSettings,
        existingUri: Uri? = null
    ) {
        try {
            val uri = existingUri ?: exportTokenToCache(context, token, settings)
            val sdfDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val formattedTimestamp = sdfDate.format(Date(token.createdAt))

            val shareCaption = """
🚩 *श्री बालाजी कृपा धाम, डूँगरा जाट* 🚩
*परम पूज्य गुरुजी तेजवीर सिंह जी*
══════════════════════════
🎫 *रविवार दर्शन पावन पर्ची*
🔢 *टोकन क्रमांक:* #${token.tokenNumber}
👤 *भक्त का नाम:* ${token.patientName}
📍 *आगमन स्थान:* ${token.city}
📅 *दरबार तिथि:* ${token.darbarDate}
⏰ *समय:* $formattedTimestamp
══════════════════════════
🙏 *भूत-प्रेत व असाध्य मानसिक समस्याओं का पूर्णतः निःशुल्क इलाज, केवल पूजा-पाठ द्वारा।*
🌐 आश्रम लाइव दर्शन व टोकन सेवा हेतु ऐप डाउनलोड करें:
https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-release.apk
            """.trimIndent()

            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                if (uri != null) {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_TEXT, shareCaption)
                setPackage("com.whatsapp")
            }

            try {
                context.startActivity(whatsappIntent)
            } catch (e: Exception) {
                // Try WhatsApp Business
                try {
                    val businessIntent = Intent(Intent.ACTION_SEND).apply {
                        if (uri != null) {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            type = "text/plain"
                        }
                        putExtra(Intent.EXTRA_TEXT, shareCaption)
                        setPackage("com.whatsapp.w4b")
                    }
                    context.startActivity(businessIntent)
                } catch (e2: Exception) {
                    // Fallback to standard share chooser
                    val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                        if (uri != null) {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            type = "text/plain"
                        }
                        putExtra(Intent.EXTRA_SUBJECT, "श्री बालाजी कृपा धाम टोकन #${token.tokenNumber}")
                        putExtra(Intent.EXTRA_TEXT, shareCaption)
                    }
                    context.startActivity(Intent.createChooser(chooserIntent, "पावन पर्ची शेयर करें"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "शेयर करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Automatically saves the generated Token Card as a high-quality PNG image directly into
     * the user's Mobile Photo Gallery (Media Storage) without requiring manual download button clicks.
     */
    suspend fun saveTokenToGallery(
        context: Context,
        token: Token,
        settings: AshramSettings
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val bitmap = renderRoyalTokenCardBitmap(context, token, settings)
            val fileName = "ShriBalaji_Token_${token.tokenNumber}_${System.currentTimeMillis()}.png"

            var imageUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Scoped Storage: Android 10+ (API 29+)
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ShriBalajiKripaDham")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (imageUri != null) {
                    resolver.openOutputStream(imageUri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        outputStream.flush()
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
            } else {
                // Legacy Android 8 - 9 (API 26-28)
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val customDir = File(picturesDir, "ShriBalajiKripaDham")
                if (!customDir.exists()) {
                    customDir.mkdirs()
                }
                val destFile = File(customDir, fileName)
                FileOutputStream(destFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                }

                // Trigger media scanner so it instantly shows in Photo Gallery
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf("image/png")
                ) { _, uri ->
                    imageUri = uri
                }
                imageUri = Uri.fromFile(destFile)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "🖼️ टोकन कार्ड (#${token.tokenNumber}) आपकी फोटो गैलरी (Gallery) में स्वतः सुरक्षित हो गया है!",
                    Toast.LENGTH_LONG
                ).show()
            }

            imageUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
