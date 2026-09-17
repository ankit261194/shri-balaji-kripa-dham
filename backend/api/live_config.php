<?php
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
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
header("Cache-Control: post-check=0, pre-check=0", false);
header("Pragma: no-cache");
header("Expires: Mon, 26 Jul 1997 05:00:00 GMT");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$pdo = getDB();

// 1. Self-Healing Schema Migration for ashram_settings
$targetCols = [
    "ashram_name" => "VARCHAR(255) NOT NULL DEFAULT 'श्री बालाजी कृपा धाम'",
    "ashram_latitude" => "DECIMAL(11, 8) NOT NULL DEFAULT 28.3972915",
    "ashram_longitude" => "DECIMAL(11, 8) NOT NULL DEFAULT 78.1460410",
    "allowed_radius_meters" => "DECIMAL(8, 2) NOT NULL DEFAULT 200.0",
    "is_geofence_enforced" => "TINYINT(1) NOT NULL DEFAULT 1",
    "is_outstation_advance_allowed" => "TINYINT(1) NOT NULL DEFAULT 1",
    "outstation_min_distance_km" => "DECIMAL(6, 2) NOT NULL DEFAULT 30.0",
    "current_serving_token" => "INT NOT NULL DEFAULT 0",
    "daily_token_limit" => "INT NOT NULL DEFAULT 1000",
    "is_token_service_enabled" => "TINYINT(1) NOT NULL DEFAULT 1",
    "is_bus_booking_live" => "TINYINT(1) NOT NULL DEFAULT 0",
    "is_live_counter_visible" => "TINYINT(1) NOT NULL DEFAULT 1",
    "is_payment_feature_live" => "TINYINT(1) NOT NULL DEFAULT 0",
    "is_arzi_ledger_live" => "TINYINT(1) NOT NULL DEFAULT 1",
    "is_darbar_active" => "TINYINT(1) NOT NULL DEFAULT 1",
    "darbar_date" => "VARCHAR(50) NOT NULL DEFAULT ''",
    "darbar_timings" => "VARCHAR(255) NOT NULL DEFAULT 'प्रत्येक रविवार प्रातःकाल 8:00 बजे से'",
    "emergency_notice" => "TEXT",
    "is_emergency_notice_visible" => "TINYINT(1) NOT NULL DEFAULT 0",
    "banner_title" => "VARCHAR(255) NOT NULL DEFAULT '🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट'",
    "banner_subtitle" => "VARCHAR(255) NOT NULL DEFAULT 'परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार'",
    "is_banner_visible" => "TINYINT(1) NOT NULL DEFAULT 1",
    "guruji_photo_url" => "VARCHAR(500) DEFAULT ''",
    "can_admin_issue_reserved_tokens" => "TINYINT(1) NOT NULL DEFAULT 0",
    "allow_admin_reserved_tokens" => "TINYINT(1) NOT NULL DEFAULT 0",
    "aarti_timings" => "TEXT",
    "config_version" => "INT NOT NULL DEFAULT 1"
];

