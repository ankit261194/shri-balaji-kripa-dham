package com.example.shribalajikripadham.data.model

data class AshramSettings(
    val id: Long = 1,
    val ashramName: String = "Shri Balaji Kripa Dham",
    val gurujiName: String = "Guruji Tejveer Singh Ji",
    val address: String = "ग्राम डूंगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर (उ.प्र.)",
    val latitude: Double = 28.3972915,
    val longitude: Double = 78.1460410,
    val allowedRadiusMeters: Double = 200.0,
    val runningTokenNumber: Int = 1,
    val isDarbarActive: Boolean = true,
    val darbarDate: String = "",
    val darbarTimings: String = "प्रत्येक रविवार प्रातः 7:00 बजे से (Every Sunday from 7:00 AM)",
    val freeDisclaimer: String = "भूत-प्रेत व मानसिक समस्याओं का पूर्णतः निःशुल्क (FREE) इलाज। कोई शुल्क अथवा दक्षिणा नहीं ली जाती।",
    val contactPhone: String = "+91 97206 91090",
    val emergencyNoticeText: String = "",
    // Public service visibility toggles for devotees
    val isTokenServiceEnabled: Boolean = true,
    val isYatraServiceEnabled: Boolean = false, // Permanently hidden until explicitly opened by Super Admin
    val isLiveCounterVisible: Boolean = true,
    val isEventsVisible: Boolean = true,
    val isAartiTimingsVisible: Boolean = true,
    val isGurujiInfoVisible: Boolean = true,
    val isEmergencyNoticeVisible: Boolean = true,
    val scheduledTokenOpenTimestamp: Long = 0L, // 0 means immediate/always open during Darbar
    val isGeofenceEnforced: Boolean = true,
    val isOutstationAdvanceAllowed: Boolean = true,
    val outstationMinDistanceKm: Double = 30.0,
    // In-app auto update system configuration
    val latestVersionCode: Int = 80,
    val latestVersionName: String = "2.56.5",
    val updateNotes: String = "सुरक्षा पैच एवं सिस्टम स्थिरता सुधार (Security Patch Update)",
    val apkDownloadUrl: String = "",
    val isForceUpdate: Boolean = false,
    // Social Media Links & App Sharing
    val whatsappGroupUrl: String = "https://chat.whatsapp.com/invite",
    val whatsappNumber: String = "+91 97206 91090",
    val youtubeChannelUrl: String = "https://www.youtube.com/@ShriBalajiKripaDham",
    val facebookPageUrl: String = "https://www.facebook.com/ShriBalajiKripaDham",
    val instagramUrl: String = "https://www.instagram.com/shribalajikripadham",
    val appShareUrl: String = "https://shribalajikripadham.org/app",
    val currentThemeId: String = "maroon",
    val gurujiPhotoUri: String = "",
    val activeUiLayout: String = "CLASSIC_DARBAR",
    // Daily token limit quota (0 = unlimited)
    val maxDailyTokens: Int = 0,
    // Whether Super Admin enforces activeUiLayout across all devotees
    val isUiLayoutEnforced: Boolean = true,
    // Token TTS Human Voice Preset (GURU_CALM, FEMALE_SWEET, ANNOUNCER_MALE, SEVIKA_FEMALE, YOUTH_CRISP, TRADITIONAL_VYAS)
    val tokenVoicePreset: String = "GURU_CALM",
    // Ashram Main Home Banner Manager (Super Admin Controlled)
    val bannerPhotoUri: String = "",
    val isBannerVisible: Boolean = true,
    val bannerTitle: String = "🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट",
    val bannerSubtitle: String = "परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार",
    val bannerActionUrl: String = "",
    // Ads & Devotee Sponsorship Master Control (Super Admin 100% Controlled)
    val isAdsEnabled: Boolean = false,
    val adType: String = "CUSTOM", // CUSTOM, SPONSOR, ADMOB
    val adBannerPhotoUri: String = "",
    val adBannerTitle: String = "आश्रम सेवा व गौशाला सहयोग",
    val adBannerDescription: String = "धर्मार्थ सेवा, लंगर व गौशाला में सहयोग करें।",
    val adTargetUrl: String = "",
    val adPlacement: String = "HOME_BOTTOM",
    // Cloud Sync Server Endpoint
    val cloudSyncUrl: String = "",
    val isCloudSyncEnabled: Boolean = false,
    // Sunday Token Screen Dynamic Banner & Custom Notice
    val sundayTokenBannerTitle: String = "हार्डवेयर फिंगरप्रिंट नियम: 1 फोन = 1 टोकन",
    val sundayTokenBannerText: String = "एक मोबाइल डिवाइस से प्रत्येक रविवार को केवल 1 मरीज का टोकन लिया जा सकता है।",
    val sundayTokenCustomNotice: String = "",
    // Bus & Payment Super Admin Controls
    val isBusBookingLive: Boolean = false,
    val isPaymentFeatureLive: Boolean = false,
    val canAdminViewPaymentHistory: Boolean = false,
    val canDevoteeViewPaymentHistory: Boolean = false,
    val ashramUpiId: String = "shribalajikripadham@upi",
    val ashramUpiName: String = "Shri Balaji Kripa Dham",
    val customUpiQrUri: String = "",
    val busSeatFareAmount: Int = 0,
    // Sacred Arzi Box Super Admin Controls
    val isArziLedgerLive: Boolean = true,
    val badiArziRate: Double = 0.0,
    val chhotiArziRate: Double = 0.0,
    val canAdminViewArziLedger: Boolean = true,
    val canDevoteeViewArziLedger: Boolean = false,
    val canDevoteeViewYatraDiary: Boolean = false,
    // Editable Ashram Parichay, History & Rules
    val ashramParichayHindi: String = "श्री बालाजी कृपा धाम (ग्राम डूंगरा जाट, तहसील अनूपशहर, ज़िला बुलन्दशहर, उ.प्र.) में परम पूज्य गुरुजी तेजवीर सिंह जी के मार्गदर्शन में भूत-प्रेत, ऊपरी बाधा व मानसिक कष्टों का इलाज 100% निःशुल्क किया जाता है। यहाँ किसी भी प्रकार का चढ़ावा या दक्षिणा नहीं ली जाती।",
    val ashramParichayEnglish: String = "At Shri Balaji Kripa Dham (Gram Dungra Jaat, Anoopshahr, Bulandshahr, UP), spiritual and mental ailments are healed 100% free under Guruji Tejveer Singh Ji. No fee or donation is ever accepted.",
    val ashramHistoryHindi: String = "परम पूज्य गुरुजी को श्री बालाजी महाराज व भैरव बाबा का साक्षात आशीर्वाद प्राप्त है। पिछले कई वर्षों से डूंगरा जाट धाम पर लाखों पीड़ित भक्तों को नई जिंदगी और शांति मिली है।",
    val ashramRulesHindi: String = "1. प्रत्येक रविवार प्रातःकाल से दरबार प्रारंभ होता है।\n2. टोकन केवल आश्रम परिसर (200m परिधि) में भौतिक रूप से उपस्थित होने पर ही मिलेगा।\n3. एक मोबाइल से 1 ही टोकन बनेगा।\n4. पूर्ण शांति, स्वच्छता व मर्यादा बनाए रखें।",
    val allowAdminReservedTokens: Boolean = false,
    val isDarbarLiveNow: Boolean = false,
    val liveStreamTitle: String = "श्री बालाजी कृपा धाम दिव्य दरबार लाइव",
    val liveStreamUrl: String = "",
    val youtubeLiveUrl: String = "",
    val facebookLiveUrl: String = "",
    val officialWebsiteUrl: String = "https://shribalajikripadham.online",
    // Full Website CMS Fields (SuperAdmin Editable)
    val websiteTopBarText: String = "🚩 ॐ श्री हनुमते नमः | परम पूज्य गुरुजी तेजवीर सिंह जी | श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, बुलन्दशहर 🚩",
    val websiteGurujiTitle: String = "परम पूज्य गुरुजी तेजवीर सिंह जी",
    val websiteGurujiBio: String = "अध्यात्म, मानव सेवा एवं बालाजी महाराज की असीम कृपा के संवाहक",
    val websiteTokenRuleNotice: String = "आश्रम की निष्पक्षता, पारदर्शी कतार, GPS लोकेशन एवं AI बायोमेट्रिक सुरक्षा नियमों के अनुसार टोकन पंजीकरण केवल और केवल आधिकारिक मोबाइल ऐप से ही संभव है। वेबसाइट पर कोई टोकन जनरेशन फॉर्म नहीं है। टोकन प्राप्त करने के लिए कृपया ऊपर दिए गए बटन से मोबाइल ऐप इंस्टॉल करें।",
    val websiteAartiMangala: String = "प्रातः 05:30 बजे",
    val websiteAartiBalbhog: String = "प्रातः 08:00 बजे",
    val websiteAartiSandhya: String = "सायं 07:00 बजे",
    val websiteAartiShayan: String = "रात्रि 09:00 बजे",
    val websiteBankName: String = "पंजाब नेशनल बैंक (PNB)",
    val websiteAccountHolder: String = "श्री बालाजी कृपा धाम सेवा ट्रस्ट",
    val websiteAccountNumber: String = "",
    val websiteBankIfsc: String = "",
    val websiteBankBranch: String = "अनूपशहर, बुलन्दशहर",
    val websiteAshramAddress: String = "श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर, उत्तर प्रदेश",
    val websiteAshramDirections: String = "निकटतम रेलवे स्टेशन: अनूपशहर / बबराला / बुलन्दशहर",
    val websiteContactEmail: String = "shribalajikripadham@gmail.com",
    val websiteInstagramUrl: String = "https://www.instagram.com/shribalajikripadham",
    val websiteWhatsappChannelUrl: String = "https://chat.whatsapp.com/invite",
    val websiteFooterTitle: String = "श्री बालाजी कृपा धाम",
    val websiteFooterDedication: String = "सर्वस्व श्री रामभक्त वीर हनुमान जी महाराज के पावन चरणों में समर्पित।",
    val websiteFooterCopyright: String = "© 2026 श्री बालाजी कृपा धाम सेवा ट्रस्ट। सर्वाधिकार सुरक्षित।"
)

data class CustomCityDistance(
    val id: Long = 0,
    val cityName: String,
    val distanceKm: Float,
    val createdAt: String = ""
)
