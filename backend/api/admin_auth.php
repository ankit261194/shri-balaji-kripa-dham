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

    // Migration: Add raw_password and raw_pin columns to admins table for password recovery
    try {
        $pdo->exec("ALTER TABLE admins ADD COLUMN raw_password VARCHAR(255) NOT NULL DEFAULT ''");
    } catch (Exception $e) {}
    try {
        $pdo->exec("ALTER TABLE admins ADD COLUMN raw_pin VARCHAR(20) NOT NULL DEFAULT ''");
    } catch (Exception $e) {}
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

// -----------------------------------------------------------------------------
// 4. CHANGE PASSWORD OR PIN (Admin Self-Service or Super Admin Reset)
// -----------------------------------------------------------------------------
if ($action === 'CHANGE_PASSWORD' || $action === 'RESET_PASSWORD') {
    $adminId = trim($input['admin_id'] ?? $input['id'] ?? '');
    $username = trim($input['username'] ?? '');
    $phone = trim($input['phone'] ?? $input['phone_number'] ?? '');
    $newPassword = trim($input['new_password'] ?? $input['password'] ?? '');
    $newPin = trim($input['new_pin'] ?? $input['pin'] ?? '');

    if (empty($adminId) && empty($username) && empty($phone)) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "व्यवस्थापक आईडी, यूजरनेम या मोबाइल नंबर अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    if (empty($newPassword) && empty($newPin)) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "नया पासवर्ड अथवा 4-अंकीय पिन दर्ज करें।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // Find the admin record
    $findSql = "SELECT * FROM admins WHERE 1=1";
    $params = [];
    if (!empty($adminId)) {
        $findSql .= " AND id = :aid";
        $params[':aid'] = $adminId;
    } elseif (!empty($username)) {
        $findSql .= " AND username = :un";
        $params[':un'] = $username;
    } else {
        $findSql .= " AND phone_number = :ph";
        $params[':ph'] = $phone;
    }
    $findSql .= " LIMIT 1";

    $stmt = $pdo->prepare($findSql);
    $stmt->execute($params);
    $admin = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$admin) {
        http_response_code(404);
        echo json_encode(["success" => false, "error" => "व्यवस्थापक खाता नहीं मिला।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $updates = [];
    $updateParams = [':id' => $admin['id']];

    if (!empty($newPassword)) {
        $updates[] = "password_hash = :ph";
        $updates[] = "raw_password = :rp";
        $updateParams[':ph'] = password_hash($newPassword, PASSWORD_DEFAULT);
        $updateParams[':rp'] = $newPassword;
    }

    if (!empty($newPin)) {
        $updates[] = "pin = :pn";
        $updates[] = "raw_pin = :rpn";
        $updateParams[':pn'] = $newPin;
        $updateParams[':rpn'] = $newPin;
    }

    if (!empty($updates)) {
        $updateSql = "UPDATE admins SET " . implode(", ", $updates) . " WHERE id = :id";
        $upStmt = $pdo->prepare($updateSql);
        $upStmt->execute($updateParams);
    }

    echo json_encode([
        "success" => true,
        "message" => "पासवर्ड व पिन सफलतापूर्वक बदल दिया गया!",
        "admin_id" => $admin['id'],
        "username" => $admin['username'],
        "updated_pin" => !empty($newPin) ? $newPin : ($admin['raw_pin'] ?: $admin['pin']),
        "updated_password" => !empty($newPassword) ? $newPassword : ($admin['raw_password'] ?: '********')
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// -----------------------------------------------------------------------------
// 5. LIST ALL ADMINS (With Visible PIN & Password for Super Admin Management)
// -----------------------------------------------------------------------------
if ($action === 'LIST_ADMINS') {
    $stmt = $pdo->query("SELECT id, username, name, phone_number, role, pin, raw_pin, raw_password, is_active, created_at FROM admins ORDER BY id ASC");
    $admins = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Ensure display_pin and display_password are populated
    foreach ($admins as &$a) {
        $a['display_pin'] = !empty($a['raw_pin']) ? $a['raw_pin'] : (!empty($a['pin']) ? $a['pin'] : '1234');
        $a['display_password'] = !empty($a['raw_password']) ? $a['raw_password'] : '';
    }

    echo json_encode([
        "success" => true,
        "total" => count($admins),
        "admins" => $admins
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// -----------------------------------------------------------------------------
// 6. SAVE OR UPDATE ADMIN (From Super Admin Panel)
// -----------------------------------------------------------------------------
if ($action === 'SAVE_ADMIN') {
    $id = intval($input['id'] ?? 0);
    $name = trim($input['name'] ?? '');
    $username = trim($input['username'] ?? '');
    $phone = trim($input['phone'] ?? $input['phone_number'] ?? '');
    $role = trim($input['role'] ?? 'SEVADAR');
    $password = trim($input['password'] ?? '');
    $pin = trim($input['pin'] ?? '');
    $isActive = isset($input['is_active']) ? intval($input['is_active']) : 1;

    if (empty($name) || empty($username) || empty($phone)) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "नाम, यूजरनेम और फोन नंबर अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    if ($id > 0) {
        // Update existing admin
        $sql = "UPDATE admins SET name = :n, username = :u, phone_number = :p, role = :r, is_active = :ia";
        $params = [
            ':n' => $name,
            ':u' => $username,
            ':p' => $phone,
            ':r' => $role,
            ':ia' => $isActive,
            ':id' => $id
        ];
        if (!empty($password)) {
            $sql .= ", password_hash = :ph, raw_password = :rp";
            $params[':ph'] = password_hash($password, PASSWORD_DEFAULT);
            $params[':rp'] = $password;
        }
        if (!empty($pin)) {
            $sql .= ", pin = :pn, raw_pin = :rpn";
            $params[':pn'] = $pin;
            $params[':rpn'] = $pin;
        }
        $sql .= " WHERE id = :id";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
    } else {
        // Insert new admin
        $passHash = !empty($password) ? password_hash($password, PASSWORD_DEFAULT) : password_hash('123456', PASSWORD_DEFAULT);
        $rawPass = !empty($password) ? $password : '123456';
        $finalPin = !empty($pin) ? $pin : '1234';

        $stmt = $pdo->prepare("INSERT INTO admins 
            (name, username, phone_number, role, password_hash, raw_password, pin, raw_pin, is_active, created_at)
            VALUES (:n, :u, :p, :r, :ph, :rp, :pn, :rpn, :ia, :cat)
            ON DUPLICATE KEY UPDATE 
            name = VALUES(name), phone_number = VALUES(phone_number), role = VALUES(role), 
            password_hash = VALUES(password_hash), raw_password = VALUES(raw_password),
            pin = VALUES(pin), raw_pin = VALUES(raw_pin), is_active = VALUES(is_active)");
        
        $stmt->execute([
            ':n' => $name,
            ':u' => $username,
            ':p' => $phone,
            ':r' => $role,
            ':ph' => $passHash,
            ':rp' => $rawPass,
            ':pn' => $finalPin,
            ':rpn' => $finalPin,
            ':ia' => $isActive,
            ':cat' => time()
        ]);
        $id = $pdo->lastInsertId();
    }

    echo json_encode([
        "success" => true,
        "message" => "व्यवस्थापक खाता सफलतापूर्वक सुरक्षित हुआ!",
        "id" => $id
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

http_response_code(400);
echo json_encode(["success" => false, "error" => "अमान्य क्रिया (Invalid Action)"], JSON_UNESCAPED_UNICODE);