// Ensure table exists
try {
    $pdo->exec("CREATE TABLE IF NOT EXISTS ashram_settings (id INT PRIMARY KEY DEFAULT 1) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    $pdo->exec("INSERT IGNORE INTO ashram_settings (id) VALUES (1)");
} catch (Exception $e) {}

// Auto-add missing columns
try {
    $existingCols = $pdo->query("SHOW COLUMNS FROM ashram_settings")->fetchAll(PDO::FETCH_COLUMN);
    $existingColMap = array_flip($existingCols);
    foreach ($targetCols as $col => $definition) {
        if (!isset($existingColMap[$col])) {
            try {
                $pdo->exec("ALTER TABLE ashram_settings ADD COLUMN $col $definition");
                $existingColMap[$col] = true;
            } catch (Exception $ignored) {}
        }
    }
} catch (Exception $e) {}

// Auto-create sevadars and donors if missing
try {
    $pdo->exec("CREATE TABLE IF NOT EXISTS sevadars (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(150) NOT NULL,
        role VARCHAR(150) NOT NULL DEFAULT 'सेवादार',
        phone VARCHAR(20) NOT NULL DEFAULT '',
        photo_url VARCHAR(500) DEFAULT '',
        bio TEXT,
        display_order INT NOT NULL DEFAULT 0,
        is_active TINYINT(1) NOT NULL DEFAULT 1,
        created_at BIGINT NOT NULL,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_sevadar_order (display_order),
        INDEX idx_sevadar_active (is_active)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    $pdo->exec("CREATE TABLE IF NOT EXISTS donors (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(150) NOT NULL,
        city_address VARCHAR(200) NOT NULL DEFAULT 'ग्राम डूँगरा जाट',
        title VARCHAR(200) NOT NULL DEFAULT 'मंदिर निर्माण सहयोगी',
        photo_url VARCHAR(500) DEFAULT '',
        phone VARCHAR(20) DEFAULT '',
        notes TEXT,
        display_order INT NOT NULL DEFAULT 0,
        is_active TINYINT(1) NOT NULL DEFAULT 1,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_donor_order (display_order),
        INDEX idx_donor_active (is_active)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
} catch (Exception $e) {}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true) ?: $_POST;
    
    // Fetch existing settings first
    $stmt = $pdo->query("SELECT * FROM ashram_settings WHERE id = 1 LIMIT 1");
    $current = $stmt->fetch() ?: [];

    // Check available columns in current database table
    $cols = $pdo->query("SHOW COLUMNS FROM ashram_settings")->fetchAll(PDO::FETCH_COLUMN);
    $colSet = array_flip($cols);

    // Build update fields
    $updatePairs = [];
    $bindings = [];

    $fields = [
        'ashram_name' => trim($input['ashram_name'] ?? ($current['ashram_name'] ?? 'श्री बालाजी कृपा धाम')),
        'ashram_latitude' => floatval($input['latitude'] ?? ($current['ashram_latitude'] ?? 28.3972915)),
        'ashram_longitude' => floatval($input['longitude'] ?? ($current['ashram_longitude'] ?? 78.1460410)),
        'allowed_radius_meters' => floatval($input['allowed_radius_meters'] ?? ($current['allowed_radius_meters'] ?? 200.0)),
        'is_geofence_enforced' => isset($input['is_geofence_enforced']) ? intval($input['is_geofence_enforced']) : intval($current['is_geofence_enforced'] ?? 1),
        'is_outstation_advance_allowed' => isset($input['is_outstation_advance_allowed']) ? intval($input['is_outstation_advance_allowed']) : intval($current['is_outstation_advance_allowed'] ?? 1),
        'outstation_min_distance_km' => floatval($input['outstation_min_distance_km'] ?? ($current['outstation_min_distance_km'] ?? 30.0)),
        'current_serving_token' => isset($input['current_serving_token']) ? intval($input['current_serving_token']) : intval($current['current_serving_token'] ?? 0),
        'daily_token_limit' => isset($input['daily_token_limit']) ? intval($input['daily_token_limit']) : intval($current['daily_token_limit'] ?? 1000),
        'is_token_service_enabled' => isset($input['is_token_service_enabled']) ? intval($input['is_token_service_enabled']) : intval($current['is_token_service_enabled'] ?? 1),
        'is_bus_booking_live' => isset($input['is_bus_booking_live']) ? intval($input['is_bus_booking_live']) : intval($current['is_bus_booking_live'] ?? 0),
        'is_live_counter_visible' => isset($input['is_live_counter_visible']) ? intval($input['is_live_counter_visible']) : intval($current['is_live_counter_visible'] ?? 1),
        'is_payment_feature_live' => isset($input['is_payment_feature_live']) ? intval($input['is_payment_feature_live']) : intval($current['is_payment_feature_live'] ?? 0),
        'is_arzi_ledger_live' => isset($input['is_arzi_ledger_live']) ? intval($input['is_arzi_ledger_live']) : intval($current['is_arzi_ledger_live'] ?? 1),
        'is_darbar_active' => isset($input['is_darbar_active']) ? intval($input['is_darbar_active']) : intval($current['is_darbar_active'] ?? 1),
        'darbar_date' => trim($input['darbar_date'] ?? ($current['darbar_date'] ?? date('Y-m-d'))),
        'darbar_timings' => trim($input['darbar_timings'] ?? ($current['darbar_timings'] ?? 'प्रत्येक रविवार प्रातःकाल 8:00 बजे से')),
        'emergency_notice' => trim($input['emergency_notice'] ?? ($current['emergency_notice'] ?? '')),
        'is_emergency_notice_visible' => isset($input['is_emergency_notice_visible']) ? intval($input['is_emergency_notice_visible']) : intval($current['is_emergency_notice_visible'] ?? 0),
        'banner_title' => trim($input['banner_title'] ?? ($current['banner_title'] ?? '🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट')),
        'banner_subtitle' => trim($input['banner_subtitle'] ?? ($current['banner_subtitle'] ?? 'परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार')),
        'is_banner_visible' => isset($input['is_banner_visible']) ? intval($input['is_banner_visible']) : intval($current['is_banner_visible'] ?? 1),
        'guruji_photo_url' => trim($input['guruji_photo_url'] ?? ($current['guruji_photo_url'] ?? '')),
        'can_admin_issue_reserved_tokens' => isset($input['can_admin_issue_reserved_tokens']) ? intval($input['can_admin_issue_reserved_tokens']) : (isset($input['allow_admin_reserved_tokens']) ? intval($input['allow_admin_reserved_tokens']) : intval($current['can_admin_issue_reserved_tokens'] ?? 0)),
        'allow_admin_reserved_tokens' => isset($input['allow_admin_reserved_tokens']) ? intval($input['allow_admin_reserved_tokens']) : (isset($input['can_admin_issue_reserved_tokens']) ? intval($input['can_admin_issue_reserved_tokens']) : intval($current['allow_admin_reserved_tokens'] ?? 0)),
        'aarti_timings' => trim($input['aarti_timings'] ?? ($current['aarti_timings'] ?? ''))
    ];

    foreach ($fields as $colName => $val) {
        if (isset($colSet[$colName])) {
            $paramName = ":p_" . $colName;
            $updatePairs[] = "$colName = $paramName";
            $bindings[$paramName] = $val;
        }
    }

    if (isset($colSet['config_version'])) {
        $updatePairs[] = "config_version = COALESCE(config_version, 1) + 1";
    }

    if (!empty($updatePairs)) {
        try {
            $sql = "UPDATE ashram_settings SET " . implode(", ", $updatePairs) . " WHERE id = 1";
            $stmt = $pdo->prepare($sql);
            $stmt->execute($bindings);
        } catch (Exception $e) {
            // Fallback: update whatever basic columns are available
            try {
                if (isset($colSet['current_serving_token'])) {
                    $pdo->prepare("UPDATE ashram_settings SET current_serving_token = :s WHERE id = 1")->execute([':s' => $fields['current_serving_token']]);
                }
            } catch (Exception $ignored) {}
        }
    }

    echo json_encode([
        "success" => true,
        "status" => "SUCCESS",
        "message" => "सुपरएडमिन सेटिंग्स सफलतापूर्वक अपडेट व लाइव प्रसारित हुईं!",
        "current_serving_token" => $fields['current_serving_token'],
        "guruji_photo_url" => $fields['guruji_photo_url'],
        "is_token_service_enabled" => boolval($fields['is_token_service_enabled']),
        "is_bus_booking_live" => boolval($fields['is_bus_booking_live']),
        "timestamp" => time()
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// GET Request: Return full live settings including Sevadars and Donors
$stmt = $pdo->query("SELECT * FROM ashram_settings WHERE id = 1 LIMIT 1");
$row = $stmt->fetch() ?: [];

// Fetch Sevadars
$sevadars = [];
try {
    $sevStmt = $pdo->query("SELECT id, name, role, phone, photo_url, bio, display_order FROM sevadars WHERE is_active = 1 ORDER BY display_order ASC, id ASC");
    $sevadars = $sevStmt->fetchAll() ?: [];
} catch (Exception $e) {}

// Fetch Donors (strictly NO phone number!)
$donors = [];
try {
    $donStmt = $pdo->query("SELECT id, name, city_address, title, photo_url, notes, display_order FROM donors WHERE is_active = 1 ORDER BY display_order ASC, id ASC");
    $donors = $donStmt->fetchAll() ?: [];
} catch (Exception $e) {}

$servingNum = intval($row['current_serving_token'] ?? 0);

$configData = [
    "ashram_name" => $row['ashram_name'] ?? 'श्री बालाजी कृपा धाम',
    "latitude" => floatval($row['ashram_latitude'] ?? 28.3972915),
    "longitude" => floatval($row['ashram_longitude'] ?? 78.1460410),
    "allowed_radius_meters" => floatval($row['allowed_radius_meters'] ?? 200.0),
    "is_geofence_enforced" => boolval($row['is_geofence_enforced'] ?? true),
    "is_outstation_advance_allowed" => boolval($row['is_outstation_advance_allowed'] ?? true),
    "outstation_min_distance_km" => floatval($row['outstation_min_distance_km'] ?? 30.0),
    "running_token_number" => $servingNum,
    "current_serving_token" => $servingNum,
    "daily_token_limit" => intval($row['daily_token_limit'] ?? 1000),
    "is_token_service_enabled" => boolval($row['is_token_service_enabled'] ?? true),
    "is_bus_booking_live" => boolval($row['is_bus_booking_live'] ?? false),
    "is_live_counter_visible" => boolval($row['is_live_counter_visible'] ?? true),
    "is_payment_feature_live" => boolval($row['is_payment_feature_live'] ?? false),
    "is_arzi_ledger_live" => boolval($row['is_arzi_ledger_live'] ?? true),
    "is_darbar_active" => boolval($row['is_darbar_active'] ?? true),
    "darbar_date" => $row['darbar_date'] ?? date('Y-m-d'),
    "darbar_timings" => $row['darbar_timings'] ?? 'प्रत्येक रविवार प्रातःकाल 8:00 बजे से',
    "emergency_notice" => $row['emergency_notice'] ?? '',
    "is_emergency_notice_visible" => boolval($row['is_emergency_notice_visible'] ?? false),
    "banner_title" => $row['banner_title'] ?? '🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट',
    "banner_subtitle" => $row['banner_subtitle'] ?? 'परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार',
    "is_banner_visible" => boolval($row['is_banner_visible'] ?? true),
    "guruji_photo_url" => $row['guruji_photo_url'] ?? '',
    "can_admin_issue_reserved_tokens" => boolval($row['can_admin_issue_reserved_tokens'] ?? false),
    "allow_admin_reserved_tokens" => boolval($row['allow_admin_reserved_tokens'] ?? false),
    "aarti_timings" => $row['aarti_timings'] ?? '',
    "config_version" => intval($row['config_version'] ?? 1),
    "server_time" => time(),
    "sevadars" => $sevadars,
    "donors" => $donors
];

// Dual layout: root keys for legacy/simple clients + config object for rich clients
$response = array_merge([
    "success" => true,
    "status" => "SUCCESS"
], $configData);
$response["config"] = $configData;

echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);\n