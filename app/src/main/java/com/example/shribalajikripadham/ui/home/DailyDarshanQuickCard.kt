package com.example.shribalajikripadham.ui.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shribalajikripadham.R
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class DailyDarshanData(
    val dateHindi: String,
    val title: String,
    val photoUrl: String,
    val quote: String,
    val viewsCount: Int
)

object DailyDarshanHelper {
    val GURU_VICHAR_COLLECTION = listOf(
        "जब जीवन में हर तरफ से रास्ते बंद दिखने लगें, मन अशांत हो और अपने भी साथ छोड़ दें, तब घबराकर कभी अधर्म का रास्ता मत चुनना। संकट की घड़ी भक्त के धैर्य की परीक्षा होती है। पूज्य गुरुदेव समझाते हैं कि अपनी विपत्ति का बोझ अपने सिर पर मत ढोओ, उसे पूर्ण विश्वास के साथ श्री बालाजी महाराज के चरणों में समर्पित कर दो। जो भक्त सच्चे हृदय से 'हे संकटमोचन, मैं आपकी शरण में हूँ' कहकर अपना कर्तव्य करता रहता है, बालाजी महाराज स्वयं ढाल बनकर उसके सारे कष्ट हर लेते हैं।",
        "अहंकार और क्रोध दो ऐसे विष हैं जो मनुष्य के सारे पुण्य और घर की खुशियों को भस्म कर देते हैं। दरबार में जब भी आओ, अपनी उपलब्धियों का घमंड चौखट के बाहर छोड़कर आना। गुरु कृपा का अटल नियम है — जो झुकता है, उसकी झोली भरती है; जो अकड़ता है, वह खाली हाथ रह जाता है। अपने माता-पिता के चरणों में नित्य शीश नवाओ और असहाय की सेवा करो; जिस घर में वृद्धों और माता-पिता का सम्मान होता है, उस घर पर श्री बालाजी का सुरक्षा चक्र सदैव अडिग रहता है।",
        "भक्ति केवल तिलक लगाने या दिखावे का नाम नहीं है, भक्ति तो अपने आचरण को पवित्र बनाने की साधना है। अपने भीतर से दूसरों के प्रति ईर्ष्या, कटु वाणी और निंदा का मैल साफ़ करो। जब तक मन का पात्र शुद्ध नहीं होगा, तब तक प्रभु कृपा का अमृत उसमें कैसे ठहरेगा? संकट चाहे तंत्र-बाधा का हो या असाध्य बीमारी का, नित्य एक माला 'जय श्री राम' व 'ॐ हनुमते नमः' की जपो, समस्त नकारात्मक शक्तियाँ स्वतः भाग खड़ी होंगी।",
        "दरबार में अर्जी लगाने के बाद मन में संशय या अधीरता मत लाओ। किसान जब बीज बोता है, तो रोज़ मिट्टी खोदकर नहीं देखता कि पौधा कितना उगा। वह विश्वास के साथ जल सींचता है और उचित समय पर फसल लहलहा उठती है। ऐसे ही गुरुदेव के वचनों और बालाजी के न्याय पर अडिग भरोसा रखो। तुम्हारी हर पीड़ा, हर एक आंसू का हिसाब धाम की पावन गद्दी पर लिखा जा चुका है, समय आने पर बालाजी का चमत्कार तुम्हारी ज़िंदगी बदल देगा।",
        "जो व्यक्ति दूसरों का हक छीनकर, बेईमानी करके या किसी का दिल दुखाकर धन कमाता है, वह कभी सुखी और निरोगी नहीं रह सकता। धर्म की कमाई में ही बरकत और शांति है। पूज्य गुरुदेव का सदैव यही संदेश रहता है कि सत्य और सेवा के मार्ग पर चलो। चाहे थोड़ी देर के लिए कष्ट उठाना पड़े, पर अंत में विजय हमेशा सत्य और धर्म की ही होती है।",
        "संसार का सबसे बड़ा सत्य यही है कि परमात्मा के सिवा कोई अपना स्थाई सहारा नहीं है। जब मनुष्य संसार के झूठे आकर्षणों और वासनाओं से मोह हटाकर प्रभु चरणों में निष्काम प्रेम करता है, तब उसके जीवन में वास्तविक आनंद का उदय होता है। हनुमान चालीसा केवल पढ़ने की वस्तु नहीं, जीने का मंत्र है। जो नित्य निष्ठा से पाठ करता है, संकट मोचन उसके अंग-संग रक्षा करते हैं।",
        "वाणी में अमृत भी है और विष भी। किसी को दिया गया कड़वा वचन किसी तीर से कम नहीं होता, जो जीवनभर दिल को छलनी करता है। इसलिए जब भी बोलो, मीठा, सत्य और मर्यादित बोलो। गुरुदेव कहते हैं कि अपनी वाणी से किसी रोते हुए के आंसू पोंछ सको तो इससे बड़ा कोई भजन नहीं। जिस मुख से निरंतर राम नाम और मधुर वचन निकलते हैं, वहाँ साक्षात प्रभु का वास होता है।",
        "कर्मों की रेखा से कोई नहीं बच सकता, लेकिन जब भक्त सच्चे मन से पश्चाताप करके बालाजी की शरण में आ जाता है, तो प्रभु उसके पूर्व जन्मों के घोर पापों और संकटों को भी भस्म कर देते हैं। अपनी गलतियों को स्वीकार करो, कपट का त्याग करो और आज से ही पवित्र जीवन जीने का संकल्प लो। बालाजी का दरबार दया और न्याय का पावन संगम है।",
        "परिवार में शांति और प्रेम बनाए रखना ही सबसे बड़ी साधना है। छोटी-छोटी बातों पर कलह, जिद और अहंकार पालना घर को नर्क बना देता है। क्षमा करना सीखो। जो झुकना जानता है, वही परिवार को बांधकर रख सकता है। नित्य प्रातः घर में शंख, घंटी और हनुमान चालीसा की पावन ध्वनि गूंजनी चाहिए, जिससे घर की नकारात्मक ऊर्जा नष्ट हो और सुख-समृद्धि का वास हो।",
        "कठिनाइयों से कभी भागना मत, क्योंकि सोना भी आग में तपकर ही कुंदन बनता है। विपत्ति मनुष्य के आत्मबल और विश्वास को मजबूत करने आती है। जब भी संकट आए, 'नासै रोग हरै सब पीरा, जपत निरंतर हनुमत बीरा' का निरंतर स्मरण करो। श्री बालाजी कृपा धाम की पावन माटी में वह सामर्थ्य है जो असंभव को भी संभव में बदल देती है।",
        "माता-पिता ईश्वर का साक्षात प्रत्यक्ष रूप हैं। जो संतान अपने वृद्ध माता-पिता को रुलाती है या उनकी उपेक्षा करती है, वह चाहे कितने भी तीर्थ नहा ले या अनुष्ठान कर ले, उसे कभी शांति नहीं मिल सकती। माता-पिता के चेहरे पर मुस्कान लाना और उनके चरणों की सेवा करना ही कलयुग का सबसे बड़ा यज्ञ है।",
        "ईश्वर से कभी सांसारिक भोग-विलास की चीजें मत मांगो; मांगना ही है तो निष्कपट भक्ति, सेवा भाव और संतोष मांगो। जब प्रभु प्रसन्न होते हैं, तो वे बिना मांगे ही वह सब कुछ दे देते हैं जो हमारे लिए सर्वश्रेष्ठ होता है। जो मिला है उसमें प्रभु का धन्यवाद करो, जो नहीं मिला उसमें प्रभु की कोई गुप्त भलाई समझो।",
        "दूसरों की उन्नति और सुख देखकर कभी मन में जलन या ईर्ष्या मत लाओ। जिसने जो बोया है, वही काटेगा। अपने मन को दर्पण की तरह स्वच्छ रखो। जब तुम दूसरों की भलाई के लिए बालाजी से प्रार्थना करोगे, तो बालाजी सबसे पहले तुम्हारी झोली खुशियों से भरेंगे। परोपकार ही मनुष्य का सच्चा आभूषण है।",
        "नकारात्मक ऊर्जा, नजर-दोष और ऊपरी बाधाएं उसी व्यक्ति पर हावी होती हैं जिसका आत्मबल कमजोर होता है और जो भयभीत रहता है। जो भक्त निडर होकर 'भूत पिशाच निकट नहिं आवै, महाबीर जब नाम सुनावै' का नाद करता है, उसके पास काल भी फटकने की हिम्मत नहीं करता। अपने भीतर के डर को बालाजी के चरणों में जला दो।",
        "दान और परोपकार हमेशा गुप्त और बिना किसी अहंकार के होना चाहिए। एक हाथ से दो तो दूसरे हाथ को पता न चले। दिखावे का दान केवल यश की भूख मिटाता है, जबकि सच्चे मन से किसी भूखे, प्यासे या असहाय की की गई सेवा सीधे परमात्मा के दरबार में जमा होती है और वंशों तक रक्षा करती है।",
        "समय सबसे बलवान है। आज जो संकट का अंधकार दिख रहा है, कल वही सुख का नया सवेरा बनकर चमकेगा। सुख में प्रभु को मत भूलो और दुख में धीरज मत खोओ। समभाव में रहना ही सच्चे साधक और बालाजी के अनन्य सेवक की पहचान है।",
        "भोजन और विचार का गहरा संबंध है। जैसा खाओगे अन्न, वैसा होगा मन। सात्विक, पवित्र और प्रभु को भोग लगाकर किया गया भोजन बुद्धि को निर्मल बनाता है, जबकि तामसिक, मदिरा और अनैतिक भोजन आत्मा को गंदा कर देता है। अपने आहार और आचार को शुद्ध रखो, रोग और दोष स्वतः समाप्त हो जाएंगे।",
        "सत्संग और अच्छे लोगों की संगति पारस पत्थर के समान है, जो लोहे जैसी मलिन बुद्धि को भी स्वर्ण बना देती है। नित्य कुछ समय संतों के विचार, गीता के श्लोक और बालाजी की महिमा में लगाओ। व्यर्थ के वाद-विवाद, मोबाइल पर गपशप और दूसरों की निंदा से अपनी ऊर्जा को बचाओ।",
        "जब भी मन में घबराहट या निराशा का दौर आए, एकांत में बैठकर अपनी दोनों आंखें बंद करो और अपने हृदय में वीर बजरंगी के सिंदूरी रूप का ध्यान करो। महसूस करो कि उनका वरदहस्त तुम्हारे मस्तक पर है। उनकी उपस्थिति का आभास ही तुम्हारे रोम-रोम में असीम साहस और नई ऊर्जा भर देगा।",
        "दरबार का पावन नियम है कि जो भी व्यक्ति निःस्वार्थ भाव से आश्रम की सेवा, स्वच्छता या भक्तों की सहायता में अपना समय देता है, उस पर बालाजी महाराज की विशेष कृपा बरसती है। सेवा से बड़ा कोई तप नहीं है। अपने जीवन का कुछ अंश समाज और धर्म की सेवा में अवश्य समर्पित करो।"
    )

