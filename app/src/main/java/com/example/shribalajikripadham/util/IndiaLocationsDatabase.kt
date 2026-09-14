package com.example.shribalajikripadham.util

/**
 * Represents a recognized geographic location across India with category and road distance to Ashram.
 */
data class IndiaLocation(
    val nameHindi: String,
    val nameEnglish: String,
    val category: String, // "स्थानीय गाँव", "तहसील / कस्बा", "ज़िला (UP)", "राज्य / UT", "प्रमुख शहर"
    val stateHindi: String = "उत्तर प्रदेश",
    val distanceKm: Float = -1f
) {
    val displayLabel: String
        get() = if (category == "स्थानीय गाँव" || category == "तहसील / कस्बा") {
            "$nameHindi (तहसील/गाँव, $stateHindi)"
        } else if (category == "ज़िला (UP)") {
            "$nameHindi ($stateHindi)"
        } else if (category == "राज्य / UT") {
            "$nameHindi (राज्य)"
        } else {
            "$nameHindi ($stateHindi)"
        }
}

/**
 * Pre-compiled, ultra-fast pan-India location database with smart bilingual search
 * (Hindi & English), prioritised for UP districts, Bulandshahr tehsils & Dungra Jaat villages.
 */
object IndiaLocationsDatabase {

    private val LOCATIONS: List<IndiaLocation> = listOf(
        // 1. LOCAL VILLAGES & GRAM PANCHAYATS AROUND DUNGRA JAAT
        IndiaLocation("डूँगरा जाट (स्थानीय आश्रम)", "Dungra Jaat (Local Ashram)", "स्थानीय गाँव", "बुलन्दशहर", 0f),
        IndiaLocation("डूँगरा जाट", "Dungra Jaat", "स्थानीय गाँव", "बुलन्दशहर", 0f),
        IndiaLocation("छोटाबांस", "Chhota Bans", "स्थानीय गाँव", "बुलन्दशहर", 5f),
        IndiaLocation("बावन", "Bavan", "स्थानीय गाँव", "बुलन्दशहर", 6f),
        IndiaLocation("बड़ाबांस", "Bada Bans", "स्थानीय गाँव", "बुलन्दशहर", 7f),
        IndiaLocation("सलेमपुर", "Salampur", "स्थानीय गाँव", "बुलन्दशहर", 8f),
        IndiaLocation("शिकारपुर", "Shikarpur", "तहसील / कस्बा", "बुलन्दशहर", 14f),
        IndiaLocation("वलीपुरा", "Walipura", "स्थानीय गाँव", "बुलन्दशहर", 20f),
        IndiaLocation("बुलन्दशहर", "Bulandshahr", "ज़िला (UP)", "उत्तर प्रदेश", 22f),
        IndiaLocation("जहाँगीराबाद", "Jahangirabad", "तहसील / कस्बा", "बुलन्दशहर", 24f),
        IndiaLocation("औरंगाबाद", "Aurangabad", "तहसील / कस्बा", "बुलन्दशहर", 26f),
        IndiaLocation("पहासू", "Pahasu", "तहसील / कस्बा", "बुलन्दशहर", 28f),
        IndiaLocation("खानपुर", "Khanpur", "तहसील / कस्बा", "बुलन्दशहर", 28f),
        IndiaLocation("छतारी", "Chhatari", "तहसील / कस्बा", "बुलन्दशहर", 32f),
        IndiaLocation("दानपुर", "Danpur", "तहसील / कस्बा", "बुलन्दशहर", 34f),
        IndiaLocation("स्याना", "Syana", "तहसील / कस्बा", "बुलन्दशहर", 35f),
        IndiaLocation("चोला", "Chola", "तहसील / कस्बा", "बुलन्दशहर", 35f),
        IndiaLocation("अनूपशहर", "Anupshahr", "तहसील / कस्बा", "बुलन्दशहर", 36f),
        IndiaLocation("खुर्जा", "Khurja", "तहसील / कस्बा", "बुलन्दशहर", 38f),
        IndiaLocation("गुलावठी", "Gulaothi", "तहसील / कस्बा", "बुलन्दशहर", 42f),
        IndiaLocation("बुगरासी", "Bugrasi", "तहसील / कस्बा", "बुलन्दशहर", 42f),
        IndiaLocation("जवां", "Jawan", "तहसील / कस्बा", "अलीगढ़", 42f),
        IndiaLocation("डिबाई", "Debai", "तहसील / कस्बा", "बुलन्दशहर", 44f),
        IndiaLocation("सिकंदराबाद", "Sikandrabad", "तहसील / कस्बा", "बुलन्दशहर", 45f),
        IndiaLocation("अरनिया", "Arnia", "तहसील / कस्बा", "बुलन्दशहर", 45f),
        IndiaLocation("ककोड़", "Kakore", "तहसील / कस्बा", "बुलन्दशहर", 45f),
        IndiaLocation("अतरौली", "Atrauli", "तहसील / कस्बा", "अलीगढ़", 48f),
        IndiaLocation("खैर", "Khair", "तहसील / कस्बा", "अलीगढ़", 48f),
        IndiaLocation("बीबीनगर", "BB Nagar", "तहसील / कस्बा", "बुलन्दशहर", 48f),
        IndiaLocation("भवन बहादुर नगर", "Bhavan Bahadur Nagar", "तहसील / कस्बा", "बुलन्दशहर", 48f),
        IndiaLocation("झाझर", "Jhajhar", "तहसील / कस्बा", "बुलन्दशहर", 50f),
        IndiaLocation("जेवर", "Jewar", "तहसील / कस्बा", "गौतम बुद्ध नगर", 58f),
        IndiaLocation("दादरी", "Dadri", "तहसील / कस्बा", "गौतम बुद्ध नगर", 62f),
        IndiaLocation("गढ़मुक्तेश्वर", "Garhmukteshwar", "तहसील / कस्बा", "हापुड़", 65f),
        IndiaLocation("हसनपुर", "Hasanpur", "तहसील / कस्बा", "अमरोहा", 65f),
        IndiaLocation("बहजोई", "Bahjoi", "तहसील / कस्बा", "संभल", 72f),
        IndiaLocation("गजरौला", "Gajraula", "तहसील / कस्बा", "अमरोहा", 75f),
        IndiaLocation("चंदौसी", "Chandausi", "तहसील / कस्बा", "संभल", 95f),

        // 2. DELHI-NCR & NEARBY REGIONAL HUBS
        IndiaLocation("ग्रेटर नोएडा", "Greater Noida", "प्रमुख शहर", "उत्तर प्रदेश", 68f),
        IndiaLocation("नोएडा", "Noida", "प्रमुख शहर", "उत्तर प्रदेश", 82f),
        IndiaLocation("गाजियाबाद", "Ghaziabad", "ज़िला (UP)", "उत्तर प्रदेश", 78f),
        IndiaLocation("हापुड़", "Hapur", "ज़िला (UP)", "उत्तर प्रदेश", 58f),
        IndiaLocation("अलीगढ़", "Aligarh", "ज़िला (UP)", "उत्तर प्रदेश", 55f),
        IndiaLocation("मेरठ", "Meerut", "ज़िला (UP)", "उत्तर प्रदेश", 85f),
        IndiaLocation("फरीदाबाद", "Faridabad", "प्रमुख शहर", "हरियाणा", 85f),
        IndiaLocation("पलवल", "Palwal", "प्रमुख शहर", "हरियाणा", 75f),
        IndiaLocation("दिल्ली", "Delhi", "राज्य / UT", "दिल्ली", 95f),
        IndiaLocation("नई दिल्ली", "New Delhi", "प्रमुख शहर", "दिल्ली", 98f),
        IndiaLocation("गुरुग्राम (गुड़गांव)", "Gurugram (Gurgaon)", "प्रमुख शहर", "हरियाणा", 110f),
        IndiaLocation("सोनीपत", "Sonipat", "प्रमुख शहर", "हरियाणा", 140f),
        IndiaLocation("रोहतक", "Rohtak", "प्रमुख शहर", "हरियाणा", 165f),
        IndiaLocation("पानीपत", "Panipat", "प्रमुख शहर", "हरियाणा", 170f),
        IndiaLocation("करनाल", "Karnal", "प्रमुख शहर", "हरियाणा", 205f),

        // 3. ALL 75 DISTRICTS OF UTTAR PRADESH
        IndiaLocation("आगरा", "Agra", "ज़िला (UP)", "उत्तर प्रदेश", 155f),
        IndiaLocation("अम्बेडकर नगर", "Ambedkar Nagar", "ज़िला (UP)", "उत्तर प्रदेश", 520f),
        IndiaLocation("अमेठी", "Amethi", "ज़िला (UP)", "उत्तर प्रदेश", 510f),
        IndiaLocation("अमरोहा", "Amroha", "ज़िला (UP)", "उत्तर प्रदेश", 90f),
        IndiaLocation("औरैया", "Auraiya", "ज़िला (UP)", "उत्तर प्रदेश", 320f),
        IndiaLocation("अयोध्या (फैजाबाद)", "Ayodhya (Faizabad)", "ज़िला (UP)", "उत्तर प्रदेश", 530f),
        IndiaLocation("आजमगढ़", "Azamgarh", "ज़िला (UP)", "उत्तर प्रदेश", 640f),
        IndiaLocation("बागपत", "Baghpat", "ज़िला (UP)", "उत्तर प्रदेश", 110f),
        IndiaLocation("बहराइच", "Bahraich", "ज़िला (UP)", "उत्तर प्रदेश", 480f),
        IndiaLocation("बलिया", "Ballia", "ज़िला (UP)", "उत्तर प्रदेश", 720f),
        IndiaLocation("बलरामपुर", "Balrampur", "ज़िला (UP)", "उत्तर प्रदेश", 520f),
        IndiaLocation("बांदा", "Banda", "ज़िला (UP)", "उत्तर प्रदेश", 480f),
        IndiaLocation("बाराबंकी", "Barabanki", "ज़िला (UP)", "उत्तर प्रदेश", 440f),
        IndiaLocation("बरेली", "Bareilly", "ज़िला (UP)", "उत्तर प्रदेश", 175f),
        IndiaLocation("बस्ती", "Basti", "ज़िला (UP)", "उत्तर प्रदेश", 580f),
        IndiaLocation("भदोही", "Bhadohi", "ज़िला (UP)", "उत्तर प्रदेश", 620f),
        IndiaLocation("बिजनौर", "Bijnor", "ज़िला (UP)", "उत्तर प्रदेश", 140f),
        IndiaLocation("बदायूं", "Budaun", "ज़िला (UP)", "उत्तर प्रदेश", 125f),
        IndiaLocation("चंदौली", "Chandauli", "ज़िला (UP)", "उत्तर प्रदेश", 680f),
        IndiaLocation("चित्रकूट", "Chitrakoot", "ज़िला (UP)", "उत्तर प्रदेश", 510f),
        IndiaLocation("देवरिया", "Deoria", "ज़िला (UP)", "उत्तर प्रदेश", 670f),
        IndiaLocation("एटा", "Etah", "ज़िला (UP)", "उत्तर प्रदेश", 110f),
        IndiaLocation("इटावा", "Etawah", "ज़िला (UP)", "उत्तर प्रदेश", 250f),
        IndiaLocation("फर्रुखाबाद", "Farrukhabad", "ज़िला (UP)", "उत्तर प्रदेश", 220f),
        IndiaLocation("फतेहपुर", "Fatehpur", "ज़िला (UP)", "उत्तर प्रदेश", 450f),
        IndiaLocation("फिरोजाबाद", "Firozabad", "ज़िला (UP)", "उत्तर प्रदेश", 160f),
        IndiaLocation("गौतम बुद्ध नगर", "Gautam Buddha Nagar", "ज़िला (UP)", "उत्तर प्रदेश", 75f),
        IndiaLocation("गाजीपुर", "Ghazipur", "ज़िला (UP)", "उत्तर प्रदेश", 690f),
        IndiaLocation("गोंडा", "Gonda", "ज़िला (UP)", "उत्तर प्रदेश", 500f),
        IndiaLocation("गोरखपुर", "Gorakhpur", "ज़िला (UP)", "उत्तर प्रदेश", 640f),
        IndiaLocation("हमीरपुर", "Hamirpur", "ज़िला (UP)", "उत्तर प्रदेश", 420f),
        IndiaLocation("हरदोई", "Hardoi", "ज़िला (UP)", "उत्तर प्रदेश", 310f),
        IndiaLocation("हाथरस", "Hathras", "ज़िला (UP)", "उत्तर प्रदेश", 82f),
        IndiaLocation("जालौन", "Jalaun", "ज़िला (UP)", "उत्तर प्रदेश", 360f),
        IndiaLocation("जौनपुर", "Jaunpur", "ज़िला (UP)", "उत्तर प्रदेश", 620f),
        IndiaLocation("झांसी", "Jhansi", "ज़िला (UP)", "उत्तर प्रदेश", 380f),
        IndiaLocation("कन्नौज", "Kannauj", "ज़िला (UP)", "उत्तर प्रदेश", 290f),
        IndiaLocation("कानपुर देहात", "Kanpur Dehat", "ज़िला (UP)", "उत्तर प्रदेश", 360f),
        IndiaLocation("कानपुर नगर", "Kanpur Nagar", "ज़िला (UP)", "उत्तर प्रदेश", 380f),
        IndiaLocation("कासगंज", "Kasganj", "ज़िला (UP)", "उत्तर प्रदेश", 95f),
        IndiaLocation("कौशाम्बी", "Kaushambi", "ज़िला (UP)", "उत्तर प्रदेश", 520f),
        IndiaLocation("कुशीनगर", "Kushinagar", "ज़िला (UP)", "उत्तर प्रदेश", 690f),
        IndiaLocation("लखीमपुर खीरी", "Lakhimpur Kheri", "ज़िला (UP)", "उत्तर प्रदेश", 340f),
        IndiaLocation("ललितपुर", "Lalitpur", "ज़िला (UP)", "उत्तर प्रदेश", 470f),
        IndiaLocation("लखनऊ", "Lucknow", "ज़िला (UP)", "उत्तर प्रदेश", 420f),
        IndiaLocation("महराजगंज", "Maharajganj", "ज़िला (UP)", "उत्तर प्रदेश", 650f),
        IndiaLocation("महोबा", "Mahoba", "ज़िला (UP)", "उत्तर प्रदेश", 460f),
        IndiaLocation("मैनपुरी", "Mainpuri", "ज़िला (UP)", "उत्तर प्रदेश", 170f),
        IndiaLocation("मथुरा", "Mathura", "ज़िला (UP)", "उत्तर प्रदेश", 110f),
        IndiaLocation("मऊ", "Mau", "ज़िला (UP)", "उत्तर प्रदेश", 670f),
        IndiaLocation("मिर्जापुर", "Mirzapur", "ज़िला (UP)", "उत्तर प्रदेश", 650f),
        IndiaLocation("मुरादाबाद", "Moradabad", "ज़िला (UP)", "उत्तर प्रदेश", 115f),
        IndiaLocation("मुजफ्फरनगर", "Muzaffarnagar", "ज़िला (UP)", "उत्तर प्रदेश", 135f),
        IndiaLocation("पीलीभीत", "Pilibhit", "ज़िला (UP)", "उत्तर प्रदेश", 230f),
        IndiaLocation("प्रतापगढ़", "Pratapgarh", "ज़िला (UP)", "उत्तर प्रदेश", 540f),
        IndiaLocation("प्रयागराज (इलाहाबाद)", "Prayagraj (Allahabad)", "ज़िला (UP)", "उत्तर प्रदेश", 560f),
        IndiaLocation("रायबरेली", "Raebareli", "ज़िला (UP)", "उत्तर प्रदेश", 460f),
        IndiaLocation("रामपुर", "Rampur", "ज़िला (UP)", "उत्तर प्रदेश", 145f),
        IndiaLocation("सहारनपुर", "Saharanpur", "ज़िला (UP)", "उत्तर प्रदेश", 195f),
        IndiaLocation("संभल", "Sambhal", "ज़िला (UP)", "उत्तर प्रदेश", 85f),
        IndiaLocation("संत कबीर नगर", "Sant Kabir Nagar", "ज़िला (UP)", "उत्तर प्रदेश", 610f),
        IndiaLocation("शाहजहांपुर", "Shahjahanpur", "ज़िला (UP)", "उत्तर प्रदेश", 230f),
        IndiaLocation("शामली", "Shamli", "ज़िला (UP)", "उत्तर प्रदेश", 145f),
        IndiaLocation("श्रावस्ती", "Shravasti", "ज़िला (UP)", "उत्तर प्रदेश", 500f),
        IndiaLocation("सिद्धार्थनगर", "Siddharthnagar", "ज़िला (UP)", "उत्तर प्रदेश", 600f),
        IndiaLocation("सीतापुर", "Sitapur", "ज़िला (UP)", "उत्तर प्रदेश", 350f),
        IndiaLocation("सोनभद्र", "Sonbhadra", "ज़िला (UP)", "उत्तर प्रदेश", 740f),
        IndiaLocation("सुल्तानपुर", "Sultanpur", "ज़िला (UP)", "उत्तर प्रदेश", 520f),
        IndiaLocation("उन्नाव", "Unnao", "ज़िला (UP)", "उत्तर प्रदेश", 390f),
        IndiaLocation("वाराणसी (बनारस)", "Varanasi (Kashi)", "ज़िला (UP)", "उत्तर प्रदेश", 660f),

        // 4. MAJOR STATES & UTs OF INDIA
        IndiaLocation("उत्तर प्रदेश", "Uttar Pradesh", "राज्य / UT", "उत्तर प्रदेश", 20f),
        IndiaLocation("दिल्ली", "Delhi", "राज्य / UT", "दिल्ली", 95f),
        IndiaLocation("हरियाणा", "Haryana", "राज्य / UT", "हरियाणा", 110f),
        IndiaLocation("राजस्थान", "Rajasthan", "राज्य / UT", "राजस्थान", 220f),
        IndiaLocation("उत्तराखंड", "Uttarakhand", "राज्य / UT", "उत्तराखंड", 220f),
        IndiaLocation("पंजाब", "Punjab", "राज्य / UT", "पंजाब", 310f),
        IndiaLocation("मध्य प्रदेश", "Madhya Pradesh", "राज्य / UT", "मध्य प्रदेश", 450f),
        IndiaLocation("बिहार", "Bihar", "राज्य / UT", "बिहार", 750f),
        IndiaLocation("गुजरात", "Gujarat", "राज्य / UT", "गुजरात", 850f),
        IndiaLocation("महाराष्ट्र", "Maharashtra", "राज्य / UT", "महाराष्ट्र", 1100f),
        IndiaLocation("पश्चिम बंगाल", "West Bengal", "राज्य / UT", "पश्चिम बंगाल", 1250f),
        IndiaLocation("झारखंड", "Jharkhand", "राज्य / UT", "झारखंड", 980f),
        IndiaLocation("छत्तीसगढ़", "Chhattisgarh", "राज्य / UT", "छत्तीसगढ़", 850f),
        IndiaLocation("हिमाचल प्रदेश", "Himachal Pradesh", "राज्य / UT", "हिमाचल प्रदेश", 380f),
        IndiaLocation("जम्मू और कश्मीर", "Jammu and Kashmir", "राज्य / UT", "जम्मू और कश्मीर", 650f),
        IndiaLocation("चंडीगढ़", "Chandigarh", "राज्य / UT", "चंडीगढ़", 290f),
        IndiaLocation("ओडिशा", "Odisha", "राज्य / UT", "ओडिशा", 1350f),
        IndiaLocation("असम", "Assam", "राज्य / UT", "असम", 1750f),
        IndiaLocation("तेलंगाना", "Telangana", "राज्य / UT", "तेलंगाना", 1300f),
        IndiaLocation("आंध्र प्रदेश", "Andhra Pradesh", "राज्य / UT", "आंध्र प्रदेश", 1550f),
        IndiaLocation("कर्नाटक", "Karnataka", "राज्य / UT", "कर्नाटक", 1750f),
        IndiaLocation("तमिलनाडु", "Tamil Nadu", "राज्य / UT", "तमिलनाडु", 2050f),
        IndiaLocation("केरल", "Kerala", "राज्य / UT", "केरल", 2400f),
        IndiaLocation("गोवा", "Goa", "राज्य / UT", "गोवा", 1700f),

        // 5. PROMINENT PILGRIMAGE & METRO DESTINATIONS
        IndiaLocation("वृंदावन", "Vrindavan", "प्रमुख शहर", "उत्तर प्रदेश", 105f),
        IndiaLocation("हरिद्वार", "Haridwar", "प्रमुख शहर", "उत्तराखंड", 220f),
        IndiaLocation("ऋषिकेश", "Rishikesh", "प्रमुख शहर", "उत्तराखंड", 245f),
        IndiaLocation("देहरादून", "Dehradun", "प्रमुख शहर", "उत्तराखंड", 275f),
        IndiaLocation("जयपुर (सालासर बालाजी मार्ग)", "Jaipur", "प्रमुख शहर", "राजस्थान", 310f),
        IndiaLocation("मेंहदीपुर बालाजी", "Mehandipur Balaji", "प्रमुख शहर", "राजस्थान", 210f),
        IndiaLocation("सालासर बालाजी", "Salasar Balaji", "प्रमुख शहर", "राजस्थान", 370f),
        IndiaLocation("मुंबई", "Mumbai", "प्रमुख शहर", "महाराष्ट्र", 1320f),
        IndiaLocation("कोलकाता", "Kolkata", "प्रमुख शहर", "पश्चिम बंगाल", 1380f),
        IndiaLocation("बेंगलुरु", "Bengaluru", "प्रमुख शहर", "कर्नाटक", 1950f),
        IndiaLocation("हैदराबाद", "Hyderabad", "प्रमुख शहर", "तेलंगाना", 1450f),
        IndiaLocation("अहमदाबाद", "Ahmedabad", "प्रमुख शहर", "गुजरात", 870f),
        IndiaLocation("सूरत", "Surat", "प्रमुख शहर", "गुजरात", 1080f),
        IndiaLocation("पुणे", "Pune", "प्रमुख शहर", "महाराष्ट्र", 1380f),
        IndiaLocation("इंदौर", "Indore", "प्रमुख शहर", "मध्य प्रदेश", 740f),
        IndiaLocation("भोपाल", "Bhopal", "प्रमुख शहर", "मध्य प्रदेश", 680f),
        IndiaLocation("पटना", "Patna", "प्रमुख शहर", "बिहार", 880f)
    )

