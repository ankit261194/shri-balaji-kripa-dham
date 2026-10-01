<?php
/**
 * OneSignal Push Notification Service
 * Shri Balaji Kripa Dham
 *
 * Handles instant cloud push notifications for:
 * 1. Broadcast announcements to all devotees
 * 2. Real-time token call alerts to specific devotee phone numbers
 */

if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} else {
    require_once __DIR__ . '/config/db.php';
}

header('Content-Type: application/json; charset=utf-8');

define('ONESIGNAL_APP_ID', 'db065153-88a1-4a01-badc-ae33c4b38cbe');
define('ONESIGNAL_REST_KEY', getenv('ONESIGNAL_REST_KEY') ?: base64_decode('b3NfdjJfYXBwXzNtZGZjdTRpdWZmYWRvdzR2eXo0am00bXh5bzRqb2xiZnhzZXB1ZmRjdm11NGR0aGN2ZHhqcnBsYXgzcnVndm14aXJvZnZrNWF4ZXNqc3IyeGpqbzRnamtia253cGhhb2p0enB4Nmk='));

/**
 * Dispatch HTTP POST request to OneSignal REST API v1
 */
function sendOneSignalRequest($payload) {
    $url = "https://onesignal.com/api/v1/notifications";
    $json = json_encode($payload, JSON_UNESCAPED_UNICODE);

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: Basic ' . ONESIGNAL_REST_KEY,
        'Content-Type: application/json; charset=utf-8',
        'Content-Length: ' . strlen($json)
    ]);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $json);
    curl_setopt($ch, CURLOPT_TIMEOUT, 12);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $err = curl_error($ch);
    curl_close($ch);

    if ($err) {
        return [
            'success' => false,
            'http_code' => $httpCode,
            'error' => $err
        ];
    }

    $decoded = json_decode($response, true);
    return [
        'success' => ($httpCode >= 200 && $httpCode < 300),
        'http_code' => $httpCode,
        'response' => $decoded ?: $response
    ];
}

/**
 * Broadcast announcement to ALL subscribed devotees
 */
function sendOneSignalBroadcast($title, $message, $data = []) {
    $cleanTitle = trim($title);
    $cleanMsg = trim($message);

    if (empty($cleanTitle) || empty($cleanMsg)) {
        return ['success' => false, 'error' => 'Title and message cannot be empty'];
    }

    $mergedData = array_merge([
        'type' => 'BROADCAST',
        'timestamp' => round(microtime(true) * 1000)
    ], $data);

    $payload = [
        'app_id' => ONESIGNAL_APP_ID,
        'included_segments' => ['Total Subscriptions'],
        'headings' => [
            'en' => $cleanTitle,
            'hi' => $cleanTitle
        ],
        'contents' => [
            'en' => $cleanMsg,
            'hi' => $cleanMsg
        ],
        'data' => $mergedData
    ];

    return sendOneSignalRequest($payload);
}

/**
 * Send targeted Token Call alert to a devotee by phone number
 */
function sendOneSignalTokenAlert($phoneNumber, $tokenNumber, $patientName = '') {
    $cleanPhone = preg_replace('/[^0-9]/', '', $phoneNumber);
    if (strlen($cleanPhone) > 10) {
        $cleanPhone = substr($cleanPhone, -10);
    }

    if (empty($cleanPhone) || intval($tokenNumber) <= 0) {
        return ['success' => false, 'error' => 'Invalid phone number or token number'];
    }

    $title = "🔔 टोकन बुलावा: टोकन #$tokenNumber";
    $body = !empty($patientName)
        ? "श्री {$patientName} जी, टोकन #$tokenNumber का नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
        : "टोकन #$tokenNumber का नंबर आ गया है। कृपया गुरुजी के समीप पधारें।";

    $customData = [
        'type' => 'TOKEN_CALL',
        'token_number' => strval($tokenNumber),
        'patient_name' => $patientName,
        'phone_number' => $cleanPhone,
        'status' => 'SERVING',
        'timestamp' => round(microtime(true) * 1000)
    ];

    // Target using both external_id aliases and tag filters for high deliverability
    $payload = [
        'app_id' => ONESIGNAL_APP_ID,
        'include_aliases' => [
            'external_id' => [$cleanPhone]
        ],
        'target_channel' => 'push',
        'headings' => [
            'en' => $title,
            'hi' => $title
        ],
        'contents' => [
            'en' => $body,
            'hi' => $body
        ],
        'data' => $customData
    ];

    $res = sendOneSignalRequest($payload);

    // Fallback: If external_id alias had 0 recipients or error, also attempt tag-based delivery
    if (!$res['success'] || (isset($res['response']['recipients']) && $res['response']['recipients'] === 0)) {
        $tagPayload = [
            'app_id' => ONESIGNAL_APP_ID,
            'filters' => [
                ['field' => 'tag', 'key' => 'phone', 'relation' => '=', 'value' => $cleanPhone]
            ],
            'headings' => [
                'en' => $title,
                'hi' => $title
            ],
            'contents' => [
                'en' => $body,
                'hi' => $body
            ],
            'data' => $customData
        ];
        $tagRes = sendOneSignalRequest($tagPayload);
        if ($tagRes['success']) {
            return $tagRes;
        }
    }

    return $res;
}

// -------------------------------------------------------------
// Direct API Endpoint Execution Handler
// -------------------------------------------------------------
if (basename($_SERVER['SCRIPT_FILENAME'] ?? '') === 'onesignal_service.php') {
    if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
        http_response_code(200);
        exit;
    }

    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        http_response_code(405);
        echo json_encode(["success" => false, "error" => "Method Not Allowed"]);
        exit;
    }

    // Verify authorized admin or app caller
    verifyApiAuth();

    $input = json_decode(file_get_contents('php://input'), true) ?: $_POST;
    $action = strtolower(trim($input['action'] ?? 'broadcast'));

    if ($action === 'broadcast') {
        $title = trim($input['title'] ?? '');
        $message = trim($input['message'] ?? '');
        $priority = trim($input['priority'] ?? 'HIGH');

        if (empty($title) || empty($message)) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "शीर्षक (title) और संदेश (message) अनिवार्य हैं।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $result = sendOneSignalBroadcast($title, $message, ['priority' => $priority]);
        echo json_encode($result, JSON_UNESCAPED_UNICODE);
        exit;
    }

    if ($action === 'token_alert') {
        $phone = trim($input['phone'] ?? $input['phone_number'] ?? '');
        $tokenNumber = intval($input['token_number'] ?? 0);
        $patientName = trim($input['patient_name'] ?? '');

        if (empty($phone) || $tokenNumber <= 0) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "फ़ोन नंबर और टोकन नंबर अनिवार्य हैं।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $result = sendOneSignalTokenAlert($phone, $tokenNumber, $patientName);
        echo json_encode($result, JSON_UNESCAPED_UNICODE);
        exit;
    }

    http_response_code(400);
    echo json_encode(["success" => false, "error" => "अमान्य क्रिया (Invalid action)"], JSON_UNESCAPED_UNICODE);
    exit;
}
