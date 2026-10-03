<?php
// ==============================================================================
// ANKIT ELEVEX ULTIMATE - HIGH SECURITY USER & SUPERADMIN LOGIN API
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
$mpin = trim($input['mpin'] ?? $_POST['mpin'] ?? '');

if (strlen($mobile) < 10) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "कृपया 10-अंकीय मान्य मोबाइल नंबर दर्ज करें।"], JSON_UNESCAPED_UNICODE);
    exit;
}

if (strlen($mpin) < 4) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "कृपया 4-अंकीय MPIN दर्ज करें।"], JSON_UNESCAPED_UNICODE);
    exit;
}

$db = getDB();

// 1. Ensure Table Exists
if ($db) {
    try {
        $db->exec("
            CREATE TABLE IF NOT EXISTS elevex_users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                mobile VARCHAR(20) UNIQUE NOT NULL,
                mpin_hash VARCHAR(128) NOT NULL,
                role VARCHAR(50) NOT NULL DEFAULT 'technician',
                full_name VARCHAR(100) NOT NULL,
                state VARCHAR(100) DEFAULT '',
                city VARCHAR(100) DEFAULT '',
                auth_token VARCHAR(255) DEFAULT '',
                profile_json TEXT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_mobile (mobile),
                INDEX idx_role (role)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        ");
    } catch (Exception $e) {}
}

// 2. SuperAdmin Master Account Check (Ankit Chaudhary)
if ($mobile === '8533955333' || $mobile === '918533955333') {
    // Master PINs: 8533, 1234, 910010025123343
    if ($mpin === '8533' || $mpin === '1234' || $mpin === '910010025123343' || hash('sha256', $mpin) === hash('sha256', '8533')) {
        $authToken = 'SUPER_ADMIN_TOKEN_' . bin2hex(random_bytes(16));
        echo json_encode([
            "status" => "success",
            "message" => "स्वागत है, सुपर एडमिन अंकित चौधरी जी!",
            "user" => [
                "id" => 1,
                "role" => "super_admin",
                "mobile" => "8533955333",
                "full_name" => "अंकित चौधरी (सुपर एडमिन)",
                "state" => "Uttar Pradesh",
                "city" => "Meerut",
                "auth_token" => $authToken,
                "profile" => [
                    "master_access" => true,
                    "simulators_enabled" => true,
                    "can_switch_all_roles" => true
                ]
            ]
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        exit;
    }
}

// 3. Database Check
if ($db) {
    try {
        $stmt = $db->prepare("SELECT * FROM elevex_users WHERE mobile = :mob LIMIT 1");
        $stmt->execute([':mob' => $mobile]);
        $user = $stmt->fetch();

        if ($user) {
            $hashed = hash('sha256', $mpin);
            if ($user['mpin_hash'] === $hashed || $user['mpin_hash'] === $mpin) {
                $authToken = 'ONLINE_TOKEN_' . bin2hex(random_bytes(16));
                $update = $db->prepare("UPDATE elevex_users SET auth_token = :t WHERE id = :id");
                $update->execute([':t' => $authToken, ':id' => $user['id']]);

                $profile = !empty($user['profile_json']) ? json_decode($user['profile_json'], true) : [];
                echo json_encode([
                    "status" => "success",
                    "message" => "लॉगिन सफल रहा!",
                    "user" => [
                        "id" => intval($user['id']),
                        "role" => $user['role'] ?? 'technician',
                        "mobile" => $user['mobile'],
                        "full_name" => $user['full_name'],
                        "state" => $user['state'] ?? '',
                        "city" => $user['city'] ?? '',
                        "auth_token" => $authToken,
                        "profile" => $profile ?: new stdClass()
                    ]
                ], JSON_UNESCAPED_UNICODE);
                exit;
            } else {
                http_response_code(401);
                echo json_encode(["status" => "error", "message" => "गलत MPIN! कृपया सही 4-अंकीय पिन दर्ज करें।"], JSON_UNESCAPED_UNICODE);
                exit;
            }
        }
    } catch (Exception $e) {}
}

// 4. Default Authenticated Response for Authorized Field Technicians
// (Allows instant onboarding if DB table is syncing)
http_response_code(404);
echo json_encode([
    "status" => "error",
    "message" => "यह मोबाइल नंबर पंजीकृत नहीं है। कृपया 'नया खाता बनाएं' पर क्लिक करके पंजीकरण करें।"
], JSON_UNESCAPED_UNICODE);
exit;
