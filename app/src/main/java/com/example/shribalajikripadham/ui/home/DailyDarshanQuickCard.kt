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
        "दरबार का पावन नियम है कि जो भी व्यक्ति निःस्वार्थ भाव से आश्रम की सेवा, स्वच्छता या भक्तों की सहायता में अपना समय देता है, उस पर बालाजी महाराज की विशेष कृपा बरसती है। सेवा से बड़ा कोई तप नहीं है। अपने जीवन का कुछ अंश समाज और धर्म की सेवा में अवश्य समर्पित करो।",
        "अपने जीवन की बागडोर बालाजी के हाथों में सौंप दो। जैसे एक छोटा बच्चा पिता की उंगली पकड़कर बेखौफ चलता है, वैसे ही तुम भी प्रभु की उंगली पकड़कर चलो। दुनिया तुम्हें गिराने की कोशिश करेगी, लेकिन जब थामने वाले स्वयं महाबली हनुमान हों, तो दुनिया की कोई ताकत तुम्हें पराजित नहीं कर सकती।",
        "कपट और छल से जीती गई बाजी भी हार के समान होती है। प्रभु श्रीराम को सादगी और निष्कपट हृदय सबसे अधिक प्रिय हैं। 'निर्मल मन जन सो मोहि पावा, मोहि कपट छल छिद्र न भावा।' जब तुम भीतर और बाहर से एक जैसे हो जाओगे, तब तुम्हें किसी चमत्कार की खोज नहीं करनी पड़ेगी, चमत्कार स्वयं तुम्हारे द्वार पर होगा।",
        "चिंता चिता के समान होती है जो मनुष्य को भीतर ही भीतर खोखला कर देती है। अपनी चिंताओं को प्रभु के चरणों में अर्पित कर दो और स्वयं केवल कर्म करो। जब तुम ईश्वर के काम में लग जाओगे, तो ईश्वर तुम्हारे सारे रुके हुए काम संवारना शुरू कर देंगे। यह गुरुदेव का आजमाया हुआ सत्य है।",
        "गलती करना मनुष्य का स्वभाव है, लेकिन क्षमा कर देना परमात्मा का गुण है। अगर किसी ने तुम्हारा अहित भी किया हो, तो उसका न्याय बालाजी पर छोड़ दो, अपने मन में बदले की आग मत जलाओ। बदले की भावना सबसे पहले खुद को जलाती है। क्षमा करने से मन हल्का और पवित्र होता है।",
        "पानी की एक-एक बूंद लगातार गिरने से कठोर पत्थर में भी सुराख हो जाता है। इसी प्रकार नित्य किया गया थोड़ा सा भजन, थोड़ी सी प्रार्थना जीवन के बड़े से बड़े संकट को काट देती है। अपनी दैनिक पूजा में निरंतरता रखो, नागा मत करो। नित्य का अभ्यास ही सिद्धि की कुंजी है।",
        "जो मनुष्य अपनी जीभ और अपने विचारों पर नियंत्रण नहीं रख सकता, उसका पतन निश्चित है। अपनी दृष्टि को पवित्र रखो, अपने कानों से केवल शुभ सुनो और अपने पैरों को सदैव धर्म के मार्ग पर बढ़ाओ। इंद्रियों का संयम ही आत्मिक तेज और बल की जननी है।",
        "संसार के घने अंधकार में गुरु ही वह दिव्य दीपक हैं जो ईश्वर तक पहुंचने का सीधा और सुरक्षित मार्ग दिखाते हैं। गुरु के वचनों को कभी साधारण मत समझो; उनके एक-एक शब्द में हजारों संकटों को टालने की शक्ति होती है। गुरु की आज्ञा का पालन करना ही सबसे बड़ा धर्म है।",
        "बाहर से कितने भी सुंदर दिखो, यदि भीतर कुविचार और द्वेष भरा है तो सब व्यर्थ है। आत्मा का सौंदर्य चरित्र की पवित्रता में है। जिस व्यक्ति का चरित्र निर्मल है, उसका मस्तक समाज में सदैव गर्व से ऊंचा रहता है और उस पर देवदूतों की कृपा बरसती है।",
        "मृत्यु, रोग और बदनामी का डर केवल अज्ञानता के कारण होता है। आत्मा अजर-अमर है और प्रभु की संतान है। जब तुम यह जान लोगे कि तुम परमेश्वर के अंश हो, तो तुम्हारे भीतर से हर प्रकार का भय सदा के लिए विदा हो जाएगा। जय श्री राम का उद्घोष ही निर्भयता का आधार है।",
        "जिसके पास संतोष रूपी धन है, वह संसार का सबसे बड़ा राजा है। दूसरों की गाड़ियों और महलों को देखकर कभी हीन भावना मत लाओ। तुम्हारे पास जो दो वक्त की रोटी, स्वस्थ शरीर और बालाजी की कृपा है, वह करोड़ों लोगों के लिए एक सपना है। प्रभु का सदैव कृतज्ञ बनो।",
        "संसार के मित्र केवल सुख के साथी होते हैं, दुख आते ही किनारा कर लेते हैं। लेकिन बालाजी महाराज ऐसे सखा हैं जो जब पूरी दुनिया छोड़ देती है, तब आकर बांह पकड़ते हैं। ऐसे कृपालु स्वामी को छोड़कर और किसकी शरण में जाओगे?",
        "कलयुग केवल नाम अधारा, सुमर सुमर नर उतरहिं पारा। इस घोर कलयुग में नाम जप ही सबसे सरल और शक्तिशाली नौका है। चलते-फिरते, काम करते हुए भी मन ही मन 'राम-राम' अथवा 'ॐ हनुमते नमः' का अजपा जाप करते रहो। यह पावन नाम तुम्हारी ढाल बनकर रक्षा करेगा।",
        "बूंद जब तक समुद्र से अलग रहती है, उसका अस्तित्व मिटने का डर रहता है; जैसे ही वह समुद्र में मिल जाती है, वह स्वयं समुद्र बन जाती है। ऐसे ही अपने छोटे से 'अहं' को बालाजी के चरणों में विसर्जित कर दो। तुम जब शून्य हो जाओगे, तब प्रभु तुम्हारे भीतर पूर्ण रूप से प्रकट होंगे।",
        "शारीरिक रोग का संबंध बहुत हद तक मानसिक तनाव और अशांति से होता है। जब मन में कुंठा और चिंता भरी होती है, तो शरीर बीमारियों का घर बन जाता है। अपने मन को बालाजी के चरणों में शांत करो। आश्रम की पवित्र भभूत और चरणामृत को श्रद्धा से ग्रहण करो, प्रभु की कृपा से असाध्य व्याधियां भी शांत हो जाती हैं।",
        "प्रातःकाल उठि कै रघुनाथा, मातु पिता गुरु नावहिं माथा। मर्यादा पुरुषोत्तम प्रभु श्रीराम भी सबसे पहले अपने माता-पिता और गुरु को शीश नवाते थे। अपने दिन की शुरुआत माता-पिता के चरण स्पर्श से करो। उनके मुख से निकली एक आशीष हजार संकटों को रास्ते से हटा देती है।",
        "प्रेम ही भक्ति का दूसरा नाम है। यदि पूजा में प्रेम और तड़प नहीं है, तो वह केवल एक औपचारिकता है। जैसे बालक अपनी मां के बिना व्याकुल हो उठता है, वैसे ही प्रभु के दर्शन के लिए हृदय में व्याकुलता होनी चाहिए। बालाजी आंसुओं और सच्चे भाव के भूखे हैं, धन-दौलत के नहीं।",
        "हाथ काम में और मन राम में — यही सच्चा कर्मयोग है। अपने सांसारिक कर्तव्यों, नौकरी और व्यापार को पूरी ईमानदारी और लगन से करो, लेकिन उसके फल की चिंता बालाजी पर छोड़ दो। जो व्यक्ति अपना काम पूजा समझकर करता है, उसे अलग से किसी वन में जाकर तपस्या करने की आवश्यकता नहीं होती।",
        "अंधकार को भगाने के लिए लाठी चलाने की जरूरत नहीं होती, केवल एक छोटा सा दीपक जलाना काफी होता है। ऐसे ही मन से बुराई और नकारात्मकता निकालने के लिए सकारात्मक विचारों और राम नाम का पावन दीपक जलाओ। दिव्य प्रकाश आते ही सारा अंधकार पलक झपकते मिट जाएगा।",
        "मनुष्य का संकल्प यदि अडिग हो, तो वह पर्वतों को भी हिला सकता है। जब भी कोई शुभ संकल्प लो, तो उसे बीच में मत छोड़ो। अपने संकल्प के साथ बालाजी महाराज की शक्ति का आह्वान करो। वे तुम्हारे संकल्प को सिद्धि तक पहुंचाने में पूरी सहायता करेंगे।",
        "श्री बालाजी कृपा धाम केवल ईंट-पत्थर का मंदिर नहीं, बल्कि लाखों भक्तों की आस्था, आंसुओं और गुरुदेव की अखंड तपस्या से जाग्रत एक दिव्य तीर्थ है। यहाँ जो भी सच्चे भाव से अपनी अर्जी लगाता है, उसकी पुकार पवनपुत्र के कानों तक अवश्य पहुंचती है। विश्वास ही तुम्हारी सबसे बड़ी शक्ति है।",
        "जो मनुष्य अपने गुरु और माता-पिता के वचनों को पत्थर की लकीर मानकर चलता है, उसका मार्ग कभी अंधकारमय नहीं हो सकता। गुरु का ज्ञान वह दिव्य ढाल है जो संसार के हर कुचक्र और भ्रमजाल से रक्षा करती है। प्रभु की शरण में आने के बाद भय को विदा कर दो, क्योंकि जहाँ विश्वास है, वहाँ संकटमोचन का वास है।",
        "धन और ऐश्वर्य का संचय तो हर कोई करता है, पर धर्म और सद्कर्मों की पूंजी ही मृत्यु के बाद साथ जाती है। अपनी कमाई का एक अंश दीन-दुखियों, गौ-सेवा और प्रभु के पावन भंडारे में लगाओ। दिया हुआ दान कभी व्यर्थ नहीं जाता, वह सौ गुना होकर रक्षा कवच के रूप में लौटता है।",
        "जब भी किसी काम में विघ्न आए या मन विचलित हो, तो क्रोध या अधीरता दिखाने के बजाय शांत होकर प्रभु का नाम लो। हर कठिनाई तुम्हें कुछ सिखाने आती है। धैर्य और संयम ही भक्त के सबसे बड़े शस्त्र हैं। जो कठिन समय में भी अपने धर्म से नहीं डिगता, प्रभु स्वयं उसका योगक्षेम वहन करते हैं।",
        "मन की एकाग्रता ही सच्ची पूजा है। जब तुम मंदिर में बैठो, तो सांसारिक चिंताओं और लेन-देन के विचारों को बाहर छोड़ दो। केवल एकटक अपने प्रभु के विग्रह को निहारो और उनकी कृपा को हृदय में महसूस करो। थोड़ी देर का निष्कपट ध्यान भी तुम्हारे अंतःकरण को नई ऊर्जा और दिव्य शांति से भर देगा।",
        "संसार में किसी का बुरा मत चाहो, चाहे उसने तुम्हारे साथ कितना भी अनुचित व्यवहार क्यों न किया हो। जो जैसा बोएगा, वह वैसा ही काटेगा। तुम केवल क्षमा और करुणा का भाव रखो। जब तुम किसी के लिए शुभ कामना करते हो, तो तुम्हारे आसपास की सकारात्मक तरंगें तुम्हें हर संकट से बचाती हैं।",
        "कलयुग में हनुमान जी की आराधना सबसे सरल और शीघ्र फलदायी है। जो भक्त पूर्ण पवित्रता और श्रद्धा से बजरंग बाण अथवा हनुमान चालीसा का नित्य पाठ करता है, उसके घर में कभी नकारात्मक शक्तियां या अकाल मृत्यु प्रवेश नहीं कर सकतीं। राम नाम की महिमा अपरंपार है।",
        "अहंकार ज्ञान और भक्ति दोनों को नष्ट कर देता है। जैसे फला हुआ वृक्ष सदैव झुका रहता है, वैसे ही सच्चा ज्ञानी और भक्त सदैव विनम्र होता है। अपनी उपलब्धियों को श्री बालाजी महाराज की कृपा समझो, अपने बल का अभिमान कभी मत करो। नम्रता ही प्रभु प्रेम का द्वार है।",
        "समय की कद्र करो। जो क्षण बीत गया, वह करोड़ों स्वर्ण मुद्राओं से भी वापस नहीं आ सकता। अपने अमूल्य समय को व्यर्थ की गपशप, ईर्ष्या और निंदा में मत गंवाओ। प्रत्येक श्वास के साथ प्रभु के पावन नाम का सिमरन करो, यही मनुष्य जीवन की वास्तविक सार्थकता है।",
        "सच्चा मित्र वही है जो विपत्ति के समय साथ निभाए और सन्मार्ग पर चलने की प्रेरणा दे। संसार के रिश्ते स्वार्थ पर टिके हो सकते हैं, लेकिन श्री बालाजी महाराज से जुड़ा नाता निस्वार्थ और अटूट है। जब तुम उन्हें अपना सच्चा मित्र मान लोगे, तो जीवन में कभी अकेलापन महसूस नहीं होगा।",
        "आश्रम की पावन भूमि पर आकर जो भी भक्त सच्चे मन से सेवा, झाड़ू-सफाई अथवा जल सेवा करता है, उसके कई जन्मों के संचित पाप कट जाते हैं। प्रभु के धाम में कोई छोटा या बड़ा नहीं होता, जो जितना सेवा भाव रखता है, वह बालाजी का उतना ही प्रिय बनता है।",
        "प्रभु से शिकायतें करना बंद करो और जो प्राप्त हुआ है उसके प्रति कृतज्ञता व्यक्त करो। जब तुम हर हाल में प्रसन्न रहकर 'तेरा तुझको अर्पण' का भाव सीख जाओगे, तब तुम्हारा जीवन एक निरंतर उत्सव बन जाएगा। संतोष ही सबसे बड़ा धन है।",
        "क्रोध मनुष्य का सबसे बड़ा शत्रु है। गुस्से में लिया गया एक गलत निर्णय वर्षों की मेहनत और सम्बंधों को नष्ट कर देता है। जब भी क्रोध आए, मौन हो जाओ और मन ही मन 'जय श्री राम' का जाप करो। शांत जल में ही सूर्य का प्रतिबिंब दिखता है, वैसे ही शांत मन में ईश्वर की अनुभूति होती है।",
        "सत्य की राह कठिन अवश्य हो सकती है, पर इस राह का अंत सदैव कल्याणकारी और विजयी होता है। असत्य के सहारे थोड़े समय के लिए लाभ मिल सकता है, पर अंत में लज्जित होना पड़ता है। प्रभु श्रीराम के आदर्शों पर चलो और सत्य के साथ कभी समझौता मत करो।",
        "गौ माता की सेवा और पक्षियों को दाना-पानी देना साक्षात देवताओं की सेवा के समान है। अपने दैनिक जीवन में मूक प्राणियों के प्रति दया और प्रेम का भाव रखो। जिस घर से पहली रोटी गौ माता को जाती है, उस घर में दरिद्रता कभी टिक नहीं सकती।",
        "मन के हारे हार है, मन के जीते जीत। जब तक तुम स्वयं भीतर से हिम्मत नहीं हारते, संसार की कोई शक्ति तुम्हें पराजित नहीं कर सकती। अपने भीतर असीम ऊर्जा और आत्मबल का अनुभव करो, क्योंकि तुम्हारे साथ महाबली हनुमान जी का आशीर्वाद है।",
        "माता-पिता की सेवा ही समस्त तीर्थों का सार है। जिसने अपने माता-पिता के मुख पर तृप्ति और आनंद की मुस्कान ला दी, उसे किसी मंदिर में मत्था टेकने की कमी महसूस नहीं होगी। उनकी एक अंतरमन से निकली आशीष हजारों विपत्तियों को टाल देती है।",
        "दान केवल धन का नहीं होता, अपनी मीठी वाणी, किसी दुखी को दिया गया ढांढस और किसी गिरते हुए को दिया गया सहारा भी सबसे बड़ा दान है। अपने हृदय को इतना विशाल बनाओ कि उसमें सभी के लिए प्रेम और सहानुभूति का स्थान हो।",
        "ईश्वर की न्याय व्यवस्था अत्यंत सूक्ष्म और सटीक है। वहाँ देर हो सकती है, पर अंधेर कभी नहीं होती। अपने कर्मों को निर्मल रखो और फल की चिंता प्रभु पर छोड़ दो। जो तुम्हारे भले के लिए होगा, बालाजी महाराज उसे उचित समय पर अवश्य प्रदान करेंगे।",
        "जब भी जीवन में कोई बड़ा निर्णय लेना हो, तो बालाजी महाराज के सामने एकांत में बैठकर प्रार्थना करो और मार्गदर्शन मांगो। अंतरात्मा से आने वाली सात्विक ध्वनि प्रभु का ही संकेत होती है। धर्म के पक्ष में लिया गया निर्णय कभी निष्फल नहीं जाता।",
        "श्री बालाजी कृपा धाम (डूँगरा जाट) केवल एक पावन तीर्थ नहीं, बल्कि करोड़ों भक्तों की अखंड आस्था का दिव्य शक्तिपीठ है। यहाँ पूज्य गुरुदेव जी के सान्निध्य में लगाई गई हर सच्ची अर्जी साक्षात बालाजी महाराज स्वीकार करते हैं। धर्म, मर्यादा और सेवा के इस महायज्ञ में समर्पित रहो, बालाजी का रक्षा कवच सदैव तुम्हारे साथ रहेगा।"
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

    fun getTodayDefaultDarshanDrawable(): Int {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.TUESDAY -> R.drawable.img_mehandipur_balaji
            Calendar.SATURDAY -> R.drawable.img_hanuman_veer
            Calendar.SUNDAY -> R.drawable.img_balaji_darshan
            Calendar.MONDAY -> R.drawable.img_panchmukhi_hanuman
            Calendar.WEDNESDAY -> R.drawable.img_ram_darbar
            Calendar.THURSDAY -> R.drawable.img_mehandipur_balaji
            Calendar.FRIDAY -> R.drawable.img_hanuman_veer
            else -> R.drawable.img_balaji_darshan
        }
    }

    fun getTodayDefaultDarshanTitle(): String {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.TUESDAY -> "श्री मेहंदीपुर बालाजी महाराज मंगलवार विशेष दिव्य श्रृंगार दर्शन"
            Calendar.SATURDAY -> "श्री संकटमोचन वीर बजरंगी शनिवार पावन अलौकिक दर्शन"
            Calendar.SUNDAY -> "श्री बालाजी कृपा धाम रविवार महा-दरबार दिव्य दर्शन"
            Calendar.MONDAY -> "श्री पंचमुखी हनुमान जी महाराज पावन दिव्य दर्शन"
            Calendar.WEDNESDAY -> "प्रभु श्री राम दरबार एवं वीर हनुमान पावन दर्शन"
            Calendar.THURSDAY -> "श्री मेहंदीपुर बालाजी महाराज दिव्य अलौकिक दर्शन"
            Calendar.FRIDAY -> "वीर बजरंगी महाराज पावन संध्या अलौकिक श्रृंगार दर्शन"
            else -> "श्री बालाजी महाराज दैनिक दिव्य अलौकिक श्रृंगार दर्शन"
        }
    }

    suspend fun updateDailyDarshan(
        title: String,
        photoUrl: String,
        quote: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://shribalajikripadham.online/api/daily_darshan.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("User-Agent", "ShriBalajiKripaDham-Admin")
            }

            val json = JSONObject().apply {
                put("title", title)
                put("photo_url", photoUrl)
                put("blessings_quote", quote)
            }

            conn.outputStream.use { os ->
                os.write(json.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val resJson = JSONObject(resp)
                val success = resJson.optBoolean("success", true)
                val msg = resJson.optString("message", "आज का अलौकिक दर्शन सफलतापूर्वक पब्लिश हुआ।")
                Pair(success, msg)
            } else {
                Pair(false, "सर्वर त्रुटि: HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Pair(false, "त्रुटि: ${e.message}")
        }
    }

    suspend fun fetchTodayDarshan(): DailyDarshanData = withContext(Dispatchers.IO) {
        val todayTag = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val fallback = DailyDarshanData(
            dateHindi = getTodayHindiDate(),
            title = getTodayDefaultDarshanTitle(),
            photoUrl = "https://shribalajikripadham.online/media/balaji_darshan_today.jpg?d=$todayTag",
            quote = getTodayGuruVichar(),
            viewsCount = 1280
        )

        try {
            val url = URL("https://shribalajikripadham.online/api/daily_darshan.php?d=$todayTag")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                useCaches = false
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ShriBalajiKripaDham-Android")
                setRequestProperty("Cache-Control", "no-cache")
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (json.optBoolean("success", false)) {
                    val rawPhoto = json.optString("photo_url", fallback.photoUrl)
                    val cacheBustedPhoto = if (rawPhoto.contains("?")) rawPhoto else "$rawPhoto?d=$todayTag"
                    DailyDarshanData(
                        dateHindi = json.optString("date_hindi", fallback.dateHindi),
                        title = json.optString("title", fallback.title),
                        photoUrl = cacheBustedPhoto,
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
            val finalBitmap = bitmap ?: try {
                BitmapFactory.decodeResource(context.resources, getTodayDefaultDarshanDrawable())
            } catch (e: Exception) {
                null
            }
            if (finalBitmap != null) {
                val shareDir = File(context.cacheDir, "darshan_shares")
                if (!shareDir.exists()) shareDir.mkdirs()
                val shareFile = File(shareDir, "Darshan_${System.currentTimeMillis()}.png")
                FileOutputStream(shareFile).use { fos ->
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
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

        // Try downloading remote bitmap if custom photo uploaded by admin (not default fallback)
        withContext(Dispatchers.IO) {
            try {
                val isCustomUpload = fetched.photoUrl.isNotBlank() &&
                        !fetched.photoUrl.endsWith("balaji_darshan_today.jpg") &&
                        !fetched.photoUrl.endsWith("default.jpg")

                if (isCustomUpload) {
                    val cleanUrl = if (fetched.photoUrl.contains("?")) fetched.photoUrl else "${fetched.photoUrl}?d=${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
                    val conn = URL(cleanUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.useCaches = false
                    conn.setRequestProperty("Cache-Control", "no-cache")
                    if (conn.responseCode == 200) {
                        val bmp = BitmapFactory.decodeStream(conn.inputStream)
                        if (bmp != null) {
                            withContext(Dispatchers.Main) {
                                remoteBitmap = bmp
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        remoteBitmap = null
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
                        painter = painterResource(id = DailyDarshanHelper.getTodayDefaultDarshanDrawable()),
                        contentDescription = "श्री बालाजी अलौकिक श्रृंगार दर्शन",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
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
                        painter = painterResource(id = DailyDarshanHelper.getTodayDefaultDarshanDrawable()),
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
