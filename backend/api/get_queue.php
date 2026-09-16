<?php
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
header("Access-Control-Allow-Origin: *");

$darbarDate = trim($_GET['date'] ?? date('Y-m-d'));
$pdo = getDB();

try {
    $settingsStmt = $pdo->query("SELECT * FROM ashram_settings WHERE id = 1 LIMIT 1");
    $settings = $settingsStmt->fetch() ?: [];

    $stmt = $pdo->prepare("SELECT * FROM tokens WHERE darbar_date = :darbar_date ORDER BY token_number ASC");
    $stmt->execute([':darbar_date' => $darbarDate]);
    $tokens = $stmt->fetchAll();

    $waiting = 0;
    $completed = 0;
    $cancelled = 0;
    foreach ($tokens as $t) {
        if ($t['status'] === 'WAITING') $waiting++;
        elseif ($t['status'] === 'COMPLETED') $completed++;
        elseif ($t['status'] === 'CANCELLED') $cancelled++;
    }

    echo json_encode([
        "success" => true,
        "darbar_date" => $darbarDate,
        "current_serving_token" => intval($settings['current_serving_token'] ?? 0),
        "total_tokens" => count($tokens),
        "waiting_count" => $waiting,
        "completed_count" => $completed,
        "cancelled_count" => $cancelled,
        "tokens" => $tokens
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
}
