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
    "latest_version" => "5.9.29",
    "version_code" => 619,
    "force_update" => true,
    "title_en" => "ANKIT ELEVEX ULTIMATE v5.9.29 (SuperAdmin Gateway & Industrial Core)",
    "title_hi" => "अंकित एलेवेक्स v5.9.29 (सुपर एडमिन गेटवे व इंडस्ट्रियल कोर इंजन)",
    "release_notes_en" => "• Launch Gate Splash: Unskippable in-splash direct update download & native installer.\n• SuperAdmin Master Gateway: Dedicated login (8533955333) with live multi-role UI switcher.\n• 3 Genuine Killer Tools: Half-Split Safety Loop Isolator, Cross-Brand VFD Translator, Ghost Tripping Blackbox Recorder with step-by-step field manuals.\n• Ankit EleveX Industrial Core Diagnostics: 100% genuine hardware diagnostics with zero demo paywalls or fake camera results.",
    "release_notes_hi" => "• स्प्लैश ऑटो-अपडेट गेट: ऐप खुलते ही लाइव डाउनलोड प्रोग्रेस बार व सीधा इंस्टॉलर।\n• सुपर एडमिन मास्टर गेटवे: अंकित चौधरी (8533955333) के लिए सीधा प्रवेश व 4-रोल लाइव सिम्युलेटर।\n• 3 नए प्रामाणिक प्रो टूल्स: हाफ-स्प्लिट डोर सेफ्टी लोकेटर, क्रॉस-ब्रांड VFD ट्रांसलेटर व घोस्ट ट्रिपिंग ब्लैकबॉक्स (स्पष्ट गाइड सहित)।\n• अंकित एलिवेक्स इंडस्ट्रियल कोर इंजन: 100% वास्तविक हार्डवेयर डायग्नोस्टिक्स, बिना किसी फेक रिपोर्ट या टाइमर पेमेंट के।",
    "apk_download_url" => "https://shribalajikripadham.online/download.php?app=elevex",
    "file_size" => "40.5 MB"
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
exit;
