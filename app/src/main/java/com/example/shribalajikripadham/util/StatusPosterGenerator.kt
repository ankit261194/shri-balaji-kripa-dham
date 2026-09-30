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
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StatusPosterGenerator {

    /**
     * Day-wise Divine Color Themes (7-Day Dynamic Theme System).
     * Prevents visual monotony so devotees see a vibrant, unique sacred poster every single day.
     */
    data class DayTheme(
        val dayNameHindi: String,
        val primaryHex: Int,
        val secondaryHex: Int,
        val accentGold: Int,
        val cardBgHex: Int,
        val quoteBgHex: Int,
        val devoteeBgHex: Int,
        val daySubtitle: String
    )

    fun getDayTheme(calendar: Calendar = Calendar.getInstance()): DayTheme {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> DayTheme(
                dayNameHindi = "रविवार",
                primaryHex = 0xFF5C001E.toInt(), // Sacred Royal Crimson / Maroon
                secondaryHex = 0xFFFF6F00.toInt(),
                accentGold = 0xFFFFD54F.toInt(),
                cardBgHex = 0xFF260E16.toInt(),
                quoteBgHex = 0xFF1F0A11.toInt(),
                devoteeBgHex = 0xFF2E101B.toInt(),
                daySubtitle = "रविवार महा-दरबार • सूर्य तेज व बालाजी महाराज की विशेष अनुकंपा"
            )
            Calendar.MONDAY -> DayTheme(
                dayNameHindi = "सोमवार",
                primaryHex = 0xFF1A237E.toInt(), // Nilkanth Mahadev Royal Indigo
                secondaryHex = 0xFF00B0FF.toInt(),
                accentGold = 0xFFFFE082.toInt(),
                cardBgHex = 0xFF0D1442.toInt(),
                quoteBgHex = 0xFF080D2B.toInt(),
                devoteeBgHex = 0xFF131B54.toInt(),
                daySubtitle = "सोमवार पावन बेला • ॐ नमः शिवाय व संकटमोचन कृपा"
            )
            Calendar.TUESDAY -> DayTheme(
                dayNameHindi = "मंगलवार",
                primaryHex = 0xFFB71C1C.toInt(), // Sacred Sindoor Red
                secondaryHex = 0xFFFF3D00.toInt(),
                accentGold = 0xFFFFD700.toInt(),
                cardBgHex = 0xFF400606.toInt(),
                quoteBgHex = 0xFF2B0303.toInt(),
                devoteeBgHex = 0xFF520B0B.toInt(),
                daySubtitle = "मंगलवार महा-पर्व • पवनपुत्र श्री हनुमान जी महाराज की विशेष कृपा"
            )
            Calendar.WEDNESDAY -> DayTheme(
                dayNameHindi = "बुधवार",
                primaryHex = 0xFF004D40.toInt(), // Divine Emerald / Peacock Teal
                secondaryHex = 0xFF00E676.toInt(),
                accentGold = 0xFFFFD54F.toInt(),
                cardBgHex = 0xFF00241E.toInt(),
                quoteBgHex = 0xFF001713.toInt(),
                devoteeBgHex = 0xFF013028.toInt(),
                daySubtitle = "बुधवार पावन दिवस • श्री विघ्नहर्ता व बालाजी रक्षा कवच"
            )
            Calendar.THURSDAY -> DayTheme(
                dayNameHindi = "गुरुवार",
                primaryHex = 0xFF4A148C.toInt(), // Imperial Guru Kripa Deep Purple
                secondaryHex = 0xFFFFC107.toInt(),
                accentGold = 0xFFFFE082.toInt(),
                cardBgHex = 0xFF240647.toInt(),
                quoteBgHex = 0xFF17032E.toInt(),
                devoteeBgHex = 0xFF2E0959.toInt(),
                daySubtitle = "गुरुवार दिव्य दिवस • परम पूज्य गुरुदेव कृपा व आत्मज्ञान"
            )
            Calendar.FRIDAY -> DayTheme(
                dayNameHindi = "शुक्रवार",
                primaryHex = 0xFF880E4F.toInt(), // Sacred Ruby Rose / Magenta
                secondaryHex = 0xFFFF4081.toInt(),
                accentGold = 0xFFFFD54F.toInt(),
                cardBgHex = 0xFF3B0421.toInt(),
                quoteBgHex = 0xFF240214.toInt(),
                devoteeBgHex = 0xFF47082A.toInt(),
                daySubtitle = "शुक्रवार विशेष • सुख, समृद्धि व भगवती-बालाजी का आशीर्वाद"
            )
            Calendar.SATURDAY -> DayTheme(
                dayNameHindi = "शनिवार",
                primaryHex = 0xFF0D1B2A.toInt(), // Midnight Cosmic Steel
                secondaryHex = 0xFFFF9100.toInt(),
                accentGold = 0xFFFFD54F.toInt(),
                cardBgHex = 0xFF142236.toInt(),
                quoteBgHex = 0xFF0B1420.toInt(),
                devoteeBgHex = 0xFF1B2E47.toInt(),
                daySubtitle = "शनिवार संकटमोचन दिवस • शनि दोष व सर्व बाधा निवारण"
            )
            else -> DayTheme(
                dayNameHindi = "शुभ दिवस",
                primaryHex = 0xFF5C001E.toInt(),
                secondaryHex = 0xFFFF8F00.toInt(),
                accentGold = 0xFFFFD54F.toInt(),
                cardBgHex = 0xFF260E16.toInt(),
                quoteBgHex = 0xFF1F0A11.toInt(),
                devoteeBgHex = 0xFF2E101B.toInt(),
                daySubtitle = "दैनिक दिव्य दर्शन व अमृत सुविचार"
            )
        }
    }

    /**
     * Renders a 9:16 (1080x1920) high-resolution WhatsApp Story & Status Poster.
     * Contains locked Balaji Maharaj, Guruji, Ashram Logo, Suvichar, scannable QR Code,
     * status touch helper callout, and optional devotee photo.
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

        // Resolve 7-Day Day-wise Dynamic Theme
        val todayTheme = getDayTheme()
        val effPrimaryHex = if (primaryHex == 0xFF5C001E.toInt()) todayTheme.primaryHex else primaryHex
        val effSecondaryHex = if (secondaryHex == 0xFFE65100.toInt()) todayTheme.secondaryHex else secondaryHex
        val effCardBgHex = todayTheme.cardBgHex
        val effQuoteBgHex = todayTheme.quoteBgHex
        val effDevoteeBgHex = todayTheme.devoteeBgHex
        val effAccentGold = todayTheme.accentGold

        // 1. Draw Sacred Gradient Background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    effPrimaryHex,
                    0xFF120307.toInt(),
                    0xFF1C0812.toInt(),
                    0xFF0A0205.toInt()
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
            color = effAccentGold
        }
        canvas.drawRoundRect(20f, 20f, width - 20f, height - 20f, 40f, 40f, borderPaint)

        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.WHITE
            alpha = 150
        }
        canvas.drawRoundRect(35f, 35f, width - 35f, height - 35f, 30f, 30f, innerBorderPaint)

        // 3. Top Sacred Header (Logo + Temple Name)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = effAccentGold
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("॥ ॐ श्री हनुमते नमः ॥", width / 2f, 95f, textPaint)

        textPaint.apply {
            color = Color.WHITE
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("श्री बालाजी कृपा धाम", width / 2f, 160f, textPaint)

        textPaint.apply {
            color = Color.parseColor("#FFE082")
            textSize = 27f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("ग्राम डूँगरा जाट, तहसील: अनूपशहर, जिला: बुलन्दशहर (उ.प्र.)", width / 2f, 210f, textPaint)

        // Divider Line
        val divPaint = Paint().apply {
            color = effAccentGold
            strokeWidth = 3f
            alpha = 180
        }
        canvas.drawLine(150f, 238f, width - 150f, 238f, divPaint)

        // 4. Ashram Logo Watermark / Emblem
        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
        if (logoBitmap != null) {
            val logoSize = 120
            val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, logoSize, logoSize, true)
            canvas.drawBitmap(scaledLogo, 55f, 75f, null)
            canvas.drawBitmap(scaledLogo, width - 55f - logoSize, 75f, null)
        }

        // 5. Central Divine Arch / Frame (Balaji Maharaj & Guruji Image)
        val cardRect = RectF(70f, 260f, width - 70f, 960f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = effCardBgHex
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)

        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = effAccentGold
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBorder)

        // Draw Balaji Maharaj Divine Portrait
        if (logoBitmap != null) {
            val balajiSize = 440
            val scaledBalaji = Bitmap.createScaledBitmap(logoBitmap, balajiSize, balajiSize, true)
            val balajiLeft = (width - balajiSize) / 2f
            canvas.drawBitmap(scaledBalaji, balajiLeft, 285f, null)

            // Circular frame around Balaji
            val circleBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 8f
                color = effAccentGold
            }
            canvas.drawCircle(width / 2f, 285f + balajiSize / 2f, balajiSize / 2f + 4f, circleBorder)
        }

        // Subtitle below portrait
        textPaint.apply {
            color = Color.parseColor("#FFF8E1")
            textSize = 33f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("🚩 परम पूज्य गुरुदेव तेजवीर सिंह जी के पावन सानिध्य में 🚩", width / 2f, 785f, textPaint)

        textPaint.apply {
            color = effAccentGold
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("hi", "IN")).format(Date())
        canvas.drawText("॥ ${todayTheme.daySubtitle} ॥", width / 2f, 840f, textPaint)

        textPaint.apply {
            color = Color.parseColor("#B2DFDB")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("प्रत्येक रविवार प्रातः 8:30 बजे से 100% निःशुल्क दिव्य दरबार • $dateStr", width / 2f, 895f, textPaint)

        // 6. Sacred Suvichar Section
        val quoteBoxRect = RectF(70f, 985f, width - 70f, 1375f)
        val quoteBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = effQuoteBgHex
        }
        canvas.drawRoundRect(quoteBoxRect, 28f, 28f, quoteBoxPaint)

        val quoteBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = effAccentGold
            alpha = 180
        }
        canvas.drawRoundRect(quoteBoxRect, 28f, 28f, quoteBorder)

        // Suvichar text drawing with multi-line wrap
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF9C4")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        drawMultilineText(
            canvas,
            "“ " + suvichar.quote + " ”",
            width / 2f,
            1055f,
            quotePaint,
            width - 200
        )

        val meaningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        drawMultilineText(
            canvas,
            "भावार्थ: " + suvichar.meaning,
            width / 2f,
            1225f,
            meaningPaint,
            width - 220
        )

        val sourcePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFB74D")
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("— ${suvichar.source}", width / 2f, 1345f, sourcePaint)

        // 7. Devotee Personalized Badge (or Ashram Blessing Badge if devoteePhoto is null)
        val devoteeBoxRect = RectF(70f, 1395f, width - 70f, 1695f)
        val devoteeBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = effDevoteeBgHex
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
            val pSize = 220
            val scaledPhoto = Bitmap.createScaledBitmap(safePhoto, pSize, pSize, true)
            val circularPhoto = getCircularBitmap(scaledPhoto)

            canvas.drawBitmap(circularPhoto, 110f, 1435f, null)

            // Circle border around devotee
            val pBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 6f
                color = effAccentGold
            }
            canvas.drawCircle(110f + pSize / 2f, 1435f + pSize / 2f, pSize / 2f + 3f, pBorder)

            // Devotee Details on the right
            val devNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 40f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            val displayName = devoteeName.ifBlank { "श्री बालाजी भक्त" }
            canvas.drawText(displayName, 370f, 1515f, devNamePaint)

            val devCityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#81C784")
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.LEFT
            }
            val displayCity = if (devoteeCity.isNotBlank()) "निवासी: $devoteeCity" else "॥ कृपाकांक्षी ॥"
            canvas.drawText(displayCity, 370f, 1575f, devCityPaint)

            val prayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFE082")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("॥ जय श्री बालाजी महाराज • जय श्री राम ॥", 370f, 1635f, prayPaint)

        } else {
            // Default Ashram Devotional Greeting
            textPaint.apply {
                color = effAccentGold
                textSize = 44f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("🚩 ॥ जय श्री राम • जय बालाजी महाराज ॥ 🚩", width / 2f, 1495f, textPaint)

            textPaint.apply {
                color = Color.WHITE
                textSize = 31f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("सर्वस्व श्री रामभक्त वीर हनुमान जी महाराज के पावन चरणों में समर्पित।", width / 2f, 1565f, textPaint)

            textPaint.apply {
                color = Color.parseColor("#A5D6A7")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("100% निःशुल्क भूत-प्रेत व आध्यात्मिक कष्ट निवारण दरबार", width / 2f, 1635f, textPaint)
        }

        // 8. Bottom Scannable QR Code & WhatsApp Status Touch Helper Footer
        val footerBoxRect = RectF(70f, 1715f, width - 70f, 1895f)
        val footerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#120308")
            alpha = 240
        }
        canvas.drawRoundRect(footerBoxRect, 24f, 24f, footerBg)

        val footerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = effAccentGold
            alpha = 200
        }
        canvas.drawRoundRect(footerBoxRect, 24f, 24f, footerBorder)

        // Render Golden Scannable QR Code for https://shribalajikripadham.online/app
        val qrBitmap = QrCodeGenerator.generateQrBitmap("https://shribalajikripadham.online/app", 155)
        if (qrBitmap != null) {
            val qrBackRect = RectF(90f, 1728f, 245f, 1883f)
            val qrBackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawRoundRect(qrBackRect, 16f, 16f, qrBackPaint)
            canvas.drawBitmap(qrBitmap, 90f, 1728f, null)
        }

        // Footer Text and Callout on the right of QR code (from X = 265f)
        val appLinkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = effAccentGold
            textSize = 29f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("📲 आधिकारिक ऐप: shribalajikripadham.online/app", 265f, 1768f, appLinkPaint)

        val touchLinkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("👆 नीचे दिए गए स्टेटस लिंक को छूकर ऐप डाउनलोड करें", 265f, 1810f, touchLinkPaint)

        val trustSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80CBC4")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("🚩 100% निःशुल्क कष्ट निवारण • जन-जन की आस्था", 265f, 1848f, trustSubPaint)

        val copyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#90A4AE")
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("© श्री बालाजी कृपा धाम (डूँगरा जाट) • क्यूआर स्कैन करें या लिंक छुएं", 265f, 1880f, copyPaint)

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
     * Saves poster bitmap to cache and launches WhatsApp share intent.
     * Includes clickable download link in status caption so status viewers can tap and download directly.
     */
    fun shareToWhatsApp(
        context: Context,
        posterBitmap: Bitmap,
        caption: String = "🚩 *श्री बालाजी कृपा धाम (डूँगरा जाट)* 🚩\n\nआज का दिव्य दर्शन व अमृत सुविचार।\n\n👉 आधिकारिक ऐप डाउनलोड करें (यहाँ टच करें):\n🌐 https://shribalajikripadham.online/app"
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
