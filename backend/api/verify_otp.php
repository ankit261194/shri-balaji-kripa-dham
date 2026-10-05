<?php
// ==============================================================================
// ANKIT ELEVEX ULTIMATE - OTP VERIFICATION & SINGLE-DEVICE BINDING ENGINE
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
$deviceId = trim($input['device_id'] ?? $_POST['device_id'] ?? 'GENERIC_DEVICE');
$deviceName = trim($input['device_name'] ?? $_POST['device_name'] ?? 'Android Terminal');

$mobile10 = substr($mobile, -10);

$db = getDB();

if ($db) {
    try {
        $db->exec("
            CREATE TABLE IF NOT EXISTS elevex_devices (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_mobile VARCHAR(20) NOT NULL,
                device_id VARCHAR(100) NOT NULL,
                device_name VARCHAR(100) DEFAULT 'Android Terminal',
                session_token VARCHAR(64) NOT NULL,
                is_active TINYINT DEFAULT 1,
                last_active DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_mob_dev (user_mobile, device_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

            CREATE TABLE IF NOT EXISTS elevex_subscriptions (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_mobile VARCHAR(20) NOT NULL,
                plan_name VARCHAR(50) DEFAULT '30_days_pro',
                amount INT DEFAULT 199,
                utr_number VARCHAR(64) DEFAULT NULL,
                start_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                expires_at DATETIME NOT NULL,
                status VARCHAR(20) DEFAULT 'active',
                INDEX idx_sub_mob (user_mobile)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        ");
    } catch (Exception $e) {}
}

// Master bypass for testing / emergency access
$isValid = ($otp === '853395' || $otp === '123456' || $otp === '910010');

if (!$isValid && $db) {
    try {
        $stmt = $db->prepare("SELECT id FROM elevex_otps WHERE mobile = :m AND otp = :o AND is_used = 0 AND created_at >= NOW() - INTERVAL 10 MINUTE ORDER BY id DESC LIMIT 1");
        $stmt->execute([':m' => $mobile10, ':o' => $otp]);
        $row = $stmt->fetch();
        if ($row) {
            $isValid = true;
            // Mark OTP used
            $upd = $db->prepare("UPDATE elevex_otps SET is_used = 1 WHERE id = :id");
            $upd->execute([':id' => $row['id']]);
        }
    } catch (Exception $e) {}
}

if (!$isValid) {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "अमान्य या समय-समाप्त OTP! कृपया पुनः प्रयास करें।",
        "remaining_attempts" => 3
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// 1. Single Active Device Enforcement (Deactivate any previously active device)
$deviceMigrated = false;
$newSessionToken = 'SES_' . bin2hex(random_bytes(16));

if ($db) {
    try {
        // Check if there was another active device for this user
        $checkOld = $db->prepare("SELECT id, device_name FROM elevex_devices WHERE user_mobile = :mob AND device_id != :did AND is_active = 1");
        $checkOld->execute([':mob' => $mobile10, ':did' => $deviceId]);
        $oldDevice = $checkOld->fetch();
        if ($oldDevice) {
            $deviceMigrated = true;
            // Revoke old device sessions
            $deact = $db->prepare("UPDATE elevex_devices SET is_active = 0 WHERE user_mobile = :mob AND device_id != :did");
            $deact->execute([':mob' => $mobile10, ':did' => $deviceId]);
        }

        // Register or activate current device
        $regDev = $db->prepare("
            INSERT INTO elevex_devices (user_mobile, device_id, device_name, session_token, is_active)
            VALUES (:mob, :did, :dname, :tok, 1)
            ON DUPLICATE KEY UPDATE session_token = :tok, is_active = 1, last_active = NOW()
        ");
        $regDev->execute([
            ':mob' => $mobile10,
            ':did' => $deviceId,
            ':dname' => $deviceName,
            ':tok' => $newSessionToken
        ]);
    } catch (Exception $e) {}
}

// 2. Fetch or initialize Subscription
$subscription = [
    "is_active" => true,
    "plan_name" => "30_DAYS_PRO",
    "days_remaining" => 30,
    "expires_at" => date('Y-m-d H:i:s', strtotime('+30 days')),
    "offline_lease_key" => hash('sha256', $mobile10 . ':' . $deviceId . ':' . date('Y-m-d', strtotime('+30 days')) . ':ELEVEX_PRO_SECRET')
];

if ($db) {
    try {
        $subStmt = $db->prepare("SELECT * FROM elevex_subscriptions WHERE user_mobile = :mob AND expires_at > NOW() AND status = 'active' ORDER BY id DESC LIMIT 1");
        $subStmt->execute([':mob' => $mobile10]);
        $subRow = $subStmt->fetch();
        if ($subRow) {
            $diffDays = max(1, (int) ceil((strtotime($subRow['expires_at']) - time()) / 86400));
            $subscription = [
                "is_active" => true,
                "plan_name" => $subRow['plan_name'],
                "days_remaining" => $diffDays,
                "expires_at" => $subRow['expires_at'],
                "offline_lease_key" => hash('sha256', $mobile10 . ':' . $deviceId . ':' . substr($subRow['expires_at'], 0, 10) . ':ELEVEX_PRO_SECRET')
            ];
        } else {
            // Give 7-day initial pro trial on first verification
            $exp = date('Y-m-d H:i:s', strtotime('+7 days'));
            $insSub = $db->prepare("INSERT INTO elevex_subscriptions (user_mobile, plan_name, amount, expires_at, status) VALUES (:m, '7_DAYS_TRIAL', 0, :exp, 'active')");
            $insSub->execute([':m' => $mobile10, ':exp' => $exp]);
            $subscription = [
                "is_active" => true,
                "plan_name" => "7_DAYS_TRIAL",
                "days_remaining" => 7,
                "expires_at" => $exp,
                "offline_lease_key" => hash('sha256', $mobile10 . ':' . $deviceId . ':' . substr($exp, 0, 10) . ':ELEVEX_PRO_SECRET')
            ];
        }
    } catch (Exception $e) {}
}

// 3. User Record
$user = null;
if ($db) {
    try {
        $stmt = $db->prepare("SELECT * FROM elevex_users WHERE mobile = :mob LIMIT 1");
        $stmt->execute([':mob' => $mobile10]);
        $user = $stmt->fetch();
    } catch (Exception $e) {}
}

echo json_encode([
    "status" => "success",
    "is_registered_user" => ($user !== false && $user !== null),
    "message" => "मोबाइल OTP सत्यापन व सिंगल-डिवाइस बाइंडिंग सफल!",
    "session_token" => $newSessionToken,
    "device_migrated" => $deviceMigrated,
    "device_id" => $deviceId,
    "subscription" => $subscription,
    "user" => $user ? [
        "id" => intval($user['id']),
        "role" => $user['role'],
        "mobile" => $user['mobile'],
        "full_name" => $user['full_name'],
        "state" => $user['state'],
        "city" => $user['city']
    ] : null
], JSON_UNESCAPED_UNICODE);
exit;
