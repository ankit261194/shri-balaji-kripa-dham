package com.example.shribalajikripadham.ui.live

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import com.example.shribalajikripadham.service.BhajanAudioService
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class SacredTrack(
    val titleHindi: String,
    val titleEnglish: String,
    val subtitleHindi: String,
    val durationText: String,
    val audioUrl: String,
    val youtubeSearchQuery: String,
    val lyricsHindi: String
)

val SACRED_TRACKS = listOf(
    SacredTrack(
        titleHindi = "श्री हनुमान चालीसा",
        titleEnglish = "Shri Hanuman Chalisa",
        subtitleHindi = "जय हनुमान ज्ञान गुन सागर • संकट कटे मिटे सब पीरा",
        durationText = "09:42",
        audioUrl = "https://shribalajikripadham.online/api/stream_audio.php?track=hanuman_chalisa",
        youtubeSearchQuery = "Shri Hanuman Chalisa Gulshan Kumar Hariharan",
        lyricsHindi = """
            ॥ दोहा ॥
            श्रीगुरु चरन सरोज रज निज मनु मुकुरु सुधारि।
            बरनऊं रघुबर बिमल जसु जो दायकु फल चारि॥
            बुद्धिहीन तनु जानिके सुमिरौं पवन-कुमार।
            बल बुद्धि बिद्या देहु मोहिं हरहु कलेस बिकार॥

            ॥ चौपाई ॥
            जय हनुमान ज्ञान गुन सागर। जय कपीस तिहुं लोक उजागर॥
            राम दूत अतुलित बल धामा। अंजनि-पुत्र पवनसुत नामा॥
            महाबीर बिक्रम बजरंगी। कुमति निवार सुमति के संगी॥
            कंचन बरन बिराज सुबेसा। कानन कुंडल कुंचित केसा॥
            हाथ बज्र औ ध्वजा बिराजै। कांधे मूंज जनेऊ साजै॥
            संकर सुवन केसरीनंदन। तेज प्रताप महा जग बन्दन॥
            बिद्यावान गुनी अति चातुर। राम काज करिबे को आतुर॥
            प्रभु चरित्र सुनिबे को रसिया। राम लखन सीता मन बसिया॥
            सूक्ष्म रूप धरि सियहिं दिखावा। बिकट रूप धरि लंक जरावा॥
            भीम रूप धरि असुर संहारे। रामचंद्र के काज संवारे॥
            लाय सजीवन लखन जियाये। श्रीरघुबीर हरषि उर लाये॥
            रघुपति कीन्ही बहुत बड़ाई। तुम मम प्रिय भरतहि सम भाई॥
            सहस बदन तुम्हरो जस गावैं। अस कहि श्रीपति कंठ लगावैं॥
            सनकादिक ब्रह्मादि मुनीसा। नारद सारद सहित अहीसा॥
            जम कुबेर दिगपाल जहां ते। कबि कोबिद कहि सके कहां ते॥
            तुम उपकार सुग्रीवहिं कीन्हा। राम मिलाय राज पद दीन्हा॥
            तुम्हरो मंत्र बिभीषन माना। लंकेस्वर भए सब जग जाना॥
            जुग सहस्र जोजन पर भानू। लील्यो ताहि मधुर फल जानू॥
            प्रभु मुद्रिका मेलि मुख माहीं। जलधि लांघि गये अचरज नाहीं॥
            दुर्गम काज जगत के जेते। सुगम अनुग्रह तुम्हरे तेते॥
            राम दुआरे तुम रखवारे। होत न आज्ञा बिनु पैसारे॥
            सब सुख लहै तुम्हारी सरना। तुम रक्षक काहू को डर ना॥
            आपन तेज सम्हारो आपै। तीनों लोक हांक तें कांपै॥
            भूत पिसाच निकट नहिं आवै। महाबीर जब नाम सुनावै॥
            नासै रोग हरै सब पीरा। जपत निरंतर हनुमत बीरा॥
            संकट तें हनुमान छुड़ावै। मन क्रम बचन ध्यान जो लावै॥
            सब पर राम तपस्वी राजा। तिन के काज सकल तुम साजा॥
            और मनोरथ जो कोई लावै। सोइ अमित जीवन फल पावै॥
            चारों जुग परताप तुम्हारा। है परसिद्ध जगत उजियारा॥
            साधु-संत के तुम रखवारे। असुर निकंदन राम दुलारे॥
            अष्ट सिद्धि नौ निधि के दाता। अस बर दीन जानकी माता॥
            राम रसायन तुम्हरे पासा। सदा रहो रघुपति के दासा॥
            तुम्हरे भजन राम को पावै। जनम-जनम के दुख बिसरावै॥
            अन्तकाल रघुबर पुर जाई। जहां जन्म हरि-भक्त कहाई॥
            और देवता चित्त न धरई। हनुमत सेइ सर्ब सुख करई॥
            संकट कटै मिटै सब पीरा। जो सुमिरै हनुमत बलबीरा॥
            जै जै जै हनुमान गोसाईं। कृपा करहु गुरुदेव की नाईं॥
            जो सत बार पाठ कर कोई। छूटहि बंदि महा सुख होई॥
            जो यह पढ़ै हनुमान चालीसा। होय सिद्धि साखी गौरीसा॥
            तुलसीदास सदा हरि चेरा। कीजै नाथ हृदय मंह डेरा॥

            ॥ दोहा ॥
            पवन तनय संकट हरन मंगल मूरति रूप।
            राम लखन सीता सहित हृदय बसहु सुर भूप॥
        """.trimIndent()
    ),
    SacredTrack(
        titleHindi = "श्री बालाजी महाआरती (डूँगरा जाट)",
        titleEnglish = "Shri Balaji Maha Aarti",
        subtitleHindi = "आरती कीजै श्री बालाजी की • कलिकाल में मंगलकारी",
        durationText = "06:15",
        audioUrl = "https://shribalajikripadham.online/api/stream_audio.php?track=balaji_aarti",
        youtubeSearchQuery = "Shri Balaji Aarti Kije Hanuman Lala Ki",
        lyricsHindi = """
            ॥ श्री बालाजी कृपा धाम पावन महाआरती ॥

            आरती कीजै श्री बालाजी की। कलिकाल में संकट हरने की॥
            डूँगरा जाट विराजे देवा। भक्त जन नित करहिं सेवा॥

            शीश मुकुट कुंडल छवि भारी। गदा हाथ में असुर संहारी॥
            लाल लंगोटा लाल सिंदूरा। राम काज सब कीन्हे पूरा॥

            अर्जी जो दरबार लगावै। मनवांछित सोई फल पावै॥
            भूत पिशाच निकट नहिं आवैं। बालाजी का नाम सुनावैं॥

            झाड़ा लगे कटे सब रोगा। कृपा करहु प्रभु दीन दयाला॥
            निशुल्क सेवा धाम तुम्हारा। सब भक्तों का तू रखवारा॥

            आरती कीजै श्री बालाजी की। डूँगरा जाट के नाथ हमारे की॥
            बोलिए श्री बालाजी महाराज की जय!
        """.trimIndent()
    ),
    SacredTrack(
        titleHindi = "बजरंग बाण",
        titleEnglish = "Bajrang Baan",
        subtitleHindi = "निश्चय प्रेम प्रतीति ते बिनय करै सनमान",
        durationText = "07:30",
        audioUrl = "https://shribalajikripadham.online/api/stream_audio.php?track=bajrang_baan",
        youtubeSearchQuery = "Bajrang Baan Rasraj Ji",
        lyricsHindi = """
            ॥ दोहा ॥
            निश्चय प्रेम प्रतीति ते, बिनय करैं सनमान।
            तेहि के कारज सकल शुभ, सिद्ध करैं हनुमान॥

            ॥ चौपाई ॥
            जय हनुमंत संत हितकारी। सुन लीजै प्रभु अरज हमारी॥
            जन के काज बिलंब न कीजै। आतुर दौरि महा सुख दीजै॥
            जैसे कूदि सिंधु महिपारा। सुरसा बदन पैठि बिस्तारा॥
            आगे जाय लंकिनी रोका। मारेहु लात गई सुर लोका॥
            जाय बिभीषन को सुख दीन्हा। सीता निरखि परमपद लीन्हा॥
            बाग उजारि सिंधु महं बोरा। अति आतुर जमकातर तोरा॥
            अक्षय कुमार मारि संहारा। लूम लपेटि लंक को जारा॥
            लाह समान लंक जरि गई। जय जय धुनि सुरपुर नभ भई॥
            अब बिलंब केहि कारन स्वामी। कृपा करहु उर अंतरयामी॥
            जय जय लखन प्रान के दाता। आतुर ह्वै दुख हरहु निपाता॥
            जै हनुमान जयति बल-सागर। सुर-समूह-समरथ भट-नागर॥
            ॐ हनु हनु हनु हनुमंत हठीले। बैरिहि मारु बज्र की कीले॥
            गदा बज्र लै बैरिहिं मारो। महाराज प्रभु दास उबारो॥
            ॐ ह्रीं ह्रीं ह्रीं हनुमंत कपीसा। ॐ हुं हुं हुं हनु अरि उर सीसा॥
            सत्य होहु हरि सपथ पाइके। राम दूत धरू मारु धाइके॥
            जय जय जय हनुमंत अगाधा। दुख पावत जन केहि अपराधा॥
            पूजा जप तप नेम अचारा। नहिं जानत कछु दास तुम्हारा॥
            बन उपबन मग गिरि गृह माहीं। तुम्हरे बल हम डरपत नाहीं॥
            जनकसुता हरि दास कहावो। ताकी सपथ बिलंब न लावो॥
            जै जै जै धुनि होत अकासा। सुमिरत होय दुसह दुख नासा॥
            चरन पकरि कर जोरि मनावौं। यहि औसर अब केहि गोहरावौं॥
            उठु उठु चलु तोहि राम दोहाई। पांय परौं कर जोरि मनाई॥
            ॐ चं चं चं चं चपल चलंता। ॐ हनु हनु हनु हनु हनुमंता॥
            ॐ हं हं हांक देत कपि चंचल। ॐ सं सं सहमि पराने खल-दल॥
            अपने जन को तुरत उबारो। सुमिरत होय आनंद अपारो॥
            यह बजरंग बाण जेहि मारै। ताहि कहौ फिरि कौन उबारै॥
            पाठ करै बजरंग बाण की। हनुमत रक्षा करै प्रान की॥
            यह बजरंग बाण जो जापै। ताते भूत-प्रेत सब कांपै॥
            धूप देय अरु जपै हमेशा। ताके तन नहिं रहै कलेसा॥

            ॥ दोहा ॥
            उर प्रतीति दृढ़, सरन ह्वै, पाठ करै धरि ध्यान।
            बाधा सब हर, करैं सब काम सफल हनुमान॥
        """.trimIndent()
    ),
    SacredTrack(
        titleHindi = "संकट मोचन हनुमानाष्टक",
        titleEnglish = "Sankat Mochan Hanumanashtak",
        subtitleHindi = "बाल समय रवि भक्ष लियो तब तीनहुं लोक भयो अंधियारों",
        durationText = "05:48",
        audioUrl = "https://shribalajikripadham.online/api/stream_audio.php?track=sankatmochan",
        youtubeSearchQuery = "Sankat Mochan Hanuman Ashtak Hariharan",
        lyricsHindi = """
            बाल समय रवि भक्ष लियो तब, तीनहुं लोक भयो अंधियारों।
            ताहि सों त्रास भयो जग को, यह संकट काहु सों जात न टारो।
            देवन आनि करी बिनती तब, छांड़ि दियो रवि कष्ट निवारो।
            को नहिं जानत है जग में कपि, संकटमोचन नाम तिहारो॥ १ ॥

            बालि की त्रास कपीस बसै गिरि, जात महाप्रभु पंथ निहारो।
            चौंकि महामुनि साप दियो तब, चाहिय कौन बिचार बिचारो।
            कैद्विज रूप लिवाय महाप्रभु, सो तुम दास के सोक निवारो।
            को नहिं जानत है जग में कपि, संकटमोचन नाम तिहारो॥ २ ॥

            अंगद के संग लेन गए सिय, खोज कपीस यह बैन उचारो।
            जीवत ना बचिहौ हम सो जु, बिना सुधि लाये इहां पगु धारो।
            हेरी थके तट सिंधु सबे तब, लाय सिया-सुधि प्रान उबारो।
            को नहिं जानत है जग में कपि, संकटमोचन नाम तिहारो॥ ३ ॥

            रावन त्रास दई सिय को सब, राक्षसि सों कहि सोक निवारो।
            ताहि समय हनुमान महाप्रभु, जाय महा रजनीचर मारो।
            चाहत सीय असोक सों आगि सु, दै बनि लंक जलाइ उबारो।
            को नहिं जानत है जग में कपि, संकटमोचन नाम तिहारो॥ ४ ॥

            बान लग्यो उर लछिमन के तब, प्रान तजे सुत रावन मारो।
            लै गृह बैद्य सुषेन समेत, तबै गिरि द्रोन सु बीर उपारो।
            आनि सजीवन हाथ दई तब, लछिमन के तुम प्रान उबारो।
            को नहिं जानत है जग में कपि, संकटमोचन नाम तिहारो॥ ५ ॥

            रावन जुद्ध अजान कियो तब, नाग कि फांस सबै सिर डारो।
            श्रीरघुनाथ समेत सबे दल, मोह भयो यह संकट भारो।
            आनि खगेस तबै हनुमान जु, बंधन काटि सुत्रास निवारो।
            को नहिं जानत है जग में कपि, संकटमोचन नाम तिहारो॥ ६ ॥

            बंधु समेत जबै अहिरावन, लै रघुनाथ पतार सिधारो।
            देबिहि पूजि भली बिधि सों बलि, देउ सबै मिलि मंत्र बिचारो।
            जाय सहाय भयो तब ही, अहिरावन सैन्य समेत संहारो।
            को नहिं जानत है जग में कपि, संकटमोचन नाम तिहारो॥ ७ ॥

            काज किये बड़ देवन के तुम, बीर महाप्रभु देखि बिचारो।
            कौन सो संकट मोर गरीब को, जो तुमसों नहिं जात है टारो।
            बेगि हरो हनुमान महाप्रभु, जो कछु संकट होय हमारो।
            को नहिं जानत है जग में कपि, संकटमोचन नाम तिहारो॥ ८ ॥

            ॥ दोहा ॥
            लाल देह लाली लसे, अरु धरि लाल लंगूर।
            बज्र देह दानव दलन, जय जय जय कपि सूर॥
        """.trimIndent()
    ),
    SacredTrack(
        titleHindi = "आरती कीजै हनुमान लला की",
        titleEnglish = "Aarti Kije Hanuman Lala Ki",
        subtitleHindi = "दुष्ट दलन रघुनाथ कला की • जाके बल से गिरिवर कांपै",
        durationText = "05:12",
        audioUrl = "https://shribalajikripadham.online/api/stream_audio.php?track=aarti_kije",
        youtubeSearchQuery = "Aarti Kije Hanuman Lala Ki Anuradha Paudwal",
        lyricsHindi = """
            आरती कीजै हनुमान लला की। दुष्ट दलन रघुनाथ कला की॥

            जाके बल से गिरिवर कांपै। रोग दोष जाके निकट न झांपै॥
            अंजनि पुत्र महाबलदाई। संतन के प्रभु सदा सहाई॥

            दे बीरा रघुनाथ पठाए। लंका जारि सीय सुधि लाए॥
            लंका सो कोट समुद्र सी खाई। जात पवनसुत बार न लाई॥

            लंका जारि असुर संहारे। सियारामजी के काज संवारे॥
            लक्ष्मण मूर्छित पड़े सकारे। आनि संजीवन प्रान उबारे॥

            पैठि पताल तोरि जम-कारे। अहिरावन की भुजा उखारे॥
            बाएं भुजा असुर दल मारे। दहिने भुजा संतजन तारे॥

            सुर नर मुनि आरती उतारैं। जय जय जय हनुमान उचारैं॥
            कंचन थार कपूर लौ छाई। आरति करत अंजना माई॥

            जो हनुमानजी की आरति गावै। बसि बैकुंठ परम पद पावै॥
            आरती कीजै हनुमान लला की। दुष्ट दलन रघुनाथ कला की॥
        """.trimIndent()
    ),
    SacredTrack(
        titleHindi = "श्री रामचन्द्र कृपालु भजु मन",
        titleEnglish = "Shri Ramachandra Kripalu",
        subtitleHindi = "हरन भवभय दारुणं • नवकंज लोचन कंज मुख",
        durationText = "06:35",
        audioUrl = "https://shribalajikripadham.online/api/stream_audio.php?track=ram_stuti",
        youtubeSearchQuery = "Shri Ramchandra Kripalu Bhajuman Lata Mangeshkar",
        lyricsHindi = """
            श्रीरामचन्द्र कृपालु भजु मन हरण भवभय दारुणं।
            नवकंज लोचन, कंज मुख, कर कंज, पद कंजारुणं॥ १ ॥

            कंदर्प अगणित अमित छबि, नवनील नीरद सुन्दरं।
            पट पीत मानहु तड़ित रुचि शुचि नौमि जनक सुतावरं॥ २ ॥

            भजु दीनबंधु दिनेश दानव दैत्य वंश निकन्दनं।
            रघुनन्द आनंदकंद कोशलचन्द दशरथ नन्दनं॥ ३ ॥

            सिर मुकुट कुंडल तिलक चारु उदारु अंग विभूषणं।
            आजानुभुज शर चाप धर, संग्राम जित खर दूषणं॥ ४ ॥

            इति वदति तुलसीदास शंकर शेष मुनि मन रंजनं।
            मम हृदय कंज निवास कुरु, कामादि खल दल गंजनं॥ ५ ॥

            मनु जाहिं राचेउ मिलहि सो बरु सहज सुंदर सांवरो।
            करुना निधान सुजान सीलु सनेहु जानत रावरो॥
            एहि भांति गौरि असीस सुनि सिय सहित हियं हरषीं अली।
            तुलसी भवानिहि पूजि पुनि पुनि मुदित मन मंदिर चली॥
        """.trimIndent()
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDarbarAndBhajanScreen(
    isHindi: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    var ashramSettings by remember { mutableStateOf(AshramSettings()) }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Live Video, 1: Sacred Audio

    // Foreground Media Service State Binding
    val svcTrackIndex by BhajanAudioService.currentTrackIndex.collectAsState()
    val isPlaying by BhajanAudioService.isPlaying.collectAsState()
    val isBuffering by BhajanAudioService.isBuffering.collectAsState()
    val currentPositionMs by BhajanAudioService.currentPositionMs.collectAsState()
    val totalDurationMs by BhajanAudioService.durationMs.collectAsState()

    var currentTrackIndex by remember { mutableIntStateOf(0) }
    var isLooping by remember { mutableStateOf(false) }
    var playbackErrorMessage by remember { mutableStateOf<String?>(null) }
    var showLyricsDialog by remember { mutableStateOf<SacredTrack?>(null) }

    LaunchedEffect(svcTrackIndex) {
        if (svcTrackIndex in SACRED_TRACKS.indices) {
            currentTrackIndex = svcTrackIndex
        }
    }

    LaunchedEffect(Unit) {
        ashramSettings = repository.getSettings()
    }

    fun playTrack(index: Int) {
        try {
            playbackErrorMessage = null
            currentTrackIndex = index
            val track = SACRED_TRACKS[index]
            BhajanAudioService.playTrack(
                context = context,
                trackIndex = index,
                title = if (isHindi) track.titleHindi else track.titleEnglish,
                artist = "श्री बालाजी कृपा धाम (डूँगरा जाट)",
                audioUrl = track.audioUrl
            )
        } catch (e: Exception) {
            playbackErrorMessage = e.localizedMessage
        }
    }

    fun togglePlayPause() {
        if (svcTrackIndex != currentTrackIndex) {
            playTrack(currentTrackIndex)
        } else {
            BhajanAudioService.togglePlayPause(context)
        }
    }

    fun formatTime(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }

    fun openTrackInYouTube(track: SacredTrack) {
        val query = track.youtubeSearchQuery
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
            setPackage("com.google.android.youtube")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "श्री बालाजी कृपा धाम (डूँगरा जाट)" else "Shri Balaji Kripa Dham",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
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
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaroonPrimary.copy(alpha = 0.95f),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(pulseScale)
                                    .background(Color.Red, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "🔴 लाइव दर्शन" else "Live Darbar",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTabIndex == 0) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎵", fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "आरती व भजन" else "Aarti & Bhajans",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTabIndex == 1) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                )
            }

            if (selectedTabIndex == 0) {
                // ============================================================
                // TAB 0: LIVE STREAM / DARBAR VIDEO IN-APP
                // ============================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val rawChannelUrl = ashramSettings.youtubeChannelUrl.trim().ifEmpty {
                        "https://www.youtube.com/@ShriBalajiKripaDham"
                    }
                    val liveVideoUrl = if (rawChannelUrl.contains("/@")) "$rawChannelUrl/live" else rawChannelUrl
                    val isDarbarLive = ashramSettings.isDarbarActive && ashramSettings.isDarbarLiveNow
                    var showInAppPlayer by remember { mutableStateOf(false) }

                    if (isDarbarLive || showInAppPlayer) {
                        // 🟢 IN-APP HIGH-PERFORMANCE VIDEO PLAYER
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            @SuppressLint("SetJavaScriptEnabled")
                                            val webConfig = this.settings
                                            webConfig.javaScriptEnabled = true
                                            webConfig.domStorageEnabled = true
                                            webConfig.mediaPlaybackRequiresUserGesture = false
                                            webConfig.loadWithOverviewMode = true
                                            webConfig.useWideViewPort = true
                                            webChromeClient = WebChromeClient()
                                            webViewClient = object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                                    return false
                                                }
                                            }
                                            loadUrl(liveVideoUrl)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // In-App player controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showInAppPlayer = false },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isHindi) "वीडियो बंद करें" else "Close Video", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(liveVideoUrl)).apply {
                                        setPackage("com.google.android.youtube")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(liveVideoUrl)))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC0000)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text(if (isHindi) "▶ YouTube में खोलें" else "▶ Open in YouTube", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    } else {
                        // 🪔 DIVINE DARBAR CARD: With Instant In-App Video Play Button!
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            border = BorderStroke(1.5.dp, SaffronPrimary)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = { showInAppPlayer = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("📺 ऐप के अंदर लाइव व पावन दर्शन वीडियो चलाएं", fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Spacer(Modifier.height(14.dp))

                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(Brush.radialGradient(listOf(Color(0xFFFFECB3), SaffronPrimary.copy(alpha = 0.3f))))
                                        .border(2.dp, SaffronPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🚩", fontSize = 36.sp)
                                }

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    text = if (isHindi) "॥ श्री बालाजी महाराज पावन दिव्य दरबार ॥" else "॥ Shri Balaji Maharaj Darbar ॥",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaroonPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (isHindi) "डूँगरा जाट, बुलन्दशहर • परम पूज्य गुरुजी तेजवीर सिंह जी" else "Dungra Jaat, Bulandshahr • Pujya Guruji",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(12.dp))

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFFFF3E0),
                                    border = BorderStroke(1.dp, SaffronPrimary)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⏳", fontSize = 14.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "वर्तमान में लाइव दर्शन विश्राम पर हैं" else "Live broadcast currently on recess",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                                    border = BorderStroke(1.dp, Color(0xFFFFF176))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text(
                                            text = if (isHindi) "🪔 दैनिक पावन आरती एवं दरबार समय-सारणी" else "🪔 Daily Aarti & Darbar Timings",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaroonPrimary
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        val timings = listOf(
                                            Pair("🌅 प्रातः मंगला आरती", "प्रातः 05:30 बजे"),
                                            Pair("☀️ दोपहर राजभोग आरती", "दोपहर 12:00 बजे"),
                                            Pair("🌆 सायं संध्या महाआरती", "सायं 07:00 बजे"),
                                            Pair("🚩 रविवार दिव्य दरबार व झाड़ा", "रविवार प्रातः 08:00 बजे से")
                                        )
                                        timings.forEach { (title, time) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(title, fontSize = 12.sp, color = Color(0xFF333333), fontWeight = FontWeight.Medium)
                                                Text(time, fontSize = 12.sp, color = MaroonPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    text = if (isHindi) "दरबार अथवा आरती लाइव शुरू होते ही यह स्क्रीन स्वतः लाइव वीडियो में बदल जाएगी। तब तक आप पावन चालीसा व भजन सुन सकते हैं।"
                                    else "When Live Darbar or Aarti begins, this screen automatically switches to Live video.",
                                    fontSize = 11.5.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )

                                Spacer(Modifier.height(16.dp))

                                Button(
                                    onClick = { selectedTabIndex = 1 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "🎵 पावन चालीसा व भजन सुनें (बैकग्राउंड प्लेयर)" else "🎵 Listen to Sacred Chalisas",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ashramSettings.youtubeChannelUrl.ifEmpty { "https://www.youtube.com/@ShriBalajiKripaDham" })).apply {
                                            setPackage("com.google.android.youtube")
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ashramSettings.youtubeChannelUrl.ifEmpty { "https://www.youtube.com/@ShriBalajiKripaDham" })))
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.2.dp, Color(0xFFCC0000))
                                ) {
                                    Text(
                                        text = if (isHindi) "▶ यूट्यूब चैनल पर पिछले पावन वीडियो देखें" else "▶ Watch Past Videos on YouTube",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFCC0000)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Darbar Status Badge
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ashramSettings.isDarbarActive) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (ashramSettings.isDarbarActive) Color(0xFF4CAF50) else SaffronPrimary
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (ashramSettings.isDarbarActive) "🟢" else "⏳",
                                fontSize = 20.sp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (ashramSettings.isDarbarActive)
                                        (if (isHindi) "पावन दरबार लाइव सक्रिय है" else "Live Darbar is Currently Active")
                                    else
                                        (if (isHindi) "आगामी दरबार की प्रतीक्षा" else "Awaiting Next Darbar"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (ashramSettings.isDarbarActive) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                                Text(
                                    text = ashramSettings.darbarTimings.ifBlank { "प्रत्येक रविवार प्रातः 8:00 बजे से" },
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Divine Ashram Notice
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isHindi) "॥ पावन दर्शन नियम एवं सूचना ॥" else "Darshan Information",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaroonPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (isHindi)
                                    "• रविवार प्रातःकाल 8:00 बजे से पूज्य गुरुजी द्वारा दिव्य दरबार प्रारंभ होता है।\n• घर बैठे भक्तगण लाइव दर्शन व महाआरती का पावन लाभ प्राप्त कर सकते हैं।\n• दरबार में टोकन वाले भक्त अपनी बारी पर ही गुरुजी के समीप पधारें।"
                                else
                                    "• Sunday Darbar starts at 8:00 AM by Pujya Guruji.\n• Devotees from home can watch Live Darbar and Aarti.\n• Token holders please proceed when your number is called.",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                // ============================================================
                // TAB 1: SACRED AUDIO, CHALISA & AARTI PLAYER
                // ============================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Player Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaroonPrimary)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val activeTrack = SACRED_TRACKS[currentTrackIndex]

                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                SaffronPrimary,
                                                Color(0xFFFFD54F),
                                                MaroonPrimary,
                                                SaffronPrimary
                                            )
                                        )
                                    )
                                    .border(3.dp, Color(0xFFFFD54F), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isPlaying) "🕉️" else "🚩",
                                    fontSize = 36.sp,
                                    modifier = if (isPlaying) Modifier.scale(pulseScale) else Modifier
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = if (isHindi) activeTrack.titleHindi else activeTrack.titleEnglish,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = activeTrack.subtitleHindi,
                                fontSize = 12.sp,
                                color = Color(0xFFFFD54F),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(10.dp))

                            // Action buttons: Read Lyrics & Watch on YouTube
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                OutlinedButton(
                                    onClick = { showLyricsDialog = activeTrack },
                                    border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("📖 सम्पूर्ण पाठ पढ़ें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(10.dp))
                                Button(
                                    onClick = { openTrackInYouTube(activeTrack) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC0000)),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("▶ यूट्यूब पर सुनें", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Seek Bar
                            Slider(
                                value = currentPositionMs.toFloat().coerceIn(0f, totalDurationMs.coerceAtLeast(1).toFloat()),
                                onValueChange = { newPos ->
                                    BhajanAudioService.seekTo(context, newPos.toInt())
                                },
                                valueRange = 0f..totalDurationMs.coerceAtLeast(1).toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD54F),
                                    activeTrackColor = Color(0xFFFFD54F),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(currentPositionMs),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = formatTime(totalDurationMs),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // Controls Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { isLooping = !isLooping }) {
                                    Text(
                                        text = "🔁",
                                        fontSize = 20.sp,
                                        color = if (isLooping) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.5f)
                                    )
                                }

                                IconButton(onClick = {
                                    val prevIdx = if (currentTrackIndex - 1 < 0) SACRED_TRACKS.size - 1 else currentTrackIndex - 1
                                    playTrack(prevIdx)
                                }) {
                                    Text(text = "⏮", fontSize = 24.sp, color = Color.White)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFD54F))
                                        .clickable { togglePlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isBuffering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaroonPrimary,
                                            strokeWidth = 3.dp
                                        )
                                    } else {
                                        Text(
                                            text = if (isPlaying) "⏸" else "▶",
                                            fontSize = 24.sp,
                                            color = MaroonPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                IconButton(onClick = {
                                    val nextIdx = (currentTrackIndex + 1) % SACRED_TRACKS.size
                                    playTrack(nextIdx)
                                }) {
                                    Text(text = "⏭", fontSize = 24.sp, color = Color.White)
                                }

                                IconButton(onClick = {
                                    BhajanAudioService.stopPlayback(context)
                                }) {
                                    Text(text = "⏹", fontSize = 20.sp, color = Color.White.copy(alpha = 0.7f))
                                }
                            }

                            if (playbackErrorMessage != null) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = playbackErrorMessage ?: "",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFCC80),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = if (isHindi) "पावन आरतियाँ, चालीसा एवं स्तुतियाँ" else "Sacred Aartis & Chalisas",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonPrimary
                    )

                    Spacer(Modifier.height(8.dp))

                    // Track List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(SACRED_TRACKS) { index, track ->
                            val isCurrent = currentTrackIndex == index
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { playTrack(index) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) Color(0xFFFFF3E0) else Color.White
                                ),
                                border = if (isCurrent) BorderStroke(1.5.dp, SaffronPrimary) else null,
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrent) SaffronPrimary else Color(0xFFF5F5F5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isCurrent && isPlaying) "▶" else "${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color.White else Color.DarkGray,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isHindi) track.titleHindi else track.titleEnglish,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isCurrent) MaroonPrimary else Color.Black
                                        )
                                        Text(
                                            text = track.subtitleHindi,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(onClick = { showLyricsDialog = track }) {
                                        Text("📖", fontSize = 18.sp)
                                    }

                                    IconButton(onClick = { openTrackInYouTube(track) }) {
                                        Text("▶", fontSize = 16.sp, color = Color(0xFFCC0000))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sacred Text / Lyrics Reader Dialog
    if (showLyricsDialog != null) {
        val track = showLyricsDialog!!
        AlertDialog(
            onDismissRequest = { showLyricsDialog = null },
            title = {
                Text(
                    text = track.titleHindi,
                    fontWeight = FontWeight.Bold,
                    color = MaroonPrimary,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = track.lyricsHindi,
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        color = Color(0xFF212121),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLyricsDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonAccent)
                ) {
                    Text("जय श्री बालाजी", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        openTrackInYouTube(track)
                    }
                ) {
                    Text("यूट्यूब पर सुनें ▶")
                }
            }
        )
    }
}
