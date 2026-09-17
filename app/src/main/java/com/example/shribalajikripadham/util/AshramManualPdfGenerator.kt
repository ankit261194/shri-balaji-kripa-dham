package com.example.shribalajikripadham.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट, बुलन्दशहर)
 * आधिकारिक मोबाइल एप्लिकेशन मार्गदर्शिका PDF जेनरेटर
 * 1. भक्त संपूर्ण मार्गदर्शिका (Devotee User Manual)
 * 2. व्यवस्थापक एवं सेवादार कार्यप्रणाली मार्गदर्शिका (Admin & Sevadar Manual - केवल एडमिन हेतु)
 */
object AshramManualPdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 Width (72 DPI)
    private const val PAGE_HEIGHT = 842 // Standard A4 Height (72 DPI)
    private const val MARGIN = 30f
    private const val USABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2) // 535pt

    private val MaroonColor = Color.rgb(128, 0, 0)
    private val SaffronColor = Color.rgb(210, 90, 0)
    private val GoldBorderColor = Color.rgb(220, 160, 40)
    private val DarkTextColor = Color.rgb(40, 40, 40)
    private val LightTextColor = Color.rgb(90, 90, 90)
    private val CardBgLight = Color.rgb(255, 252, 244)
    private val CardBgAdmin = Color.rgb(253, 246, 255)

    data class ManualSection(
        val icon: String,
        val title: String,
        val points: List<String>
    )

    /**
     * 1. भक्त संपूर्ण मार्गदर्शिका PDF (Devotee User Manual)
     */
    fun generateDevoteeGuidePdf(context: Context): File? {
        val sections = listOf(
            ManualSection(
                icon = "🎟️",
                title = "1. रविवार दरबार टोकन व्यवस्था (Sunday Token Booking)",
                points = listOf(
                    "• टोकन खुलने का समय: प्रत्येक रविवार प्रातः निश्चित समय पर (अथवा सुपर एडमिन द्वारा निर्धारित समय पर) टोकन सेवा स्वतः खुलती है।",
                    "• GPS व आश्रम परिधि (500 मीटर Geofence): टोकन केवल वही भक्त ले सकते हैं जो धाम परिसर (डूँगरा जाट) के 500 मीटर के दायरे में उपस्थित हों और जिनका मोबाइल GPS चालू हो।",
                    "• एंटी-स्पूफ सुरक्षा: घर बैठे फर्जी लोकेशन (Fake GPS) से टोकन बनाना पूर्णतः वर्जित व ब्लॉक है।",
                    "• टोकन बुकिंग चरण: होम स्क्रीन पर 'दरबार टोकन जनरेट करें' दबाएं ➔ अपना नाम, मोबाइल नंबर व शहर दर्ज करें ➔ पहचान हेतु फोटो खींचें (वैकल्पिक) ➔ 'टोकन जारी करें' दबाएं। आपका डिजिटल टोकन नंबर स्क्रीन पर आ जाएगा।"
                )
            ),
            ManualSection(
                icon = "🤳",
                title = "2. फेस वेरिफिकेशन टोकन (AI Face Recognition Token)",
                points = listOf(
                    "• बिना किसी फॉर्म भरे सीधे 2 सेकंड में टोकन जनरेट करने की आधुनिक सुविधा।",
                    "• स्क्रीन पर 'फेस वेरिफिकेशन टोकन' खोलें ➔ कैमरा के सामने चेहरा रखें ➔ सिस्टम स्वतः पहचान करके टोकन जारी कर देगा।"
                )
            ),
            ManualSection(
                icon = "🔴",
                title = "3. इन-ऐप लाइव दर्शन व 24x7 पावन भजन/आरती प्लेयर",
                points = listOf(
                    "• लाइव दरबार दर्शन: बिना किसी बाहरी ऐप या विज्ञापन के, सीधे ऐप के अंदर यूट्यूब लाइव दर्शन देखें।",
                    "• 24x7 पावन ऑडियो प्लेयर: श्री हनुमान चालीसा, संकटमोचन हनुमानाष्टक, बजरंग बाण, आरती कीजै हनुमान लला की एवं श्री रामचन्द्र कृपालु भजु मन का निरंतर श्रवण करें।",
                    "• बैकग्राउंड प्लेबैक: ऐप बंद होने या स्क्रीन लॉक होने पर भी आरती व भजन निरंतर चलते रहते हैं।"
                )
            ),
            ManualSection(
                icon = "⚡",
                title = "4. लाइव टोकन काउंटर व सब-5 सेकंड हेड्स-अप अलर्ट्स",
                points = listOf(
                    "• होम स्क्रीन पर वर्तमान में सेवारत (Serving) टोकन नंबर हर 2-3 सेकंड में लाइव अपडेट होता है।",
                    "• सायरन / हेड्स-अप अलर्ट: जैसे ही आपका नंबर आने वाला होगा (5 टोकन शेष रहने पर), आपके फोन पर तेज ध्वनि व कंपन के साथ अलर्ट आएगा: '🚨 आपका टोकन समीप है - आश्रम हॉल में उपस्थित रहें!'",
                    "• नंबर आते ही सर्वोच्च प्राथमिकता अलर्ट बजता है ताकि भीड़ में आपका नंबर कभी न छूटे।"
                )
            ),
            ManualSection(
                icon = "📜",
                title = "5. आश्रम पावन पर्चे (Digital Parchas & Vidhi)",
                points = listOf(
                    "• धाम पर दी जाने वाली विभिन्न पूजा विधियों (हवन पर्चा, उतारा पर्चा, अरदास पर्चा आदि) को ऐप में पढ़ें।",
                    "• प्रत्येक पर्चे की आवश्यक सामग्री सूची व चरणबद्ध विधि घर बैठे देखें तथा तुरंत A4 साइज में PDF डाउनलोड व शेयर करें।"
                )
            ),
            ManualSection(
                icon = "🚗",
                title = "6. श्री बालाजी यात्रा बस सेवा",
                points = listOf(
                    "• धाम से विभिन्न तीर्थों (मेहंदीपुर बालाजी, सालासर आदि) हेतु बस सेवा की तारीख व विवरण देखें।",
                    "• उपलब्ध सीटें देखकर अपनी मनपसंद सीट चुनें और डिजिटल यात्रा टिकट डाउनलोड करें।"
                )
            ),
            ManualSection(
                icon = "ℹ️",
                title = "7. आश्रम नियम, आरती समय व दर्शन व्यवस्था",
                points = listOf(
                    "• प्रातः व सायं आरती के पावन समय का विवरण देखें।",
                    "• दरबार के दौरान पूर्ण अनुशासन, मर्यादा व स्वच्छता बनाए रखें।",
                    "• किसी भी बिचौलिए अथवा बाहरी व्यक्ति को दक्षिणा या चढ़ावा न दें।"
                )
            ),
            ManualSection(
                icon = "📞",
                title = "8. हेल्पलाइन, दान रसीद व सोशल मीडिया संपर्क",
                points = listOf(
                    "• आधिकारिक व्हाट्सएप ग्रुप, यूट्यूब चैनल, फेसबुक पेज व इंस्टाग्राम से एक टैप में जुड़ें।",
                    "• धाम हेल्पलाइन पर संपर्क करें अथवा ऐच्छिक सहयोग/दान की आधिकारिक रसीद ऐप में देखें।"
                )
            )
        )

        return renderManualPdf(
            context = context,
            fileName = "ShriBalajiKripaDham_Bhakt_Margdarshika.pdf",
            docTitle = "भक्त संपूर्ण मोबाइल ऐप मार्गदर्शिका",
            subTitle = "ऐप में क्या-क्या है, कैसे देखें और कैसे उपयोग करें - संपूर्ण विवरण",
            isConfidential = false,
            sections = sections
        )
    }

    /**
     * 2. व्यवस्थापक एवं सेवादार संपूर्ण कार्यप्रणाली मार्गदर्शिका (Admin & Sevadar Manual)
     * (केवल एडमिन पैनल में अधिकृत सेवादारों को ही दिखाई देगी)
     */
    fun generateAdminGuidePdf(context: Context): File? {
        val sections = listOf(
            ManualSection(
                icon = "🔐",
                title = "1. व्यवस्थापक / सेवादार लॉगिन व क्लाउड सुरक्षा",
                points = listOf(
                    "• सुपर एडमिन लॉगिन: मास्टर पासवर्ड द्वारा सर्वोच्च नियंत्रण लॉगिन।",
                    "• सेवादार लॉगिन: यूजरनेम व पासवर्ड अथवा 4-अंकीय त्वरित सुरक्षा पिन।",
                    "• स्वतः क्लाउड रिकवरी (Zero-Effort Auto Sync): ऐप अनइंस्टॉल होने या नए फोन पर लॉगिन करने पर बिना कोई बटन दबाए सभी सेवादार, सेटिंग्स, परचे व डेटा स्वतः क्लाउड से डाउनलोड हो जाते हैं।",
                    "• साल्टेड क्रिप्टोग्राफिक हैशिंग: सभी पासवर्ड व पिन सर्वर व ऐप में अत्यधिक सुरक्षित साल्टेड हैश द्वारा सुरक्षित हैं।"
                )
            ),
            ManualSection(
                icon = "🎟️",
                title = "2. लाइव टोकन कतार प्रबंधन (Queue Calling & Management)",
                points = listOf(
                    "• टोकन कॉल करना: 'अगला टोकन बुलाएं' दबाते ही सिस्टम अगले मरीज को कॉल करता है और आश्रम में स्वचालित हिंदी उद्घोषणा (TTS) बोलती है।",
                    "• दर्शन संपन्न मार्क करना: मरीज के दर्शन होने पर 'दर्शन पूर्ण' मार्क करें ताकि रिकॉर्ड और डिस्प्ले बोर्ड लाइव अपडेट हो सके।",
                    "• टोकन रद्द / हटाना: अनुपस्थित मरीज का टोकन अधिकृत सेवादार द्वारा रद्द अथवा हटाया जा सकता है।"
                )
            ),
            ManualSection(
                icon = "📝",
                title = "3. सेवादार डेस्क से मैन्युअल टोकन जारी करना (Admin Desk)",
                points = listOf(
                    "• वृद्ध, दिव्यांग अथवा दूरदराज से आए मरीजों हेतु काउंटर से टोकन बनाना।",
                    "• GPS परिधि बाईपास (Bypass Geofence) करने का अधिकार (यदि सुपर एडमिन द्वारा अनुमति प्राप्त हो)।",
                    "• कैमरे से मरीज का फोटो खींचना व इच्छानुसार कस्टम टोकन नंबर आवंटित करना।"
                )
            ),
            ManualSection(
                icon = "📺",
                title = "4. स्मार्ट टीवी व आश्रम हॉल डिस्प्ले बोर्ड (Hall Display)",
                points = listOf(
                    "• आश्रम हॉल के टीवी अथवा टैबलेट पर 'स्मार्ट टीवी हॉल डिस्प्ले' चालू करें।",
                    "• स्क्रीन पर वर्तमान टोकन, मरीज का नाम व शहर 50 फीट दूर से भी बड़ा-बड़ा दिखाई देता है।",
                    "• स्क्रीन कभी लॉक नहीं होती (FLAG_KEEP_SCREEN_ON) और हर नए टोकन पर स्वतः हिंदी आवाज में उद्घोषणा होती है।"
                )
            ),
            ManualSection(
                icon = "💰",
                title = "5. दान (चंदा) व आश्रम व्यय (खर्च) लेजर प्रबंधन",
                points = listOf(
                    "• भक्तों द्वारा दिए गए ऐच्छिक दान की रसीद काटना व व्हाट्सएप पर तुरंत भेजना।",
                    "• आश्रम के दैनिक खर्च (भंडारा, बिजली, निर्माण, मरम्मत आदि) का वाउचर दर्ज करना।",
                    "• सम्पूर्ण आय-व्यय की एक्सेल (Excel) व PDF बैलेंस शीट डाउनलोड करना।"
                )
            ),
            ManualSection(
                icon = "📜",
                title = "6. आश्रम पावन पर्चे जोड़ना व संपादित करना",
                points = listOf(
                    "• नए पर्चे जोड़ना, आवश्यक पूजन सामग्री की सूची व चरणबद्ध विधि दर्ज करना और भक्तों के लिए लाइव करना।",
                    "• आधिकारिक मोहर व इंसिग्निया के साथ A4 प्रिंट निकालना।"
                )
            ),
            ManualSection(
                icon = "👥",
                title = "7. सेवादार प्रबंधन व परिचय पत्र (Super ID Card Studio)",
                points = listOf(
                    "• नए सेवादार की आईडी बनाना, पासवर्ड व 4-अंकीय पिन सेट करना।",
                    "• प्रत्येक सेवादार को अलग-अलग अधिकार (Permissions) सौंपना व जरूरत पड़ने पर निष्क्रिय करना।",
                    "• सुपर आईडी कार्ड स्टूडियो से आधिकारिक बारकोड/क्यूआर युक्त पहचान पत्र डाउनलोड व प्रिंट करना।"
                )
            ),
            ManualSection(
                icon = "⚙️",
                title = "8. आश्रम सेटिंग्स व सुपर एडमिन कंट्रोल (Sub-3s Live Reflection)",
                points = listOf(
                    "• रनिंग टोकन सीधे बदलना, रविवार टोकन खुलने का समय निर्धारित करना।",
                    "• आपातकालीन सूचनाएं व मुख्य बैनर फोटो बदलना।",
                    "• किसी भी सेवा (टोकन, यात्रा आदि) को एक क्लिक में चालू/बंद करना। यह सभी भक्तों के फोन पर 2-3 सेकंड में लाइव लागू होता है।"
                )
            )
        )

        return renderManualPdf(
            context = context,
            fileName = "ShriBalajiKripaDham_Admin_Sevadar_Manual.pdf",
            docTitle = "व्यवस्थापक एवं सेवादार कार्यप्रणाली मार्गदर्शिका",
            subTitle = "गोपनीय संस्करण - केवल अधिकृत आश्रम सेवादारों एवं सुपर एडमिन हेतु",
            isConfidential = true,
            sections = sections
        )
    }

    /**
     * Common multi-page A4 PDF rendering engine with mathematical overflow protection
     */
    private fun renderManualPdf(
        context: Context,
        fileName: String,
        docTitle: String,
        subTitle: String,
        isConfidential: Boolean,
        sections: List<ManualSection>
    ): File? {
        val pdfDoc = PdfDocument()

        val titlePaint = Paint().apply {
            color = MaroonColor
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val subTitlePaint = Paint().apply {
            color = SaffronColor
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val confidentialBadgePaint = Paint().apply {
            color = Color.rgb(180, 0, 0)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val sectionTitlePaint = Paint().apply {
            color = MaroonColor
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val pointPaint = Paint().apply {
            color = DarkTextColor
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val footerPaint = Paint().apply {
            color = LightTextColor
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val outerBorderPaint = Paint().apply {
            color = MaroonColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }

        val innerBorderPaint = Paint().apply {
            color = GoldBorderColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

        val symbolPaint = Paint().apply {
            color = MaroonColor
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val cardBgPaint = Paint().apply {
            color = if (isConfidential) CardBgAdmin else CardBgLight
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val cardBorderPaint = Paint().apply {
            color = if (isConfidential) Color.rgb(180, 140, 200) else GoldBorderColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

        var currentPageNumber = 1
        var currentY = MARGIN + 25f
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null

        fun startPage() {
            if (currentPage != null) {
                // Draw footer on previous page
                canvas?.let { c ->
                    c.drawText(
                        "पृष्ठ $currentPageNumber | श्री बालाजी कृपा धाम (डूँगरा जाट, बुलन्दशहर) | अधिक जानकारी हेतु संपर्क: 8006518960",
                        PAGE_WIDTH / 2f,
                        PAGE_HEIGHT - MARGIN + 12f,
                        footerPaint
                    )
                }
                pdfDoc.finishPage(currentPage)
                currentPageNumber++
            }

            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
            currentPage = pdfDoc.startPage(pageInfo)
            canvas = currentPage!!.canvas

            // 1. Background and double border
            canvas?.drawColor(Color.WHITE)
            canvas?.drawRect(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN, outerBorderPaint)
            canvas?.drawRect(MARGIN + 3.5f, MARGIN + 3.5f, PAGE_WIDTH - MARGIN - 3.5f, PAGE_HEIGHT - MARGIN - 3.5f, innerBorderPaint)

            // 2. Corner Sacred Symbols
            canvas?.drawText("ॐ", MARGIN + 12f, MARGIN + 16f, symbolPaint)
            canvas?.drawText("卐", PAGE_WIDTH - MARGIN - 12f, MARGIN + 16f, symbolPaint)
            canvas?.drawText("卐", MARGIN + 12f, PAGE_HEIGHT - MARGIN - 8f, symbolPaint)
            canvas?.drawText("ॐ", PAGE_WIDTH - MARGIN - 12f, PAGE_HEIGHT - MARGIN - 8f, symbolPaint)

            // 3. Header
            currentY = MARGIN + 22f
            canvas?.drawText("॥ श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट (बुलन्दशहर) ॥", PAGE_WIDTH / 2f, currentY, subTitlePaint)
            currentY += 18f
            canvas?.drawText(docTitle, PAGE_WIDTH / 2f, currentY, titlePaint)
            currentY += 14f
            canvas?.drawText(subTitle, PAGE_WIDTH / 2f, currentY, if (isConfidential) confidentialBadgePaint else subTitlePaint)
            currentY += 10f

            // Decorative separator line
            val divPaint = Paint().apply {
                color = MaroonColor
                strokeWidth = 1f
            }
            canvas?.drawLine(MARGIN + 30f, currentY, PAGE_WIDTH - MARGIN - 30f, currentY, divPaint)
            currentY += 15f
        }

        // Start first page
        startPage()

        val textWidth = USABLE_WIDTH - 24f // 12pt padding on left and right inside card

        for (sec in sections) {
            // Calculate height of this section
            val wrappedLines = mutableListOf<String>()
            for (p in sec.points) {
                wrappedLines.addAll(wrapText(p, pointPaint, textWidth))
            }

            val cardPaddingY = 10f
            val headerHeight = 20f
            val lineHeight = 13.5f
            val cardHeight = (cardPaddingY * 2) + headerHeight + (wrappedLines.size * lineHeight) + 6f

            // Check if card fits on current page (Leave 35pt margin for bottom border & footer)
            if (currentY + cardHeight > PAGE_HEIGHT - MARGIN - 25f) {
                startPage()
            }

            // Draw Section Card
            val cardRect = RectF(MARGIN + 8f, currentY, PAGE_WIDTH - MARGIN - 8f, currentY + cardHeight)
            canvas?.drawRoundRect(cardRect, 8f, 8f, cardBgPaint)
            canvas?.drawRoundRect(cardRect, 8f, 8f, cardBorderPaint)

            var cardTextY = currentY + cardPaddingY + 12f

            // Section Header
            canvas?.drawText("${sec.icon} ${sec.title}", MARGIN + 18f, cardTextY, sectionTitlePaint)
            cardTextY += 16f

            // Section Points
            for (line in wrappedLines) {
                canvas?.drawText(line, MARGIN + 20f, cardTextY, pointPaint)
                cardTextY += lineHeight
            }

            currentY += cardHeight + 8f
        }

        // Finish last page
        canvas?.let { c ->
            c.drawText(
                "पृष्ठ $currentPageNumber | श्री बालाजी कृपा धाम (डूँगरा जाट, बुलन्दशहर) | अधिक जानकारी हेतु संपर्क: 8006518960",
                PAGE_WIDTH / 2f,
                PAGE_HEIGHT - MARGIN + 12f,
                footerPaint
            )
        }
        pdfDoc.finishPage(currentPage)

        // Write to external documents directory
        return try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!docsDir.exists()) docsDir.mkdirs()

            val pdfFile = File(docsDir, fileName)
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

    /**
     * Helper to wrap text according to maxWidth
     */
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

    /**
     * Opens or shares the generated PDF using Android FileProvider and Intent.createChooser
     */
    fun openOrSharePdf(context: Context, pdfFile: File?, title: String) {
        if (pdfFile == null || !pdfFile.exists()) return
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(viewIntent, "$title खोलें / डाउनलोड करें").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            try {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, "जय श्री बालाजी! $title")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "साझा करें").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