    /**
     * Searches the Pan-India database for location matches.
     * Prioritizes prefix matches in Hindi/English, local villages and nearby locations.
     */
    fun search(query: String, maxLimit: Int = 8): List<IndiaLocation> {
        val q = query.trim().lowercase()
        if (q.length < 2) {
            // Default suggestions: Dungra Jaat and local nearby hubs
            return LOCATIONS.take(maxLimit)
        }

        val cleanedQ = q.replace(Regex("^(ग्राम|गांव|गाँव|तहसील|जिला|dist|district|post|post office|थाना|thana|श्री|shri)\\s+"), "")

        // Filter and sort:
        // 1. Starts with Hindi name
        // 2. Starts with English name
        // 3. Contains in Hindi
        // 4. Contains in English
        return LOCATIONS
            .map { loc ->
                val hi = loc.nameHindi.lowercase()
                val en = loc.nameEnglish.lowercase()

                val score = when {
                    hi.startsWith(cleanedQ) -> 100 - (loc.distanceKm / 50f).toInt().coerceAtMost(30)
                    en.startsWith(cleanedQ) -> 90 - (loc.distanceKm / 50f).toInt().coerceAtMost(30)
                    hi.contains(cleanedQ) -> 60 - (loc.distanceKm / 50f).toInt().coerceAtMost(20)
                    en.contains(cleanedQ) -> 50 - (loc.distanceKm / 50f).toInt().coerceAtMost(20)
                    else -> 0
                }
                Pair(loc, score)
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(maxLimit)
    }

    fun getAllLocations(): List<IndiaLocation> = LOCATIONS
}
