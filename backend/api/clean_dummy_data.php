<?php
date_default_timezone_set('Asia/Kolkata');

if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} elseif (file_exists(__DIR__ . '/config/db.php')) {
    require_once __DIR__ . '/config/db.php';
} else {
    if (!defined('DB_HOST')) define('DB_HOST', 'localhost');
    if (!defined('DB_NAME')) define('DB_NAME', 'u237101617_balaji');
    if (!defined('DB_USER')) define('DB_USER', 'u237101617_ankitantim0');
    if (!defined('DB_PASS')) define('DB_PASS', 'Aa@8006518960');
    function getDB() {
        $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4";
        return new PDO($dsn, DB_USER, DB_PASS, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
        ]);
    }
}

header('Content-Type: application/json; charset=utf-8');

try {
    $pdo = getDB();

    // 1. Permanently delete all fake/dummy sevadars (with dummy numbers or placeholder entries)
    $deletedCount = $pdo->exec("DELETE FROM sevadars WHERE phone LIKE '9100100%' OR phone LIKE '987654321%' OR phone = '' OR phone IS NULL");
    
    // Also if only dummy sevadars remained, clean table
    $remaining = $pdo->query("SELECT COUNT(*) FROM sevadars")->fetchColumn();
    if ($remaining <= 6) {
        $pdo->exec("TRUNCATE TABLE sevadars");
        $deletedCount = "All dummy sevadars purged";
    }

    // 2. Clean ashram_directions and enforce exact darbar timings (8:30 AM to 5:00 PM) in ashram_settings
    $stmt = $pdo->prepare("UPDATE ashram_settings SET 
        darbar_timings = 'प्रत्येक रविवार प्रातः 8:30 बजे से सायं 5:00 बजे तक',
        ashram_directions = '',
        ashram_latitude = 28.3972915,
        ashram_longitude = 78.1460410,
        latitude = 28.3972915,
        longitude = 78.1460410,
        allowed_radius_meters = 200,
        outstation_min_distance_km = 30,
        is_outstation_advance_allowed = 1,
        is_geofence_enforced = 1,
        token_service_mode = 'AUTO_SUNDAY'
        WHERE id = 1");
    $stmt->execute();

    // 3. Clear website cache files
    $cacheDir = __DIR__ . '/../cache';
    if (is_dir($cacheDir)) {
        $files = glob($cacheDir . '/*');
        foreach ($files as $f) {
            if (is_file($f)) @unlink($f);
        }
    }

    echo json_encode([
        "success" => true,
        "message" => "सभी फ़र्ज़ी डेटा (नकली सेवादार, रेलवे स्टेशन रूट, ग़लत समय) पूर्णतः साफ़ कर दिए गए हैं!",
        "sevadars_deleted" => $deletedCount,
        "updated_darbar_timings" => "प्रत्येक रविवार प्रातः 8:30 बजे से सायं 5:00 बजे तक",
        "ashram_directions" => "CLEARED",
        "ashram_coordinates" => "28.3972915, 78.1460410 (ग्राम डूँगरा जाट 100% लॉक्ड)"
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
}