    fun getTodayGuruVichar(): String {
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        return GURU_VICHAR_COLLECTION[dayOfYear % GURU_VICHAR_COLLECTION.size]
    }

    private val sdfHindiMonth = arrayOf(
        "जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून",
        "जुलाई", "अगस्त", "सितम्बर", "अक्टूबर", "नवम्बर", "दिसम्बर"
    )

    fun getTodayHindiDate(): String {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = sdfHindiMonth[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        return "$day $month $year"
    }

    suspend fun fetchTodayDarshan(): DailyDarshanData = withContext(Dispatchers.IO) {
        val fallback = DailyDarshanData(
            dateHindi = getTodayHindiDate(),
            title = "श्री बालाजी महाराज दैनिक दिव्य अलौकिक श्रृंगार दर्शन",
            photoUrl = "https://shribalajikripadham.online/media/balaji_darshan_today.jpg",
            quote = getTodayGuruVichar(),
            viewsCount = 1280
        )

        try {
            val url = URL("https://shribalajikripadham.online/api/daily_darshan.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ShriBalajiKripaDham-Android")
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (json.optBoolean("success", false)) {
                    DailyDarshanData(
                        dateHindi = json.optString("date_hindi", fallback.dateHindi),
                        title = json.optString("title", fallback.title),
                        photoUrl = json.optString("photo_url", fallback.photoUrl),
                        quote = json.optString("blessings_quote", fallback.quote),
                        viewsCount = json.optInt("views_count", fallback.viewsCount)
                    )
                } else fallback
            } else fallback
        } catch (e: Exception) {
            fallback
        }
    }

