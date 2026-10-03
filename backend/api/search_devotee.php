<?php
// ==============================================================================
// श्री बालाजी कृपा धाम - केंद्रीय भक्त खोज व स्वतः-पूर्ति सेवा (Cloud Auto-Fill API)
// High-Speed Universal Devotee Directory & Auto-Fill Search API
// ==============================================================================

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
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$query = trim($_GET['query'] ?? $_GET['q'] ?? $_GET['phone'] ?? $_GET['name'] ?? $_POST['query'] ?? '');
$limit = min(15, max(1, intval($_GET['limit'] ?? $_POST['limit'] ?? 6)));

if (empty($query) || mb_strlen($query, 'UTF-8') < 2) {
    echo json_encode(["success" => true, "count" => 0, "devotees" => []]);
    exit;
}

$cleanPhone = preg_replace('/[^0-9]/', '', $query);
$isNumericSearch = (strlen($cleanPhone) >= 3);

try {
    $pdo = getDB();
    if (!$pdo) {
        echo json_encode(["success" => false, "error" => "डेटाबेस अनुपलब्ध है"]);
        exit;
    }

    $results = [];
    $seenPhones = [];

    // 1. Search in devotee_profiles table first
    if ($isNumericSearch) {
        $stmt = $pdo->prepare("SELECT patient_name, phone_number, city, photo_url, total_darshans, last_darbar_date 
                               FROM devotee_profiles 
                               WHERE phone_number LIKE :phone 
                               ORDER BY total_darshans DESC, last_darbar_date DESC 
                               LIMIT :lim");
        $stmt->bindValue(':phone', '%' . $cleanPhone . '%', PDO::PARAM_STR);
        $stmt->bindValue(':lim', $limit, PDO::PARAM_INT);
        $stmt->execute();
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
        foreach ($rows as $r) {
            $ph10 = substr(preg_replace('/[^0-9]/', '', $r['phone_number']), -10);
            if (!isset($seenPhones[$ph10])) {
                $seenPhones[$ph10] = true;
                $results[] = [
                    "patient_name" => $r['patient_name'],
                    "phone_number" => $r['phone_number'],
                    "city" => $r['city'] ?: "डूँगरा जाट",
                    "photo_url" => $r['photo_url'] ?: "",
                    "visit_count" => (int)($r['total_darshans'] ?: 1),
                    "last_visit_date" => $r['last_darbar_date'] ?: ""
                ];
            }
        }
    }

    // 2. Search by Name or Phone in tokens table if more results needed
    if (count($results) < $limit) {
        $remaining = $limit - count($results);
        $sql = "SELECT patient_name, phone_number, city, photo_url, darbar_date, COUNT(*) as visit_count 
                FROM tokens 
                WHERE ";
        $params = [];
        
        if ($isNumericSearch) {
            $sql .= "phone_number LIKE :search ";
            $params[':search'] = '%' . $cleanPhone . '%';
        } else {
            $sql .= "(patient_name LIKE :search OR city LIKE :search) ";
            $params[':search'] = '%' . $query . '%';
        }
        
        $sql .= "GROUP BY phone_number, patient_name 
                 ORDER BY visit_count DESC, id DESC 
                 LIMIT " . intval($remaining);

        $stmtTokens = $pdo->prepare($sql);
        $stmtTokens->execute($params);
        $tokenRows = $stmtTokens->fetchAll(PDO::FETCH_ASSOC);

        foreach ($tokenRows as $r) {
            $ph10 = substr(preg_replace('/[^0-9]/', '', $r['phone_number']), -10);
            if (!isset($seenPhones[$ph10])) {
                $seenPhones[$ph10] = true;
                $results[] = [
                    "patient_name" => $r['patient_name'],
                    "phone_number" => $r['phone_number'],
                    "city" => $r['city'] ?: "डूँगरा जाट",
                    "photo_url" => $r['photo_url'] ?: "",
                    "visit_count" => (int)($r['visit_count'] ?: 1),
                    "last_visit_date" => $r['darbar_date'] ?: ""
                ];
            }
        }
    }

    echo json_encode([
        "success" => true,
        "count" => count($results),
        "devotees" => $results
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
}
