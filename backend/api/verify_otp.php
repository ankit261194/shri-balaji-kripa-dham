<?php
// ==============================================================================
// ANKIT ELEVEX ULTIMATE - OTP VERIFICATION API
// Conceived & Engineered by Ankit Chaudhary (8533955333)
// ==============================================================================

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-EleveX-Auth");
header("Cache-Control: no-cache, no-store, must-revalidate");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../config/db.php';

$raw = file_get_contents('php://input');
$input = !empty($raw) ? json_decode($raw, true) : [];
$mobile = preg_replace('/[^0-9]/', '', $input['mobile'] ?? $_POST['mobile'] ?? '');
$otp = trim($input['otp'] ?? $_POST['otp'] ?? '');

$db = getDB();

// Master OTP bypass
$isValid = ($otp === '853395' || $otp === '123456');

if (!$isValid && $db) {
    try {
        $stmt = $db->prepare("SELECT id FROM elevex_otps WHERE mobile = :m AND otp = :o ORDER BY id DESC LIMIT 1");
        $stmt->execute([':m' => $mobile, ':o' => $otp]);
        if ($stmt->fetch()) {
            $isValid = true;
        }
    } catch (Exception $e) {}
}

if (!$isValid) {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "अमान्य OTP कोड! कृपया पुनः प्रयास करें।",
        "remaining_attempts" => 2
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// Check if already registered
$user = null;
if ($db) {
    try {
        $stmt = $db->prepare("SELECT * FROM elevex_users WHERE mobile = :mob LIMIT 1");
        $stmt->execute([':mob' => $mobile]);
        $user = $stmt->fetch();
    } catch (Exception $e) {}
}

if ($user) {
    echo json_encode([
        "status" => "success",
        "is_registered_user" => true,
        "message" => "सत्यापन व लॉगिन सफल!",
        "user" => [
            "id" => intval($user['id']),
            "role" => $user['role'],
            "mobile" => $user['mobile'],
            "full_name" => $user['full_name'],
            "state" => $user['state'],
            "city" => $user['city'],
            "auth_token" => $user['auth_token'],
            "profile" => !empty($user['profile_json']) ? json_decode($user['profile_json'], true) : new stdClass()
        ]
    ], JSON_UNESCAPED_UNICODE);
} else {
    echo json_encode([
        "status" => "success",
        "is_registered_user" => false,
        "verification_token" => 'VERIFIED_' . bin2hex(random_bytes(16)),
        "message" => "मोबाइल नंबर सत्यापित हो गया है! कृपया प्रोफ़ाइल पूरी करें।"
    ], JSON_UNESCAPED_UNICODE);
}
exit;
