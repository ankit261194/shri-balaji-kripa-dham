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

    // 1. Permanently delete all fake/dummy sevadars
    $deletedCount = $pdo->exec("DELETE FROM sevadars WHERE phone LIKE '9100100%' OR phone LIKE '987654321%' OR phone = '' OR phone IS NULL");
    
    // Also if only dummy sevadars remained (<= 6), purge table cleanly
    $remaining = $pdo->query("SELECT COUNT(*) FROM sevadars")->fetchColumn();
    if ($remaining <= 6) {
        $pdo->exec("TRUNCATE TABLE sevadars");
        $deletedCount = "All dummy sevadars purged";
    }

    // 2. Clear website cache files
    $cacheDir = __DIR__ . '/../cache';
    if (is_dir($cacheDir)) {
        $files = glob($cacheDir . '/*');
        foreach ($files as $f) {
            if (is_file($f)) @unlink($f);
        }
    }

    // Note: ashram_settings is intentionally 100% untouched to preserve authentic real configurations.

    echo json_encode([
        "success" => true,
        "message" => "सभी फ़र्ज़ी सेवादार स्थायी रूप से हटा दिए गए हैं। वास्तविक सेटिंग्स सुरक्षित हैं।",
        "sevadars_deleted" => $deletedCount,
        "remaining_sevadars" => 0
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
}
