<?php
// Central FCM Push Notification Dispatcher
// Shri Balaji Kripa Dham
require_once __DIR__ . '/../config/db.php';

header('Content-Type: application/json; charset=utf-8');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

function sendFcmToTokens($tokens, $title, $body, $data = [], $apiKey = '') {
    if (empty($tokens)) return ["sent" => 0, "failed" => 0];

    // Default legacy FCM server key or API key if configured
    if (empty($apiKey)) {
        // Fallback key
        $apiKey = 'AIzaSyDbJQvMUopfPb0at-_upRMnR6MjL6z9lRo';
    }

    $url = 'https://fcm.googleapis.com/fcm/send';
    $results = ["sent" => 0, "failed" => 0, "responses" => []];

    // Chunk into 500 max per FCM request
    $tokenChunks = array_chunk($tokens, 500);

    foreach ($tokenChunks as $chunk) {
        $fields = [
            'registration_ids' => $chunk,
            'priority' => 'high',
            'notification' => [
                'title' => $title,
                'body' => $body,
                'sound' => 'default',
                'badge' => '1',
                'channel_id' => 'ashram_darbar_channel'
            ],
            'data' => array_merge([
                'title' => $title,
                'body' => $body,
                'click_action' => 'FLUTTER_NOTIFICATION_CLICK'
            ], $data)
        ];

        $headers = [
            'Authorization: key=' . $apiKey,
            'Content-Type: application/json'
        ];

        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
        curl_setopt($ch, CURLOPT_TIMEOUT, 10);

        $result = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($result && $httpCode == 200) {
            $resp = json_decode($result, true);
            $results["sent"] += intval($resp['success'] ?? 0);
            $results["failed"] += intval($resp['failure'] ?? 0);
            $results["responses"][] = $resp;
        } else {
            $results["failed"] += count($chunk);
            $results["responses"][] = ["http_code" => $httpCode, "raw" => $result];
        }
    }

    return $results;
}

try {
    $pdo = getDB();

    // Auto-create devotee notifications table
    $pdo->exec("CREATE TABLE IF NOT EXISTS devotee_notifications (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        phone_number VARCHAR(50) NOT NULL,
        token_number INT DEFAULT 0,
        title VARCHAR(255) NOT NULL,
        message TEXT NOT NULL,
        type VARCHAR(50) DEFAULT 'TOKEN_CALL',
        is_read TINYINT DEFAULT 0,
        created_at BIGINT DEFAULT 0,
        INDEX idx_phone (phone_number),
        INDEX idx_created (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

    $raw = file_get_contents('php://input');
    $input = json_decode($raw, true) ?: $_POST;

    $phoneNumber = trim($input['phone_number'] ?? '');
    $tokenNumber = intval($input['token_number'] ?? 0);
    $patientName = trim($input['patient_name'] ?? '');
    $title = trim($input['title'] ?? '');
    $body = trim($input['body'] ?? '');
    $type = trim($input['type'] ?? 'TOKEN_CALL');

    if (empty($title)) {
        if ($tokenNumber > 0) {
            $title = "🔔 टोकन बुलावा: टोकन #$tokenNumber";
        } else {
            $title = "श्री बालाजी कृपा धाम";
        }
    }

    if (empty($body)) {
        if ($tokenNumber > 0) {
            $body = !empty($patientName)
                ? "श्री $patientName जी, टोकन #$tokenNumber का नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
                : "टोकन #$tokenNumber का नंबर आ गया है। कृपया गुरुजी के समीप पधारें।";
        } else {
            $body = "आश्रम से नया संदेश प्राप्त हुआ है।";
        }
    }

    $cleanPhone = preg_replace('/[^0-9]/', '', $phoneNumber);
    if (strlen($cleanPhone) > 10) {
        $cleanPhone = substr($cleanPhone, -10);
    }

    // 1. Save notification record in database
    $now = round(microtime(true) * 1000);
    $stmt = $pdo->prepare("INSERT INTO devotee_notifications (phone_number, token_number, title, message, type, is_read, created_at)
        VALUES (:phone, :tok, :title, :msg, :type, 0, :now)");
    $stmt->execute([
        ':phone' => $cleanPhone,
        ':tok' => $tokenNumber,
        ':title' => $title,
        ':msg' => $body,
        ':type' => $type,
        ':now' => $now
    ]);

    // 2. Query FCM tokens for this phone
    $tokens = [];
    if (!empty($cleanPhone)) {
        $tokStmt = $pdo->prepare("SELECT fcm_token FROM fcm_device_tokens WHERE phone_number = :phone");
        $tokStmt->execute([':phone' => $cleanPhone]);
        $tokens = $tokStmt->fetchAll(PDO::FETCH_COLUMN);
    }

    $fcmResult = ["sent" => 0, "failed" => 0];
    if (!empty($tokens)) {
        $fcmResult = sendFcmToTokens($tokens, $title, $body, [
            "type" => $type,
            "token_number" => strval($tokenNumber),
            "patient_name" => $patientName,
            "timestamp" => strval($now)
        ]);
    }

    echo json_encode([
        "success" => true,
        "phone_number" => $cleanPhone,
        "token_number" => $tokenNumber,
        "registered_devices_found" => count($tokens),
        "fcm_dispatch" => $fcmResult,
        "message" => "Devotee notification recorded and push triggered."
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => $e->getMessage()]);
}