    fun shareDarshanOnWhatsApp(
        context: Context,
        darshan: DailyDarshanData,
        bitmap: Bitmap? = null
    ) {
        try {
            var imageUri: Uri? = null
            if (bitmap != null) {
                val shareDir = File(context.cacheDir, "darshan_shares")
                if (!shareDir.exists()) shareDir.mkdirs()
                val shareFile = File(shareDir, "Darshan_${System.currentTimeMillis()}.png")
                FileOutputStream(shareFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
                    fos.flush()
                }
                imageUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    shareFile
                )
            }

            val shareText = """
🚩 *श्री बालाजी कृपा धाम, डूँगरा जाट* 🚩
*परम पूज्य गुरुजी तेजवीर सिंह जी*
══════════════════════════
🌺 *आज का पावन अलौकिक श्रृंगार दर्शन* 🌺
📅 *तिथि:* ${darshan.dateHindi}
✨ *दैनिक पावन आशीर्वाद:*
"${darshan.quote}"
══════════════════════════
🙏 *भूत-प्रेत व असाध्य मानसिक समस्याओं का पूर्णतः निःशुल्क इलाज।*
🌐 लाइव दर्शन व रविवार टोकन हेतु ऐप डाउनलोड करें:
https://shribalajikripadham.online/app
            """.trimIndent()

            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                if (imageUri != null) {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_TEXT, shareText)
                setPackage("com.whatsapp")
            }

