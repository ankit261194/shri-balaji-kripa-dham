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
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-SBKD-API-KEY, X-SBKD-ADMIN-TOKEN, X-SBKD-HMAC-AUTH");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if (function_exists('verifyApiAuth')) {
    verifyApiAuth();
}

$input = json_decode(file_get_contents('php://input'), true) ?: $_POST;
$id = intval($input['id'] ?? 0);
$parchaId = trim($input['parcha_id'] ?? '');

if ($id <= 0 && empty($parchaId)) {
    http_response_code(400);
    echo json_encode(["success" => false, "error" => "मान्य पर्चा ID अथवा parcha_id अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
    exit;
}

$pdo = getDB();

try {
    if (!empty($parchaId)) {
        // First check if column parcha_id exists
        try {
            $stmt = $pdo->prepare("DELETE FROM sacred_parchas WHERE parcha_id = :pid");
            $stmt->execute([':pid' => $parchaId]);
        } catch (Exception $e) {
            // Fallback to numeric extraction if parcha_id column is absent
            $cleanId = intval(preg_replace('/[^0-9]/', '', $parchaId));
            if ($cleanId > 0) {
                $stmt = $pdo->prepare("DELETE FROM sacred_parchas WHERE id = :id");
                $stmt->execute([':id' => $cleanId]);
            }
        }
    } else {
        $stmt = $pdo->prepare("DELETE FROM sacred_parchas WHERE id = :id");
        $stmt->execute([':id' => $id]);
    }

    echo json_encode([
        "success" => true,
        "id" => $id,
        "parcha_id" => $parchaId,
        "message" => "पर्चा सफलतापूर्वक हटाया गया।"
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
}
