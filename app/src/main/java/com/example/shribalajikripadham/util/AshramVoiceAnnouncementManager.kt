package com.example.shribalajikripadham.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

data class VoicePresetInfo(
    val id: String,
    val nameHindi: String,
    val nameEnglish: String,
    val gender: String, // "MALE", "FEMALE", or "KIDS"
    val description: String,
    val pitch: Float,
    val speechRate: Float,
    val icon: String,
    val category: String = gender, // "MALE", "FEMALE", or "KIDS"
    val speechStyle: String = "DEVOTIONAL",
    val neuralVoiceHint: String = ""
)

object AshramVoiceAnnouncementManager {
    private const val TAG = "VoiceAnnouncement"
    private const val PREFS_NAME = "sbkd_voice_announcement_prefs"
    private const val KEY_IS_MUTED = "is_tts_muted"
    private const val KEY_VOICE_PRESET = "selected_voice_preset"

    const val PRESET_GURU_CALM = "GURU_CALM"
    const val PRESET_FEMALE_SWEET = "FEMALE_SWEET"
    const val PRESET_ANNOUNCER_MALE = "ANNOUNCER_MALE"
    const val PRESET_SEVIKA_FEMALE = "SEVIKA_FEMALE"
    const val PRESET_YOUTH_CRISP = "YOUTH_CRISP"
    const val PRESET_TRADITIONAL_VYAS = "TRADITIONAL_VYAS"