            try {
                context.startActivity(whatsappIntent)
            } catch (e: Exception) {
                try {
                    val businessIntent = Intent(Intent.ACTION_SEND).apply {
                        if (imageUri != null) {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            type = "text/plain"
                        }
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        setPackage("com.whatsapp.w4b")
                    }
                    context.startActivity(businessIntent)
                } catch (e2: Exception) {
                    val chooser = Intent(Intent.ACTION_SEND).apply {
                        if (imageUri != null) {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            type = "text/plain"
                        }
                        putExtra(Intent.EXTRA_SUBJECT, "आज का पावन अलौकिक दर्शन")
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(chooser, "अलौकिक दर्शन शेयर करें"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "दर्शन शेयर करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun DailyDarshanQuickCard(
    isHindi: Boolean,
    ashramSettings: AshramSettings,
    currentTheme: SacredTheme = LocalSacredStyle.current.theme,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var darshanData by remember {
        mutableStateOf(
            DailyDarshanData(
                dateHindi = DailyDarshanHelper.getTodayHindiDate(),
                title = "श्री बालाजी महाराज दैनिक दिव्य अलौकिक श्रृंगार दर्शन",
                photoUrl = "https://shribalajikripadham.online/media/balaji_darshan_today.jpg",
                quote = DailyDarshanHelper.getTodayGuruVichar(),
                viewsCount = 1280
            )
        )
    }

    var remoteBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showZoomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val fetched = DailyDarshanHelper.fetchTodayDarshan()
        darshanData = fetched

        // Try downloading remote bitmap
        withContext(Dispatchers.IO) {
            try {
                val conn = URL(fetched.photoUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode == 200) {
                    val bmp = BitmapFactory.decodeStream(conn.inputStream)
                    if (bmp != null) {
                        withContext(Dispatchers.Main) {
                            remoteBitmap = bmp
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(currentTheme.cardBorderWidth, currentTheme.cardBorderColor), currentTheme.cardShape),
        shape = currentTheme.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = currentTheme.cardElevation),
        colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(currentTheme.surfaceLight)
                .padding(14.dp)
        ) {
            // Header: Sacred Title + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(currentTheme.primaryColor.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, currentTheme.primaryColor.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌺", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHindi) "आज का पावन अलौकिक दर्शन" else "Today's Sacred Darshan",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = currentTheme.primaryColor
                        )
                        Text(
                            text = darshanData.dateHindi,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentTheme.isDark) Color(0xFF8696A0) else Color(0xFF667781)
                        )
                    }
                }

                // Devotee count tag
                Surface(
                    color = currentTheme.primaryColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, currentTheme.primaryColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "👁️ ${darshanData.viewsCount}+ भक्त",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sacred Deity Image Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(BorderStroke(1.dp, currentTheme.cardBorderColor), RoundedCornerShape(14.dp))
                    .clickable { showZoomDialog = true }
            ) {
                if (remoteBitmap != null) {
                    Image(
                        bitmap = remoteBitmap!!.asImageBitmap(),
                        contentDescription = "श्री बालाजी अलौकिक श्रृंगार दर्शन",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "श्री बालाजी अलौकिक श्रृंगार दर्शन",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(currentTheme.primaryColor.copy(alpha = 0.2f))
                            .padding(16.dp)
                    )
                }

                // Bottom gradient with caption & zoom hint
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚩 ॥ श्री हनुमते नमः ॥",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "🔍 स्पर्श कर बड़ा देखें",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Auspicious Chaupai / Blessing
            Surface(
                color = if (currentTheme.isDark) Color(0xFF202C33) else Color(0xFFF7F8FA),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.8.dp, currentTheme.cardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✨", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = darshanData.quote,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (currentTheme.isDark) Color(0xFFD1D7DB) else Color(0xFF3B4A54),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Zoom & WhatsApp Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showZoomDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = currentTheme.buttonShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryColor),
                    border = BorderStroke(1.dp, currentTheme.primaryColor)
                ) {
                    Text(
                        text = if (isHindi) "🔍 दर्शन बड़ा करें" else "Zoom Darshan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        DailyDarshanHelper.shareDarshanOnWhatsApp(
                            context = context,
                            darshan = darshanData,
                            bitmap = remoteBitmap
                        )
                    },
                    modifier = Modifier.weight(1.3f),
                    shape = currentTheme.buttonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHindi) "WhatsApp शेयर" else "Share Darshan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Full Screen Zoom Dialog
    if (showZoomDialog) {
        DailyDarshanZoomDialog(
            darshanData = darshanData,
            bitmap = remoteBitmap,
            onDismiss = { showZoomDialog = false },
            onShare = {
                DailyDarshanHelper.shareDarshanOnWhatsApp(
                    context = context,
                    darshan = darshanData,
                    bitmap = remoteBitmap
                )
            }
        )
    }
}

@Composable
fun DailyDarshanZoomDialog(
    darshanData: DailyDarshanData,
    bitmap: Bitmap?,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0000000))
        ) {
            // Top Bar with Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🚩 श्री बालाजी अलौकिक दर्शन",
                        color = Color(0xFFFFD700),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = darshanData.dateHindi,
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Text("✕", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Central Zoomable Deity Image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 80.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "दर्शन",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "दर्शन",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }
            }

            // Bottom Floating Bar: Blessing Quote & WhatsApp Share Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\"${darshanData.quote}\"",
                    color = Color(0xFFFFE082),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("💬", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WhatsApp पर यह पावन दर्शन शेयर करें",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
