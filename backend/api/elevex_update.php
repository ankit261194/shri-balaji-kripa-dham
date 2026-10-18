<?php
// ==============================================================================
// श्री बालाजी कृपा धाम - Ankit EleveX Direct In-App Auto-Update API Engine
// Dedicated high-speed JSON endpoint avoiding Apache directory 301 redirects
// ==============================================================================

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Cache-Control: no-cache, no-store, must-revalidate, max-age=0");
header("Pragma: no-cache");
header("Expires: 0");

$manifestPath = __DIR__ . '/../elevex_gateway/app_update.json';

if (file_exists($manifestPath)) {
    $content = @file_get_contents($manifestPath);
    if (!empty($content) && json_decode($content) !== null) {
        echo $content;
        exit;
    }
}

// Fallback dynamic manifest
echo json_encode([
    "latest_version" => "5.9.35",
    "version_code" => 625,
    "force_update" => true,
    "title_en" => "ANKIT ELEVEX PRO v5.9.35 Permanent Login & Instant OTP Released!",
    "title_hi" => "अंकित एलेवेक्स प्रो v5.9.35 परमानेंट लॉगिन व तत्काल OTP अपडेट उपलब्ध!",
    "release_notes_en" => "• Permanent Zero-Logout Session: Log in once and stay logged in forever across app restarts, reboots, and updates.\n• Frictionless OTP Engine: Instant 1-tap OTP verification for all mobile numbers with automatic account setup.\n• 30-Day Cryptographic Offline Lease: Full functionality in machine rooms and basement pits without internet.\n• Anti-Piracy Single Device Binding: 1 phone = 1 account with seamless upgrade migration.",
    "release_notes_hi" => "• परमानेंट जीरो-लॉगआउट सत्र: एक बार लॉगिन करें और ऐप रीस्टार्ट, फोन रीबूट या अपडेट के बाद भी हमेशा लॉगिन रहें।\n• फ्रिक्शनलेस OTP इंजन: किसी भी मोबाइल नंबर के लिए तत्काल 1-टैप OTP सत्यापन व स्वतः खाता सक्रियण।\n• 30-दिन का ऑफ़लाइन लाइसेंस: मशीन रूम व बेसमेंट पिट में बिना इंटरनेट 100% कार्यक्षमता।\n• एंटी-पायरेसी सिंगल डिवाइस लॉक: 1 नंबर = 1 फोन सुरक्षा।",
    "apk_download_url" => "https://shribalajikripadham.online/download.php?app=elevex",
    "file_size" => "39.0 MB"
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
exit;
