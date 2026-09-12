package com.example.shribalajikripadham.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

object AdminCredentialBannerHelper {

    private const val BANNER_WIDTH = 1080
    private const val BANNER_HEIGHT = 1620

    /**
     * Formats a respectful, devotional, and structured text message for WhatsApp.
     */
    fun formatWhatsAppMessage(
        name: String,
        username: String,
        password: String,
        pin: String,
        phone: String,
        permissions: List<String>
    ): String {
        val permLines = if (permissions.isNotEmpty()) {
            permissions.joinToString("\n") { "  ✓ $it" }
        } else {
            "  ✓ सामान्य दर्शन व टोकन सेवा"
        }

        return """
🚩 *श्री बालाजी कृपा धाम, डूँगरा जाट* 🚩
॥ ॐ श्री हनुमते नमः ॥ जय श्री राम ॥

✨ *आधिकारिक व्यवस्थापक / सेवादार नियुक्ति पत्र* ✨

प्रिय *${name.trim()}* जी,
श्री बालाजी कृपा धाम सेवा मंडल में आपका हार्दिक स्वागत एवं अभिनंदन है। परम पूज्य गुरुदेव व श्री बालाजी महाराज की असीम अनुकंपा से आपको धाम प्रबंधन का दायित्व सौंपा गया है।

━━━━━━━━━━━━━━━━━━━━━
🔐 *लॉगिन क्रेडेंशियल्स (Login Credentials):*
━━━━━━━━━━━━━━━━━━━━━
👤 *यूजरनेम (Username):* `${username.trim()}`
🔑 *पासवर्ड (Password):* `${password.trim()}`
🔢 *सुरक्षा पिन (PIN):* `${pin.trim()}`
📱 *पंजीकृत मोबाइल:* `${phone.trim()}`

🛡️ *आपको प्रदत्त प्रबंधन अधिकार (Permissions):*
$permLines

━━━━━━━━━━━━━━━━━━━━━
📲 *आधिकारिक ऐप डाउनलोड लिंक:*
https://github.com/ankit261194/shri-balaji-kripa-dham/releases/latest

⚠️ *सुरक्षा निर्देश:*
1. ऐप खोलें और "व्यवस्थापक लॉगिन" (Admin Login) विकल्प चुनें।
2. अपना यूजरनेम, पासवर्ड व पिन दर्ज करके सुरक्षित लॉगिन करें।
3. यह क्रेडेंशियल गोपनीय है, कृपया इसे किसी अन्य व्यक्ति के साथ साझा न करें।

🙏 *सेवा में समर्पित: श्री बालाजी कृपा धाम परिवार*
🚩 *डूँगरा जाट, बुलन्दशहर (उत्तर प्रदेश)*
""".trimIndent()
    }

    /**
     * Renders a high-resolution, print-quality Royal Devotional Credential Banner Bitmap.
     */
    fun renderCredentialBannerBitmap(
        name: String,
        username: String,
        password: String,
        pin: String,
        phone: String,
        permissions: List<String>
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(BANNER_WIDTH, BANNER_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Radiant Parchment Background
        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, BANNER_WIDTH.toFloat(), BANNER_HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#FFFDF9"),
                    Color.parseColor("#FFF9EE"),
                    Color.parseColor("#FFF3E0")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, BANNER_WIDTH.toFloat(), BANNER_HEIGHT.toFloat(), bgPaint)

        // 2. Outer Royal Golden Gradient Border (16px)
        val goldBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 16f
            shader = LinearGradient(
                0f, 0f, BANNER_WIDTH.toFloat(), BANNER_HEIGHT.toFloat(),
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
        canvas.drawRoundRect(RectF(16f, 16f, BANNER_WIDTH - 16f, BANNER_HEIGHT - 16f), 32f, 32f, goldBorderPaint)

        // 3. Inner Sacred Maroon Inset Border (4px)
        val innerBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.parseColor("#800000") // Sacred Maroon
        }
        canvas.drawRoundRect(RectF(32f, 32f, BANNER_WIDTH - 32f, BANNER_HEIGHT - 32f), 24f, 24f, innerBorderPaint)

        // 4. Sacred Header Banner Box
        val headerBannerPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 48f, 0f, 260f,
                intArrayOf(Color.parseColor("#800000"), Color.parseColor("#5A0000")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        val headerRect = RectF(48f, 48f, BANNER_WIDTH - 48f, 260f)
        canvas.drawRoundRect(headerRect, 20f, 20f, headerBannerPaint)

        val bannerOutline = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#FFD700")
        }
        canvas.drawRoundRect(headerRect, 20f, 20f, bannerOutline)

        // Header Text: Shloka
        val mantraPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFD700")
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🚩 ॥ ॐ श्री हनुमते नमः ॥ जय श्री राम ॥ 🚩", (BANNER_WIDTH / 2).toFloat(), 95f, mantraPaint)

        // Header Text: Sanstha Name
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(5f, 2f, 2f, Color.BLACK)
        }
        canvas.drawText("श्री बालाजी कृपा धाम", (BANNER_WIDTH / 2).toFloat(), 155f, titlePaint)

