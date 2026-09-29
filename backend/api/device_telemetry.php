<?php
// ==============================================================================
// श्री बालाजी कृपा धाम - केंद्रीय सक्रिय फोन एवं डिवाइस टेलीमेट्री सेवा
// Live Active Device Telemetry & Presence Tracking API
// ==============================================================================

if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} elseif (file_exists(__DIR__ . '/config/db.php')) {
    require_once __DIR__ . '/config/db.php';
}

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-SBKD-API-KEY, x-sbkd-api-key");
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
header("Pragma: no-cache");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

try {
    $pdo = getDB();
    if (!$pdo) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => "Database connection unavailable"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // Auto-create active_devices table
    $pdo->exec("CREATE TABLE IF NOT EXISTS active_devices (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        device_id VARCHAR(120) NOT NULL UNIQUE,
        device_model VARCHAR(100) NOT NULL DEFAULT 'Android Device',
        android_version VARCHAR(50) NOT NULL DEFAULT '',
        app_version VARCHAR(50) NOT NULL DEFAULT '2.56.18',
        user_name VARCHAR(100) NOT NULL DEFAULT '',
        phone_number VARCHAR(30) NOT NULL DEFAULT '',
        city VARCHAR(100) NOT NULL DEFAULT '',
        role VARCHAR(50) NOT NULL DEFAULT 'USER',
        open_count INT NOT NULL DEFAULT 1,
        ip_address VARCHAR(50) NOT NULL DEFAULT '',
        last_seen_at BIGINT NOT NULL,
        created_at BIGINT NOT NULL,
        INDEX idx_last_seen (last_seen_at DESC),
        INDEX idx_role (role)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

    $clientIp = $_SERVER['HTTP_CF_CONNECTING_IP'] ?? $_SERVER['HTTP_X_FORWARDED_FOR'] ?? $_SERVER['REMOTE_ADDR'] ?? '';
    if (strpos($clientIp, ',') !== false) {
        $clientIp = trim(explode(',', $clientIp)[0]);
    }
    $nowMs = round(microtime(true) * 1000);

    // =========================================================================
    // 1. RECORD DEVICE HEARTBEAT (POST)
    // =========================================================================
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $raw = file_get_contents('php://input');
        $input = json_decode($raw, true) ?: $_POST;

        $deviceId = trim($input['device_id'] ?? '');
        if (empty($deviceId)) {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "Device ID is required"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $deviceModel = trim($input['device_model'] ?? 'Android Device');
        $androidVersion = trim($input['android_version'] ?? '');
        $appVersion = trim($input['app_version'] ?? '2.56.13');
        $userName = trim($input['user_name'] ?? '');
        $phoneNumber = trim($input['phone_number'] ?? '');
        $city = trim($input['city'] ?? '');
        $role = strtoupper(trim($input['role'] ?? 'USER'));
        if (!in_array($role, ['SUPER_ADMIN', 'ADMIN', 'SEVADAR', 'USER'])) {
            $role = 'USER';
        }

        // If user_name is missing, assign clean devotee device name based on model
        if (empty($userName)) {
            $userName = "भक्त (" . ($deviceModel ?: 'Android Device') . ")";
        }

        // Upsert into active_devices
        $stmt = $pdo->prepare("
            INSERT INTO active_devices 
                (device_id, device_model, android_version, app_version, user_name, phone_number, city, role, open_count, ip_address, last_seen_at, created_at)
            VALUES 
                (:did, :model, :android, :appv, :uname, :phone, :city, :role, 1, :ip, :lastseen, :created)
            ON DUPLICATE KEY UPDATE
                device_model = IF(VALUES(device_model) != '' AND VALUES(device_model) != 'Android Device', VALUES(device_model), device_model),
                android_version = IF(VALUES(android_version) != '', VALUES(android_version), android_version),
                app_version = VALUES(app_version),
                user_name = IF(VALUES(user_name) != '', VALUES(user_name), user_name),
                phone_number = IF(VALUES(phone_number) != '', VALUES(phone_number), phone_number),
                city = IF(VALUES(city) != '', VALUES(city), city),
                role = IF(VALUES(role) != 'USER', VALUES(role), role),
                open_count = open_count + 1,
                ip_address = VALUES(ip_address),
                last_seen_at = VALUES(last_seen_at)
        ");

        $stmt->execute([
            ':did' => $deviceId,
            ':model' => $deviceModel,
            ':android' => $androidVersion,
            ':appv' => $appVersion,
            ':uname' => $userName,
            ':phone' => $phoneNumber,
            ':city' => $city,
            ':role' => $role,
            ':ip' => $clientIp,
            ':lastseen' => $nowMs,
            ':created' => $nowMs
        ]);

        echo json_encode([
            "success" => true,
            "message" => "हार्टबीट दर्ज की गई (Heartbeat recorded)",
            "device_id" => $deviceId,
            "user_name" => $userName,
            "role" => $role,
            "timestamp" => $nowMs
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        exit;
    }

    // =========================================================================
    // 2. FETCH ALL ACTIVE DEVICES TELEMETRY (GET)
    // =========================================================================

    // Today midnight in IST (UTC+5:30)
    $todayMidnightMs = (strtotime('today midnight')) * 1000;
    $fifteenMinsAgoMs = $nowMs - (15 * 60 * 1000);

    // Fetch total and active today counts
    $countStmt = $pdo->query("SELECT COUNT(*) AS total, SUM(CASE WHEN last_seen_at >= $todayMidnightMs THEN 1 ELSE 0 END) AS active_today FROM active_devices");
    $counts = $countStmt->fetch(PDO::FETCH_ASSOC);
    $totalDevices = (int)($counts['total'] ?? 0);
    $activeToday = (int)($counts['active_today'] ?? 0);

    // Fetch all devices sorted by latest activity
    $devStmt = $pdo->query("
        SELECT 
            device_id,
            device_model,
            android_version,
            app_version,
            user_name,
            phone_number,
            city,
            role,
            open_count,
            ip_address,
            last_seen_at,
            created_at
        FROM active_devices
        ORDER BY last_seen_at DESC
        LIMIT 500
    ");

    $devices = [];
    while ($row = $devStmt->fetch(PDO::FETCH_ASSOC)) {
        $lastSeen = (int)$row['last_seen_at'];
        $devices[] = [
            "device_id" => $row['device_id'],
            "device_model" => !empty($row['device_model']) ? $row['device_model'] : 'Android Device',
            "android_version" => $row['android_version'] ?? '',
            "app_version" => !empty($row['app_version']) ? $row['app_version'] : '2.56.13',
            "user_name" => $row['user_name'] ?? '',
            "phone_number" => $row['phone_number'] ?? '',
            "city" => $row['city'] ?? '',
            "role" => !empty($row['role']) ? $row['role'] : 'USER',
            "open_count" => (int)($row['open_count'] ?? 1),
            "ip_address" => $row['ip_address'] ?? '',
            "last_seen_at" => $lastSeen,
            "created_at" => (int)($row['created_at'] ?? $lastSeen),
            "is_online" => ($lastSeen >= $fifteenMinsAgoMs)
        ];
    }

    echo json_encode([
        "success" => true,
        "total_devices" => $totalDevices,
        "active_today" => $activeToday,
        "devices" => $devices,
        "server_time" => $nowMs
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "error" => "सर्वर त्रुटि: " . $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
}
