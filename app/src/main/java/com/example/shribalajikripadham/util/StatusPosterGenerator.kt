package com.example.shribalajikripadham.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.shribalajikripadham.R
import com.example.shribalajikripadham.data.model.DailySuvichar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatusPosterGenerator {

    /**
     * Renders a 9:16 (1080x1920) high-resolution WhatsApp Story & Status Poster.
     * Contains locked Balaji Maharaj, Guruji, Ashram Logo, Suvichar, and optional devotee photo.
     */
    fun generateBhaktiPoster(
        context: Context,
        devoteePhoto: Bitmap?,
        devoteeName: String,
        devoteeCity: String,
        suvichar: DailySuvichar,
        primaryHex: Int = 0xFF5C001E.toInt(),
        secondaryHex: Int = 0xFFE65100.toInt()
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Sacred Gradient Background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    primaryHex,
                    0xFF1A0A0E.toInt(),
                    0xFF2D0F18.toInt(),
                    0xFF120307.toInt()
                ),
                floatArrayOf(0f, 0.35f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Decorative Outer Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 14f
            color = Color.parseColor("#FFD54F")
        }
        canvas.drawRoundRect(20f, 20f, width - 20f, height - 20f, 40f, 40f, borderPaint)

        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#FFFFFF").apply { }
            alpha = 150
        }
        canvas.drawRoundRect(35f, 35f, width - 35f, height - 35f, 30f, 30f, innerBorderPaint)

        // 3. Top Sacred Header (Logo + Temple Name)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#FFD54F")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("॥ ॐ श्री हनुमते नमः ॥", width / 2f, 105f, textPaint)

        textPaint.apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("श्री बालाजी कृपा धाम", width / 2f, 175f, textPaint)

        textPaint.apply {
            color = Color.parseColor("#FFE082")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("ग्राम डूँगरा जाट, तहसील: अनूपशहर, जिला: बुलन्दशहर (उ.प्र.)", width / 2f, 225f, textPaint)

        // Divider Line
        val divPaint = Paint().apply {
            color = Color.parseColor("#FFD54F")
            strokeWidth = 3f
            alpha = 180
        }
        canvas.drawLine(150f, 255f, width - 150f, 255f, divPaint)

        // 4. Ashram Logo Watermark / Emblem
        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
        if (logoBitmap != null) {
            val logoSize = 130
            val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, logoSize, logoSize, true)
            canvas.drawBitmap(scaledLogo, 60f, 80f, null)
            canvas.drawBitmap(scaledLogo, width - 60f - logoSize, 80f, null)
        }

        // 5. Central Divine Arch / Frame (Balaji Maharaj & Guruji Image)
        val cardRect = RectF(70f, 280f, width - 70f, 1020f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#260E16")
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)

        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.parseColor("#FFB300")
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBorder)

        // Draw Balaji Maharaj Divine Portrait
        if (logoBitmap != null) {
            val balajiSize = 480
            val scaledBalaji = Bitmap.createScaledBitmap(logoBitmap, balajiSize, balajiSize, true)
            val balajiLeft = (width - balajiSize) / 2f
            canvas.drawBitmap(scaledBalaji, balajiLeft, 310f, null)

            // Circular frame around Balaji
            val circleBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 8f
                color = Color.parseColor("#FFD54F")
            }
            canvas.drawCircle(width / 2f, 310f + balajiSize / 2f, balajiSize / 2f + 4f, circleBorder)
        }

        // Subtitle below portrait
        textPaint.apply {
            color = Color.parseColor("#FFF8E1")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("🚩 परम पूज्य गुरुदेव तेजवीर सिंह जी के पावन सानिध्य में 🚩", width / 2f, 850f, textPaint)

        textPaint.apply {
            color = Color.parseColor("#FFCA28")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("hi", "IN")).format(Date())
        canvas.drawText("॥ दैनिक दिव्य दर्शन व अमृत सुविचार • $dateStr ॥", width / 2f, 900f, textPaint)

        textPaint.apply {
            color = Color.parseColor("#80CBC4")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("प्रत्येक रविवार प्रातः 8:30 बजे से दिव्य दरबार", width / 2f, 950f, textPaint)

        // 6. Sacred Suvichar Section
        val quoteBoxRect = RectF(70f, 1050f, width - 70f, 1420f)
        val quoteBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F0A11")
        }
        canvas.drawRoundRect(quoteBoxRect, 28f, 28f, quoteBoxPaint)

        val quoteBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.parseColor("#FFD54F")
            alpha = 180
        }
        canvas.drawRoundRect(quoteBoxRect, 28f, 28f, quoteBorder)

        // Suvichar text drawing with multi-line wrap
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF9C4")
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        drawMultilineText(
            canvas,
            "“ " + suvichar.quote + " ”",
            width / 2f,
            1130f,
            quotePaint,
            width - 200
        )

        val meaningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        drawMultilineText(
            canvas,
            "भावार्थ: " + suvichar.meaning,
            width / 2f,
            1270f,
            meaningPaint,
            width - 220
        )

        val sourcePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFB74D")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("— ${suvichar.source}", width / 2f, 1380f, sourcePaint)

        // 7. Devotee Personalized Badge (or Ashram Blessing Badge if devoteePhoto is null)
        val devoteeBoxRect = RectF(70f, 1450f, width - 70f, 1780f)
        val devoteeBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E101B")
        }
        canvas.drawRoundRect(devoteeBoxRect, 24f, 24f, devoteeBoxPaint)

        val devoteeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.parseColor("#4CAF50")
        }
        canvas.drawRoundRect(devoteeBoxRect, 24f, 24f, devoteeBorder)

        if (devoteePhoto != null) {
            // Draw Circular Devotee Photo
            val safePhoto = DevoteePhotoHelper.toSoftwareBitmap(devoteePhoto)
            val pSize = 240
            val scaledPhoto = Bitmap.createScaledBitmap(safePhoto, pSize, pSize, true)
            val circularPhoto = getCircularBitmap(scaledPhoto)

            canvas.drawBitmap(circularPhoto, 120f, 1490f, null)

            // Circle border around devotee
            val pBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 6f
                color = Color.parseColor("#FFD54F")
            }
            canvas.drawCircle(120f + pSize / 2f, 1490f + pSize / 2f, pSize / 2f + 3f, pBorder)

            // Devotee Details on the right
            val devNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            val displayName = devoteeName.ifBlank { "श्री बालाजी भक्त" }
            canvas.drawText(displayName, 390f, 1570f, devNamePaint)

            val devCityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#81C784")
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.LEFT
            }
            val displayCity = if (devoteeCity.isNotBlank()) "निवासी: $devoteeCity" else "॥ कृपाकांक्षी ॥"
            canvas.drawText(displayCity, 390f, 1630f, devCityPaint)

            val prayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFE082")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("॥ जय श्री बालाजी महाराज ॥", 390f, 1690f, prayPaint)

        } else {
            // Default Ashram Devotional Greeting
            textPaint.apply {
                color = Color.parseColor("#FFD54F")
                textSize = 46f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("🚩 ॥ जय श्री राम • जय बालाजी महाराज ॥ 🚩", width / 2f, 1570f, textPaint)

            textPaint.apply {
                color = Color.WHITE
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("सर्वस्व श्री रामभक्त वीर हनुमान जी महाराज के पावन चरणों में समर्पित।", width / 2f, 1640f, textPaint)

            textPaint.apply {
                color = Color.parseColor("#A5D6A7")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("100% निःशुल्क भूत-प्रेत व आध्यात्मिक कष्ट निवारण दरबार", width / 2f, 1710f, textPaint)
        }

        // 8. Bottom App & Website Branding Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD54F")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("📲 आधिकारिक ऐप डाउनलोड करें: shribalajikripadham.online/download.php", width / 2f, 1845f, footerPaint)

        footerPaint.apply {
            color = Color.parseColor("#B0BEC5")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("© 2026 श्री बालाजी कृपा धाम। सर्वाधिकार सुरक्षित।", width / 2f, 1885f, footerPaint)

        return bitmap
    }

    /**
     * Helper to draw text with automatic line wrapping
     */
    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        maxWidth: Int
    ) {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width < maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())

        var currentY = y
        val lineHeight = paint.textSize * 1.35f
        for (line in lines) {
            canvas.drawText(line, x, currentY, paint)
            currentY += lineHeight
        }
    }

    /**
     * Helper to create a circular bitmap
     */
    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, bitmap.width / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    /**
     * Saves poster bitmap to cache and launches WhatsApp share intent
     */
    fun shareToWhatsApp(
        context: Context,
        posterBitmap: Bitmap,
        caption: String = "🚩 *श्री बालाजी कृपा धाम (डूँगरा जाट)* 🚩\n\nआज का दिव्य दर्शन व अमृत सुविचार। धाम का आधिकारिक ऐप डाउनलोड करें:\n🌐 https://shribalajikripadham.online/download.php"
    ) {
        try {
            val cacheDir = File(context.cacheDir, "shared_statuses").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(cacheDir, "bhakti_status_$timeStamp.jpg")

            FileOutputStream(file).use { out ->
                posterBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                out.flush()
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp")
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // If direct WhatsApp package not found, open general chooser
                val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "व्हाट्सएप या अन्य ऐप पर स्टेटस लगाएं")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }

        } catch (e: Exception) {
            Toast.makeText(context, "शेयर करने में समस्या आई: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
