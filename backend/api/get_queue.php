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
$darbarVenue = trim($_GET['venue'] ?? $_GET['darbar_venue'] ?? '');
$pdo = getDB();

try {
    $settingsStmt = $pdo->query("SELECT * FROM ashram_settings WHERE id = 1 LIMIT 1");
    $settings = $settingsStmt->fetch() ?: [];

    $sql = "SELECT * FROM tokens WHERE darbar_date = :darbar_date";
    $params = [':darbar_date' => $darbarDate];
    if (!empty($darbarVenue)) {
        $sql .= " AND darbar_venue = :darbar_venue";
        $params[':darbar_venue'] = $darbarVenue;
    }
    $sql .= " ORDER BY token_number ASC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $tokens = $stmt->fetchAll();

    $waiting = 0;
    $completed = 0;
    $cancelled = 0;
    $absent = 0;
    foreach ($tokens as $t) {
        $st = strtoupper($t['status'] ?? '');
        if ($st === 'COMPLETED' || !empty($t['is_darshan_completed'])) {
            $completed++;
        } elseif ($st === 'ABSENT') {
            $absent++;
        } elseif ($st === 'CANCELLED') {
            $cancelled++;
        } else {
            $waiting++;
        }
    }

    echo json_encode([
        "success" => true,
        "darbar_date" => $darbarDate,
        "current_serving_token" => intval($settings['current_serving_token'] ?? 0),
        "total_tokens" => count($tokens),
        "waiting_count" => $waiting,
        "completed_count" => $completed,
        "cancelled_count" => $cancelled,
        "absent_count" => $absent,
        "tokens" => $tokens
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
}
