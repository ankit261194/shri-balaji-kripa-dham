<?php
if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} else {
    require_once __DIR__ . '/config/db.php';
}

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["success" => false, "error" => "Method Not Allowed"]);
    exit;
}

$input = json_decode(file_get_contents('php://input'), true) ?: $_POST;

$darbarDate = trim($input['darbar_date'] ?? date('Y-m-d'));
$tokenNumber = intval($input['token_number'] ?? 0);
$status = strtoupper(trim($input['status'] ?? ''));

if ($tokenNumber <= 0 || empty($status)) {
    http_response_code(400);
    echo json_encode(["success" => false, "error" => "टोकन नंबर और स्थिति अनिवार्य हैं।"], JSON_UNESCAPED_UNICODE);
    exit;
}

$validStatuses = ['WAITING', 'SERVING', 'COMPLETED', 'CANCELLED'];
if (!in_array($status, $validStatuses)) {
    http_response_code(400);
    echo json_encode(["success" => false, "error" => "अमान्य टोकन स्थिति।"], JSON_UNESCAPED_UNICODE);
    exit;
}

$pdo = getDB();

try {
    $pdo->beginTransaction();

    $isCompleted = ($status === 'COMPLETED') ? 1 : 0;
    $completedAt = ($status === 'COMPLETED') ? (time() * 1000) : null;

    $stmt = $pdo->prepare("UPDATE tokens SET
        status = :status,
        is_darshan_completed = :completed,
        darshan_completed_at = COALESCE(:completed_at, darshan_completed_at)
        WHERE darbar_date = :darbar_date AND token_number = :token_number");
    
    $stmt->execute([
        ':status' => $status,
        ':completed' => $isCompleted,
        ':completed_at' => $completedAt,
        ':darbar_date' => $darbarDate,
        ':token_number' => $tokenNumber
    ]);

    // If setting to SERVING, update current_serving_token in ashram_settings
    if ($status === 'SERVING') {
        $upSettings = $pdo->prepare("UPDATE ashram_settings SET current_serving_token = :tok WHERE id = 1");
        $upSettings->execute([':tok' => $tokenNumber]);
    }

    $pdo->commit();

    echo json_encode([
        "success" => true,
        "darbar_date" => $darbarDate,
        "token_number" => $tokenNumber,
        "status" => $status,
        "message" => "टोकन $tokenNumber की स्थिति '$status' सफलतापूर्वक अपडेट हुई।"
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    http_response_code(500);
    echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
}
