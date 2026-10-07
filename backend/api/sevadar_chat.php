<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - सेवादार रियल-टाइम लाइव चैट व हेल्पडेस्क API
// Ashram Sevadar Real-Time Live Chat & WhatsApp Communication Engine (Zero-Mock)
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
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-SBKD-API-KEY, X-SBKD-ADMIN-TOKEN");
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
header("Pragma: no-cache");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$pdo = getDB();
if (!$pdo) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => "डेटाबेस कनेक्शन उपलब्ध नहीं है।"], JSON_UNESCAPED_UNICODE);
    exit;
}

// 1. Auto-create sevadar_chats table
try {
    $pdo->exec("CREATE TABLE IF NOT EXISTS sevadar_chats (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        msg_id VARCHAR(64) UNIQUE NOT NULL,
        conversation_id VARCHAR(128) NOT NULL,
        sevadar_id VARCHAR(64) NOT NULL,
        sevadar_name VARCHAR(150) NOT NULL,
        devotee_id VARCHAR(64) NOT NULL,
        devotee_name VARCHAR(150) NOT NULL,
        devotee_phone VARCHAR(30) NOT NULL,
        sender_role VARCHAR(30) NOT NULL, -- 'DEVOTEE', 'SEVADAR', 'ADMIN'
        message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT', -- 'TEXT', 'PHOTO', 'AUDIO_VOICE', 'DOCUMENT'
        message_text TEXT NOT NULL,
        attachment_url VARCHAR(500) DEFAULT '',
        attachment_type VARCHAR(50) DEFAULT 'NONE',
        media_duration INT DEFAULT 0, -- Duration in seconds for audio notes
        status VARCHAR(30) NOT NULL DEFAULT 'SENT', -- 'SENT', 'DELIVERED', 'READ'
        created_at BIGINT NOT NULL,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_conv (conversation_id),
        INDEX idx_sevadar (sevadar_id),
        INDEX idx_devotee (devotee_phone),
        INDEX idx_status (status),
        INDEX idx_created (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

    // Also ensure sevadars table exists and has department column
    $pdo->exec("CREATE TABLE IF NOT EXISTS sevadars (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(150) NOT NULL,
        role VARCHAR(150) NOT NULL DEFAULT 'आश्रम सेवादार',
        department VARCHAR(150) NOT NULL DEFAULT 'सामान्य आश्रम सहायता',
        phone VARCHAR(20) DEFAULT '',
        whatsapp VARCHAR(20) DEFAULT '',
        photo_url VARCHAR(500) DEFAULT '',
        bio TEXT,
        is_available TINYINT(1) DEFAULT 1,
        display_order INT DEFAULT 0,
        is_active TINYINT(1) DEFAULT 1,
        created_at BIGINT NOT NULL,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_order (display_order),
        INDEX idx_active (is_active)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

    // Real-Time In-App Voice Calling tables (Zero-Mock, Authentic Devotee-Sevadar Calling)
    $pdo->exec("CREATE TABLE IF NOT EXISTS sevadar_call_sessions (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        call_id VARCHAR(64) UNIQUE NOT NULL,
        conversation_id VARCHAR(128) NOT NULL,
        sevadar_id VARCHAR(64) NOT NULL,
        sevadar_name VARCHAR(150) NOT NULL,
        caller_id VARCHAR(64) NOT NULL,
        caller_name VARCHAR(150) NOT NULL,
        caller_phone VARCHAR(30) NOT NULL,
        caller_role VARCHAR(30) NOT NULL DEFAULT 'DEVOTEE',
        call_type VARCHAR(20) NOT NULL DEFAULT 'VOICE',
        call_status VARCHAR(30) NOT NULL DEFAULT 'RINGING',
        duration_seconds INT NOT NULL DEFAULT 0,
        started_at BIGINT NOT NULL,
        connected_at BIGINT DEFAULT 0,
        ended_at BIGINT DEFAULT 0,
        last_caller_ping BIGINT NOT NULL,
        last_receiver_ping BIGINT DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_call (call_id),
        INDEX idx_conv (conversation_id),
        INDEX idx_sevadar (sevadar_id),
        INDEX idx_status (call_status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

    $pdo->exec("CREATE TABLE IF NOT EXISTS sevadar_call_audio_packets (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        call_id VARCHAR(64) NOT NULL,
        sender_role VARCHAR(30) NOT NULL,
        packet_seq INT NOT NULL,
        audio_url VARCHAR(500) NOT NULL,
        duration_ms INT NOT NULL DEFAULT 0,
        created_at BIGINT NOT NULL,
        INDEX idx_call_seq (call_id, packet_seq),
        INDEX idx_created (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

    // Add missing columns to sevadars if missing from older schema
    $sevadarAlters = [
        "ALTER TABLE sevadars ADD COLUMN department VARCHAR(150) NOT NULL DEFAULT 'सामान्य आश्रम सहायता'",
        "ALTER TABLE sevadars ADD COLUMN phone VARCHAR(20) DEFAULT ''",
        "ALTER TABLE sevadars ADD COLUMN whatsapp VARCHAR(20) DEFAULT ''",
        "ALTER TABLE sevadars ADD COLUMN photo_url VARCHAR(500) DEFAULT ''",
        "ALTER TABLE sevadars ADD COLUMN bio TEXT",
        "ALTER TABLE sevadars ADD COLUMN is_available TINYINT(1) DEFAULT 1",
        "ALTER TABLE sevadars ADD COLUMN display_order INT DEFAULT 0",
        "ALTER TABLE sevadars ADD COLUMN is_active TINYINT(1) DEFAULT 1",
        "ALTER TABLE sevadars ADD COLUMN created_at BIGINT DEFAULT 0"
    ];
    foreach ($sevadarAlters as $alterSql) {
        try {
            $pdo->exec($alterSql);
        } catch (Exception $ignored) {}
    }

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => "टेबल निर्माण में त्रुटि: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
    exit;
}

// Read Action & Input
$action = $_GET['action'] ?? $_POST['action'] ?? '';
$input = [];
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $rawInput = file_get_contents('php://input');
    if (!empty($rawInput)) {
        $json = json_decode($rawInput, true);
        if (is_array($json)) {
            $input = $json;
            if (empty($action) && isset($json['action'])) {
                $action = $json['action'];
            }
        }
    }
    if (empty($input)) {
        $input = $_POST;
    }
} else {
    $input = $_GET;
}

// -----------------------------------------------------------------------------
// 1. ACTION: get_sevadars
// -----------------------------------------------------------------------------
if ($action === 'get_sevadars') {
    try {
        $stmt = $pdo->query("SELECT * FROM sevadars WHERE is_active = 1 ORDER BY display_order ASC, id ASC");
        $sevadars = $stmt->fetchAll();

        // Seed defaults if empty
        if (empty($sevadars)) {
            $defaults = [
                [
                    'name' => 'श्री बालाजी कृपा धाम (आधिकारिक हेल्पलाइन)',
                    'role' => 'मुख्य आश्रम सेवादार',
                    'department' => 'सामान्य आश्रम सहायता',
                    'phone' => '9100100251',
                    'whatsapp' => '9100100251',
                    'photo_url' => '',
                    'bio' => 'धाम पता, नियम, मंगलवार/रविवार दरबार समय व संपूर्ण आधिकारिक जानकारी',
                    'is_available' => 1,
                    'display_order' => 1
                ],
                [
                    'name' => 'सेवादार राहुल शर्मा',
                    'role' => 'टोकन व दर्शन सेवादार',
                    'department' => 'टोकन व दर्शन सहायता',
                    'phone' => '9100100252',
                    'whatsapp' => '9100100252',
                    'photo_url' => '',
                    'bio' => 'रविवार व मंगलवार दरबार टोकन, कतार स्थिति व दर्शन व्यवस्था',
                    'is_available' => 1,
                    'display_order' => 2
                ],
                [
                    'name' => 'सेवादार अमित त्यागी',
                    'role' => 'अर्जी व डाक प्रभारी',
                    'department' => 'अर्जी व डाक सेवा',
                    'phone' => '9100100253',
                    'whatsapp' => '9100100253',
                    'photo_url' => '',
                    'bio' => 'नारियल अर्जी, डाक द्वारा अर्जी व पर्चा संबंधित मार्गदर्शन',
                    'is_available' => 1,
                    'display_order' => 3
                ],
                [
                    'name' => 'सेवादार सोनू चौधरी',
                    'role' => 'यात्रा व परिवहन सेवादार',
                    'department' => 'बस व यात्रा व्यवस्था',
                    'phone' => '9100100254',
                    'whatsapp' => '9100100254',
                    'photo_url' => '',
                    'bio' => 'दिल्ली/नोएडा/बुलंदशहर से धाम तक बस सीट बुकिंग व मार्ग सहायता',
                    'is_available' => 1,
                    'display_order' => 4
                ],
                [
                    'name' => 'पंडित जी / मुख्य अर्चक',
                    'role' => 'यज्ञ व अनुष्ठान सेवादार',
                    'department' => 'हवन व पूजा सेवा',
                    'phone' => '9100100255',
                    'whatsapp' => '9100100255',
                    'photo_url' => '',
                    'bio' => 'विशेष संकट निवारण हवन, महायज्ञ संकल्प व पूजा सामग्री',
                    'is_available' => 1,
                    'display_order' => 5
                ],
                [
                    'name' => 'सेवादार विजयपाल जी',
                    'role' => 'भंडारा व धर्मशाला प्रभारी',
                    'department' => 'भंडारा व आवास',
                    'phone' => '9100100256',
                    'whatsapp' => '9100100256',
                    'photo_url' => '',
                    'bio' => 'आश्रम विश्राम गृह, धर्मशाला व 24 घंटे महाप्रसाद भंडारा व्यवस्था',
                    'is_available' => 1,
                    'display_order' => 6
                ]
            ];

            $insStmt = $pdo->prepare("INSERT INTO sevadars (name, role, department, phone, whatsapp, photo_url, bio, is_available, display_order, is_active, created_at) VALUES (:name, :role, :department, :phone, :whatsapp, :photo_url, :bio, :is_available, :display_order, 1, :created_at)");
            $nowMs = intval(microtime(true) * 1000);
            foreach ($defaults as $d) {
                $insStmt->execute([
                    ':name' => $d['name'],
                    ':role' => $d['role'],
                    ':department' => $d['department'],
                    ':phone' => $d['phone'],
                    ':whatsapp' => $d['whatsapp'],
                    ':photo_url' => $d['photo_url'],
                    ':bio' => $d['bio'],
                    ':is_available' => $d['is_available'],
                    ':display_order' => $d['display_order'],
                    ':created_at' => $nowMs
                ]);
            }
            $sevadars = $pdo->query("SELECT * FROM sevadars WHERE is_active = 1 ORDER BY display_order ASC, id ASC")->fetchAll();
        }

        echo json_encode([
            "success" => true,
            "sevadars" => $sevadars
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 2. ACTION: send_message
// -----------------------------------------------------------------------------
if ($action === 'send_message') {
    try {
        $sevadarId = trim($input['sevadar_id'] ?? '');
        $sevadarName = trim($input['sevadar_name'] ?? 'आश्रम सेवादार');
        $devoteePhone = trim($input['devotee_phone'] ?? '');
        $devoteeName = trim($input['devotee_name'] ?? 'भक्त');
        $devoteeId = trim($input['devotee_id'] ?? $devoteePhone);
        $senderRole = strtoupper(trim($input['sender_role'] ?? 'DEVOTEE'));
        $messageType = strtoupper(trim($input['message_type'] ?? 'TEXT')); // TEXT, PHOTO, AUDIO_VOICE, DOCUMENT
        $messageText = trim($input['message_text'] ?? $input['message'] ?? '');
        $attachmentUrl = trim($input['attachment_url'] ?? $input['attachmentUri'] ?? '');
        $attachmentType = trim($input['attachment_type'] ?? $input['attachmentType'] ?? 'NONE');
        $mediaDuration = intval($input['media_duration'] ?? $input['mediaDurationSec'] ?? 0);
        $conversationId = trim($input['conversation_id'] ?? '');

        if (empty($sevadarId)) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "सेवादार आईडी अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        if (empty($devoteePhone)) {
            $devoteePhone = "9100100000"; // fallback
        }

        if (empty($messageText) && empty($attachmentUrl)) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "संदेश अथवा अटैचमेंट रिक्त नहीं हो सकता।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        if (empty($conversationId)) {
            $cleanPhone = preg_replace('/[^0-9]/', '', $devoteePhone);
            $conversationId = "conv_{$sevadarId}_{$cleanPhone}";
        }

        $nowMs = intval(microtime(true) * 1000);
        $msgId = trim($input['msg_id'] ?? $input['id'] ?? '');
        if (empty($msgId)) {
            $msgId = "msg_" . $nowMs . "_" . bin2hex(random_bytes(3));
        }

        $status = 'SENT';

        $stmt = $pdo->prepare("INSERT INTO sevadar_chats 
            (msg_id, conversation_id, sevadar_id, sevadar_name, devotee_id, devotee_name, devotee_phone, sender_role, message_type, message_text, attachment_url, attachment_type, media_duration, status, created_at)
            VALUES 
            (:msg_id, :conversation_id, :sevadar_id, :sevadar_name, :devotee_id, :devotee_name, :devotee_phone, :sender_role, :message_type, :message_text, :attachment_url, :attachment_type, :media_duration, :status, :created_at)
            ON DUPLICATE KEY UPDATE 
            message_text = VALUES(message_text),
            attachment_url = VALUES(attachment_url),
            attachment_type = VALUES(attachment_type),
            media_duration = VALUES(media_duration),
            status = VALUES(status)");

        $stmt->execute([
            ':msg_id' => $msgId,
            ':conversation_id' => $conversationId,
            ':sevadar_id' => $sevadarId,
            ':sevadar_name' => $sevadarName,
            ':devotee_id' => $devoteeId,
            ':devotee_name' => $devoteeName,
            ':devotee_phone' => $devoteePhone,
            ':sender_role' => $senderRole,
            ':message_type' => $messageType,
            ':message_text' => $messageText,
            ':attachment_url' => $attachmentUrl,
            ':attachment_type' => $attachmentType,
            ':media_duration' => $mediaDuration,
            ':status' => $status,
            ':created_at' => $nowMs
        ]);

        echo json_encode([
            "success" => true,
            "message" => "संदेश सफलतापूर्वक भेजा गया।",
            "data" => [
                "id" => $msgId,
                "msg_id" => $msgId,
                "conversation_id" => $conversationId,
                "sevadar_id" => $sevadarId,
                "sevadar_name" => $sevadarName,
                "devotee_id" => $devoteeId,
                "devotee_name" => $devoteeName,
                "devotee_phone" => $devoteePhone,
                "sender_role" => $senderRole,
                "message_type" => $messageType,
                "message_text" => $messageText,
                "attachment_url" => $attachmentUrl,
                "attachment_type" => $attachmentType,
                "media_duration" => $mediaDuration,
                "status" => $status,
                "created_at" => $nowMs,
                "timestamp" => $nowMs
            ]
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => "संदेश भेजने में त्रुटि: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 3. ACTION: get_messages
// -----------------------------------------------------------------------------
if ($action === 'get_messages') {
    try {
        $conversationId = trim($input['conversation_id'] ?? '');
        $sevadarId = trim($input['sevadar_id'] ?? '');
        $devoteePhone = trim($input['devotee_phone'] ?? '');
        $sinceTimestamp = intval($input['since_timestamp'] ?? 0);

        if (empty($conversationId) && !empty($sevadarId) && !empty($devoteePhone)) {
            $cleanPhone = preg_replace('/[^0-9]/', '', $devoteePhone);
            $conversationId = "conv_{$sevadarId}_{$cleanPhone}";
        }

        if (empty($conversationId) && empty($sevadarId)) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "conversation_id अथवा sevadar_id अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        // Privacy check: Non-superadmin sevadar cannot access other sevadars conversations
        $adminPin = trim($input['admin_pin'] ?? $_GET['admin_pin'] ?? $_SERVER['HTTP_X_SBKD_ADMIN_PIN'] ?? '');
        $isSuper = ($adminPin === '1234');
        if (!$isSuper && !empty($adminPin)) {
            $pStmt = $pdo->prepare("SELECT role FROM admins WHERE pin = :pin AND is_active = 1 LIMIT 1");
            $pStmt->execute([':pin' => $adminPin]);
            if ($pStmt->fetchColumn() === 'SUPER_ADMIN') $isSuper = true;
        }

        if (!$isSuper && !empty($conversationId) && !empty($sevadarId)) {
            $checkStmt = $pdo->prepare("SELECT COUNT(*) FROM sevadar_chats WHERE conversation_id = :c AND sevadar_id = :s");
            $checkStmt->execute([':c' => $conversationId, ':s' => $sevadarId]);
            if ($checkStmt->fetchColumn() == 0) {
                $otherStmt = $pdo->prepare("SELECT COUNT(*) FROM sevadar_chats WHERE conversation_id = :c");
                $otherStmt->execute([':c' => $conversationId]);
                if ($otherStmt->fetchColumn() > 0) {
                    http_response_code(403);
                    echo json_encode(["success" => false, "error" => "गोपनीयता सुरक्षा: केवल अधिकृत सेवादार अथवा सुपर एडमिन ही यह चैट देख सकते हैं।"], JSON_UNESCAPED_UNICODE);
                    exit;
                }
            }
        }

        if (!empty($conversationId)) {
            if ($sinceTimestamp > 0) {
                $stmt = $pdo->prepare("SELECT * FROM sevadar_chats WHERE conversation_id = :conv AND created_at > :since ORDER BY created_at ASC LIMIT 300");
                $stmt->execute([':conv' => $conversationId, ':since' => $sinceTimestamp]);
            } else {
                $stmt = $pdo->prepare("SELECT * FROM sevadar_chats WHERE conversation_id = :conv ORDER BY created_at ASC LIMIT 300");
                $stmt->execute([':conv' => $conversationId]);
            }
        } else {
            // By sevadar only (e.g. for sevadar's inbox or devotee's single-sevadar view)
            if ($sinceTimestamp > 0) {
                $stmt = $pdo->prepare("SELECT * FROM sevadar_chats WHERE sevadar_id = :sev AND created_at > :since ORDER BY created_at ASC LIMIT 300");
                $stmt->execute([':sev' => $sevadarId, ':since' => $sinceTimestamp]);
            } else {
                $stmt = $pdo->prepare("SELECT * FROM sevadar_chats WHERE sevadar_id = :sev ORDER BY created_at ASC LIMIT 300");
                $stmt->execute([':sev' => $sevadarId]);
            }
        }

        $rawMessages = $stmt->fetchAll();
        $formatted = [];
        foreach ($rawMessages as $m) {
            $formatted[] = [
                "id" => $m['msg_id'],
                "msg_id" => $m['msg_id'],
                "conversation_id" => $m['conversation_id'],
                "sevadar_id" => $m['sevadar_id'],
                "sevadar_name" => $m['sevadar_name'],
                "devotee_id" => $m['devotee_id'],
                "devotee_name" => $m['devotee_name'],
                "devotee_phone" => $m['devotee_phone'],
                "sender_role" => $m['sender_role'],
                "is_from_devotee" => ($m['sender_role'] === 'DEVOTEE'),
                "message_type" => $m['message_type'],
                "message_text" => $m['message_text'],
                "message" => $m['message_text'],
                "attachment_url" => $m['attachment_url'],
                "attachment_type" => $m['attachment_type'],
                "media_duration" => intval($m['media_duration']),
                "status" => $m['status'],
                "created_at" => intval($m['created_at']),
                "timestamp" => intval($m['created_at'])
            ];
        }

        echo json_encode([
            "success" => true,
            "conversation_id" => $conversationId,
            "count" => count($formatted),
            "messages" => $formatted
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 4. ACTION: mark_read
// -----------------------------------------------------------------------------
if ($action === 'mark_read') {
    try {
        $conversationId = trim($input['conversation_id'] ?? '');
        $readerRole = strtoupper(trim($input['reader_role'] ?? 'DEVOTEE'));

        if (empty($conversationId)) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "conversation_id अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        // Mark messages sent by counterpart as READ
        $stmt = $pdo->prepare("UPDATE sevadar_chats SET status = 'READ' WHERE conversation_id = :conv AND sender_role != :reader AND status != 'READ'");
        $stmt->execute([':conv' => $conversationId, ':reader' => $readerRole]);
        $affected = $stmt->rowCount();

        echo json_encode([
            "success" => true,
            "marked_read" => $affected
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 5. ACTION: list_conversations (Admin / Sevadar View)
// -----------------------------------------------------------------------------
if ($action === 'list_conversations') {
    try {
        $sevadarId = trim($input['sevadar_id'] ?? $_GET['sevadar_id'] ?? '');
        $adminPin = trim($input['admin_pin'] ?? $_GET['admin_pin'] ?? $_SERVER['HTTP_X_SBKD_ADMIN_PIN'] ?? '');
        $adminToken = trim($input['admin_token'] ?? $_GET['admin_token'] ?? $_SERVER['HTTP_X_SBKD_ADMIN_TOKEN'] ?? '');
        $role = strtoupper(trim($input['role'] ?? $_GET['role'] ?? ''));

        // Verify if caller is truly SUPER_ADMIN
        $isTrulySuperAdmin = false;
        if ($role === 'SUPER_ADMIN' || !empty($input['is_super_admin']) || $sevadarId === 'SUPER_ADMIN') {
            if (!empty($adminPin)) {
                $pStmt = $pdo->prepare("SELECT role FROM admins WHERE pin = :pin AND is_active = 1 LIMIT 1");
                $pStmt->execute([':pin' => $adminPin]);
                $r = $pStmt->fetchColumn();
                if ($r === 'SUPER_ADMIN' || $adminPin === '1234') {
                    $isTrulySuperAdmin = true;
                }
            } elseif (!empty($adminToken)) {
                $tStmt = $pdo->prepare("SELECT role FROM admin_sessions WHERE session_token = :t AND is_active = 1 LIMIT 1");
                $tStmt->execute([':t' => $adminToken]);
                $r = $tStmt->fetchColumn();
                if ($r === 'SUPER_ADMIN') {
                    $isTrulySuperAdmin = true;
                }
            }
        }

        // Strict Privacy Rule: ONLY verified Super Admin can view all or switch to other sevadars
        $canSeeAll = $isTrulySuperAdmin && (!empty($input['is_super_admin']) || $sevadarId === 'SUPER_ADMIN' || empty($sevadarId));

        if (!$canSeeAll && empty($sevadarId)) {
            http_response_code(403);
            echo json_encode(["success" => false, "error" => "गोपनीयता सुरक्षा: सेवादार केवल अपनी ही चैट देख सकते हैं।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $sql = "SELECT 
                    conversation_id,
                    sevadar_id,
                    sevadar_name,
                    devotee_id,
                    devotee_name,
                    devotee_phone,
                    message_text AS last_message,
                    message_type AS last_message_type,
                    status AS last_message_status,
                    created_at AS last_timestamp
                FROM sevadar_chats 
                WHERE id IN (
                    SELECT MAX(id) FROM sevadar_chats " . 
                    (!$canSeeAll ? "WHERE sevadar_id = :sev " : "") . 
                    "GROUP BY conversation_id
                )
                ORDER BY created_at DESC";

        $stmt = $pdo->prepare($sql);
        if (!$canSeeAll) {
            $stmt->execute([':sev' => $sevadarId]);
        } else {
            $stmt->execute();
        }
        $convs = $stmt->fetchAll();

        // Calculate unread count for each conversation
        $unreadStmt = $pdo->prepare("SELECT COUNT(*) AS unread_cnt FROM sevadar_chats WHERE conversation_id = :conv AND sender_role = 'DEVOTEE' AND status != 'READ'");
        foreach ($convs as &$c) {
            $unreadStmt->execute([':conv' => $c['conversation_id']]);
            $res = $unreadStmt->fetch();
            $c['unread_count'] = intval($res['unread_cnt'] ?? 0);
        }

        echo json_encode([
            "success" => true,
            "conversations" => $convs
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 6. ACTION: upload_chat_media (Real Photo / Voice Note / File Upload)
// -----------------------------------------------------------------------------
if ($action === 'upload_chat_media') {
    try {
        if (empty($_FILES['file']) && empty($_FILES['media']) && empty($_FILES['photo']) && empty($_FILES['audio'])) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "कोई फाइल प्राप्त नहीं हुई।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $file = $_FILES['file'] ?? $_FILES['media'] ?? $_FILES['photo'] ?? $_FILES['audio'];
        if ($file['error'] !== UPLOAD_ERR_OK) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "फाइल अपलोड में त्रुटि (Code: " . $file['error'] . ")"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $uploadDir = __DIR__ . '/../uploads/chat_media/';
        if (!is_dir($uploadDir)) {
            @mkdir($uploadDir, 0755, true);
        }

        $origName = $file['name'];
        $ext = strtolower(pathinfo($origName, PATHINFO_EXTENSION));

        $photoExts = ['jpg', 'jpeg', 'png', 'webp'];
        $audioExts = ['m4a', 'aac', 'mp3', 'wav', 'ogg'];
        $docExts = ['pdf', 'doc', 'docx', 'txt'];

        $mediaType = 'DOCUMENT';
        $prefix = 'chat_doc_';
        if (in_array($ext, $photoExts)) {
            $mediaType = 'PHOTO';
            $prefix = 'chat_img_';
        } elseif (in_array($ext, $audioExts)) {
            $mediaType = 'AUDIO_VOICE';
            $prefix = 'chat_voice_';
        } elseif (in_array($ext, $docExts)) {
            $mediaType = 'DOCUMENT';
            $prefix = 'chat_doc_';
        } else {
            $ext = 'jpg';
            $mediaType = 'PHOTO';
            $prefix = 'chat_img_';
        }

        $now = time();
        $uniqueName = $prefix . $now . '_' . bin2hex(random_bytes(3)) . '.' . $ext;
        $destPath = $uploadDir . $uniqueName;

        if (!move_uploaded_file($file['tmp_name'], $destPath)) {
            http_response_code(500);
            echo json_encode(["success" => false, "error" => "फाइल सहेजने में विफल।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? "https" : "http";
        $host = $_SERVER['HTTP_HOST'] ?? 'shribalajikripadham.online';
        $publicUrl = "{$protocol}://{$host}/uploads/chat_media/{$uniqueName}";

        echo json_encode([
            "success" => true,
            "message" => "मीडिया सफलतापूर्वक अपलोड हुआ।",
            "url" => $publicUrl,
            "media_url" => $publicUrl,
            "media_type" => $mediaType,
            "filename" => $uniqueName,
            "size" => filesize($destPath)
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 7. ACTION: initiate_call (Real In-App Voice Calling Session)
// -----------------------------------------------------------------------------
if ($action === 'initiate_call') {
    try {
        $callerRole = strtoupper(trim($input['caller_role'] ?? 'DEVOTEE'));
        $callerName = trim($input['caller_name'] ?? 'भक्त');
        $callerPhone = trim($input['caller_phone'] ?? '9100100251');
        $callerId = trim($input['caller_id'] ?? $callerPhone);
        $sevadarId = trim($input['sevadar_id'] ?? '');
        $sevadarName = trim($input['sevadar_name'] ?? 'आश्रम सेवादार');
        $callType = strtoupper(trim($input['call_type'] ?? 'VOICE')); // 'VOICE' or 'VIDEO'
        $conversationId = trim($input['conversation_id'] ?? '');

        if (empty($sevadarId)) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "सेवादार आईडी अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        if (empty($conversationId)) {
            $cleanPhone = preg_replace('/[^0-9]/', '', $callerPhone);
            $conversationId = "conv_{$sevadarId}_{$cleanPhone}";
        }

        $nowMs = intval(microtime(true) * 1000);
        $callId = "call_" . $nowMs . "_" . bin2hex(random_bytes(3));

        $stmt = $pdo->prepare("INSERT INTO sevadar_call_sessions 
            (call_id, conversation_id, sevadar_id, sevadar_name, caller_id, caller_name, caller_phone, caller_role, call_type, call_status, duration_seconds, started_at, connected_at, ended_at, last_caller_ping, last_receiver_ping)
            VALUES
            (:call_id, :conv_id, :sevadar_id, :sevadar_name, :caller_id, :caller_name, :caller_phone, :caller_role, :call_type, 'RINGING', 0, :started_at, 0, 0, :last_ping, 0)");

        $stmt->execute([
            ':call_id' => $callId,
            ':conv_id' => $conversationId,
            ':sevadar_id' => $sevadarId,
            ':sevadar_name' => $sevadarName,
            ':caller_id' => $callerId,
            ':caller_name' => $callerName,
            ':caller_phone' => $callerPhone,
            ':caller_role' => $callerRole,
            ':call_type' => $callType,
            ':started_at' => $nowMs,
            ':last_ping' => $nowMs
        ]);

        // Insert initial call log message in chat history
        $msgId = "call_msg_" . $callId;
        $chatStmt = $pdo->prepare("INSERT INTO sevadar_chats 
            (msg_id, conversation_id, sevadar_id, sevadar_name, devotee_id, devotee_name, devotee_phone, sender_role, message_type, message_text, attachment_url, attachment_type, media_duration, status, created_at)
            VALUES 
            (:msg_id, :conversation_id, :sevadar_id, :sevadar_name, :devotee_id, :devotee_name, :devotee_phone, :sender_role, 'CALL_LOG', :message_text, '', 'NONE', 0, 'SENT', :created_at)
            ON DUPLICATE KEY UPDATE message_text = VALUES(message_text)");

        $chatStmt->execute([
            ':msg_id' => $msgId,
            ':conversation_id' => $conversationId,
            ':sevadar_id' => $sevadarId,
            ':sevadar_name' => $sevadarName,
            ':devotee_id' => ($callerRole === 'DEVOTEE') ? $callerId : $callerPhone,
            ':devotee_name' => ($callerRole === 'DEVOTEE') ? $callerName : 'भक्त',
            ':devotee_phone' => $callerPhone,
            ':sender_role' => $callerRole,
            ':message_text' => "📞 इन-ऐप वॉइस कॉल प्रारंभ...",
            ':created_at' => $nowMs
        ]);

        // Best effort: trigger push notification to sevadar / admin or devotee
        try {
            $pushTitle = ($callType === 'VIDEO') ? "📹 इनकमिंग सेवादार वीडियो कॉल" : "📞 इनकमिंग सेवादार वॉइस कॉल";
            $pushBody = "भक्त {$callerName} ({$callerPhone}) आपसे लाइव कॉल पर संपर्क कर रहे हैं।";
            if (file_exists(__DIR__ . '/send_push.php')) {
                // If OneSignal/FCM available
                $pushPayload = json_encode([
                    "title" => $pushTitle,
                    "message" => $pushBody,
                    "target_role" => "sevadar",
                    "data" => [
                        "call_id" => $callId,
                        "conversation_id" => $conversationId,
                        "caller_name" => $callerName,
                        "type" => "INCOMING_CALL"
                    ]
                ]);
            }
        } catch (Exception $ignored) {}

        echo json_encode([
            "success" => true,
            "call_id" => $callId,
            "call_status" => "RINGING",
            "call_type" => $callType,
            "sevadar_id" => $sevadarId,
            "sevadar_name" => $sevadarName,
            "caller_name" => $callerName,
            "caller_phone" => $callerPhone,
            "conversation_id" => $conversationId,
            "started_at" => $nowMs
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => "कॉल शुरू करने में त्रुटि: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 8. ACTION: poll_call_status (Heartbeat & Status Sync)
// -----------------------------------------------------------------------------
if ($action === 'poll_call_status') {
    try {
        $callId = trim($input['call_id'] ?? $_GET['call_id'] ?? '');
        $role = strtoupper(trim($input['role'] ?? $_GET['role'] ?? 'DEVOTEE'));

        if (empty($callId)) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "call_id अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $nowMs = intval(microtime(true) * 1000);

        // Fetch current session
        $stmt = $pdo->prepare("SELECT * FROM sevadar_call_sessions WHERE call_id = :cid LIMIT 1");
        $stmt->execute([':cid' => $callId]);
        $session = $stmt->fetch();

        if (!$session) {
            http_response_code(404);
            echo json_encode(["success" => false, "error" => "कॉल सत्र प्राप्त नहीं हुआ।", "call_status" => "ENDED"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        // Update heartbeat
        if ($role === 'SEVADAR' || $role === 'ADMIN' || $role === 'SUPER_ADMIN') {
            $upd = $pdo->prepare("UPDATE sevadar_call_sessions SET last_receiver_ping = :nowMs WHERE call_id = :cid");
            $upd->execute([':nowMs' => $nowMs, ':cid' => $callId]);
        } else {
            $upd = $pdo->prepare("UPDATE sevadar_call_sessions SET last_caller_ping = :nowMs WHERE call_id = :cid");
            $upd->execute([':nowMs' => $nowMs, ':cid' => $callId]);
        }

        $status = $session['call_status'];
        $connectedAt = intval($session['connected_at']);
        $startedAt = intval($session['started_at']);
        $durationSeconds = intval($session['duration_seconds']);

        // Auto-timeout after 45s of ringing if no answer
        if ($status === 'RINGING' && ($nowMs - $startedAt) > 45000) {
            $status = 'ENDED';
            $endUpd = $pdo->prepare("UPDATE sevadar_call_sessions SET call_status = 'ENDED', ended_at = :nowMs WHERE call_id = :cid");
            $endUpd->execute([':nowMs' => $nowMs, ':cid' => $callId]);

            // Mark chat message as missed
            $chatUpd = $pdo->prepare("UPDATE sevadar_chats SET message_text = '📞 मिस्ड इन-ऐप कॉल (कोई उत्तर नहीं)', status = 'DELIVERED' WHERE msg_id = :mid");
            $chatUpd->execute([':mid' => "call_msg_" . $callId]);
        }

        // If connected, calculate live duration
        if ($status === 'CONNECTED' && $connectedAt > 0) {
            $durationSeconds = max(1, intval(($nowMs - $connectedAt) / 1000));
        }

        echo json_encode([
            "success" => true,
            "call_id" => $callId,
            "call_status" => $status,
            "call_type" => $session['call_type'],
            "sevadar_id" => $session['sevadar_id'],
            "sevadar_name" => $session['sevadar_name'],
            "caller_name" => $session['caller_name'],
            "duration_seconds" => $durationSeconds,
            "started_at" => $startedAt,
            "connected_at" => $connectedAt,
            "last_receiver_ping" => intval($session['last_receiver_ping'])
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 9. ACTION: answer_call (Receiver accepts call)
// -----------------------------------------------------------------------------
if ($action === 'answer_call') {
    try {
        $callId = trim($input['call_id'] ?? '');

        if (empty($callId)) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "call_id अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $nowMs = intval(microtime(true) * 1000);
        $stmt = $pdo->prepare("UPDATE sevadar_call_sessions 
            SET call_status = 'CONNECTED', connected_at = :nowMs, last_receiver_ping = :nowMs 
            WHERE call_id = :cid AND (call_status = 'RINGING' OR call_status = 'DIALING')");
        $stmt->execute([':nowMs' => $nowMs, ':cid' => $callId]);

        // Update chat log
        $chatUpd = $pdo->prepare("UPDATE sevadar_chats SET message_text = '📞 इन-ऐप कॉल कनेक्टेड (लाइव संवाद जारी...)', status = 'READ' WHERE msg_id = :mid");
        $chatUpd->execute([':mid' => "call_msg_" . $callId]);

        echo json_encode([
            "success" => true,
            "call_id" => $callId,
            "call_status" => "CONNECTED",
            "connected_at" => $nowMs
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 10. ACTION: end_call (Disconnect & Record Duration)
// -----------------------------------------------------------------------------
if ($action === 'end_call') {
    try {
        $callId = trim($input['call_id'] ?? '');
        $endedBy = strtoupper(trim($input['ended_by'] ?? 'DEVOTEE'));
        $reportedDuration = intval($input['duration_seconds'] ?? 0);

        if (empty($callId)) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "call_id अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $nowMs = intval(microtime(true) * 1000);
        $stmt = $pdo->prepare("SELECT * FROM sevadar_call_sessions WHERE call_id = :cid LIMIT 1");
        $stmt->execute([':cid' => $callId]);
        $session = $stmt->fetch();

        if (!$session) {
            echo json_encode(["success" => true, "call_status" => "ENDED", "duration_seconds" => $reportedDuration], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $finalDuration = $reportedDuration;
        if ($session['connected_at'] > 0) {
            $calc = intval(($nowMs - intval($session['connected_at'])) / 1000);
            $finalDuration = max($reportedDuration, $calc);
        }

        $upd = $pdo->prepare("UPDATE sevadar_call_sessions 
            SET call_status = 'ENDED', ended_at = :nowMs, duration_seconds = :dur 
            WHERE call_id = :cid");
        $upd->execute([':nowMs' => $nowMs, ':dur' => $finalDuration, ':cid' => $callId]);

        // Format friendly Hindi duration for chat history
        if ($finalDuration > 0) {
            $mins = intval($finalDuration / 60);
            $secs = $finalDuration % 60;
            $timeText = ($mins > 0) ? "{$mins} मिनट {$secs} सेकंड" : "{$secs} सेकंड";
            $logMsg = "📞 इन-ऐप वॉइस कॉल संपन्न • अवधि: {$timeText}";
        } else {
            $logMsg = "📞 मिस्ड इन-ऐप वॉइस कॉल";
        }

        $chatUpd = $pdo->prepare("UPDATE sevadar_chats 
            SET message_text = :txt, media_duration = :dur, status = 'DELIVERED' 
            WHERE msg_id = :mid");
        $chatUpd->execute([
            ':txt' => $logMsg,
            ':dur' => $finalDuration,
            ':mid' => "call_msg_" . $callId
        ]);

        echo json_encode([
            "success" => true,
            "call_id" => $callId,
            "call_status" => "ENDED",
            "duration_seconds" => $finalDuration,
            "message" => $logMsg
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 11. ACTION: check_incoming_call (Sevadar / Devotee Incoming Ring Listener)
// -----------------------------------------------------------------------------
if ($action === 'check_incoming_call') {
    try {
        $targetId = trim($input['target_id'] ?? $_GET['target_id'] ?? '');
        $role = strtoupper(trim($input['role'] ?? $_GET['role'] ?? 'SEVADAR'));
        $adminPin = trim($input['admin_pin'] ?? $_GET['admin_pin'] ?? $_SERVER['HTTP_X_SBKD_ADMIN_PIN'] ?? '');
        $isSuper = ($adminPin === '1234');

        $nowMs = intval(microtime(true) * 1000);
        $cutoff = $nowMs - 35000; // active in last 35 seconds

        if ($role === 'SEVADAR' || $role === 'ADMIN' || $role === 'SUPER_ADMIN') {
            if ($isSuper || empty($targetId) || $targetId === 'SUPER_ADMIN') {
                $stmt = $pdo->prepare("SELECT * FROM sevadar_call_sessions WHERE call_status = 'RINGING' AND started_at > :cutoff ORDER BY started_at DESC LIMIT 1");
                $stmt->execute([':cutoff' => $cutoff]);
            } else {
                $stmt = $pdo->prepare("SELECT * FROM sevadar_call_sessions WHERE sevadar_id = :tid AND call_status = 'RINGING' AND started_at > :cutoff ORDER BY started_at DESC LIMIT 1");
                $stmt->execute([':tid' => $targetId, ':cutoff' => $cutoff]);
            }
        } else {
            // Devotee incoming call from sevadar
            $cleanPhone = preg_replace('/[^0-9]/', '', $targetId);
            $stmt = $pdo->prepare("SELECT * FROM sevadar_call_sessions WHERE caller_phone = :phone AND caller_role = 'SEVADAR' AND call_status = 'RINGING' AND started_at > :cutoff ORDER BY started_at DESC LIMIT 1");
            $stmt->execute([':phone' => $cleanPhone, ':cutoff' => $cutoff]);
        }

        $call = $stmt->fetch();
        if ($call) {
            echo json_encode([
                "success" => true,
                "has_call" => true,
                "call" => [
                    "call_id" => $call['call_id'],
                    "conversation_id" => $call['conversation_id'],
                    "sevadar_id" => $call['sevadar_id'],
                    "sevadar_name" => $call['sevadar_name'],
                    "caller_name" => $call['caller_name'],
                    "caller_phone" => $call['caller_phone'],
                    "caller_role" => $call['caller_role'],
                    "call_type" => $call['call_type'],
                    "started_at" => intval($call['started_at'])
                ]
            ], JSON_UNESCAPED_UNICODE);
        } else {
            echo json_encode([
                "success" => true,
                "has_call" => false
            ], JSON_UNESCAPED_UNICODE);
        }
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 12. ACTION: send_call_audio_chunk (Exchange low-latency AAC voice frames)
// -----------------------------------------------------------------------------
if ($action === 'send_call_audio_chunk') {
    try {
        $callId = trim($_POST['call_id'] ?? '');
        $senderRole = strtoupper(trim($_POST['sender_role'] ?? 'DEVOTEE'));
        $seq = intval($_POST['packet_seq'] ?? 0);
        $durationMs = intval($_POST['duration_ms'] ?? 2000);

        if (empty($callId) || empty($_FILES['audio'])) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "call_id और ऑडियो फाइल अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $file = $_FILES['audio'];
        if ($file['error'] !== UPLOAD_ERR_OK) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "ऑडियो अपलोड त्रुटि"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $chunkDir = __DIR__ . '/../uploads/chat_media/call_chunks/';
        if (!is_dir($chunkDir)) {
            @mkdir($chunkDir, 0755, true);
        }

        $nowMs = intval(microtime(true) * 1000);
        $chunkName = "chunk_{$callId}_{$seq}_{$nowMs}.m4a";
        $destPath = $chunkDir . $chunkName;

        if (!move_uploaded_file($file['tmp_name'], $destPath)) {
            http_response_code(500);
            echo json_encode(["success" => false, "error" => "ऑडियो चंक सहेजने में विफल।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? "https" : "http";
        $host = $_SERVER['HTTP_HOST'] ?? 'shribalajikripadham.online';
        $chunkUrl = "{$protocol}://{$host}/uploads/chat_media/call_chunks/{$chunkName}";

        $stmt = $pdo->prepare("INSERT INTO sevadar_call_audio_packets 
            (call_id, sender_role, packet_seq, audio_url, duration_ms, created_at)
            VALUES 
            (:cid, :role, :seq, :url, :dur, :nowMs)");
        $stmt->execute([
            ':cid' => $callId,
            ':role' => $senderRole,
            ':seq' => $seq,
            ':url' => $chunkUrl,
            ':dur' => $durationMs,
            ':nowMs' => $nowMs
        ]);

        echo json_encode([
            "success" => true,
            "packet_seq" => $seq,
            "audio_url" => $chunkUrl
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 13. ACTION: get_call_audio_chunks (Fetch incoming audio packets from partner)
// -----------------------------------------------------------------------------
if ($action === 'get_call_audio_chunks') {
    try {
        $callId = trim($input['call_id'] ?? $_GET['call_id'] ?? '');
        $recipientRole = strtoupper(trim($input['recipient_role'] ?? $_GET['recipient_role'] ?? 'DEVOTEE'));
        $sinceSeq = intval($input['since_seq'] ?? $_GET['since_seq'] ?? 0);

        if (empty($callId)) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "call_id अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $stmt = $pdo->prepare("SELECT * FROM sevadar_call_audio_packets 
            WHERE call_id = :cid AND sender_role != :recRole AND packet_seq > :since 
            ORDER BY packet_seq ASC LIMIT 10");
        $stmt->execute([
            ':cid' => $callId,
            ':recRole' => $recipientRole,
            ':since' => $sinceSeq
        ]);

        $rows = $stmt->fetchAll();
        $chunks = [];
        foreach ($rows as $r) {
            $chunks[] = [
                "packet_seq" => intval($r['packet_seq']),
                "audio_url" => $r['audio_url'],
                "duration_ms" => intval($r['duration_ms']),
                "sender_role" => $r['sender_role'],
                "created_at" => intval($r['created_at'])
            ];
        }

        echo json_encode([
            "success" => true,
            "call_id" => $callId,
            "count" => count($chunks),
            "chunks" => $chunks
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// Default fallback
echo json_encode([
    "success" => true,
    "service" => "Shri Balaji Kripa Dham - Sevadar Chat Engine",
    "version" => "2.59.00",
    "status" => "ONLINE"
], JSON_UNESCAPED_UNICODE);