    val AVAILABLE_VOICE_PRESETS = listOf(
        // ==========================================
        // 1. MALE VOICES (16 UNIQUE HUMAN PERSONAS)
        // ==========================================
        VoicePresetInfo(
            id = PRESET_GURU_CALM,
            nameHindi = "गुरुवाणी व शांत उद्घोषणा",
            nameEnglish = "Guru Calm & Reverent",
            gender = "MALE",
            category = "MALE",
            description = "गंभीर, आदरणीय एवं शांत पुरुष वाणी (आश्रम दरबार हेतु सर्वोत्तम)",
            pitch = 0.88f,
            speechRate = 0.85f,
            icon = "🙏",
            speechStyle = "CALM"
        ),
        VoicePresetInfo(
            id = PRESET_ANNOUNCER_MALE,
            nameHindi = "रेडियो उद्घोषक बुलन्द वाणी",
            nameEnglish = "Temple Announcer Male",
            gender = "MALE",
            category = "MALE",
            description = "स्पष्ट, बुलन्द एवं आधिकारिक पुरुष उद्घोषक स्वर",
            pitch = 0.82f,
            speechRate = 0.94f,
            icon = "📢",
            speechStyle = "ANNOUNCER"
        ),
        VoicePresetInfo(
            id = "ELDER_PANDIT",
            nameHindi = "वरिष्ठ पंडित जी आशीर्वाद",
            nameEnglish = "Elder Pandit Blessing",
            gender = "MALE",
            category = "MALE",
            description = "अनुभवी, स्नेहमयी व आशीर्वादमयी वरिष्ठ पंडित स्वर",
            pitch = 0.78f,
            speechRate = 0.82f,
            icon = "🪔",
            speechStyle = "DEVOTIONAL"
        ),
        VoicePresetInfo(
            id = PRESET_YOUTH_CRISP,
            nameHindi = "युवा ऊर्जावान उद्घोषक",
            nameEnglish = "Youth Crisp & Modern",
            gender = "MALE",
            category = "MALE",
            description = "तेज़, स्पष्ट और ऊर्जावान युवा सेवक स्वर",
            pitch = 0.98f,
            speechRate = 1.02f,
            icon = "⚡",
            speechStyle = "ENERGETIC"
        ),
        VoicePresetInfo(
            id = PRESET_TRADITIONAL_VYAS,
            nameHindi = "शास्त्रीय व्यास कथावाचक",
            nameEnglish = "Traditional Vyas Narrator",
            gender = "MALE",
            category = "MALE",
            description = "पारंपरिक धीर-गंभीर कथावाचक व्यास शैली",
            pitch = 0.75f,
            speechRate = 0.80f,
            icon = "🕉️",
            speechStyle = "TRADITIONAL"
        ),
        VoicePresetInfo(
            id = "SOMBER_PUJARI",
            nameHindi = "आश्रम मुख्य पुजारी",
            nameEnglish = "Devout Ashram Pujari",
            gender = "MALE",
            category = "MALE",
            description = "नित्य आरती व पूजा करने वाले निष्ठावान पुजारी स्वर",
            pitch = 0.85f,
            speechRate = 0.88f,
            icon = "🔔",
            speechStyle = "DEVOTIONAL"
        ),
        VoicePresetInfo(
            id = "BRAJ_KATHA",
            nameHindi = "ब्रज मधुर वाणी कथा",
            nameEnglish = "Braj Madhur Devotional",
            gender = "MALE",
            category = "MALE",
            description = "ब्रजधाम की मिठास से परिपूर्ण कोमल पुरुष स्वर",
            pitch = 0.92f,
            speechRate = 0.86f,
            icon = "🦚",
            speechStyle = "SWEET"
        ),
        VoicePresetInfo(
            id = "HARYANVI_CRISP",
            nameHindi = "ग्रामीण स्पष्ट देहाती स्वर",
            nameEnglish = "Crisp Regional Seva Voice",
            gender = "MALE",
            category = "MALE",
            description = "स्पष्ट, ज़मीनी व आत्मविश्वास से भरा ग्रामीण स्वर",
            pitch = 0.86f,
            speechRate = 0.96f,
            icon = "🌾",
            speechStyle = "CRISP"
        ),
        VoicePresetInfo(
            id = "RADIO_BROADCASTER",
            nameHindi = "आकाशवाणी स्टूडियो उद्घोषक",
            nameEnglish = "Studio Radio Broadcaster",
            gender = "MALE",
            category = "MALE",
            description = "आधिकारिक राष्ट्रीय रेडियो उद्घोषणा जैसी शुद्ध हिंदी",
            pitch = 0.84f,
            speechRate = 0.92f,
            icon = "🎙️",
            speechStyle = "STUDIO"
        ),
        VoicePresetInfo(
            id = "DEEP_BARITONE",
            nameHindi = "गंभीर घंटा अनुनाद स्वर",
            nameEnglish = "Deep Baritone Temple Call",
            gender = "MALE",
            category = "MALE",
            description = "गहरा बास व मंदिर के घंटे जैसी अनुनादक वाणी",
            pitch = 0.70f,
            speechRate = 0.84f,
            icon = "🔊",
            speechStyle = "DEEP"
        ),
        VoicePresetInfo(
            id = "SANSKRIT_ACHARYA",
            nameHindi = "संस्कृत विद्वान आचार्य",
            nameEnglish = "Sanskrit Acharya Timbre",
            gender = "MALE",
            category = "MALE",
            description = "स्पष्ट उच्चारण व गुरुकुल आचार्य जैसी गरिमामयी शैली",
            pitch = 0.80f,
            speechRate = 0.86f,
            icon = "📜",
            speechStyle = "SCHOLARLY"
        ),
        VoicePresetInfo(
            id = "WARM_SEVAK",
            nameHindi = "विनम्र आश्रम सेवक",
            nameEnglish = "Humble Ashram Sevak",
            gender = "MALE",
            category = "MALE",
            description = "भक्तों का स्वागत करने वाले विनम्र व सहयोगी सेवक का स्वर",
            pitch = 0.90f,
            speechRate = 0.90f,
            icon = "🤝",
            speechStyle = "WARM"
        ),
        VoicePresetInfo(
            id = "GENTLE_GUIDE",
            nameHindi = "धैर्यवान मार्गदर्शक",
            nameEnglish = "Patient Darbar Guide",
            gender = "MALE",
            category = "MALE",
            description = "सहज, धीमा व पंक्ति में खड़े भक्तों को राह दिखाने वाला स्वर",
            pitch = 0.88f,
            speechRate = 0.88f,
            icon = "🚶",
            speechStyle = "PATIENT"
        ),
        VoicePresetInfo(
            id = "MAJESTIC_DARBAR",
            nameHindi = "राजसी दरबार उद्घोष",
            nameEnglish = "Majestic Royal Court Call",
            gender = "MALE",
            category = "MALE",
            description = "श्री बालाजी महाराज के पावन दरबार का भव्य उद्घोष",
            pitch = 0.80f,
            speechRate = 0.92f,
            icon = "👑",
            speechStyle = "MAJESTIC"
        ),
        VoicePresetInfo(
            id = "PILGRIM_SAINIK",
            nameHindi = "यात्री दल नायक",
            nameEnglish = "Pilgrim Group Leader",
            gender = "MALE",
            category = "MALE",
            description = "पदयात्रा दल के नायक जैसी दृढ़ व सुरक्षात्मक वाणी",
            pitch = 0.85f,
            speechRate = 0.95f,
            icon = "🚩",
            speechStyle = "LEADER"
        ),
        VoicePresetInfo(
            id = "VEDIC_CHANTER",
            nameHindi = "ऋषि परम्परा वैदिक स्वर",
            nameEnglish = "Vedic Sage Resonance",
            gender = "MALE",
            category = "MALE",
            description = "हवन व अनुष्ठान जैसी प्राचीन ऋषि गूँज",
            pitch = 0.76f,
            speechRate = 0.82f,
            icon = "🔥",
            speechStyle = "VEDIC"
        ),

        // ==========================================
        // 2. FEMALE VOICES (16 UNIQUE HUMAN PERSONAS)
        // ==========================================
        VoicePresetInfo(
            id = PRESET_FEMALE_SWEET,
            nameHindi = "देवी वंदना मधुर स्वर",
            nameEnglish = "Sweet Devotional Female",
            gender = "FEMALE",
            category = "FEMALE",
            description = "मधुर, शांत एवं सौम्य महिला वाणी",
            pitch = 1.18f,
            speechRate = 0.90f,
            icon = "🌸",
            speechStyle = "SWEET"
        ),
        VoicePresetInfo(
            id = PRESET_SEVIKA_FEMALE,
            nameHindi = "आदरणीय सेविका उद्घोषणा",
            nameEnglish = "Respectful Ashram Sevika",
            gender = "FEMALE",
            category = "FEMALE",
            description = "विनम्र, स्पष्ट व आदरणीय आश्रम सेविका स्वर",
            pitch = 1.05f,
            speechRate = 0.92f,
            icon = "💐",
            speechStyle = "RESPECTFUL"
        ),
        VoicePresetInfo(
            id = "DIDIS_VOICE",
            nameHindi = "स्नेहमयी दीदी का स्वर",
            nameEnglish = "Affectionate Elder Sister",
            gender = "FEMALE",
            category = "FEMALE",
            description = "बड़ी बहन जैसी अपनेपन और अपनत्व से भरी आवाज़",
            pitch = 1.12f,
            speechRate = 0.88f,
            icon = "🥻",
            speechStyle = "CARING"
        ),
        VoicePresetInfo(
            id = "MOTHERLY_VATSALYA",
            nameHindi = "मातृ वात्सल्य ममता स्वर",
            nameEnglish = "Motherly Warmth & Love",
            gender = "FEMALE",
            category = "FEMALE",
            description = "माँ जैसी दुलार भरी, धैर्यवान और आश्वस्त करने वाली वाणी",
            pitch = 1.02f,
            speechRate = 0.84f,
            icon = "🤱",
            speechStyle = "MATERNAL"
        ),
        VoicePresetInfo(
            id = "MELODIOUS_BHAJAN",
            nameHindi = "मधुर भजन गायिका",
            nameEnglish = "Melodious Bhajan Singer",
            gender = "FEMALE",
            category = "FEMALE",
            description = "संगीतमय, सुरीली व भक्तिभाव से भरी गायिका वाणी",
            pitch = 1.22f,
            speechRate = 0.86f,
            icon = "🎶",
            speechStyle = "MELODIC"
        ),
        VoicePresetInfo(
            id = "DIGNIFIED_EDUCATOR",
            nameHindi = "गरिमामयी शिक्षिका स्वर",
            nameEnglish = "Dignified Teacher Diction",
            gender = "FEMALE",
            category = "FEMALE",
            description = "एक-एक शब्द को अत्यंत शुद्ध व स्पष्ट बोलने वाली वाणी",
            pitch = 1.10f,
            speechRate = 0.94f,
            icon = "📖",
            speechStyle = "EDUCATED"
        ),
        VoicePresetInfo(
            id = "DEVOTIONAL_KATHA_F",
            nameHindi = "कथा वाचिका भक्ति स्वर",
            nameEnglish = "Devotional Storyteller Female",
            gender = "FEMALE",
            category = "FEMALE",
            description = "कथा सत्संग जैसी भावपूर्ण, संवेदनशील व मधुर शैली",
            pitch = 1.15f,
            speechRate = 0.85f,
            icon = "🌺",
            speechStyle = "STORYTELLER"
        ),
        VoicePresetInfo(
            id = "CLEAR_NEWSCASTER_F",
            nameHindi = "आधिकारिक महिला उद्घोषिका",
            nameEnglish = "Official Female Presenter",
            gender = "FEMALE",
            category = "FEMALE",
            description = "पेशेवर न्यूज़ एंकर जैसी स्पष्ट व आत्मविश्वासपूर्ण आवाज़",
            pitch = 1.08f,
            speechRate = 0.96f,
            icon = "🎤",
            speechStyle = "PRESENTER"
        ),
        VoicePresetInfo(
            id = "GANGA_AARTI_F",
            nameHindi = "माँ गंगा आरती पावन स्वर",
            nameEnglish = "Sacred Aarti Female Chanting",
            gender = "FEMALE",
            category = "FEMALE",
            description = "पवित्र घाटों पर गूँजने वाली पावन भक्ति तरंग",
            pitch = 1.25f,
            speechRate = 0.88f,
            icon = "🌊",
            speechStyle = "SACRED"
        ),
        VoicePresetInfo(
            id = "POETIC_DEVOTEE_F",
            nameHindi = "भक्ति कवयित्री स्वर",
            nameEnglish = "Poetic Devotee Grace",
            gender = "FEMALE",
            category = "FEMALE",
            description = "ललित, काव्यमय और शांत रस से ओत-प्रोत वाणी",
            pitch = 1.16f,
            speechRate = 0.87f,
            icon = "✍️",
            speechStyle = "POETIC"
        ),
        VoicePresetInfo(
            id = "PRASAD_SEVIKA_F",
            nameHindi = "प्रसाद सेवा सौम्य स्वर",
            nameEnglish = "Gentle Prasad Sevika",
            gender = "FEMALE",
            category = "FEMALE",
            description = "भंडारा व प्रसाद वितरण जैसी प्रसन्नचित्त व मीठी वाणी",
            pitch = 1.14f,
            speechRate = 0.92f,
            icon = "🍯",
            speechStyle = "PLEASANT"
        ),
        VoicePresetInfo(
            id = "GRANDMOTHER_F",
            nameHindi = "दादी माँ का वात्सल्य",
            nameEnglish = "Wise Loving Grandmother",
            gender = "FEMALE",
            category = "FEMALE",
            description = "परिपक्व, सहज व आत्मीयता से भरपूर वरिष्ठ वाणी",
            pitch = 0.98f,
            speechRate = 0.80f,
            icon = "👵",
            speechStyle = "ELDER"
        ),
        VoicePresetInfo(
            id = "ENERGETIC_SEVIKA_F",
            nameHindi = "ऊर्जावान स्वयंसेविका",
            nameEnglish = "Active Seva Volunteer",
            gender = "FEMALE",
            category = "FEMALE",
            description = "तेज़, फुर्तीली व मददगार महिला स्वयंसेवक की आवाज़",
            pitch = 1.12f,
            speechRate = 1.00f,
            icon = "✨",
            speechStyle = "ACTIVE"
        ),
        VoicePresetInfo(
            id = "PEACEFUL_MEDITATION_F",
            nameHindi = "ध्यान शांति स्वर",
            nameEnglish = "Meditative Peace Guide",
            gender = "FEMALE",
            category = "FEMALE",
            description = "मन को विश्राम देने वाली अति-शांत व कोमल वाणी",
            pitch = 1.06f,
            speechRate = 0.82f,
            icon = "🧘‍♀️",
            speechStyle = "MEDITATIVE"
        ),
        VoicePresetInfo(
            id = "DIVINE_SHAKTI_F",
            nameHindi = "दिव्य शक्ति तेजस्विनी",
            nameEnglish = "Divine Shakti Resonance",
            gender = "FEMALE",
            category = "FEMALE",
            description = "तेजस्वी, प्रेरणादायी व श्रद्धा भाव जागृत करने वाली वाणी",
            pitch = 1.10f,
            speechRate = 0.90f,
            icon = "🔱",
            speechStyle = "POWERFUL"
        ),
        VoicePresetInfo(
            id = "RURAL_DEVOTEE_F",
            nameHindi = "ग्रामीण श्रद्धा स्वर",
            nameEnglish = "Rural Devotee Natural",
            gender = "FEMALE",
            category = "FEMALE",
            description = "निष्कपट, सीधी व अंतर्मन से निकली ग्रामीण महिला वाणी",
            pitch = 1.15f,
            speechRate = 0.92f,
            icon = "🌿",
            speechStyle = "NATURAL"
        ),

        // ==========================================
        // 3. KIDS / BAL SWAR (16 UNIQUE HUMAN PERSONAS)
        // ==========================================
        VoicePresetInfo(
            id = "BAL_GOPAL_SWEET",
            nameHindi = "बाल गोपाल नटखट स्वर",
            nameEnglish = "Little Kanha Sweet Child",
            gender = "KIDS",
            category = "KIDS",
            description = "मासूम, मनमोहक व नन्हे बाल गोपाल जैसी प्यारी आवाज़",
            pitch = 1.60f,
            speechRate = 0.92f,
            icon = "🦚",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "BALIKA_SARASWATI",
            nameHindi = "नन्हीं बालिका सरस्वती",
            nameEnglish = "Sweet Little Girl Saraswati",
            gender = "KIDS",
            category = "KIDS",
            description = "मधुर, कोमल व स्पष्ट नन्हीं गुड़िया का पवित्र स्वर",
            pitch = 1.68f,
            speechRate = 0.94f,
            icon = "👧",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "CHHOTA_BHAKT",
            nameHindi = "छोटा भक्त उत्साही वाणी",
            nameEnglish = "Enthusiastic Little Devotee",
            gender = "KIDS",
            category = "KIDS",
            description = "बालाजी के दर्शन हेतु उत्साहित नन्हे बालक का स्वर",
            pitch = 1.55f,
            speechRate = 0.95f,
            icon = "🧒",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "BAL_AARTI_LEAD",
            nameHindi = "बाल आरती गायक स्वर",
            nameEnglish = "Child Aarti Lead Singer",
            gender = "KIDS",
            category = "KIDS",
            description = "आरती में सुंदर बाल स्वर में गाता हुआ बालक",
            pitch = 1.58f,
            speechRate = 0.88f,
            icon = "🪕",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "INNOCENT_SEVAK",
            nameHindi = "मासूम बाल सेवक",
            nameEnglish = "Innocent Little Ashram Helper",
            gender = "KIDS",
            category = "KIDS",
            description = "विनम्रता से टोकन पुकारता हुआ आश्रम का नन्हा सेवक",
            pitch = 1.62f,
            speechRate = 0.90f,
            icon = "👶",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "LITTLE_MEERA",
            nameHindi = "नन्हीं मीरा भक्ति स्वर",
            nameEnglish = "Little Meera Devotional",
            gender = "KIDS",
            category = "KIDS",
            description = "भक्ति में डूबी हुई नन्हीं बालिका का मधुर स्वर",
            pitch = 1.70f,
            speechRate = 0.88f,
            icon = "🌼",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "SCHOOL_BAL_CRISP",
            nameHindi = "स्कूली छात्र स्पष्ट वाणी",
            nameEnglish = "Crisp Student Child Voice",
            gender = "KIDS",
            category = "KIDS",
            description = "प्राथमिक विद्यालय के होशियार बालक जैसा स्पष्ट स्वर",
            pitch = 1.50f,
            speechRate = 0.98f,
            icon = "🎒",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "BAL_HANUMAN",
            nameHindi = "वीर बाल हनुमान स्वर",
            nameEnglish = "Brave Little Hanuman",
            gender = "KIDS",
            category = "KIDS",
            description = "वीरता, उमंग व निडरता से भरा बाल हनुमान स्वर",
            pitch = 1.48f,
            speechRate = 1.02f,
            icon = "🚩",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "CHHOTA_PUJARI",
            nameHindi = "छोटा पुजारी बाल वाणी",
            nameEnglish = "Little Pujari Chanting",
            gender = "KIDS",
            category = "KIDS",
            description = "संस्कारवान बालक द्वारा आदरपूर्वक की गई घोषणा",
            pitch = 1.52f,
            speechRate = 0.86f,
            icon = "🏺",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "BAL_GAWAIYA",
            nameHindi = "बाल गवैया चहकता स्वर",
            nameEnglish = "Bright Little Singer",
            gender = "KIDS",
            category = "KIDS",
            description = "प्रसन्नता से चहकता हुआ सुरीला बाल स्वर",
            pitch = 1.64f,
            speechRate = 0.92f,
            icon = "🕊️",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "TINY_PILGRIM",
            nameHindi = "नन्हा तीर्थयात्री",
            nameEnglish = "Tiny Pilgrim Innocent Call",
            gender = "KIDS",
            category = "KIDS",
            description = "माता-पिता के साथ दरबार आया नन्हा यात्री",
            pitch = 1.66f,
            speechRate = 0.90f,
            icon = "🌻",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "BRIGHT_SANSKAR",
            nameHindi = "संस्कारी बाल वाणी",
            nameEnglish = "Well-Mannered Sanskar Child",
            gender = "KIDS",
            category = "KIDS",
            description = "माता-पिता और गुरुओं का आदर करने वाला संस्कारी बालक",
            pitch = 1.54f,
            speechRate = 0.92f,
            icon = "⭐",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "CHIRPY_SEVIKA_KID",
            nameHindi = "चहकती बाल सेविका",
            nameEnglish = "Chirpy Little Girl Helper",
            gender = "KIDS",
            category = "KIDS",
            description = "खुशी-खुशी काम करने वाली नन्हीं नटखट सेविका",
            pitch = 1.72f,
            speechRate = 1.00f,
            icon = "🎀",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "SWEET_LADDU_GOPAL",
            nameHindi = "प्यारा लड्डू गोपाल",
            nameEnglish = "Charming Laddu Gopal Voice",
            gender = "KIDS",
            category = "KIDS",
            description = "सबका मन मोह लेने वाली बाल वाणी",
            pitch = 1.58f,
            speechRate = 0.88f,
            icon = "🧁",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "BAL_RAMLILA",
            nameHindi = "बाल कलाकार रामलीला स्वर",
            nameEnglish = "Young Ramlila Performer",
            gender = "KIDS",
            category = "KIDS",
            description = "भावपूर्ण व स्पष्ट संवाद बोलने वाला बाल कलाकार",
            pitch = 1.50f,
            speechRate = 0.90f,
            icon = "🏹",
            speechStyle = "CHILD"
        ),
        VoicePresetInfo(
            id = "GENTLE_ANGEL_KID",
            nameHindi = "शांत बाल मुकुंद",
            nameEnglish = "Peaceful Little Angel",
            gender = "KIDS",
            category = "KIDS",
            description = "अति-कोमल, शांतिप्रिय व एकाग्रचित्त बाल स्वर",
            pitch = 1.56f,
            speechRate = 0.84f,
            icon = "👼",
            speechStyle = "CHILD"
        )
    )

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingSpeech: String? = null
    private var lastAnnouncementText: String = ""