        // Header Text: Address
        val addressPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFE082")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ग्राम डूँगरा जाट, बुलन्दशहर (उत्तर प्रदेश)", (BANNER_WIDTH / 2).toFloat(), 198f, addressPaint)

        val headerSubPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("परम पूज्य गुरुजी के पावन सानिध्य में • आधिकारिक सेवा मंडल", (BANNER_WIDTH / 2).toFloat(), 238f, headerSubPaint)

        // 5. Title Ribbon: Official Appointment Pass
        val ribbonPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFF3E0")
            style = Paint.Style.FILL
        }
        val ribbonRect = RectF(60f, 280f, BANNER_WIDTH - 60f, 350f)
        canvas.drawRoundRect(ribbonRect, 14f, 14f, ribbonPaint)

        val ribbonStroke = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#FFA000")
        }
        canvas.drawRoundRect(ribbonRect, 14f, 14f, ribbonStroke)

        val ribbonTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#800000")
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✨ आधिकारिक व्यवस्थापक / सेवादार नियुक्ति पत्र ✨", (BANNER_WIDTH / 2).toFloat(), 325f, ribbonTextPaint)

        // 6. Sevadar Profile Box
        val profileBoxPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val profileRect = RectF(60f, 370f, BANNER_WIDTH - 60f, 550f)
        canvas.drawRoundRect(profileRect, 16f, 16f, profileBoxPaint)

        val profileBorder = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#E0E0E0")
        }
        canvas.drawRoundRect(profileRect, 16f, 16f, profileBorder)

        // Profile details
        val nameLabelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#800000")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("👤 ${name.trim().ifEmpty { "सेवादार" }}", 90f, 425f, nameLabelPaint)

        val detailPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#424242")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("📱 पंजीकृत मोबाइल / व्हाट्सएप: ${phone.trim().ifEmpty { "N/A" }}", 90f, 475f, detailPaint)

        val rolePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#2E7D32")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("🛡️ पद: श्री बालाजी सेवा दल (अधिकृत व्यवस्थापक)", 90f, 520f, rolePaint)

        // 7. Central Credentials Golden Vault Box
        val vaultShader = LinearGradient(
            0f, 570f, 0f, 960f,
            intArrayOf(Color.parseColor("#FFFDE7"), Color.parseColor("#FFF8E1")),
            null,
            Shader.TileMode.CLAMP
        )
        val vaultPaint = Paint().apply {
            isAntiAlias = true
            shader = vaultShader
        }
        val vaultRect = RectF(60f, 570f, BANNER_WIDTH - 60f, 960f)
        canvas.drawRoundRect(vaultRect, 20f, 20f, vaultPaint)

        val vaultBorder = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#D4AF37")
        }
        canvas.drawRoundRect(vaultRect, 20f, 20f, vaultBorder)

        // Vault Header
        val vaultTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#800000")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🔐 गोपनीय लॉगिन क्रेडेंशियल्स (CONFIDENTIAL)", (BANNER_WIDTH / 2).toFloat(), 620f, vaultTitlePaint)

        val vaultDiv = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFE082")
            strokeWidth = 2f
        }
        canvas.drawLine(90f, 645f, BANNER_WIDTH - 90f, 645f, vaultDiv)

        // Row 1: Username
        val credLabelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#616161")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val credValPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#212121")
            textSize = 32f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        canvas.drawText("यूजरनेम (Username):", 90f, 685f, credLabelPaint)
        canvas.drawText(username.trim().ifEmpty { "N/A" }, 90f, 725f, credValPaint)

        // Row 2: Password
        canvas.drawText("पासवर्ड (Password):", 90f, 775f, credLabelPaint)
        val passValPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#C62828")
            textSize = 32f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText(password.trim().ifEmpty { "N/A" }, 90f, 815f, passValPaint)

        // Row 3: PIN (with golden badge)
        canvas.drawText("सुरक्षा पिन (PIN):", 90f, 865f, credLabelPaint)
        val pinValPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#E65100")
            textSize = 36f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText(pin.trim().ifEmpty { "N/A" }, 90f, 915f, pinValPaint)

        // 8. Assigned Permissions Section
        val permBoxPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val permRect = RectF(60f, 980f, BANNER_WIDTH - 60f, 1310f)
        canvas.drawRoundRect(permRect, 16f, 16f, permBoxPaint)

        val permBorder = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#E0E0E0")
        }
        canvas.drawRoundRect(permRect, 16f, 16f, permBorder)

        val permTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#800000")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("🛡️ आपको प्रदत्त प्रबंधन अधिकार (Assigned Privileges):", 90f, 1025f, permTitlePaint)

        val permItemPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#37474F")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        var permY = 1070f
        val displayPerms = if (permissions.isNotEmpty()) permissions else listOf("सामान्य सेवा व टोकन कतार")
        displayPerms.take(6).forEach { perm ->
            canvas.drawText("• $perm", 100f, permY, permItemPaint)
            permY += 40f
        }
        if (displayPerms.size > 6) {
            val extraPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#E65100")
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("+ ${displayPerms.size - 6} अन्य महत्वपूर्ण अधिकार", 100f, permY, extraPaint)
        }

        // 9. Caution & Security Warning Box
        val cautionPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFF3E0")
        }
        val cautionRect = RectF(60f, 1330f, BANNER_WIDTH - 60f, 1450f)
        canvas.drawRoundRect(cautionRect, 12f, 12f, cautionPaint)

        val cautionBorder = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.parseColor("#FFB74D")
        }
        canvas.drawRoundRect(cautionRect, 12f, 12f, cautionBorder)

        val warnTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#C62828")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("⚠️ अत्यंत महत्वपूर्ण सुरक्षा निर्देश:", (BANNER_WIDTH / 2).toFloat(), 1368f, warnTitlePaint)

        val warnSubPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#616161")
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("यह क्रेडेंशियल पास अति गोपनीय है। केवल अधिकृत सेवादार द्वारा ही प्रयोग हेतु मान्य है।", (BANNER_WIDTH / 2).toFloat(), 1405f, warnSubPaint)
        canvas.drawText("लॉगिन करने के लिए श्री बालाजी कृपा धाम ऐप में 'व्यवस्थापक लॉगिन' का प्रयोग करें।", (BANNER_WIDTH / 2).toFloat(), 1435f, warnSubPaint)

        // 10. Footer Stamp & Digital Verification Code
        val stampPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#757575")
            textSize = 19f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val timeStampStr = sdf.format(Date())
        canvas.drawText("SBKD-AUTH-ID: ${username.uppercase()}-PASS-VERIFIED • $timeStampStr", (BANNER_WIDTH / 2).toFloat(), 1500f, stampPaint)

        val footerCredit = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#9E9E9E")
            textSize = 18f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("श्री बालाजी कृपा धाम, डूँगरा जाट (बुलन्दशहर) • अधिकृत प्रशासनिक सेवा प्रणाली", (BANNER_WIDTH / 2).toFloat(), 1545f, footerCredit)

        return bitmap
    }

    /**
     * Saves the rendered credential bitmap into internal cache or app files for sharing.
     */
    suspend fun saveBannerBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        username: String
    ): File = withContext(Dispatchers.IO) {
        val cleanUser = username.replace(Regex("[^a-zA-Z0-9_]"), "_").ifEmpty { "sevadar" }
        val fileName = "ShriBalaji_Sevadar_Pass_${cleanUser}_${System.currentTimeMillis()}.png"
        val shareDir = File(context.cacheDir, "admin_credentials")
        if (!shareDir.exists()) {
            shareDir.mkdirs()
        }
        val destFile = File(shareDir, fileName)
        FileOutputStream(destFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
        }
        destFile
    }

    /**
     * Cleans phone number to international WhatsApp format (+91 for India if 10 digits).
     */
    fun cleanPhoneNumber(phone: String): String {
        var p = phone.replace("+", "").replace(" ", "").replace("-", "").replace("(", "").replace(")", "").trim()
        if (p.length == 10) {
            p = "91$p"
        }
        return p
    }

    /**
     * Sends the credentials directly to WhatsApp:
     * - If image file provided: sends image + caption text via WhatsApp
     * - Also supports direct WhatsApp URL fallback: https://api.whatsapp.com/send?phone=...&text=...
     */
    fun sendToWhatsApp(
        context: Context,
        phone: String,
        messageText: String,
        bannerFile: File? = null
    ) {
        val cleanPhone = cleanPhoneNumber(phone)
        try {
            if (bannerFile != null && bannerFile.exists()) {
                val imageUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    bannerFile
                )

                // Direct WhatsApp Intent with image + caption + recipient phone
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, messageText)
                    if (cleanPhone.isNotBlank()) {
                        putExtra("jid", "$cleanPhone@s.whatsapp.net")
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val pm = context.packageManager
                val whatsappInstalled = try {
                    pm.getPackageInfo("com.whatsapp", 0)
                    true
                } catch (e: Exception) {
                    try {
                        pm.getPackageInfo("com.whatsapp.w4b", 0)
                        true
                    } catch (e2: Exception) {
                        false
                    }
                }

                if (whatsappInstalled) {
                    val pkg = if (try { pm.getPackageInfo("com.whatsapp", 0); true } catch(e: Exception) { false }) "com.whatsapp" else "com.whatsapp.w4b"
                    intent.setPackage(pkg)
                    context.startActivity(intent)
                } else {
                    val chooser = Intent.createChooser(intent, "व्हाट्सएप या अन्य ऐप पर क्रेडेंशियल भेजें")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            } else {
                // Text-only direct WhatsApp launch
                val encodedText = URLEncoder.encode(messageText, "UTF-8")
                val url = if (cleanPhone.isNotBlank()) {
                    "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText"
                } else {
                    "https://api.whatsapp.com/send?text=$encodedText"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val textIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(textIntent, "विवरण साझा करें"))
            } catch (err: Exception) {
                Toast.makeText(context, "व्हाट्सएप खोलने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Copies the formatted credential text to clipboard.
     */
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Balaji Admin Credentials", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "✓ क्रेडेंशियल्स क्लिपबोर्ड पर कॉपी हो गए!", Toast.LENGTH_SHORT).show()
    }
}
