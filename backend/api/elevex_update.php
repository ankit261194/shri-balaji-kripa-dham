<?php
// ==============================================================================
// श्री बालाजी कृपा धाम - Ankit EleveX Direct In-App Auto-Update API Engine
// Dedicated high-speed JSON endpoint avoiding Apache directory 301 redirects
// Multi-Flavor Dynamic Routing (superadmin, supervisor, technician, owner, gateway)
// ==============================================================================

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Cache-Control: no-cache, no-store, must-revalidate, max-age=0");
header("Pragma: no-cache");
header("Expires: 0");

$rawApp = strtolower(trim($_GET['app'] ?? $_GET['package'] ?? ''));
if (strpos($rawApp, 'superadmin') !== false) {
    $app = 'superadmin';
} elseif (strpos($rawApp, 'supervisor') !== false) {
    $app = 'supervisor';
} elseif (strpos($rawApp, 'technician') !== false) {
    $app = 'technician';
} elseif (strpos($rawApp, 'owner') !== false) {
    $app = 'owner';
} else {
    $app = 'gateway';
}

$simulate = isset($_GET['simulate']) || isset($_GET['test']);

$manifestPath = __DIR__ . '/../elevex_gateway/app_update.json';
$data = null;

if (file_exists($manifestPath)) {
    $content = @file_get_contents($manifestPath);
    if (!empty($content)) {
        $data = json_decode($content, true);
    }
}

if (!is_array($data)) {
    $data = [
        "latest_version" => "5.9.41",
        "version_code" => 631,
        "force_update" => false,
        "title_en" => "ANKIT ELEVEX PRO v5.9.41 - Enterprise 4-App Update",
        "title_hi" => "अंकित एलेवेक्स प्रो v5.9.41 - एंटरप्राइज 4-ऐप अपडेट",
        "release_notes_en" => "• Universal in-app auto update for all 4 standalone flavors\n• Direct sub-second APK download with live progress\n• 1-Tap manual update check button on top bar",
        "release_notes_hi" => "• चारों स्टैंडअलोन ऐप्स के लिए यूनिवर्सल इन-ऐप ऑटो अपडेट\n• लाइव प्रोग्रेस बार के साथ डायरेक्ट APK डाउनलोड\n• टॉप बार में 1-टैप मैन्युअल अपडेट चेक बटन",
        "file_size" => "39.0 MB",
        "sha256_checksum" => "",
        "verified_oem_brands" => ["MONARCH", "STEP", "OTIS", "SCHINDLER", "KONE"]
    ];
}

// Dynamically inject app-specific download URL
$data['apk_download_url'] = "https://shribalajikripadham.online/download.php?app={$app}";

if ($simulate) {
    $data['version_code'] = ($data['version_code'] ?? 630) + 10;
    $data['simulate_newer'] = true;
}

echo json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
exit;
