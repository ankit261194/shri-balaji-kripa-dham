package com.example.shribalajikripadham.ui.info

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AshramTravelGuideScreen(
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val ashramAddressHindi = "श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर (उत्तर प्रदेश) - 202394"
    val ashramAddressEnglish = "Shri Balaji Kripa Dham, Gram Dungra Jaat, Tehsil Anupshahr, Dist. Bulandshahr (U.P.) - 202394"
    val ashramLat = 28.3972915
    val ashramLng = 78.1460410

    fun openGoogleMaps() {
        try {
            val gmmIntentUri = Uri.parse("geo:$ashramLat,$ashramLng?q=$ashramLat,$ashramLng(श्री+बालाजी+कृपा+धाम+डूँगरा+जाट)")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$ashramLat,$ashramLng"))
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$ashramLat,$ashramLng"))
            context.startActivity(webIntent)
        }
    }

    fun copyAddressToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Ashram Address", if (isHindi) ashramAddressHindi else ashramAddressEnglish)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            context,
            if (isHindi) "📋 आश्रम का पूरा पता कॉपी हो गया है!" else "📋 Address copied to clipboard!",
            Toast.LENGTH_SHORT
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "🗺️ आश्रम कैसे पहुँचें?" else "🗺️ How to Reach Ashram?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isHindi) "सटीक मार्ग, रेलवे स्टेशन व शहर गाइड" else "Smart Route & Station Guide",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "⬅", color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaroonAccent)
            )
        },
        containerColor = SacredBackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            // 1. HERO CARD: SACRED DESTINATION & MAPS SHORTCUT
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.2.dp, GoldDark.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(SaffronPrimary, GoldDark)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚩", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "श्री बालाजी कृपा धाम" else "Shri Balaji Kripa Dham",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaroonAccent
                            )
                            Text(
                                text = if (isHindi) "परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार" else "Param Pujya Guruji Tejveer Singh Ji",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SaffronDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = Color(0xFFFFF8E7),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldDark.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isHindi) "📍 पावन स्थल का पूरा पता:" else "📍 Full Address:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaroonPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHindi) ashramAddressHindi else ashramAddressEnglish,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryDark,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { openGoogleMaps() },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text(
                                text = if (isHindi) "🗺️ गूगल मैप्स नेविगेशन" else "🗺️ Start Navigation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = { copyAddressToClipboard() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaroonAccent),
                            border = BorderStroke(1.2.dp, MaroonAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text(
                                text = if (isHindi) "📋 पता कॉपी" else "📋 Copy Address",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. NEAREST RAILWAY STATION CARD (STRICTLY ONLY BULANDSHAHR)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.5.dp, Color(0xFF0288D1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE1F5FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚆", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isHindi) "नजदीकी रेलवे स्टेशन (एकमात्र स्टेशन)" else "Nearest Railway Station (Only Station)",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0277BD)
                            )
                            Text(
                                text = if (isHindi) "बुलन्दशहर रेलवे स्टेशन (BSC)" else "Bulandshahr Railway Station (BSC)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaroonAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color(0xFFF1F8E9),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isHindi)
                                    "⚠️ महत्वपूर्ण सूचना: आश्रम का निकटतम एकमात्र रेलवे स्टेशन 'बुलन्दशहर' है (दूरी लगभग 28-30 किमी)। इसके अलावा कोई अन्य निकटवर्ती स्टेशन नहीं है।"
                                else
                                    "⚠️ Important Notice: The ONLY nearest railway station to the Ashram is 'Bulandshahr' (~28-30 km).",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32),
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isHindi)
                            "• ट्रेन द्वारा आने वाले भक्त दिल्ली, हापुड़, गाजियाबाद या मेरठ मार्ग से 'बुलन्दशहर रेलवे स्टेशन' पर उतरें।\n• बुलन्दशहर रेलवे स्टेशन या बस स्टैंड से जहांगीराबाद के लिए नियमित बसें व ऑटो/टैक्सी 24 घंटे उपलब्ध रहती हैं।\n• जहांगीराबाद पहुँचकर वहाँ से सीधे ग्राम डूँगरा जाट आश्रम के लिए लोकल सवारी तुरंत मिल जाती है।"
                        else
                            "• Devotees traveling by train should deboard at Bulandshahr Railway Station (BSC).\n• Frequent buses and taxis connect Bulandshahr to Jahangirabad, from where local e-rickshaws reach Dungra Jaat Ashram directly.",
                        fontSize = 12.5.sp,
                        color = TextPrimaryDark,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. NEARBY CITIES & HUBS (STRICTLY ONLY JAHANGIRABAD, BULANDSHAHR, ANOOPSHAHR)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.2.dp, SaffronPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF3E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏙️", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isHindi) "निकटवर्ती प्रमुख 3 शहर व मार्ग विवरण" else "Nearby 3 Cities & Route Details",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaroonAccent
                            )
                            Text(
                                text = if (isHindi) "जहांगीराबाद • बुलन्दशहर • अनूपशहर" else "Jahangirabad • Bulandshahr • Anoopshahr",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SaffronDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // CITY 1: JAHANGIRABAD
                    CityRouteCard(
                        number = "1",
                        cityName = if (isHindi) "जहांगीराबाद (Jahangirabad)" else "Jahangirabad",
                        tag = if (isHindi) "निकटतम प्रमुख नगर (~10 किमी)" else "Nearest Town (~10 km)",
                        tagColor = Color(0xFF2E7D32),
                        tagBg = Color(0xFFE8F5E9),
                        description = if (isHindi)
                            "• यह आश्रम का सबसे नजदीकी कस्बा व प्रमुख बाज़ार है।\n• जहांगीराबाद बस स्टैंड / मुख्य चौराहे से ग्राम डूँगरा जाट आश्रम के लिए नियमित ई-रिक्शा, ऑटो एवं डग्गामार वाहन उपलब्ध रहते हैं (समय: 10-15 मिनट)।"
                        else
                            "• The closest town & market to the Ashram (~10 km).\n• Local e-rickshaws and autos run regularly from Jahangirabad bus stand/crossroad directly to Dungra Jaat Ashram (10-15 mins)."
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // CITY 2: BULANDSHAHR
                    CityRouteCard(
                        number = "2",
                        cityName = if (isHindi) "बुलन्दशहर (Bulandshahr)" else "Bulandshahr",
                        tag = if (isHindi) "जिला मुख्यालय व रेलवे स्टेशन (~30 किमी)" else "District HQ & Railway Station (~30 km)",
                        tagColor = Color(0xFF0277BD),
                        tagBg = Color(0xFFE1F5FE),
                        description = if (isHindi)
                            "• जिला मुख्यालय एवं एकमात्र रेलवे स्टेशन।\n• दिल्ली, नोएडा, गाजियाबाद, मेरठ से आने वाली सभी बसें व ट्रेनें बुलन्दशहर आती हैं।\n• बुलन्दशहर (भूड़ चौराहा / बस स्टैंड) से जहांगीराबाद की बस/टैक्सी लें। जहांगीराबाद से लोकल सवारी सीधे आश्रम पहुँचा देती है।"
                        else
                            "• District headquarters and only railway hub (~30 km).\n• Buses from Delhi, Noida, Ghaziabad, Meerut arrive here. Take bus to Jahangirabad, then local ride to Dungra Jaat Ashram."
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // CITY 3: ANOOPSHAHR
                    CityRouteCard(
                        number = "3",
                        cityName = if (isHindi) "अनूपशहर (Anoopshahr)" else "Anoopshahr",
                        tag = if (isHindi) "पवित्र गंगा तट (~16 किमी)" else "Holy Ganga Ghat (~16 km)",
                        tagColor = Color(0xFFE65100),
                        tagBg = Color(0xFFFFF3E0),
                        description = if (isHindi)
                            "• पवित्र गंगा स्नान कर आने वाले भक्त अनूपशहर से आते हैं।\n• अनूपशहर (गंगा घाट / बस स्टैंड) से जहांगीराबाद मार्ग होते हुए ग्राम डूँगरा जाट आश्रम 20-25 मिनट में आसानी से पहुँचा जा सकता है।"
                        else
                            "• Sacred Ganga Ghat town (~16 km).\n• Reached in 20-25 mins via the Jahangirabad road directly to Dungra Jaat Ashram."
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. PRIVATE VEHICLES, PARKING & SUNDAY ADVISORY
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, GoldDark.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚗", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isHindi) "निजी वाहन, पार्किंग व रविवार परामर्श" else "Parking & Sunday Rush Advisory",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaroonAccent
                            )
                            Text(
                                text = if (isHindi) "विशाल निःशुल्क पार्किंग सुविधा" else "Spacious Free Parking Available",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isHindi)
                            "• दोपहिया (बाइक/स्कूटर) और चार-पहिया (कार/ट्रैक्टर/बस) वाहनों के लिए आश्रम परिसर में विशाल निःशुल्क पार्किंग की समुचित व्यवस्था है।\n• रविवार दरबार में अत्यधिक रश होने के कारण भक्तों से अनुरोध है कि प्रातःकाल 8:00 बजे से पूर्व ही आश्रम पहुँचने का प्रयास करें ताकि टोकन और दर्शन निर्विघ्न संपन्न हो सकें।\n• आश्रम में किसी भी सेवा, अर्जी या इलाज का कोई शुल्क नहीं है। समस्त व्यवस्था पूर्णतः निःशुल्क (FREE) है।"
                        else
                            "• Free parking is available for two-wheelers, four-wheelers, and buses inside the Ashram.\n• Due to heavy rush on Sundays, devotees are advised to reach before 8:00 AM.\n• All spiritual services, Darshan, and tokens are 100% FREE.",
                        fontSize = 12.5.sp,
                        color = TextPrimaryDark,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { openGoogleMaps() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isHindi) "📍 आश्रम के लिए नेविगेशन चालू करें" else "📍 Navigate to Ashram Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CityRouteCard(
    number: String,
    cityName: String,
    tag: String,
    tagColor: Color,
    tagBg: Color,
    description: String
) {
    Surface(
        color = Color(0xFFFAFAFA),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaroonAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(number, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cityName,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonAccent
                    )
                }

                Surface(
                    color = tagBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = tagColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = TextPrimaryDark,
                lineHeight = 17.5.sp
            )
        }
    }
}
