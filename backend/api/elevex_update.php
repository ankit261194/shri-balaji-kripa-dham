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
    "latest_version" => "5.9.28",
    "version_code" => 618,
    "force_update" => false,
    "title_en" => "ANKIT ELEVEX ULTIMATE v5.9.28 (100% Genuine Tools & Honest Diagnostics)",
    "title_hi" => "अंकित एलेवेक्स v5.9.28 (100% प्रामाणिक टूल्स व पारदर्शी डायग्नोस्टिक्स)",
    "release_notes_en" => "• 100% Genuine Diagnostics: Strictly requires error code or observed symptom (Zero fake camera reports).\n• Honest Tool Standards: Relabeled reference guides and test-point blueprints.\n• Direct In-App Auto-Update verified.\n• Strict MNC Onboarding: Language -> Role -> Secure Login.",
    "release_notes_hi" => "• 100% प्रामाणिक डायग्नोस्टिक्स: एरर कोड या वास्तविक लक्षण अनिवार्य (शून्य फेक कैमरा रिपोर्ट)।\n• पारदर्शी टूल मानक: मल्टीमीटर व मॉडबस को शुद्ध संदर्भ गाइड के रूप में स्पष्ट किया गया।\n• डायरेक्ट इन-ऐप ऑटो-अपडेट सत्यापित।\n• सख्त ऑनबोर्डिंग: भाषा चयन ➔ रोल चयन ➔ सुरक्षित लॉगिन।",
    "apk_download_url" => "https://shribalajikripadham.online/download.php?app=elevex",
    "file_size" => "38.6 MB"
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
exit;