    fun isMuted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_MUTED, false)
    }

    fun setMuted(context: Context, muted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_MUTED, muted).apply()
        if (muted) {
            stop()
        }
    }

    fun getSelectedVoicePreset(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VOICE_PRESET, PRESET_GURU_CALM) ?: PRESET_GURU_CALM
    }

    fun setVoicePreset(context: Context, presetId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_VOICE_PRESET, presetId).apply()
        applyVoiceSettings(context, presetId)
    }

    fun initIfNeeded(context: Context) {
        if (tts != null && isInitialized) return
        val appContext = context.applicationContext
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val hindiLocale = Locale("hi", "IN")
                val res = tts?.setLanguage(hindiLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Hindi TTS locale not directly supported, falling back to default locale")
                    tts?.language = Locale.getDefault()
                }

                val currentPreset = getSelectedVoicePreset(context)
                applyVoiceSettings(context, currentPreset)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS announcement started: $utteranceId")
                    }
                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS announcement completed: $utteranceId")
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS announcement error on $utteranceId")
                    }
                })
                isInitialized = true
                pendingSpeech?.let { speech ->
                    speakRaw(speech)
                    pendingSpeech = null
                }
            } else {
                Log.e(TAG, "Failed to initialize TextToSpeech: status $status")
            }
        }
    }

    private fun applyVoiceSettings(context: Context, presetId: String) {
        val preset = AVAILABLE_VOICE_PRESETS.find { it.id == presetId } ?: AVAILABLE_VOICE_PRESETS[0]
        try {
            // Apply pitch and speech rate modulation for human resonance
            tts?.setPitch(preset.pitch)
            tts?.setSpeechRate(preset.speechRate)

            // Inspect available native system voices for Hindi (hi-IN)
            tts?.voices?.let { allVoices ->
                val hindiVoices = allVoices.filter { it.locale.language == "hi" }
                if (hindiVoices.isNotEmpty()) {
                    val matchingVoice = when (preset.category) {
                        "KIDS" -> {
                            // Kids favor sweet female or high-pitch natural voices
                            hindiVoices.find { it.name.contains("female", ignoreCase = true) || it.name.contains("-c-", ignoreCase = true) || it.name.contains("-a-", ignoreCase = true) }
                                ?: hindiVoices.first()
                        }
                        "FEMALE" -> {
                            hindiVoices.find { it.name.contains("female", ignoreCase = true) || it.name.contains("-c-", ignoreCase = true) || it.name.contains("-a-", ignoreCase = true) }
                                ?: hindiVoices.first()
                        }
                        else -> {
                            hindiVoices.find { it.name.contains("male", ignoreCase = true) || it.name.contains("-b-", ignoreCase = true) || it.name.contains("-d-", ignoreCase = true) }
                                ?: hindiVoices.first()
                        }
                    }
                    tts?.voice = matchingVoice
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying voice settings for preset $presetId", e)
        }
    }

    /**
     * Speaks the Next Token announcement in clear, respectful Hindi.
     * Natural human prosody with commas providing natural breathing pauses.
     */
    fun announceNextToken(
        context: Context,
        tokenNumber: Int,
        devoteeName: String = "",
        city: String = ""
    ) {
        if (isMuted(context)) return

        val cleanName = devoteeName.trim()
        val cleanCity = city.trim()

        val textToSpeak = when {
            cleanName.isNotBlank() && cleanCity.isNotBlank() -> {
                "टोकन नंबर $tokenNumber, श्री $cleanName जी, $cleanCity से, आपका नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
            }
            cleanName.isNotBlank() -> {
                "टोकन नंबर $tokenNumber, श्री $cleanName जी, आपका नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
            }
            else -> {
                "टोकन नंबर $tokenNumber, आपका नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
            }
        }

        lastAnnouncementText = textToSpeak
        speakWithCurrentPreset(context, textToSpeak)
    }

    /**
     * Speaks a test phrase for the given voice preset so Super Admin / Admin can preview.
     */
    fun testVoice(context: Context, presetId: String) {
        initIfNeeded(context)
        applyVoiceSettings(context, presetId)
        val preset = AVAILABLE_VOICE_PRESETS.find { it.id == presetId } ?: AVAILABLE_VOICE_PRESETS[0]
        val testText = when (preset.category) {
            "KIDS" -> "जय श्री बालाजी! टोकन नंबर एक, श्री रमेश कुमार जी, कृपया गुरुजी के पास आइए।"
            "FEMALE" -> "टोकन नंबर एक, श्री रमेश कुमार जी, आपका नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
            else -> "टोकन नंबर एक, श्री रमेश कुमार जी, आपका नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
        }
        speakRaw(testText)
    }

    fun repeatLastAnnouncement(context: Context) {
        if (lastAnnouncementText.isNotBlank()) {
            speakWithCurrentPreset(context, lastAnnouncementText)
        }
    }

    fun speak(context: Context, text: String) {
        speakWithCurrentPreset(context, text)
    }

    private fun speakWithCurrentPreset(context: Context, text: String) {
        if (isMuted(context)) return
        initIfNeeded(context)
        val preset = getSelectedVoicePreset(context)
        applyVoiceSettings(context, preset)
        if (!isInitialized) {
            pendingSpeech = text
        } else {
            speakRaw(text)
        }
    }

    private fun speakRaw(text: String) {
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "token_announcement_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error executing speakRaw", e)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
