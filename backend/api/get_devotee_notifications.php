<?php
// Fetch & manage unread notifications for a devotee by phone number
require_once __DIR__ . '/../config/db.php';

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-SBKD-API-KEY, x-sbkd-api-key");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$raw = file_get_contents('php://input');
$input = json_decode($raw, true) ?: $_POST;

$action = strtoupper(trim($input['action'] ?? $_GET['action'] ?? 'GET'));
$phone = trim($input['phone'] ?? $_GET['phone'] ?? '');
$cleanPhone = preg_replace('/[^0-9]/', '', $phone);
if (strlen($cleanPhone) > 10) {
    $cleanPhone = substr($cleanPhone, -10);
}

if (empty($cleanPhone)) {
    echo json_encode(["success" => true, "notifications" => []]);
    exit;
}

try {
    $pdo = getDB();

    if ($action === 'MARK_READ') {
        $notifId = intval($input['id'] ?? $_GET['id'] ?? 0);
        if ($notifId > 0) {
            $stmt = $pdo->prepare("UPDATE devotee_notifications SET is_read = 1 WHERE id = :id AND phone_number = :phone");
            $stmt->execute([':id' => $notifId, ':phone' => $cleanPhone]);
        } else {
            $stmt = $pdo->prepare("UPDATE devotee_notifications SET is_read = 1 WHERE phone_number = :phone");
            $stmt->execute([':phone' => $cleanPhone]);
        }
        echo json_encode(["success" => true, "message" => "अधिसूचना पढ़ी गई चिह्नित की गई।"]);
        exit;
    }

    $stmt = $pdo->prepare("SELECT * FROM devotee_notifications WHERE phone_number = :phone ORDER BY id DESC LIMIT 20");
    $stmt->execute([':phone' => $cleanPhone]);
    $notifications = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode([
        "success" => true,
        "count" => count($notifications),
        "notifications" => $notifications
    ], JSON_UNESCAPED_UNICODE);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => $e->getMessage()]);
}
