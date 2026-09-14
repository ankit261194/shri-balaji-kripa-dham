package com.example.shribalajikripadham.data.model

data class AshramSettings(
    val id: Long = 1,
    val ashramName: String = "Shri Balaji Kripa Dham",
    val gurujiName: String = "Guruji Tejveer Singh Ji",
    val address: String = "Gram Dungra Jaat, Bulandshahr, UP",
    val latitude: Double = 28.3972915,
    val longitude: Double = 78.1460410,
    val allowedRadiusMeters: Double = 1500.0,
    val runningTokenNumber: Int = 1,
    val isDarbarActive: Boolean = true,
    val darbarDate: String = "",
    val darbarTimings: String = "प्रत्येक रविवार प्रातः 7:00 बजे से (Every Sunday from 7:00 AM)",
    val freeDisclaimer: String = "भूत-प्रेत व मानसिक समस्याओं का पूर्णतः निःशुल्क (FREE) इलाज। कोई शुल्क अथवा दक्षिणा नहीं ली जाती।",
    val contactPhone: String = "+91 98765 00000",
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
    // In-app auto update system configuration
    val latestVersionCode: Int = 3,
    val latestVersionName: String = "2.2.0",
    val updateNotes: String = "नया अपडेट v2.2.0: 8 नए सुपर एडमिन नियंत्रण फीचर्स, पासवर्ड चेंज, CSV एक्सपोर्ट, टोकन डिलीट, डेली कोटा...",
    val apkDownloadUrl: String = "",
    val isForceUpdate: Boolean = false,
    // Social Media Links & App Sharing
    val whatsappGroupUrl: String = "https://chat.whatsapp.com/invite",
    val whatsappNumber: String = "+919876543210",
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
    val isUiLayoutEnforced: Boolean = false,
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
    val busSeatFareAmount: Int = 1500
)

data class CustomCityDistance(
    val id: Long = 0,
    val cityName: String,
    val distanceKm: Float,
    val createdAt: String = ""
)
