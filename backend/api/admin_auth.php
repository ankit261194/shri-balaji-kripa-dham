<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - केंद्रीय एडमिन प्रमाणीकरण व टोकन सेवा
// Central Admin Authentication, JWT Session Token & RBAC Service
// ==============================================================================

if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} elseif (file_exists(__DIR__ . '/config/db.php')) {
    require_once __DIR__ . '/config/db.php';
}

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-SBKD-API-KEY, X-SBKD-ADMIN-TOKEN, x-sbkd-api-key, x-sbkd-admin-token");
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
header("Pragma: no-cache");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$pdo = getDB();
if (!$pdo) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => "Database connection unavailable"], JSON_UNESCAPED_UNICODE);
    exit;
}

// Auto-create admin_sessions table if not exists
try {
    $pdo->exec("CREATE TABLE IF NOT EXISTS admin_sessions (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        session_token VARCHAR(255) NOT NULL UNIQUE,
        admin_id VARCHAR(50) NOT NULL,
        admin_name VARCHAR(100) NOT NULL,
        admin_role VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
        device_id VARCHAR(120) NOT NULL DEFAULT '',
        device_model VARCHAR(100) NOT NULL DEFAULT '',
        ip_address VARCHAR(50) NOT NULL DEFAULT '',
        created_at BIGINT NOT NULL,
        expires_at BIGINT NOT NULL,
        is_active TINYINT NOT NULL DEFAULT 1,
        INDEX idx_token (session_token),
        INDEX idx_active_admin (admin_id, is_active)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

    // Also auto-create admins table on server if not present
    $pdo->exec("CREATE TABLE IF NOT EXISTS admins (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(100) NOT NULL UNIQUE,
        password_hash VARCHAR(255) NOT NULL,
        name VARCHAR(100) NOT NULL,
        phone_number VARCHAR(30) NOT NULL DEFAULT '',
        role VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
        pin VARCHAR(10) NOT NULL DEFAULT '',
        is_active TINYINT NOT NULL DEFAULT 1,
        created_at BIGINT NOT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

    // Seed master Super Admin if empty
    $chkAdmin = $pdo->query("SELECT COUNT(*) FROM admins WHERE role = 'SUPER_ADMIN'")->fetchColumn();
    if ($chkAdmin == 0) {
        $now = time();
        $ins = $pdo->prepare("INSERT INTO admins (username, password_hash, name, phone_number, role, pin, is_active, created_at) 
                              VALUES ('admin', :ph, 'अंकित चौधरी', '9100100251', 'SUPER_ADMIN', '1234', 1, :now)");
        $ins->execute([
            ':ph' => password_hash('Aa@8006518960', PASSWORD_DEFAULT),
            ':now' => $now
        ]);
    }
} catch (Exception $e) {}

$raw = file_get_contents('php://input');
$input = json_decode($raw, true) ?: $_POST;
$action = strtoupper(trim($input['action'] ?? $_GET['action'] ?? 'VERIFY'));

$clientIp = $_SERVER['HTTP_CF_CONNECTING_IP'] ?? $_SERVER['HTTP_X_FORWARDED_FOR'] ?? $_SERVER['REMOTE_ADDR'] ?? '';
if (strpos($clientIp, ',') !== false) {
    $clientIp = trim(explode(',', $clientIp)[0]);
}

// -----------------------------------------------------------------------------
// 1. ADMIN LOGIN
// -----------------------------------------------------------------------------
if ($action === 'LOGIN') {
    $username = trim($input['username'] ?? '');
    $password = trim($input['password'] ?? '');
    $pin = trim($input['pin'] ?? '');
    $deviceId = trim($input['device_id'] ?? '');
    $deviceModel = trim($input['device_model'] ?? 'Android Device');

    if (empty($password) && empty($pin)) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "पासवर्ड अथवा पिन दर्ज करना अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $authenticatedAdmin = null;

    // Check Master Super Admin password
    if ($password === 'Aa@8006518960' || $password === 'Balaji@2026') {
        $authenticatedAdmin = [
            "id" => "1",
            "name" => "अंकित चौधरी",
            "phone_number" => "9100100251",
            "role" => "SUPER_ADMIN"
        ];
    } else {
        // Check in admins table by username/phone and password or PIN
        try {
            $stmt = $pdo->prepare("SELECT * FROM admins WHERE (username = :u OR phone_number = :p) AND is_active = 1 LIMIT 1");
            $stmt->execute([':u' => $username, ':p' => $username]);
            $adminRow = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($adminRow) {
                $passValid = false;
                if (!empty($password) && password_verify($password, $adminRow['password_hash'])) {
                    $passValid = true;
                } elseif (!empty($password) && $password === $adminRow['password_hash']) {
                    $passValid = true; // Plaintext fallback
                } elseif (!empty($pin) && $pin === $adminRow['pin']) {
                    $passValid = true;
                }

                if ($passValid) {
                    $authenticatedAdmin = [
                        "id" => (string)$adminRow['id'],
                        "name" => $adminRow['name'],
                        "phone_number" => $adminRow['phone_number'],
                        "role" => $adminRow['role']
                    ];
                }
            }
        } catch (Exception $e) {}
    }

    if (!$authenticatedAdmin) {
        http_response_code(401);
        echo json_encode(["success" => false, "error" => "गलत क्रेडेंशियल्स! यूजरनेम व पासवर्ड की जांच करें।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // Generate signed HMAC session token
    $now = time();
    $expiresAt = $now + (30 * 24 * 3600); // 30 Days
    $randomNonce = bin2hex(random_bytes(16));
    $payloadData = $authenticatedAdmin['id'] . '|' . $authenticatedAdmin['role'] . '|' . $now . '|' . $randomNonce;
    $signature = hash_hmac('sha256', $payloadData, defined('SBKD_API_SECRET') ? SBKD_API_SECRET : 'SECRET_SALT_2026');
    $sessionToken = 'sbkd_tok_' . base64_encode($payloadData . '|' . $signature);

    // Save session in MySQL
    try {
        // Deactivate previous active sessions for this device if needed
        $pdo->prepare("UPDATE admin_sessions SET is_active = 0 WHERE admin_id = :aid AND device_id = :did")
            ->execute([':aid' => $authenticatedAdmin['id'], ':did' => $deviceId]);

        $insStmt = $pdo->prepare("INSERT INTO admin_sessions 
            (session_token, admin_id, admin_name, admin_role, device_id, device_model, ip_address, created_at, expires_at, is_active)
            VALUES (:st, :aid, :aname, :arole, :did, :dmodel, :ip, :cat, :eat, 1)");
        $insStmt->execute([
            ':st' => $sessionToken,
            ':aid' => $authenticatedAdmin['id'],
            ':aname' => $authenticatedAdmin['name'],
            ':arole' => $authenticatedAdmin['role'],
            ':did' => $deviceId,
            ':dmodel' => $deviceModel,
            ':ip' => $clientIp,
            ':cat' => $now,
            ':eat' => $expiresAt
        ]);
    } catch (Exception $e) {}

    echo json_encode([
        "success" => true,
        "message" => "सफलतापूर्वक लॉगिन संपन्न!",
        "token" => $sessionToken,
        "admin" => [
            "id" => $authenticatedAdmin['id'],
            "name" => $authenticatedAdmin['name'],
            "phone_number" => $authenticatedAdmin['phone_number'],
            "role" => $authenticatedAdmin['role']
        ],
        "expires_at" => $expiresAt
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// -----------------------------------------------------------------------------
// 2. VERIFY SESSION TOKEN
// -----------------------------------------------------------------------------
if ($action === 'VERIFY') {
    $token = trim($input['token'] ?? $_GET['token'] ?? $_SERVER['HTTP_X_SBKD_ADMIN_TOKEN'] ?? '');
    if (empty($token)) {
        http_response_code(400);
        echo json_encode(["success" => false, "is_valid" => false, "error" => "टोकन आवश्यक है।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $valid = verifyAdminSessionToken($token, $pdo);
    if ($valid) {
        echo json_encode([
            "success" => true,
            "is_valid" => true,
            "admin" => $valid
        ], JSON_UNESCAPED_UNICODE);
    } else {
        http_response_code(401);
        echo json_encode([
            "success" => false,
            "is_valid" => false,
            "error" => "अमान्य अथवा समाप्त हो चुका एडमिन सत्र।"
        ], JSON_UNESCAPED_UNICODE);
    }
    exit;
}

// -----------------------------------------------------------------------------
// 3. LOGOUT / INVALIDATE SESSION
// -----------------------------------------------------------------------------
if ($action === 'LOGOUT') {
    $token = trim($input['token'] ?? $_GET['token'] ?? $_SERVER['HTTP_X_SBKD_ADMIN_TOKEN'] ?? '');
    if (!empty($token)) {
        try {
            $pdo->prepare("UPDATE admin_sessions SET is_active = 0 WHERE session_token = :st")->execute([':st' => $token]);
        } catch (Exception $e) {}
    }
    echo json_encode(["success" => true, "message" => "सत्र समाप्त कर दिया गया।"], JSON_UNESCAPED_UNICODE);
    exit;
}

http_response_code(400);
echo json_encode(["success" => false, "error" => "अमान्य क्रिया (Invalid Action)"], JSON_UNESCAPED_UNICODE);
