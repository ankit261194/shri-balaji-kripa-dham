<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - केंद्रीय समस्या, सुझाव व प्रश्न निवारण सेवा
// Central Helpdesk, Query & Feedback Management System (Devotee & Admin -> Super Admin)
// ==============================================================================

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
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-SBKD-ADMIN-TOKEN");
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

// Auto-create app_queries table
try {
    $pdo->exec("CREATE TABLE IF NOT EXISTS app_queries (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        sender_name VARCHAR(150) NOT NULL,
        sender_phone VARCHAR(30) NOT NULL,
        sender_city VARCHAR(100) DEFAULT '',
        sender_role VARCHAR(50) NOT NULL DEFAULT 'DEVOTEE',
        category VARCHAR(100) NOT NULL DEFAULT 'OTHER',
        subject VARCHAR(200) NOT NULL DEFAULT '',
        message TEXT NOT NULL,
        attachment_url VARCHAR(500) DEFAULT '',
        device_id VARCHAR(150) DEFAULT '',
        status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
        admin_reply TEXT DEFAULT '',
        replied_by VARCHAR(100) DEFAULT '',
        replied_at BIGINT DEFAULT 0,
        created_at BIGINT NOT NULL,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_phone (sender_phone),
        INDEX idx_status (status),
        INDEX idx_role (sender_role),
        INDEX idx_created (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
} catch (Exception $e) {}

$raw = file_get_contents('php://input');
$input = json_decode($raw, true) ?: $_POST;
$action = strtoupper(trim($input['action'] ?? $_GET['action'] ?? 'SUBMIT'));

// -----------------------------------------------------------------------------
// 1. SUBMIT QUERY / FEEDBACK (Devotee or Admin)
// -----------------------------------------------------------------------------
if ($action === 'SUBMIT') {
    $name = trim($input['sender_name'] ?? $input['name'] ?? '');
    $phone = trim($input['sender_phone'] ?? $input['phone'] ?? '');
    $city = trim($input['sender_city'] ?? $input['city'] ?? '');
    $role = strtoupper(trim($input['sender_role'] ?? $input['role'] ?? 'DEVOTEE'));
    $category = trim($input['category'] ?? 'OTHER');
    $subject = trim($input['subject'] ?? '');
    $message = trim($input['message'] ?? '');
    $attachment = trim($input['attachment_url'] ?? '');
    $deviceId = trim($input['device_id'] ?? '');

    if (empty($name) || empty($phone) || empty($message)) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "नाम, मोबाइल नंबर और समस्या/सुझाव विवरण दर्ज करना अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $now = time();
    $stmt = $pdo->prepare("INSERT INTO app_queries 
        (sender_name, sender_phone, sender_city, sender_role, category, subject, message, attachment_url, device_id, status, created_at)
        VALUES (:n, :p, :c, :r, :cat, :sub, :msg, :att, :did, 'PENDING', :cat_time)");
    
    $stmt->execute([
        ':n' => $name,
        ':p' => $phone,
        ':c' => $city,
        ':r' => in_array($role, ['ADMIN', 'SUPER_ADMIN', 'SEVADAR']) ? 'ADMIN' : 'DEVOTEE',
        ':cat' => $category,
        ':sub' => $subject,
        ':msg' => $message,
        ':att' => $attachment,
        ':did' => $deviceId,
        ':cat_time' => $now
    ]);

    $insertId = $pdo->lastInsertId();

    echo json_encode([
        "success" => true,
        "message" => "आपकी समस्या/सुझाव सफलतापूर्वक सुपर एडमिन तक पहुँचा दिया गया है। जैसे ही सुपर एडमिन उत्तर देंगे, आपको सूचित कर दिया जाएगा।",
        "query_id" => (int)$insertId,
        "status" => "PENDING",
        "created_at" => $now
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// -----------------------------------------------------------------------------
// 2. GET ALL QUERIES (For Super Admin Dashboard)
// -----------------------------------------------------------------------------
if ($action === 'GET_ALL') {
    $limit = intval($_GET['limit'] ?? 200);
    $statusFilter = trim($_GET['status'] ?? '');
    $roleFilter = trim($_GET['role'] ?? '');

    $sql = "SELECT * FROM app_queries WHERE 1=1";
    $params = [];

    if (!empty($statusFilter)) {
        $sql .= " AND status = :st";
        $params[':st'] = $statusFilter;
    }
    if (!empty($roleFilter)) {
        $sql .= " AND sender_role = :ro";
        $params[':ro'] = $roleFilter;
    }

    $sql .= " ORDER BY id DESC LIMIT " . $limit;

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $rows = $stmt->fetchAll();

    echo json_encode([
        "success" => true,
        "total" => count($rows),
        "queries" => $rows
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// -----------------------------------------------------------------------------
// 3. GET MY QUERIES (For Devotee / Admin to see replies)
// -----------------------------------------------------------------------------
if ($action === 'GET_MY') {
    $phone = trim($input['phone'] ?? $_GET['phone'] ?? '');
    $deviceId = trim($input['device_id'] ?? $_GET['device_id'] ?? '');

    if (empty($phone) && empty($deviceId)) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "Phone number or Device ID required"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $sql = "SELECT * FROM app_queries WHERE ";
    $params = [];
    if (!empty($phone) && !empty($deviceId)) {
        $sql .= "(sender_phone = :p OR device_id = :did)";
        $params[':p'] = $phone;
        $params[':did'] = $deviceId;
    } elseif (!empty($phone)) {
        $sql .= "sender_phone = :p";
        $params[':p'] = $phone;
    } else {
        $sql .= "device_id = :did";
        $params[':did'] = $deviceId;
    }

    $sql .= " ORDER BY id DESC LIMIT 50";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $rows = $stmt->fetchAll();

    echo json_encode([
        "success" => true,
        "total" => count($rows),
        "queries" => $rows
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// -----------------------------------------------------------------------------
// 4. REPLY TO QUERY (Super Admin Action)
// -----------------------------------------------------------------------------
if ($action === 'REPLY') {
    $id = intval($input['id'] ?? 0);
    $reply = trim($input['reply'] ?? $input['admin_reply'] ?? '');
    $repliedBy = trim($input['replied_by'] ?? 'सुपर एडमिन (अंकित चौधरी)');
    $newStatus = trim($input['status'] ?? 'REPLIED');

    if ($id <= 0 || empty($reply)) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "आईडी व उत्तर संदेश अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $now = time();
    $stmt = $pdo->prepare("UPDATE app_queries SET 
        admin_reply = :rep,
        replied_by = :rb,
        replied_at = :ra,
        status = :st
        WHERE id = :id");
    
    $stmt->execute([
        ':rep' => $reply,
        ':rb' => $repliedBy,
        ':ra' => $now,
        ':st' => $newStatus,
        ':id' => $id
    ]);

    echo json_encode([
        "success" => true,
        "message" => "सुपर एडमिन का उत्तर सफलतापूर्वक दर्ज हो गया!",
        "id" => $id,
        "status" => $newStatus,
        "replied_at" => $now
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// -----------------------------------------------------------------------------
// 5. DELETE QUERY
// -----------------------------------------------------------------------------
if ($action === 'DELETE') {
    $id = intval($input['id'] ?? 0);
    if ($id <= 0) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "Valid ID required"]);
        exit;
    }

    $stmt = $pdo->prepare("DELETE FROM app_queries WHERE id = :id");
    $stmt->execute([':id' => $id]);

    echo json_encode(["success" => true, "message" => "समस्या/सुझाव हटा दिया गया।"], JSON_UNESCAPED_UNICODE);
    exit;
}

http_response_code(400);
echo json_encode(["success" => false, "error" => "अमान्य क्रिया (Invalid action)"], JSON_UNESCAPED_UNICODE);
